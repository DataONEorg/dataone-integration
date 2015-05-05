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
        return new int[] {  1 };//, 2 , 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
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


    @WebTestName("getPackage: ... test not yet implemented ... ")
    @WebTestDescription(" ... test not yet implemented ... ")
    @Ignore("getPackage() functionality is not yet implemented")
    @Test
    public void testGetPackage() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("Testing getPackage() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing getPackage(), Case" + caseNum);
            
            Method setupMethod = SidMNTestImplementations.class.getDeclaredMethod("setupMNCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
                
//                InputStream sidPkg = callAdapter.getPackage(null, ???ObjectFormatIdentifier???, sid);
//                InputStream pidPkg = callAdapter.getPackage(null, ???ObjectFormatIdentifier???, pid);
//                
//                assertTrue("getPackage() Case " + caseNum, IOUtils.contentEquals(sidPkg, pidPkg));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario. First calls archive() on the PID. Doing this makes
     * it mandatory for an update() call on the same object to fail with an {@link InvalidRequest}.
     * Then it calls update() on the SID and expects to catch the {@link InvalidRequest}.
     */
    @WebTestName("update: tests that update works if given a SID")
    @WebTestDescription("this test checks that calling update with a SID "
            + "doesn't work if the head PID is archived")
    @Test
    public void testUpdate() {
        logger.info("Testing update() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing update(), Case" + caseNum);
            
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = null;
                try {
                    Method setupMethod = SidMNTestImplementations.class.getDeclaredMethod("setupMNCase" + caseNum, CommonCallAdapter.class, Node.class);
                    idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
                
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                // archive PID
                try {
                    callAdapter.archive(null, pid);
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", setup step failed for testUpdate(): couldn't archive().", false);
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
                    sysmeta.setObsoletes(pid);
                    InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
                    callAdapter.update(null, sid, objectInputStream, newPid, sysmeta);
                } catch (InvalidRequest ir) {
                    // expect InvalidRequest on archived object
                    invalidRequestCaught = true;
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", an exception occurred while trying to update() : " +
                            e.getMessage(), false);
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
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    // TODO test systemMetadataChanged() ...
                    
                    // systemMetadataChanged() implies authoritative sysmeta on CN was updated
                    // so ... update sysmeta on CN
                    // call systemMetadataChanged() - impl should be grabbing from CN using SID
                    //                              so CN does resolving, so this tests CN =/
                    // wait ... an unknown amount of time (no way to guarantee correctness here ...)
                    // check MN sysmeta against sysmeta updated to CN
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
            }
        }
    }
        
        
}
