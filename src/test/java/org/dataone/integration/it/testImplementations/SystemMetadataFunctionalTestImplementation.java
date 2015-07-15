package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * This class should prove whether the following series of events is functioning correctly:
 * 
 * <ol>
 *      <li>An object is created on an MN.</li>
 *      <li>The system metadata is changed on the MN. - we call MN.updateSystemMetadata(). This is a synchronous call.</li>
 *      <li>Under the hood, the MN will call CN.synchronize() to push the change out. This is a synchronous call but only tells us the request for synchronize was successful</li>
 *      <li>Under the hood, the CN REST will call CNStorage.updateSystemMetadata() to update the CN's sysmeta</li>
 *      <li>CN REST will also call MN.systemMetadataChanged() on notify them of new metadata.</li>
 *      <li>The replica MNs will then call getSystemMetadata() on the authoritative MN to update their copy.</li>
 * </ol>
 * 
 * Assumptions made:
 * </p>
 * <ul>
 *      <li>We have one or more working CNs in the environment</li>
 *      <li>We have two or more working v2 MNs in the environment</li>
 *      <li>At least one of the MNs has synchronization disabled 
 *      (this is so we can test if CN.synchronize() works on its own correctly, as opposed to the harbest sync task).</li>
 *      <li>The MNs/CNs are properly registered with each other.</li>
 *      <li>We know how long it takes the CN to sync MN data and how long it takes MNs to replicate authoritative sysmeta. 
 *      <b>NOTE: currently hard-coded to 30 minutes.</b></li>
 * </ul>
 * 
 * @author Andrei
 */
public class SystemMetadataFunctionalTestImplementation extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    private Node mnV2NoSync;
    private static final long SYNC_TIME = 30 * 60000;       // FIXME is there a reliable way to know these?
    private static final long REPLICATION_TIME = 5 * 60000;    // FIXME "
    
    private static Log log = LogFactory.getLog(SystemMetadataFunctionalTestImplementation.class);
    
    @Before
    public void setup() {
        cnList = new ArrayList<Node>();
        mnList = new ArrayList<Node>();

        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        if(cnIter != null)
            cnList = IteratorUtils.toList(cnIter);
        
        assertTrue("Test requires at least one CN to function!", cnList.size() > 0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        
        // check node list, add all MNs that support v2 to mnList
        try {
            for(Node n : cn.listNodes().getNodeList())
                if(n.getType() == NodeType.MN)
                    try {
                        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                        mnCallAdapter.ping();   // FIXME wish there was a better way to check for v2 support
                        mnList.add(n);
                    }
                    catch (Exception e1) {
                        log.warn("MN failed V2 ping(), skipping : " + n.getIdentifier().getValue());
                    }
        } catch (NotImplemented | ServiceFailure | InstantiationException
                | IllegalAccessException | InvocationTargetException | ClientSideException
                | JiBXException | IOException e) {
            log.error("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }

        log.info("CNs available: " + cnList.size());
        log.info("MNs available: " + mnList.size());
        
        assertTrue("This test requires at least two v2 MNs to work.", mnList.size() >= 2);
        assertTrue("This test requires at least one CN to work.", cnList.size() >= 1);
        
        // we need to test against an MN that has synchronize disabled
        // so we can be sure that the MN->CN synchronize() call is working
        // instead of the harvesting sync job
        
        // do we already have an MN with sync turned off?
        Node mnNoSync = null;
        for (Node n : mnList) {
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
            Node capabilities = null;
            try {
                capabilities = mnCallAdapter.getCapabilities();
            } catch (NotImplemented | ServiceFailure | ClientSideException e) {
                e.printStackTrace();
                log.error("Unable to get MN capabilities. "
                        + "Error: " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
            
            if (capabilities == null) {
                log.error("MN returned NULL capabilities from getCapabilities(). MN: " 
                        + n.getBaseURL());
                continue;
            }
            boolean synchronize = capabilities.isSynchronize();
            if (!synchronize) {
                mnNoSync = n;
                break;
            }
        }
        
        if (mnNoSync != null) {
            mnV2NoSync = mnNoSync;
            return;
        }
        
        // no MN with sync turned off?
        // disable sync on an MN
        
        for (Node n : mnList) {
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
            Node capabilities = null;
            try {
                capabilities = mnCallAdapter.getCapabilities();
                capabilities.setSynchronize(false);
                org.dataone.service.types.v2.Node v2Capabilities = TypeMarshaller.convertTypeFromType(capabilities, org.dataone.service.types.v2.Node.class);
                cn.updateNodeCapabilities(null, v2Capabilities.getIdentifier(), v2Capabilities);
                mnV2NoSync = capabilities;
                break;
            } catch (Exception e) {
                log.error("Unable to disable synchronization on MN! : " + n.getBaseURL());
            }
        }
        
        assertTrue("Environment for test must have at least one v2 MN with synchronize disabled "
                + "(so we can test if CN.synchronize() works on its own correctly).", mnV2NoSync != null);
    }
    
    @After
    public void tearDown() {
        Node cNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cNode, "v2");
        
        try {
            NodeReference nodeRef = mnV2NoSync.getIdentifier();
            mnV2NoSync.setSynchronize(true);
            org.dataone.service.types.v2.Node v2Capabilities = TypeMarshaller.convertTypeFromType(mnV2NoSync, org.dataone.service.types.v2.Node.class);
            cn.updateNodeCapabilities(null, nodeRef, v2Capabilities);
        } catch (Exception e) {
            throw new AssertionError("Unable to update MN capabilities to re-enable synchronization on: " + mnV2NoSync.getIdentifier());
        }
    }
    
    @Override
    protected String getTestDescription() {
        return "Tests the MN-triggered system metadata updating.";
    }

    @WebTestName("systemMetadataChanged: tests that changes made to system metadata by an MN are propegated")
    @WebTestDescription("This test needs to be run in an environment with two or more MNs, one of which must "
            + "have synchronization disabled (so we can test if CN.synchronize() works on its own correctly)."
            + "The test checks whether the following events on the MN trigger the correct changes: "
            + "Some metadata is created and changed on an MN, so when the CN sees it, it'll be a new object from its point of view."
            + "The MN calls CN.synchronize() to notify the CN to update its version of the object. (Asynchronous call.)"
            + "We then need to wait for the CN to synchronize."
            + "When that happens, the CN should update its own copy, then propegate the change to "
            + "the replica MNs. We check that the CN's as well as the other MNs' copies are up to date.")
    @Test
    public void testSystemMetadataChanged() {
        
        Identifier createdPid = null;
        CommonCallAdapter mn = new CommonCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        try {
            
            // create a test object
            
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_" + ExampleUtilities.generateIdentifier());
            try {
                createdPid = procureTestObject(mn, accessRule, pid);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() +  "Unable to create a test object: " + pid);
            }
            
            // modify the sysmeta
            
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Call to MN updateSystemMetadata failed: " + be.getMessage());
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            Node cnNode = cnList.get(0);
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

            // CN needs time to synchronize
            
            Thread.sleep(SYNC_TIME);
            
            // verify that sysmeta fetched from CN is updated
            
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            Thread.sleep(REPLICATION_TIME);
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            // notify replica-holding MNs of the sysmeta change
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        break;
                    }
                assertTrue("Should be able to find another MN that holds a replica.", replicaHolderNode != null);
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                serialVersionMatches = replicaSysmeta.getSerialVersion().equals(newSerialVersion);
                dateModifiedMatches = replicaSysmeta.getDateSysMetadataModified().equals(nowIsh);
                assertTrue("System metadata fetched from replica-holder MN should now have updated serialVersion", serialVersionMatches);
                assertTrue("System metadata fetched from replica-holder MN should now have updated dateSysMetadataModified", dateModifiedMatches );
            }
            
        } catch (Exception e) {
            assertTrue("Testing failed with exception: " + e.getMessage(), false);
            e.printStackTrace();
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
    
    
    @WebTestName("systemMetadataChanged: tests that changes made to system metadata by an MN are propegated when the object is not a new one")
    @WebTestDescription("This test needs to be run in an environment with two or more MNs, one of which must "
            + "have synchronization disabled (so we can test if CN.synchronize() works on its own correctly)."
            + "The test checks whether the following events on the MN trigger the correct changes: "
            + "Some metadata is created on an MN then waits for the CN to sync this data. The data is then modified on the MN."
            + "The MN calls CN.synchronize() to notify the CN to update its version of the object. (Asynchronous call.)"
            + "We then need to wait for the CN to synchronize."
            + "When that happens, the CN should update its own copy, then propegate the change to "
            + "the replica MNs. We check that the CN's as well as the other MNs' copies are up to date.")
    @Test
    public void testSystemMetadataChanged_ExistingObj() {
        
        Identifier createdPid = null;
        CommonCallAdapter mn = new CommonCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        Node cnNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

        try {
            
            // create a test object
            
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_ExistingObj_" + ExampleUtilities.generateIdentifier());
            try {
                createdPid = procureTestObject(mn, accessRule, pid);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Unable to create a test object: " + pid);
            }
            
            // wait for CN to synchronize
            //  - we're testing the case where the object already exists on the CN
            //    at the time when CN.synchronize() gets called
            
            Thread.sleep(SYNC_TIME);
            
            try {
                SystemMetadata fetchedSysmeta = cn.getSystemMetadata(null, createdPid);
                assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta", fetchedSysmeta != null);
            } catch (Exception e) {
                throw new AssertionError("cn.getSystemMetadata() should successfully fetch the sysmeta");
            }
            
            // modify the sysmeta
            
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Call to MN updateSystemMetadata failed: " + be.getMessage());
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            // CN needs time to synchronize
            
            Thread.sleep(SYNC_TIME);
            
            // verify that sysmeta fetched from CN is updated
            
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            Thread.sleep(REPLICATION_TIME);
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            // notify replica-holding MNs of the sysmeta change
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        break;
                    }
                assertTrue("Should be able to find another MN that holds a replica.", replicaHolderNode != null);
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                serialVersionMatches = replicaSysmeta.getSerialVersion().equals(newSerialVersion);
                dateModifiedMatches = replicaSysmeta.getDateSysMetadataModified().equals(nowIsh);
                assertTrue("System metadata fetched from replica-holder MN should now have updated serialVersion", serialVersionMatches);
                assertTrue("System metadata fetched from replica-holder MN should now have updated dateSysMetadataModified", dateModifiedMatches );
            }
            
        } catch (Exception e) {
            assertTrue("Testing failed with exception: " + e.getMessage(), false);
            e.printStackTrace();
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
}
