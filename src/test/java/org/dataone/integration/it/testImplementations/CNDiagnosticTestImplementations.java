package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CNDiagnosticTestImplementations extends ContextAwareAdapter {

    /** wait time between creating/getting an object - since metacat indexing on separate thread */
    private static final int METACAT_WAIT = 5000;
    
    public CNDiagnosticTestImplementations(ContextAwareTestCaseDataone catc) {
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
            assertTrue("echoCredentials() with the person certificate "
                    + "should return SubjectInfo containing the correct subjects", 
                    personSubjectFound && personSubject1Found &&
                    personSubject2Found && personSubject3Found);
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

    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with an unauthorized"
            + "certificate subject")
    @WebTestDescription("this test calls echoSystemMetadata() and verifies that a "
            + "NotAuthorized exception is thrown")
    public void testEchoSystemMetadata_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_NotAuthorized(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_NotAuthorized(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter testPersonCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_NotAuthorized(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta");
            testPersonCallAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(testPersonCallAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_NotAuthorized() should throw a "
                    + "NotAuthorized with an unauthorized certificate subject.");
        }
        catch (NotAuthorized e) {
            // expected outcome
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

    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a negative "
            + "serialVersion and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_SerialVer(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_SerialVer(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_SerialVer(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_SerialVer(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_SerialVer");
            BigInteger negativeNum = new BigInteger("-1");
            sysmeta.setSerialVersion(negativeNum);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_SerialVer() should throw a "
                    + "InvalidSystemMetadata with a negative serialVersion in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an empty "
            + "formatId and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_FormatId(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_FormatId(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_FormatId(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_FormatId(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_FormatId");
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_FormatId() should throw a "
                    + "InvalidSystemMetadata with an empty formatId in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "pid and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoPid(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_NoPid(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoPid(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoPid");
            sysmeta.setIdentifier(null);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoPid() should throw a "
                    + "InvalidSystemMetadata with a null pid in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an empty "
            + "pid and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_EmptyPid(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_EmptyPid(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_EmptyPid(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_EmptyPid");
            sysmeta.setIdentifier(D1TypeBuilder.buildIdentifier(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_EmptyPid() should throw a "
                    + "InvalidSystemMetadata with an empty pid in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a "
            + "pid with whitespace and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_BadPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_BadPid(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_BadPid(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_BadPid(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_BadPid");
            sysmeta.setIdentifier(D1TypeBuilder.buildIdentifier("a b c d " + System.currentTimeMillis()));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_BadPid() should throw a "
                    + "InvalidSystemMetadata with a pid containing whitespace in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "size and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoSize(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoSize(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_NoSize(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoSize(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoSize");
            sysmeta.setSize(null);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoSize() should throw a "
                    + "InvalidSystemMetadata with a null size in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "checksum and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoChecksum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoChecksum(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_NoChecksum(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoChecksum(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoChecksum");
            sysmeta.setSerialVersion(null);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoChecksum() should throw a "
                    + "InvalidSystemMetadata with a null checksum in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "submitter and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoSubmitter");
            sysmeta.setSubmitter(null);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoSubmitter() should throw a "
                    + "InvalidSystemMetadata with a null submitter in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an empty "
            + "submitter and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter");
            sysmeta.setSubmitter(D1TypeBuilder.buildSubject(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter() should throw a "
                    + "InvalidSystemMetadata with an empty submitter in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "rightsHolder and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder");
            sysmeta.setRightsHolder(null);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder() should throw a "
                    + "InvalidSystemMetadata with a null rightsHolder in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an empty "
            + "rightsHolder and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder");
            sysmeta.setRightsHolder(D1TypeBuilder.buildSubject(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder() should throw a "
                    + "InvalidSystemMetadata with an empty rightsHolder in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an invalid "
            + "accessPolicy (containing no rules) and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_AccessPolicy");
            sysmeta.setAccessPolicy(new AccessPolicy());
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_AccessPolicy() should throw a "
                    + "InvalidSystemMetadata with an invalid accessPolicy (containing no rules) in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an invalid "
            + "replicationPolicy (containing no numberReplicas) and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_ReplNum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_ReplNum(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_ReplNum(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_ReplNum(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_ReplNum");
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            sysmeta.setReplicationPolicy(replPolicy);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_ReplNum() should throw a "
                    + "InvalidSystemMetadata with an invalid replicationPolicy (containing no numberReplicas) "
                    + "in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an invalid "
            + "replicationPolicy (containing no replicationAllowed) and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_ReplAllow(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_ReplAllow(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta_ReplAllow(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_ReplAllow(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_ReplAllow");
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setReplicationAllowed(null);
            sysmeta.setReplicationPolicy(replPolicy);
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_ReplAllow() should throw a "
                    + "InvalidSystemMetadata with an invalid replicationPolicy (containing no replicationAllowed) "
                    + "in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an invalid "
            + "originMemberNode (empty string) and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(nodeIterator.next(), version);        
    }
    
    private void testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoOriginMN");
            sysmeta.setOriginMemberNode(D1TypeBuilder.buildNodeReference(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoOriginMN() should throw a "
                    + "InvalidSystemMetadata with an invalid originMemberNode (empty string) "
                    + "in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call fails with invalid system metadata sent")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing an invalid "
            + "authoritativeMemberNode (empty string) and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(nodeIterator.next(), version);        
    }
    
    private void testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(...) vs. node: " + currentUrl);
        
        try {
            SystemMetadata sysmeta = createTestSysmeta(callAdapter, "testEchoSystemMetadata_InvalidSysMeta_NoAuthMN");
            sysmeta.setAuthoritativeMemberNode(D1TypeBuilder.buildNodeReference(""));
            callAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta_NoAuthMN() should throw a "
                    + "InvalidSystemMetadata with an invalid authoritativeMemberNode (empty string) "
                    + "in the system metadata.");
        }
        catch (InvalidSystemMetadata e) {
            // expected outcome
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



    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call with a non-unique pid")
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata that has "
            + "a pid which already exists in the system")
    public void testEchoSystemMetadata_IdentifierNotUnique(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_IdentifierNotUnique(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_IdentifierNotUnique(Node node, String version) {

            CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
            CNCallAdapter testPersonCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
            String currentUrl = node.getBaseURL();
            printTestHeader("testEchoSystemMetadata_IdentifierNotUnique(...) vs. node: " + currentUrl);
            
            try {
                AccessRule accessRule = new AccessRule();
                getSession("testRightsHolder");
                Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
                accessRule.addSubject(subject);
                accessRule.addPermission(Permission.CHANGE_PERMISSION);
                
                Identifier pid = catc.createTestObject(callAdapter, "testEchoSystemMetadata_IdentifierNotUnique", accessRule);
                SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                sysmeta.setSerialVersion(null);   
                
                testPersonCallAdapter.echoSystemMetadata(null, sysmeta);
                handleFail(testPersonCallAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_IdentifierNotUnique() should throw an "
                        + "IdentifierNotUnique if the system metadata's pid already exists.");
            }
            catch (IdentifierNotUnique e) {
                // expected outcome
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
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call ")
    @WebTestDescription("this test calls echoIndexedObject()   ...   ")
    public void testEchoIndexedObject(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        InputStream is = null;
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
            
            InputStream objStream = callAdapter.get(null, pid);
            
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
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call ")
    @WebTestDescription("this test calls echoIndexedObject()   ...   ")
    public void testEchoIndexedObject_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_NotAuthorized(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_NotAuthorized(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter testPersonCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            
            InputStream objStream = callAdapter.get(null, pid);
            is = testPersonCallAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(testPersonCallAdapter.getLatestRequestUrl(), "testEchoIndexedObject_NotAuthorized() should throw a "
                    + "NotAuthorized with an unauthorized certificate subject.");
        }
        catch (NotAuthorized e) {
            // expected outcome
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
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing a null identifier), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoPid(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoPid(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoPid(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoPid", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setIdentifier(null);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoPid should fail with "
                    + "an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(contains no identifier).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty / zero-length identifier), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_EmptyPid(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_EmptyPid(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_EmptyPid(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_EmptyPid", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setIdentifier(D1TypeBuilder.buildIdentifier(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_EmptyPid should fail with "
                    + "an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(contains an empty / zero-length identifier).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(with an identifier containing whitespace), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_BadPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_BadPid(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_BadPid(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_BadPid(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_BadPid", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setIdentifier(D1TypeBuilder.buildIdentifier("a b c d " + System.currentTimeMillis()));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_BadPid "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(with an identifier containing whitespace).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an invalid serialVersion, with a negative value), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_SerialVer(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_SerialVer(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_SerialVer(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_SerialVer(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_SerialVer", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            BigInteger negativeNum = new BigInteger("-1");
            sysmeta.setSerialVersion(negativeNum);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_SerialVer "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an invalid serialVersion, with a negative value).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty formatId), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_FormatId(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_FormatId(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_FormatId(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_FormatId(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_FormatId", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_FormatId "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an empty formatId).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty size field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSize(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoSize(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoSize(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoSize(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoSize", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setSize(null);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoSize "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an empty size field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty checksum field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoChecksum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoChecksum(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoChecksum(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoChecksum(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoChecksum", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setChecksum(null);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoChecksum "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an empty checksum field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing a null submitter field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setSubmitter(null);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing a null submitter field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty submitter field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setSubmitter(D1TypeBuilder.buildSubject(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an empty submitter field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing a null rightsHolder field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setRightsHolder(null);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing a null rightsHolder field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an empty rightsHolder field), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setRightsHolder(D1TypeBuilder.buildSubject(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an empty rightsHolder field).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an invalid accessPolicy), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setAccessPolicy(new AccessPolicy());
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an invalid accessPolicy).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an invalid replicationPolicy, with null numberReplicas), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplNum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_ReplNum(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_ReplNum(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            sysmeta.setReplicationPolicy(replPolicy);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an invalid replicationPolicy, with null numberReplicas).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing an invalid replicationPolicy, with null replicationAllowed), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplAllow(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_ReplAllow(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_ReplAllow(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_ReplAllow(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_ReplAllow", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setReplicationAllowed(null);
            sysmeta.setReplicationPolicy(replPolicy);
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_ReplAllow "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing an invalid replicationPolicy, with null replicationAllowed).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing a null originMemberNode), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setOriginMemberNode(D1TypeBuilder.buildNodeReference(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing a null originMemberNode).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the system metadata is invalid")
    @WebTestDescription("this test calls echoIndexedObject() with an invalid system metadata "
            + "(containing a null authoritativeMemberNode), expecting an InvalidSystemMetadata exception to be thrown")
    public void testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setAuthoritativeMemberNode(D1TypeBuilder.buildNodeReference(""));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN "
                    + "should fail with an InvalidSystemMetadata exception if the sysmeta given is invalid "
                    + "(containing a null authoritativeMemberNode).");
        }
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails if the formatId is not supported")
    @WebTestDescription("this test calls echoIndexedObject() with an unsupported ObjectFormatIdentifier in the system metadata "
            + "(an identifier that does not exist in the object format list at /formats), "
            + "expecting an UnsupportedType exception to be thrown")
    public void testEchoIndexedObject_UnsupportedType(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_UnsupportedType(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_UnsupportedType(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_UnsupportedType(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_UnsupportedType", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier("blarg/blarg"));
            
            objStream = callAdapter.get(null, pid);
            // FIXME ensure object format matches what API asks for
            // where "bytes" is meant to be a UTF-8 String ?
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_UnsupportedType should fail with an UnsupportedType exception "
                    + "if the formatId in the system metadata given is not of a supported type.");
        }
        catch (UnsupportedType e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails when given a data format")
    @WebTestDescription("this test calls echoIndexedObject() with a formatId that's neither metadata or a resource, "
            + "but is a data type, expecting an UnsupportedMetadataType exception")
    public void testEchoIndexedObject_UnsupportedMetadataType(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_UnsupportedMetadataType(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_UnsupportedMetadataType(Node node, String version) {
        
        handleFail("", "Need to create a test object of unsupported type to test this.");

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject_UnsupportedMetadataType(...) vs. node: " + currentUrl);
        
        InputStream objStream = null;
        InputStream echoStream = null;
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject_UnsupportedMetadataType", accessRule);
            Thread.sleep(METACAT_WAIT);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier("text/xml"));
            
            objStream = callAdapter.get(null, pid);
            echoStream = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "testEchoIndexedObject_UnsupportedMetadataType should fail "
                    + "with an UnsupportedMetadataType exception "
                    + "if the formatId in the system metadata given is a data format type.");
        }
        catch (UnsupportedType e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        } 
        finally {
            IOUtils.closeQuietly(objStream);
            IOUtils.closeQuietly(echoStream);
        }
    }
    
    private SystemMetadata createTestSysmeta(CNCallAdapter callAdapter, String pidBase) 
            throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, 
            NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(accessRule);
        
        Identifier pid = D1TypeBuilder.buildIdentifier(pidBase + ExampleUtilities.generateIdentifier() );
        SystemMetadata sysmeta = catc.createTestSysmeta(callAdapter, pid, null, null, null, policy, cnSubmitter, "CN=testRightsHolder,DC=dataone,DC=org");
        
        return sysmeta;
    }
}
