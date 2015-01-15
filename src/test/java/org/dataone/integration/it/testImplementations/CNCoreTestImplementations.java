package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.springframework.util.StringUtils;

public class CNCoreTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(CNCoreTestImplementations.class);
    
    private static final String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
    private static final String unicodeIdPrefix = "testCNodeTier1";
    private static Vector<String> unicodeString;
    private static Vector<String> escapedString;
    
    public CNCoreTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
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
            SystemMetadata sysMetaV1 = (SystemMetadata) dataPackage[2];
            org.dataone.service.types.v2.SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, org.dataone.service.types.v2.SystemMetadata.class);
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

                SystemMetadata sysMetaV1 = (SystemMetadata) dataPackage[2];
                org.dataone.service.types.v2.SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, org.dataone.service.types.v2.SystemMetadata.class);
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
                ObjectFormatIdentifier formatId = ofList.getObjectFormat(0).getFormatId();
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

            Identifier response = callAdapter.reserveIdentifier(null,pid);
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
            SystemMetadata smd = (SystemMetadata) dataPackage[2];
            Identifier pid = callAdapter.registerSystemMetadata(null,(Identifier) dataPackage[0], smd);  
            
            checkEquals(callAdapter.getLatestRequestUrl(),"pid of registered sysmetadata should equal that given",
                    ((Identifier)dataPackage[0]).getValue(), pid.getValue());
            
            SystemMetadata smdReturned = callAdapter.getSystemMetadata(null,pid);
            checkEquals(callAdapter.getLatestRequestUrl(),"should be able to get registered sysmeta",
                    smdReturned.getIdentifier().getValue(),
                    smd.getIdentifier().getValue());
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
        Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
        printTestHeader("testHasReservation(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            boolean response = false;
//              if (reservedIdentifier != null) {
//                  response = cn.hasReservation(null,clientSubject,reservedIdentifier);
//              } else {
                Identifier pid = new Identifier();
                pid.setValue(ExampleUtilities.generateIdentifier());
                callAdapter.reserveIdentifier(null, pid );
                response = callAdapter.hasReservation(session, clientSubject, pid);
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
    
}
