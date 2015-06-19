package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.OptionList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ViewFunctionalTestImplementations extends ContextAwareAdapter {

    private long METACAT_INDEXING_TIME = 10000;

    public ViewFunctionalTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    public void testView(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView(nodeIterator.next(), version);        
    }
    
    @WebTestName("view - tests if the view call returns an html document for the default theme")
    @WebTestDescription("this test calls view() with the 'default' theme and verifies that it "
            + "returns an html document")
    private void testView(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testView_" + ExampleUtilities.generateIdentifier());
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        try {
            pid = catc.procureTestObject(callAdapter, accessRule, pid);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to create a test object for testView functional test: " 
                    + e.getCause().getMessage());
        }
        
        InputStream is = null;
        try {
            is = callAdapter.view(null, "default", pid);
        
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                handleFail(callAdapter.getLatestRequestUrl(), "view() should return an InputStream"
                        + "that can be parsed into a document. Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String headerExp = "/html";
            org.w3c.dom.Node headerNode = null;
            
            try {
                headerNode = (org.w3c.dom.Node) xPath.compile(headerExp).evaluate(doc, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                handleFail(callAdapter.getLatestRequestUrl(), "view() should return an InputStream"
                        + "that represents an HTML document. Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            if (headerNode == null)
                handleFail(callAdapter.getLatestRequestUrl(), "view() did not return an HTML document.");
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testView functional test: " 
                    + e.getCause().getMessage());
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
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testListViews functional test: " 
                    + e.getMessage() + ", "
                    + e.getCause() == null ? "" : e.getCause().getMessage());
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

                try {
                    AccessRule accessRule = new AccessRule();
                    getSession("testRightsHolder");
                    Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
                    accessRule.addSubject(subject);
                    accessRule.addPermission(Permission.CHANGE_PERMISSION);
                    
                    Identifier pid = catc.createTestObject(callAdapter, "testEchoSystemMetadata", accessRule);
                    
                    Thread.sleep(METACAT_INDEXING_TIME);
                    
                    callAdapter.view(null, viewType, pid);
                    
                } catch (Exception e1) {
                    handleFail(callAdapter.getNodeBaseServiceUrl(), 
                            "listViews() returned a theme \"" + viewType 
                            + "\", which does not seem to be supported. Yielded exception: "  
                            + e1.getMessage() + " : " + e1.getCause() == null ? "" : e1.getCause().getMessage());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testListViews functional test: " 
                    + e.getMessage() + ", "
                    + e.getCause() == null ? "" : e.getCause().getMessage());
        }
    }
}
