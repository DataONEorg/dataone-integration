package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.SystemMetadata;

public class CNUpdateSystemMetadataTestImplementations extends UpdateSystemMetadataTestImplementations {

    public CNUpdateSystemMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    protected CNCallAdapter getCallAdapter(Node node, String version) {
        return new CNCallAdapter(getSession(cnSubmitter), node, version);
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with any MN certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with an MN certificate subject "
            + "and expects a NotAuthorized exception to be thrown since this call is CN-to-CN only")
    public void testUpdateSystemMetadata_NotAuthorizedMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorizedMN(nodeIterator.next(), version);
    }
    
    // @Ignore'd in test class for now
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
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call fails with a rights holder certificate")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the metadata, "
            + "checks that the call fails since this should only work for a CN")
    public void testUpdateSystemMetadata_NotAuthorized_RightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized_RightsHolder(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_NotAuthorized_RightsHolder(Node node, String version) {
        
        CommonCallAdapter cnCallAdapter = getCallAdapter(node, version);
        CommonCallAdapter rightsHolderCallAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_RightsHolder(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_RightsHolder_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCallAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for a connection with non-CN certificate");
        } catch (NotAuthorized e) {
            // expected outcome 
        } catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works with a CN calling")
    @WebTestDescription("this test calls updateSystemMetadata() using a certificate with "
            + "a coordinating node subject to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata_CN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_CN(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_CN(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_CN_(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule(cnSubmitter, Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_CN_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            sysmeta.setSerialVersion(newSerialVersion);
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            Thread.sleep(5000);
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean dateModifiedChanged = fetchedSysmeta.getDateSysMetadataModified().after(sysmeta.getDateSysMetadataModified());
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedChanged );
        } 
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
}
