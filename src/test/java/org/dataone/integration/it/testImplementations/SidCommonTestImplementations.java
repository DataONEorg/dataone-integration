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
import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Ignore;
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

    private Logger logger = Logger.getLogger(SidCommonTestImplementations.class);
    protected static final String subjectLabel = cnSubmitter;
    protected final AccessPolicy policy = buildPublicReadAccessPolicy();
    
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
                    logger.error("Unable to delete Identifier \"" + idStr + "\" on node \""
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
            Thread.sleep(1);    // avoid identical Identifiers
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
    private void markForCleanUp(Node node, Identifier pid) {
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

        Identifier testObjPid = null;
        
        try {
            testObjPid = super.createTestObject(callAdapter, pid, sid, obsoletesId, obsoletedById, policy, subjectLabel, "testRightsHolder");
        } catch (BaseException be) {
            logger.error("Unable to create test object. "
                    + be.getMessage() + " " + be.getDescription(), be);
            throw be;
        }
        
        markForCleanUp(callAdapter.getNode(), pid);
        return testObjPid;
    }

    /**
     * For MN cases. The first object in a series can be created with 
     * {@link #createTestObject(CommonCallAdapter, Identifier, Identifier, Identifier, Identifier)}
     * but following objects should be updated with this method, which uses MN.update().
     */
    public Identifier updateTestObject(CommonCallAdapter callAdapter, Identifier oldPid,
            Identifier newPid, Identifier sid) throws InvalidToken, ServiceFailure, NotAuthorized,
            IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata,
            NotImplemented, InvalidRequest, NotFound, ClientSideException, IOException, NoSuchAlgorithmException, InstantiationException, IllegalAccessException, InvocationTargetException, JiBXException {
        
        if(callAdapter.getNodeType() == NodeType.CN)
            throw new ClientSideException("Not for CN use!");
        
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(subjectLabel), callAdapter.getNode(), "v2");
        byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
        D1Object d1o = new D1Object(newPid, contentBytes,
                D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                D1TypeBuilder.buildSubject(subjectLabel),
                D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
        
        SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
        sysmeta.setObsoletes(oldPid);
        InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
        
        Identifier newPid1 = mnCallAdapter.update(null, oldPid, objectInputStream, newPid, sysmeta);
        
        markForCleanUp(callAdapter.getNode(), newPid);
        return newPid1;
    }
    
    

    /**
     * Sets up each pid chain scenario, then calls getSystemMetadata() with the 
     * SID and asserts that the returned metadata's identifier matches the PID we expect.
     */
    @WebTestName("getSystemMetadata: tests that getSystemMetadata works if given a SID")
    @WebTestDescription("this test checks if calling getSystemMetadata with a SID "
            + "yields metadata that points to the expected head PID")
    public void testGetSystemMetadata() {
        logger.info("Testing getSystemMetadata() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing getSystemMetadata(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    SystemMetadata sysMeta = callAdapter.getSystemMetadata(null, sid);
                    Identifier fetchedID = sysMeta.getIdentifier();
                    assertTrue("getSystemMetaData() Case " + caseNum, fetchedID.equals(pid));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testGet() {
        logger.info("Testing get() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing get(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    InputStream sidIS = callAdapter.get(null, sid);
                    InputStream pidIS = callAdapter.get(null, pid);
                    
                    assertTrue("get() Case " + caseNum, IOUtils.contentEquals(sidIS, pidIS));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testDescribe() {
        logger.info("Testing describe() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing describe(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    DescribeResponse sidObjectDescription = callAdapter.describe(null, sid);
                    DescribeResponse pidObjectDescription = callAdapter.describe(null, pid);
                    Checksum sidChecksum = sidObjectDescription.getDataONE_Checksum();
                    Checksum pidChecksum = pidObjectDescription.getDataONE_Checksum();
                    
                    assertTrue("describe() Case " + caseNum, sidChecksum.equals(pidChecksum));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testCreate() {
        logger.info("Testing create() method ... ");
            
        Iterator<Node> nodeIter = getNodeIterator();
        while (nodeIter.hasNext()) {
            Node node = nodeIter.next();
            CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
            try {

                Identifier pid = createIdentifier("P1", node);
                Identifier sid = createIdentifier("S1", node);
                byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                
                D1Object d1o = new D1Object(pid, contentBytes,
                        D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                        D1TypeBuilder.buildSubject(subjectLabel),
                        D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                
                SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                sysmeta.setSeriesId(sid);
                InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
                
                Identifier createdPid = callAdapter.create(null, pid, objectInputStream, sysmeta);
                logger.info("Testing create(), created new object: " + createdPid);
                markForCleanUp(callAdapter.getNode(), createdPid);
                
                SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, sid);
                
                assertTrue("create() metadata for sid should match what we created ",
                        fetchedSysmeta.getIdentifier().equals(pid));
                
            } catch (BaseException e) {
                e.printStackTrace();
                handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testDelete() {
        logger.info("Testing delete() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing delete(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    Identifier deletedObjectID = callAdapter.delete(null, sid);
                    // delete(SID) should return the PID of deleted object
                    assertTrue("delete() Case " + caseNum, deletedObjectID.equals(pid));
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls archive() with the SID.
     * After this, the head PID should still be resolvable, but not show up in searches.
     * So we do a solr query and assert that it returns no results.
     */
    @WebTestName("archive: tests that archive works if given a SID")
    @WebTestDescription("this test creates a PID chain, calls archive with the SID of the chain "
            + ", then does a solr query for the head PID of the created chain, and makes sure "
            + "that it returns no results (since archive should make an identifier no longer show"
            + "up in solr queries)")
    public void testArchive() {
        logger.info("Testing archive() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing archive(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
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
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testListObjects() {
        logger.info("Testing get() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing get(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    ObjectList pidObjList = callAdapter.listObjects(null, null, null, null, pid, null, null, null);
                    ObjectList sidObjList = callAdapter.listObjects(null, null, null, null, sid, null, null, null);
                    
                    assertEquals("listObjects() Case " + caseNum + ", filter down to 1 pid", 1, pidObjList.getCount());
                    // calling listObjects() for a SID will return results for every PID under that SID
                    assertEquals("listObjects() Case " + caseNum, getPidsPerSid()[caseNum-1], sidObjList.getCount());
                
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
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
    public void testIsAuthorized() {
        logger.info("Testing isAuthorized() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing isAuthorized(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
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
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls getLogRecords() with the SID.toString()
     * and PID.toString as the idFilter parameter.
     * </p>
     * <b>Note: this test is allowed to fail.</b> From the API documentation:
     * </p>
     * <tt>
     * idFilter (string) – Return only log records for identifiers that start 
     * with the supplied identifier string. Support for this parameter is optional 
     * and MAY be ignored by the Coordinating Node implementation with no warning. 
     * Supports PID and SID values. 
     * Only PID values will be included in the returned entries.
     * </tt>
     * </p>
     * 
     */
    @WebTestName("getLogRecords: tests that getLogRecords works if given a SID")
    @WebTestDescription("... test not yet implemented ... may only need to support PIDs")
    @Ignore("... test not yet implemented ... ")
    public void testGetLogRecords() {
        logger.info("Testing getLogRecords() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing getLogRecords(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    Log sidLogRecords = callAdapter.getLogRecords(null, null, null, null, sid.toString(), null, null);
                    Log pidLogRecords = callAdapter.getLogRecords(null, null, null, null, pid.toString(), null, null);
                    int numSidRecords = sidLogRecords.getCount();
                    int numPidRecords = pidLogRecords.getCount();
    
                    // TODO  ignoring ... getLogRecords() only accepts PIDs ... probably ... 
                    
                } catch (BaseException e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getDescription());
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
            }
        }
    }
    
    @WebTestName("view: tests that view works if given a SID")
    @WebTestDescription("this test checks that calling view() with a pid and sid "
            + "yields the same result")
    public void testView() {
        logger.info("Testing view() method ... ");
        
        int[] casesToTest = getCasesToTest();
        for (int i = 0; i <= casesToTest.length; i++) {
            int caseNum = casesToTest[i];
            logger.info("Testing view(), Case" + caseNum);
            
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(subjectLabel), node, "v2");
                String setupMethodName = "setup" + node.getType() + "Case" + caseNum;
                try {
                    Method setupMethod = getSetupClass().getClass().getDeclaredMethod(setupMethodName, CommonCallAdapter.class, Node.class);
                    IdPair idPair = (IdPair) setupMethod.invoke(getSetupClass(), callAdapter, node);
                    Identifier sid = idPair.firstID;
                    Identifier pid = idPair.secondID;
        
                    InputStream sidView = callAdapter.view(null, "default", sid);
                    InputStream pidView = callAdapter.view(null, "default", pid);
                    
                    assertTrue("view() Case " + caseNum, IOUtils.contentEquals(sidView, pidView));
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    handleFail(callAdapter.getNodeBaseServiceUrl(), e.getMessage());
                }
            }
        }
    }
    
    protected static class IdPair {
        Identifier firstID, secondID;

        public IdPair(Identifier firstID, Identifier secondID) {
            this.firstID = firstID;
            this.secondID = secondID;
        }
    }
}
