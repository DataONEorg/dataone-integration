package org.dataone.integration.it.testImplementations;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.NodeList;
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
                NodeReference nodeRef = node.getIdentifier();

                Ping ping = node.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
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
                NodeReference nodeRef = node.getIdentifier();
                nodeRef.setValue(nodeRef.getValue() + "bizzBazzBuzz");
                node.setIdentifier(nodeRef);
                node.addSubject(clientSubject);

                Ping ping = node.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
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

    public void testUpdateNodeCapabilities_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities_NotAuthorized(nodeIterator.next(), version);
    }

    public void testUpdateNodeCapabilities_NotAuthorized(Node node, String version) {

        //TODO ensure that the current subject is not able to update the node record
        // do this by looking at the node record?
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

        try {
            List<Node> cNodeList = selectNodes(callAdapter.listNodes(), NodeType.CN);
            if (cNodeList.isEmpty()) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Cannot test updateNodeCapabilities unless there is a node in the NodeList");
            } else {
                NodeReference nodeRef = node.getIdentifier();

                Ping ping = node.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
                boolean response = callAdapter.updateNodeCapabilities(null, nodeRef, nodeV2);
                handleFail(callAdapter.getLatestRequestUrl(),
                        "updateNodeCapabilities on fictitious node should fail");

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
                node.setDescription(node.getDescription()
                        + " Tier2 updateNodeCapabilities_updatingOtherField test");
                NodeReference nodeRef = node.getIdentifier();

                Ping ping = node.getPing();
                if (ping == null)
                    ping = new Ping();
                Date orginalLastSuccess = ping.getLastSuccess();
                ping.setLastSuccess(new Date());
                node.setPing(ping);

                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
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
                String nr = node.getIdentifier().getValue();
                NodeReference newRef = new NodeReference();
                newRef.setValue(nr + "abcdefghij");
                node.setIdentifier(newRef);
                node.setBaseURL(node.getBaseURL() + "/fakeBaseUrlThatIsDifferent");
                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
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
                org.dataone.service.types.v2.Node nodeV2 = TypeMarshaller.convertTypeFromType(node, org.dataone.service.types.v2.Node.class);
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

}
