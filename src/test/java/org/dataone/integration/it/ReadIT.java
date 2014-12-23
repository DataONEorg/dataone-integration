package org.dataone.integration.it;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.CommonCallAdapter;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Test;

public abstract class ReadIT extends ContextAwareTestCaseDataone {

    private static Vector<String> unicodeStringV;
    private static Vector<String> escapedStringV;

    // common methods:

    //    public InputStream get();
    //    public SystemMetadata getSystemMetadata();
    //    public DescribeResponse describe();
    //    public Checksum getChecksum();
    //    public ObjectList listObjects();
    //    public DescribeResponse describe();

    @Test
    public abstract void testGet();

    @Test
    public abstract void testGet_NotFound();

    @Test
    public abstract void testGet_IdentifierEncoding();
    
    
    // CN-only methods:

    //    public ObjectLocationList resolve(Session session, Identifier id) ;
    //    public ObjectList search(Session session, String queryType, String query) ;
    //    public InputStream query(Session session, String queryEngine, String query) ;
    //    public QueryEngineDescription getQueryEngineDescription(Session session, String queryEngine);
    //    public QueryEngineList listQueryEngines(Session session);

    // MN-only methods:

    //    public boolean systemMetadataChanged(Session session, Identifier id, long serialVersion,
    //            Date dateSystemMetadataLastModified) ;
    //    public boolean synchronizationFailed(Session session, SynchronizationFailed message) ;
    //    public InputStream getReplica(Session session, Identifier pid);

    

    protected void testGet(Iterator<Node> nodeIterator, String version) {

        setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            testGet(nodeIterator.next(), version);
        }
    }

    private void testGet(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + testObjectSeriesSuffix;
            Identifier id = procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            //             Identifier id = procurePublicReadableTestObject(mn);
            InputStream is = callAdapter.get(null, id);
            checkTrue(callAdapter.getLatestRequestUrl(), "get() returns an objectStream", is != null);
        } catch (TestIterationEndingException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against:: " + e.getMessage());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void testGet_NotFound(Iterator<Node> nodeIterator, String version) {

        setupClientSubject_NoCert();
        Iterator<Node> it = getMemberNodeIterator();
        while (nodeIterator.hasNext()) {
            testGet_NotFound(nodeIterator.next(), version);
        }
    }

    private void testGet_NotFound(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet() vs. node: " + currentUrl);

        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
            InputStream is = callAdapter.get(null, D1TypeBuilder.buildIdentifier(fakeID));
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
        }
    }

    /**
     * test getting data with challenging unicode identifiers.  Will try to 
     * differentiate between NotFound and ServiceFailure
     */
    protected void testGet_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {

        setupClientSubject_NoCert();
        Iterator<Node> it = getMemberNodeIterator();
        while (nodeIterator.hasNext()) {
            testGet_IdentifierEncoding(nodeIterator.next(), version);
        }
    }

    /**
     * test getting data with challenging unicode identifiers.  Will try to 
     * differentiate between NotFound and ServiceFailure
     */
    private void testGet_IdentifierEncoding(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGet_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);

        for (int j = 0; j < unicodeStringV.size(); j++) {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j);
            String idStringEscaped = "Test" + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);

            try {

                InputStream data = callAdapter.get(null, D1TypeBuilder.buildIdentifier(idString));
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

    @Before
    public void setupIdentifierVectors() {
        if (unicodeStringV == null) {
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
