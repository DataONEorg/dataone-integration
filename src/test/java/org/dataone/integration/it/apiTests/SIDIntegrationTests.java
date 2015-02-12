package org.dataone.integration.it.apiTests;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Before;
import org.junit.Test;


public class SIDIntegrationTests extends ContextAwareTestCaseDataone {

    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods that accept SID parameters";
    }
    
    private IdPair setupCase1(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 1. P1(S1) <-> P2(S1),  S1 = P2 (Rule 1)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        // TODO     ^ does the SID need to be created or reserved
        //          before it's assigned to an obsoletes/obsoletedBy PID???

        return new IdPair(s1, p2);
    }
    
    private IdPair setupCase2(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 2. P1(S1) ? P2(S1), S1 = P2, Error condition, P2 not allowed (should not exist) (Rule 2)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase3(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 3. P1(S1) <- P2(S1), S1 = P2, Discouraged, but not error condition, S1 = P2 (Rule 2, missingFields)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase4(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 4. P1(S1) <-> P2(S1) <-> P3(S2), S1 = P2(use Rule 3), S2 = P3 (use Rule 1)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase5(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 5. P1(S1) <- P2(S1) <- P3(S2), S1 = P2 (use Rule 2 or have missing field), S2 = P3  (use Rule 1)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, true, false, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase6(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 6. P1(S1) <-> P2(S1) <-> P3(), S1 = P2 (use Rule 3)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, null, p2, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase7(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 7. P1(S1) <-> P2(S1) <-> P3() <-> P4(S2), S1 = P2 (Rule 3), S2 = P4 (Rule 1)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, null, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s2, p3, true, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p2);
    }

    private IdPair setupCase8(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 8. P1(S1) <-> P2(S1) ->  ??  <- P4(S1), S1 = P4, (Rule 1) (Error, but will happen)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        // TODO ^ serialVersion ????
        // may need to fetch metadata after creation to get version
        createTestObject(callAdapter, p4, s1, p3, false, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p4);
    }

    private IdPair setupCase9(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 9. P1(S1) <-> P2(S1)  ??  <- P4(S1), S1 = P4 (Rule 2) (??: object was not synchronized)
        // TODO ..... how is this any different from Case 8 ?
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        // TODO ^ serialVersion ???? 
        createTestObject(callAdapter, p4, s1, p3, false, true, policy, cnSubmitter, "testRightsHolder");
    
        return new IdPair(s1, p4);
    }

    private IdPair setupCase10(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 10: P1(S1) <-> P2(S1) ->  XX  <- P4(S1), S1 = P4, (Rule 1) (XX: object P3 was deleted)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s1, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s1, p3, true, true, policy, cnSubmitter, "testRightsHolder");
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p4);
    }

    private IdPair setupCase11(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 11: P1(S1) <-> P2(S1) <-> [archived:P3(S1)], S1 = P3, (Rule 1) 
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s1, p2, true, true, policy, cnSubmitter, "testRightsHolder");
        callAdapter.archive(null, p3);
        
        return new IdPair(s1, p3);
    }

    private IdPair setupCase12(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
    
        // Case 12. P1(S1) <-> P2(S1) -> ??, S1 = P2, (Rule 4) (Error, but will happen)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        // TODO ^ may not work due to validation?
        
        return new IdPair(s1, p2);
    }
    
    private IdPair setupCase13(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 13. P1(S1) <- P2(S1) -> ??, S1 = P2
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
//        createTestObject(callAdapter, p3, s1, p2, false, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        
        return new IdPair(s1, p2);
    }

    private IdPair setupCase14(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 14: P1(S1) <- P2(S1) -> P3(S2).
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p3, s2, p2, false, true, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p2);
    }

    private IdPair setupCase15(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 15. P1(S1) <-> P2(S1)  ?? <- P4(S1) <-> P5(S2), S1 = P4 (Rule 1) 
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p5 = new Identifier();
        p5.setValue("P5" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p4, s1, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p5, s2, p4, true, true, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p4);
    }

    private IdPair setupCase16(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 16. P1(S1) <- P2(S1) -> ?? <-P4(S2) S1 = P2 (two ends, not an ideal chain), S2=P4 (rule1)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s2 = new Identifier();
        s2.setValue("S2" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        createTestObject(callAdapter, p4, s2, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p2);
    }

    private IdPair setupCase17(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 17. P1(S1) <- P2(S1) -> ?? <-P4(S1) S1 = P4 (P1 and P4 are two ends, not an ideal chain)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, false, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        createTestObject(callAdapter, p4, s1, p3, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p4);
    }

    private IdPair setupCase18(CNCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        
        // Case 18. P1(S1) <->P2(S1) -> ??  ???<-P5(S1) S1 = P5 (P2 is a type 2 end and P4  is a type 1 end, not an ideal chain)
        Identifier p1 = new Identifier();
        p1.setValue("P1" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p2 = new Identifier();
        p2.setValue("P2" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p3 = new Identifier();
        p3.setValue("P3" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p4 = new Identifier();
        p4.setValue("P4" + "_" + ExampleUtilities.generateIdentifier());
        Identifier p5 = new Identifier();
        p5.setValue("P5" + "_" + ExampleUtilities.generateIdentifier());
        Identifier s1 = new Identifier();
        s1.setValue("S1" + "_" + ExampleUtilities.generateIdentifier());
        
        AccessPolicy policy = buildPublicReadAccessPolicy();
        createTestObject(callAdapter, p1, s1, null, false, false, policy, cnSubmitter, "testRightsHolder");
        createTestObject(callAdapter, p2, s1, p1, true, true, policy, cnSubmitter, "testRightsHolder");
        setObsoletedBy(callAdapter, p2, p3, 1);
        createTestObject(callAdapter, p5, s1, p4, true, false, policy, cnSubmitter, "testRightsHolder");
        
        return new IdPair(s1, p5);
    }
    
    private static class IdPair {
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
     * It avoids creating 18-36 other methods, but is not great for seeing test results.
     */
    @Test
    public void testGetSystemMetadata() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException {

        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            Method setupMethod = SIDIntegrationTests.class.getDeclaredMethod("setupCase" + caseNum,
                    CNCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, sid);
                Identifier fetchedS1 = sysMeta.getIdentifier();
                assertTrue("getSystemMetaData() Case " + caseNum, fetchedS1.equals(pid));
            }
        }
    }
    
    /*
    @Test
    public void testGetSystemMetadataCase1() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase1(cnIter.next());
    }
    
    public void testGetSystemMetadataCase1(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase1(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 1", fetchedS1.equals(p2));
    }

    @Test
    public void testGetSystemMetadataCase2() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase2(cnIter.next());
    }
    
    public void testGetSystemMetadataCase2(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase2(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 2", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase3() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase3(cnIter.next());
    }
    
    public void testGetSystemMetadataCase3(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase3(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 3", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase4() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase4(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase4(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase4(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 4", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase5() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase5(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase5(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase5(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 5", fetchedS1.equals(p2));
    }

    @Test
    public void testGetSystemMetadataCase6() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase6(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase6(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase6(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 6", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase7() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase7(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase7(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase7(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 7", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase8() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase8(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase8(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase8(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 8", fetchedS1.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase9() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase9(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase9(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase9(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 9", fetchedS1.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase10() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase10(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase10(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase10(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 10", fetchedS1.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase11() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase11(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase11(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase11(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p3 = idPair.secondID;
        
        // confirm S1 = P3
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 11", fetchedS1.equals(p3));
    }
    
    @Test
    public void testGetSystemMetadataCase12() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase12(cnIter.next());
    }
    
    
    public void testGetSystemMetadataCase12(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase12(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 12", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase13() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase13(cnIter.next());
    }
    
    public void testGetSystemMetadataCase13(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase13(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 13", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase14() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase14(cnIter.next());
    }
    
    public void testGetSystemMetadataCase14(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase14(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 14", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase15() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase15(cnIter.next());
    }
    
    public void testGetSystemMetadataCase15(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase15(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 15", fetchedS1.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase16() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase16(cnIter.next());
    }
    
    public void testGetSystemMetadataCase16(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase16(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p2 = idPair.secondID;
        
        // confirm S1 = P2
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 16", fetchedS1.equals(p2));
    }
    
    @Test
    public void testGetSystemMetadataCase17() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase17(cnIter.next());
    }
    
    public void testGetSystemMetadataCase17(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase17(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p4 = idPair.secondID;
        
        // confirm S1 = P4
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 17", fetchedS1.equals(p4));
    }
    
    @Test
    public void testGetSystemMetadataCase18() throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, InstantiationException, IllegalAccessException, ClientSideException, IOException, JiBXException {
        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        while (cnIter.hasNext())
            testGetSystemMetadataCase18(cnIter.next());
    }
    
    public void testGetSystemMetadataCase18(Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, InstantiationException, IllegalAccessException, IOException, JiBXException {
        
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
        IdPair idPair = setupCase18(callAdapter, node);
        Identifier s1 = idPair.firstID;
        Identifier p5 = idPair.secondID;
        
        // confirm S1 = P5
        SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, s1);
        Identifier fetchedS1 = sysMeta.getIdentifier();
        assertTrue("getSystemMetaData() Case 18", fetchedS1.equals(p5));
    }
    */

}
