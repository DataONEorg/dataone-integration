package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.auth.CertificateManager;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.Constants;
//import org.dataone.integration.ExampleUtilities;


public class CoreTestImplementations extends ContextAwareAdapter { 
    
    
    public CoreTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    /**
     * Will test the ping call for all nodes. Requires an iterator to go through
     * all the nodes (this may iterate across either MN or CN nodes). Also requires
     * a version string so it knows against which version of API it should test ping. 
     * 
     * @param nodeIterator 
     *      an {@link Iterator} accross MN or CN {@link Node}s
     * @param version 
     *      either "v1" or "v2", to match the API version being tested
     */
    public void testPing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testPing(nodeIterator.next(), version);
    }

    /**
     * Will run the ping command and test for proper execution.
     * Makes use of {@link CommonCallAdapter} to call ping on the correct node type
     * (MN or CN) against the correct API version.
     * @param node 
     * @param version
     */
    public void testPing(Node node, String version) {

    	ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            //          Assume.assumeTrue(APITestUtils.isTierImplemented(mn, "Tier5"));
            Date localNow = new Date();
            Date pingDate = callAdapter.ping();

            assertTrue(callAdapter.getLatestRequestUrl() + " ping should return a valid date", pingDate != null);
            // other invalid dates will be thrown as IOExceptions cast to ServiceFailures

            assertTrue(callAdapter.getLatestRequestUrl()
                    + " returned date should be within 1 minute of time measured on test machine", pingDate.getTime()
                    - localNow.getTime() < 1000 * 60
                    && localNow.getTime() - pingDate.getTime() > -1000 * 60);

        } catch (BaseException e) {
            fail(callAdapter.getLatestRequestUrl() + " " + e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            fail(currentUrl + " " + e.getClass().getName() + ": " + e.getMessage());
        }

    }

    

    public void testGetCapabilities(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetCapabilities(nodeIterator.next(), version);
    }

    public void testGetCapabilities(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetCapabilities() vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Node capabilitiesNode = callAdapter.getCapabilities();
            checkTrue(callAdapter.getLatestRequestUrl(), "getCapabilities returns a Node", capabilitiesNode != null);
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }

    }
    
    public void testGetCapabilities_HasCompatibleNodeContact(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetCapabilities_HasCompatibleNodeContact(nodeIterator.next(), version);
    }
    
    /**
     * Tests that at least one of the node contacts is RFC2253 compatible, 
     * meaning that it could be represented by a CILogon issued certificate
     */
    public void testGetCapabilities_HasCompatibleNodeContact(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetCapabilities() vs. node: " + currentUrl);

        try {
            Node capabilitiesNode = callAdapter.getCapabilities();
            checkTrue(callAdapter.getLatestRequestUrl(), "getCapabilities returns a Node", capabilitiesNode != null);

            List<Subject> contacts = capabilitiesNode.getContactSubjectList();
            boolean found = false;
            if (contacts != null) {
                for (Subject s : contacts) {
                    try {
                        CertificateManager.getInstance().standardizeDN(s.getValue());
                        found = true;
                    } catch (IllegalArgumentException e) {
                        ; // this can happen legally, but means that it is not actionable
                    }
                }
            }
            checkTrue(callAdapter.getLatestRequestUrl(),
                    "the node should have at least one contactSubject that conforms to RFC2253.", found);

        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public void testGetCapabilities_NodeIdentityValidFormat(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetCapabilities_NodeIdentityValidFormat(nodeIterator.next(), version);
    }
    
    /**
     * Tests that the nodeReference of the node is in the proper urn format.
     */
    public void testGetCapabilities_NodeIdentityValidFormat(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetCapabilities() vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Node capabilitiesNode = callAdapter.getCapabilities();
            checkTrue(callAdapter.getLatestRequestUrl(), "getCapabilities returns a Node", capabilitiesNode != null);

            NodeReference nodeRef = capabilitiesNode.getIdentifier();
            checkTrue(callAdapter.getLatestRequestUrl(),
                    "the node identifier should conform to specification 'urn:node:[\\w_]{2,23}'", nodeRef.getValue()
                            .matches("^urn:node:[\\w_]{2,23}"));

        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
