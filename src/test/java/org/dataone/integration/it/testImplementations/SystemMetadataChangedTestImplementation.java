package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
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
 *      <li>We have one or more working CNs in the environment</li>
 *      <li>We have two or more working MNs in the environment</li>
 *      <li>The MNs/CNs are properly registered with each other.</li>
 *      <li>The metadata can by synced to the the other MNs in the environment</li>
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
    @Ignore("Not completely implemented yet")
    public void testUpdateMetadata() {
     
        assertTrue("This test requires at least two MNs to work.", mnList.size() >= 2);
        assertTrue("This test requires at least one CN to work.", cnList.size() >= 1);

        String subjectLabel = cnSubmitter;
        
        Node mnNode = mnList.get(0);
        CommonCallAdapter mn = new CommonCallAdapter(getSession(subjectLabel), mnNode, "v2");
        try {

            // create some data to work with
            Identifier pid = new Identifier();
            //  pid.setValue("SystemMetadataChangedTest_" + ExampleUtilities.generateIdentifier());
            pid.setValue("SystemMetadataChangedTest");    // FIXME <-- revert this later :p
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            
            D1Object d1o = new D1Object(pid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(subjectLabel),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            
            SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            // null sysmeta.ReplicationPolicy should mean it gets 2 replicas by default
            InputStream objectInputStream = new ByteArrayInputStream(contentBytes);

            Identifier createdPid = mn.create(null, pid, objectInputStream, sysmeta);
            
            // CN should sync the data                         (alternatively, use existing data?)
            Thread.sleep(SYNC_TIME);   // sleep long enough for CN sync to happen
            
            // modify the data
            sysmeta = mn.getSystemMetadata(null, pid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            Date nowIsh = new Date();
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = mn.updateSystemMetadata(null, pid, sysmeta);
            assertTrue("MN should have modified its own system metadata successfully.", success);
            // TODO ^ does the MN update its own sysmeta the same way it would other MNs' ?
            
            // notify CN of the MN's change
            Node cnNode = cnList.get(0);
            CNCallAdapter cn = new CNCallAdapter(getSession(subjectLabel), cnNode, "v2");
            success = cn.updateSystemMetadata(null, createdPid, sysmeta);
            assertTrue("CN should have had its system metadata updated successfully.", success);
            
            // verify that sysmeta fetched from CN is updated
            SystemMetadata fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            boolean serialVersionMatches = fetchedCNSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedCNSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata fetched from CN should now have updated serialVersion.", serialVersionMatches);
            assertTrue("System metadata fetched from CN should now have updated dateSysMetadataModified.", dateModifiedMatches );
            
            // CN needs to run replication in order for metadata to contain replica info
            // and in order for originating MN to update replica-holding MNs
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
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession(subjectLabel), replicaHolderNode, "v2");
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
            // TODO delete(pid) ... or ideally purge(pid)
        }
    }
    
}
