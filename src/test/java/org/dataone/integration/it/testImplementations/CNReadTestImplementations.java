package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.it.testDefinitions.CNReadTestDefinitions;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.util.Constants;
import org.dataone.service.util.D1Url;

/**
 * Implementations of tests for CNode read API calls.
 * Should match tests required by {@link CNReadTestDefinitions}.
 */
public class CNReadTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(CNReadTestImplementations.class);
    private static final String unicodeIdPrefix = "testCNodeTier1";
    private static final String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
    private Vector<String> unicodeString = null;
    private Vector<String> escapedString = null;

    public CNReadTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("resolve - calling resolve returns valid ObjectLocationList")
    @WebTestDescription("tests that calling resolve returns a non-null ObjectLocationList")
    public void testResolve(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testResolve(nodeIterator.next(), version);
    }

    public void testResolve(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testResolve(...) vs. node: " + currentUrl);

        try {
            ObjectList ol = catc.procureObjectList(callAdapter);
            Identifier pid = null;
            for (int i = 0; i < ol.sizeObjectInfoList(); i++) {
                try {
                    callAdapter.getSystemMetadata(null, ol.getObjectInfo(i).getIdentifier());
                    pid = ol.getObjectInfo(i).getIdentifier();
                    break;
                } catch (BaseException be) {
                    ;
                }
            }
            if (pid != null) {
                log.debug("   pid = " + pid.getValue());
                ObjectLocationList response = callAdapter.resolve(null, pid);
                checkTrue(callAdapter.getLatestRequestUrl(),
                        "resolve(...) returns an ObjectLocationList object", response != null
                                && response instanceof ObjectLocationList);
            } else {
                handleFail(currentUrl, "No public object available to test resolve against");
            }
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("search - search returns valid ObjectList")
    @WebTestDescription("tests that calling search returns a non-null ObjectList")
    public void testSearch(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSearch(nodeIterator.next(), version);
    }

    public void testSearch(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSearch(...) vs. node: " + currentUrl);

        try {
            ObjectList response = callAdapter.search(null, "solr", "?q=*:*");
            checkTrue(callAdapter.getLatestRequestUrl(), "search(...) returns a ObjectList object",
                    response != null);
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("search - tests search with a variety of unicode strings")
    @WebTestDescription("tests that calling search with a variety of unicode strings "
            + "returns a non-null ObjectList (which may be empty)")
    public void testSearch_Solr_unicodeTests(Iterator<Node> nodeIterator, String version) {
        initializeUnicodeStrings();
        while (nodeIterator.hasNext())
            testSearch_Solr_unicodeTests(nodeIterator.next(), version);
    }

    public void testSearch_Solr_unicodeTests(Node node, String version) {

        // get identifiers to check with
        initializeUnicodeStrings();
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSearch_Solr_unicodeTests(...) vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        for (int i = 0; i < unicodeString.size(); i++) {
            String status = "OK   ";

            // String unicode = unicodeString.get(j);
            System.out.println();
            System.out.println(i + "    unicode String:: " + unicodeString.get(i));
            // String idSubStringEscaped =  escapedString.get(i);

            try {
                D1Url query = new D1Url("a", "b");
                String wildcardPattern = unicodeIdPrefix + "*" + unicodeString.get(i) + "*";
                String solrEscapedWildcardPattern = ClientUtils.escapeQueryChars(wildcardPattern);
                query.addNonEmptyParamPair("q", "id:" + solrEscapedWildcardPattern);

                ObjectList response = callAdapter.search(null, "solr", query.getUrl());
                checkTrue(callAdapter.getLatestRequestUrl(),
                        "search(...) should return an ObjectList", response != null);
                // checkTrue(cn.getLatestRequestUrl(),"search(...) should ")
            } catch (IndexOutOfBoundsException e) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "No Objects available to test against");
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
            }

            nodeSummary.add("Test " + i + ": " + status + ": " + unicodeString.get(i));
        }

        System.out.println();
        for (int k = 0; k < nodeSummary.size(); k++) {
            System.out.println(nodeSummary.get(k));
        }
        System.out.println();

    }

    private void initializeUnicodeStrings() {
        if (unicodeString != null && escapedString != null)
            return; // already initialized

        unicodeString = new Vector<String>();
        escapedString = new Vector<String>();
        InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
        Scanner s = new Scanner(is, "UTF-8");
        String[] temp;
        int c = 0;
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("common-") || line.startsWith("query-")) {
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

    @WebTestName("query - tests query returns a valid object")
    @WebTestDescription("tests that running a solr query with ?q=*.* returns "
            + "a non-null InputStream")
    public void testQuery(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testQuery(nodeIterator.next(), version);
    }

    public void testQuery(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testQuery(...) vs. node: " + currentUrl);

        try {
            InputStream response2 = callAdapter.query(null, "solr", "?q=*:*");
            checkTrue(callAdapter.getLatestRequestUrl(), "query(...)"
                    + " returns an InputStream object", response2 != null);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("query - tests query with a certificate-less connection")
    @WebTestDescription("tests that running a solr query with with a certificate-less connection "
            + "throws no exceptions and returns a non-null InputStream")
    public void testQuery_Authentication(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testQuery_Authentication(nodeIterator.next(), version);
    }

    public void testQuery_Authentication(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testQuery(...) vs. node: " + currentUrl);

        try {
            InputStream response2 = callAdapter.query(null, "solr", "?q=*:*");
            checkTrue(callAdapter.getLatestRequestUrl(), "query(...)"
                    + " returns an InputStream object", response2 != null);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("listQueryEngines - tests listQueryEngines returns a valid QueryEngineList")
    @WebTestDescription("tests that calling listQueryEngines returns a non-null QueryEngineList")
    public void testListQueryEngines(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext()) {
            testListQueryEngines(nodeIterator.next(), version);
        }
    }

    public void testListQueryEngines(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListQueryEngines(...) vs. node: " + currentUrl);

        try {
            QueryEngineList response = callAdapter.listQueryEngines(null);
            checkTrue(callAdapter.getLatestRequestUrl(),
                    "listQueryEngines(...) returns a QueryEngineList object", response != null);
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getQueryEngineDescription - tests getQueryEngineDescription returns a valid QueryEngineDescription")
    @WebTestDescription("calls listQueryEngines and uses the first query engine in the list to call "
            + "getQueryEngineDescription with, and checks that this returns a non-null QueryEngineDescription")
    public void testGetQueryEngineDescription(Iterator<Node> nodeIterator, String version) {
        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            testGetQueryEngineDescription(nodeIterator.next(), version);
        }
    }

    public void testGetQueryEngineDescription(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetQueryEngineDescription(...) vs. node: " + currentUrl);

        try {
            QueryEngineList response = callAdapter.listQueryEngines(null);

            QueryEngineDescription response2 = callAdapter.getQueryEngineDescription(null,
                    response.getQueryEngine(0));
            checkTrue(callAdapter.getLatestRequestUrl(), "getQueryEngineDescription(...)"
                    + " returns a QueryEngineDescription object", response2 != null);
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
