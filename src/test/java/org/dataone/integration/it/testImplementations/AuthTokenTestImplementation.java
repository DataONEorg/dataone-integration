package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.dataone.client.RetryHandler;
import org.dataone.client.auth.AuthTokenSession;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ContextAwareTestCaseDataone.LogContents;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;

public class AuthTokenTestImplementation extends ContextAwareAdapter {

    private static final int MAX_SYNC_WAIT = 15 * 60 * 1000; // 15 minutes
    private static final String SAMPLE_ORCID = "http://orcid.org/0000-0002-1825-0097";
    
    public AuthTokenTestImplementation(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    private Session getTokenSesssion(String userId, String fullName) {
        
        String token = null; 
        
        try {
            token = TokenGenerator.getInstance().getJWT(userId, fullName);
        } catch (Exception e) {
            throw new AssertionError("Unable to get a token for (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        } 
        
        Session session = null;
        try {
            session = TokenGenerator.getInstance().getSession(token);
        } catch (IOException e) {
            throw new AssertionError("Unable to get a session for token (" + userId + ", " + fullName + "). "
                    + "got IOException : " + e.getMessage(), e);
        }

        return session;
    }

    @WebTestName("CN.echoCredentials with a token")
    @WebTestDescription("tests that echoCredintials can be called successfully with "
            + "an auth token (and doesn't yield something like an InvalidToken exception)")
    public void testEchoCredentials(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoCredentials(nodeIterator.next(), version);
    }

    public void testEchoCredentials(Node node, String version) {

        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        // calls will override subject with tokenSession
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testEchoCredentials(...) vs. node: " + currentUrl);
        
        try {
            SubjectInfo subjectInfo = cn.echoCredentials(tokenSession);
            for(Person p : subjectInfo.getPersonList())
                log.info("credentials subject :" + p.getSubject());
        } catch (BaseException e) {
            throw new AssertionError("echoCredentials failed with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("echoCredentials failed with token (" 
                    + userId + ", " + fullName + "). " + "got " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        }
    }
    
    @WebTestName("MN.create with token")
    @WebTestDescription("tests that creating an object on the MN is possible "
            + "with a token")
    public void testMnCreate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testMnCreate(nodeIterator.next(), version);
    }

    public void testMnCreate(Node node, String version) {

        final CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter mn = null;
        
        try {
            NodeList nodeList = cn.listNodes();
            
            for (Node n : nodeList.getNodeList()) {
                if (n.getType() != NodeType.MN)
                    continue;
                
                try {
                    MNCallAdapter mnCaller = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                    mnCaller.ping();
                    Node capabilities = mnCaller.getCapabilities();
                    for (Service s : capabilities.getServices().getServiceList()) {
                        if (s.getVersion().equalsIgnoreCase("v2")) {
                            mn = mnCaller;
                            break;
                        }
                    }
                    if (mn != null)
                        break;
                } catch (Exception e1) {
                    continue;
                }
            }
        } catch (Exception e) {
            throw new AssertionError("testCnQuery - test setup failed! ", e);
        }
        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        if (tokenSession instanceof AuthTokenSession)
            log.info("Created auth token: " + ((AuthTokenSession) tokenSession).getAuthToken());
        
        String currentUrl = node.getBaseURL();
        printTestHeader("testMnCreate(...) vs. node: " + currentUrl);
        
        Object[] dataPackage;
        try {
            dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "testMnCreate_", true, userId);
        } catch (Exception e) {
            throw new AssertionError("Unable to generate a test object! "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        org.dataone.service.types.v1.SystemMetadata sysmetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
        final Identifier pid = (Identifier) dataPackage[0];
        SystemMetadata sysmeta;
        try {
            sysmeta = TypeFactory.convertTypeFromType(sysmetaV1,SystemMetadata.class);
        } catch (Exception e) {
            throw new AssertionError("Unable to convert v1 sysmeta to v2 sysmeta. "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        try {
            mn.create(tokenSession, pid, (InputStream) dataPackage[1], sysmeta);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
     
        // MN.create() so need to wait for CN sync 
        
        try {
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnGetSysmetaHandler.execute(30*1000, MAX_SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testCnQuery: Unable to fetch sysmeta from CN. Check status of CN sync. " 
                    + cn.getLatestRequestUrl() + " for pid " + pid.getValue() + 
                    ", Created on " + mn.getNodeBaseServiceUrl(), e);
        }
        
        try {
            mn.isAuthorized(tokenSession, pid, Permission.READ);
        } catch (BaseException e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        }
    }
    
    @WebTestName("CN.isAuthorized with token")
    @WebTestDescription("tests that creating an object on the CN with a token's subject "
            + "in the access policy, then using CN.isAuthorized succeeds "
            + "and returns true for that token")
    public void testCnIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCnIsAuthorized(nodeIterator.next(), version);
    }

    public void testCnIsAuthorized(Node node, String version) {

        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        if (tokenSession instanceof AuthTokenSession)
            log.info("Created auth token: " + ((AuthTokenSession) tokenSession).getAuthToken());
        
        // calls will public subject with tokenSession
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testCnIsAuthorized(...) vs. node: " + currentUrl);
        
        AccessRule accessRule = new AccessRule();
        accessRule.addSubject(tokenSession.getSubject());
        accessRule.addPermission(Permission.READ);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(false);
        replPolicy.setNumberReplicas(0);
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testCnIsAuthorized_token_8");
        
        try {
            catc.procureTestObject(cn, accessRule, pid, cnSubmitter, userId, replPolicy);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + cn.getLatestRequestUrl(), e);
        }
     
        // CN.create() so no need to wait for sync 
        
        try {
            cn.isAuthorized(tokenSession, pid, Permission.READ);
        } catch (BaseException e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        }
    }
    
    @WebTestName("MN.isAuthorized with token")
    @WebTestDescription("tests that creating an object then using "
            + "MN.isAuthorized succeeds and returns true for that token")
    public void testMnIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testMnIsAuthorized(nodeIterator.next(), version);
    }

    public void testMnIsAuthorized(Node node, String version) {

        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        // calls will override subject with tokenSession
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testMnIsAuthorized(...) vs. node: " + currentUrl);
        
        AccessRule accessRule = new AccessRule();
        accessRule.addSubject(tokenSession.getSubject());
        accessRule.addPermission(Permission.READ);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(accessRule);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(false);
        replPolicy.setNumberReplicas(0);
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testMnIsAuthorized_token_8");
        
        try {
            catc.procureTestObject(mn, accessRule, pid, cnSubmitter, userId, replPolicy);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
     
        try {
            mn.isAuthorized(tokenSession, pid, Permission.READ);
        } catch (BaseException e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        }
    }
    
    @WebTestName("MN.update with token")
    @WebTestDescription("tests that creating an object with an auth token, then "
            + "using MN.update with the token succeeds")
    public void testMnUpdate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testMnUpdate(nodeIterator.next(), version);
    }

    public void testMnUpdate(Node node, String version) {

        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        // calls will override subject with tokenSession
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testMnIsAuthorized(...) vs. node: " + currentUrl);
        
        AccessRule accessRule = new AccessRule();
        accessRule.addSubject(tokenSession.getSubject());
        accessRule.addPermission(Permission.READ);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(accessRule);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(false);
        replPolicy.setNumberReplicas(0);
        
        Identifier oldPid = D1TypeBuilder.buildIdentifier("testMnUpdate_token_8_" + ExampleUtilities.generateIdentifier());
        
        // create 
        
        try {
            catc.createTestObject(mn, oldPid, accessRule, cnSubmitter, userId, replPolicy);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + oldPid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
     
        // get sysmeta
        SystemMetadata oldSysmeta = null; 
        try {
            Thread.sleep(10000); // metacat writes to db asynchronously, wait 10s
            oldSysmeta = mn.getSystemMetadata(null, oldPid);
        } catch (Exception e) {
            throw new AssertionError("Unable to get sysmeta for created object (" + oldPid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
        
        // create update object 
        
        Identifier newPid = D1TypeBuilder.buildIdentifier("testMnUpdate_token_8_" + ExampleUtilities.generateIdentifier());
        SystemMetadata newSysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(ContextAwareTestCaseDataone.DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(ContextAwareTestCaseDataone.DEFAULT_TEST_OBJECTFORMAT),
                    tokenSession.getSubject(),
                    oldSysmeta.getAuthoritativeMemberNode());
            newSysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            newSysmeta.setAuthoritativeMemberNode(oldSysmeta.getAuthoritativeMemberNode());
            newSysmeta.setObsoletes(oldPid);
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (Exception e) {
            throw new AssertionError("creating object for MN.update() failed for object (" + newPid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        }
        
        // update 
        
        try {
            mn.update(tokenSession, oldPid, objectInputStream, newPid, newSysmeta);
        } catch (BaseException e) {
            throw new AssertionError("update failed for object (" + oldPid + ") with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("update failed for object (" + oldPid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        }
    }
    
    @WebTestName("CN.query with token")
    @WebTestDescription("tests that creating an object and then using "
            + "CN.query can locate the object")
    public void testCnQuery(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCnQuery(nodeIterator.next(), version);
    }

    public void testCnQuery(Node node, String version) {

        final CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter mn = null;
        
        try {
            NodeList nodeList = cn.listNodes();
            
            for (Node n : nodeList.getNodeList()) {
                if (n.getType() != NodeType.MN)
                    continue;
                
                try {
                    MNCallAdapter mnCaller = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                    mnCaller.ping();
                    Node capabilities = mnCaller.getCapabilities();
                    for (Service s : capabilities.getServices().getServiceList()) {
                        if (s.getVersion().equalsIgnoreCase("v2")) {
                            mn = mnCaller;
                            break;
                        }
                    }
                    if (mn != null)
                        break;
                } catch (Exception e1) {
                    continue;
                }
            }
        } catch (Exception e) {
            throw new AssertionError("testCnQuery - test setup failed! ", e);
        }

        assertTrue("testCnQuery - test setup needs to be able to locate a v2 MN!", mn != null);
        
        String userId = SAMPLE_ORCID;
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        if (tokenSession instanceof AuthTokenSession)
            log.info("Created auth token: " + ((AuthTokenSession) tokenSession).getAuthToken());
        
        // calls will public subject with tokenSession
        String currentUrl = node.getBaseURL();
        printTestHeader("testCnQuery(...) vs. node: " + currentUrl);
        
        AccessRule accessRule = new AccessRule();
        accessRule.addSubject(tokenSession.getSubject());
        accessRule.addPermission(Permission.READ);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(false);
        replPolicy.setNumberReplicas(0);
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testCnQuery_token_8");
        
        try {
            catc.procureTestObject(mn, accessRule, pid, cnSubmitter, userId, replPolicy);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
     
        // MN.create() so need to wait for CN sync 
        
        try {
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnGetSysmetaHandler.execute(30*1000, MAX_SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testCnQuery: Unable to fetch sysmeta from CN. Check status of CN sync. " 
                    + cn.getLatestRequestUrl() + " for pid " + pid.getValue() + 
                    ", Created on " + mn.getNodeBaseServiceUrl(), e);
        }
        
        // CN.query
        
        InputStream is = null;
        try {
            is = cn.query(tokenSession, "solr", "?q=identifier:" + pid.getValue());
            log.info("CN.query ran against " + cn.getLatestRequestUrl() + " for pid " + pid.getValue());
            
            LogContents numQueryContents = ContextAwareTestCaseDataone.getNumQueryContents(is);
            log.info("CN.query results have a count of " + numQueryContents.existingLogs + " logs "
                    + "and contain " + numQueryContents.docsReturned + " returned docs.");
            assertTrue("CN.query resutls should have a non-zero count for pid " + pid.getValue() + 
                    " against CN " + cn.getLatestRequestUrl(), numQueryContents.existingLogs > 0);
            
        } catch (BaseException e) {
            throw new AssertionError("query failed for object (" + pid + ") with token (" 
                    + userId + ", " + fullName + "). " + "got " + e.getClass().getSimpleName() 
                    + " [" + e.getCode() + "," + e.getDetail_code() + "] : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        } catch (Exception e) {
            throw new AssertionError("query failed for object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + cn.getLatestRequestUrl(), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}