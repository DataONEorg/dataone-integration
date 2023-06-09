package org.dataone.integration.it.testImplementations;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;

public class CNRegisterTestImplementations extends ContextAwareAdapter {

    private String cnSubmitter = Settings.getConfiguration().getString(
            "dataone.it.cnode.submitter.cn", /* default */"cnDevUNM1");

    public CNRegisterTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    public void testUpdateNodeCapabilities(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities(nodeIterator.next(), version);
    }

    //    @Ignore("confirm the change to the Node record")
    public void testUpdateNodeCapabilities(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        // TODO: set the appropriate subject - will need a subject that can
        // update its node record on the CN.  either an MN or CN subject.
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> cNodeList = selectNodes(callAdapter.listNodes(), NodeType.CN);
            if (cNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test updateNodeCapabilities unless there is a node in the NodeList");
            } else {
                Node node0 = cNodeList.get(0);
                NodeReference nodeRef = node0.getIdentifier();

                Ping ping = node0.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node0.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(node0, org.dataone.service.types.v2.Node.class);
                boolean response = callAdapter.updateNodeCapabilities(null, nodeRef, nodeV2);
                checkTrue(callAdapter.getLatestRequestUrl(),
                        "response cannot be false. [Only true or exception].", response);

                // TODO: confirm the changed node record.  currently cannot do this as the node update
                // process is not automatic.
            }
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("updateNodeCapabilities - test for nonexistent node")
    @WebTestDescription("this tests a negative case by calling updateNodeCapabilities with "
            + "an invalid NodeReference, expecting a NotFound exception")
    public void testUpdateNodeCapabilities_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities_NotFound(nodeIterator.next(), version);
    }

    public void testUpdateNodeCapabilities_NotFound(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        Subject clientSubject = ContextAwareTestCaseDataone.setupClientSubject(cnSubmitter);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> cNodeList = selectNodes(callAdapter.listNodes(), NodeType.CN);
            if (cNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test updateNodeCapabilities unless there is a node in the NodeList");
            } else {
                Node node0 = cNodeList.get(0);
                NodeReference nodeRef = node0.getIdentifier();
                nodeRef.setValue(nodeRef.getValue() + "BAD");
                node0.setIdentifier(nodeRef);
                node0.addSubject(clientSubject);

                Ping ping = node0.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node0.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(node0, org.dataone.service.types.v2.Node.class);
                boolean response = callAdapter.updateNodeCapabilities(null, nodeRef, nodeV2);
                handleFail(callAdapter.getLatestRequestUrl(),
                        "updateNodeCapabilities on fictitious node should fail");
            }
        } catch (NotFound e) {
            // this is the expected behavior
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "expected fail with NotFound. Got: " + e.getClass() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("updateNodeCapabilities - test with unauthorized subject")
    @WebTestDescription("this tests a negative case by calling updateNodeCapabilities with "
            + "an unauthorized certificate, expecting a NotAuthorized exception")
    public void testUpdateNodeCapabilities_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities_NotAuthorized(nodeIterator.next(), version);
    }

    public void testUpdateNodeCapabilities_NotAuthorized(Node node, String version) {

        //TODO ensure that the current subject is not able to update the node record
        // do this by looking at the node record?
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> cNodeList = selectNodes(callAdapter.listNodes(), NodeType.CN);
            if (cNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test updateNodeCapabilities unless there is a node in the NodeList");
            } else {
                Node node0 = cNodeList.get(0);
                NodeReference nodeRef = node0.getIdentifier();

                Ping ping = node0.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node0.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(node0, org.dataone.service.types.v2.Node.class);
                boolean response = callAdapter.updateNodeCapabilities(null, nodeRef, nodeV2);
                handleFail(callAdapter.getLatestRequestUrl(),
                        "updateNodeCapabilities with unauthorized subject should fail");

                // TODO: confirm the changed node record.  currently cannot do this as the node update
                // process is not automatic.

            }
        } catch (NotAuthorized e) {
            // this is the expected behavior
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "expected fail with NotAuthorized. Got: "
                    + e.getClass() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("updateNodeCapabilities - test updating other fields")
    @WebTestDescription("this tests a negative case by calling updateNodeCapabilities "
            + "and trying to set fields other than the node capabilities, "
            + "expecting an InvalidRequest exception")
    public void testUpdateNodeCapabilities_updatingOtherField(Iterator<Node> nodeIterator,
            String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities_updatingOtherField(nodeIterator.next(), version);
    }

    //    @Ignore("not many values it will refuse change on")
    public void testUpdateNodeCapabilities_updatingOtherField(Node node, String version) {
        // TODO: set the appropriate subject - will need a subject that can
        // update it's node record on the CN.  either an MN or CN subject.
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> cNodeList = selectNodes(callAdapter.listNodes(), NodeType.CN);
            if (cNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test updateNodeCapabilities unless there is a node in the NodeList");
            } else {
                Node node0 = cNodeList.get(0);
                node0.setDescription(node.getDescription()
                        + " Tier2 updateNodeCapabilities_updatingOtherField test");
                NodeReference nodeRef = node0.getIdentifier();

                Ping ping = node0.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node0.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(node0, org.dataone.service.types.v2.Node.class);
                boolean response = callAdapter.updateNodeCapabilities(null, nodeRef, nodeV2);
                handleFail(callAdapter.getLatestRequestUrl(),
                        "updateNodeCapabilities to update other fields should fail");
            }
        } catch (InvalidRequest e) {
            // this is the expected behavior
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "expected fail with InvalidRequest. Got: " + e.getClass() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("register - test that register works with a valid node")
    @WebTestDescription("tests that calling register with a "
            + "valid Node will return a non-null NodeReference")
    public void testRegister(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegister(nodeIterator.next(), version);
    }

    //    @Ignore("don't want to keep creating new phantom nodes")
    public void testRegister(Node node, String version) {
        // TODO: set the appropriate subject - will need a subject that can
        // create a node record.  
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testRegister(...) vs. node: " + currentUrl);

        try {
            List<Node> mNodeList = selectNodes(callAdapter.listNodes(), NodeType.MN);
            if (mNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test cn.register() unless there is a Member Node in the NodeList");
            } else {
                Node mNode = mNodeList.get(0);
                String nr = mNode.getIdentifier().getValue();
                NodeReference newRef = new NodeReference();
                newRef.setValue(nr + "abcdefghij");
                mNode.setIdentifier(newRef);
                mNode.setBaseURL(mNode.getBaseURL() + "/fakeBaseUrlThatIsDifferent");
                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(mNode, org.dataone.service.types.v2.Node.class);
                NodeReference response = callAdapter.register(null, nodeV2);
                checkTrue(callAdapter.getLatestRequestUrl(),
                        "register(...) returns a NodeReference object", response != null);
            }
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("register - test with a non-unique identifier")
    @WebTestDescription("tests a negative case by calling register with a "
            + "Node that is not unique and has already been registered, "
            + "expecting a IdentifierNotUnique exception")
    public void testRegister_IdentifierNotUnique(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegister_IdentifierNotUnique(nodeIterator.next(), version);
    }

    public void testRegister_IdentifierNotUnique(Node node, String version) {
        // TODO: set the appropriate subject - will need a subject that can
        // create a node record.  
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testRegister(...) vs. node: " + currentUrl);

        try {
            List<Node> mNodeList = selectNodes(callAdapter.listNodes(), NodeType.MN);
            if (mNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test cn.register() unless there is a Member Node in the NodeList");
            } else {
                // attempt to re-register a node...
                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(mNodeList.get(0), org.dataone.service.types.v2.Node.class);
                NodeReference response = callAdapter.register(null, nodeV2);
                handleFail(callAdapter.getLatestRequestUrl(),
                        "register(...) should throw IndentifierNotUnique exception when"
                                + " trying to re-register a node (same Identifer)");
            }
        } catch (IdentifierNotUnique e) {
            // this is expected outcome
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "expected fail with InvalidRequest. Got: " + e.getClass() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private List<Node> selectNodes(NodeList nl, NodeType nodeType) {
        List<Node> nodes = new ArrayList<Node>();
        for (int i = 0; i < nl.sizeNodeList(); i++) {
            Node node = nl.getNode(i);
            if (nodeType == null) {
                nodes.add(node);
            } else if (node.getType() == nodeType) {
                nodes.add(node);
            }
        }
        return nodes;
    }
    
    @WebTestName("getNodeCapabilities - test that getNodeCapabilities call works")
    @WebTestDescription("this test just calls getNodeCapabilities with an MN reference "
            + "and verifies that it gets back a non-null Node containing a non-null Services object")
    public void testGetNodeCapabilities(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetNodeCapabilities(nodeIterator.next(), version);
    }

    public void testGetNodeCapabilities(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> mNodeList = selectNodes(callAdapter.listNodes(), NodeType.MN);
            if (mNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test cn.getNodeCapabilities() unless there is a Member Node in the NodeList");
            } else {
                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(mNodeList.get(0), org.dataone.service.types.v2.Node.class);
                NodeReference nodeRef = nodeV2.getIdentifier();
                org.dataone.service.types.v2.Node capabilities = callAdapter.getNodeCapabilities(nodeRef);
                Assert.assertTrue("getNodeCapabilities call should return a non-null Node", capabilities != null);
                
                Services services = capabilities.getServices();
                Assert.assertTrue("getNodeCapabilities call should return a Node containing non-null Services definition", services != null);                
            }
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(), e.getClass() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getNodeCapabilities - test that getNodeCapabilities fails for an invalid Node")
    @WebTestDescription("this test just calls getNodeCapabilities with an invalid MN reference "
            + "and expects a NotFound exception")
    public void testGetNodeCapabilities_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetNodeCapabilities_NotFound(nodeIterator.next(), version);
    }

    public void testGetNodeCapabilities_NotFound(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> mNodeList = selectNodes(callAdapter.listNodes(), NodeType.MN);
            if (mNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test cn.getNodeCapabilities() unless there is a Member Node in the NodeList");
            } else {
                org.dataone.service.types.v2.Node nodeV2 = TypeFactory.convertTypeFromType(mNodeList.get(0), org.dataone.service.types.v2.Node.class);
                NodeReference nodeRef = new NodeReference();
                nodeRef.setValue("urn:node:bogus");
                callAdapter.getNodeCapabilities(nodeRef);
                handleFail(callAdapter.getLatestRequestUrl(), "Expected getNodeCapabilities() to fail with NotFound if given an invalid Node reference.");          
            }
        } catch (NotFound e) {
            // expected
        } catch (BaseException e) {
            handleFail( callAdapter.getLatestRequestUrl(), "expected fail with NotFound. Got: " + e.getClass() + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "expected fail with NotFound. Got: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
}
