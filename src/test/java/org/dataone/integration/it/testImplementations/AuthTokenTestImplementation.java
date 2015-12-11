package org.dataone.integration.it.testImplementations;

import java.io.IOException;
import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SubjectInfo;

public class AuthTokenTestImplementation extends ContextAwareAdapter {

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

    @WebTestName("CN ")
    @WebTestDescription("tests  ...")
    public void testEchoCredentials(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testEchoCredentials(nodeIterator.next(), version);
    }

    public void testEchoCredentials(Node node, String version) {

        String userId = "testId";
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
    
    @WebTestName("CN.isAuthorized with token")
    @WebTestDescription("tests that using an auth token to create an object works and "
            + "that CN.isAuthorized then succeeds and returns true for that token")
    public void testCnIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCnIsAuthorized(nodeIterator.next(), version);
    }

    public void testCnIsAuthorized(Node node, String version) {

        // need to find a better way around this, but for now
        // it makes (mostly) sure that we're only sending calls 
        // to the CN for which we have the CN cert, otherwise
        // the token signature can't be verified
        boolean unmSubj = cnSubmitter.toLowerCase().contains("unm");
        boolean ucsbSubj = cnSubmitter.toLowerCase().contains("ucsb");
        boolean orcSubj = cnSubmitter.toLowerCase().contains("orc");
        if (node.getBaseURL().toLowerCase().contains("unm") && !unmSubj)
            return;
        if (node.getBaseURL().toLowerCase().contains("ucsb") && !ucsbSubj)
            return;
        if (node.getBaseURL().toLowerCase().contains("orc") && !orcSubj)
            return;
        
        String userId = "testId";
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
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
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testCnIsAuthorized_token_5");
        
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
    @WebTestDescription("tests that using an auth token to create an object works and "
            + "that MN.isAuthorized then succeeds and returns true for that token")
    public void testMnIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testMnIsAuthorized(nodeIterator.next(), version);
    }

    public void testMnIsAuthorized(Node node, String version) {

        String userId = "testId";
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
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testMnIsAuthorized_token_5");
        
        try {
            catc.procureTestObject(mn, accessRule, pid, cnSubmitter, userId, replPolicy);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + "), "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + mn.getLatestRequestUrl(), e);
        }
     
        // CN.create() so no need to wait for sync 
        
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
}