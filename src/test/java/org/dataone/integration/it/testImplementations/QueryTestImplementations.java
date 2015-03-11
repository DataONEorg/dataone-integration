package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.fail;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.util.Constants;


public class QueryTestImplementations extends ContextAwareAdapter { 
    
    
    public QueryTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("getQueryEngineDescription - tests that getQueryEngineDescription works")
    @WebTestDescription("this test uses listQueryEngines to get a QueryEngineList, "
            + "then calls getQueryEngineDescription with the first one, verifying that "
            + "it throws no exceptions. This test will also fail if listQueryEngines "
            + "returns no results.")
    /**
     * Tests that getQueryEngineDescription(foo) returns a QueryEngineDescription
     */
    public void testGetQueryEngineDescription(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetQueryEngineDescription(nodeIterator.next(), version);
    }


    public void testGetQueryEngineDescription(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            QueryEngineList qel = callAdapter.listQueryEngines(null);
            if (qel.sizeQueryEngineList() > 0) {
                QueryEngineDescription qed = callAdapter.getQueryEngineDescription(null, qel.getQueryEngine(0));
            } else {
                fail("Cannot test. There are no query engines listed");
            }
        } catch (BaseException e) {
            fail(callAdapter.getLatestRequestUrl() + " " + e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            fail(currentUrl + " " + e.getClass().getName() + ": " + e.getMessage());
        }

    }

    @WebTestName("listQueryEngines - tests that listQueryEngines works")
    @WebTestDescription("this test calls listQueryEngines and verifies that "
            + "no exceptions are thrown")
    /**
     * Tests that listQueryEngines() returns a QueryEngineList
     */    
    public void testListQueryEngines(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListQueryEngines(nodeIterator.next(), version);
    }

    /**
     * Tests that listQueryEngines() returns a QueryEngineList
     */
    public void testListQueryEngines(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListQueryEngines(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
             QueryEngineList qel = callAdapter.listQueryEngines(null);
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

}
