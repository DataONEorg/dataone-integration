package org.dataone.integration.it.apiTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Contains test against the methods that accept SID {@link Identifier} parameters.
 * The tests all use the setupCase* methods to set up the different PID chain 
 * scenarios. Then they run their respective methods and test the outcomes.
 * </p>
 * Currently the tests use reflection so we don't need to create 18 methods 
 * to test the 18 scenarios. This may need to be expanded for clarity... 
 * 
 * @author Andrei
 */
public abstract class SidCommonIT extends ContextAwareTestCaseDataone {

    private Logger logger = Logger.getLogger(SidCommonIT.class);
    
    /** The number of PIDs in S1 for each test case set up by setupCase*() methods.
     *  Needed for {@link #testListObjects()} */
    private static final int[] pidsPerSid = {2, 2, 2, 2, 2, 
                                             2, 2, 3, 3, 3,
                                             2, 2, 2, 2, 3,
                                             2, 3, 3};
    
    /**
     * Needed for {@link #cleanUp()} after tests run.
     * {@link Map} of {@link Node} to a {@link Set} of Strings of {@link Identifier} values.
     * Tells us on which {@link Node} we created objects with which {@link Identifier}s.
     */
    private Map<Node,Set<String>> createdIDs = new HashMap<Node,Set<String>>();
    
    /**
     * Returns a {@link Node} {@link Iterator}. 
     * Should be {@link #getCoordinatingNodeIterator()} for CN implementation
     * and {@link #getMemberNodeIterator()} for MN implementation.
     */
    protected abstract Iterator<Node> getNodeIterator();
    
    
    
    /**
     * Cleans up the nodes created using the {@link #createdIDs} map.
     */
    @After
    private void cleanUp() {
        for( Entry<Node, Set<String>> idsForNode :  createdIDs.entrySet())
        {
            Node node = idsForNode.getKey();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
            for(String idStr : idsForNode.getValue()) {
                Identifier id = new Identifier();
                id.setValue(idStr);
                try {
                    callAdapter.delete(null, id);
                } catch (InvalidToken | ServiceFailure | NotAuthorized | NotFound | NotImplemented
                        | InvalidRequest | ClientSideException e) {
                    logger.error("Unable to delete Identifier \"" + idStr + "\" on node \""
                            + node.getBaseURL() + "\"", e); 
                }
            }
                
        }
    }
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods common to MNs/CNs that accept SID parameters";
    }
    
    /**
     * Creates an identifier starting with the given prefix. 
     * Uses the current time in milliseconds to generate the rest of the ID.
     * @param prefix the String prefix at the start of the {@link Identifier}
     * @param node the {@link Node} on which the Identifier's object will be put. 
     *                  Required for clean-up after tests run.
     * @return the {@link Identifier} with the given <code>prefix</code>
     */
    protected Identifier createIdentifier(String prefix, Node node) {
        Identifier id = new Identifier();
        id.setValue(prefix + ExampleUtilities.generateIdentifier());
        // TODO do these need to be reserved before the objects are created?
        //          NO for create()
        //          YES for update()    <-- weird... test w/o this first
        
        if(!createdIDs.containsKey(node))
            createdIDs.put(node, new HashSet<String>());
        Set<String> idsForNode = createdIDs.get(node);
        idsForNode.add(id.getValue());

        return id;
    }
    
    /**
     * Sets the obsoletedBy parameter on the object with the given <code>pid</code>
     * to be the <code>obsoletedByPid</code>.
     * </p>
     * In the case where <code>node</code> is a {@link CNCallAdapter}, we just call
     * setObsoletedBy(). 
     * </p>
     * In the case where <code>node</code> is an {@link MNCallAdapter}
     * we need to use a work-around since MNs don't provide a setObsoletedBy() call.
     * Instead we need to use update() with the following object (<code>obsoletedByPid</code>)
     * whose <code>obsoletes</code> is set to <code>pid</code>. 
     * <b>After this, the object for <code>obsoletedByPid</code> is deleted!</b>
     * 
     * @param node the {@link CNCallAdapter} on which to call setObsoletedBy
     * @param pid the {@link Identifier} of the object whose obsoletedBy we're changing
     * @param obsoletedByPid the {@link Identifier} of the object doing the obsoleting
     * @param serialVersion the version of the {@link SystemMetadata}
     */
    protected void setObsoletedBy(CommonCallAdapter node, Identifier pid, Identifier obsoletedByPid, long serialVersion, 
            String submitter)
            throws NotImplemented, NotFound, NotAuthorized, ServiceFailure, InvalidRequest,
            InvalidToken, ClientSideException {
        try {
            if(node instanceof CNCallAdapter)
                ((CNCallAdapter)node).setObsoletedBy(null, pid, obsoletedByPid, serialVersion);
            else if(node instanceof MNCallAdapter) {

                // in order to set obsoletedBy on an object, we need a work-around
                // we need to use update() on the following PID
                
                byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                D1Object d1o = new D1Object(pid, contentBytes,
                        D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                        D1TypeBuilder.buildSubject(submitter),
                        D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                
                SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                sysmeta.setObsoletes(pid);
                
                InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
                ((MNCallAdapter)node).update(null, obsoletedByPid, objectInputStream, obsoletedByPid, sysmeta);
                ((MNCallAdapter)node).delete(null, obsoletedByPid);
            }
        } catch (VersionMismatch e) {
            throw new ServiceFailure(e.getDetail_code(), "VersionMismatch : " + e.getDescription());
        } catch (Exception e) {
            throw new ServiceFailure("", "VersionMismatch : " + e.getMessage());
        }
    }
    
    protected IdPair setupCase1(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 1. P1(S1) <-> P2(S1),  S1 = P2 (Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");

        return new IdPair(s1, p2);
    }
    
    protected IdPair setupCase2(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 2. P1(S1) ? P2(S1), S1 = P2, Error condition, P2 not allowed (should not exist) (Rule 2)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase3(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 3. P1(S1) <- P2(S1), S1 = P2, Discouraged, but not error condition, S1 = P2 (Rule 2, missingFields)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase4(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 4. P1(S1) <-> P2(S1) <-> P3(S2), S1 = P2(use Rule 3), S2 = P3 (use Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase5(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 5. P1(S1) <- P2(S1) <- P3(S2), S1 = P2 (use Rule 2 or have missing field), S2 = P3  (use Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, true, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase6(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 6. P1(S1) <-> P2(S1) <-> P3(), S1 = P2 (use Rule 3)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, null, p2, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase7(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 7. P1(S1) <-> P2(S1) <-> P3() <-> P4(S2), S1 = P2 (Rule 3), S2 = P4 (Rule 1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, null, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s2, p3, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase8(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 8. P1(S1) <-> P2(S1) ->  ??  <- P4(S1), S1 = P4, (Rule 1) (Error, but will happen)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        // TODO ^ serialVersion ????
        // may need to fetch metadata after creation to get version
        createTestObject(callAdapter, p4, s1, p3, false, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p4);
    }

    protected IdPair setupCase9(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 9. P1(S1) <-> P2(S1)  ??  <- P4(S1), S1 = P4 (Rule 2) (??: object was not synchronized)
        // TODO ..... how is this any different from Case 8 ?
        
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        // TODO ^ serialVersion ???? 
        createTestObject(callAdapter, p4, s1, p3, false, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p4);
    }

    protected IdPair setupCase10(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 10: P1(S1) <-> P2(S1) ->  XX  <- P4(S1), S1 = P4, (Rule 1) (XX: object P3 was deleted)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s1, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s1, p3, true, true, policy, cnSubmitter, "testRightsHolder");
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCase11(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 11: P1(S1) <-> P2(S1) <-> [archived:P3(S1)], S1 = P3, (Rule 1) 
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s1, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        callAdapter.archive(null, p3);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupCase12(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 12. P1(S1) <-> P2(S1) -> ??, S1 = P2, (Rule 4) (Error, but will happen)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        // TODO ^ may not work due to validation?
        
        return new IdPair(s1, p2);
    }
    
    protected IdPair setupCase13(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 13. P1(S1) <- P2(S1) -> ??, S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
//        createTestObject(callAdapter, p3, s1, p2, false, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase14(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 14: P1(S1) <- P2(S1) -> P3(S2).
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, false, true, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase15(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 15. P1(S1) <-> P2(S1)  ?? <- P4(S1) <-> P5(S2), S1 = P4 (Rule 1) 
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier p5 = createIdentifier("P5_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s1, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p5, s2, p4, true, true, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCase16(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 16. P1(S1) <- P2(S1) -> ?? <-P4(S2) S1 = P2 (two ends, not an ideal chain), S2=P4 (rule1)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        createTestObject(callAdapter, p4, s2, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupCase17(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 17. P1(S1) <- P2(S1) -> ?? <-P4(S1) S1 = P4 (P1 and P4 are two ends, not an ideal chain)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        createTestObject(callAdapter, p4, s1, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p4);
    }

    protected IdPair setupCase18(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 18. P1(S1) <->P2(S1) -> ??  ???<-P5(S1) S1 = P5 (P2 is a type 2 end and P4  is a type 1 end, not an ideal chain)
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier p4 = createIdentifier("P4_", node);
        Identifier p5 = createIdentifier("P5_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1, cnSubmitter);
        createTestObject(callAdapter, p5, s1, p4, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p5);
    }
    
    protected static class IdPair {
        Identifier firstID, secondID;

        public IdPair(Identifier firstID, Identifier secondID) {
            this.firstID = firstID;
            this.secondID = secondID;
        }
    }

    /**
     * Sets up each pid chain scenario, then calls getSystemMetadata() with the 
     * SID and asserts that the returned metadata's identifier matches the PID we expect.
     * 
     * This method is the reflection approach.
     * It avoids creating a billion other methods, but is not great for seeing test results.
     */
    @Test
    public void testGetSystemMetadata() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException {

        logger.info("Testing getSystemMetadata() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing getSystemMetadata(), Case" + caseNum);
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, sid);
                Identifier fetchedID = sysMeta.getIdentifier();
                assertTrue("getSystemMetaData() Case " + caseNum, fetchedID.equals(pid));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls get() with the 
     * SID and asserts that the returned data is equal to the we created for the correct PID.
     */
    @Test
    public void testGet() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException {

        logger.info("Testing get() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing get(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                InputStream sidIS = callAdapter.get(null, sid);
                InputStream pidIS = callAdapter.get(null, pid);
                
                assertTrue("get() Case " + caseNum, IOUtils.contentEquals(sidIS, pidIS));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls describe() with the 
     * SID and asserts that the returned data's checksum is equal to the checksum 
     * of the object of the head PID.
     */
    @Test
    public void testDescribe() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException {

        logger.info("Testing describe() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing describe(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                DescribeResponse sidObjectDescription = callAdapter.describe(null, sid);
                DescribeResponse pidObjectDescription = callAdapter.describe(null, pid);
                Checksum sidChecksum = sidObjectDescription.getDataONE_Checksum();
                Checksum pidChecksum = pidObjectDescription.getDataONE_Checksum();
                
                assertTrue("describe() Case " + caseNum, sidChecksum.equals(pidChecksum));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls delete() with the SID. 
     * This should remove the head PID. We then do a get() on the head PID
     * and make sure we get back the expected {@link NotFound}
     */
    @Test
    public void testDelete() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest {

        logger.info("Testing delete() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing delete(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                Identifier deletedObjectID = callAdapter.delete(null, sid);
                boolean notFound = false;
                try {
                    callAdapter.get(null, pid);
                } catch (NotFound e) {
                    // expected result
                    notFound = true;
                }
                
                assertTrue("delete() Case " + caseNum, notFound);
                
                // TODO maybe delete(SID) is meant to return the PID? (API is unclear on this) 
                // If so, this test can be simplified to compare that with our PID.
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls archive() with the SID.
     * After this, the head PID should still be resolvable, but not show up in searches.
     * So we do a solr query and assert that it returns no reults.
     */
    @Test
    public void testArchive() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest {

        logger.info("Testing archive() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing archive(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                callAdapter.archive(null, sid);

                SystemMetadata sysmeta = null;
                try {
                    sysmeta = callAdapter.getSystemMetadata(null, pid);
                } catch (NotFound e) {
                    assertTrue("archive() Case " + caseNum + ", should be able to getSystemMetadata() for an archived object", false);
                }
                assertTrue("archive() Case " + caseNum + ", object should be archived", sysmeta.getArchived());
            }
        }
    }
    
    
    
    /**
     * Sets up each pid chain scenario, then calls listObjects(), once with the PID as a filter,
     * once with the SID as a filter. The former call should return 1 result, the latter
     * should return an expected number based on the case we're testing.
     */
    @Test
    public void testListObjects() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest {

        logger.info("Testing get() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing get(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                ObjectList pidObjList = callAdapter.listObjects(null, null, null, null, pid, null, null, null);
                ObjectList sidObjList = callAdapter.listObjects(null, null, null, null, sid, null, null, null);
                
                assertEquals("listObjects() Case " + caseNum + ", filter down to 1 pid", 1, pidObjList.getCount());
                // calling listObjects() for a SID will return results for every PID under that SID
                assertEquals("listObjects() Case " + caseNum, pidsPerSid[caseNum-1], sidObjList.getCount());
            }
        }
    }
    
    @Test
    @Ignore("view() is not yet implemented in client code")
    public void testView() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("Testing view() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing view(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                // TODO test view() ...
                
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls isAuthorized() with both the SID and head PID.
     * The best we can do with the result / behavior of isAuthorized() isn't quite enough to
     * ensure the SID was resolved correctly to the head PID, but we do at least check that
     * they either both return the same value or both throw the same exception.
     */
    @Test
    public void testIsAuthorized() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest {

        logger.info("Testing isAuthorized() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing isAuthorized(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CNCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                boolean sidRead = false, pidRead = false;
                String sidReadExc = "", pidReadExc = "";
                boolean sidWrite = false, pidWrite = false;
                String sidWriteExc = "", pidWriteExc = "";
                boolean sidChange = false, pidChange = false;
                String sidChangeExc = "", pidChangeExc = "";
                
                try {
                    sidRead = callAdapter.isAuthorized(null, sid, Permission.READ);
                } catch (Exception e) {
                    sidReadExc = e.getClass().getSimpleName();
                }
                try {
                    pidRead = callAdapter.isAuthorized(null, pid, Permission.READ);
                } catch (Exception e) {
                    pidReadExc = e.getClass().getSimpleName();
                }
                assertEquals("isAuthorized() Case " + caseNum + ", read permissions should match", 
                        sidRead, pidRead);
                assertEquals("isAuthorized() Case " + caseNum + ", read exceptions should match", 
                        sidReadExc, pidReadExc);
                
                try {
                    sidWrite = callAdapter.isAuthorized(null, sid, Permission.WRITE);
                } catch (Exception e) {
                    sidWriteExc = e.getClass().getSimpleName();
                }
                try {
                    pidWrite = callAdapter.isAuthorized(null, pid, Permission.WRITE);
                } catch (Exception e) {
                    pidWriteExc = e.getClass().getSimpleName();
                }
                assertEquals("isAuthorized() Case " + caseNum + ", write permissions should match", 
                        sidWrite, pidWrite);
                assertEquals("isAuthorized() Case " + caseNum + ", write exceptions should match", 
                        sidWriteExc, pidWriteExc);
                
                try {
                    sidChange = callAdapter.isAuthorized(null, sid, Permission.CHANGE_PERMISSION);
                } catch (Exception e) {
                    sidChangeExc = e.getClass().getSimpleName();
                }
                try {
                    pidChange = callAdapter.isAuthorized(null, pid, Permission.CHANGE_PERMISSION);
                } catch (Exception e) {
                    pidChangeExc = e.getClass().getSimpleName();
                }
                assertEquals("isAuthorized() Case " + caseNum + ", change permissions should match", 
                        sidChange, pidChange);
                assertEquals("isAuthorized() Case " + caseNum + ", change exceptions should match", 
                        sidChangeExc, pidChangeExc);
                
            }
        }
    }
    
    
    /*
    @Test
    public void testGetSystemMetadataCase1() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase1(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase1(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase1(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 1", fetchedID.equals(p2));
    }

    @Test
    public void testGetSystemMetadataCase2() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase2(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase2(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase2(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 2", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase3() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase3(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase3(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase3(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 3", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase4() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase4(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase4(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase4(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 4", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase5() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase5(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase5(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase5(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 5", fetchedID.equals(p2));
    }

    @Test
    public void testGetSystemMetadataCase6() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase6(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase6(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase6(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 6", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase7() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase7(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase7(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase7(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 7", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase8() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase8(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase8(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase8(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 8", fetchedID.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase9() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase9(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase9(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase9(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 9", fetchedID.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase10() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase10(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase10(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase10(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 10", fetchedID.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase11() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase11(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase11(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase11(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p3 = idPair.secondID;
        
        // confirm S1 = P3
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 11", fetchedID.equals(p3));
    }
    
    @Test
    public void testGetSystemMetadataCase12() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase12(nodeIter.next());
    }
    
    
    public void testGetSystemMetadataCase12(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase12(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 12", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase13() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase13(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase13(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase13(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 13", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase14() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase14(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase14(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase14(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 14", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase15() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase15(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase15(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase15(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 15", fetchedID.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase16() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase16(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase16(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase16(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 16", fetchedID.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase17() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase17(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase17(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase17(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 17", fetchedID.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase18() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext())
            testGetSystemMetadataCase18(nodeIter.next());
    }
    
    public void testGetSystemMetadataCase18(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase18(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p5 = idPair.secondID;
        
        // confirm S1 = P5
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedID = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 18", fetchedID.equals(p5));
    }
    */

}
