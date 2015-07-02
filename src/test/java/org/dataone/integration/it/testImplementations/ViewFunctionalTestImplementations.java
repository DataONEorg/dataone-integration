package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.OptionList;
import org.dataone.service.util.Constants;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ViewFunctionalTestImplementations extends ContextAwareAdapter {

    private long METACAT_INDEXING_TIME = 10000;
    List<Node> cns;
    List<Node> mns;
    
    public ViewFunctionalTestImplementations(ContextAwareTestCaseDataone catc, Iterator<Node> cNodes, Iterator<Node> mNodes) {
        super(catc);
        
        cns = IteratorUtils.toList(cNodes);
        mns = IteratorUtils.toList(mNodes);
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
        
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                // output to file for debugging:
//                FileOutputStream fout = new FileOutputStream(new File("C:\\Users\\Andrei\\stuff\\viewScimeta.txt"));
//                IOUtils.copy(is, fout);
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream "
                        + "that contains a valid Document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String headerExp = "/html";
            org.w3c.dom.Node headerNode = null;
            
            try {
                headerNode = (org.w3c.dom.Node) xPath.compile(headerExp).evaluate(doc, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream"
                        + "that represents an HTML document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " + e.getClass().getName() + ": " + e.getMessage(), e);
            }
            
            if (headerNode == null)
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
        
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                // output to file for debugging:
//                FileOutputStream fout = new FileOutputStream(new File("C:\\Users\\Andrei\\stuff\\viewScimeta.txt"));
//                IOUtils.copy(is, fout);
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream"
                        + "that can be parsed into a document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String headerExp = "/html";
            org.w3c.dom.Node headerNode = null;
            
            try {
                headerNode = (org.w3c.dom.Node) xPath.compile(headerExp).evaluate(doc, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() did not return an InputStream "
                        + "that represents an HTML document for the default theme "
                        + "and pid: " + pid.getValue() + ". Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            if (headerNode == null)
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
    
    @WebTestName("view - tests if the view call returns an html document for a data object")
    @WebTestDescription("this test calls locates a data object of type text/csv, then "
            + "calls view() with the 'default' theme and the pid of a data "
            + "object, then verifies that it returns an html document")
    public void testView_Data(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView_Data(nodeIterator.next(), version);
    }
    
    private void testView_Data(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        Identifier pid = null;
        InputStream viewIS = null;
        try {
            List<ObjectInfo> dataFiles = new ArrayList<>();
            
            if (cns != null && cns.size() > 0) {
                CNCallAdapter cn = new CNCallAdapter(getSession("testRightsHolder"), cns.get(0), "v2");
                try {
                    ObjectList objs = cn.search(null, "solr", "?q=formatType:DATA");
                    dataFiles = objs.getObjectInfoList();
                } catch (Exception e) {
                    throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                            + "Unable to find a test object for testView_Data functional test! "
                            + "Call to search() failed on " + callAdapter.getNodeBaseServiceUrl() + " : " 
                            + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
                }
            } else {
                try {
                    ObjectList textDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("text/plain"), null, null, null);
                    ObjectList csvDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("text/csv"), null, null, null);
//                    ObjectList xmlDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("text/xml"), null, null, null);
//                    ObjectList htmlDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("text/html"), null, null, null);
//                    ObjectList bmpDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("image/bmp"), null, null, null);
//                    ObjectList jpegDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("image/jpeg"), null, null, null);
//                    ObjectList gifDataFiles = callAdapter.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("image/gif"), null, null, null);
                    if(textDataFiles.getTotal() > 0)
                        dataFiles.addAll(textDataFiles.getObjectInfoList());
                    if(csvDataFiles.getObjectInfoList().size() > 0)
                        dataFiles.addAll(csvDataFiles.getObjectInfoList());
//                    if(xmlDataFiles.getObjectInfoList().size() > 0)
//                        dataFiles.addAll(xmlDataFiles.getObjectInfoList());
//                    if(htmlDataFiles.getObjectInfoList().size() > 0)
//                        dataFiles.addAll(htmlDataFiles.getObjectInfoList());
//                    if(bmpDataFiles.getObjectInfoList().size() > 0)
//                        dataFiles.addAll(bmpDataFiles.getObjectInfoList());
//                    if(jpegDataFiles.getObjectInfoList().size() > 0)
//                        dataFiles.addAll(jpegDataFiles.getObjectInfoList());
//                    if(gifDataFiles.getObjectInfoList().size() > 0)
//                        dataFiles.addAll(gifDataFiles.getObjectInfoList());
                } catch (Exception e) {
                    throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                            + "Unable to find a test object for testView_Data functional test! "
                            + "Call to listObjects() failed on " + callAdapter.getNodeBaseServiceUrl() + " : " 
                            + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
                }
            }
            assertTrue("testView_Data() needs to be able to locate a \"text/csv\" data file to test with.", 
                    dataFiles.size() > 0);
            
            for (ObjectInfo objInfo : dataFiles) {
                Identifier csvPid = objInfo.getIdentifier();
                try {
                    viewIS = callAdapter.get(null, csvPid);
                    if (viewIS != null) {
                        pid = objInfo.getIdentifier();
                        break;
                    }
                } catch (Exception e) {}
                finally {
                    IOUtils.closeQuietly(viewIS);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to find a test object for testView_Data functional test: " 
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        assertTrue("testView_Data() needs to be able to locate a \"text/csv\" data file to test with; "
                + "found none on the current node.", pid != null);
        
        InputStream is = null;
        try {
            is = callAdapter.view(null, "default", pid);
        
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream"
                        + "that can be parsed into a document. Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String headerExp = "/html";
            org.w3c.dom.Node headerNode = null;
            
            try {
                headerNode = (org.w3c.dom.Node) xPath.compile(headerExp).evaluate(doc, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() should return an InputStream"
                        + "that represents an HTML document. Error: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            if (headerNode == null)
                throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                        + "view() did not return an HTML document.");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to run testView_Data functional test: " 
                    + e.getMessage());
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
            throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + ":   "
                    + "Unable to run testListViewsExist functional test: " 
                    + e.getMessage() + ", "
                    + e.getCause() == null ? "" : e.getCause().getMessage());
        }
    }
}
