package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
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
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

public class ViewTestImplementations extends ContextAwareAdapter {

    public ViewTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }


    @WebTestName("view - tests if the call fails with an unauthorized certificate subject")
    @WebTestDescription("this test calls view() with the \"testPerson\" certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testView_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testView_NotAuthorized(Node node, String version) {
        
        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter personCallAdapter = new CommonCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testView_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
        
        InputStream resultStream = null;
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testView_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
            
            resultStream = personCallAdapter.view(null, "default", testObjPid);
            handleFail(personCallAdapter.getLatestRequestUrl(), "view call should fail for a connection with unauthorized certificate");
        } 
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription()
                    + "from : " + cnCertCallAdapter.getLatestRequestUrl());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(resultStream);
        }
    }
    
    @WebTestName("view - tests if the call fails if given a non-existent theme")
    @WebTestDescription("this test calls view() with a bogus theme "
            + "and expects no exception to be thrown - should use the default theme instead")
    public void testView_InvalidTheme(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_InvalidTheme(nodeIterator.next(), version);
    }

    public void testView_InvalidTheme(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testView_InvalidTheme(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        InputStream resultStream = null;
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testView_InvalidTheme_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            resultStream = callAdapter.view(null, "bogus_theme_yaaaay", testObjPid);
        } 
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "Expected an InvalidRequest exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(resultStream);
        }
    }
    
    @WebTestName("view - tests if the call fails with a non-existent pid")
    @WebTestDescription("this test calls view() with a pid that does not exist "
            + "and expects a NotFound exception to be thrown")
    public void testView_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_NotFound(nodeIterator.next(), version);
    }
    
    public void testView_NotFound(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testView_InvalidSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        InputStream resultStream = null;
        try {
            Identifier pid = D1TypeBuilder.buildIdentifier("bogus pid");
            resultStream = callAdapter.view(null, "default", pid);
            handleFail(callAdapter.getLatestRequestUrl(), "view call should fail for bogus pid");
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
        } finally {
            IOUtils.closeQuietly(resultStream);
        }
    }
    
    @WebTestName("listViews - tests that the listViews call works")
    @WebTestDescription("this test calls listViews and ensures it returns an OptionList")
    public void testListViews(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListViews(nodeIterator.next(), version);
    }

    public void testListViews(Node node, String version) {
        CommonCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListViews(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            OptionList optionList = callAdapter.listViews(null);
            assertTrue("listViews() should return a non-null OptionList", optionList != null);
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
