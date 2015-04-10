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
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
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
            personCallAdapter.updateSystemMetadata(null, pid , sysmeta);
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
    
    @WebTestName("updateSystemMetadata - tests if the call fails with invalid system metadata")
    @WebTestDescription("this test calls updateSystemMetadata() with invalid system metadata "
            + "and expects an InvalidSystemMetadata exception to be thrown")
    public void testUpdateSystemMetadata_InvalidSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata(Node node, String version) {
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCertCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(null);
            sysmeta.setIdentifier(null);
            sysmeta.setDateSysMetadataModified(new Date());
            
            rightsHolderCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidSystemMetadata exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works")
    @WebTestDescription("this test calls updateSystemMetadata() to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated serialVersion and "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata(Node node, String version) {
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCertCallAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = rightsHolderCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        } 
        catch (BaseException e) {
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }  
    }
    
    // TODO test more negative cases:
    //      Exceptions.InvalidRequest   - what causes this exception ???
    //      Exceptions.InvalidToken     - what causes this exception ???
    
}
