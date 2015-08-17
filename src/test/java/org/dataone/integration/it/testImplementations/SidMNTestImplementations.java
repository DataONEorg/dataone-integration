package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Ignore;
import org.junit.Test;

public class SidMNTestImplementations extends SidCommonTestImplementations {

    private Logger logger = Logger.getLogger(SidMNTestImplementations.class);
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods for MNs that accept SID parameters";
    }
    
    @Override
    protected Iterator<Node> getNodeIterator() {
        return getMemberNodeIterator();
    }
    
    @Override
    protected SidCommonTestImplementations getSetupClass() {
        return this;
    }
    
    protected int[] getCasesToTest() {
        return new int[] { 1 };//  1 , 2 , 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    }
    
    @Override
    protected int[] getPidsPerSid() {
        return new int[] {  1, 2, 1, 1, 1, 1, 3, 2, 2, 1,
                            2, 1, 2, 1, 1 };
    }
    
    protected IdPair setupMNCase1(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 1   P1(S1)   S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);

        return new IdPair(s1, p1);
    }
    
    protected IdPair setupMNCase2(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 2   P1(S1) <-> P2(S1)   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        updateTestObject(callAdapter, p1, p2, s1);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupMNCase3(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 5   P1(S1) <-> P2(S2)  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        updateTestObject(callAdapter, p1, p2, s2);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupMNCase4(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 6   P1(S1) <-> P2(S2)  S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        updateTestObject(callAdapter, p1, p2, s2);
        
        return new IdPair(s2, p2);
    }

    protected IdPair setupMNCase5(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 7   [deleted] <- P2(S2)  S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        updateTestObject(callAdapter, p1, p2, s2);
        callAdapter.delete(null, p1);
        
        return new IdPair(s2, p2);
    }

    protected IdPair setupMNCase6(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 8   P1(S1) -> [deleted]  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, null);
        updateTestObject(callAdapter, p1, p2, s2);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupMNCase7(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 9   P1(S1) <-> P2(S1) <-> P3(S1)   S1 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s1);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupMNCase8(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 10   P1(S1) -> [deleted] <- P3(S1)   S1 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s1);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupMNCase9(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 11   P1(S1) <-> P2(S1) -> [deleted]   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s1);
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupMNCase10(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 12   P1(S1) <-> P2(S1) <-> P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s2);
        
        return new IdPair(s2, p3);
    }
    
    protected IdPair setupMNCase11(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 13   P1(S1) <-> P2(S2) <-> P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s2);
        updateTestObject(callAdapter, p2, p3, s2);
        
        return new IdPair(s2, p3);
    }

    protected IdPair setupMNCase12(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 14   P1(S1) <-> [deleted] <- P3(S2)  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s2);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupMNCase13(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 15   P1(S1) <-> P2(S1) -> [deleted]   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s1);
        updateTestObject(callAdapter, p2, p3, s2);
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupMNCase14(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 13   P1(S1) -> [deleted] <- P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s2);
        updateTestObject(callAdapter, p2, p3, s2);
        callAdapter.delete(null, p2);
        
        return new IdPair(s2, p3);
    }

    protected IdPair setupMNCase15(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 13   P1(S1) <-> P2(S2) <-> [deleted]   S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createTestObject(callAdapter, p1, s1, null, p2);
        updateTestObject(callAdapter, p1, p2, s2);
        updateTestObject(callAdapter, p2, p3, s2);
        callAdapter.delete(null, p3);
        
        return new IdPair(s2, p2);
    }

    protected IdPair setupResourcePkgMNCase1(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 1   P1(S1)   S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupResourcePkgMNCase2(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 2   P1(S1) <-> P2(S1)   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s1, p1, null);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupResourcePkgMNCase3(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 5   P1(S1) <-> P2(S2)  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s1, p1, null);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupResourcePkgMNCase4(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 6   P1(S1) <-> P2(S2)  S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s2, p1, null);
        
        return new IdPair(s2, p2);
    }

    protected IdPair setupResourcePkgMNCase5(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 7   [deleted] <- P2(S2)  S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s1, p1, null);
        callAdapter.delete(null, p1);
        
        return new IdPair(s2, p2);
    }

    protected IdPair setupResourcePkgMNCase6(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 8   P1(S1) -> [deleted]  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s2, p1, null);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupResourcePkgMNCase7(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 9   P1(S1) <-> P2(S1) <-> P3(S1)   S1 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s1, p2, null);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupResourcePkgMNCase8(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 10   P1(S1) -> [deleted] <- P3(S1)   S1 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createPackage(callAdapter, p1, s1, null, null);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s1, p2, null);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p3);
    }

    protected IdPair setupResourcePkgMNCase9(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 11   P1(S1) <-> P2(S1) -> [deleted]   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s1, p2, null);
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupResourcePkgMNCase10(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
    
        // Case 12   P1(S1) <-> P2(S1) <-> P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        
        return new IdPair(s2, p3);
    }
    
    protected IdPair setupResourcePkgMNCase11(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 13   P1(S1) <-> P2(S2) <-> P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s2, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        
        return new IdPair(s2, p3);
    }

    protected IdPair setupResourcePkgMNCase12(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {
        
        // Case 14   P1(S1) <-> [deleted] <- P3(S2)  S1 = P1
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        callAdapter.delete(null, p2);
        
        return new IdPair(s1, p1);
    }

    protected IdPair setupResourcePkgMNCase13(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 15   P1(S1) <-> P2(S1) -> [deleted]   S1 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s1, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        callAdapter.delete(null, p3);
        
        return new IdPair(s1, p2);
    }

    protected IdPair setupResourcePkgMNCase14(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 13   P1(S1) -> [deleted] <- P3(S2)   S2 = P3
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s2, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        callAdapter.delete(null, p2);
        
        return new IdPair(s2, p3);
    }

    protected IdPair setupResourcePkgMNCase15(CommonCallAdapter callAdapter, Node node) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, NotFound, ClientSideException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, JiBXException {

        // Case 13   P1(S1) <-> P2(S2) <-> [deleted]   S2 = P2
        Identifier p1 = createIdentifier("P1_", node);
        Identifier p2 = createIdentifier("P2_", node);
        Identifier p3 = createIdentifier("P3_", node);
        Identifier s1 = createIdentifier("S1_", node);
        Identifier s2 = createIdentifier("S2_", node);
        
        createPackage(callAdapter, p1, s1, null, p2);
        createPackage(callAdapter, p2, s2, p1, null);
        createPackage(callAdapter, p3, s2, p2, null);
        callAdapter.delete(null, p3);
        
        return new IdPair(s2, p2);
    }
    
    @WebTestName("getPackage: tests that getPackage() works whether given a sid and pid")
    @WebTestDescription("this test checks that calling getPackage() with a sid and with "
            + "the pid that's the head of that sid chain, then compares the resulting "
            + "InputStreams for equality")
    @Test
    public void testGetPackage() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InvalidToken, ServiceFailure, NotAuthorized, InvalidRequest, NotImplemented, NotFound, ClientSideException {
        logger.info("Testing getPackage() method ... ");
        
        for (int caseNum = 1; caseNum <= 15; caseNum++) {
            logger.info("Testing getPackage(), Case" + caseNum);
            
            Method setupMethod = SidMNTestImplementations.class.getDeclaredMethod("setupResourcePkgMNCase" + caseNum, CommonCallAdapter.class, Node.class);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.sid;
                Identifier pid = idPair.headPid;
                
                ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
                
                formatID.setValue("application/zip");
                InputStream sidPkg = null;
                InputStream pidPkg = null;
                try {
                    sidPkg = callAdapter.getPackage(null, formatID, sid);
                    pidPkg = callAdapter.getPackage(null, formatID, pid);
                    assertTrue("getPackage() Case " + caseNum, IOUtils.contentEquals(sidPkg, pidPkg));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AssertionError(callAdapter.getNodeBaseServiceUrl() + " Case: " + caseNum + 
                            " : " + e.getClass().getSimpleName() + " : " + e.getMessage());
                } finally {
                    IOUtils.closeQuietly(pidPkg);
                    IOUtils.closeQuietly(sidPkg);
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario. First calls archive() on the PID. Doing this makes
     * it mandatory for an update() call on the same object to fail with an {@link InvalidRequest}.
     * Then it calls update() on the SID and expects to catch the {@link InvalidRequest}.
     */
    @WebTestName("update: tests that update fails if given a SID's head PID is archived")
    @WebTestDescription("this test checks that calling update with a SID "
            + "doesn't work if the head PID has been archived")
    @Test
    public void testUpdate() {
        logger.info("Testing update() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing update(), Case" + caseNum);
            
            NodeReference nodeId = null;
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, "v2");
                IdPair idPair = null;
                
                // will need valid node identifier for update
                try {
                    Node nodeCap = callAdapter.getCapabilities();
                    nodeId = nodeCap.getIdentifier();
                    node.setIdentifier(nodeId);
                } catch (Exception e) {
                    logger.error("Unable to fetch node identifier for node at " 
                            + node.getBaseURL(), e);
                }
                
                // setup PID chain
                try {
                    Method setupMethod = SidMNTestImplementations.class.getDeclaredMethod("setupMNCase" + caseNum, CommonCallAdapter.class, Node.class);
                    idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new AssertionError("testUpdate unable to set up : " + callAdapter.getNodeBaseServiceUrl() 
                            + " : " + e.getClass().getSimpleName() + " : " + e.getMessage());
                }
                
                Identifier sid = idPair.sid;
                Identifier pid = idPair.headPid;
    
                // archive PID
                try {
                    Thread.sleep(INDEXING_TIME);
                    callAdapter.archive(null, pid);
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", setup step failed for testUpdate(): couldn't archive().\n"
                            + "Error: " + e.getMessage(), false);
                }
                
                boolean invalidRequestCaught = false;
                try {
                    // update SID
                    Identifier newPid = createIdentifier("P9_", node);
                    byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                    D1Object d1o = new D1Object(newPid, contentBytes,
                            D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                            D1TypeBuilder.buildSubject(subjectLabel),
                            D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                    SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                    sysmeta.setAuthoritativeMemberNode(nodeId);
                    sysmeta.setObsoletes(pid);
                    InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
                    callAdapter.update(null, sid, objectInputStream, newPid, sysmeta);
                } catch (InvalidRequest ir) {
                    // expect InvalidRequest on archived object
                    invalidRequestCaught = true;
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", an exception occurred while trying to update() : " +
                            e.getClass().getSimpleName() + " : " + e.getMessage(), false);
                    e.printStackTrace();
                }
                
                assertTrue("update() Case " + caseNum + ", InvalidRequest expected on update of archived object.", invalidRequestCaught);
            }
        }
    }
    
    @WebTestName("systemMetadataChanged: ... test not yet implemented ... ")
    @WebTestDescription(" ... test not yet implemented ... ")
    @Ignore(" ... test not yet implemented ... ")
    @Test
    public void testSystemMetadataChanged() {
        logger.info("Testing systemMetadataChanged() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing systemMetadataChanged(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(subjectLabel), node, "v2");
                try {
                    Method setupMethod = SidMNTestImplementations.class.getDeclaredMethod("setupMNCase" + caseNum, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
                    
                    // TODO test systemMetadataChanged() ...
                    
                    // systemMetadataChanged() implies authoritative sysmeta was updated
                    // on another MN (and probably on the CN too, since authMN should've notified it)
                    // so update sysmeta on another MN
                    // call MN.systemMetadataChanged() - impl should be grabbing from CN using SID
                    //                              so CN does resolving, so this tests CN =/
                    // wait ... an unknown amount of time (no way to guarantee correctness here ...)
                    // check MN sysmeta against sysmeta updated to CN
                    
                    // need an existing object on authMN and anotherMN
                    //      create obj on authMN
                    //      wait or CN sync & MN replication
                    // update obj on authMN
                    // wait for CN sync
                    // call anotherMN . systemMetadataChanged(sid)
                    // verify 
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage());
                }
            }
        }
    }
        
    @WebTestName("sid reuse not allowed")
    @WebTestDescription("this test checks that reusing a sid in a pid chain yields an "
            + "InvalidSystemMetadata exception if the chain containing that sid has been"
            + "ended (obsoleted by another sid). Scenario: P1(S1) <-> P2(S2) <-> P3(S1)")
    @Test
    public void testSidReuse() {
        logger.info("testSidReuse() method ... ");
        
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext()) {
            
            Node node = nodeIter.next();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
            
            Identifier p1 = createIdentifier("P1_", node);
            Identifier p2 = createIdentifier("P2_", node);
            Identifier p3 = createIdentifier("P3_", node);
            Identifier s1 = createIdentifier("S1_", node);
            Identifier s2 = createIdentifier("S2_", node);
            
            // set up   P1(S1) <-> P2(S2) 
            try {
                createTestObject(callAdapter, p1, s1, null, p2);
                updateTestObject(callAdapter, p1, p2, s2);
            } catch (Exception e) {
                throw new AssertionError("testSidReuse() : unable to set up test! : " + e.getClass().getSimpleName() 
                        + " : " + e.getMessage());
            }

            // try to update, adding P3(S1), to create  P1(S1) <-> P2(S2) <-> P3(S1)
            try {
                updateTestObject(callAdapter, p2, p3, s1);
                throw new AssertionError("testSidReuse() : should not be able to reuse a sid "
                        + "in a pid chain if it was used earlier but obsoleted by another sid! "
                        + "However, the update() call succeeded. "
                        + "Scenario: P1(S1) <-> P2(S2) <-> P3(S1)");
            } catch (InvalidSystemMetadata e) {
                // expected
                log.info("testSidReuse() : yielded an exception on update() as expected : "
                        + e.getClass().getSimpleName() + " : " + e.getMessage());
            } catch (Exception e) {
                throw new AssertionError("testSidReuseDiffChain() : expected an InvalidSystemMetadata "
                        + "exception but got : " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        }
    }
    
    @WebTestName("sid reuse not allowed - different pid chains")
    @WebTestDescription("this test checks that reusing a sid, even in a different pid chain, "
            + "yields an InvalidSystemMetadata exception if the chain containing that sid has been "
            + "ended (obsoleted by another sid). "
            + "Scenario: P1(S1) <-> P2(S2), separate chain:  P3(S3) <-> P4(S1)")
    @Test
    public void testSidReuseDiffChain() {
        logger.info("testSidReuseDiffChain() method ... ");
        
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext()) {
            
            Node node = nodeIter.next();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
            
            Identifier p1 = createIdentifier("P1_", node);
            Identifier p2 = createIdentifier("P2_", node);
            Identifier p3 = createIdentifier("P3_", node);
            Identifier p4 = createIdentifier("P4_", node);
            Identifier s1 = createIdentifier("S1_", node);
            Identifier s2 = createIdentifier("S2_", node);
            Identifier s3 = createIdentifier("S3_", node);
            
            // set up   P1(S1) <-> P2(S2)
            //          P3(S3)
            try {
                createTestObject(callAdapter, p1, s1, null, p2);
                updateTestObject(callAdapter, p1, p2, s2);
                createTestObject(callAdapter, p3, s3, null, p2);
                
            } catch (Exception e) {
                throw new AssertionError("testSidReuseDiffChain() : unable to set up test! : " + e.getClass().getSimpleName() 
                        + " : " + e.getMessage());
            }

            // try to update, adding P4(S1)
            // to create:   P1(S1) <-> P2(S2)
            //              P3(S3) <-> P4(S1)
            try {
                updateTestObject(callAdapter, p3, p4, s1);
                throw new AssertionError("testSidReuseDiffChain() : should not be able to reuse a sid "
                        + "in a different pid chain if it was used in another pid chain and obsoleted "
                        + "by a different sid! "
                        + "However, the update() call succeeded. "
                        + "Scenario: P1(S1) <-> P2(S2),   P3(S3) <-> P4(S1)");
            } catch (InvalidSystemMetadata e) {
                // expected
                log.info("testSidReuseDiffChain() : yielded an exception on update() as expected : "
                        + e.getClass().getSimpleName() + " : " + e.getMessage());
            } catch (Exception e) {
                throw new AssertionError("testSidReuseDiffChain() : expected an InvalidSystemMetadata "
                        + "exception but got : " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        }
    }
}
