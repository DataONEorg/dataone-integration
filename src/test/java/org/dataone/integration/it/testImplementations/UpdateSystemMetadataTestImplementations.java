package org.dataone.integration.it.testImplementations;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;

public abstract class UpdateSystemMetadataTestImplementations extends ContextAwareAdapter {

    public UpdateSystemMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    /**
     * Returns a {@link CommonCallAdapter} authorized to create objects on the node.
     * This will be a client for the MN test subclass and a CN for the CN test subclass.
     */
    abstract protected CommonCallAdapter getCallAdapter(Node node, String version);

    @WebTestName("updateSystemMetadata - tests if the call fails with an unauthorized certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with the \"testPerson\" certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testUpdateSystemMetadata_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_NotAuthorized(Node node, String version) {
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter personCallAdapter = new CommonCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCertCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            personCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(personCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for a connection with unauthorized certificate");
        } 
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with system metadata containing no identifier")
    @WebTestDescription("this test calls updateSystemMetadata() with invalid system metadata "
            + "(because the identifier is empty) and expects an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_NoPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_NoPid(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_NoPid(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_NoPid(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_NoPid_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setIdentifier(D1TypeBuilder.buildIdentifier(""));
            sysmeta.setDateSysMetadataModified(new Date());
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with system metadata containing no serialVersion")
    @WebTestDescription("this test calls updateSystemMetadata() with invalid system metadata "
            + "(because the serialVersion is null) and expects an InvalidSystemMetadata exception to be thrown")
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(null);
            sysmeta.setDateSysMetadataModified(new Date());
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidSystemMetadata exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if the pid and system metadata don't match")
    @WebTestDescription("this test calls updateSystemMetadata() with a pid and system matadata whose identifier "
            + "doesn't match, expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_PidMismatch(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_PidMismatch(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_PidMismatch" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            Identifier diffPid = new Identifier();
            diffPid.setValue("bogus pid");
            sysmeta.setIdentifier(diffPid);
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if the system metadata was unchanged")
    @WebTestDescription("this test calls updateSystemMetadata() with system matadata identical to the "
            + "existing metadata, expecting an InvalidSystemMetadata exception to be thrown")
    public void testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for unchanged metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidSystemMetadata exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the identifier")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(identifier), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified pid
            Identifier modifiedPid = D1TypeBuilder.buildIdentifier(testObjPid.getValue() + "_MODIFIED");
            sysmeta.setIdentifier(modifiedPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for unchanged metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            // TODO we may actually get a NotFound instead
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the size")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(size), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedSize(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSize(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSize" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified pid
            
            sysmeta.setSize(BigInteger.ONE);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify size field in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            // TODO we may actually get a NotFound instead
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the checksum")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(checksum), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified checksum
            Checksum checksum = new Checksum();
            checksum.setValue("bogusChecksum");
            checksum.setAlgorithm("md5");
            sysmeta.setChecksum(checksum);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify checksum in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the submitter")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(submitter), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified submitter
            Subject submitter = new Subject();
            submitter.setValue("bogusSubject");
            sysmeta.setSubmitter(submitter);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the submitter in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the dateUploaded")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(dateUploaded), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified dateUploaded
            sysmeta.setDateUploaded(new Date());
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the dateUploaded in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the originMemberNode")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(originMemberNode), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified originMemberNode
            sysmeta.setOriginMemberNode(D1TypeBuilder.buildNodeReference("bogusNode"));
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the originMemberNode in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to set the originMemberNode to null")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(originMemberNode) and set to null, expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_NullOriginMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_NullOriginMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_NullOriginMN(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_NullOriginMN(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_NullOriginMN" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified originMemberNode
            sysmeta.setOriginMemberNode(null);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to set the originMemberNode to null in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if trying to modify the seriesId")
    @WebTestDescription("this test calls updateSystemMetadata() with one of the unmodifiable fields modified "
            + "(seriesId), expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(Node node, String version) {
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.createTestObject(callAdapter, pid, accessRule);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified seriesId
            Identifier seriesId = new Identifier();
            seriesId.setValue("bogusSeriesId" + ExampleUtilities.generateIdentifier());
            sysmeta.setSeriesId(seriesId);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            try {
                callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            } catch (Exception e) {
                handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata with a new seriesId should "
                        + "succeed the first time. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
            
            // try to updateSystemMetadata with the modified seriesId a second time
            seriesId = new Identifier();
            seriesId.setValue("bogusSeriesId" + ExampleUtilities.generateIdentifier());
            sysmeta.setSeriesId(seriesId);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            
            callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the seriesId in system metadata a second time");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with a non-existent pid")
    @WebTestDescription("this test calls updateSystemMetadata() with a pid that does not exist "
            + "and expects a NotFound exception to be thrown")
    public void testUpdateSystemMetadata_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_NotFound(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_NotFound(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("bogus pid");
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            
            callAdapter.updateSystemMetadata(null, pid , sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for bogus pid");
        } 
        catch (NotFound e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected a NotFound exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotFound exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
}
