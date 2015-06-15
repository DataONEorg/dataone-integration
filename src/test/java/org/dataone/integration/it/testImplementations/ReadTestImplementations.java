package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.TestIterationEndingException;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.DateTimeMarshaller;

public class ReadTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(ReadTestImplementations.class);
    
    private static final String format_text_csv = "text/csv";
    private static Vector<String> unicodeStringV;
    private static Vector<String> escapedStringV;
    
    public ReadTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    @WebTestName("get - returns an object stream")
    @WebTestDescription("tests that get returns a valid non-null object stream")
    public void testGet(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGet(nodeIterator.next(), version);
    }

    public void testGet(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet() vs. node: " + currentUrl);

        InputStream is = null;
        try {
            String objectIdentifier = "TierTesting:" + catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier id = catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            //             Identifier id =catc.procurePublicReadableTestObject(mn);
            is = callAdapter.get(null, id);
            checkTrue(callAdapter.getLatestRequestUrl(), "get() returns an objectStream", is != null);
        } catch (TestIterationEndingException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against:: " + e.getMessage());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @WebTestName("get - should not find a fake identifier")
    @WebTestDescription("tests the negative case when get has an invalid identifier as a parameter, "
            + "a NotFound exception is expected")
    public void testGet_NotFound(Iterator<Node> nodeIterator, String version) {
       while (nodeIterator.hasNext())
            testGet_NotFound(nodeIterator.next(), version);
    }

    public void testGet_NotFound(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet() vs. node: " + currentUrl);

        InputStream is = null;
        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
            is = callAdapter.get(null, D1TypeBuilder.buildIdentifier(fakeID));
            handleFail(callAdapter.getLatestRequestUrl(), "get(fakeID) should not return an objectStream.");
            is.close();
        } catch (NotFound nf) {
            ; // expected outcome
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * test getting data with challenging unicode identifiers.  Will try to 
     * differentiate between NotFound and ServiceFailure
     */
    @WebTestName("get - getting data with challenging unicode identifiers")
    @WebTestDescription("tests the negative case when get has an invalid identifier as a parameter, "
            + "containing a variety of unicode identifiers, and expects either "
            + "a NotFound or a ServiceFailue (the latter only if it mentions "
            + "\"Providing message body\" or \"404: NotFound:\")")
    public void testGet_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGet_IdentifierEncoding(nodeIterator.next(), version);
    }

    /**
     * test getting data with challenging unicode identifiers.  Will try to 
     * differentiate between NotFound and ServiceFailure
     */
    public void testGet_IdentifierEncoding(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);
        setupIdentifierVectors();
        
        for (int j = 0; j < unicodeStringV.size(); j++) {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j);
            String idStringEscaped = "Test" + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);

            InputStream data = null;
            try {
                data = callAdapter.get(null, D1TypeBuilder.buildIdentifier(idString));
                handleFail(callAdapter.getLatestRequestUrl(), "get() against the fake identifier (" + idStringEscaped
                        + ") should throw NotFound");
                data.close();
                status = "Error";
            } catch (NotFound nf) {
                ;
            } catch (ServiceFailure e) {
                if (e.getDescription().contains("Providing message body")) {
                    if (e.getDescription().contains("404: NotFound:")) {
                        // acceptable result
                        ;
                    }
                } else {
                    status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                            first100Characters(e.getDescription()));
                }
            } catch (BaseException e) {
                status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                        first100Characters(e.getDescription()));
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                status = String.format("Error:: %s: %s", e.getClass().getName(), first100Characters(e.getMessage()));
            } finally {
                IOUtils.closeQuietly(data);
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
        }

        for (String result : nodeSummary) {
            if (result.contains("Error")) {
                handleFail(null, currentUrl + " " + tablifyResults(nodeSummary));
                break;
            }
        }
    }

    @WebTestName("getSystemMetadata - getting system metadata returns a valid object")
    @WebTestDescription("tests the getSystemMetadata returns a valid non-null SystemMetadata object")
    public void testGetSystemMetadata(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSystemMetadata(nodeIterator.next(), version);
    }

    public void testGetSystemMetadata(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier id = catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            //              Identifier id =catc.procurePublicReadableTestObject(callAdapter);
            SystemMetadata smd = callAdapter.getSystemMetadata(null, id);
            checkTrue(callAdapter.getLatestRequestUrl(), "getSystemMetadata() returns a SystemMetadata object",
                    smd != null);
        } catch (IndexOutOfBoundsException e) {
            handleFail(currentUrl, "No Objects available to test against");
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getSystemMetadata - should not find system metadata when given a fake identifier")
    @WebTestDescription("tests the negative case where getSystemMetadata is given a fake identifier, "
            + "expecting a NotFound exception")
    public void testGetSystemMetadata_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSystemMetadata_NotFound(nodeIterator.next(), version);
    }

    public void testGetSystemMetadata_NotFound(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);

        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
            callAdapter.getSystemMetadata(null, D1TypeBuilder.buildIdentifier(fakeID));
            handleFail(callAdapter.getLatestRequestUrl(), "getSystemMetadata(fakeID) should throw dataone NotFound.");
        } catch (NotFound nf) {
            ; // expected outcome
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getSystemMetadata - getting data with challenging unicode identifiers")
    @WebTestDescription("tests the negative case when get has an invalid identifier as a parameter, "
            + "containing a variety of unicode identifiers, and expects either "
            + "a NotFound or a ServiceFailue (the latter only if it mentions "
            + "\"Providing message body\" or \"404: NotFound:\")")
    public void testGetSystemMetadata_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSystemMetadata_IdentifierEncoding(nodeIterator.next(), version);
    }

    public void testGetSystemMetadata_IdentifierEncoding(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetSystemMetadata_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);
        setupIdentifierVectors();
        
        for (int j = 0; j < unicodeStringV.size(); j++) {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j);
            String idStringEscaped = "Test" + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);

            try {
                callAdapter.getSystemMetadata(null, D1TypeBuilder.buildIdentifier(idString));
                handleFail(callAdapter.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier ("
                        + idStringEscaped + ") should throw NotFound");

                status = "Error";
            } catch (NotFound nf) {
                ;
            } catch (ServiceFailure e) {
                if (e.getDescription().contains("Providing message body")) {
                    if (e.getDescription().contains("404: NotFound:")) {
                        // acceptable result
                        ;
                    }
                } else {
                    status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                            first100Characters(e.getDescription()));
                }
            } catch (BaseException e) {
                status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                        first100Characters(e.getDescription()));
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                status = String.format("Error:: %s: %s", e.getClass().getName(), first100Characters(e.getMessage()));
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
        }

        for (String result : nodeSummary) {
            if (result.contains("Error")) {
                handleFail(null, currentUrl + " " + tablifyResults(nodeSummary));
                break;
            }
        }
    }

    @WebTestName("describe - describe returns a DescribeResponse")
    @WebTestDescription("tests that calling describe with a valid identifier "
            + "returns a non-null DescribeResponse")
    public void testDescribe(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testDescribe(nodeIterator.next(), version);
    }

    public void testDescribe(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testDescribe() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier id = catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            //              Identifier id =catc.procurePublicReadableTestObject(callAdapter);
            DescribeResponse dr = callAdapter.describe(null, id);
            checkTrue(callAdapter.getLatestRequestUrl(), "describe() returns a DescribeResponse object", dr != null);
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("describe - describe should not work with a fake identifier")
    @WebTestDescription("tests that calling describe with a fake identifier "
            + "returns a NotFoundException")
    public void testDescribe_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testDescribe_NotFound(nodeIterator.next(), version);
    }

    public void testDescribe_NotFound(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testDescribe() vs. node: " + currentUrl);

        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
            callAdapter.describe(null, D1TypeBuilder.buildIdentifier(fakeID));
            handleFail(callAdapter.getLatestRequestUrl(), "describe(fakeID) should return a d1 NotFound in the header.");

        } catch (NotFound nf) {
            ; // expected outcome
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("describe - calling describe with challenging unicode identifiers")
    @WebTestDescription("tests the negative case when get has an invalid identifier as a parameter, "
            + "containing a variety of unicode identifiers, and expects either "
            + "a NotFound or a ServiceFailue (the latter only if it mentions "
            + "\"Providing message body\" or \"404: NotFound:\")")
    public void testDescribe_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testDescribe_IdentifierEncoding(nodeIterator.next(), version);
    }

    public void testDescribe_IdentifierEncoding(Node node, String version) {

        printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testDescribe_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);
        setupIdentifierVectors();
        
        for (int j = 0; j < unicodeStringV.size(); j++) {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j);
            String idStringEscaped = "Test" + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);

            try {
                callAdapter.describe(null, D1TypeBuilder.buildIdentifier(idString));
                handleFail(callAdapter.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier ("
                        + idStringEscaped + ") should throw NotFound");

                status = "Error";
            } catch (NotFound nf) {
                ;
            } catch (ServiceFailure e) {
                if (e.getDescription().contains("Providing message body")) {
                    if (e.getDescription().contains("404: NotFound:")) {
                        // acceptable result
                        ;
                    }
                } else {
                    status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                            first100Characters(e.getDescription()));
                }
            } catch (BaseException e) {
                status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                        first100Characters(e.getDescription()));
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                status = String.format("Error:: %s: %s", e.getClass().getName(), first100Characters(e.getMessage()));
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
        }

        for (String result : nodeSummary) {
            if (result.contains("Error")) {
                handleFail(null, currentUrl + " " + tablifyResults(nodeSummary));
                break;
            }
        }
    }

    @WebTestName("getChecksum - calling getChecksum returns a valid Checksum")
    @WebTestDescription("tests that calling getChecksum with a valid identifier "
            + "returns a non-null Checksum")
    public void testGetChecksum(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetChecksum(nodeIterator.next(), version);
    }

    public void testGetChecksum(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetChecksum() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" +catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" +catc.getTestObjectSeriesSuffix();
            Identifier id =catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            //              Identifier id =catc.procurePublicReadableTestObject(callAdapter);
            Checksum cs = callAdapter.getChecksum(null, id, ContextAwareTestCaseDataone.CHECKSUM_ALGORITHM);
            checkTrue(callAdapter.getLatestRequestUrl(), "getChecksum() returns a Checksum object", cs != null);
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getChecksum - calling getChecksum returns a valid Checksum")
    @WebTestDescription("tests the negative case, when getChecksum is called with a fake "
            + "identifier and is expected to return a NotFoundException")
    public void testGetChecksum_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetChecksum_NotFound(nodeIterator.next(), version);
    }

    public void testGetChecksum_NotFound(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetChecksum() vs. node: " + currentUrl);

        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
            callAdapter.getChecksum(null, D1TypeBuilder.buildIdentifier(fakeID), null);
            handleFail(callAdapter.getLatestRequestUrl(), "getChecksum(fakeID) should return a NotFound");
        } catch (NotFound nf) {
            ; // expected outcome
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getChecksum - calling getChecksum with challenging unicode identifiers")
    @WebTestDescription("tests the negative case when get has an invalid identifier as a parameter, "
            + "containing a variety of unicode identifiers, and expects either "
            + "a NotFound or a ServiceFailue (the latter only if it mentions "
            + "\"Providing message body\" or \"404: NotFound:\")")
    public void testGetChecksum_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetChecksum_IdentifierEncoding(nodeIterator.next(), version);
    }

    public void testGetChecksum_IdentifierEncoding(Node node, String version) {
        
        printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetChecksum_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);
        setupIdentifierVectors();
        
        for (int j = 0; j < unicodeStringV.size(); j++) {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j);
            String idStringEscaped = "Test" + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);

            try {
                callAdapter.getChecksum(null, D1TypeBuilder.buildIdentifier(idString), null);
                handleFail(callAdapter.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier ("
                        + idStringEscaped + ") should throw NotFound");

                status = "Error";
            } catch (NotFound nf) {
                ;
            } catch (ServiceFailure e) {
                if (e.getDescription().contains("Providing message body")) {
                    if (e.getDescription().contains("404: NotFound:")) {
                        // acceptable result
                        ;
                    }
                } else {
                    status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                            first100Characters(e.getDescription()));
                }
            } catch (BaseException e) {
                status = String.format("Error:: %s: %s: %s", e.getClass().getSimpleName(), e.getDetail_code(),
                        first100Characters(e.getDescription()));
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                status = String.format("Error:: %s: %s", e.getClass().getName(), first100Characters(e.getMessage()));
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
        }

        for (String result : nodeSummary) {
            if (result.contains("Error")) {
                handleFail(null, currentUrl + " " + tablifyResults(nodeSummary));
                break;
            }
        }
    }

    /**
     * Tests the parameterless and parameterized listObject methods for proper returns.
     */
    @WebTestName("listObjects - calling listObjects returns a valid ObjectList")
    @WebTestDescription("tests that callin listObjects with a valid identifier "
            + "returns a non-null ObjectList")
    public void testListObjects(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListObjects(nodeIterator.next(), version);
    }

    /**
     * Tests the parameterless and parameterized listObject methods for proper returns.
     */
    public void testListObjects(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListObjects() vs. node: " + currentUrl);

        try {
            ObjectList ol = catc.procureObjectList(callAdapter);
            checkTrue(callAdapter.getLatestRequestUrl(), "listObjects() should return an ObjectList", ol != null);

            Date fromDate = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
            Date toDate = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
            ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
            formatId.setValue(format_text_csv);
            Boolean replicaStatus = true;
            ol = callAdapter.listObjects(null, fromDate, toDate, formatId, null, replicaStatus, Integer.valueOf(0),
                    Integer.valueOf(10));
            checkTrue(callAdapter.getLatestRequestUrl(), "listObjects(<parameters>) returns an ObjectList", ol != null);
        } catch (TestIterationEndingException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getMessage() + ":: cause: "
                    + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are the correct values.
     */
    @WebTestName("listObjects - tests slicing with listObjects")
    @WebTestDescription("tests that the 'count' attribute should equal the number of ObjectInfos returned, "
            + "that the 'total' attribute is >= the 'count' attribute in the returned ObjectList, "
            + "that the 'total' attribute should be >= the number of ObjectInfos returned, "
            + "and that we should be able to limit the number of returned ObjectInfos using the 'count' parameter.")
    public void testListObjects_Slicing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext()) 
            testListObjects_Slicing(nodeIterator.next(), version);
    }

    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are the correct values.
     */
    public void testListObjects_Slicing(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListObjects_Slicing(...) vs. node: " + currentUrl);

        try {
            ObjectList ol = callAdapter.listObjects(null, null, null, null, null, null, null, null);
            // make sure the count is accurate
            StringBuffer sb = new StringBuffer();
            int i = 0;
            if (ol.getCount() != ol.sizeObjectInfoList())
                sb.append(++i + ". 'count' attribute should equal the number of ObjectInfos returned. ["
                        + callAdapter.getLatestRequestUrl() + "]  \n");

            if (ol.getTotal() < ol.getCount())
                sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned ObjectList. ["
                        + callAdapter.getLatestRequestUrl() + "]  \n");

            if (ol.getTotal() < ol.sizeObjectInfoList())
                sb.append(++i + "'total' attribute should be >= the number of ObjectInfos returned. ["
                        + callAdapter.getLatestRequestUrl() + "]  \n");

            // test that one can limit the count
            int halfCount = ol.sizeObjectInfoList() / 2; // rounds down
            ol = callAdapter.listObjects(null, null, null, null, null, null, 0, halfCount);

            if (ol.sizeObjectInfoList() != halfCount)
                sb.append(++i + ". should be able to limit the number of returned ObjectInfos using "
                        + "'count' parameter. [" + callAdapter.getLatestRequestUrl() + "]  \n");

            // TODO:  test that 'start' parameter does what it says

            // TODO: paging test

            if (i > 0) {
                handleFail(currentUrl, "Slicing errors:\n" + sb.toString());
            }

        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that the fromDate parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than fromDate.
     */
    @WebTestName("listObjects - tests date filtering with listObjects")
    @WebTestDescription("tests that the fromDate parameter for listObjects works and can "
            + "be used to filter results based on the date")
    public void testListObjects_FromDateTest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext()) 
            testListObjects_FromDateTest(nodeIterator.next(), version);
    }

    /**
     * Tests that the fromDate parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than fromDate.
     */
    public void testListObjects_FromDateTest(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListObjects_FromDateTest() vs. node: " + currentUrl);

        try {
            ObjectList ol = catc.procureObjectList(callAdapter);
            checkTrue(callAdapter.getLatestRequestUrl(), "listObjects() should return an ObjectList", ol != null);
            if (ol.getTotal() == 0)
                throw new TestIterationEndingException("no objects found in listObjects");
            ObjectInfo oi0 = ol.getObjectInfo(0);
            Date fromDate = null;
            ObjectInfo excludedObjectInfo = null;
            for (ObjectInfo oi : ol.getObjectInfoList()) {
                if (!oi.getDateSysMetadataModified().equals(oi0.getDateSysMetadataModified())) {
                    // which is earlier?  can't assume chronological order of objectlist
                    if (oi.getDateSysMetadataModified().after(oi0.getDateSysMetadataModified())) {
                        fromDate = oi.getDateSysMetadataModified();
                        excludedObjectInfo = oi0;
                    } else {
                        fromDate = oi0.getDateSysMetadataModified();
                        excludedObjectInfo = oi;
                    }
                    break;
                }
            }
            if (excludedObjectInfo == null) {
                // all objects in list have same date, so set the from date
                // to a future date
                long millisec = oi0.getDateSysMetadataModified().getTime() + 60000;
                fromDate = new Date(millisec);
                excludedObjectInfo = oi0;
            }

            // call listObjects with a fromDate
            ol = callAdapter.listObjects(null, fromDate, null, null, null, null, null, null);

            if (ol.getObjectInfoList() != null) {
                // at least some objects returned
                // so we have to check that the excluded object was excluded
                for (ObjectInfo oi : ol.getObjectInfoList()) {
                    if (oi.getIdentifier().equals(excludedObjectInfo.getIdentifier())) {
                        handleFail(callAdapter.getLatestRequestUrl(), String.format(
                                "identifier %s with sysMetaModified date of '%s'"
                                        + " should not be in the objectList where 'fromDate' parameter set to '%s'",
                                excludedObjectInfo.getIdentifier().getValue(),
                                DateTimeMarshaller.serializeDateToUTC(excludedObjectInfo.getDateSysMetadataModified()),
                                DateTimeMarshaller.serializeDateToUTC(fromDate)));
                    }
                }
            } // else the excluded object was definitely excluded - test passes
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that the formatID parameter rightly returns no records
     * when a fake format is given
     */
    @WebTestName("listObjects - tests fake formatID parameter")
    @WebTestDescription("tests that a fake formatID parameter passed to listObjects will "
            + "return no records")
    public void testListObjects_FormatIdFilteringTestFakeFormat(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext()) 
            testListObjects_FormatIdFilteringTestFakeFormat(nodeIterator.next(), version);
    }

    /**
     * Tests that the formatID parameter rightly returns no records
     * when a fake format is given
     */
    public void testListObjects_FormatIdFilteringTestFakeFormat(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListObjects_FormatIdFilteringFakeFormat() vs. node: " + currentUrl);

        try {
            // call listObjects with a fake format
            ObjectList ol = callAdapter.listObjects(null, null, null,
                    D1TypeBuilder.buildFormatIdentifier("fake_format"), null, null, null, null);
            if (ol.getTotal() != 0) {
                handleFail(callAdapter.getLatestRequestUrl(), "filtering the object list by a fake "
                        + "format should return zero objects");
            }
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that the formatID parameter successfully filters records by
     * the given formatId.  It is an indirect test of the totals returned
     * by list objects with and without a formatId filter.
     */
    @WebTestName("listObjects - tests filtering by formatID parameter")
    @WebTestDescription("tests that the formatID parameter successfully filters records by "
            + "the given formatId, calling listObjects with and without formatID and "
            + "comparing totals")
    public void testListObjects_FormatIdFilteringTest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListObjects_FormatIdFilteringTest(nodeIterator.next(), version);
    }

    /**
     * Tests that the formatID parameter successfully filters records by
     * the given formatId.  It is an indirect test of the totals returned
     * by list objects with and without a formatId filter.
     */
    public void testListObjects_FormatIdFilteringTest(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListObjects_FormatIdFiltering() vs. node: " + currentUrl);

        try {
            ObjectList ol = callAdapter.listObjects(null, null, null, null, null, null, null, null);
            checkTrue(callAdapter.getLatestRequestUrl(), "listObjects() should return an ObjectList", ol != null);
            if (ol == null || ol.getTotal() == 0)
                throw new TestIterationEndingException("no objects found in listObjects");

            int allTotal = ol.getTotal();

            ObjectFormatIdentifier formatA = ol.getObjectInfo(0).getFormatId();

            boolean foundAnother = false;
            int increment = 200;
            findAnotherFormat: for (int i = 0; i < allTotal; i += increment) {
                ol = callAdapter.listObjects(null, null, null, null, null, null, i, increment);
                for (ObjectInfo oi : ol.getObjectInfoList()) {
                    if (!oi.getFormatId().equals(formatA)) {
                        foundAnother = true;
                        break findAnotherFormat;
                    }
                }
            }
            if (!foundAnother) {
                throw new TestIterationEndingException("only one object format was found.  Can't test format filtering");
            }
            ol = callAdapter.listObjects(null, null, null, formatA, null, null, null, null);
            checkTrue(callAdapter.getLatestRequestUrl(), "objectList filtered by " + formatA.getValue()
                    + " should contain fewer objects than unfiltered", ol.getTotal() < allTotal);

        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void setupIdentifierVectors() {
        if (unicodeStringV != null && escapedStringV != null)
            return;
        
        // get identifiers to check with
        unicodeStringV = new Vector<String>();
        escapedStringV = new Vector<String>();
        //   TODO: test against Unicode characters when metacat supports unicode        
        InputStream is = this.getClass().getResourceAsStream(
                "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
        //InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
        Scanner s = new Scanner(is, "UTF-8");
        String[] temp;
        int c = 0;
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("common-") || line.startsWith("path-")) {
                    if (line.contains("supplementary"))
                        continue;

                    temp = line.split("\t");

                    // identifiers can't contain spaces by default
                    if (temp[0].contains(" "))
                        continue;

                    log.info(c++ + "   " + line);
                    unicodeStringV.add(temp[0]);
                    escapedStringV.add(temp[1]);
                }
            }
        } finally {
            s.close();
        }
        
    }

    private String first100Characters(String s) {
        if (s.length() <= 100)
            return s;

        return s.substring(0, 100) + "...";

    }

    private String tablifyResults(Vector<String> results) {
        StringBuffer table = new StringBuffer("Failed 1 or more identifier encoding tests");
        for (String result : results) {
            table.append(result);
            table.append("\n    ");
        }
        return table.toString();
    }
}
