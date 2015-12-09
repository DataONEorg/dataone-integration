package org.dataone.integration.it.testImplementations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.portal.TokenGenerator;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;

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

    @WebTestName("CN.isAuthorized with token")
    @WebTestDescription("tests that using an auth token to create an object works and "
            + "that CN.isAuthorized then succeeds and returns true for that token")
    public void testCnIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCnIsAuthorized(nodeIterator.next(), version);
    }

    public void testCnIsAuthorized(Node node, String version) {

        String userId = "testId";
        String fullName = "Jane Scientist";
        Session tokenSession = getTokenSesssion(userId, fullName);
        
        // calls will override public subject with tokenSession
        CNCallAdapter cn = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testCnIsAuthorized(...) vs. node: " + currentUrl);
        
        Object[] dataPackage;
        try {
            dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "testCnIsAuthorized", true, userId);
        } catch (Exception e) {
            throw new AssertionError("Unable to generate a test object! "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        org.dataone.service.types.v1.SystemMetadata sysmetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
        Identifier pid = (Identifier) dataPackage[0];
        SystemMetadata sysmeta;
        try {
            sysmeta = TypeFactory.convertTypeFromType(sysmetaV1,SystemMetadata.class);
        } catch (Exception e) {
            throw new AssertionError("Unable to convert v1 sysmeta to v2 sysmeta. "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        try {
            cn.create(tokenSession, pid, (InputStream) dataPackage[1], sysmeta);
        } catch (Exception e) {
            throw new AssertionError("Unable to create object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + " from " + cn.getLatestRequestUrl(), e);
        }
     
        // CN.create() so no need to wait for sync 
        
        try {
            cn.isAuthorized(tokenSession, pid, Permission.READ);
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
        
        // calls will override public subject with tokenSession
        MNCallAdapter mn = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testCnIsAuthorized(...) vs. node: " + currentUrl);
        
        Object[] dataPackage;
        try {
            dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "testCnIsAuthorized", true, userId);
        } catch (Exception e) {
            throw new AssertionError("Unable to generate a test object! "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        org.dataone.service.types.v1.SystemMetadata sysmetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
        Identifier pid = (Identifier) dataPackage[0];
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
     
        // CN.create() so no need to wait for sync 
        
        try {
            mn.isAuthorized(tokenSession, pid, Permission.READ);
        } catch (Exception e) {
            throw new AssertionError("isAuthorized failed for object (" + pid + ") with token (" + userId + ", " + fullName + "). "
                    + "got " + e.getClass().getSimpleName() + " : " + e.getMessage()
                    + " from " + mn.getLatestRequestUrl(), e);
        }
    }
}