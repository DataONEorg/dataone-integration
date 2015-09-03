package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
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
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;

public class SynchronizeMetadataTestImplementations extends ContextAwareAdapter {

    public SynchronizeMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("synchronize - tests if the call fails when made with a non-authorized CN certificate")
    @WebTestDescription("this test calls synchronize() with a CN certificate (synchronize should only be "
            + "called by the authoritative MN), and expects a NotAuthorized exception")
    public void testSynchronize_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testSynchronize_NotAuthorized(Node node, String version) {
        
        CNCallAdapter cnCertCallAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronize_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronize_NotAuthorized" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            cnCertCallAdapter.synchronize(null, testObjPid);
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "synchronize call should fail for non-authorized certificate");
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
        CNCallAdapter mnCertCallAdapter = new CNCallAdapter(getSession("mnCertSubject - NOT corresponding to node -->"), node, version);
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
    
    @WebTestName("synchronize - tests if the call succeeds when made by an authoritative MN")
    @WebTestDescription("this test calls synchronize() with the authoritative MN certificate")
    public void testSynchronize_AuthorizedMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_AuthorizedMN(nodeIterator.next(), version);
    }
    
    public void testSynchronize_AuthorizedMN(Node node, String version) {
        
        assertTrue("Test should be @Ignore'd. No MN cert to test with", false);
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter mnCertCallAdapter = new CNCallAdapter(getSession("mnCertSubject - corresponding to node -->"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronize_NotAuthorized_MN(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronize_NotAuthorized_MN" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            boolean success = mnCertCallAdapter.synchronize(null, testObjPid);
            assertTrue(mnCertCallAdapter.getLatestRequestUrl() + "synchronize call should succeed for authoritative MN certificate", success);
        } 
        catch (BaseException e) {
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("testSynchronizeObject - tests if the call fails with a bogus pid")
    @WebTestDescription("this test calls testSynchronizeObject() with a bogus pid "
            + "and expects a NotFound exception to be thrown")
    public void testSynchronize_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSynchronize_NotFound(nodeIterator.next(), version);
    }
    
    public void testSynchronize_NotFound(Node node, String version) {
        
        CNCallAdapter cn = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronizeObject_NotFound(...) vs. node: " + currentUrl);
        currentUrl = cn.getNodeBaseServiceUrl();
        
        try {
            Identifier testObjPid = D1TypeBuilder.buildIdentifier("supercalifragilisticexpialidocious");
            cn.synchronize(null, testObjPid);
            
            handleFail(cn.getLatestRequestUrl(), "Expected a NotFound exception.");
        }
        catch (NotFound e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cn.getLatestRequestUrl(), "Expected a NotFound exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotFound exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
}
