package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeReplicationPolicy;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.Schedule;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.Synchronization;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.Property;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.TypeMarshaller;

public class NodeRegistryExtensibilityTestImplementations extends ContextAwareAdapter {

    public NodeRegistryExtensibilityTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("register - Node with extra properties")
    @WebTestDescription("this test calls creates a new Node object, adds some properties, "
            + "and calls register() with it. It then fetches the Node info with "
            + "getNodeCapabilities() and makes sure the properties match what was added.")
    public void testRegister(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegister(nodeIterator.next(), version);        
    }
    
    private void testRegister(Node node, String version) {
        
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        
        // create a new Node
        org.dataone.service.types.v2.Node newNode = new org.dataone.service.types.v2.Node();
        newNode.setBaseURL("https://fake.node.org");
        newNode.setContactSubjectList(new ArrayList<Subject>());
        newNode.setDescription("Node made for Register API testing");
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue("urn:node:TestNode");
        newNode.setName("urn:node:TestNode");
        newNode.setIdentifier(nodeRef);
        NodeReplicationPolicy replPolicy = new NodeReplicationPolicy();
        replPolicy.setAllowedNodeList(new ArrayList<NodeReference>());
        newNode.setNodeReplicationPolicy(replPolicy);
        Ping ping = new Ping();
        ping.setLastSuccess(new Date());
        ping.setSuccess(false);
        newNode.setPing(ping);
        newNode.setReplicate(false);
        Services services = new Services();
        newNode.setServices(services);
        newNode.setState(NodeState.DOWN);
        List<Subject> subjects = new ArrayList<Subject>();
        subjects.add(D1TypeBuilder.buildSubject("TestSubject"));
        newNode.setSubjectList(subjects);
        newNode.setContactSubjectList(subjects);
        Synchronization synchronization = new Synchronization();
        Schedule schedule = new Schedule();
        schedule.setHour("5");
        schedule.setMday("*");
        schedule.setMin("H");
        schedule.setMon("*");
        schedule.setSec("*");
        schedule.setWday("*");
        schedule.setYear("*");
        synchronization.setSchedule(schedule);
        newNode.setSynchronization(synchronization);
        newNode.setSynchronize(false);
        newNode.setType(NodeType.MN);
        
        // add to Node properties
        List<Property> propertyList = newNode.getPropertyList();
        Property p1 = new Property();
        p1.setKey("NodeLogo");
        p1.setValue("<(o_O)>");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setValue("Pepperoni");
        propertyList.add(p2);
        
        try {
            cn.register(null, newNode);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.register() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        
        // get the node info
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(nodeRef);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "unable to fetch updated Node capabilities!");
        } finally {
            // ideally... unregister(newNode);
        }
        
        // check if node has properties we set
        List<Property> fetchedPropertyList = fetchedNode.getPropertyList();
        assertTrue("testRegister(): fetched Node property list "
                + "should contain two properties. Number of properties: " + 
                fetchedPropertyList.size(), fetchedPropertyList.size() == 2);
        
        Property fetchedP1 = fetchedPropertyList.get(0);
        assertTrue("testRegister(): fetched Node property 1 key "
                + "should match the property we gave it.", 
                fetchedP1.getKey().equals(p1.getKey()));
        assertTrue("testRegister(): fetched Node property 1 value "
                + "should match the property we gave it.", 
                fetchedP1.getValue().equals(p1.getValue()));
        
        Property fetchedP2 = fetchedPropertyList.get(1);
        assertTrue("testRegister(): fetched Node property 2 key "
                + "should match the property we gave it.", 
                fetchedP2.getKey().equals(p2.getKey()));
        assertTrue("testRegister(): fetched Node property 2 value "
                + "should match the property we gave it.", 
                fetchedP2.getValue().equals(p2.getValue()));
    }
    
    @WebTestName("updateNodeCapabilities - add extra properties to Node")
    @WebTestDescription("this test calls takes an existing Node, adds some properties, "
            + "and calls updateNodeCapabilities(). It then fetches the Node info with "
            + "getNodeCapabilities() and makes sure the properties match what was added.")
    public void testUpdateNodeCapabilities(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateNodeCapabilities(nodeIterator.next(), version);        
    }
    
    private void testUpdateNodeCapabilities(Node node, String version) {
        
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        
        NodeList knownNodes = null;
        List<Property> oldPropertyListBackup = null;
        
        try {
            knownNodes = cn.listNodes();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "unable to perform test setup, call to CN.listNodes() failed! : "
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        List<Node> mNodes = new ArrayList<Node>();
        for (Node n : knownNodes.getNodeList())
            if (n.getType() == NodeType.MN
                    && n.getState() == NodeState.UP)
                try {
                    MNCallAdapter testMN = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                    testMN.getCapabilities();
                    mNodes.add(n);
                } catch (Exception e) {
                    // continue
                }
                
        
        assertTrue("testUpdateNodeCapabilities() requires CN.listNodes() to contain "
                + "at least one MN", mNodes.size() > 0);
        Node v1MN = mNodes.get(0);
        NodeReference mnRef = v1MN.getIdentifier();
        
        org.dataone.service.types.v2.Node v2MN = null;
        try {
            v2MN = TypeFactory.convertTypeFromType(v1MN, org.dataone.service.types.v2.Node.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "unable to convert between v1 and v2 Node types! : "
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        // add to Node properties
        List<Property> propertyList = v2MN.getPropertyList();
        if (propertyList == null)
            propertyList = new ArrayList<Property>();
        Property p1 = new Property();
        p1.setKey("NodeLogo");
        p1.setValue("<(o_O)>");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setValue("Pepperoni");
        propertyList.add(p2);
        
        // backup old node properties
        
        try {
            org.dataone.service.types.v2.Node oldNodeCapabilities = cn.getNodeCapabilities(mnRef);
            oldPropertyListBackup = oldNodeCapabilities.getPropertyList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "couldn't backup existing node property list : "
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        // update node properties on CN
        
        try {
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "failed - unable to updateNodeCapabilities() : "
                    + e.getMessage() + ", " + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        // get the node info
        
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(mnRef);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testUpdateNodeCapabilities() : "
                    + "unable to fetch updated Node capabilities!");
        }
        
        // reset properties we changed on the node
        try {
            v2MN.setPropertyList(oldPropertyListBackup);
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getNodeBaseServiceUrl(), "testUpdateNodeCapabilities() : "
                    + "Unable to reset Node properties on: " + cn.getNodeBaseServiceUrl() 
                    + " to backed-up properties! : " + e.getMessage() + ", " 
                    + e.getCause() == null ? "" : e.getCause().getMessage());
        }
        
        // check if node is updated
        
        List<Property> fetchedPropertyList = fetchedNode.getPropertyList();
        assertTrue("testUpdateNodeCapabilities(): fetched Node property list "
                + "should not be null", fetchedPropertyList != null);
        assertTrue("testUpdateNodeCapabilities(): fetched Node property list "
                + "should contain two properties. Number of properties: " + 
                fetchedPropertyList.size(), fetchedPropertyList.size() == 2);
        
        Property fetchedP1 = fetchedPropertyList.get(0);
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 1 key "
                + "should match the property we gave it.", 
                fetchedP1.getKey().equals(p1.getKey()));
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 1 value "
                + "should match the property we gave it.", 
                fetchedP1.getValue().equals(p1.getValue()));
        
        Property fetchedP2 = fetchedPropertyList.get(1);
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 2 key "
                + "should match the property we gave it.", 
                fetchedP2.getKey().equals(p2.getKey()));
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 2 value "
                + "should match the property we gave it.", 
                fetchedP2.getValue().equals(p2.getValue()));
    }
    
}
