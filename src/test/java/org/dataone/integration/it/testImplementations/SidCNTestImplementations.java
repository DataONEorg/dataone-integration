package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
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
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class SidCNTestImplementations extends SidCommonTestImplementations {

    private Logger logger = Logger.getLogger(SidCNTestImplementations.class);
    
    /** expected time it takes for an object to sync from MN to CN - should be set configured to be lower on actual node */
    long SYNC_TIME = 5 * 60 * 1000;
    
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
        return new int[] { 1 };//, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
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
        
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext()) {
            Node node = cnIter.next();
            CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
            
            try {
                // Case 1. P1(S1) <-> P2(S1),  S1 = P2 (Rule 1)
                Identifier p1 = createIdentifier("P1_", node);
                Identifier p2 = createIdentifier("P2_", node);
                Identifier s1 = createIdentifier("S1_", node);
                
                createTestObject(callAdapter, p1, s1, null, null, true);
                //createTestObject(callAdapter, p1, s1, null, p2, true);
                createTestObject(callAdapter, p2, s1, p1, null, true);
                
                Identifier sid = s1;
                Identifier pid = p2;
    
                ObjectLocationList pidLocationList = callAdapter.resolve(null, pid);
                ObjectLocationList sidLocationList = callAdapter.resolve(null, sid);
                
                if(sidLocationList.getObjectLocationList().size() == 0)
                    assertTrue("resolve() on SID should yield non-empty location list", 
                            false);
                if(pidLocationList.getObjectLocationList().size() == 0)
                    assertTrue("resolve() on head PID should yield non-empty location list", 
                            false);
                
                ObjectLocation sidLoc = sidLocationList.getObjectLocation(0);
                ObjectLocation pidLoc = pidLocationList.getObjectLocation(0);
                
                String sidResolveURL = sidLoc.getUrl();
                String pidResolveURL = pidLoc.getUrl();
                assertEquals("resolve() : SID and head PID should resolve() to same URL",
                        sidResolveURL, pidResolveURL);
            } catch (BaseException e) {
                throw new AssertionError("testResolve() yielded exception: " 
                        + callAdapter.getLatestRequestUrl() + " : " + e.getDetail_code() + " : " + e.getDescription() 
                        + e.getClass().getSimpleName() + ": " +  e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
            } catch (Exception e) {
                throw new AssertionError("testResolve() yielded exception: " 
                        + callAdapter.getLatestRequestUrl() + " : " 
                        + e.getClass().getSimpleName() + ": " +  e.getMessage() + ", " + (e.getCause() == null ? "" : e.getCause().getMessage()));
            }
        }
    }
    
    /**
     * Creates a test object according to the parameters provided. If createOnMN is specified,
     * will only create on a v2 MN.
     * Also allows setting the SID and the obsoletes / obsoletedBy chain.
     * If <code>obsoletesId</code> or <code>obsoletedById</code> are set, we need to
     * make multiple calls - to create() and to updateSystemMetadata() since setting
     * obsoletes or obsoletedBy on system metadata on create is invalid.
     * Holds some special logic for the CN tests: If the <tt>createOnMN</tt> parameter is set, 
     * it'll do a listNodes call, look for a working MN, create the object there, 
     * then wait for sync to happen. This is necessary for testing resolve() at the moment.
     * 
     * @param callAdapter - the adapter for the node we're creating the object on
     * @param pid - the identifier for the create object
     * @param sid - the series identifier for the given pid
     * @param obsoletesId - an {@link Identifier} for the previous object in the chain (optional)
     * @param obsoletedById an {@link Identifier} for the next object in the chain (optional)
     * @param createOnMN whether the created test object needs to exist and therefore be created on
     *              an MN, not just on the CN (we'll need to wait for it to sync to the CN)
     * @return the Identifier for the created object
     */
    public Identifier createTestObject(CNCallAdapter callAdapter, Identifier pid,
            Identifier sid, Identifier obsoletesId, Identifier obsoletedById, boolean createOnMN) throws InvalidToken,
            ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
            InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
            UnsupportedEncodingException, NotFound, ClientSideException {

        if(!createOnMN) // CN.create() is simple
            return super.createTestObject(callAdapter, pid, sid, obsoletesId, obsoletedById);
        
        String sidVal = sid == null ? "null" : sid.getValue();
        String obsoletesVal = obsoletesId == null ? "null" : obsoletesId.getValue();
        String obsoletedVal = obsoletedById == null ? "null" : obsoletedById.getValue();
        logger.info("creating test object (on MN)... pid: " + pid.getValue() 
                + " with a sid: " + sidVal 
                + " obsoletes: " + obsoletesVal 
                + " obsoletedBy: " + obsoletedVal);
        
        Identifier testObjPid = null;
        
        getSession("testRightsHolder");
        
        // get MN to create on
        Node mn = null;
        try {
            NodeList listOfNodes = callAdapter.listNodes();
            for (Node n : listOfNodes.getNodeList()) {
                // must be MN
                if (n.getType() != NodeType.MN)
                    continue;
                
                // must support v2
                MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                try {
                    mnCallAdapter.ping();
                } catch (BaseException be) {
                    continue;
                }
                
                try {
                    log.info("checking MN " + n.getBaseURL());
                    Node mnCapabilities = mnCallAdapter.getCapabilities();
                    
                    if(mnCapabilities.isSynchronize()) {
                        mn = n;
                        break;
                    }
                } catch (Exception e1) {
                    log.info("skipping MN " + n == null ? "null" : n.getBaseURL() + " because: "
                            + e1.getClass().getSimpleName() + " : " + e1.getMessage());
                    continue;
                }
                
            }
            assertTrue("Should be able to find a v2 MN that responds to getCapabilities() "
                    + "and supports synchronize.", mn != null);
            log.info("creating a test object on MN " + mn.getBaseURL() + " with pid: "
                    + pid.getValue() + " ..... ");
            
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), mn, "v2");
            
            try {
                if (obsoletesId == null)
                    super.createTestObject(mnCallAdapter, pid, sid, obsoletesId, obsoletedById);
                else
                    super.updateTestObject(mnCallAdapter, obsoletesId, pid, sid);
            } catch (Exception maybeBogusTimeout) {
                log.warn("pid not created: " + pid.getValue());
            }
            
            log.info("created a test object on MN " + mn.getBaseURL() + " with pid: "
                    + pid.getValue());
            log.info("waiting for object (" + pid.getValue() + ") to sync from " + mn.getBaseURL() 
                    + " to " + callAdapter.getNodeBaseServiceUrl());
            
            Thread.sleep(SYNC_TIME);
            
            log.info("test object should be synchronized to CN...");
            try {
                callAdapter.getSystemMetadata(null, pid);
            } catch (NotFound nf) {
                log.error("test object not synchronized to CN! pid: "
                        + pid.getValue() + " : " + nf.getClass().getSimpleName() 
                        + " : " + nf.getMessage());
                throw nf;
            }
        } catch (BaseException be) {
        	be.printStackTrace();
            throw new AssertionError("Unable to create test object. " + be.getClass().getSimpleName() + " : "
                    + be.getMessage() + " " + be.getDescription(), be);
        } catch (Exception e) {
            throw new AssertionError("Unable to create test object. " + e.getClass().getSimpleName() + " : "
                    + e.getMessage() + " ", e);
        }
        
        markForCleanUp(callAdapter.getNode(), pid);
        
        try {
            Thread.sleep(INDEXING_TIME);
        } catch (InterruptedException e) {}
        
        return testObjPid;
    }

    /**
     * For MN cases. The first object in a series can be created with 
     * {@link #createTestObject(CommonCallAdapter, Identifier, Identifier, Identifier, Identifier)}
     * but following objects should be updated with this method, which uses MN.update().
     * @throws NoSuchMethodException 
     */
    public Identifier updateTestObject(CommonCallAdapter callAdapter, Identifier oldPid,
            Identifier newPid, Identifier sid) throws InvalidToken, ServiceFailure, NotAuthorized,
            IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata,
            NotImplemented, InvalidRequest, NotFound, ClientSideException, IOException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        
        logger.info("UPDATING test object... pid: " + oldPid.getValue() + " with pid: " + newPid.getValue() 
                + " with a sid: " + (sid == null ? "null" : sid.getValue()));
        
        if(callAdapter.getNodeType() == NodeType.CN)
            throw new ClientSideException("Not for CN use!");
        
        getSession("testRightsHolder");
        Subject subject = getSubject("testRightsHolder");
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(subjectLabel), callAdapter.getNode(), "v2");
        byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
        D1Object d1o = new D1Object(newPid, contentBytes,
                D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                subject,
                D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
        
        SystemMetadata sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
        sysmeta.setObsoletes(oldPid);
        sysmeta.setSeriesId(sid);
        InputStream objectInputStream = null;
        Identifier updatedPid = null;
        
        objectInputStream = new ByteArrayInputStream(contentBytes);
        updatedPid = mnCallAdapter.update(null, oldPid, objectInputStream, newPid, sysmeta);

        markForCleanUp(callAdapter.getNode(), newPid);
        
        try {
            Thread.sleep(INDEXING_TIME);
        } catch (InterruptedException e) {}
        
        return updatedPid;
    }
    
}
