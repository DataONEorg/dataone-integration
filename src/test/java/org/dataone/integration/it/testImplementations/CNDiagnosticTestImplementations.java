package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

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
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoCredentials(...) vs. node: " + currentUrl);
        
        try {
            SubjectInfo subjInfo = callAdapter.echoCredentials(null);
            assertTrue("echoCredentials should echo a non-null SubjectInfo", 
                    subjInfo != null);
            
            // TODO test if the returned SubjectInfo is correct...
            
//            assertTrue("echoCredentials should echo the subject it was given", 
//                    );
            
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
          Subject subject = catc.getSubject("testRightsHolder");
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
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = catc.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoSystemMetadata_NotAuthorized", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            
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
    @WebTestDescription("this test calls echoSystemMetadata() with system metadata containing a null "
            + "serialVersion and expects an InvalidSystemMetadata exception to be thrown")
    public void testEchoSystemMetadata_InvalidSysMeta(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidSysMeta(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidSysMeta(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter testPersonCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoSystemMetadata_NotAuthorized(...) vs. node: " + currentUrl);
        
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = catc.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoSystemMetadata_NotAuthorized", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setSerialVersion(null);   
            
            testPersonCallAdapter.echoSystemMetadata(null, sysmeta);
            handleFail(testPersonCallAdapter.getLatestRequestUrl(), "testEchoSystemMetadata_InvalidSysMeta() should throw a "
                    + "InvalidSystemMetadata with a null serialVersion in the system metadata.");
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
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call ")
    @WebTestDescription("this test calls echoSystemMetadata()   ...   ")
    public void testEchoSystemMetadata_IdentifierNotUnique(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_IdentifierNotUnique(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_IdentifierNotUnique(Node node, String version) {

            handleFail("", "Nooooo idea what this is supposed to test...");
            
//            what causes this?? 
//            are we assuming the pid/sid in the sysmeta is supposed to exist already?
    }
    
    @WebTestName("echoSystemMetadata - tests if the echoSystemMetadata call ")
    @WebTestDescription("this test calls echoSystemMetadata()   ...   ")
    public void testEchoSystemMetadata_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoSystemMetadata_InvalidRequest(nodeIterator.next(), version);        
    }

    private void testEchoSystemMetadata_InvalidRequest(Node node, String version) {

        handleFail("", "Nooooo idea what this is supposed to test...");

        //      what causes this?? 
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
            Subject subject = catc.getSubject("testRightsHolder");
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
            Subject subject = catc.getSubject("testRightsHolder");
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
    
    @WebTestName("echoIndexedObject - tests if the echoIndexedObject call fails when there is a format mismatch")
    @WebTestDescription("this test calls echoIndexedObject() with system metadata that has a formatId which doesn't"
            + "match the actual format of the object, "
            + "expecting an InvalidSystemMetadata exception")
    public void testEchoIndexedObject_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoIndexedObject_InvalidRequest(nodeIterator.next(), version);        
    }

    private void testEchoIndexedObject_InvalidRequest(Node node, String version) {
    
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoIndexedObject(...) vs. node: " + currentUrl);
        
        try {
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = catc.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            Identifier pid = catc.createTestObject(callAdapter, "testEchoIndexedObject", accessRule);
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
            sysmeta.setFormatId(D1TypeBuilder.buildFormatIdentifier("application/pdf"));
            
            Thread.sleep(10000);    // indexing time for metacat (indexing runs on separate thread)
            
            InputStream objStream = callAdapter.get(null, pid);
            // FIXME ensure object format matches what API asks for
            // where "bytes" is meant to be a UTF-8 String ?
            InputStream is = callAdapter.echoIndexedObject(null, "solr", sysmeta, objStream);
            handleFail(callAdapter.getLatestRequestUrl(), "echoIndexedObject should fail with an InvalidRequest exception "
                    + "if the system metadata's format doesn't match the actual format.");
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
            Subject subject = catc.getSubject("testRightsHolder");
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
    
}
