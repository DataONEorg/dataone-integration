package org.dataone.integration.it.testImplementations;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

public class CNReplicationTestImplementations extends ContextAwareAdapter {
    
    protected static Log log = LogFactory.getLog(CNReplicationTestImplementations.class);
    private static Map<String,ObjectList> listedObjects;
    
    public CNReplicationTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    
    // see:     CNodeTier4_metacat_IT.java
    
    
    private ObjectInfo getPrefetchedObject(String currentUrl, Integer index)
    {
        if (index == null) 
            index = new Integer(0);
        if (index < 0) {
            // return off the right end of the list
            index = listedObjects.get(currentUrl).getCount() + index;
        }
        return listedObjects.get(currentUrl).getObjectInfo(index);
    }

    public void prefetchObjects(Iterator<Node> cnIterator, String version) throws ServiceFailure {
        if (listedObjects == null) {
            listedObjects = new Hashtable<String,ObjectList>();
            while (cnIterator.hasNext()) {
                Node node = cnIterator.next();
                String currentUrl = node.getBaseURL();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
                
                try {
                    ObjectList ol = callAdapter.search(null, ContextAwareTestCaseDataone.QUERYTYPE_SOLR, ""); 
                    listedObjects.put(currentUrl, ol);
                } 
                catch (BaseException e) {
                    handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
                }
                catch(Exception e) {
                    log.warn(e.getClass().getName() + ": " + e.getMessage());
                }   
            }
        }
    }

    @WebTestName("setReplicationStatus - test with unauthorized certificate")
    @WebTestDescription("this tests the negative case, when we try to call setReplicationStatus with "
            + "an unauthorized certificate, and expect a NotAuthorized exception")
    public void testSetReplicationStatus_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationStatus_NotAuthorized(nodeIterator.next(), version);
    }
    
    /**
     * Test the that the membernode can set the replication status for on an object
     */
//    @Ignore("test not implemented")
    public void testSetReplicationStatus_NotAuthorized(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());

            boolean response = callAdapter.setReplicationStatus(null,oi.getIdentifier(),new NodeReference(),
                    ReplicationStatus.FAILED,new ServiceFailure("0000","a test exception"));

//              checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
            
            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationStatus should fail when using no-rights client subject");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotAuthorized e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationStatus - tests with an invalid request")
    @WebTestDescription("tests a negative case in which setReplicationStatus is called "
            + "with a bogus NodeReference and expect an InvalidRequest exception")
    public void testSetReplicationStatus_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationStatus_InvalidRequest(nodeIterator.next(), version);
    }
    
    /**
     * Test the that the membernode can set the replication status for on an object
     */
//    @Ignore("test not implemented")
    public void testSetReplicationStatus_InvalidRequest(Node node, String version) {
        
        //TODO: implement a memberNode test subject
//      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

        try {
            Identifier pid = new Identifier();
            pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());

            boolean response = callAdapter.setReplicationStatus(null,pid, new NodeReference(),
                    ReplicationStatus.COMPLETED, new ServiceFailure("0000","a test exception"));

            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationStatus should fail when bogus nodeReference passed in");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidRequest e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationStatus - tests with a fake identifier")
    @WebTestDescription("this tests a negative case in which setReplicationStatus is called with "
            + "an invalid identifier and expect an InvalidRequest exception")
    public void testSetReplicationStatus_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationStatus_NotFound(nodeIterator.next(), version);
    }
    
    /**
     * Test the that the membernode can set the replication status for on an object
     */
//    @Ignore("test not implemented")
    public void testSetReplicationStatus_NotFound(Node node, String version) {
        
        //TODO: implement a memberNode test subject
//      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

        try {
            Identifier pid = new Identifier();
            pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());

            boolean response = callAdapter.setReplicationStatus(null,pid, new NodeReference(),
                    ReplicationStatus.FAILED, new ServiceFailure("0000","a test exception"));

            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationStatus should fail when fictitious pid passed in");
        } 
        catch (NotFound e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationStatus - tests for a valid response")
    @WebTestDescription("tests that calling setReplicationStatus with valid parameters "
            + "returns a boolean \"true\" response and throws no exceptions")
    public void testSetReplicationPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationPolicy(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testSetReplicationPolicy(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testAdmin"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
            
            BigInteger serialVersion = callAdapter.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();

            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setNumberReplicas(4);
            
            boolean response = callAdapter.setReplicationPolicy(null,oi.getIdentifier(),
                    policy, serialVersion.longValue());
            checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationPolicy - test with invalid certificate")
    @WebTestDescription("tests a negative case in which setReplicationPolicy is called "
            + "with a certificate whose subject doesn't have the rights to change "
            + "the replication policy - expects a NotAuthorizedException")
    public void testSetReplicationPolicy_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationPolicy_NotAuthorized(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testSetReplicationPolicy_NotAuthorized(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
            
            BigInteger serialVersion = callAdapter.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();

            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setNumberReplicas(4);
            
            boolean response = callAdapter.setReplicationPolicy(null,oi.getIdentifier(),
                    policy, serialVersion.longValue());
            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationPolicy should fail when using no-right client subject");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotAuthorized e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationPolicy - test with fake identifier")
    @WebTestDescription("tests a negative case in which setReplicationPolicy is called "
            + "with a fake identifier and expects a NotFound exception")
    public void testSetReplicationPolicy_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationPolicy_NotFound(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testSetReplicationPolicy_NotFound(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testAdmin"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

        try {
            Identifier pid = new Identifier();
            pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());
            
            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setNumberReplicas(4);
            
            boolean response = callAdapter.setReplicationPolicy(null,pid,
                    policy, 100);
            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationPolicy should fail when passing in fictitious pid");
        } 
        
        catch (NotFound e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationPolicy - test with incorrect system metadata version")
    @WebTestDescription("tests a negative case in which setReplicationPolicy is called "
            + "with the incorrect version number passed for the system metadata, expecting "
            + "a VersionMismatch exception")
    public void testSetReplicationPolicy_VersionMismatch(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationPolicy_VersionMismatch(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testSetReplicationPolicy_VersionMismatch(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testAdmin"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
            
            BigInteger serialVersion = callAdapter.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();
            
            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setNumberReplicas(4);

            boolean response = callAdapter.setReplicationPolicy(null,oi.getIdentifier(),
                    policy, serialVersion.longValue()+10);
            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationPolicy should fail when setting a bogus serial version of the sysmeta");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (VersionMismatch e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with VersionMismatch. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setReplicationPolicy - test with incorrect number of replicas")
    @WebTestDescription("tests a negative case in which setReplicationPolicy is called "
            + "with -1 set for the number of replicas on the ReplicationPolicy, expecting "
            + "an InvalidRequest exception")
    public void testSetReplicationPolicy_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetReplicationPolicy_InvalidRequest(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testSetReplicationPolicy_InvalidRequest(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testAdmin"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
            
            long serialVersion = callAdapter.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion().longValue();

            ReplicationPolicy policy = new ReplicationPolicy();
            policy.setNumberReplicas(-1);
            
            boolean response = callAdapter.setReplicationPolicy(null,oi.getIdentifier(),
                    policy, serialVersion);
            handleFail(callAdapter.getLatestRequestUrl(),"setReplicationPolicy should fail when setting number of replicas to -1");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidRequest e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("isNodeAuthorized - tests with invalid identification token")
    @WebTestDescription(" ... test not yet implemented ...?")
    public void testIsNodeAuthorized_InvalidToken(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testIsNodeAuthorized_InvalidToken(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented")
    public void testIsNodeAuthorized_InvalidToken(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testAdmin"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());

            // TODO: should not be current identity, but Subject of a listed member node
            Subject subject = ClientIdentityManager.getCurrentIdentity();
            
            boolean response = callAdapter.isNodeAuthorized(null, subject, oi.getIdentifier());
            
//              checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
            handleFail(callAdapter.getLatestRequestUrl(),"isNodeAuthorized should fail when using no-rights client subject");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidToken e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with InvalidToken. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("isNodeAuthorized - tests with an unauthorized client subject")
    @WebTestDescription(" ... test not yet implemented ...?"
            + "tests a negative case in which isNodeAuthorized is called "
            + "with a client subject that is not authorized")
    public void testIsNodeAuthorized_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testIsNodeAuthorized_NotAuthorized(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented")
    public void testIsNodeAuthorized_NotAuthorized(Node node, String version) {
        
        ContextAwareTestCaseDataone.setupClientSubject("testSubmitter");
        Subject noRightsSubject = ClientIdentityManager.getCurrentIdentity();
        // TODO: 
//      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
        
            boolean response = callAdapter.isNodeAuthorized(null, noRightsSubject, oi.getIdentifier());
            
//              checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
            handleFail(callAdapter.getLatestRequestUrl(),"isNodeAuthorized should fail when using no-rights client subject");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotAuthorized e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("isNodeAuthorized - tests with no subject")
    @WebTestDescription("tests a negative case in which isNodeAuthorized is called "
            + "with a null Subject, expecting an InvalidRequest exception")
    public void testIsNodeAuthorized_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testIsNodeAuthorized_InvalidRequest(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented")
    public void testIsNodeAuthorized_InvalidRequest(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

        try {
            ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
            log.debug("   pid = " + oi.getIdentifier());
            
            // passing in a null value for subject
            boolean response = callAdapter.isNodeAuthorized(null, null, oi.getIdentifier());
            
            handleFail(callAdapter.getLatestRequestUrl(),"isNodeAuthorized should fail when passing in null subject (omitting subject)");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidRequest e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("isNodeAuthorized - tests a fake identifier")
    @WebTestDescription("tests a negative case in which isNodeAuthorized is called "
            + "with an identifier that doesn't exist, expecting a NotFound exception")
    public void testIsNodeAuthorized_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testIsNodeAuthorized_NotFound(nodeIterator.next(), version);
    }
    
//    @Ignore("test not implemented") 
    public void testIsNodeAuthorized_NotFound(Node node, String version) {
        // TODO: 
//      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
        String currentUrl = node.getBaseURL();
        
        printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

        try {
            Identifier pid = new Identifier();
            pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());
            
            Subject subject = ClientIdentityManager.getCurrentIdentity();
            
            // passing in a null value for subject
            boolean response = callAdapter.isNodeAuthorized(null, subject, pid);
            
            handleFail(callAdapter.getLatestRequestUrl(),"isNodeAuthorized should fail when passing in fictitious pid");
        }
        catch (NotFound e) {
            // the expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
                    ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

//  NOTE: Commenting out the below Ignored tests because they now have bad listObject calls.  The parameter
//  replicaStatus is not universal anymore, and these tests rely on replicaStatus of false.
//    
//    @WebTestName("updateReplicationMetadata - tests with copy of current replica")
//    @WebTestDescription("test makes a call to updateReplicationMetadata using a copy "
//            + "of the current replica")
//    public void testUpdateReplicationMetadata(Iterator<Node> nodeIterator, String version) {
//        while (nodeIterator.hasNext())
//            testUpdateReplicationMetadata(nodeIterator.next(), version);
//    }
//    
////    @Ignore("test not implemented")
//    public void testUpdateReplicationMetadata(Node node, String version) {
//        //TODO:
////      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
//        String currentUrl = node.getBaseURL();
//        
//        printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//        try {
//            // want to get an object already replicated
//            // will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
//            List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
//            List<ObjectInfo> unreplicatedObjects = callAdapter.listObjects(null, null, null, null, false, null, null).getObjectInfoList();
//
//            allObjects.removeAll(unreplicatedObjects);
//            Identifier replicatedObject = allObjects.get(0).getIdentifier();                
//            log.debug("   pid = " + replicatedObject);
//            
//            SystemMetadata smd = callAdapter.getSystemMetadata(null, replicatedObject);
//            long serialVersion = smd.getSerialVersion().longValue();
//            Replica replica = smd.getReplica(0);
//            
//            // try an update to the replica by replacing it with itself... (no changes)
//            boolean response = callAdapter.updateReplicationMetadata(null, replicatedObject, replica, serialVersion);
//
//            checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//        }
//        catch (IndexOutOfBoundsException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
//        }
//         
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
//    }
//    
//    @WebTestName("updateReplicationMetadata - tests using a no-rights subject")
//    @WebTestDescription("test makes a call to updateReplicationMetadata using a copy "
//            + "of the current replica but with a subject that is not authorized, "
//            + "expecting a NotAuthorized exception")
//    public void testUpdateReplicationMetadata_NotAuthorized(Iterator<Node> nodeIterator, String version) {
//        while (nodeIterator.hasNext())
//            testUpdateReplicationMetadata_NotAuthorized(nodeIterator.next(), version);
//    }
//    
////    @Ignore("test not implemented") 
//    public void testUpdateReplicationMetadata_NotAuthorized(Node node, String version) {
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testNoRights"), node, version);
//        String currentUrl = node.getBaseURL();
//        
//        printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//        try {
//            // want to get an object already replicated
//            // will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
//            List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
//            List<ObjectInfo> unreplicatedObjects = callAdapter.listObjects(null, null, null, null, false, null, null).getObjectInfoList();
//
//            allObjects.removeAll(unreplicatedObjects);
//            Identifier replicatedObject = allObjects.get(0).getIdentifier();                
//            log.debug("   pid = " + replicatedObject);
//            
//            
//            SystemMetadata smd = callAdapter.getSystemMetadata(null, replicatedObject);
//            long serialVersion = smd.getSerialVersion().longValue();
//            Replica replica = smd.getReplica(0);
//            
//            // try an update to the replica by replacing it with itself... (no changes)
//            boolean response = callAdapter.updateReplicationMetadata(null, replicatedObject, replica, serialVersion);
//
//            handleFail(callAdapter.getLatestRequestUrl(),"updateReplicaMetadata should fail when using no-rights subject");
//        }
//        catch (IndexOutOfBoundsException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
//        }
//        catch (NotAuthorized e) {
//            // the expected outcome
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
//                    ":: " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
//    }
//    
//    @WebTestName("updateReplicationMetadata - ")
//    @WebTestDescription(" ... not implemented ... "
//            + "seems to be a copy of testUpdateReplicationMetadata_NotAuthorized "
//            + "instead of testing a NotFound exception")
//    public void testUpdateReplicationMetadata_NotFound(Iterator<Node> nodeIterator, String version) {
//        while (nodeIterator.hasNext())
//            testUpdateReplicationMetadata_NotFound(nodeIterator.next(), version);
//    }
//    
////    @Ignore("test not implemented") 
//    public void testUpdateReplicationMetadata_NotFound(Node node, String version) {
//        //TODO:
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
//        String currentUrl = node.getBaseURL();
//        
//        printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//        try {
//            // want to get an object already replicated
//            // will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
//            List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
//            List<ObjectInfo> unreplicatedObjects = callAdapter.listObjects(null, null, null, null, false, null, null).getObjectInfoList();
//
//            allObjects.removeAll(unreplicatedObjects);
//            Identifier replicatedObject = allObjects.get(0).getIdentifier();                
//            log.debug("   pid = " + replicatedObject);
//
//            Identifier badPid = new Identifier();
//            badPid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());
//            
//            SystemMetadata smd = callAdapter.getSystemMetadata(null, replicatedObject);
//            long serialVersion = smd.getSerialVersion().longValue();
//            Replica replica = smd.getReplica(0);
//            
//            // try an update to the replica by replacing it with itself... (no changes)
//            boolean response = callAdapter.updateReplicationMetadata(null, badPid, replica, serialVersion);
//
//            handleFail(callAdapter.getLatestRequestUrl(),"updateReplicaMetadata should fail when using no-rights subject");
//        }
//        catch (IndexOutOfBoundsException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
//        }
//        catch (NotFound e) {
//            // the expected outcome
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
//                    ":: " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
//    }
//    
//    @WebTestName("updateReplicationMetadata - tests with a null replica")
//    @WebTestDescription("tests a negative case, calling updateReplicationMetadata with "
//            + "a null value for the replica, expecting an InvalidRequest exception")
//    public void testUpdateReplicationMetadata_InvalidRequest(Iterator<Node> nodeIterator, String version) {
//        while (nodeIterator.hasNext())
//            testUpdateReplicationMetadata_InvalidRequest(nodeIterator.next(), version);
//    }
//    
////    @Ignore("test not implemented") 
//    public void testUpdateReplicationMetadata_InvalidRequest(Node node, String version) {
//        //TODO:
////      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
//        String currentUrl = node.getBaseURL();
//        
//        printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//        try {
//            // want to get an object already replicated
//            // will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
//            List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
//            List<ObjectInfo> unreplicatedObjects = callAdapter.listObjects(null, null, null, null, false, null, null).getObjectInfoList();
//
//            allObjects.removeAll(unreplicatedObjects);
//            Identifier replicatedObject = allObjects.get(0).getIdentifier();                
//            log.debug("   pid = " + replicatedObject);
//
//            
//            SystemMetadata smd = callAdapter.getSystemMetadata(null, replicatedObject);
//            long serialVersion = smd.getSerialVersion().longValue();
//            
//            // try an update to the replica by replacing it with a null value Replica
//            boolean response = callAdapter.updateReplicationMetadata(null, replicatedObject, null, serialVersion);
//
//            handleFail(callAdapter.getLatestRequestUrl(),"updateReplicaMetadata should fail when using a null Replica");
//        }
//        catch (IndexOutOfBoundsException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
//        }
//        catch (InvalidRequest e) {
//            // the expected outcome
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
//                    ":: " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
//    }
//    
//    @WebTestName("updateReplicationMetadata - tests with an bad serialVersion")
//    @WebTestDescription("tests a negative case, calling updateReplicationMetadata with "
//            + "a serialVersion number that's incorrect, expecting a VersionMismatch exception")
//    public void testUpdateReplicationMetadata_VersionMismatch(Iterator<Node> nodeIterator, String version) {
//        while (nodeIterator.hasNext())
//            testUpdateReplicationMetadata_VersionMismatch(nodeIterator.next(), version);
//    }
//    
////    @Ignore("test not implemented")
//    public void testUpdateReplicationMetadata_VersionMismatch(Node node, String version) {
//        //TODO:
////      ContextAwareTestCaseDataone.setupClientSubject("testMemberNode");
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testMemberNode"), node, version);
//        String currentUrl = node.getBaseURL();
//        
//        printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//        try {
//            // want to get an object already replicated
//            // will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
//            List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
//            List<ObjectInfo> unreplicatedObjects = callAdapter.listObjects(null, null, null, null, false, null, null).getObjectInfoList();
//
//            allObjects.removeAll(unreplicatedObjects);
//            Identifier replicatedObject = allObjects.get(0).getIdentifier();                
//            log.debug("   pid = " + replicatedObject);
//
//            
//            SystemMetadata smd = callAdapter.getSystemMetadata(null, replicatedObject);
//            long serialVersion = smd.getSerialVersion().longValue();
//            Replica replica = smd.getReplica(0);
//            
//            // try an update to the replica by replacing it with itself... (no changes)
//            boolean response = callAdapter.updateReplicationMetadata(null, replicatedObject, replica, serialVersion + 10);
//
//            handleFail(callAdapter.getLatestRequestUrl(),"updateReplicaMetadata should fail when passing in a bad serialVersion");
//        }
//        catch (IndexOutOfBoundsException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
//        }
//        catch (VersionMismatch e) {
//            // the expected outcome
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),"expected fail with VersionMismatch. Got: " + e.getClass() + 
//                    ":: " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
//    }
}
