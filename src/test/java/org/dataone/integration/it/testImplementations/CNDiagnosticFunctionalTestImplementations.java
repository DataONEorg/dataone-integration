package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CNDiagnosticFunctionalTestImplementations extends ContextAwareAdapter {

    /** wait time between creating/getting an object - since metacat indexing on separate thread */
    private static final int METACAT_WAIT = 5000;
    
    
    public CNDiagnosticFunctionalTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    @WebTestName("echoCredentials - tests if the echoCredentials call succeeds")
    @WebTestDescription("this test calls echoCredentials() and verifies that "
            + "a SubjectInfo is returned, containing the expected values, "
            + "and that no exception is thrown")
    public void testEchoCredentials(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoCredentials(nodeIterator.next(), version);        
    }
    
    private void testEchoCredentials(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoCredentials(...) vs. node: " + currentUrl);
        
        try {
            
            // echo "testRightsHolder" credentials
            
            SubjectInfo subjInfo = callAdapter.echoCredentials(null);
            assertTrue("echoCredentials should echo a non-null SubjectInfo", 
                    subjInfo != null);
            
            boolean correctSubjectFound = false;
            for (Person person : subjInfo.getPersonList()) {
                if (person.getSubject().getValue().equals("DC=org, DC=dataone, CN=testRightsHolder")) {
                    correctSubjectFound = true;
                    break;
                }
            }
            assertTrue("echoCredentials() with the rights-holder certificate "
                    + "should return SubjectInfo containing the correct subject", correctSubjectFound);

            // echo "testPerson" credentials
            
            
            callAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
            subjInfo = callAdapter.echoCredentials(null);
            assertTrue("echoCredentials should echo a non-null SubjectInfo", 
                subjInfo != null);
            
            boolean personSubjectFound = false;
            boolean personSubject1Found = false;
            boolean personSubject2Found = false;
            boolean personSubject3Found = false;
            for (Person person : subjInfo.getPersonList()) {
                if (person.getSubject().getValue().equals("CN=testPerson,DC=dataone,DC=org"))
                    personSubjectFound = true;
                if (person.getSubject().getValue().equals("CN=testEQPerson1,DC=dataone,DC=org"))
                    personSubject1Found = true;
                if (person.getSubject().getValue().equals("CN=testEQPerson2,DC=dataone,DC=org"))
                    personSubject2Found = true;
                if (person.getSubject().getValue().equals("CN=testEQPerson3,DC=dataone,DC=org"))
                    personSubject3Found = true;
            }
            
            assertTrue("echoCredentials() with the testPerson certificate "
                    + "should return SubjectInfo containing the correct subjects", 
                    personSubjectFound && personSubject1Found &&
                    personSubject2Found && personSubject3Found);
            
            boolean correctGroupFound = false;
            for (Group group : subjInfo.getGroupList())
                if (group.getSubject().getValue().equals("CN=testGroup,DC=dataone,DC=org")) {
                    correctGroupFound = true;
                    break;
                }
            
            assertTrue("echoCredentials() with the testPerson certificate "
                    + "should return SubjectInfo containing the correct group subject.", 
                    correctGroupFound);
            
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call succeeds")
    @WebTestDescription("this test calls echoSystemMetadata() and verifies that "
            + "an equivalent SystemMetadata object is sent back, "
            + "and that no exception is thrown")
    public void testEchoSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata(Node node, String version) {
        
      CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
      String currentUrl = node.getBaseURL();
      printTestHeader("testEchoSystemMetadata(...) vs. node: " + currentUrl);
      
      try {
          AccessRule accessRule = new AccessRule();
          getSession("testRightsHolder");
          Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
          accessRule.addSubject(subject);
          accessRule.addPermission(Permission.CHANGE_PERMISSION);
          
          Identifier pid = catc.createTestObject(callAdapter, "testEchoSystemMetadata", accessRule);
          SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
          
          SystemMetadata echoedSysmeta = callAdapter.echoSystemMetadata(null, sysmeta);
          assertTrue("echoSystemMetadata() should send back a valid SystemMetadata object", echoedSysmeta != null);
          assertTrue("echoSystemMetadata() - echoed system metadata identifier should match", sysmeta.getIdentifier().equals(echoedSysmeta.getIdentifier()));
          assertTrue("echoSystemMetadata() - echoed system metadata serialVersion should match", sysmeta.getSerialVersion().equals(echoedSysmeta.getSerialVersion()));
          assertTrue("echoSystemMetadata() - echoed system metadata checksum should match", sysmeta.getChecksum().equals(echoedSysmeta.getChecksum()));
      }
      catch (BaseException e) {
          handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                  e.getDetail_code() + ":: " + e.getDescription());
      }
      catch(Exception e) {
          e.printStackTrace();
          handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
      }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call succeeds")
    @WebTestDescription("this test calls echoIndexedObject() and verifies that a "
            + "valid stream is returned, that it contains the expected values which "
            + "would have been indexed, and that no exceptions are thrown")
    public void testEchoIndexedObject(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        InputStream objStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            assertTrue("Test object should be created succesfully.", pid != null);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            
            objStream = callAdapter.get(null, pid);
            
            //-------------------------
            // prints out the document to System.out:
            
//            Document docO = null;
//            try {
//                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//                docO = builder.parse(new InputSource(objStream));
//                Transformer transformer = TransformerFactory.newInstance().newTransformer();
//                DOMSource source = new DOMSource(docO);
//                StreamResult result = new StreamResult(System.out);
//                transformer.transform(source, result);
//            } catch (Exception e) {
//                handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
//            }
            //-------------------------
            
            is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            assertTrue("testEchoIndexedObject() should return a non-null InputStream", is != null);
            
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(objStream));
            } catch (Exception e) {
                handleFail(currentUrl, "echoIndexedObject() should return document representing the parsed object "
                        + "as it would be prior to being added to a search index. " + e.getClass().getName()
                        + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String abstractExp = "/response/result/doc/str[@name='abstract']";
            String abstractVal = xPath.compile(abstractExp).evaluate(doc);
            assertTrue("returned document should contain the same abstract as the metadata sent", 
                    abstractVal.startsWith("PISCO is a large-scale marine research program"));
            
            String authorGivenNameExp = "/response/result/doc/str[@name='authorGivenName']";
            String authorGivenNameVal = xPath.compile(authorGivenNameExp).evaluate(doc);
            assertTrue("returned document should contain the same authorGivenName as the metadata sent", 
                    authorGivenNameVal.equals("Margaret"));
            
            String authorSurNameExp = "/response/result/doc/str[@name='authorSurName']";
            String authorSurNameVal = xPath.compile(authorSurNameExp).evaluate(doc);
            assertTrue("returned document should contain the same authorSurName as the metadata sent", 
                    authorSurNameVal.equals("McManus"));
            
            String formatIdExpExp = "/response/result/doc/str[@name='formatId']";
            String formatIdValVal = xPath.compile(formatIdExpExp).evaluate(doc);
            assertTrue("returned document should contain the same formatId as the metadata sent", 
                    formatIdValVal.equals("eml://ecoinformatics.org/eml-2.0.1"));
            
            String formatTypeExp = "/response/result/doc/str[@name='formatType']";
            String formatTypeVal = xPath.compile(formatTypeExp).evaluate(doc);
            assertTrue("returned document should contain the same formatType as the metadata sent", 
                    formatTypeVal.equals("METADATA"));
            
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(objStream);
        }
    }
}
