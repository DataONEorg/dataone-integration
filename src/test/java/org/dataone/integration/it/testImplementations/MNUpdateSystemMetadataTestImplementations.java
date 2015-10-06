package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
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
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;

public class MNUpdateSystemMetadataTestImplementations extends UpdateSystemMetadataTestImplementations {

    private CNCallAdapter cn;
    private static final long METACAT_INDEXING_WAIT = 10000;
    private static final long REPLICATION_WAIT = 10 * 60000; 
    
    public MNUpdateSystemMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    protected MNCallAdapter getCallAdapter(Node node, String version) {
        return new MNCallAdapter(getSession("testRightsHolder"), node, version);
    }
    
    public void setup(Node cNode) {
        cn = new CNCallAdapter(getSession(cnSubmitter), cNode, "v2");
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with an unauthorized MN certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with a non-authoritative MN certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testUpdateSystemMetadata_NotAuthorizedMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorizedMN(nodeIterator.next(), version);
    }
    
    // @Ignore'd in test class: No MN test certificates to test with at the moment
    public void testUpdateSystemMetadata_NotAuthorizedMN(Node node, String version) {
        
        assertTrue("Should be @Ignore'd", false);
        
//        String mnSubject = no MN test certs to use;
//        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
//        CommonCallAdapter mnCertCallAdapter = new CommonCallAdapter(getSession(mnSubject), node, version);
//        String currentUrl = node.getBaseURL();
//        printTestHeader("testUpdateSystemMetadata_NotAuthorizedMN(...) vs. node: " + currentUrl);
//        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
//        
//        try {
//            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
//            Identifier pid = new Identifier();
//            pid.setValue("testUpdateSystemMetadata_NotAuthorizedMN_" + ExampleUtilities.generateIdentifier());
//            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
//            
//            SystemMetadata sysmeta = cnCertCallAdapter.getSystemMetadata(null, testObjPid);
//            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
//            mnCertCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
//            handleFail(mnCertCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for a connection with unauthorized certificate");
//        } 
//        catch (NotAuthorized e) {
//            // expected
//        }
//        catch (BaseException e) {
//            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
//                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
//                    ": " + e.getMessage());
//        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works for a rights holder")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated serialVersion and "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata_RightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_RightsHolder(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_RightsHolder(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_RightsHolder(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue(); 
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_RightsHolder_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date originalDate = sysmeta.getDateSysMetadataModified();
            sysmeta.setSerialVersion(newSerialVersion);
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().after(originalDate);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change mutable fields with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the rightsHolder field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableRightsHolder(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableRightsHolder(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableRightsHolder(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableRightsHolder_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            getSession("testPerson");
            sysmeta.setRightsHolder(catc.getSubject("testPerson"));
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean rightsHolderMatches = fetchedSysmeta.getRightsHolder().getValue().equals(sysmeta.getRightsHolder().getValue());
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated rightsHolder", rightsHolderMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change mutable fields with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the formatId field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableFormat(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableFormat(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableFormat(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableFormat(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableFormat_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier("image/png"));
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean formatIdMatches = fetchedSysmeta.getFormatId().getValue().equals("image/png");
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated formatId", formatIdMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change mutable fields with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the accessPolicy field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableAccessPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableAccessPolicy(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableAccessPolicy(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableAccessPolicy(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = ContextAwareTestCaseDataone.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableAccessPolicy_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            
            AccessPolicy accessPolicy = new AccessPolicy();
            getSession("testGroup");
            String groupSubjStr = ContextAwareTestCaseDataone.getSubject("testGroup").getValue();
            accessRule = APITestUtils.buildAccessRule(groupSubjStr, Permission.CHANGE_PERMISSION);
            accessPolicy.addAllow(accessRule);
            sysmeta.setAccessPolicy(accessPolicy);
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            List<AccessRule> allowList = fetchedSysmeta.getAccessPolicy().getAllowList();

            assertTrue("System metadata should have an access policy with one item in its allow list.", allowList.size() == 1);
            AccessRule ar0 = allowList.get(0);
            String subjStr = ar0.getSubject(0).getValue();
            Permission permission = ar0.getPermission(0);
            boolean accessPolicyMatches = permission == Permission.CHANGE_PERMISSION
                    && subjStr.equals(groupSubjStr);
            
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated accessPolicy", accessPolicyMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change mutable fields with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the replicationPolicy field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableReplPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableReplPolicy(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableReplPolicy(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableReplPolicy(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableReplPolicy_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);

            ReplicationPolicy replicationPolicy = new ReplicationPolicy();
            replicationPolicy.setNumberReplicas(42);
            replicationPolicy.setReplicationAllowed(false);
            sysmeta.setReplicationPolicy(replicationPolicy);
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            ReplicationPolicy fetchedReplPolicy = fetchedSysmeta.getReplicationPolicy();
            Integer numberReplicas = fetchedReplPolicy.getNumberReplicas();
            Boolean replicationAllowed = fetchedReplPolicy.getReplicationAllowed();
            boolean replicationPolicyMatches = replicationAllowed == false && numberReplicas == 42;
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated replicationPolicy", replicationPolicyMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change mutable fields with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the authoritativeMemberNode field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableAuthMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableAuthMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableAuthMN(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableAuthMN(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableAuthMN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            
            NodeList nodes = cn.listNodes();
            Node diffMN = null;
            for (Node n : nodes.getNodeList()) {
                if (n.getType() == NodeType.MN && !n.getIdentifier().getValue().equals(node.getIdentifier().getValue()));
                   diffMN = n;
            }
            assertTrue("Environment should have at least one other MN so we can test "
                    + "changing the authoritativeMemberNode.", diffMN != null);
             
            sysmeta.setAuthoritativeMemberNode(diffMN.getIdentifier());
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean authMNMatches = fetchedSysmeta.getAuthoritativeMemberNode().getValue().equals(diffMN.getIdentifier().getValue());
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated authoritativeMemberNode", authMNMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can change the archived field with updateSystemMetadata")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It modifies the archived field in the system metadata when making the "
            + "getSystemMetadata() call and verifies that no exception is thrown "
            + "(that the mutable field can be modified by the rights holder). ")
    public void testUpdateSystemMetadata_MutableArchived(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableArchived(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableArchived(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableArchived(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableArchived_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setArchived(true);
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean archivedMatches = fetchedSysmeta.getArchived().equals(true);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated archived", archivedMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder cannot updateSystemMetadata on non-authoritative MN")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to try to update the system metadata, "
            + "but it makes the call on a non-authoritative MN, and is expected to fail "
            + "with an InvalidRequest exception. ")
    public void testUpdateSystemMetadata_RightsHolderNonAuthMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_RightsHolderNonAuthMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_RightsHolderNonAuthMN(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableAuthMN(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_MutableAuthMN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.getSerialVersion().add(BigInteger.ONE);
            
            NodeList nodes = cn.listNodes();
            Node diffMN = null;
            for (Node n : nodes.getNodeList()) {
                if (n.getType() == NodeType.MN && !n.getIdentifier().getValue().equals(node.getIdentifier().getValue()));
                   diffMN = n;
            }
            assertTrue("Environment should have at least one other MN "
                    + "(fetched through CN.getNodeList()) so we can test "
                    + "changing the authoritativeMemberNode.", diffMN != null);
             
            callAdapter = getCallAdapter(diffMN, version);
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            
            handleFail(currentUrl, "testUpdateSystemMetadata_RightsHolderNonAuthMN: "
                    + "call to MN.updateSystemMetadata() should fail if made to an MN "
                    + "that is not the authoritative MN.");
            
        } catch (InvalidRequest e) {
            // should fail
        } catch (NotAuthorized e) {
            // should fail
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            log.warn("MN.updateSystemMetadata() returned a ServiceFailure, "
                    + "was expecting an InvalidRequest or NotAuthorized.");
        } catch (SynchronizationFailed e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
               "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that a CN certificate can updateSystemMetadata on a non-authoritative MN")
    @WebTestDescription("this test calls updateSystemMetadata() using a CN certificate "
            + "to try to update the system metadata, "
            + "but it makes the call on a non-authoritative MN, and is expected to succeed ")
    public void testUpdateSystemMetadata_CNCertNonAuthMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_CNCertNonAuthMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_CNCertNonAuthMN(Node node, String version) {
        
        MNCallAdapter cnCertAuthMN = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter cnCertNonAuthMN = null;
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_CNCertNonAuthMN(...) vs. node: " + currentUrl);
        currentUrl = cnCertAuthMN.getNodeBaseServiceUrl();
        
        Identifier testObjPid = null;
        SystemMetadata sysmeta = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_CNCertNonAuthMN_" + ExampleUtilities.generateIdentifier());
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setReplicationAllowed(true);
            replPolicy.setNumberReplicas(2);
            testObjPid = catc.createTestObject(cnCertAuthMN, pid, accessRule, replPolicy);
            
            Thread.sleep(REPLICATION_WAIT);
            
        } catch (BaseException e) {
            throw new AssertionError("Test setup failed. Couldn't create object: " + cnCertAuthMN.getLatestRequestUrl() + " : " + e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError("Test setup failed. Couldn't create object: " + cnCertAuthMN.getLatestRequestUrl() + " : " + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            sysmeta = cn.getSystemMetadata(null, testObjPid);
            sysmeta.getSerialVersion().add(BigInteger.ONE);
        } catch (BaseException e) {
            throw new AssertionError("Test setup failed. Couldn't fetch object from CN: " + cn.getLatestRequestUrl() + " : " + e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError("Test setup failed. Couldn't fetch object from CN: " + cn.getLatestRequestUrl() + " : " + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            List<Replica> replicaList = sysmeta.getReplicaList();
            assertTrue("testUpdateSystemMetadata_CNCertNonAuthMN : After waiting for replication to occur, "
                    + "fetched sysmeta should contain a non-empty replica list!", 
                    replicaList != null && replicaList.size() > 0);
            
            List<Replica> successfulReplicas = new ArrayList<Replica>();
            String replicaStr = "";
            for (Replica replica : replicaList) {
                replicaStr += replica.getReplicaMemberNode().getValue() + ", ";
                if (replica.getReplicationStatus() == ReplicationStatus.COMPLETED 
                        && !replica.getReplicaMemberNode().getValue().equals(node.getIdentifier().getValue()))
                    successfulReplicas.add(replica);
            }
            
            assertTrue("testUpdateSystemMetadata_CNCertNonAuthMN : After waiting for replication to occur, "
                    + "there should be at least one successful replica MN available! "
                    + "Replicas: " + replicaStr, successfulReplicas.size() > 0);
            
            Node nonAuthMN = null;
            outerloop:
            for (Replica replica : successfulReplicas) {
                NodeReference replicaMN = replica.getReplicaMemberNode();
                
                NodeList nodes = cn.listNodes();
                for (Node n : nodes.getNodeList()) {
                    if (n.getType() == NodeType.MN
                            && n.getIdentifier().getValue().equals(replicaMN.getValue())
                            && n.getState() == NodeState.UP) {
                        nonAuthMN = n;
                        break outerloop;
                    }
                }
            }
            assertTrue("Environment should have at least one other MN that is up and "
                            + "was used to replicate to.", nonAuthMN != null);
 
            cnCertNonAuthMN = new MNCallAdapter(getSession(cnSubmitter), nonAuthMN, version);
            boolean success = cnCertNonAuthMN.updateSystemMetadata(null, testObjPid , sysmeta);
            
            assertTrue("testUpdateSystemMetadata_CNCertNonAuthMN: "
                    + "should succeed with a CN cert making the updateSystemMetadata() call "
                    + "to a non-authoritative MN.", success);
            
        } catch (BaseException e) {
            handleFail(cnCertNonAuthMN.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(cnCertNonAuthMN.getLatestRequestUrl(), e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can't change the obsoletes field if it causes an inconsistency")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It sets up a 2-pid chain (in which they both point to each other) "
            + "and modifies the obsoletes field in the system metadata "
            + "of the first one and verifies that an exception is thrown - "
            + "since this would create an inconsistency in the chain.")
    public void testUpdateSystemMetadata_ObsoletesFail(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_ObsoletesFail(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_ObsoletesFail(Node node, String version) {
        
        MNCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_ObsoletesFail(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        SystemMetadata v2sysmeta = null;
        Identifier p1 = createPid("testUpdateSystemMetadata_ObsoletesFail_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = createPid("testUpdateSystemMetadata_ObsoletesFail_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = createPid("testUpdateSystemMetadata_ObsoletesFail_" + ExampleUtilities.generateIdentifier());
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            p1 = catc.createTestObject(callAdapter, p1, accessRule);
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(ExampleUtilities.FORMAT_EML_2_0_1);
            ByteArrayInputStream objectInputStream = new ByteArrayInputStream(contentBytes);
            D1Object d1o = new D1Object(p2, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(ExampleUtilities.FORMAT_EML_2_0_1),
                    catc.getSubject("testRightsHolder"),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            
            v2sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            v2sysmeta.setAuthoritativeMemberNode(node.getIdentifier());
            p2 = d1o.getIdentifier();
            
            callAdapter.update(null, p1, objectInputStream, p2, v2sysmeta);
            p3 = catc.createTestObject(callAdapter, p3, accessRule);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            v2sysmeta = callAdapter.getSystemMetadata(null, p2);
        } catch (BaseException e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_ObsoletesFail() setup steps failed! " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_ObsoletesFail() setup steps failed! "  
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            v2sysmeta.setObsoletes(p3);
            callAdapter.updateSystemMetadata(null, p2 , v2sysmeta);
            
            handleFail(currentUrl, "testUpdateSystemMetadata_ObsoletesFail() expects the "
                    + "updateSystemMetadata() call to fail with an InvalidRequest exception"
                    + "since it's trying to overwrite the existing obsoletes field,"
                    + "but the call succeeded! (The obsoletes field can only be set once.)");
            
        } catch (InvalidRequest e) {
            // expected
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), 
                    "testUpdateSystemMetadata_ObsoletesFail() expected InvalidRequest but got: " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            handleFail(currentUrl, "testUpdateSystemMetadata_ObsoletesFail() expected InvalidRequest but got: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can't change the obsoletedBy field if it causes an inconsistency")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It sets up a 2-pid chain (in which they both point to each other) "
            + "and modifies the obsoletedBy field in the system metadata "
            + "of the second pid and verifies that an exception is thrown - "
            + "since this would create an inconsistency in the chain.")
    public void testUpdateSystemMetadata_ObsoletedByFail(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_ObsoletedByFail(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_ObsoletedByFail(Node node, String version) {
        
        MNCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_ObsoletedByFail(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        SystemMetadata v2sysmeta = null;
        Identifier p1 = createPid("testUpdateSystemMetadata_ObsoletedByFail_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = createPid("testUpdateSystemMetadata_ObsoletedByFail_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = createPid("testUpdateSystemMetadata_ObsoletedByFail_" + ExampleUtilities.generateIdentifier());
        SystemMetadata p1sysmeta = null;
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            p1 = catc.createTestObject(callAdapter, p1, accessRule);
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(ExampleUtilities.FORMAT_EML_2_0_1);
            ByteArrayInputStream objectInputStream = new ByteArrayInputStream(contentBytes);
            D1Object d1o = new D1Object(p2, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(ExampleUtilities.FORMAT_EML_2_0_1),
                    catc.getSubject("testRightsHolder"),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            
            v2sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            p2 = d1o.getIdentifier();
            callAdapter.update(null, p1, objectInputStream, p2, v2sysmeta);
            p3 = catc.createTestObject(callAdapter, p3, accessRule);
            
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            p1sysmeta = callAdapter.getSystemMetadata(null, p1);
        } catch (BaseException e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_ObsoletedByFail() setup steps failed! " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_ObsoletedByFail() setup steps failed! "  
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            p1sysmeta.setObsoletedBy(p3);
            callAdapter.updateSystemMetadata(null, p1 , p1sysmeta);
            
            handleFail(currentUrl, "testUpdateSystemMetadata_ObsoletedByFail() expects the "
                    + "updateSystemMetadata() call to fail with an InvalidSystemMetadata exception"
                    + "since it's trying to overwrite the existing obsoletedBy field, "
                    + "but the call succeeded! (The obsoletes field can only be set once.)");
            
        } catch (InvalidRequest e) {
            // expected
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), 
                    "testUpdateSystemMetadata_ObsoletedByFail() expected InvalidRequest but got: " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            handleFail(currentUrl, "testUpdateSystemMetadata_ObsoletedByFail() expected InvalidRequest but got: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can't change the obsoletes field if it causes an inconsistency")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It creates two objects (not related to each other through obsoletes / obsoletedBy) "
            + "and modifies the obsoletedBy field in the system metadata "
            + "of the first one (to point to the second) and verifies that no exception is thrown.")
    public void testUpdateSystemMetadata_MutableObsoletedBy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableObsoletedBy(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableObsoletedBy(Node node, String version) {
        
        MNCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableObsoletedBy(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        SystemMetadata p1sysmeta = null;
        Identifier p1 = createPid("testUpdateSystemMetadata_MutableObsoletedBy_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = createPid("testUpdateSystemMetadata_MutableObsoletedBy_" + ExampleUtilities.generateIdentifier());
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            p1 = catc.createTestObject(callAdapter, p1, accessRule);
            p2 = catc.createTestObject(callAdapter, p2, accessRule);
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            p1sysmeta = callAdapter.getSystemMetadata(null, p1);
        } catch (BaseException e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_MutableObsoletedBy() setup steps failed! " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_MutableObsoletedBy() setup steps failed! "  
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            p1sysmeta.setObsoletedBy(p2);
            p1sysmeta.setSerialVersion(p1sysmeta.getSerialVersion().add(BigInteger.ONE));
            callAdapter.updateSystemMetadata(null, p1 , p1sysmeta);
            
        } catch (InvalidSystemMetadata e) {
            // expected
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), 
                    "testUpdateSystemMetadata_MutableObsoletedBy() expected InvalidSystemMetadata but got: " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            handleFail(currentUrl, "testUpdateSystemMetadata_MutableObsoletedBy() expected InvalidSystemMetadata but got: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the rights holder can't change the obsoletes field if it causes an inconsistency")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the system metadata. "
            + "It creates two objects (not related to each other through obsoletes / obsoletedBy) "
            + "and modifies the obsoletes field in the system metadata "
            + "of the second one (to point to the first) and verifies that no exception is thrown.")
    public void testUpdateSystemMetadata_MutableObsoletes(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_MutableObsoletes(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_MutableObsoletes(Node node, String version) {
        
        MNCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_MutableObsoletes(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        SystemMetadata p2sysmeta = null;
        Identifier p1 = createPid("testUpdateSystemMetadata_MutableObsoletes_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = createPid("testUpdateSystemMetadata_MutableObsoletes_" + ExampleUtilities.generateIdentifier());
        
        try {
            getSession("testRightsHolder");
            String rightsHolderSubjStr = catc.getSubject("testRightsHolder").getValue();
            AccessRule accessRule = APITestUtils.buildAccessRule(rightsHolderSubjStr, Permission.CHANGE_PERMISSION);
            p1 = catc.createTestObject(callAdapter, p1, accessRule);
            p2 = catc.createTestObject(callAdapter, p2, accessRule);
            Thread.sleep(METACAT_INDEXING_WAIT);
            
            p2sysmeta = callAdapter.getSystemMetadata(null, p2);
        } catch (BaseException e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_MutableObsoletes() setup steps failed! " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            throw new AssertionError(currentUrl + " testUpdateSystemMetadata_MutableObsoletes() setup steps failed! "  
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            p2sysmeta.setObsoletes(p1);
            p2sysmeta.setSerialVersion(p2sysmeta.getSerialVersion().add(BigInteger.ONE));
            callAdapter.updateSystemMetadata(null, p2 , p2sysmeta);
            
        } catch (InvalidSystemMetadata e) {
            // expected
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    "testUpdateSystemMetadata_MutableObsoletes() expected InvalidSystemMetadata but got: " 
                    + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            handleFail(currentUrl, "testUpdateSystemMetadata_MutableObsoletes() expected InvalidSystemMetadata but got: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private Identifier createPid(String string) {
        try {
            Thread.sleep(1);    // millisecond granularity isn't enough - was getting overlapping pids
        } catch (InterruptedException e) {}
        return D1TypeBuilder.buildIdentifier(string);
    }
}
