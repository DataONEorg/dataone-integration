package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;

public class MNUpdateSystemMetadataTestImplementations extends UpdateSystemMetadataTestImplementations {

    private CNCallAdapter cn;
    
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
//            sysmeta.setDateSysMetadataModified(new Date());
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            getSession("testPerson");
            sysmeta.setRightsHolder(catc.getSubject("testPerson"));
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier("image/png"));
            
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            
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
    
    // TODO can updateSystemMetadata change the following?
    // obsoletes/obsoletedBy (as opposed to update()? changing individually can mess chain up badly)
    // archived (as opposed to archive()?)
    
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
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setDateSysMetadataModified(new Date());
            
            NodeList nodes = cn.listNodes();
            Node diffMN = null;
            for (Node n : nodes.getNodeList()) {
                if (n.getType() == NodeType.MN && !n.getIdentifier().getValue().equals(node.getIdentifier().getValue()));
                   diffMN = n;
            }
            assertTrue("Environment should have at least one other MN so we can test "
                    + "changing the authoritativeMemberNode.", diffMN != null);
             
            callAdapter = getCallAdapter(diffMN, version);
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            
            handleFail(currentUrl, "testUpdateSystemMetadata_RightsHolderNonAuthMN: "
                    + "call to MN.updateSystemMetadata() should fail if made to an MN "
                    + "that is not the authoritative MN.");
            
        } catch (InvalidRequest e) {
            // should fail
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
}
