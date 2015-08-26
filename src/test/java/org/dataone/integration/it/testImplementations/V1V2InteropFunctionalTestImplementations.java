package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
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
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class V1V2InteropFunctionalTestImplementations extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private CNCallAdapter cn;
    /** MNs supporting ONLY the V1 API */
    private List<Node> v1mns;
    /** MNs supporting the V2 API (might also support V1 API) */
    private List<Node> v2mns;
    /** MNs supporting BOTH the V1 & V2 APIs */
    private List<Node> v1v2mns;
    
    private static final long REPLICATION_WAIT = 5 * 60000;    // 5 minutes
    private static final long METACAT_INDEXING_WAIT = 10000;
    
    private static Log log = LogFactory.getLog(SystemMetadataFunctionalTestImplementation.class);
    
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs various V1 and V2 methods in conjunction with each other, "
                + "checking for invalid interoperation.";
        }

    public void setup(Iterator<Node> cnIter) {
        List<Node> cnList = new ArrayList<Node>();
        List<Node> mnList = new ArrayList<Node>();
        v1mns = new ArrayList<Node>();
        v2mns = new ArrayList<Node>();
        v1v2mns = new ArrayList<Node>();
        
        cnList = IteratorUtils.toList(cnIter);
        
        if(cnList.size() > 0)
            cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        if(cn != null) {
            try {
                for(Node n : cn.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (Exception e) {
                throw new AssertionError("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
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

        log.info("v1-ONLY MNs available:     " + v1mns.size());
        log.info("v2 MNs available:          " + v2mns.size());
        log.info("v1 & v2 MNs available:     " + v1v2mns.size());
        
        for (Node n : v1mns)
            log.info("v1-ONLY MN:   " + n.getBaseURL());
        for (Node n : v2mns)
            log.info("v2 MN     :   " + n.getBaseURL());
        for (Node n : v1v2mns)
            log.info("v1 & v2 MN:   " + n.getBaseURL());
    }
    
    @WebTestName("v2 create, v1 update")
    @WebTestDescription(
     "Test operates on a single MN that supports both v1 & v2 APIs." +
     "It does a create on the v2 endpoint, then an update on the v1 endpoint." +
     "The update operation should be successful, BUT we check that it has not " + 
     "erased any v2-specific data from the existing system metadata (meaning the sid).")
    public void testV2CreateV1UpdateSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(accessRule);
        
        Node mNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v2 create
        
        Identifier oldPid = D1TypeBuilder.buildIdentifier("testV2CreateV1Update_pid_" + ExampleUtilities.generateIdentifier());
        Identifier oldSid = D1TypeBuilder.buildIdentifier("testV2CreateV1Update_sid_" + ExampleUtilities.generateIdentifier());
        try {
            createTestObject(v2CallAdapter, oldPid, oldSid, null, null, policy, "testRightsHolder", "testRightsHolder");
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // grab the old sysmeta
        
        SystemMetadata oldSysmeta = null;
        try {
            oldSysmeta = v2CallAdapter.getSystemMetadata(null, oldPid);
        } catch (Exception e) {
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't fetch old sysmeta: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v1 update
        
        Identifier newPid = null;
        SystemMetadata newSysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV2CreateV1Update_2_" + ExampleUtilities.generateIdentifier());
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            newSysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            v1CallAdapter.update(null, oldPid, objectInputStream, newPid, newSysmeta);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() update call failed! : " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        try {    
            newSysmeta = v1CallAdapter.getSystemMetadata(null, newPid);
            
            boolean sidsMatch = false;
            if(newSysmeta.getSeriesId() == null && oldSid == null)
                sidsMatch = true;
            if(newSysmeta.getSeriesId() != null && oldSid != null 
                    && newSysmeta.getSeriesId().getValue().equals(oldSid.getValue()))
                sidsMatch = true;
            
            assertTrue("The system metadata associated with a v2 object, "
                    + "after doing a v1 update(), should still contain the "
                    + "seriesId from the old system metadata. It should not have "
                    + "been erased by the update on the v1 endpoint!",
                    sidsMatch);
            
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() expected InvalidRequest, got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() expected InvalidRequest, got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 update")
    @WebTestDescription(
     "Test operates on a single MN that supports both v1 & v2 APIs." +
     "It does a create on the v1 endpoint, then an update on the v2 endpoint." +
     "The update operation should succeed.")
    public void testV1CreateV2UpdateSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node mNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2Update_1_" + ExampleUtilities.generateIdentifier(), accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Update() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Update() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 update
        
        Identifier newPid = null;
        SystemMetadata sysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV1CreateV2Update_2_" + ExampleUtilities.generateIdentifier());
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Identifier updPid = v2CallAdapter.update(null, pid, objectInputStream, newPid, sysmeta);
            assertTrue("testV2CreateV1Update: update on v2 endpoint should succeed", updPid != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update: update on v2 endpoint should succeed; got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update: update on v2 endpoint should succeed; got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 getSystemMetadata")
    @WebTestDescription(
     "Test operates on two MNs - one that supports the v2 APIs, one that supports ONLY v1." +
     "It does a create on the v2 endpoint, then sleeps for some time, " + 
     "then checks the v1 MN whether replication has moved anything to it." + 
     "This check should fail since v2-created data should NOT be replicated " +
     "down to v1 nodes.")
    public void testV2CreateV1GetSysMeta() {
        
        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSysMeta_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysMeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysMeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // go have a sandwich or ten
        try {
            Thread.sleep(REPLICATION_WAIT);
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
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysMeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysMeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 getSystemMetadata, same node")
    @WebTestDescription(
     "Test operates on a single MN - one that supports BOTH the v1 and v2 APIs. " +
     "It does a create on the v2 endpoint, then attempts a getSystemMetadata on the v1 endpoint. " +
     "The getSystemMetadata should succeed since we're on the same node; " +
     "the SystemMetadata will just be downcasted from the v2 version.")
    public void testV2CreateV1GetSysmetaSameNode() {
        
        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2Node = v1v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1GetSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 getSysmeta

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v1");
        
        try {
            SystemMetadata sysmeta = v1CallAdapter.getSystemMetadata(null, pid);
            assertTrue("v1 getSystemMetadata() after a v2 create() on the same node should succeed, "
                    + "returning a non-null SystemMetadata", sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysmeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSysmeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 get, different nodes")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a get() on the v2 MN. " +
     "The get() should succeed")
    public void testV1CreateV2Get() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        
        
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Get_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Get() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Get() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // take a nap
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 get

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        InputStream is = null;
        try {
            is = v2CallAdapter.get(null, pid);
            assertTrue("a v2 get() after a v1 create() on a different node should "
                    + "(given enough time for replication) "
                    + "return a non-null InputStream", 
                    is != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2Get() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("v2 create, v1 get, same node")
    @WebTestDescription(
     "Test operates on a single MN - one that supports BOTH the v1 and v2 APIs. " +
     "It does a create on the v2 endpoint, then attempts a get() on the v1 endpoint. " +
     "The get() should succeed since we're on the same node.")
    public void testV2CreateV1GetSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2Node = v1v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1GetSameNode_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 get

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v1");
        
        InputStream is = null;
        try {
            is = v1CallAdapter.get(null, pid);
            assertTrue("A get on the v1 endpoint after a v2 endpoint create on the same node "
                    + "should return a non-null InputStream.", is != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1GetSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("v1 create, v2 getSystemMetadata, different nodes")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a getSystemMetadata on the v2 MN. " +
     "The getSystemMetadata should succeed; the SystemMetadata will just be " + 
     "upcasted to the v2 version.")
    public void testV1CreateV2GetSysmeta() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2GetSysmeta_" + ExampleUtilities.generateIdentifier());
        
        log.info("Trying to create test object: " + pid.getValue() + " on MN: " + v1CallAdapter.getNodeBaseServiceUrl());
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            log.info("Created test object: " + pid.getValue() + " on MN: " + v1CallAdapter.getNodeBaseServiceUrl());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // take a nap
        try {
            Thread.sleep(REPLICATION_WAIT);
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
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " : testV1CreateV2GetSysmeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " : testV1CreateV2GetSysmeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 getSystemMetadata")
    @WebTestDescription(
     "Test operates on one MN which supports BOTH the v1 API and the v2 API" +
     "It does a create on the v1 endpoint, then attempts a getSystemMetadata on the v2 endpoint." +
     "The getSystemMetadata should succeed; the SystemMetadata will just be" + 
     "upcasted to the v2 version.")
    public void testV1CreateV2GetSysmetaSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1MNode = v1v2mns.get(0);
        MNCallAdapter v1Endpoint = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1Endpoint, "testV1CreateV2GetSysmeta_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1Endpoint.getLatestRequestUrl() + "testV1CreateV2GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1Endpoint.getLatestRequestUrl() + "testV1CreateV2GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 getSysmeta

        Node v2MNode = v1v2mns.get(0);
        MNCallAdapter v2Endpoint = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            SystemMetadata sysmeta = v2Endpoint.getSystemMetadata(null, pid);
            assertTrue("getSystemMetadata() should return an upcasted version of the created object's system metadata", 
                    sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2Endpoint.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2Endpoint.getLatestRequestUrl() + "testV2CreateV1Update() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 query")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a query for the created pid on the v2 MN. " +
     "The query should succeed, returning a non-empty result.")
    public void testV1CreateV2Query() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
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
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 query

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        InputStream is = null;
        try {
            String encodedPid = URLEncoder.encode(pid.getValue(), "UTF-8");
            is = v2CallAdapter.query(null, "solr", "?q=identifier:" + encodedPid);
            assertTrue("query() should return a non-null stream", is != null);
            
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                throw new AssertionError(v2CallAdapter.getLatestRequestUrl() +
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
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2Query() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2Query() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @WebTestName("v2 create, v1 query")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then waits for replication to happen, " +
     "then attempts a query for the created pid on the v1 MN. " +
     "The query should fail with an exception, since replication shouldn't " + 
     "have happened upward for a v1 object to a v2 node.")
    public void testV2CreateV1Query() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Query_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 query

        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        InputStream is = null;
        try {
            String encodedPid = URLEncoder.encode(pid.getValue(), "UTF-8");
            is = v1CallAdapter.query(null, "solr", "?q=identifier:" + encodedPid);
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Query(): "
                    + "query() on the v1 MN should fail.");
        } catch (InvalidRequest e) {
            // expected - query() on v1 MN should fail
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Query() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Query() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("v2 create, v1 delete, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v2 endpoint, then attempts to delete the object " +
     "for that pid on the v1 endpoint. The delete should succeed.")
    public void testV2CreateV1DeleteSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1DeleteSameNode_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1DeleteSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1DeleteSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 delete

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        try {
            Identifier deleteId = v1CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2DeleteSameNode() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1DeleteSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1DeleteSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 delete, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v1 endpoint, then attempts to delete the object " +
     "for that pid on the v2 endpoint. The delete should succeed since we're on " +
     "the same node.")
    public void testV1CreateV2DeleteSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2DeleteSameNode_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2DeleteSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2DeleteSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 delete

        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        try {
            Identifier deleteId = v2CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2DeleteSameNode() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2DeleteSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2DeleteSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 delete")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then attempts to delete the object " +
     "for that pid on the v1 MN. The delete should fail because replication from " +
     "a v2 MN to a v1 MN shouldn't happen for v2 objects.")
    public void testV2CreateV1Delete() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Delete_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Delete() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Delete() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 delete

        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        try {
            v1CallAdapter.delete(null, pid);
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Delete(): "
                    + "delete() on the v1 MN should fail.");
        } catch (NotFound e) {
            // expected - not available on this node
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Delete() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1Delete() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 delete")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then attempts to delete the object " +
     "for that pid on the v2 MN. The delete should succeed because the object should " +
     "have been replicated from the v1 MN to the v2 MN.")
    public void testV1CreateV2Delete() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Delete_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Delete() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2Delete() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 delete

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            Identifier deleteId = v2CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2Delete() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2Delete() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2Delete() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 listObjects, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v2 endpoint, then attempts to locate the object " +
     "for that pid using listObjects() on the v1 endpoint. It should be found.")
    public void testV2CreateV1ListObjectsSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v2CallAdapter, "testV2CreateV1ListObjectsSameNode_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjectsSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjectsSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 delete

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        try {
            ObjectList objList = v1CallAdapter.listObjects(null, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV2CreateV1ListObjectsSameNode() - v1 listObjects() results "
                    + "should include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjectsSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjectsSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 listObjects, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v1 endpoint, then attempts to locate the object " +
     "for that pid using listObjects() on the v2 endpoint. The pid should be found.")
    public void testV1CreateV2ListObjectsSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        // v1 create
        
        Identifier pid = null;
        try {
            pid = createTestObject(v1CallAdapter, "testV1CreateV2ListObjectsSameNode_", accessRule);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjectsSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjectsSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Thread.sleep(METACAT_INDEXING_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 listObjects

        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        try {
            ObjectList objList = v2CallAdapter.listObjects(null, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV1CreateV2ListObjectsSameNode() - v2 listObjects() results "
                    + "should include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjectsSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjectsSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 listObjects")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then attempts to locate the object " +
     "for that pid using listObjects() on the v1 MN. The listObjects results " +
     "should not contain the pid created because replication from " +
     "a v2 MN to a v1 MN shouldn't happen for v2 objects.")
    public void testV2CreateV1ListObjects() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1ListObjects_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjects() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjects() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v1 listObjects

        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        try {
            ObjectList objList = v1CallAdapter.listObjects(null, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertFalse("testV2CreateV1ListObjects() - v2 listObjects() results "
                    + "should NOT include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjects() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV2CreateV1ListObjects() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 listObjects")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then attempts to delete the object " +
     "for that pid on the v2 MN. The listObjects call should return results containing " +
     "the created pid because the object should have been replicated " +
     "from the v1 MN to the v2 MN.")
    public void testV1CreateV2ListObjects() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2ListObjects_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjects() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjects() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // v2 listObjects

        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            ObjectList objList = v2CallAdapter.listObjects(null, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV1CreateV2ListObjects() - v2 listObjects() on " + v2MNode.getBaseURL() + "results "
                    + "should include the created object: " + pid.getValue(), objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjects() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV1CreateV2ListObjects() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 archive")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to archive the object on the CN. " +
     "The archive call should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, so the v2 API should be used to do the update.")
    public void testV2CreateV1CnArchive() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Archive_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Archive() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1Archive() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // CN archive
        
        try {
            cn.archive(null, pid);
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1Archive() : archive call to CN should fail because "
                    + "object being archived has a v2 node as its authoritative MN" );
        } catch (InvalidRequest e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1Archive() : expected archive call to CN should fail "
                    + "with an InvalidRequest exception because object being archived has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    @WebTestName("v2 create, v1 setReplicationPolicy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setReplicationPolicy on the object on the CN. " +
     "The archive call should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, so the v2 API should be used to do the update.")
    public void testV2CreateV1CnSetReplicationPolicy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetReplicationPolicy_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetReplicationPolicy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetReplicationPolicy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // CN setReplicationPolicy
        
        try {
            replPolicy.setNumberReplicas(replPolicy.getNumberReplicas() + 1);
            cn.setReplicationPolicy(null, pid, replPolicy, 2);
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetReplicationPolicy() : setReplicationPolicy "
                    + "call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (InvalidRequest e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetReplicationPolicy() : expected setReplicationPolicy "
                    + "call to CN should fail with an InvalidRequest exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    @WebTestName("v2 create, v1 setAccessPolicy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setAccessPolicy on the object on the CN. " +
     "The archive call should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, so the v2 API should be used to do the update.")
    public void testV2CreateV1CnSetAccessPolicy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetAccessPolicy_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetAccessPolicy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetAccessPolicy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // CN setAccessPolicy
        
        try {
            AccessPolicy accessPolicy = new AccessPolicy();
            accessPolicy.addAllow(accessRule);
            cn.setAccessPolicy(null, pid, null, 2);
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetAccessPolicy() : setAccessPolicy "
                    + "call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (InvalidRequest e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetAccessPolicy() : expected setAccessPolicy "
                    + "call to CN should fail with an InvalidRequest exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    @WebTestName("v2 create, v1 setRightsHolder")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setRightsHolder the object on the CN. " +
     "The archive call should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, so the v2 API should be used to do the update.")
    public void testV2CreateV1CnSetRightsHolder() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetRightsHolder_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetRightsHolder() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetRightsHolder() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // CN setRightsHolder
        
        try {
            getSession("testRightsHolder");
            Subject newSubject = D1TypeBuilder.buildSubject("testRightsHolder");
            cn.setRightsHolder(null, pid, newSubject, 2);
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetRightsHolder() : setRightsHolder "
                    + "call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (InvalidRequest e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetRightsHolder() : expected setRightsHolder "
                    + "call to CN should fail with an InvalidRequest exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    @WebTestName("v2 create, v1 setObsoletedBy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setObsoletedBy on the object on the CN. " +
     "The archive call should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, so the v2 API should be used to do the update.")
    public void testV2CreateV1CnSetObsoletedBy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2mns.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(2);
        
        Node v2MNode = v2mns.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_" + ExampleUtilities.generateIdentifier());
        Identifier obsoletedByPid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_obs_" + ExampleUtilities.generateIdentifier());
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            obsoletedByPid = createTestObject(v2CallAdapter, obsoletedByPid, accessRule, replPolicy);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetObsoletedBy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + "testV2CreateV1CnSetObsoletedBy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
        // CN setObsoletedBy
        
        try {
            cn.setObsoletedBy(null, pid, obsoletedByPid, 2);
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : setObsoletedBy "
                    + "call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (InvalidRequest e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cn.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : expected setObsoletedBy "
                    + "call to CN should fail with an InvalidRequest exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
}
