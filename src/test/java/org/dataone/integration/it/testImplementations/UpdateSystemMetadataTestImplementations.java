package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

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

public class UpdateSystemMetadataTestImplementations extends ContextAwareAdapter {

    public UpdateSystemMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

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
    
    @WebTestName("updateSystemMetadata - tests if the call fails with an unauthorized MN certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with a non-authoritative MN certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testUpdateSystemMetadata_NotAuthorizedMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorizedMN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_NotAuthorizedMN(Node node, String version) {
        
//        String mnSubject = no MN test certs to use;
//        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
//        CommonCallAdapter mnCertCallAdapter = new CommonCallAdapter(getSession(mnSubject), node, version);
//        String currentUrl = node.getBaseURL();
//        printTestHeader("testUpdateSystemMetadata_NotAuthorizedMN(...) vs. node: " + currentUrl);
//        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
//        
//        try {
//            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
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
    
    @WebTestName("updateSystemMetadata - tests if the call fails with system metadata containing no identifier")
    @WebTestDescription("this test calls updateSystemMetadata() with invalid system metadata "
            + "(because the identifier is null) and expects an InvalidSystemMetadata exception to be thrown")
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidSystemMetadata_NoPid(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoPid(Node node, String version) {
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setIdentifier(null);
            sysmeta.setDateSysMetadataModified(new Date());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidSystemMetadata exception. Got: " + e.getClass().getName() + 
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
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(null);
            sysmeta.setDateSysMetadataModified(new Date());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata exception. Got: " + 
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
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequestPidMismatch(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequestPidMismatch" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            Identifier diffPid = new Identifier();
            diffPid.setValue("bogus pid");
            sysmeta.setIdentifier(diffPid);
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
            + "existing metadata, expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_SysmetaUnmodified(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_SysmetaUnmodified(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_SysmetaUnmodified(Node node, String version) {
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequestSysmetaUnmodified(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequestSysmetaUnmodified" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for unchanged metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified pid
            testObjPid.setValue(testObjPid.getValue() + "_MODIFIED");
            sysmeta.setIdentifier(testObjPid);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for unchanged metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            // TODO we may actually get a NotFound instead
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSize(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSize" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified pid
            testObjPid.setValue(testObjPid.getValue() + "_MODIFIED");
            sysmeta.setSize(BigInteger.ONE);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify size field in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            // TODO we may actually get a NotFound instead
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified checksum
            Checksum checksum = new Checksum();
            checksum.setValue("bogusChecksum");
            sysmeta.setChecksum(checksum);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify checksum in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified submitter
            Subject submitter = new Subject();
            submitter.setValue("bogusSubject");
            sysmeta.setSubmitter(submitter);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the submitter in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified dateUploaded
            sysmeta.setDateUploaded(new Date());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the dateUploaded in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified originMemberNode
            sysmeta.setOriginMemberNode(node.getIdentifier());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the originMemberNode in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            // try to updateSystemMetadata with the modified seriesId
            Identifier seriesId = new Identifier();
            seriesId.setValue("bogusSeriesId");
            sysmeta.setSeriesId(seriesId);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail "
                    + "if trying to modify the seriesId in system metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
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
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("bogus pid");
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for bogus pid");
        } 
        catch (NotFound e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected a NotFound exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotFound exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
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
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_RightsHolder(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_RightsHolder_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        } 
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works for a CN")
    @WebTestDescription("this test calls updateSystemMetadata() using a certificate with "
            + "a coordinating node subject to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated serialVersion and "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata_CN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_CN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_CN(Node node, String version) {
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_CN_(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule(cnSubmitter, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_CN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        } 
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    
}
