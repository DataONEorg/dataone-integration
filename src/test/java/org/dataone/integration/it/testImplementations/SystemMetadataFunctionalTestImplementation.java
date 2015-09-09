package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
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
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
 * </ul>
 * 
 * @author Andrei
 */
public class SystemMetadataFunctionalTestImplementation extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    private Node mnV2NoSync;
    private static final long SYNC_WAIT = 5 * 60000;       // FIXME this is based on manually setting sync time on MNs
    private static final long REPLICATION_WAIT = 1 * 60000;
    
    @Before
    public void setup() {
        cnList = new ArrayList<Node>();
        mnList = new ArrayList<Node>();

        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        if(cnIter != null)
            cnList = IteratorUtils.toList(cnIter);
        
        assertTrue("Test requires at least one CN to function!", cnList.size() > 0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        
        // check node list, add an MN to mnList if:
        //      it supports v2 calls
        //      it supports MNReplication
        try {
            for(Node n : cn.listNodes().getNodeList()) {
                if(n.getType() == NodeType.MN)
                    try {
                        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                        Node capabilities = mnCallAdapter.getCapabilities();    // also doubles as a v2 ping
                        
                        List<Service> serviceList = capabilities.getServices().getServiceList();
                        for (Service service : serviceList)
                            if(service.getName().equalsIgnoreCase("MNReplication")) {
                                mnList.add(n);
                                break;
                            }
                    }
                    catch (Exception e1) {
                        log.warn("MN failed V2 getCapabilities(), skipping : " + n.getIdentifier().getValue());
                    }
            }
        } catch (Exception e) {
            log.error("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }

        log.info("v2 CNs available:                         " + cnList.size());
        log.info("v2 MNs available supporting replication:  " + mnList.size());
        
        for (Node n : cnList)
            log.info("CN:           " + n.getBaseURL());
        for (Node n : mnList)
            log.info("v2 MN:        " + n.getBaseURL());
        
        assertTrue("This test requires at least two v2 MNs that are tier 4 (support replication) to work.", mnList.size() >= 2);
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
                org.dataone.service.types.v2.Node v2Capabilities = TypeFactory.convertTypeFromType(capabilities, org.dataone.service.types.v2.Node.class);
                cn.updateNodeCapabilities(null, v2Capabilities.getIdentifier(), v2Capabilities);
                mnV2NoSync = capabilities;
                break;
            } catch (Exception e) {
                log.error("Unable to disable synchronization on MN! : " + n.getBaseURL() + "\n"
                        + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        }
        
        assertTrue("Environment for test must have at least one v2 MN with synchronize disabled "
                + "(so we can test if CN.synchronize() works on its own correctly).", mnV2NoSync != null);
    }
    
    @After
    public void tearDown() {
        if(mnV2NoSync == null)
            return;
        
        Node cNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cNode, "v2");
        
        try {
            NodeReference nodeRef = mnV2NoSync.getIdentifier();
            mnV2NoSync.setSynchronize(true);
            org.dataone.service.types.v2.Node v2Capabilities = TypeFactory.convertTypeFromType(mnV2NoSync, org.dataone.service.types.v2.Node.class);
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
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        try {
            
            // create a test object
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.CHANGE_PERMISSION);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_" + ExampleUtilities.generateIdentifier());
            
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() +  "Unable to create a test object: " + pid);
            }
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "created test object: " + createdPid.getValue());
            
            // modify the sysmeta
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "fetching sysmeta from auth MN");
                    
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            
            Date originalSysmetaModified = sysmeta.getDateSysMetadataModified();
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "original sysmeta.dateSystemMetadataChanged: " + originalSysmetaModified);
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "original sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "new sysmeta.dateSystemMetadataChanged: " + sysmeta.getDateSysMetadataModified());
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "new sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updating sysmeta on auth MN");
                    
			boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Call to MN updateSystemMetadata failed: " + be.getMessage());
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updated sysmeta on auth MN successfully");
                    
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            Node cnNode = cnList.get(0);
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

            // CN needs time to synchronize
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN.synchronize() to run (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            // TODO not the same as CN sync task wait time
            Thread.sleep(SYNC_WAIT);
            
            // verify that sysmeta fetched from CN is updated
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for CN.synchronize(), verifying CN has correct sysmeta");
            
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().after(originalSysmetaModified);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "CN sysmeta matches updates made on MN");
                    
            // TODO could also inspect the system metadata on the CN 
            //      for a replica in 'requested' status 
            //      and wait for it to move to 'completed' or 'failed'
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN to trigger replication "
                    + "so we have replica info in sysmeta (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            
            Thread.sleep(REPLICATION_WAIT);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for replication, fetching sysmeta from CN");
                    
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "looking at MNs that have a replica");
                    
            // notify replica-holding MNs of the sysmeta change
            int replicasFound = 0;
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        log.info("testSystemMetadataChanged_ExistingObj:   "
                                + "found replica MN: " + replicaNodeRef.getValue());
                        break;
                    }

                if (replicaHolderNode == null)
                    continue;   // may be a CN NodeRef, skip 
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "checking if replica MN holds updated sysmeta");
                        
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                replicasFound++;
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "sysmeta found on replica MN, checking contents");
                
                serialVersionMatches = replicaSysmeta.getSerialVersion().equals(newSerialVersion);
                dateModifiedMatches = replicaSysmeta.getDateSysMetadataModified().after(originalSysmetaModified);
                assertTrue("System metadata fetched from replica-holder MN should now have updated serialVersion", serialVersionMatches);
                assertTrue("System metadata fetched from replica-holder MN should now have updated dateSysMetadataModified", dateModifiedMatches );
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "sysmeta contents are updated");
            }
            
            assertTrue("Should have found at least one replica.", replicasFound > 0);
            
        } catch (Exception e) {
            assertTrue("Testing failed with exception: " + e.getMessage(), false);
            e.printStackTrace();
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.error("Unable to delete test pid after running the test: " + createdPid, e2);
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
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        Node cnNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

        try {
            
            // create a test object
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(getSubject("testRightsHolder").getValue(), Permission.CHANGE_PERMISSION);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_ExistingObj_" + ExampleUtilities.generateIdentifier());
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Unable to create a test object: " + pid);
            }
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "created test object: " + createdPid.getValue());
            
            // wait for CN to synchronize
            //  - we're testing the case where the object already exists on the CN
            //    at the time when CN.synchronize() gets called
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN sync (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            
            Thread.sleep(SYNC_WAIT);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for CN sync, fetching sysmeta");
            
            try {
                SystemMetadata fetchedSysmeta = cn.getSystemMetadata(null, createdPid);
                assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta", fetchedSysmeta != null);
            } catch (Exception e) {
                throw new AssertionError("cn.getSystemMetadata() should successfully fetch the sysmeta");
            }
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "fetched sysmeta from CN successfully");
            
            // modify the sysmeta
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "fetching sysmeta from auth MN");
            
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            Date originalSysmetaModified = sysmeta.getDateSysMetadataModified();
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "original sysmeta.dateSystemMetadataChanged: " + sysmeta.getDateSysMetadataModified());
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "original sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            accessRule = APITestUtils.buildAccessRule(getSubject("testRightsHolder").getValue(), Permission.CHANGE_PERMISSION);
            AccessPolicy accessPolicy = new AccessPolicy();
            accessPolicy.addAllow(accessRule);
            sysmeta.setAccessPolicy(accessPolicy);

            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "new sysmeta.dateSystemMetadataChanged: " + originalSysmetaModified);
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "new sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updating sysmeta on auth MN");
            
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Call to MN updateSystemMetadata failed: " + be.getMessage());
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updated sysmeta on auth MN successfully");
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            // CN needs time to synchronize
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN.synchronize() to run (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            // TODO not the same as CN sync task wait time
            Thread.sleep(SYNC_WAIT);
            
            // verify that sysmeta fetched from CN is updated
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for CN.synchronize(), verifying CN has correct sysmeta");
            
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().after(originalSysmetaModified);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "CN sysmeta matches updates made on MN");
            
            // TODO could also inspect the system metadata on the CN 
            //      for a replica in 'requested' status 
            //      and wait for it to move to 'completed' or 'failed'
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN to trigger replication "
                    + "so we have replica info in sysmeta (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            
            Thread.sleep(REPLICATION_WAIT);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for replication, fetching sysmeta from CN");
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("Replica list fetched from CN (after having time to replicate should not be null!", replicaList != null);
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "looking at MNs that have a replica");

            // check if replicas have updated sysmeta
            int replicasFound = 0;
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        log.info("testSystemMetadataChanged_ExistingObj:   "
                                + "found replica MN: " + replicaNodeRef.getValue());
                        break;
                    }
                
                if (replicaHolderNode == null)
                    continue;   // may be a CN NodeRef, skip 
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "checking if replica MN holds updated sysmeta");
                
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                replicasFound++;
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "sysmeta found on replica MN, checking contents");
                
                serialVersionMatches = replicaSysmeta.getSerialVersion().equals(newSerialVersion);
                dateModifiedMatches = replicaSysmeta.getDateSysMetadataModified().after(originalSysmetaModified);
                assertTrue("System metadata fetched from replica-holder MN should now have updated serialVersion", serialVersionMatches);
                assertTrue("System metadata fetched from replica-holder MN should now have updated dateSysMetadataModified", dateModifiedMatches );
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "sysmeta contents are updated");
            }
            
            assertTrue("Should have found at least one replica.", replicasFound > 0);
            
        } catch (Exception e) {
            assertTrue("Testing failed with exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), false);
            e.printStackTrace();
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.error("Unable to delete test pid after running the test: " + createdPid, e2);
                e2.printStackTrace();
            }
        }
    }
    
    @WebTestName("setReplicationStatus: calling CN.setReplicationStatus() doesn't succeed regardless of status")
    @WebTestDescription("This test needs to be run in an environment ... "
            + "Calling setReplicaStatus with an unchanging status should not succeed - the update should "
            + "only be valid if updating the replica status to a new one.")
    @Test
    public void testSetReplicationStatus_NoChange() {
        
        // documentation says setReplicationStatus can be called by another CN,
        // using CN cert since no MN certs available ... hope it works ...
        Identifier createdPid = null;
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        Node cnNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

        try {
            // create a test object
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.CHANGE_PERMISSION);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            Identifier pid = new Identifier();
            pid.setValue("testSetReplicationStatus_NoChange" + ExampleUtilities.generateIdentifier());
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
                Thread.sleep(10000);
                SystemMetadata mnSysmeta = mn.getSystemMetadata(null, createdPid);
                mnSysmeta.setSerialVersion(mnSysmeta.getSerialVersion().add(BigInteger.ONE));
                mnSysmeta.setRightsHolder(getSubject("testRightsHolder"));
                mn.updateSystemMetadata(null, createdPid, mnSysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Unable to create a test object: " + pid);
            }
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "created test object: " + createdPid.getValue());
            
            // wait for CN to synchronize
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "waiting for CN sync (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            
            Thread.sleep(SYNC_WAIT);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "done waiting for CN sync, fetching sysmeta");
            
            try {
                SystemMetadata fetchedSysmeta = cn.getSystemMetadata(null, createdPid);
                assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta", fetchedSysmeta != null);
            } catch (Exception e) {
                throw new AssertionError("cn.getSystemMetadata() should successfully fetch the sysmeta");
            }
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "fetched sysmeta from CN successfully");
            
            // modify the sysmeta
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "fetching sysmeta from auth MN");
            
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            Date originalSysmetaModified = sysmeta.getDateSysMetadataModified();
            log.info("testSetReplicationStatus_NoChange:   "
                    + "original sysmeta.dateSystemMetadataChanged: " + sysmeta.getDateSysMetadataModified());
            log.info("testSetReplicationStatus_NoChange:   "
                    + "original sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "new sysmeta.dateSystemMetadataChanged: " + originalSysmetaModified);
            log.info("testSetReplicationStatus_NoChange:   "
                    + "new sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "updating sysmeta on auth MN");
            
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                throw new AssertionError(mn.getLatestRequestUrl() + "Call to MN updateSystemMetadata failed: " + be.getMessage());
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "updated sysmeta on auth MN successfully");
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            // CN needs time to synchronize
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "waiting for CN.synchronize() to run (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            // TODO not the same as CN sync task wait time
            Thread.sleep(SYNC_WAIT);
            
            // verify that sysmeta fetched from CN is updated
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "done waiting for CN.synchronize(), verifying CN has correct sysmeta");
            
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().after(originalSysmetaModified);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "CN sysmeta matches updates made on MN");
            
            // TODO could also inspect the system metadata on the CN 
            //      for a replica in 'requested' status 
            //      and wait for it to move to 'completed' or 'failed'
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "waiting for CN to trigger replication "
                    + "so we have replica info in sysmeta (" + ((double)SYNC_WAIT / 60000) + " minutes)");
            
            Thread.sleep(REPLICATION_WAIT);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "done waiting for replication, fetching sysmeta from CN");
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("Test can't continue if replica list fetched from CN (after having time to replicate) is null!", replicaList != null);
            assertTrue("Test can't continue if system metadata fetched from CN does not have a non-empty replica list", replicaList.size() > 0);

            // request replication a bunch
            int statusUpdateFailures = 0; 
            for (int i = 0; i < 10; i++) {
                try {
                    log.info("testSetReplicationStatus_NoChange:   "
                            + "calling setReplicationStatus (iteration " + i + ")");
                    cn.setReplicationStatus(null, createdPid, mn.getNodeId(), ReplicationStatus.REQUESTED, new ServiceFailure("0101", "Setting replication status to INVALIDATED failed"));
                } catch (BaseException e) {
                    statusUpdateFailures++;
                    log.info("testSetReplicationStatus_NoChange:   "
                            + "status update failure (iteration " + i + ") " 
                            + e.getClass().getSimpleName() + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "status update failures: " + statusUpdateFailures);
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            BigInteger serialVersion = fetchedCNSysmeta.getSerialVersion();
            
            assertTrue("", serialVersion.intValue() < 10);
            log.info("testSetReplicationStatus_NoChange:   "
                    + "status update failures: " + statusUpdateFailures);
            
        } catch (Exception e) {
            assertTrue("Testing failed with exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), false);
            e.printStackTrace();
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.error("Unable to delete test pid after running the test: " + createdPid, e2);
                e2.printStackTrace();
            }
        }
    }
}
