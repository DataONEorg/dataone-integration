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
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
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
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Test;


/**
 * This class should prove whether the following series of events is functioning correctly:
 * 
 * <ol>
 *      <li>Some metadata is changed on an MN</li>
 *      <li>The MN calls CN.updateSystemMetadata() to update its version of the system metadata. (Synchronous call.)</li>
 *      <li>The MN also calls MN.updateSystemMetadata() on other replica-holding MNs, to update their system metadata. (Synchronous call.)</li>
 * </ol>
 * 
 * Assumptions made:
 * </p>
 * <ul>
 *      <li>It's the originating MN's responsibility to update replica-holder MNs of sysmeta changes (as opposed to the CN's).</li>
 *      <li>We have one or more working CNs in the environment</li>
 *      <li>We have two or more working MNs in the environment</li>
 *      <li>The MNs/CNs are properly registered.</li>
 *      <li>We know how long it takes the CN to sync MN data (if using newly-created data).</li>
 * </ul>
 * 
 * @author Andrei
 */
public class SystemMetadataFunctionalTestImplementation extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    private static final long SYNC_TIME = 300000;           // FIXME is there a reliable way to know these?
    private static final long REPLICATION_TIME = 300000;    // FIXME "
    
    private static Log log = LogFactory.getLog(SystemMetadataFunctionalTestImplementation.class);
    
    @Before
    public void setup() {
        cnList = new ArrayList<Node>();
        mnList = new ArrayList<Node>();

        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        if(cnIter != null)
            cnList = IteratorUtils.toList(cnIter);
        
        CNCallAdapter cn = null;
        if(cnList.size() > 0)
            cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        if(cn != null) {
            try {
                for(Node n : cn.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (NotImplemented | ServiceFailure | InstantiationException
                    | IllegalAccessException | InvocationTargetException | ClientSideException
                    | JiBXException | IOException e) {
                log.warn("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
            }
        }

        log.info("CNs available: " + cnList.size());
        log.info("MNs available: " + mnList.size());
    }
    
    @Override
    protected String getTestDescription() {
        return "Tests the MN-triggered system metadata updating.";
    }

    @WebTestName("systemMetadataChanged: tests that changes made to system metadata by an MN are propegated")
    @WebTestDescription("This test checks whether the following events on the MN trigger the correct changes: "
            + "Some metadata is changed on an MN"
            + "The MN calls CN.synchronizeObject() to notify the CN to update its version of the object. (Asynchronous call.)"
            + "We then need to wait for the CN to synchronize."
            + "When that happens, the CN should update its own copy, then propegate the change to "
            + "the replica MNs. We check that the CN's as well as the other MNs' copies are up to date.")
    @Test
    public void testSystemMetadataChanged() {
        
        assertTrue("This test requires at least two MNs to work.", mnList.size() >= 2);
        assertTrue("This test requires at least one CN to work.", cnList.size() >= 1);
        
        Identifier createdPid = null;
        Node mnNode = mnList.get(0);
        CommonCallAdapter mn = new CommonCallAdapter(getSession("testRightsHolder"), mnNode, "v2");
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_" + ExampleUtilities.generateIdentifier());
            try {
                createdPid = procureTestObject(mn, accessRule, pid);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Unable to create a test object: " + pid);
            }
            
            // modify the data
            SystemMetadata sysmeta = mn.getSystemMetadata(null, createdPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Call to MN updateSystemMetadata failed: " + be.getMessage());
                be.printStackTrace();
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            // notify CN of the MN's change
            Node cnNode = cnList.get(0);
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");
            try {
                success = cn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Call to CN updateSystemMetadata failed: " + be.getMessage());
                be.printStackTrace();
            }
            assertTrue("CN should have been notified to synchronize object: " + createdPid, success);

            // CN needs time to synchronize
            Thread.sleep(SYNC_TIME);
            
            // verify that sysmeta fetched from CN is updated
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // so we can test if we can update the sysmeta on the replica-holding MNs
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
                // this should happen as part of CN sync / replication
                // so here we just check if the replicas have the updated version of sysmeta 
                
                // success = replicaHolderMN.updateSystemMetadata(null, createdPid, sysmeta);
                // assertTrue("Replica-holder MN (" + replica.getReplicaMemberNode().getValue() + ") should have had its system metadata updated successfully.", success);
                
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
