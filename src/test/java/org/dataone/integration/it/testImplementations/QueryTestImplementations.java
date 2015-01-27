package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.auth.CertificateManager;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.TestIterationEndingException;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1_1.QueryEngineDescription;
import org.dataone.service.types.v1_1.QueryEngineList;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.util.Constants;
//import org.dataone.integration.ExampleUtilities;


public class QueryTestImplementations extends ContextAwareAdapter { 
    
    
    public QueryTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
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
