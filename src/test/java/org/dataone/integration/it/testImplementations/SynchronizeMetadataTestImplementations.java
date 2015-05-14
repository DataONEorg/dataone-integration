package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;

public class SynchronizeMetadataTestImplementations extends ContextAwareAdapter {

    public SynchronizeMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("synchronize - tests if the call fails when made with a non-authorized certificate")
    @WebTestDescription("this test calls synchronize() with an unauthorized certificate, "
            + "expecting a NotAuthorized exception")
    public void testSynchronize_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testSynchronize_NotAuthorized(Node node, String version) {
        
        assertTrue("Test should be @Ignore'd. No MN cert to test with", false);
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter rightsHolderCallAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronize_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronize_NotAuthorized" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            rightsHolderCallAdapter.synchronize(null, testObjPid);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "synchronize call should fail for non-authorized certificate");
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
    
    @WebTestName("synchronize - tests if the call fails when made by a non-authoritative MN")
    @WebTestDescription("this test calls synchronize() with an MN certificate, but not the authoritative MN "
            + "for the object")
    public void testSynchronize_NotAuthorized_MN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_NotAuthorized_MN(nodeIterator.next(), version);
    }
    
    public void testSynchronize_NotAuthorized_MN(Node node, String version) {
        
        assertTrue("Test should be @Ignore'd. No MN cert to test with", false);
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter mnCertCallAdapter = new CNCallAdapter(getSession("mnCertSubject"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronize_NotAuthorized_MN(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronize_NotAuthorized_MN" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            mnCertCallAdapter.synchronize(null, testObjPid);
            handleFail(mnCertCallAdapter.getLatestRequestUrl(), "synchronize call should fail for non-authoritative MN certificate");
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
    
    @WebTestName("synchronize - tests if the call fails when an invalid authentication token is passed")
    @WebTestDescription("this test calls synchronize() with an invalid authentication token ")
    public void testSynchronize_InvalidToken(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_InvalidToken(nodeIterator.next(), version);
    }
    
    public void testSynchronize_InvalidToken(Node node, String version) {
        
        assertTrue("Not yet implemented.", false);
        
    }
    
}
