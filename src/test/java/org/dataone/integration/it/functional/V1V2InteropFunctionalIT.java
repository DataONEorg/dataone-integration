package org.dataone.integration.it.functional;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.testImplementations.SystemMetadataFunctionalTestImplementation;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class V1V2InteropFunctionalIT extends ContextAwareTestCaseDataone {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs metadata management scenarios including v1 and v2 nodes.";
    }

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private CNCallAdapter cn;
    /** MNs supporting ONLY the V1 API */
    private List<Node> v1mns;
    /** MNs supporting the V2 API (might also support V1 API) */
    private List<Node> v2mns;
    /** MNs supporting BOTH the V1 & V2 APIs */
    private List<Node> v1v2mns;
    
    private static final long SYNC_TIME = 300000;           // FIXME is there a reliable way to know these?
    private static final long REPLICATION_TIME = 300000;    // FIXME "
    
    private static Log log = LogFactory.getLog(SystemMetadataFunctionalTestImplementation.class);
    
    @Before
    public void setup() {
        List<Node> cnList = new ArrayList<Node>();
        List<Node> mnList = new ArrayList<Node>();
        v1mns = new ArrayList<Node>();
        v2mns = new ArrayList<Node>();
        v1v2mns = new ArrayList<Node>();
        
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        if(cnIter != null)
            cnList = IteratorUtils.toList(cnIter);
        
        if(cnList.size() > 0)
            cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        if(cn != null) {
            try {
                for(Node n : cn.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (NotImplemented | ServiceFailure | InstantiationException
                    | IllegalAccessException | InvocationTargetException | ClientSideException
                    | JiBXException | IOException e) {
                log.warn("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
            }
        }
        
        for (Node mNode : mnList) {
            MNCallAdapter v1mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
            MNCallAdapter v2mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
            
            boolean v1support = false;
            boolean v2support = false;
            
            // TODO is there a more reliable way to check this?
            try {
                v1mn.ping();        // ping v1 endpoint
                v1support = true;
            } catch (Exception e1) {}
            
            try {
                v2mn.ping();        // ping v2 endpoint
                v2support = true;
            } catch (Exception e1) {}

            if (v1support && !v2support)
                v1mns.add(mNode);
            if (v2support)
                v2mns.add(mNode);
            if (v1support && v2support)
                v1v2mns.add(mNode);
        }

        log.info("v1 MNs available: " + v1mns.size());
        log.info("v2 MNs available: " + v2mns.size());

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
    }
    
    /**
     * Test operates on a single MN that supports both v1 & v2 APIs.
     * It does a create on the v2 endpoint, then an updateSystemMetadata on the v1 endpoint.
     * The updateSystemMetadata operation should yield an exception since it could be erasing
     * v2-specific data from the create. 
     */
    @Test
    public void testV2CreateV1UpdateSysmeta() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node mNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1UpdateSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v1 updateSystemMetadata
        
        SystemMetadata sysmeta = null;
        try {
            sysmeta = v2CallAdapter.getSystemMetadata(null, pid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            v1CallAdapter.updateSystemMetadata(null, pid, sysmeta);
        } catch (InvalidRequest e) {
            // expected v1 updateSystemMetadata() to fail
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() expected InvalidRequest, got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1UpdateSysmeta() expected InvalidRequest, got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on a single MN that supports both v1 & v2 APIs.
     * It does a create on the v2 endpoint, then an update on the v1 endpoint.
     * The update operation should yield an exception since it could be erasing
     * v2-specific data from the create. 
     */
    @Test
    public void testV2CreateV1Update() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node mNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1Update_1_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v1 update
        
        Identifier newPid = null;
        SystemMetadata sysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV2CreateV1Update_2_");
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            v1CallAdapter.update(null, pid, objectInputStream, newPid, sysmeta);
        } catch (InvalidRequest e) {
            // expected v1 update() to fail
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() expected InvalidRequest, got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() expected InvalidRequest, got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on a single MN that supports both v1 & v2 APIs.
     * It does a create on the v1 endpoint, then an update on the v2 endpoint.
     * The update operation should succeed.
     */
    @Test
    public void testV1CreateV2Update() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node mNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2Update_1_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Update() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Update() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v2 update
        
        Identifier newPid = null;
        SystemMetadata sysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV1CreateV2Update_2_");
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Identifier updPid = v2CallAdapter.update(null, pid, objectInputStream, newPid, sysmeta);
            assertTrue("testV2CreateV1Update: update on v2 endpoint should succeed", updPid != null);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update: update on v2 endpoint should succeed; got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update: update on v2 endpoint should succeed; got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on two MNs - one that supports the v2 APIs, one that supports ONLY v1.
     * It does a create on the v2 endpoint, then sleeps for some time,
     * then checks the v1 MN whether replication has moved anything to it.
     * This check should fail since v2-created data should NOT be replicated 
     * down to v1 nodes.
     */
    @Test
    public void testV2CreateReplicate() {
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateReplicate_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateReplicate() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateReplicate() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // go have a sandwich or ten
        try {
            Thread.sleep(REPLICATION_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 getSysmeta

        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        try {
            v1CallAdapter.getSystemMetadata(null, pid);
        } catch (NotFound e) {
            // expected, shouldn't have been replicated to a v1 node
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateReplicate() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateReplicate() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on a single MN - one that supports BOTH the v1 and v2 APIs.
     * It does a create on the v2 endpoint, then attempts a getSystemMetadata on the v1 endpoint.
     * The getSystemMetadata should succeed since we're on the same node; 
     * the SystemMetadata will just be downcasted from the v2 version.
     */
    @Test
    public void testV2CreateV1GetSysmetaSameNode() {
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v2Endpoint = v1v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2Endpoint, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1GetSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v1 getSysmeta

        Node v1Endpoint = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1Endpoint, "v1");
        
        try {
            v1CallAdapter.getSystemMetadata(null, pid);
        } catch (NotFound e) {
            // expected, shouldn't have been replicated to a v1 node
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1GetSysmeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1GetSysmeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on two MNs - one that supports ONLY the v1 API and a node 
     * that supports the v2 API.
     * It does a create on the v1 MN, then waits for replication to happen,
     * then attempts a getSystemMetadata on the v2 MN.
     * The getSystemMetadata should succeed; the SystemMetadata will just be 
     * upcasted to the v2 version.
     */
    @Test
    public void testV1CreateV2GetSysmeta() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2GetSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // take a nap
        try {
            Thread.sleep(REPLICATION_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 getSysmeta

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            SystemMetadata sysmeta = v2CallAdapter.getSystemMetadata(null, pid);
            assertTrue("getSystemMetadata() should return an upcasted version of the created object's system metadata", 
                    sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on two MNs - one that supports ONLY the v1 API and a node 
     * that supports the v2 API.
     * It does a create on the v1 MN, then waits for replication to happen,
     * then attempts a getSystemMetadata on the v2 MN.
     * The getSystemMetadata should succeed; the SystemMetadata will just be 
     * upcasted to the v2 version.
     */
    @Test
    public void testV1CreateReplicate() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2GetSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // take a nap
        try {
            Thread.sleep(REPLICATION_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 getSysmeta

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            SystemMetadata sysmeta = v2CallAdapter.getSystemMetadata(null, pid);
            assertTrue("getSystemMetadata() should return an upcasted version of the created object's system metadata", 
                    sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Test operates on two MNs - one that supports ONLY the v1 API and a node 
     * that supports the v2 API.
     * It does a create on the v1 MN, then waits for replication to happen,
     * then attempts a query for the created pid on the v2 MN.
     * The query should succeed, returning a non-empty result.
     */
    @Test
    public void testV1CreateV2Query() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2Query_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV1CreateV2Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 query

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        InputStream is = null;
        try {
            is = v2CallAdapter.query(null, "solr", "");
            assertTrue("query() should return a non-null stream", is != null);
            
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                handleFail(v2CallAdapter.getLatestRequestUrl(), 
                        "testV1CreateV2Query: unable to convert response to document: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String resultCountExp = "/response/result";
            org.w3c.dom.Node resultNode = (org.w3c.dom.Node) xPath.compile(resultCountExp).evaluate(doc, XPathConstants.NODE);
            org.w3c.dom.Node numFoundAttr = resultNode.getAttributes().getNamedItem("numFound");
            assertTrue("testV1CreateV2Query() query response doesn't have valid numFound attribute.", numFoundAttr != null);

            String numFoundVal = numFoundAttr.getNodeValue(); 
            Integer numFoundInt = Integer.getInteger(numFoundVal);
            assertTrue("testV1CreateV2Query() query response contain a result.", 
                    numFoundInt > 0);

            String pidExp = "/response/result/doc/str[@name='identifier']";
            String pidVal = xPath.compile(pidExp).evaluate(doc);
            assertTrue("testV1CreateV2Query() query response should be for the pid created", 
                    pidVal.equals(pid));
            
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV1CreateV2Query() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV1CreateV2Query() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Test operates on two MNs - one that supports ONLY the v1 API and a node 
     * that supports the v2 API.
     * It does a create on the v2 MN, then waits for replication to happen,
     * then attempts a query for the created pid on the v1 MN.
     * The query should fail with an exception, since replication shouldn't 
     * have happened upward for a v1 object to a v2 node.
     */
    @Test
    public void testV2CreateV1Query() {
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1Query_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v2CallAdapter.getLatestRequestUrl(), "testV2CreateV1Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_TIME);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 query

        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        InputStream is = null;
        try {
            is = v1CallAdapter.query(null, "solr", "");
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Query(): "
                    + "query() on the v1 MN should fail.");
        } catch (InvalidRequest e) {
            // expected - query() on v1 endpoint should fail
        } catch (BaseException e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Query() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(v1CallAdapter.getLatestRequestUrl(), "testV2CreateV1Query() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
