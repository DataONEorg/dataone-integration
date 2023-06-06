package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeReplicationPolicy;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.Schedule;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.Synchronization;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.Property;
import org.dataone.service.util.TypeMarshaller;

public class NodeRegistryExtensibilityTestImplementations extends ContextAwareAdapter {

    public NodeRegistryExtensibilityTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("register - Node with extra properties")
    @WebTestDescription("this test calls creates a new Node object, adds some properties, "
            + "(leaving the optional Property.type null) and calls register() with it. "
            + "It then fetches the Node info with getNodeCapabilities() and makes sure "
            + "the properties match what was added.")
    public void testRegister_NoPropType(Iterator<Node> nodeIterator, String version) {
        if (nodeIterator.hasNext()) // all CNs share LDAP data
            testRegister_NoPropType(nodeIterator.next(), version);  
        else
            throw new AssertionError("No CN to test against!");
    }
    
    private void testRegister_NoPropType(Node node, String version) {
        
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        
        SubjectInfo verifiedSubjects = null;
        try {
            verifiedSubjects = cn.listSubjects(null, null, "verified", null, null);
        } catch (BaseException | ClientSideException e) {
            throw new AssertionError("Unable to fetch verified subjects on " + cn.getNodeBaseServiceUrl(), e);
        }
        if (verifiedSubjects.getPersonList() == null || verifiedSubjects.getPersonList().size() == 0)
            throw new AssertionError("No verified subjects on " + cn.getNodeBaseServiceUrl());
        
        // create a new Node
        org.dataone.service.types.v2.Node newNode = new org.dataone.service.types.v2.Node();
        newNode.setBaseURL("https://fake.node.org");
        newNode.setContactSubjectList(new ArrayList<Subject>());
        newNode.setDescription("Node made for Register API testing");
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue("urn:node:TestNode");
        newNode.setName("urn:node:TestNode");
        newNode.setIdentifier(nodeRef);
        Ping ping = new Ping();
        ping.setLastSuccess(new Date());
        ping.setSuccess(true);
        newNode.setPing(ping);
        newNode.setReplicate(true);
        newNode.setState(NodeState.DOWN);
        List<Subject> subjects = new ArrayList<Subject>();
        Subject subj = verifiedSubjects.getPerson(0).getSubject();
        subjects.add(subj);
        newNode.setSubjectList(subjects);
        newNode.setContactSubjectList(subjects);
        Synchronization synchronization = new Synchronization();
        Schedule schedule = new Schedule();
        schedule.setHour("*");
        schedule.setMday("*");
        schedule.setMin("0/3");
        schedule.setMon("*");
        schedule.setSec("10");
        schedule.setWday("?");
        schedule.setYear("*");
        synchronization.setSchedule(schedule);
        newNode.setSynchronization(synchronization);
        newNode.setSynchronize(false);
        newNode.setType(NodeType.MN);
        Services services = new Services();
        Service service = new Service();
        service.setName("MNCore");
        service.setVersion("v2");
        service.setAvailable(true);
        services.addService(service);
        newNode.setServices(services);
        
        // add to Node properties
        List<Property> propertyList = newNode.getPropertyList();
        Property p1 = new Property();
        p1.setKey("NodeLogo");
        p1.setValue("(o_O)");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setValue("Pepperoni");
        propertyList.add(p2);

        
        try {
            log.info("Attempting to register new node: " + newNode.getName() + " (" + 
                    newNode.getBaseURL() + ") with CN " + cn.getNodeBaseServiceUrl());
            cn.register(null, newNode);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.register() call failed to register new node with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.register() call failed to register new node with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // get the node info
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(nodeRef);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.getNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.getNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        } finally {
            // ideally... unregister(newNode);
        }
        
        // check if node has properties we set
        List<Property> fetchedPropertyList = fetchedNode.getPropertyList();
        assertTrue("testRegister(): fetched Node property list "
                + "should contain only two properties. Number of properties: " + 
                fetchedPropertyList.size(), fetchedPropertyList.size() == 2);
        
        Property fetchedP1 = fetchedPropertyList.get(0);
        assertTrue("testRegister(): fetched Node property 1 key "
                + "should match the property we gave it.", 
                fetchedP1.getKey().equals(p1.getKey()));
        assertTrue("testRegister(): fetched Node property 1 type "
                + "should match the type we gave it (null).", 
                fetchedP1.getType() == null);
        assertTrue("testRegister(): fetched Node property 1 value "
                + "should match the property we gave it.", 
                fetchedP1.getValue().equals(p1.getValue()));
        
        Property fetchedP2 = fetchedPropertyList.get(1);
        assertTrue("testRegister(): fetched Node property 2 key "
                + "should match the property we gave it.", 
                fetchedP2.getKey().equals(p2.getKey()));
        assertTrue("testRegister(): fetched Node property 2 type "
                + "should match the type we gave it (null).", 
                fetchedP2.getType() == null);
        assertTrue("testRegister(): fetched Node property 2 value "
                + "should match the property we gave it.", 
                fetchedP2.getValue().equals(p2.getValue()));
    }
    
    
    @WebTestName("register - Node with extra properties")
    @WebTestDescription("this test calls creates a new Node object, adds some properties, "
            + "and calls register() with it. It then fetches the Node info with "
            + "getNodeCapabilities() and makes sure the properties match what was added.")
    public void testRegister(Iterator<Node> nodeIterator, String version) {
        if (nodeIterator.hasNext()) // all CNs share LDAP data
            testRegister(nodeIterator.next(), version);  
        else
            throw new AssertionError("No CN to test against!");
    }
    
    private void testRegister(Node node, String version) {
        
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        
        SubjectInfo verifiedSubjects = null;
        try {
            verifiedSubjects = cn.listSubjects(null, null, "verified", null, null);
        } catch (BaseException | ClientSideException e) {
            throw new AssertionError("Unable to fetch verified subjects on " + cn.getNodeBaseServiceUrl(), e);
        }
        if (verifiedSubjects.getPersonList() == null || verifiedSubjects.getPersonList().size() == 0)
            throw new AssertionError("No verified subjects on " + cn.getNodeBaseServiceUrl());
        
        // create a new Node
        org.dataone.service.types.v2.Node newNode = new org.dataone.service.types.v2.Node();
        newNode.setBaseURL("https://fake.node.org");
        newNode.setContactSubjectList(new ArrayList<Subject>());
        newNode.setDescription("Node made for Register API testing");
        NodeReference nodeRef = new NodeReference();
        nodeRef.setValue("urn:node:TestNode");
        newNode.setName("urn:node:TestNode");
        newNode.setIdentifier(nodeRef);
        Ping ping = new Ping();
        ping.setLastSuccess(new Date());
        ping.setSuccess(true);
        newNode.setPing(ping);
        newNode.setReplicate(true);
        newNode.setState(NodeState.DOWN);
        List<Subject> subjects = new ArrayList<Subject>();
        Subject subj = verifiedSubjects.getPerson(0).getSubject();
        subjects.add(subj);
        newNode.setSubjectList(subjects);
        newNode.setContactSubjectList(subjects);
        Synchronization synchronization = new Synchronization();
        Schedule schedule = new Schedule();
        schedule.setHour("*");
        schedule.setMday("*");
        schedule.setMin("0/3");
        schedule.setMon("*");
        schedule.setSec("10");
        schedule.setWday("?");
        schedule.setYear("*");
        synchronization.setSchedule(schedule);
        newNode.setSynchronization(synchronization);
        newNode.setSynchronize(false);
        newNode.setType(NodeType.MN);
        Services services = new Services();
        Service service = new Service();
        service.setName("MNCore");
        service.setVersion("v2");
        service.setAvailable(true);
        services.addService(service);
        newNode.setServices(services);
        
        // add to Node properties
        List<Property> propertyList = newNode.getPropertyList();
        Property p1 = new Property();
        p1.setKey("NodeLogo");
        p1.setType("propType");
        p1.setValue("(o_O)");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setType("propType");
        p2.setValue("Pepperoni");
        propertyList.add(p2);
        
        try {
            log.info("Attempting to register new node: " + newNode.getName() + " (" + 
                    newNode.getBaseURL() + ") with CN " + cn.getNodeBaseServiceUrl());
            cn.register(null, newNode);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.register() call failed to register new node with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.register() call failed to register new node with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // get the node info
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(nodeRef);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.getNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testRegister() : "
                    + "CN.getNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
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
        assertTrue("testRegister(): fetched Node property 1 type "
                + "should match the type we gave it.", 
                fetchedP1.getType().equals(p1.getType()));
        assertTrue("testRegister(): fetched Node property 1 value "
                + "should match the property we gave it.", 
                fetchedP1.getValue().equals(p1.getValue()));
        
        Property fetchedP2 = fetchedPropertyList.get(1);
        assertTrue("testRegister(): fetched Node property 2 key "
                + "should match the property we gave it.", 
                fetchedP2.getKey().equals(p2.getKey()));
        assertTrue("testRegister(): fetched Node property 2 type "
                + "should match the type we gave it.", 
                fetchedP2.getType().equals(p2.getType()));
        assertTrue("testRegister(): fetched Node property 2 value "
                + "should match the property we gave it.", 
                fetchedP2.getValue().equals(p2.getValue()));
    }
    
    @WebTestName("updateNodeCapabilities - add extra properties to Node")
    @WebTestDescription("this test calls takes an existing Node, adds some properties, "
            + "and calls updateNodeCapabilities(). It then fetches the Node info with "
            + "getNodeCapabilities() and makes sure the properties match what was added.")
    public void testUpdateNodeCapabilities(Iterator<Node> nodeIterator, String version) {
        if (nodeIterator.hasNext()) // all CNs share LDAP data
            testUpdateNodeCapabilities(nodeIterator.next(), version);
        else
            throw new AssertionError("No CN to test against!");
    }
    
    private void testUpdateNodeCapabilities(Node node, String version) {
        
        if (!catc.nodeListContainsV2Mn())
            return;
            
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
        List<org.dataone.service.types.v2.Node> v2MNs = new ArrayList<org.dataone.service.types.v2.Node>();
        for (Node n : knownNodes.getNodeList())
            if (n.getType() == NodeType.MN
                    && n.getState() == NodeState.UP)
                try {
                    MNCallAdapter testMN = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                    Node capabilities = testMN.getCapabilities();
                    if (capabilities instanceof org.dataone.service.types.v2.Node == false)
                        throw new AssertionError("v2 getCapabilities() should not be returning v1 Node types");
                    for (Service service : capabilities.getServices().getServiceList()) {
                        if (service.getName().equalsIgnoreCase("MNCore") && service.getVersion().equalsIgnoreCase("v2")) {
                            v2MNs.add((org.dataone.service.types.v2.Node) capabilities);
                            break;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
        
        assertTrue("testUpdateNodeCapabilities() requires CN.listNodes() to contain "
                + "at least one v2 MN since it tests the ability to save modifications to a Node's "
                + "property list (which is v2 Node only).", v2MNs.size() > 0);
        
        org.dataone.service.types.v2.Node v2MN = v2MNs.get(0);
        NodeReference mnRef = v2MN.getIdentifier();
        log.warn("testing with node: " + mnRef.getValue());
        
        // add to Node properties
        List<Property> propertyList = v2MN.getPropertyList();
        if (propertyList == null)
            propertyList = new ArrayList<Property>();
        Property p1 = new Property();
        p1.setKey("NodeLogo");
        p1.setType("propType");
        p1.setValue("(o_O)");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setType("propType");
        p2.setValue("Pepperoni");
        propertyList.add(p2);
        v2MN.setPropertyList(propertyList);
        // backup old node properties
        
        try {
            org.dataone.service.types.v2.Node oldNodeCapabilities = cn.getNodeCapabilities(mnRef);
            oldPropertyListBackup = oldNodeCapabilities.getPropertyList();
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getPropertyList() call failed to get old properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getPropertyList() call failed to get old properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // update node properties on CN
        
        try {
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // get the node info
        
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(mnRef);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getNodeCapabilities() call failed to get updated info with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getNodeCapabilities() call failed to get updated info with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        if (fetchedNode.getPropertyList() != null) {
        	log.warn("fetchedNode has PropertyList size: " + fetchedNode.getPropertyList().size());
        } else {
        	log.warn("fetchedNode has null property list: " + fetchedNode.getPropertyList());

        }
        
        // reset properties we changed on the node
        try {
            v2MN.setPropertyList(oldPropertyListBackup);
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed to reset properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed to reset properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
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
   
    @WebTestName("updateNodeCapabilities - Cannot add 'CN_' prefixed properties to Node")
    @WebTestDescription("this test tries to add properties that begin with the reserved prefix 'CN_'."
            + " The call to updateNodeCapabilities should succeed, but the properties not added.")
    public void testUpdateNodeCapabilities_CannotAddCnProps(Iterator<Node> nodeIterator, String version) {
        if (nodeIterator.hasNext()) // all CNs share LDAP data
            testUpdateNodeCapabilities_CannotAddCnProps(nodeIterator.next(), version);
        else
            throw new AssertionError("No CN to test against!");
    }
    
    private void testUpdateNodeCapabilities_CannotAddCnProps(Node node, String version) {
        
        if (!catc.nodeListContainsV2Mn())
            return;
            
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
        List<org.dataone.service.types.v2.Node> v2MNs = new ArrayList<org.dataone.service.types.v2.Node>();
        for (Node n : knownNodes.getNodeList())
            if (n.getType() == NodeType.MN
                    && n.getState() == NodeState.UP)
                try {
                    MNCallAdapter testMN = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                    Node capabilities = testMN.getCapabilities();
                    if (capabilities instanceof org.dataone.service.types.v2.Node == false)
                        throw new AssertionError("v2 getCapabilities() should not be returning v1 Node types");
                    for (Service service : capabilities.getServices().getServiceList()) {
                        if (service.getName().equalsIgnoreCase("MNCore") && service.getVersion().equalsIgnoreCase("v2")) {
                            v2MNs.add((org.dataone.service.types.v2.Node) capabilities);
                            break;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
        
        assertTrue("testUpdateNodeCapabilities() requires CN.listNodes() to contain "
                + "at least one v2 MN since it tests the ability to save modifications to a Node's "
                + "property list (which is v2 Node only).", v2MNs.size() > 0);
        
        org.dataone.service.types.v2.Node v2MN = v2MNs.get(0);
        NodeReference mnRef = v2MN.getIdentifier();
        log.warn("testing with node: " + mnRef.getValue());
        
        // add to Node properties
        List<Property> propertyList = v2MN.getPropertyList();
        if (propertyList == null)
            propertyList = new ArrayList<Property>();
        Property p1 = new Property();
        p1.setKey("CN_fromUpdateNodeCapabilities");
        p1.setType("propType");
        p1.setValue("this property should not have been added");
        propertyList.add(p1);
        Property p2 = new Property();
        p2.setKey("NodeTopping");
        p2.setType("propType");
        p2.setValue("Pepperoni");
        propertyList.add(p2);
        v2MN.setPropertyList(propertyList);
        // backup old node properties
        
        try {
            org.dataone.service.types.v2.Node oldNodeCapabilities = cn.getNodeCapabilities(mnRef);
            oldPropertyListBackup = oldNodeCapabilities.getPropertyList();
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getPropertyList() call failed to get old properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getPropertyList() call failed to get old properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // update node properties on CN
        
        try {
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // get the node info
        
        org.dataone.service.types.v2.Node fetchedNode = null;
        try {
            fetchedNode = cn.getNodeCapabilities(mnRef);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getNodeCapabilities() call failed to get updated info with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.getNodeCapabilities() call failed to get updated info with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        if (fetchedNode.getPropertyList() != null) {
            log.warn("fetchedNode has PropertyList size: " + fetchedNode.getPropertyList().size());
        } else {
            log.warn("fetchedNode has null property list: " + fetchedNode.getPropertyList());

        }
        
        // reset properties we changed on the node
        try {
            v2MN.setPropertyList(oldPropertyListBackup);
            cn.updateNodeCapabilities(null, mnRef, v2MN);
        } catch (BaseException e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed to reset properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getDetail_code() + " : " 
                    + e.getDescription() + " : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AssertionError(cn.getNodeBaseServiceUrl() + ":   "
                    + "testUpdateNodeCapabilities() : "
                    + "CN.updateNodeCapabilities() call failed to reset properties with exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // check if node is updated
        
        List<Property> fetchedPropertyList = fetchedNode.getPropertyList();
        assertTrue("testUpdateNodeCapabilities(): fetched Node property list "
                + "should not be null", fetchedPropertyList != null);
        assertTrue("testUpdateNodeCapabilities(): fetched Node property list "
                + "should contain 1 property. Number of properties: " + 
                fetchedPropertyList.size(), fetchedPropertyList.size() == 1);
        

        Property fetchedP2 = fetchedPropertyList.get(0);
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 2 key "
                + "should match the property we gave it.", 
                fetchedP2.getKey().equals(p2.getKey()));
        assertTrue("testUpdateNodeCapabilities(): fetched Node property 2 value "
                + "should match the property we gave it.", 
                fetchedP2.getValue().equals(p2.getValue()));
    }

    
    
}
