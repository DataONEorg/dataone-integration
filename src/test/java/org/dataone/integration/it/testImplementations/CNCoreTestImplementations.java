package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.util.StringUtils;

public class CNCoreTestImplementations extends ContextAwareAdapter {

    private static org.apache.commons.logging.Log log = LogFactory.getLog(CNCoreTestImplementations.class);
    
    private static final String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
    private static final String unicodeIdPrefix = "testCNodeTier1";
    private static Vector<String> unicodeString;
    private static Vector<String> escapedString;
    
    public CNCoreTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    @WebTestName("create: tests that calling create works")
    @WebTestDescription("this test checks that the PID of a created object equals the one given, "
            + "and that it contains the data passed in")
    public void testCreate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreate(nodeIterator.next(), version);
    }
    
//    @Ignore("need a subject able to call cn.create()")
    public void testCreate(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreate() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage("cNodeTier1TestCreate",true);
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            Identifier pid = callAdapter.create(null,(Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2 ); 
            
            checkEquals(callAdapter.getLatestRequestUrl(),"pid of created object should equal that given",
                    ((Identifier)dataPackage[0]).getValue(), pid.getValue());
            
            InputStream theDataObject = callAdapter.get(null,pid);
            String objectData = IOUtils.toString(theDataObject);
            checkTrue(callAdapter.getLatestRequestUrl(),"should get back an object containing submitted text:" + objectData,
                    objectData.contains("Plain text source"));
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    @WebTestName("create - identifier encoding: tests that calling create works for different types of identifier encodings")
    @WebTestDescription("this test checks that calling create can succeed even if the identifier given "
            + "contains a variety of character encodings")
    public void testCreateData_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreateData_IdentifierEncoding(nodeIterator.next(), version);
    }
    
    /**
     * test creation of data with challenging unicode identifier.
     */
//    @Ignore("need a subject able to call cn.create()")
    public void testCreateData_IdentifierEncoding(Node node, String version) 
    {
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreateData_IdentifierEncoding() vs. node: " + currentUrl);     
        setupIdentifierVectors();

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl );

        printTestHeader("  Node:: " + currentUrl);
        
        for (int j=0; j<unicodeString.size(); j++) 
        {
            String status = "OK   ";
            try {
                // String unicode = unicodeString.get(j);
                System.out.println();
                System.out.println(j + "    unicode String:: " + unicodeString.get(j));
                String idString = unicodeIdPrefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
                // String idStringEscaped = unicodeIdPrefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);

                Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage(idString,false);

                // rGuid is either going to be the escaped ID or the non-escaped ID
                Identifier rGuid = null;

                org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
                SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, SystemMetadata.class);
                rGuid = callAdapter.create(null, (Identifier) dataPackage[0], 
                        (InputStream)dataPackage[1], sysMetaV2);
                
                System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
                checkEquals(callAdapter.getLatestRequestUrl(),"guid returned from create should equal that given",
                        ((Identifier)dataPackage[0]).getValue(), rGuid.getValue());
                InputStream data = callAdapter.get(null, rGuid);
                checkTrue(callAdapter.getLatestRequestUrl(), "get against the object should not equal null", null != data);
                String str = IOUtils.toString(data);
                checkTrue(callAdapter.getLatestRequestUrl(),"should be able to read the content as created ('" + str + "')",
                        str.indexOf("Plain text source") != -1);
                data.close();
            }
            catch (BaseException e) {
                status = "Error";
                handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() +
                        ": " + e.getDetail_code() + ": " + e.getDescription());
            }
            catch(Exception e) {
                status = "Error";
                e.printStackTrace();
                handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
            }   
            nodeSummary.add("Test " + j + ": " + status + ":  " + unicodeString.get(j));
        }
        
        System.out.println();
        for (int k=0; k<nodeSummary.size(); k++) 
        {
            log.info(nodeSummary.get(k));
        }
        System.out.println();
    }
    
    private void setupIdentifierVectors() {
        if (unicodeString != null && escapedString != null)
            return;
        
        unicodeString = new Vector<String>();
        escapedString = new Vector<String>();
        InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
        Scanner s = new Scanner(is,"UTF-8");
        String[] temp;
        int c = 0;
        try{
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("common-") || line.startsWith("path-"))
                {
                    System.out.println(c++ + "   " + line);
                    temp = line.split("\t");
                    unicodeString.add(temp[0]);
                    escapedString.add(temp[1]);     
                }
            }
        } finally {
            s.close();
        }
    }
    
    @WebTestName("listChecksumAlgorithms: tests that the listChecksumAlgorithms call works")
    @WebTestDescription("tests that calling listChecksumAlgorithms "
            + "returns a valid ChecksumAlgorithmList object")
    public void testListChecksumAlgorithms(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListChecksumAlgorithms(nodeIterator.next(), version);
    }
    
    public void testListChecksumAlgorithms(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListChecksumAlgorithms(...) vs. node: " + currentUrl);

        try {
            ChecksumAlgorithmList response = callAdapter.listChecksumAlgorithms();
            checkTrue(callAdapter.getLatestRequestUrl(),"listChecksumAlgorithms(...) returns a valid ChecksumAlgorithmList object", 
                    response instanceof ChecksumAlgorithmList);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    
    @WebTestName("listFormats: tests that the listFormats call works")
    @WebTestDescription("tests that calling listFormats "
            + "returns a valid ObjectFormatList object")
    public void testListFormats(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListFormats(nodeIterator.next(), version);
    }
    
    /**
     * tests that a valid ObjectFormatList is returned from listFormats()
     */
    public void testListFormats(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListFormats(...) vs. node: " + currentUrl);

        try {
            ObjectFormatList response = callAdapter.listFormats();
            checkTrue(callAdapter.getLatestRequestUrl(),"listFormats(...) returns an ObjectFormatList object",
                    response instanceof ObjectFormatList);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    
    @WebTestName("getFormat: tests that the getFormat call works")
    @WebTestDescription("tests that calling getFormat "
            + "returns a valid ObjectFormat object")
    public void testGetFormat(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetFormat(nodeIterator.next(), version);
    }
    
    /**
     * tests that getFormat returns a valid ObjectFormat.  calls listFormats first
     * and fails if there aren't any in the list.
     */
    public void testGetFormat(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

        try {
            ObjectFormatList ofList = callAdapter.listFormats();
            if (ofList.getCount() > 0) {
                ObjectFormat firstObj = ofList.getObjectFormat(0);
                ObjectFormatIdentifier formatId = firstObj.getFormatId();
                ObjectFormat response = callAdapter.getFormat(formatId);
                checkTrue(callAdapter.getLatestRequestUrl(),"getFormat(...) returns an ObjectFormat object", 
                        response instanceof ObjectFormat);
            } else {
                handleFail(callAdapter.getLatestRequestUrl(),"no formats in format list to use for testing getFormat()");
            }
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    
    @WebTestName("getFormat - : tests that the getFormat call fails for invalid formats")
    @WebTestDescription("tests that calling getFormat yields a NotFound exception if we "
            + "give it a bogus formatID")
    public void testGetFormat_bogusFormat(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetFormat_bogusFormat(nodeIterator.next(), version);
    }
    
    /**
     * tests that a bogus formatID throws a NotFound exception
     */
    public void testGetFormat_bogusFormat(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

        try {
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue("aBogusFormat");
            callAdapter.getFormat(formatId);
        
            handleFail(callAdapter.getLatestRequestUrl(),"getFormat(...) with a bogus formatID should " +
                    "throw an exception.");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotFound e) {
            // the desired outcome
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
    
    @WebTestName("listNodes: tests that the listNodes call works")
    @WebTestDescription("tests that calling listNodes returns a valid NodeList object")
    public void testListNodes(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListNodes(nodeIterator.next(), version);
    }
    
    public void testListNodes(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListNodes(...) vs. node: " + currentUrl);

        try {
            NodeList response = callAdapter.listNodes();
            checkTrue(callAdapter.getLatestRequestUrl(),"listNodes(...) returns a valid NodeList object", 
                    response instanceof NodeList);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    
    @WebTestName("generateIdentifier: tests that the generateIdentifier call works")
    @WebTestDescription("tests that calling generateIdentifier returns an "
            + "identifier with 5 hexidecimal segments separated by '-'s")
    public void testGenerateIdentifier(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListNodes(nodeIterator.next(), version);
    }
    
    public void testGenerateIdentifier(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

        try {
            String fragment = "CNodeTier1Test";
            Identifier response = callAdapter.generateIdentifier(null,"UUID",fragment);
            //TODO: UUID isn't supporting the fragment concept, so can't check 
            // this yet.
//              checkTrue(cn.getLatestRequestUrl(),"generateIdentifier(...) should return an Identifier object" +
//                      " containing the given fragment: '" + fragment + "' Got: '" + response.getValue() + "'",
//                      response.getValue().contains(fragment));
            checkTrue(callAdapter.getLatestRequestUrl(),"generateIdentifier(...) should return a UUID-style" +
                    "identifier with 5 hexidecimal segments separated by '-'s.  Got: " +
                    response.getValue(),
                    StringUtils.countOccurrencesOf(response.getValue(),"-") >=4);

        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    
    @WebTestName("generateIdentifier - bad scheme: tests that the generateIdentifier call fails if given an invalid scheme")
    @WebTestDescription("tests that calling generateIdentifier yields an "
            + "an InvalidRequest exception if the given scheme parameter is invalid")
    public void testGenerateIdentifier_badScheme(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGenerateIdentifier_badScheme(nodeIterator.next(), version);
    }
    
    /**
     * tests validity of scheme names by trying an invalid scheme.
     * It should throw an error.
     */
    public void testGenerateIdentifier_badScheme(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

        try {
            String fragment = "CNodeTier1Test";
            callAdapter.generateIdentifier(null,"bloip",fragment);
            handleFail(callAdapter.getLatestRequestUrl(),"generateIdentifier(...) with a bogus scheme should" +
                    "throw an exception (should not have reached here)");
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidRequest e) {
            // the expected outcome indicating good behavior :-)
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("reserveIdentifier: tests that the reserveIdentifier call works")
    @WebTestDescription("tests that calling reserveIdentifier returns the given identifier, "
            + "and that calling it again with the same identifier yields the expected "
            + "IdentifierNotUnique exception")
    public void testReserveIdentifier(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReserveIdentifier(nodeIterator.next(), version);
    }
    
    /**
     * tests that reserveIdentifier works, then tries again to make sure
     * that the identifier cannot be reserved again.
     */
    public void testReserveIdentifier(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testReserveIdentifier(...) vs. node: " + currentUrl);
        
        boolean isReserved = false;
        try {
            Identifier pid = new Identifier();
            pid.setValue(ExampleUtilities.generateIdentifier());

            Identifier response = callAdapter.reserveIdentifier(null, pid);
            checkTrue(callAdapter.getLatestRequestUrl(),"reserveIdentifier(...) should return the given identifier",
                    response.equals(pid));
            isReserved = true;
            // try again - should fail
            response = callAdapter.reserveIdentifier(null,pid);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (IdentifierNotUnique e) {
            if (isReserved) {
                // then got the desired outcome
            } else {
                handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
            }
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
    
    @WebTestName("registerSystemMetadata: tests that the registerSystemMetadata call works")
    @WebTestDescription("tests that calling registerSystemMetadata returns the same pid it was given, "
            + "and that we can then fetch the system metadata after it's been registered")
    public void testRegisterSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegisterSystemMetadata(nodeIterator.next(), version);
    }
    
    public void testRegisterSystemMetadata(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreate() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage("cNodeTier1TestCreate",true);
            org.dataone.service.types.v1.SystemMetadata smdV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata smdV2 = TypeMarshaller.convertTypeFromType(smdV1, SystemMetadata.class);
            Identifier pid = callAdapter.registerSystemMetadata(null,(Identifier) dataPackage[0], smdV2);  
            
            checkEquals(callAdapter.getLatestRequestUrl(),"pid of registered sysmetadata should equal that given",
                    ((Identifier)dataPackage[0]).getValue(), pid.getValue());
            
            SystemMetadata smdReturned = callAdapter.getSystemMetadata(null,pid);
            checkEquals(callAdapter.getLatestRequestUrl(),"should be able to get registered sysmeta",
                    smdReturned.getIdentifier().getValue(),
                    smdV2.getIdentifier().getValue());
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }

    @WebTestName("hasReservation: tests that the hasReservation call works")
    @WebTestDescription("tests that calling hasReservation after having registered the identifier "
            + "returns true")
    public void testHasReservation(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testHasReservation(nodeIterator.next(), version);
    }
    
    /**
     * Creates an identifier and reserves it.  Then checks that the client indeed
     * has the reservation (hasReservation());
     */
    public void testHasReservation(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        String currentUrl = node.getBaseURL();
        Subject clientSubject = ContextAwareTestCaseDataone.getSubject("testSubmitter");
        printTestHeader("testHasReservation(...) vs. node: " + currentUrl);

        try {
            boolean response = false;
//              if (reservedIdentifier != null) {
//                  response = cn.hasReservation(null,clientSubject,reservedIdentifier);
//              } else {
                Identifier pid = new Identifier();
                pid.setValue(ExampleUtilities.generateIdentifier());
                callAdapter.reserveIdentifier(null, pid);
                response = callAdapter.hasReservation(null, clientSubject, pid);
//              }
            checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("hasReservation - no reservation: tests that the hasReservation call fails if no reservation")
    @WebTestDescription("tests that calling hasReservation after NOT having registered the identifier "
            + "yields a NotFound exception")
    public void testHasReservation_noReservation(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testHasReservation_noReservation(nodeIterator.next(), version);
    }
    
    /**
     * Generates a new identifier (locally), and tries hasReservation() without first reserving it.
     * Expect a NotFound exception.  
     */
    public void testHasReservation_noReservation(Node node, String version) {
        

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testHasReservation(...) vs. node: " + currentUrl);

        try {
            boolean response = false;
            Identifier pid = new Identifier();
            pid.setValue(ExampleUtilities.generateIdentifier());
            response = callAdapter.hasReservation(null,D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC), pid);

            checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", 
                    response);
        }
        catch (NotFound e) {
            ; // this is desired behavior
        }
//          catch (NotAuthorized e) {
//              ; // this is also acceptable
//          }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
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
    

    /**
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
     * Runs tests across all nodes in the given nodeIterator.
     * 
     * @param nodeIterator 
     *      an {@link Iterator} accross MN or CN {@link Node}s
     * @param version 
     *      either "v1" or "v2", to match the API version being tested
     */
    @WebTestName("getLogRecords: tests that the getLogRecords call works")
    @WebTestDescription("tests that calling getLogRecords returns a non-null result")
    public void testGetLogRecords(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetLogRecords(nodeIterator.next(), version);
    }

    /**
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
     */
    public void testGetLogRecords(Node node, String version) {

        String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "cnDevUNM1");
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Log eventLog = callAdapter.getLogRecords(null, null, null, null, null, null, null);
            checkTrue(callAdapter.getLatestRequestUrl(),"getLogRecords should return a log datatype", eventLog != null);
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }              
    }

    @WebTestName("getLogRecords - slicing: tests that the getLogRecords call's count and start parameters work")
    @WebTestDescription("tests that calling getLogRecords returns a Log whose 'count' attribute matches the "
            + "number of LogEntry objects returned, that 'total' is >= the 'count', and that 'total' is >= "
            + "the number of LogEntry objects returned. It then calls getLogRecords with half the result set size "
            + "as the 'count' parameter and checks that the number of results matches the count")
    public void testGetLogRecords_Slicing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetLogRecords_Slicing(nodeIterator.next(), version);
    }
    
    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
    public void testGetLogRecords_Slicing(Node node, String version) {
        
        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
        // TODO: change to testCnAdmin subject when obtained
        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn",
                "cnStageUNM1");
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubject), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetLogRecords_Slicing(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Log eventLog = callAdapter.getLogRecords(null, null, null, null, null, null, null);

            StringBuffer sb = new StringBuffer();
            int i = 0;
            if (eventLog.getCount() != eventLog.sizeLogEntryList())
                sb.append(++i + ". 'count' attribute should equal the number of LogEntry objects returned.  \n");

            if (eventLog.getTotal() < eventLog.getCount())
                sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned Log.  \n");

            if (eventLog.getTotal() < eventLog.sizeLogEntryList())
                sb.append(++i + "'total' attribute should be >= the number of LogEntry objects returned.  \n");

            // test that one can limit the count
            int halfCount = eventLog.sizeLogEntryList() / 2; // rounds down
            eventLog = callAdapter.getLogRecords(null, null, null, null, null, 0, halfCount);

            if (eventLog.sizeLogEntryList() != halfCount)
                sb.append(++i
                        + ". should be able to limit the number of returned LogEntry objects using 'count' parameter.");

            // TODO:  test that 'start' parameter does what it says

            // TODO: paging test

            if (i > 0) {
                handleFail(callAdapter.getLatestRequestUrl(), "Slicing errors:\n" + sb.toString());
            }

        } catch (NotAuthorized e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "Should not get a NotAuthorized when connecting"
                            + "with a cn admin subject . Check NodeList and MN configuration.  Msg details:"
                            + e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }

    }

    @WebTestName("getLogRecords - date filtering: tests that the getLogRecords call's fromData parameter works")
    @WebTestDescription("calls getLogRecords and checks what the earliest log is, calls getLogRecords with "
            + "that records date as the fromDate parameter, then checks to make sure that earliest log "
            + "is not in the results")
    public void testGetLogRecords_dateFiltering(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetLogRecords_dateFiltering(nodeIterator.next(), version);
    }
    
    public void testGetLogRecords_dateFiltering(Node node, String version) {
        
        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
        // TODO: change to testCnAdmin subject when obtained
        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn",
                "cnStageUNM1");
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubject), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetLogRecords_DateFiltering(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            Log eventLog = callAdapter.getLogRecords(null, null, null, null, null, null, null);
            int allEventsCount = eventLog.getTotal();
            
            
            LogEntry entry0 = eventLog.getLogEntry(0);
            Date fromDate = null;
            LogEntry excludedEntry = null;
            List<LogEntry> logEntryList = eventLog.getLogEntryList();
            if (logEntryList == null || logEntryList.size() == 0)
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Log entry list is empty");
            
            for (LogEntry le: logEntryList) {
                if (!le.getDateLogged().equals(entry0.getDateLogged())) {
                    // which is earlier?  can't assume chronological order of the list
                    if (le.getDateLogged().after(entry0.getDateLogged())) {
                        fromDate = le.getDateLogged();
                        excludedEntry = entry0;
                    } else {
                        fromDate = entry0.getDateLogged();
                        excludedEntry = le;
                    }
                    break;
                }
            }
            if (excludedEntry == null) {
                handleFail(callAdapter.getLatestRequestUrl(),"could not find 2 objects with different dateLogged times");
            } else {
            
                // call with a fromDate
                eventLog = callAdapter.getLogRecords(null, fromDate, null, null, null, null, null);

                for (LogEntry le : eventLog.getLogEntryList()) {
                    if (le.getEntryId().equals(excludedEntry.getEntryId())) {
                        handleFail(callAdapter.getLatestRequestUrl(),"entryID " + excludedEntry.getEntryId() +
                                " should not be in the event log where fromDate set to " + fromDate);
                        break;
                    }
                }
            }
        } 
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }

    @WebTestName("updateSystemMetadata - tests if the call fails with an unauthorized certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with the \"testPerson\" certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testUpdateSystemMetadata_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_NotAuthorized(Node node, String version) {
        
        CommonCallAdapter cnCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter personCallAdapter = new CommonCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            personCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            handleFail(personCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for a connection with unauthorized certificate");
        } 
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with invalid system metadata")
    @WebTestDescription("this test calls updateSystemMetadata() with invalid system metadata "
            + "and expects an InvalidSystemMetadata exception to be thrown")
    public void testUpdateSystemMetadata_InvalidSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata(Node node, String version) {
        
        CommonCallAdapter cnCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter personCallAdapter = new CommonCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(null);
            sysmeta.setIdentifier(null);
            sysmeta.setDateSysMetadataModified(new Date());
            
            personCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            handleFail(personCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidSystemMetadata e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected an InvalidSystemMetadata exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidSystemMetadata exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    // test more negative cases
    /*  no documentation on when these happen though:
     * 
        Exceptions.ServiceFailure – ??? 
        Exceptions.InvalidRequest – ???
        Exceptions.InvalidToken – invalid session info? how so?
     */
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works")
    @WebTestDescription("this test calls updateSystemMetadata() to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated serialVersion and "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata(Node node, String version) {
        
        CommonCallAdapter cnCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(cnCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = cnCallAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = rightsHolderCallAdapter.updateSystemMetadata(null, pid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        } 
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }  
    }
    
}
