package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

public class SidCNTestImplementations extends SidCommonTestImplementations {

    private Logger logger = Logger.getLogger(SidCNTestImplementations.class);
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods for CNs that accept SID parameters";
    }
    
    @Override
    protected Iterator<Node> getNodeIterator() {
        return getCoordinatingNodeIterator();
    }
    
    @Override
    protected SidCommonTestImplementations getSetupClass() {
        return this;
    }
    
    @Override
    protected int[] getCasesToTest() {
        return new int[] {  1 };//, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
    }
    
    @Override
    protected int[] getPidsPerSid() {
        return new int[] {  2, 2, 2, 2, 2, 2, 2, 3, 3, 3,
                            2, 2, 2, 2, 3, 2, 3, 3 };
    }

    protected IdPair setupCNCase1(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 1. P1(S1) <-> P2(S1),  S1 = P2 (Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, null);

        return new IdPair(s1, p2);
    }
    
    protected IdPair setupCNCase2(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 2. P1(S1) ? P2(S1), S1 = P2, Error condition, P2 not allowed (should not exist) (Rule 2)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, null, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase3(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 3. P1(S1) <- P2(S1), S1 = P2, Discouraged, but not error condition, S1 = P2 (Rule 2, missingFields)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase4(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 4. P1(S1) <-> P2(S1) <-> P3(S2), S1 = P2(use Rule 3), S2 = P3 (use Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p3, s2, p2, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase5(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 5. P1(S1) <- P2(S1) <- P3(S2), S1 = P2 (use Rule 2 or have missing field), S2 = P3  (use Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, null);
        createTestObject(callAdapter, p3, s2, p2, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase6(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 6. P1(S1) <-> P2(S1) <-> P3(), S1 = P2 (use Rule 3)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p3, null, p2, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase7(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 7. P1(S1) <-> P2(S1) <-> P3() <-> P4(S2), S1 = P2 (Rule 3), S2 = P4 (Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p3, null, p2, p4);
        createTestObject(callAdapter, p4, s2, p3, null);
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase8(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 8. P1(S1) <-> P2(S1) ->  ??  <- P4(S1), S1 = P4, (Rule 1) (Error, but will happen)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p4, s1, p3, null);
    
        return new IdPair(s1, p4);
    }

    protected IdPair setupCNCase9(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 9. P1(S1) <-> P2(S1)  ??  <- P4(S1), S1 = P4 (Rule 2) (??: object was not synchronized)
        // ... how is this any different from Case 8 ?
        
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p4, s1, p3, null);
    
        return new IdPair(s1, p4);
    }

    protected IdPair setupCNCase10(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 10: P1(S1) <-> P2(S1) ->  XX  <- P4(S1), S1 = P4, (Rule 1) (XX: object P3 was deleted)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p4, s1, p3, null);
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCNCase11(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 11: P1(S1) <-> P2(S1) <-> [archived:P3(S1)], S1 = P3, (Rule 1) 
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p3, s1, p2, null);
        callAdapter.archive(null, p3);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupCNCase12(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 12. P1(S1) <-> P2(S1) -> ??, S1 = P2, (Rule 4) (Error, but will happen)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        
        return new IdPair(s1, p2);
    }
    
    protected IdPair setupCNCase13(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 13. P1(S1) <- P2(S1) -> ??, S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, p3);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase14(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 14: P1(S1) <- P2(S1) -> P3(S2).
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p3, s2, null, null);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase15(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 15. P1(S1) <-> P2(S1)  ?? <- P4(S1) <-> P5(S2), S1 = P4 (Rule 1) 
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier p5 = createIdentifier("P5_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, null);
        createTestObject(callAdapter, p4, s1, p3, p5);
        createTestObject(callAdapter, p5, s2, p4, null);
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCNCase16(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 16. P1(S1) <- P2(S1) -> ?? <-P4(S2) S1 = P2 (two ends, not an ideal chain), S2=P4 (rule1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p4, s2, p3, null);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCNCase17(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 17. P1(S1) <- P2(S1) -> ?? <-P4(S1) S1 = P4 (P1 and P4 are two ends, not an ideal chain)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p4, s1, p3, null);
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCNCase18(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 18. P1(S1) <->P2(S1) -> ??  ???<-P5(S1) S1 = P5 (P2 is a type 2 end and P4  is a type 1 end, not an ideal chain)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier p5 = createIdentifier("P5_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        createTestObject(callAdapter, p2, s1, p1, p3);
        createTestObject(callAdapter, p5, s1, p4, null);
        
        return new IdPair(s1, p5);
    }
    
    /**
     * Sets up each pid chain scenario, then calls resolve() with the SID and the head PID.
     * The resulting {@link ObjectLocationList}s should both hold the same resolve URLs.
     */
    @WebTestName("resolve: tests that resolve works if given a SID")
    @WebTestDescription("this test checks to see if calling resolve with a SID returns "
            + "the same location list as if it were given the head PID")
    @Test
    public void testResolve() {

        logger.info("Testing resolve() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing resolve(), case " + caseNum);
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    ObjectLocationList sidLocationList = callAdapter.resolve(null, sid);
                    ObjectLocationList pidLocationList = callAdapter.resolve(null, pid);
                    
                    if(sidLocationList.getObjectLocationList().size() == 0)
                        assertTrue("resolve() Case " + caseNum + ", resolve() on SID should yield non-empty location list", 
                                false);
                    if(pidLocationList.getObjectLocationList().size() == 0)
                        assertTrue("resolve() Case " + caseNum + ", resolve() on head PID should yield non-empty location list", 
                                false);
                    
                    ObjectLocation sidLoc = sidLocationList.getObjectLocation(0);
                    ObjectLocation pidLoc = pidLocationList.getObjectLocation(0);
                    
                    String sidResolveURL = sidLoc.getUrl();
                    String pidResolveURL = pidLoc.getUrl();
                    assertEquals("resolve() Case " + caseNum + ", SID and head PID should resolve() to same URL",
                            sidResolveURL, pidResolveURL);
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setRightsHolder() with the SID.
     * This should update the rights holder of the head PID.
     * So we get the rights holder on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @WebTestName("setRightsHolder: tests that setRightsHolder works if given a SID")
    @WebTestDescription("this test checks if setting the rights holder with a SID "
            + "correctly changed the rights holder for the head PID")
    @Test
    public void testSetRightsHolder() {

        logger.info("Testing setRightsHolder() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing setRightsHolder(), case " + caseNum);
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    Subject sidRightsHolder = getSubject("testRightsHolder");
                    callAdapter.setRightsHolder(null, sid, sidRightsHolder, 1);
                    
                    SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                    Subject pidRightsHolder = sysmeta.getRightsHolder();
                    
                    assertTrue("setRightsHolder() Case " + caseNum, sidRightsHolder.getValue().equals(pidRightsHolder.getValue()));
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setAccessPolicy() with the SID.
     * This should update the access policy of the head PID.
     * So we get the access policy on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @WebTestName("setAccessPolicy: tests that setAccessPolicy works if given a SID")
    @WebTestDescription("this test checks if setting the access policy with a SID "
            + "correctly changed the policy for the head PID")
    @Test
    public void testSetAccessPolicy() {

        logger.info("Testing setAccessPolicy() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing setAccessPolicy(), case " + caseNum);
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    AccessPolicy sidAccessPolicy = new AccessPolicy();
                    Subject cnSubject = getSubject(subjectLabel);
                    AccessRule accessRuleCN = AccessUtil.createAccessRule(
                            new Subject[] { cnSubject }, new Permission[] { Permission.CHANGE_PERMISSION });
                    String testSubject = "BLARG";
                    AccessRule accessRuleBlarg = AccessUtil.createAccessRule(
                            new String[] { testSubject }, new Permission[] { Permission.READ });
                    sidAccessPolicy.addAllow(accessRuleCN);
                    sidAccessPolicy.addAllow(accessRuleBlarg);
                    
                    callAdapter.setAccessPolicy(null, sid, sidAccessPolicy, 1);
                    
                    SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                    AccessPolicy pidAccessPolicy = sysmeta.getAccessPolicy();
                    
                    assertTrue("setAccessPolicy() Case " + caseNum + " : allow list size", 
                            pidAccessPolicy.getAllowList().size() == 2);
                    AccessRule fetchedAccessRuleCN = pidAccessPolicy.getAllowList().get(0);
                    AccessRule fetchedAccessRuleBlarg = pidAccessPolicy.getAllowList().get(1);
                    assertTrue("setAccessPolicy() Case " + caseNum + " : CN access rule", 
                            fetchedAccessRuleCN.getSubject(0).getValue().equals(cnSubject.getValue()));
                    assertTrue("setAccessPolicy() Case " + caseNum + " : test access rule",
                            fetchedAccessRuleBlarg.getSubject(1).getValue().equals(testSubject));
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setReplicationPolicy() with the SID.
     * This should update the policy of the head PID.
     * So we get the replication policy on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @WebTestName("setReplicationPolicy: tests that setReplicationPolicy works if given a SID")
    @WebTestDescription("this test checks if setting the replication policy with a SID "
            + "correctly changed the policy for the head PID")
    @Ignore("According to \"Mutability of Content\" page, only supposed to work for PIDS. v2 API disagrees though...")
    @Test
    public void testSetReplicationPolicy() {

        logger.info("Testing setReplicationPolicy() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing setReplicationPolicy(), case " + caseNum);
            
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    ReplicationPolicy policy = new ReplicationPolicy();
                    NodeReference nodeRef = new NodeReference();
                    String testNodeRef = "BLARG";
                    nodeRef.setValue(testNodeRef);
                    policy.addBlockedMemberNode(nodeRef);
                    callAdapter.setReplicationPolicy(null, sid, policy, 1);
                    
                    SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                    ReplicationPolicy fetchedPolicy = sysmeta.getReplicationPolicy();
                    
                    assertTrue("setReplicationPolicy() Case " + caseNum + " : blocked nodes size", 
                            fetchedPolicy.getBlockedMemberNodeList().size() == 1);
                    
                    NodeReference fetchedNodeRef = fetchedPolicy.getBlockedMemberNodeList().get(0);
                    
                    assertTrue("setReplicationPolicy() Case " + caseNum + " : test node ref",
                            fetchedNodeRef.getValue().equals(testNodeRef));
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * A more thorough CN test for archive(); overrides {@link SidCommonTestImplementations#testArchive()}.
     * Sets up each pid chain scenario, then calls archive() with the SID.
     * After this, the head PID should still be resolvable, but not show up in searches.
     * So we do a solr query and assert that it returns no reults.
     */
    @WebTestName("archive: tests that archive works if given a SID")
    @WebTestDescription("this test checks if calling archive with a SID "
            + "makes the head PID no longer show up in solr queries")
    @Override
    @Test
    public void testArchive() {
        logger.info("Testing archive() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing archive(), case " + caseNum);
            
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    callAdapter.archive(null, sid);
                    
                    // test it's actually archived
                    SystemMetadata sysmeta = null;
                    try {
                        sysmeta = callAdapter.getSystemMetadata(null, pid);
                    } catch (NotFound e) {
                        assertTrue("archive() Case " + caseNum + ", should be able to getSystemMetadata() for an archived object", 
                                false);
                    }
                    assertTrue("archive() Case " + caseNum + ", object should be archived", 
                            sysmeta.getArchived());
                    
                    // test search
                    ObjectList objectList = callAdapter.search(null, QUERYTYPE_SOLR, "?q=identifier:" + pid);
                    assertTrue("archive() Case " + caseNum + " search() for archived object shouldn't return results",
                            objectList.getObjectInfoList().size() == 0);
    
                    // test resolve()-able
                    ObjectLocationList locationList = null;
                    try {
                        locationList = callAdapter.resolve(null, sid);
                    } catch (NotFound e) {
                        assertTrue("archive() Case " + caseNum + ", should be able to resolve() an archived object", 
                                false);
                    }
                    
                    assertTrue("archive() Case " + caseNum + ", should be able to resolve() an archived object to a location list", 
                            locationList != null && locationList.getObjectLocationList().size() > 0);
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail( "Case: " + i + " : " + callAdapter.getNodeBaseServiceUrl(), e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
}
