package org.dataone.integration.it;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dataone.integration.CNCallAdapter;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
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

    public void testResolve(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testResolve(nodeIterator.next(), version);
    }

    public void testResolve(Node node, String version) {

        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testResolve(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            ObjectList ol = catc.procureObjectList(callAdapter);
            Identifier pid = null;
            for (int i = 0; i < ol.sizeObjectInfoList(); i++) {
                try {
                    callAdapter.getSystemMetadata(session, ol.getObjectInfo(i).getIdentifier());
                    pid = ol.getObjectInfo(i).getIdentifier();
                    break;
                } catch (BaseException be) {
                    ;
                }
            }
            if (pid != null) {
                log.debug("   pid = " + pid.getValue());
                ObjectLocationList response = callAdapter.resolve(session, pid);
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

    public void testSearch(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSearch(nodeIterator.next(), version);
    }

    public void testSearch(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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

    public void testSearch_Solr_unicodeTests(Iterator<Node> nodeIterator, String version) {
        initializeUnicodeStrings();
        while (nodeIterator.hasNext())
            testSearch_Solr_unicodeTests(nodeIterator.next(), version);
    }

    public void testSearch_Solr_unicodeTests(Node node, String version) {

        // get identifiers to check with
        initializeUnicodeStrings();
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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

    public void testQuery(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testQuery(nodeIterator.next(), version);
    }

    public void testQuery(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testQuery(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            InputStream response2 = callAdapter.query(session, "solr", "?q=*:*");
            checkTrue(callAdapter.getLatestRequestUrl(), "query(...)"
                    + " returns an InputStream object", response2 != null);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void testQuery_Authentication(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testQuery_Authentication(nodeIterator.next(), version);
    }

    public void testQuery_Authentication(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testQuery(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            InputStream response2 = callAdapter.query(session, "solr", "?q=*:*");
            checkTrue(callAdapter.getLatestRequestUrl(), "query(...)"
                    + " returns an InputStream object", response2 != null);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void testListQueryEngines(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext()) {
            testListQueryEngines(nodeIterator.next(), version);
        }
    }

    public void testListQueryEngines(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testListQueryEngines(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            QueryEngineList response = callAdapter.listQueryEngines(session);
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

    public void testGetQueryEngineDescription(Iterator<Node> nodeIterator, String version) {
        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            testGetQueryEngineDescription(nodeIterator.next(), version);
        }
    }

    public void testGetQueryEngineDescription(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetQueryEngineDescription(...) vs. node: " + currentUrl);
        Session session = ExampleUtilities.getTestSession();

        try {
            QueryEngineList response = callAdapter.listQueryEngines(session);

            QueryEngineDescription response2 = callAdapter.getQueryEngineDescription(session,
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
