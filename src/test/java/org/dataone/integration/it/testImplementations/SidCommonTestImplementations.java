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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.junit.Test;

/**
 * Contains test against the methods that accept SID {@link Identifier} parameters.
 * The tests all use the setupCase* methods to set up the different PID chain 
 * scenarios. Then they run their respective methods and test the outcomes.
 * </p>
 * The setup methods use reflection so we don't need to create over a dozen test 
 * methods for one API call with each of the setups.
 * 
 * @author Andrei
 */
public abstract class SidCommonTestImplementations extends ContextAwareTestCaseDataone {

    protected static final String subjectLabel = cnSubmitter;
    protected final AccessPolicy policy = buildPublicReadAccessPolicy();
    
    /** 
     * metacat needs time to perform indexing operations
     * (these run on a separate thread)
     **/
    protected static final int INDEXING_TIME = 10000;
    
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
     * Returns an array of ints that specify which test cases to try.
     * This is just for debugging convenience, to easily enable/disable lots
     * of tests at once. 
     * See the <code>setupCNCase*</code> and <code>setupMNCase*</code> methods
     * in subclasses.
     */
    protected abstract int[] getCasesToTest();
    
    /**
     * Returns the {@link SidCommonTestImplementations} subclass containing 
     * the setup*Case*() methods.
     * This will be {@link SidMNTestImplementations} or {@link SidCNTestImplementations}.
     * This is needed to call those methods through reflection 
     * from one place (this class), but allows us to push 
     * the MN/CN setup details into the MN/CN subclass.
     */
    protected abstract SidCommonTestImplementations getSetupClass();
    
    
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods common to MNs/CNs that accept SID parameters";
    }
    
    /**
     * Cleans up the nodes created using the {@link #createdIDs} map.
     * Since this class is no longer the test class, we can't enforce this.
     * <b>Make sure this gets called at the end of your tests</b> if you 
     * don't want a lot of nonsense objects in the system.
     */
    public void cleanUp() {
        for( Entry<Node, Set<String>> idsForNode :  createdIDs.entrySet())
        {
            Node node = idsForNode.getKey();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
            for(String idStr : idsForNode.getValue()) {
                Identifier id = new Identifier();
                id.setValue(idStr);
                try {
                    callAdapter.delete(null, id);
                } catch (InvalidToken | ServiceFailure | NotAuthorized | NotFound | NotImplemented
                        | InvalidRequest | ClientSideException e) {
                    log.error("Unable to delete Identifier \"" + idStr + "\" on node \""
                            + node.getBaseURL() + "\"", e); 
                }
            }
                
        }
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
        
        try {
            Thread.sleep(2);    // avoid identical Identifiers
        } catch (InterruptedException e) {
            // I'd be surprised
        }
        
        Identifier id = new Identifier();
        id.setValue(prefix + ExampleUtilities.generateIdentifier());
        
        return id;
    }
    
    /**
     * Makes sure we delete the created object during cleanUp()
     * @param node the the Node from which the object will need to be deleted 
     * @param pid the PID of the object to delete
     */
    protected void markForCleanUp(Node node, Identifier pid) {
        if(!createdIDs.containsKey(node))
            createdIDs.put(node, new HashSet<String>());
        Set<String> idsForNode = createdIDs.get(node);
        idsForNode.add(pid.getValue());
    }

    /**
     * Creates a test object according to the parameters provided.  
     * Also allows setting the SID and the obsoletes / obsoletedBy chain.
     * If <code>obsoletesId</code> or <code>obsoletedById</code> are set, we need to
     * make multiple calls - to create() and to updateSystemMetadata() since setting
     * obsoletes or obsoletedBy on system metadata on create is invalid.
     * 
     * @param callAdapter - the adapter for the node we're creating the object on
     * @param pid - the identifier for the create object
     * @param sid - the series identifier for the given pid
     * @param obsoletesId - an {@link Identifier} for the previous object in the chain (optional)
     * @param obsoletedById an {@link Identifier} for the next object in the chain (optional)

     * @return the Identifier for the created object
     */
    public Identifier createTestObject(CommonCallAdapter callAdapter, Identifier pid,
            Identifier sid, Identifier obsoletesId, Identifier obsoletedById) throws InvalidToken,
            ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
            InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
            UnsupportedEncodingException, NotFound, ClientSideException {

        String sidVal = sid == null ? "null" : sid.getValue();
        String obsoletesVal = obsoletesId == null ? "null" : obsoletesId.getValue();
        String obsoletedVal = obsoletedById == null ? "null" : obsoletedById.getValue();
        log.info("CREATING test object... pid: " + pid.getValue() 
                + " with a sid: " + sidVal 
                + " obsoletes: " + obsoletesVal 
                + " obsoletedBy: " + obsoletedVal);
        
        Identifier testObjPid = null;
        
        getSession("testRightsHolder");
        Subject rightsHolder = getSubject("testRightsHolder");
        try {
            testObjPid = super.createTestObject(callAdapter, pid, sid, obsoletesId, obsoletedById, policy, subjectLabel, rightsHolder.getValue());
        } catch (BaseException be) {
            log.error("Unable to create test object. "
                    + be.getMessage() + " " + be.getDescription(), be);
            throw be;
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
            NotImplemented, InvalidRequest, NotFound, ClientSideException, IOException, NoSuchAlgorithmException, 
            InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return updateTestObject(callAdapter, oldPid, newPid, sid, null);
    }
    
    /**
     * For MN cases. The first object in a series can be created with 
     * {@link #createTestObject(CommonCallAdapter, Identifier, Identifier, Identifier, Identifier)}
     * but following objects should be updated with this method, which uses MN.update().
     * @throws NoSuchMethodException 
     */
    public Identifier updateTestObject(CommonCallAdapter callAdapter, Identifier oldPid,
            Identifier newPid, Identifier sid, Identifier obsoletedBy) throws InvalidToken, ServiceFailure, NotAuthorized,
            IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata,
            NotImplemented, InvalidRequest, NotFound, ClientSideException, IOException, NoSuchAlgorithmException, 
            InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        
        log.info("UPDATING test object... pid: " + oldPid.getValue() + " with pid: " + newPid.getValue() 
                + " with a sid: " + (sid == null ? "null" : sid.getValue()));
        
        if(callAdapter.getNodeType() == NodeType.CN)
            throw new ClientSideException("Not for CN use!");
        
        Subject subject = getSubject("testRightsHolder");
        try {
            subject = policy.getAllow(0).getSubject(0);
        } catch (Exception e) {
            e.printStackTrace();
            // continue
        }
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(subjectLabel), callAdapter.getNode(), "v2");
        byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
        D1Object d1o = new D1Object(newPid, contentBytes,
                D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                subject,
                D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
        
        SystemMetadata sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
        sysmeta.setObsoletes(oldPid);
        sysmeta.setObsoletedBy(obsoletedBy);
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

    /**
     * Sets up each pid chain scenario, then calls getSystemMetadata() with the 
     * SID and asserts that the returned metadata's identifier matches the PID we expect.
     */
    @WebTestName("getSystemMetadata: tests that getSystemMetadata works if given a SID")
    @WebTestDescription("this test checks if calling getSystemMetadata with a SID "
            + "yields metadata that points to the expected head PID")
    @Test
    public void testGetSystemMetadata() {
        log.info("Testing getSystemMetadata() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing getSystemMetadata(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    Thread.sleep(INDEXING_TIME);
                    
                    SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, sid);
                    Identifier fetchedID = sysMeta.getIdentifier();
                    assertTrue("getSystemMetaData() Case " + caseNum + " : identifier in "
                            + "sysmeta fetched by the sid should be the head pid ", 
                            fetchedID.equals(pid));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls get() with the 
     * SID and the PID, then asserts that the returned data streams are equal.
     * (Note: This won't load entire object streams into memory to compare.)
     */
    @WebTestName("get: tests that get() works if given a SID")
    @WebTestDescription("this test checks that calling get() with a SID, and calling "
            + "get() with the head PID yield the exact same object")
    @Test
    public void testGet() {
        log.info("Testing get() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing get(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                InputStream sidIS = null;
                InputStream pidIS = null;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    sidIS = callAdapter.get(null, sid);
                    pidIS = callAdapter.get(null, pid);
                    
                    assertTrue("get() Case " + caseNum, IOUtils.contentEquals(sidIS, pidIS));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                } finally {
                    IOUtils.closeQuietly(sidIS);
                    IOUtils.closeQuietly(pidIS);
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls describe() with the 
     * SID and asserts that the returned data's checksum is equal to the checksum 
     * of the object of the head PID.
     */
    @WebTestName("describe: tests that describe works if given a SID")
    @WebTestDescription("this test checks that calling describe with a SID, and calling "
            + "describe with the head PID yield the same object")
    @Test
    public void testDescribe() {
        log.info("Testing describe() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing describe(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    DescribeResponse sidObjectDescription = callAdapter.describe(null, sid);
                    DescribeResponse pidObjectDescription = callAdapter.describe(null, pid);
                    Checksum sidChecksum = sidObjectDescription.getDataONE_Checksum();
                    Checksum pidChecksum = pidObjectDescription.getDataONE_Checksum();
                    
                    assertTrue("describe() Case " + caseNum + " checksums of retrieved descriptions should be the same", sidChecksum.getValue().equals(pidChecksum.getValue()));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Calls create() with the PID, and the SID sitting in the {@link SystemMetadata}. 
     * We then do a getSystemMetadata() on the head SID and make sure we 
     * get back the expected metadata of the head PID.
     * Note: this is a sanity check for create(), but we also 
     * test it a ton with all the calls during test setup. 
     */
    @WebTestName("create: tests that create works if given a SID in the system metadata")
    @WebTestDescription("this test calls create with a PID while providing a"
            + "SID in the system metadata, then fetches the system metadata using that SID, "
            + "and makes sure the PID on the metadata is the expected one we called create with")
    @Test
    public void testCreate() {
        log.info("Testing create() method ... ");
            
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext()) {
            Node node = nodeIter.next();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
            InputStream objectInputStream = null;
            try {

                Identifier pid = createIdentifier("P1", node);
                Identifier sid = createIdentifier("S1", node);
                byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                
                D1Object d1o = new D1Object(pid, contentBytes,
                        D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                        D1TypeBuilder.buildSubject(subjectLabel),
                        D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                
                SystemMetadata sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                sysmeta.setSeriesId(sid);
                objectInputStream = new ByteArrayInputStream(contentBytes);
                
                Identifier createdPid = callAdapter.create(null, pid, objectInputStream, sysmeta);
                
                log.info("Testing create(), created new object: " + createdPid);
                markForCleanUp(callAdapter.getNode(), createdPid);
                
                SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, sid);
                
                assertTrue("create() metadata for sid should match what we created ",
                        fetchedSysmeta.getIdentifier().equals(pid));
                
            } catch (BaseException e) {
                e.printStackTrace();
                handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls delete() with the SID. 
     * This should remove the head PID. We then do a get() on the head PID
     * and make sure we get back the expected {@link NotFound}
     */
    @WebTestName("delete: tests that delete works if given a SID")
    @WebTestDescription("this test creates a PID chain, calls delete with the SID of the chain "
            + ", then calls get() on the head PID of the created chain, and makes sure it's not found")
    @Test
    public void testDelete() {
        log.info("Testing delete() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing delete(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
                    
                    Thread.sleep(INDEXING_TIME);
                    
                    Identifier deletedObjectID = callAdapter.delete(null, sid);
//                    // delete(SID) should return the PID of deleted object
//                    assertTrue("testDelete(), Case " + caseNum + ", delete() should return the pid of the deleted object.", deletedObjectID.equals(pid));
                    
                    boolean notFound = false;
                    try {
                        callAdapter.get(null, pid);
                    } catch (NotFound e) {
                        // expected result
                        notFound = true;
                    } catch (Exception e1) {
                        assertTrue("testDelete(), Case " + caseNum + ", expected NotFound "
                                + "but got: " + e1.getClass().getSimpleName() + " : "
                                + e1.getMessage(),false);
                    }
                    
                    assertTrue("testDelete(), Case " + caseNum + ", object for the head pid should have been deleted by its sid.", notFound);
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getLatestRequestUrl(), "Case: " + i + " : " + e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getLatestRequestUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls listObjects(), once with the PID as a filter,
     * once with the SID as a filter. The former call should return 1 result, the latter
     * should return an expected number based on the case we're testing.
     */
    @WebTestName("listObjects: tests that archive works if given a SID")
    @WebTestDescription("this test sets up different PID chain scenarios, then calls listObjects with "
            + "the head PID and with the SID, then makes sure the former returns on result and the latter "
            + "returns the expected number of results based on the chain we created")
    @Test
    public void testListObjects() {
        log.info("Testing listObjects() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing listObjects(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    Thread.sleep(INDEXING_TIME);
                    
                    ObjectList pidObjList = callAdapter.listObjects(null, null, null, null, null, pid, null, null, null);
                    ObjectList sidObjList = callAdapter.listObjects(null, null, null, null, null, sid, null, null, null);
                    
                    assertEquals("listObjects() Case " + caseNum + ", filter down to 1 pid", 1, pidObjList.getCount());
                    // calling listObjects() for a SID will return results for every PID under that SID
                    assertEquals("listObjects() Case " + caseNum, getPidsPerSid()[caseNum-1], sidObjList.getCount());
                
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    /** 
     * Returns the number of PIDs in the SID being tested,
     * for each test case set up by setup*Case*() methods.
     * (This is the SID returned from each of these methods.)
     * Needed for {@link #testListObjects()}, and varies 
     * between CN/MN setup cases. 
     **/
    protected abstract int[] getPidsPerSid();
    
    /**
     * Sets up each pid chain scenario, then calls isAuthorized() with both the SID and head PID.
     * The best we can do with the result / behavior of isAuthorized() isn't quite enough to
     * ensure the SID was resolved correctly to the head PID, but we do at least check that
     * they either both return the same value or both throw the same exception.
     */
    @WebTestName("isAuthorized: tests that isAuthorized works if given a SID")
    @WebTestDescription("this test checks that calling isAuthorized for different permissions " + 
            " with the SID or head PID of a chain will return the same permissions")
    @Test
    public void testIsAuthorized() {
        log.info("Testing isAuthorized() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing isAuthorized(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    boolean sidRead = false,   pidRead = false;
                    String sidReadExc = "",    pidReadExc = "";
                    boolean sidWrite = false,  pidWrite = false;
                    String sidWriteExc = "",   pidWriteExc = "";
                    boolean sidChange = false, pidChange = false;
                    String sidChangeExc = "",  pidChangeExc = "";
                    
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
                    
//                } catch (BaseException e) {
//                    handleFail(callAdapter.getLatestRequestUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), "Case: " + i + " : " + e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                }
            }
        }
    }
    
    @WebTestName("view: tests that view works if given a SID")
    @WebTestDescription("this test checks that calling view() with a pid and sid "
            + "yields the same result")
    @Test
    public void testView() {
        log.info("Testing view() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i < casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            log.info("Testing view(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                CommonCallAdapter publicCallAdapter = new CommonCallAdapter(getSession(null), node, "v2");
                
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                InputStream sidView = null;
                InputStream pidView = null;
                
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.sid;
                    Identifier pid = idPair.headPid;
        
                    sidView = publicCallAdapter.view(null, "default", sid);
                    pidView = publicCallAdapter.view(null, "default", pid);
                    
                    assertTrue("view() Case " + caseNum, IOUtils.contentEquals(sidView, pidView));
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage() + (e.getCause() == null ? "" : e.getCause().getMessage()));
                } finally {
                    IOUtils.closeQuietly(sidView);
                    IOUtils.closeQuietly(pidView);
                }
            }
        }
    }
    
    /**
     * Holds a pair of {@link Identifier}s.
     * Used to store the sid and head pid it should resolve to.
     */
    protected static class IdPair {
        Identifier sid, headPid;

        public IdPair(Identifier sid, Identifier headPid) {
            this.sid = sid;
            this.headPid = headPid;
            log.info("Created SID (" + sid.getValue() + ") and head PID (" + headPid.getValue() + ") pair.");
        }
    }
}
