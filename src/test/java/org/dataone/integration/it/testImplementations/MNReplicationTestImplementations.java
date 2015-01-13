package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v1.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Ignore;
import org.junit.Test;

public class MNReplicationTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(MNReplicationTestImplementations.class);
    
    
    public MNReplicationTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    public void testReplicate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate(nodeIterator.next(), version);
    }
    
//  @Ignore("test not implemented")
    public void testReplicate(Node node, String version) {
        
    }
    
    public void testReplicate_NoCertificate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_NoCertificate(nodeIterator.next(), version);
    }

    /**
     *  Test MN.Replicate() functionality
     */
    public void testReplicate_NoCertificate(Node node, String version) {

        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);             
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2 , sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"should not be able to initiate replication without a certificate");
        }
        catch (NotAuthorized na) {
            // expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    public void testReplicate_ValidCertificate_NotCN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_ValidCertificate_NotCN(nodeIterator.next(), version);
    }

    /**
     *  Test MN.Replicate() functionality
     */
    public void testReplicate_ValidCertificate_NotCN(Node node, String version) {

        ContextAwareTestCaseDataone.setupClientSubject("testPerson");
        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_ValidCertificate_NotCN vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);             
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2, sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"should not be able to initiate replication a certificate representing a CN");
        }
        catch (NotAuthorized na) {
            // expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    public void testReplicate_FaultyNodeReference(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_FaultyNodeReference(nodeIterator.next(), version);
    }
    
    /**
     *  Test MN.Replicate() functionality
     */
//    @Ignore("need to create testCN certificate to run this subtest")
    public void testReplicate_FaultyNodeReference(Node node, String version) {

        ContextAwareTestCaseDataone.setupClientSubject("testCN");

        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2, sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"replicate call should not succeed with faulty node reference");
        }
        catch (InvalidRequest na) {
            // expected behavior ??
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected InvalidRequest, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    
    
    
    
    
    
    
    
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------
    
    
    
    
    
    
    
    
    private static final String format_text_plain = "text/plain";

    private static final long replicationDelay = 120000; //240000; //how many minutes? 2? 4?

    private Subject subject;
    private String originMNId = "DEMO1";
    private String targetMNId = "DEMO2";
    private String blockedMNId = "DEMO4";
    MNode originMN;
    MNode targetMN;
    MNode blockedMN;
    Node originNode;
    Node targetNode;
    Node blockedNode;
    private InputStream textPlainSource;
    private SystemMetadata sysMeta;

    /**
     * Test replication from origin to at least one other target node
     */
    public void testReplicateOnCreateWithoutPreferredList(Iterator<Node> memberNodeIterator) {
        // set up the common member variables
        setup(memberNodeIterator);
        
        // Ensure we have valid system metadata fields for replication
        // build a valid ReplicationPolicy
        ReplicationPolicy policy = new ReplicationPolicy();
        policy.setReplicationAllowed(true);
        policy.setNumberReplicas(1);

        List<NodeReference> preferredList = new ArrayList<NodeReference>();
        List<NodeReference> blockedList = new ArrayList<NodeReference>();

        policy.setBlockedMemberNodeList(blockedList);
        policy.setPreferredMemberNodeList(preferredList);
        sysMeta.setReplicationPolicy(policy);

        // create the object on the origin node
        submit();

        // check one of the randomly selected target nodes
        checkAnyTarget();
    }

    /**
     * Test replication from origin to a preferred target node
     * 
     */
    public void testReplicateOnCreateWithPreferredList(Iterator<Node> memberNodeIterator) {

        int count = 1; //10;
        while (count > 0) {
            count--;
        
            // set up the common member variables
            setup(memberNodeIterator);
            
            // Ensure we have valid system metadata fields for this test
            // build a valid ReplicationPolicy
            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setReplicationAllowed(true);
            policy.setNumberReplicas(1);
            
            // the preferred list of targets
            List<NodeReference> preferredList = new ArrayList<NodeReference>();
            preferredList.add(targetNode.getIdentifier());
            List<NodeReference> blockedList = new ArrayList<NodeReference>();
    
            policy.setBlockedMemberNodeList(blockedList);
            policy.setPreferredMemberNodeList(preferredList);
            sysMeta.setReplicationPolicy(policy);
    
            // create the object on the origin node
            submit();
            
            // check the target node
            //checkTarget();
        }

    }

    /**
     * Test replication from origin to any node other than blocked node
     * 
     */
    public void testReplicateOnCreateWithBlockedList(Iterator<Node> memberNodeIterator) {

        // set up the common member variables
        setup(memberNodeIterator);
        
        // Ensure we have valid system metadata fields for this test
        // build a valid ReplicationPolicy
        ReplicationPolicy policy = new ReplicationPolicy();
        policy.setReplicationAllowed(true);
        policy.setNumberReplicas(1);
        
        // the blocked list of targets
        List<NodeReference> preferredList = new ArrayList<NodeReference>();
        List<NodeReference> blockedList = new ArrayList<NodeReference>();
        blockedList.add(blockedNode.getIdentifier());
        
        policy.setBlockedMemberNodeList(blockedList);
        policy.setPreferredMemberNodeList(preferredList);
        sysMeta.setReplicationPolicy(policy);

        // create the object on the origin node
        submit();
        
        // check the blocked node to make sure it is node there
        checkBlockedTarget();

    }
    
    /**
     * Test replication from origin when no policy is given
     * 
     */
    public void testReplicateOnCreateNoPolicy(Iterator<Node> memberNodeIterator) {

        // set up the common member variables
        setup(memberNodeIterator);
        
        // Ensure we have valid system metadata fields for this test
        sysMeta.setReplicationPolicy(null);

        // create the object on the origin node
        submit();
        
        // check that there are NOT replicas
        checkAllTargets(false);
    }
    
    /**
     * Set up the usual scenario -- individual tests should mutate the 
     * sysMeta object for their specific needs
     */
    protected void setup(Iterator<Node> memberNodeIterator) {

        // the subject nad certificate
        subject = ContextAwareTestCaseDataone.setupClientSubject("testRightsHolder");
        log.debug("Subject is: " + subject.getValue());

        // iterate over the node list and set the source and target nodes
        while (memberNodeIterator.hasNext()) {
            Node currentNode = memberNodeIterator.next();
            String currentNodeId = currentNode.getIdentifier().getValue();
            if (currentNodeId.equals(originMNId)) {
                originNode = currentNode;
            } else if (currentNodeId.equals(targetMNId)) {
                targetNode = currentNode;
            } else if (currentNodeId.equals(blockedMNId)) {
                blockedNode = currentNode;
            }
        }

        // get callable MN objects
        try {
            
            // FIXME the following MN variables are not used in this class
            // does fetching them have a side-effect that's necessary 
            // for these tests to run ??? or... can we remove this bit?
            
            originMN = D1Client.getMN(originNode.getIdentifier());
            targetMN = D1Client.getMN(targetNode.getIdentifier());
            blockedMN = D1Client.getMN(blockedNode.getIdentifier());
            
        } catch (ServiceFailure e) {
            fail("Couldn't get origin or target node objects: " + e.getMessage());
        }

        String identifierStr = ExampleUtilities.generateIdentifier();
        Identifier guid = new Identifier();
        guid.setValue("MNReplicationIT." + identifierStr);

        try {
            textPlainSource = 
                new ByteArrayInputStream("<test>Plain text source</test>".getBytes("UTF-8"));
            log.debug("Data string is: " + IOUtils.toString(textPlainSource));
        } catch (Exception e) {
            fail("Couldn't get an example input stream: " + e.getMessage());
        }

        // build the system metadata object
        org.dataone.service.types.v1.SystemMetadata sysMetaV1 = ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource, null);
        try {
            sysMeta = TypeMarshaller.convertTypeFromType(sysMetaV1, SystemMetadata.class);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | JiBXException | IOException e1) {
            fail("Couldn't convert system metadata to the v2 type: " + e1.getMessage());
        }
        //ExampleUtilities.generateSystemMetadata(guid, "eml://ecoinformatics.org/eml-2.1.0", textPlainSource, null);

        // Ensure we have valid system metadata fields for tests

        // clear the Replica list
        sysMeta.clearReplicaList();

        // build a valid access policy
        AccessPolicy accessPolicy = new AccessPolicy();
        AccessRule allowRule = new AccessRule();
        Subject publicSubject = new Subject();
        publicSubject.setValue("public");
        allowRule.addSubject(publicSubject);
        allowRule.addPermission(Permission.READ);
        accessPolicy.addAllow(allowRule);
        sysMeta.setAccessPolicy(accessPolicy);

        // update other critical fields for replication
        sysMeta.setAuthoritativeMemberNode(originNode.getIdentifier());
        sysMeta.setOriginMemberNode(originNode.getIdentifier());

        // set the submitter as the cert DN
        try {
            X509Certificate certificate = 
                CertificateManager.getInstance().loadCertificate();
            String ownerX500 = 
                CertificateManager.getInstance().getSubjectDN(certificate);
            sysMeta.getRightsHolder().setValue(ownerX500);
            sysMeta.getSubmitter().setValue(ownerX500);
        } catch (Exception e) {
            // warn about this
            e.printStackTrace();
        }
    }
    
    /**
     * save the object, then check the 
     */
    protected void submit() {

        // try the create
        try {
            log.debug("adding object to origin node " + originMNId);
            Identifier pid = 
                originMN.create(null, sysMeta.getIdentifier(), textPlainSource, sysMeta);
            log.debug("Created object, pid=" + pid.getValue());
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * check the target node after a time delay
     */
    protected void checkTarget() {

        // look for it on the target
        try {
            // wait for replication
            log.debug("Waiting for replication, delay=" + replicationDelay);
            Thread.sleep(replicationDelay);
            
            // now check
            log.debug("checking target node for object, pid=" + sysMeta.getIdentifier().getValue());
            InputStream returnedObject = targetMN.get(null, sysMeta.getIdentifier());
            log.debug(
                    "Returned data string is: " + IOUtils.toString(returnedObject));
            assertTrue(IOUtils.contentEquals(textPlainSource, returnedObject));
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Check that a replica DID NOT end up on a blocked MN
     */
    protected void checkBlockedTarget() {

        // look for it on the target
        try {
            // wait for replication
            log.debug("Waiting for replication, delay=" + replicationDelay);
            Thread.sleep(replicationDelay);
            
            // look at the CN's sysmeta
            int replicaCount = 1;
            log.debug("checking CN for system metadata, pid=" + sysMeta.getIdentifier().getValue());
            SystemMetadata sysMetaCN = D1Client.getCN().getSystemMetadata(null, sysMeta.getIdentifier());
            // check that there is no replica for the blocked node 
            for (Replica replica: sysMetaCN.getReplicaList()) {
                // if it's on the blocked node, this is bad
                if (replica.getReplicaMemberNode().getValue().equals(blockedMNId)) {
                    fail("Replica is reported by CN to be on blocked node, " + blockedMNId);
                }
                replicaCount++;
            }
            log.debug("CN reports replica count=" + replicaCount);

            // we have enough replicas
            assertTrue(replicaCount >= sysMeta.getReplicationPolicy().getNumberReplicas());

            // now double check that the blocked node does not have the object
            log.debug("checking other target node for object, node=" + blockedMNId);
            
            try {
                InputStream returnedObject = blockedMN.get(null, sysMeta.getIdentifier());
                fail("Replica found on blocked node, " + blockedMNId);
            } catch (NotFound e) {
                // this is what we want
                log.debug("Object not found on the blocked node");
            }
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }
    
    protected String getTestDescription() {
        return "Test Case that runs through the Member Node Tier 4 (Replication) API methods";

    }

    /**
     * check the CN node after a time delay
     * Finds the replicas and checks that one of them was successful
     */
    protected void checkAnyTarget() {
    
        // look for it on the target
        try {
            // wait for replication
            log.debug("Waiting for replication, delay=" + replicationDelay);
            Thread.sleep(replicationDelay);
            
            // look at the CN's sysmeta
            int replicaCount = 1;
            NodeReference otherTarget = null;
            log.debug("checking CN for system metadata, pid=" + sysMeta.getIdentifier().getValue());
    
            SystemMetadata sysMetaCN = D1Client.getCN().getSystemMetadata(null, sysMeta.getIdentifier());
            for (Replica replica: sysMetaCN.getReplicaList()) {
                // if it's on another node besides the orign, increment the count
                if (!replica.getReplicaMemberNode().getValue().equals(originMNId)) {
                    replicaCount++;
                    otherTarget = replica.getReplicaMemberNode();
                }
            }
            log.debug("CN reports replica count=" + replicaCount);
    
            // we have enough replicas
            assertTrue(replicaCount >= sysMeta.getReplicationPolicy().getNumberReplicas());
    
            // now check that other node
            log.debug("checking other target node for object, node=" + otherTarget.getValue());
            InputStream returnedObject = D1Client.getMN(otherTarget).get(null, sysMeta.getIdentifier());
            log.debug("Returned data string is: " + IOUtils.toString(returnedObject));
            assertTrue(IOUtils.contentEquals(textPlainSource, returnedObject));
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * check the CN node after a time delay
     * Finds the replicas and checks that they all exist
     */
    protected void checkAllTargets(boolean exists) {
    
        // look for it on the target
        try {
            // wait for replication
            log.debug("Waiting for replication, delay=" + replicationDelay);
            Thread.sleep(replicationDelay);
            
            // look at the CN's sysmeta
            int replicaCount = 1;
            NodeReference otherTarget = null;
            log.debug("checking CN for system metadata, pid=" + sysMeta.getIdentifier().getValue());
    
            SystemMetadata sysMetaCN = D1Client.getCN().getSystemMetadata(null, sysMeta.getIdentifier());
            for (Replica replica: sysMetaCN.getReplicaList()) {
                // if it's on another node besides the origin, increment the count
                if (!replica.getReplicaMemberNode().getValue().equals(originMNId)) {
                    replicaCount++;
                    otherTarget = replica.getReplicaMemberNode();
                    
                    // now check that other node has it or not
                    InputStream returnedObject = null;
                    try {
                        log.debug("checking other target node for object, node=" + otherTarget.getValue());
                        returnedObject = D1Client.getMN(otherTarget).get(null, sysMeta.getIdentifier());
                        log.debug("Returned data string is: " + IOUtils.toString(returnedObject));
                    } catch (NotFound nfe) {
                        if (exists) {
                            fail("Replica not found where it should be, node=" + otherTarget.getValue());
                        } else {
                            // this passes for the node
                            assertTrue(IOUtils.contentEquals(textPlainSource, returnedObject));
                        }
                    }
                }
            }
            log.debug("CN reports replica count=" + replicaCount);
    
            // we have enough replicas, where applicable?
            if (sysMeta.getReplicationPolicy() != null) {
                assertTrue(replicaCount >= sysMeta.getReplicationPolicy().getNumberReplicas());
            }
            
        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }
}
