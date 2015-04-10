package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Ignore;


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
public class SystemMetadataChangedTestImplementation extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    private static final long SYNC_TIME = 300000;           // FIXME is there a reliable way to know these?
    private static final long REPLICATION_TIME = 300000;    // FIXME "
    
    private static Log log = LogFactory.getLog(SystemMetadataChangedTestImplementation.class);
    
    @Before
    public void setup() {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        Iterator<Node> mnIter = getMemberNodeIterator();
        cnList = IteratorUtils.toList(cnIter);
        mnList = IteratorUtils.toList(mnIter);
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
            + "The MN calls CN.updateSystemMetadata() to update its version of the system metadata. (Synchronous call.)"
            + "The MN also calls MN.updateSystemMetadata() on other replica-holding MNs, to update their system metadata. (Synchronous call.)</li>")
    public void testSystemMetadataChanged() {
     
        assertTrue("This test requires at least two MNs to work.", mnList.size() >= 2);
        assertTrue("This test requires at least one CN to work.", cnList.size() >= 1);

        Identifier pid = null;
        Node mnNode = mnList.get(0);
        CommonCallAdapter mn = new CommonCallAdapter(getSession("testRightsHolder"), mnNode, "v2");
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            pid = new Identifier();
            pid.setValue("testSystemMetadataChanged_" + ExampleUtilities.generateIdentifier());
            Identifier createdPid = procureTestObject(mn, accessRule, pid);
            // procureTestObject() is probably creating a new object
            Thread.sleep(SYNC_TIME);   // sleep long enough for CN sync to happen
            
            // modify the data
            SystemMetadata sysmeta = mn.getSystemMetadata(null, pid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = mn.updateSystemMetadata(null, pid, sysmeta);
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            // notify CN of the MN's change
            Node cnNode = cnList.get(0);
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");
            success = cn.updateSystemMetadata(null, createdPid, sysmeta);
            assertTrue("CN should have had its system metadata updated successfully.", success);
            
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
                success = replicaHolderMN.updateSystemMetadata(null, createdPid, sysmeta);
                assertTrue("Replica-holder MN (" + replica.getReplicaMemberNode().getValue() + ") should have had its system metadata updated successfully.", success);
                
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
            // TODO purge(pid)
            try {
                if(pid != null)     
                    mn.delete(null, pid);
            } catch (Exception e2) {
            }
        }
    }
    
}
