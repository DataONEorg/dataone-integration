package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.util.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ViewFunctionalTestImplementations extends ContextAwareAdapter {

    private long METACAT_INDEXING_TIME = 10000;
    List<Node> cns;
    
    public ViewFunctionalTestImplementations(ContextAwareTestCaseDataone catc, Iterator<Node> cNodes) {
        super(catc);
        
        cns = IteratorUtils.toList(cNodes);
    }
    
    @WebTestName("view - tests if the view call returns an html document for science metadata")
    @WebTestDescription("this test calls view() with the 'default' theme and the pid of a science "
            + "metadata object, then verifies that it returns an html document")
    public void testView_Scimeta(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_Scimeta(nodeIterator.next(), version);        
    }
    
    private void testView_Scimeta(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testView_Scimeta_" + ExampleUtilities.generateIdentifier());
        AccessRule accessRule = new AccessRule();
        accessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        accessRule.addPermission(Permission.READ);
        
        try {
            pid = catc.createTestObject(callAdapter, pid, accessRule);
        } catch (Exception e) {
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to create a test object for testView_Scimeta functional test "
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage(), e);
        }
        
        InputStream is = null;
        try {
            is = callAdapter.view(null, "default", pid);
        
            org.jsoup.nodes.Document doc = null;
            try {
                doc = Jsoup.parse(is, null, "");
                
            } catch (Exception e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream "
                        + "that contains a valid HTML Document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
            
            Element htmlRoot = doc.select(":root").first();
            if (htmlRoot == null)
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() did not return an HTML document with a header node for the default theme "
                        + "and pid: " + pid.getValue() + "" );
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testView_Scimeta functional test: " 
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @WebTestName("view - tests if the view call returns an html document for a resource map")
    @WebTestDescription("this test calls view() with the 'default' theme and the pid of a resource "
            + "map object, then verifies that it returns an html document")
    public void testView_ResMap(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_ResMap(nodeIterator.next(), version);        
    }
    
    private void testView_ResMap(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        Identifier pid = null;
        try {
            pid = catc.procureResourceMap(callAdapter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to create a test object for testView_ResMap functional test: " 
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        InputStream is = null;
        try {
            is = callAdapter.view(null, "default", pid);
        
            org.jsoup.nodes.Document doc = null;
            try {
                doc = Jsoup.parse(is, null, "");
                
            } catch (Exception e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream"
                        + "that can be parsed into a document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            Element htmlRoot = doc.select(":root").first();
            if (htmlRoot == null)
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() either did not return an HTML document, or did not return "
                        + "an HTML document with a header, for the default theme "
                        + "and pid: " + pid.getValue() );
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to run testView_ResMap functional test: " 
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("listViews - tests if the listViews call returns valid themes, including 'default'")
    @WebTestDescription("this test calls listViews() and verifies that it returns a valid list of themes "
            + "including the required 'default' theme")
    public void testListViews(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListViews(nodeIterator.next(), version);   
    }
    
    private void testListViews(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        try {
            OptionList views = callAdapter.listViews(null);
        
            boolean foundDefault = false;
            for (String viewType : views.getOptionList())
                if (viewType.equals("default")) {
                    foundDefault = true;
                    break;
                }
            
            assertTrue("testListViews() should return at least a \"default\" view option type", 
                    foundDefault);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to run testListViews functional test: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    @WebTestName("listViews / view - tests if the listViews call returns themes that are supported by the node")
    @WebTestDescription("this test calls listViews() and verifies that each of the returned themes does not throw "
            + "an exception when used with view()")
    public void testListViewsExist(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListViewsExist(nodeIterator.next(), version);   
    }
    
    private void testListViewsExist(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        try {
            OptionList views = callAdapter.listViews(null);
        
            for (String viewType : views.getOptionList()) {

                InputStream is = null;
                try {
                    AccessRule accessRule = new AccessRule();
                    Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
                    accessRule.addSubject(subject);
                    accessRule.addPermission(Permission.CHANGE_PERMISSION);
                    AccessPolicy policy = new AccessPolicy();
                    policy.addAllow(accessRule);
                    
                    Identifier pid = D1TypeBuilder.buildIdentifier("testListViewsExist_" + ExampleUtilities.generateIdentifier()); 
                    pid = catc.createTestObject(callAdapter, pid, policy, cnSubmitter, Constants.SUBJECT_PUBLIC);
                    
                    Thread.sleep(METACAT_INDEXING_TIME);
                    
                    is = callAdapter.view(null, viewType, pid);
                    
                } catch (Exception e1) {
                    handleFail(callAdapter.getNodeBaseServiceUrl(), 
                            "listViews() returned a theme \"" + viewType 
                            + "\", which does not seem to be supported. Yielded exception: "  
                            + e1.getMessage() + " : " + e1.getCause() == null ? "" : e1.getCause().getMessage());
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to run testListViewsExist functional test: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
}
