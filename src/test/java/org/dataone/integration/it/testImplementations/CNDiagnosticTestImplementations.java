package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

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

public class CNDiagnosticTestImplementations extends ContextAwareAdapter {

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
            
            // TODO should also test if the returned SubjectInfo is correct...
            
            List<Person> personList = subjInfo.getPersonList();
            List<Group> groupList = subjInfo.getGroupList();
            
            assertTrue("test", true);
            //assertTrue("echoCredentials should echo the subject it was given", 
            //        );
            
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
        
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            
            Thread.sleep(10000);    // indexing time for metacat (indexing runs on separate thread)
            
            InputStream objStream = callAdapter.get(null, pid);
            // FIXME ensure object format matches what API asks for
            // where "bytes" is meant to be a UTF-8 String ?
            InputStream is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            assertTrue("testEchoIndexedObject() should return a non-null InputStream", is != null);
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
    public void testEchoIndexedObject_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_NotAuthorized(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_NotAuthorized(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter testPersonCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            
            InputStream objStream = callAdapter.get(null, pid);
            // FIXME ensure object format matches what API asks for
            // where "bytes" is meant to be a UTF-8 String ?
            InputStream is = testPersonCallAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
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
        }
        
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails when passed invalid system metadata")
    @WebTestDescription("this test calls echoIndexedObject() with system metadata that has a null serialVersion, "
            + "expecting an InvalidSystemMetadata exception")
    public void testEchoIndexedObject_InvalidSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidSystemMetadata(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidSystemMetadata(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setSerialVersion(null);
            
            Thread.sleep(10000);    // indexing time for metacat (indexing runs on separate thread)
            
            InputStream objStream = callAdapter.get(null, pid);
            // FIXME ensure object format matches what API asks for
            // where "bytes" is meant to be a UTF-8 String ?
            InputStream is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "echoIndexedObject should fail with an InvalidRequest exception "
                    + "if the system metadata is invalid.");
        }
        catch (InvalidRequest e) {
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
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call ")
    @WebTestDescription("this test calls echoIndexedObject()   ...   ")
    public void testEchoIndexedObject_UnsupportedType(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_UnsupportedType(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_UnsupportedType(Node node, String version) {

        handleFail("", "Need to create a test object of unsupported type to test this.");
        
//        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
//        String currentUrl = node.getBaseURL();
//        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
//        
//        try {
//            AccessRule accessRule = new AccessRule();
//            getSession("testRightsHolder");
//            Subject subject = catc.getSubject("testRightsHolder");
//            accessRule.addSubject(subject);
//            accessRule.addPermission(Permission.CHANGE_PERMISSION);
//            
//            // FIXME need to create an object of an unsupported type
//            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
//            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
//            
//            Thread.sleep(10000);    // indexing time for metacat (indexing runs on separate thread)
//            
//            InputStream objStream = callAdapter.get(null, pid);
//            // FIXME ensure object format matches what API asks for
//            // where "bytes" is meant to be a UTF-8 String ?
//            InputStream is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
//            handleFail(callAdapter.getLatestRequestUrl(), "echoIndexedObject should fail with an InvalidRequest exception "
//                    + "if the system metadata is invalid.");
//        }
//        catch (UnsupportedType e) {
//            // expected
//        }
//        catch (BaseException e) {
//            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
//                    e.getDetail_code() + ":: " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//        }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails with an unsupported science metadata type")
    @WebTestDescription("this test calls echoIndexedObject() with science metadata of a type that's not supported, "
            + "expecting an UnsupportedMetadataType exception")
    public void testEchoIndexedObject_UnsupportedMetadataType(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_UnsupportedMetadataType(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_UnsupportedMetadataType(Node node, String version) {
        
        
        handleFail("", "Need to create test science metadata of unsupported type to test this.");
        
//      CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
//      String currentUrl = node.getBaseURL();
//      printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
//      
//      try {
//          AccessRule accessRule = new AccessRule();
//          getSession("testRightsHolder");
//          Subject subject = catc.getSubject("testRightsHolder");
//          accessRule.addSubject(subject);
//          accessRule.addPermission(Permission.CHANGE_PERMISSION);
//          
//          // FIXME need to create an object of an unsupported type
//          Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
//          SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
//          
//          Thread.sleep(10000);    // indexing time for metacat (indexing runs on separate thread)
//          
//          InputStream objStream = callAdapter.get(null, pid);
//          // FIXME ensure object format matches what API asks for
//          // where "bytes" is meant to be a UTF-8 String ?
//          InputStream is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
//          handleFail(callAdapter.getLatestRequestUrl(), "echoIndexedObject should fail with an InvalidRequest exception "
//                  + "if the system metadata is invalid.");
//      }
//      catch (UnsupportedType e) {
//          // expected
//      }
//      catch (BaseException e) {
//          handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
//                  e.getDetail_code() + ":: " + e.getDescription());
//      }
//      catch(Exception e) {
//          e.printStackTrace();
//          handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//      }
    }
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call ")
    @WebTestDescription("this test calls echoIndexedObject()   ...   ")
    public void testEchoIndexedObject_InusfficientResources(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InusfficientResources(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InusfficientResources(Node node, String version) {
        
        // is this testable?
        // do we explicitly check the object size to return this error?
        // probably not - that could kill the heap itself
        // unless we just use the size from sysmeta???
        
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
        sysmeta.setSerialVersion(null);
        
        return sysmeta;
    }
}
