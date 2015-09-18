package org.dataone.integration.it.testImplementations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;

public class MNStorageTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(MNStorageTestImplementations.class);
    
    private String idPrefix = "testMNodeTier3:";
    private static Vector<String> unicodeString = null;
    private static Vector<String> escapedString = null;
    
    public MNStorageTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("create - tests that create works")
    @WebTestDescription("this test calls create, then verifies that the "
            + "returned identifier matches the one passed to create, "
            + "and also calls get in order to test that the object with "
            + "that identifier contains the text submitted")
    public void testCreate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreate(nodeIterator.next(), version);
    }

    /**
     *  Test MNStorage.create() functionality
     */
    public void testCreate(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        String currentUrl = node.getBaseURL();
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreate() vs. node: " + currentUrl);
        
        InputStream theDataObject = null;
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestCreate", true, subjectRH.getValue());
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            Identifier pid = callAdapter.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            checkEquals(callAdapter.getLatestRequestUrl(),
                    "pid of created object should equal that given",
                    ((Identifier) dataPackage[0]).getValue(), pid.getValue());

            theDataObject = callAdapter.get(null, pid);
            String objectData = IOUtils.toString(theDataObject);
            checkTrue(
                    callAdapter.getLatestRequestUrl(),
                    "should get back an object containing submitted text:"
                            + objectData.substring(0, 1000),
                    objectData.contains("IPCC Data Distribution Centre Results "));
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(theDataObject);
        }
    }

    @WebTestName("create - test with unauthorized subject")
    @WebTestDescription("this test calls create with a certificate whose "
            + "subject is not authorized and expects to get either a "
            + "NotAuthorized or InvalidToken exception")
    public void testCreate_NoCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreate_NoCert(nodeIterator.next(), version);
    }

    /**
     *  Test MNStorage.create() functionality
     */
    public void testCreate_NoCert(Node node, String version) {

        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreate_NoCert() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestCreate", true, Constants.SUBJECT_PUBLIC);

            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            callAdapter.create(null, (Identifier) dataPackage[0], (InputStream) dataPackage[1],
                    sysMetaV2);
            handleFail(callAdapter.getLatestRequestUrl(),
                    "should not be able to create an object if no certificate");
        } catch (InvalidToken na) {
            // expected behavior
        } catch (NotAuthorized na) {
            // expected behavior
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    "Expected InvalidToken or NotAuthorized, got: " + e.getClass().getSimpleName()
                            + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("create - test with challenging unicode identifiers")
    @WebTestDescription("tests create called with a variety of "
            + "unicode identifiers, verifying that create throws no exceptions")
    public void testCreateData_IdentifierEncoding(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreateData_IdentifierEncoding(nodeIterator.next(), version);
    }

    private void initializeUnicodeStrings() {
        if (unicodeString != null && escapedString != null)
            return; // already initialized
        
        unicodeString = new Vector<String>();
        escapedString = new Vector<String>();
        //   TODO: test against Unicode characters when metacat supports unicode        
        InputStream is = this.getClass().getResourceAsStream(
                "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
        //InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
        Scanner s = new Scanner(is, "UTF-8");
        String[] temp;
        int c = 0;
        try {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("common-") || line.startsWith("path-")) {
                    if (line.contains("supplementary"))
                        continue;
                    System.out.println(c++ + "   " + line);
                    temp = line.split("\t");
                    unicodeString.add(temp[0]);
                    escapedString.add(temp[1]);
                }
            }
        } finally {
            s.close();
        }
        
    }
    
    /**
     * test creation of data with challenging unicode identifier.
     */
    //    @Ignore("ignoring to save time for local testing")
    public void testCreateData_IdentifierEncoding(Node node, String version) {
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
        
        // get identifiers to check with
        initializeUnicodeStrings();

        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testCreateData_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl);

        printTestHeader("  Node:: " + currentUrl);

        for (int j = 0; j < unicodeString.size(); j++) {
            String status = "OK   ";
            String testLoc = "      ";
            InputStream data = null;
            try {
                //              String unicode = unicodeString.get(j);
                log.info("");
                log.info(j + "    unicode String:: " + unicodeString.get(j));
                String idString = idPrefix + ExampleUtilities.generateIdentifier() + "_"
                        + unicodeString.get(j);
                String idStringEscaped = idPrefix + ExampleUtilities.generateIdentifier() + "_"
                        + escapedString.get(j);

                testLoc = "generate";
                Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(idStringEscaped,
                        false, subjectRH.getValue());

                checkEquals(callAdapter.getLatestRequestUrl(),
                        "ExampleUtilities.generateTestSciDataPackage() should produce"
                                + "identifier same as the one provided",
                        ((Identifier) dataPackage[0]).getValue(), idStringEscaped);

                testLoc = "create";
                // rGuid is either going to be the escaped ID or the non-escaped ID
                org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
                SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1, SystemMetadata.class);
                Identifier rPid = callAdapter.create(null, (Identifier) dataPackage[0],
                        (InputStream) dataPackage[1], sysMetaV2);
                log.info("    == returned Guid (rPid): " + rPid.getValue());

                checkEquals(callAdapter.getLatestRequestUrl(),
                        "pid returned from create should equal that given",
                        ((Identifier) dataPackage[0]).getValue(), rPid.getValue());

                testLoc = "get   ";
                Thread.sleep(2000);
                data = callAdapter.get(null, rPid);
                checkTrue(callAdapter.getLatestRequestUrl(),
                        "get against the object should not equal null", null != data);
                //                  String str = IOUtils.toString(data);
                //                  checkTrue(callAdapter.getLatestRequestUrl(),"should be able to read the content as created ('" + str.substring(0,100) + "...')",
                //                          str.indexOf("IPCC Data Distribution Centre Results ") != -1);
                testLoc = "      ";
            } catch (BaseException e) {
                status = "Error";
                handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName()
                        + ": " + e.getDetail_code() + ": " + e.getDescription());
            } catch (Exception e) {
                status = "Error";
                e.printStackTrace();
                handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(data);
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + testLoc + ": "
                    + unicodeString.get(j));
        }
        
        System.out.println();
        for (int k = 0; k < nodeSummary.size(); k++) {
            System.out.println(nodeSummary.get(k));
        }
        System.out.println();
    }

    @WebTestName("update - tests that update works ")
    @WebTestDescription("this test creates a new object and calls update on the "
            + "object. "
            + "It verifies that the identifier returned from update matches "
            + "the new identifier given to the update call. It then gets the "
            + "updated system metadata and verifies the it has the "
            + "original identifier in obsoletes field. "
            + "It also verifies that dateSystemMetadataModified was set properly. "
            + "It then gets the original system metadata and "
            + "verifies that it has the new identifier in the obsoletedBy field. "
            + "It also verifies that a call to listObjects will contain the "
            + "updated system metadata.")
    public void testUpdate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdate(nodeIterator.next(), version);
    }

    /**
     *  Test MNStorage.update() functionality
     */
    public void testUpdate(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testUpdate() vs. node: " + currentUrl);

        try {
            Identifier originalPid = D1TypeBuilder.buildIdentifier("testUpdate_" + ExampleUtilities.generateIdentifier());
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            catc.createTestObject(callAdapter, originalPid, accessRule);
            
            SystemMetadata createdObjSysMeta = callAdapter.getSystemMetadata(null, originalPid);
            Date dateCreated = createdObjSysMeta.getDateSysMetadataModified();

            // create the new data package to update with. 
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("testUpdate_", true, subject.getValue());
            Identifier newPid = (Identifier) dataPackage[0];

            // do the update
            SystemMetadata updateSysmeta = TypeFactory.convertTypeFromType((org.dataone.service.types.v1.SystemMetadata) dataPackage[2], SystemMetadata.class);
            updateSysmeta.setAuthoritativeMemberNode(createdObjSysMeta.getAuthoritativeMemberNode());
            updateSysmeta.setOriginMemberNode(createdObjSysMeta.getOriginMemberNode());
            Identifier updatedPid = callAdapter.update(null, originalPid,
                    (InputStream) dataPackage[1], // new data
                    newPid, updateSysmeta // new sysmeta
                    );

            checkEquals(callAdapter.getLatestRequestUrl(),
                    "pid returned from update should match that given", newPid.getValue(),
                    updatedPid.getValue());

            // check obsoletes and obsoletedBy fields
            SystemMetadata updatedSysmeta = callAdapter.getSystemMetadata(null, updatedPid);
            checkEquals(callAdapter.getLatestRequestUrl(),
                    "sysmeta of updatePid should have the originalPid in obsoletes field",
                    updatedSysmeta.getObsoletes().getValue(), originalPid.getValue());

            checkTrue(callAdapter.getLatestRequestUrl(),
                    "MN should be setting the dateSystemMetadataModified property",
                    updatedSysmeta.getDateSysMetadataModified() != null);

            SystemMetadata oldSysmeta = callAdapter.getSystemMetadata(null, originalPid);
            checkEquals(callAdapter.getLatestRequestUrl(),
                    "sysmeta of original Pid should have new pid in obsoletedBy field", oldSysmeta
                            .getObsoletedBy().getValue(), updatedPid.getValue());

            // the old pid needs to be in a timebound listObject search
            ObjectList ol = APITestUtils.pagedListObjects(callAdapter, dateCreated, null, null, null, null);
            
            checkTrue(callAdapter.getLatestRequestUrl(), "Object info list shouldn't be null."
                    + "This means that the listObjects() call failed.", 
                    ol!= null && ol.getObjectInfoList() != null);
            
            boolean foundUpdatedSysmeta = false;
            for (ObjectInfo oi : ol.getObjectInfoList()) {
                if (oi.getIdentifier().getValue().equals(originalPid.getValue())) {
                    foundUpdatedSysmeta = true;
                }
            }
            checkTrue(callAdapter.getLatestRequestUrl(),
                    "should find original pid in time-bound listObject "
                            + "where start time is after original create and before update",
                    foundUpdatedSysmeta);

        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("update - tests with bad obsoletedBy info")
    @WebTestDescription("this test incorrectly sets the obsoletedBy property "
            + "instead of the obsoletes property on the system metadata before "
            + "calling update with it, expecting an InvalidSystemMetadata exception")
    public void testUpdate_badObsoletedByInfo(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdate_badObsoletedByInfo(nodeIterator.next(), version);
    }

    public void testUpdate_badObsoletedByInfo(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testUpdate_badObsoletedByInfo() vs. node: " + currentUrl);

        try {
            Identifier originalPid = D1TypeBuilder.buildIdentifier("testUpdate_" + ExampleUtilities.generateIdentifier());
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            catc.createTestObject(callAdapter, originalPid, accessRule);
            
            SystemMetadata createdObjSysMeta = callAdapter.getSystemMetadata(null, originalPid);
            
            // create the new data package to update with. 
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate", true, subject.getValue());
            Identifier newPid = (Identifier) dataPackage[0];

            //  incorrectly set the obsoletedBy property instead of obsoletes
            org.dataone.service.types.v1.SystemMetadata smd = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            smd.setObsoletedBy(originalPid);
            
            // do the update
            SystemMetadata updateSysmeta = TypeFactory.convertTypeFromType((org.dataone.service.types.v1.SystemMetadata) dataPackage[2], SystemMetadata.class);
            updateSysmeta.setAuthoritativeMemberNode(createdObjSysMeta.getAuthoritativeMemberNode());
            updateSysmeta.setOriginMemberNode(createdObjSysMeta.getOriginMemberNode());
            
            Identifier updatedPid = callAdapter.update(null, originalPid,
                    (InputStream) dataPackage[1], // new data
                    newPid, updateSysmeta);
            
            updateSysmeta = callAdapter.getSystemMetadata(null, updatedPid);
            if (updateSysmeta.getObsoletedBy() != null) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "should not be able to update with obsoletedBy " + "field set (for pid = "
                                + updatedPid.getValue() + ")");
            }
        } catch (InvalidSystemMetadata e) {
            // expected outcome
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("update - tests with bad obsoletes info")
    @WebTestDescription("this test sets the obsoletes property on the system metadata "
            + "before calling update with it, expecting an InvalidSystemMetadata exception")
    public void testUpdate_badObsoletesInfo(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdate_badObsoletesInfo(nodeIterator.next(), version);
    }

    public void testUpdate_badObsoletesInfo(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testUpdate_badObsoletesInfo() vs. node: " + currentUrl);

        try {
            Identifier originalPid = D1TypeBuilder.buildIdentifier("testUpdate_" + ExampleUtilities.generateIdentifier());
            AccessRule accessRule = new AccessRule();
            getSession("testRightsHolder");
            Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
            accessRule.addSubject(subject);
            accessRule.addPermission(Permission.CHANGE_PERMISSION);
            
            catc.createTestObject(callAdapter, originalPid, accessRule);
            
            SystemMetadata createdObjSysMeta = callAdapter.getSystemMetadata(null, originalPid);
            
            // create the new data package to update with. 
            Object []dataPackage = ExampleUtilities.generateTestSciDataPackage("testUpdate_badObsoletesInfo_", true, subject.getValue());
            Identifier newPid = (Identifier) dataPackage[0];

            //  incorrectly set the obsoletes property
            SystemMetadata updateSysmeta = TypeFactory.convertTypeFromType((org.dataone.service.types.v1.SystemMetadata) dataPackage[2], SystemMetadata.class);
            updateSysmeta.setAuthoritativeMemberNode(createdObjSysMeta.getAuthoritativeMemberNode());
            updateSysmeta.setOriginMemberNode(createdObjSysMeta.getOriginMemberNode());
            
            Identifier phonyId = new Identifier();
            phonyId.setValue("phonyId");
            updateSysmeta.setObsoletes(phonyId);
            
            // do the update
            Identifier updatedPid = callAdapter.update(null, originalPid,
                    (InputStream) dataPackage[1], // new data
                    newPid, updateSysmeta);
            handleFail(callAdapter.getLatestRequestUrl(),
                    "should not be able to update with faulty "
                            + "obsoletes information (for pid = " + updatedPid.getValue() + ")");
        } catch (InvalidSystemMetadata e) {
            // expected outcome
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("update - tests without a certificate")
    @WebTestDescription("this test creates an object and then tries, without a "
            + "certificate, to call update, expecting a NotAuthorized or "
            + "InvalidToken exception")
    public void testUpdate_NoCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdate_NoCert(nodeIterator.next(), version);
    }

    public void testUpdate_NoCert(Node node, String version) {
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testUpdate_NoCert() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestUpdate", true, subject.getValue());

            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1, SystemMetadata.class);
           Identifier pid = callAdapter.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            // create the new data package to update with
            dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate", true, subject.getValue());

            // TODO: reinstated the checks when obsolete behavior refactored.
            // update the obsoletesList
            //        newSysMeta.addObsolete(pid);

            // update the derivedFrom list
            //        newSysMeta.addDerivedFrom(pid);

            // set the new pid on the sysmeta object
            // TODO: should the MN do this?
            sysMetaV2.setIdentifier((Identifier) dataPackage[0]);

            callAdapter = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
            // do the update
            try {
                Identifier updatedPid = callAdapter.update(null, pid, // old pid
                        (InputStream) dataPackage[1], // new data
                        (Identifier) dataPackage[0], // new pid
                        sysMetaV2 // modified sysmeta
                        );

                handleFail(callAdapter.getLatestRequestUrl(),
                        "should not be able to update an object if no certificate " + "(updated  "
                                + pid + " with " + updatedPid);
            } catch (InvalidToken na) {
                // expected behavior
            } catch (NotAuthorized na) {
                // expected behavior
            } catch (BaseException e) {
                handleFail(callAdapter.getLatestRequestUrl(),
                        "Expected InvalidToken or NotAuthorized, got: "
                                + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                                + e.getDescription());
            }
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "Exception while setting up test (callAdapter.create): "
                            + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("update - tests without a certificate")
    @WebTestDescription("this test creates an object and then tries, with a "
            + "certificate that isn't authorized, to call update, expecting a NotAuthorized "
            + "exception. It also gets the system metadata and verifies that the "
            + "obsoletedBy was not modified")
    public void testUpdate_NoRightsOnObsoleted(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdate_NoRightsOnObsoleted(nodeIterator.next(), version);
    }

    public void testUpdate_NoRightsOnObsoleted(Node node, String version) {
        
        MNCallAdapter callAdapterRH = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        MNCallAdapter callAdapterSubmitter = new MNCallAdapter(getSession("testSubmitter"), node, version);
        Subject subjectSubmitter = ContextAwareTestCaseDataone.getSubject("testSubmitter");
        String currentUrl = callAdapterRH.getNodeBaseServiceUrl();
        printTestHeader("testUpdate_NoRightsOnObsoleted() vs. node: " + currentUrl);
        
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestUpdate", true, subjectRH.getValue());
            
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1,SystemMetadata.class);
            Identifier originalPid = callAdapterRH.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            // create the new data package to update with. 
            dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate", true, subjectSubmitter.getValue());
            Identifier newPid = (Identifier) dataPackage[0];

            try {
                // do the update
                SystemMetadata smdV2 = TypeFactory.convertTypeFromType((org.dataone.service.types.v1.SystemMetadata) dataPackage[2], SystemMetadata.class);
                callAdapterSubmitter.update(null, originalPid,
                        (InputStream) dataPackage[1], // new data
                        newPid, smdV2 // new sysmeta
                        );
                handleFail(callAdapterSubmitter.getLatestRequestUrl(),
                        "update from different subject should fail");
            } catch (NotAuthorized na) {
                // expected behavior, update() above should fail
            }

            SystemMetadata orig = callAdapterRH.getSystemMetadata(null, originalPid);
            String obsoletedByValue = orig.getObsoletedBy() == null ? "" : orig.getObsoletedBy()
                    .getValue();
            checkTrue(callAdapterRH.getLatestRequestUrl(),
                    "Original object should not be obsoleted, but was obsoleted by "
                            + obsoletedByValue, orig.getObsoletedBy() == null);
        } catch (BaseException e) {
            handleFail(
                    callAdapterRH.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("archive - tests that archive works")
    @WebTestDescription("this test creates an object, calls archive on it, "
            + "then checks that the identifier returned from the archive call "
            + "matches the one given and that the system metadata for the "
            + "archived object has a status of archived")
    public void testArchive(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testArchive(nodeIterator.next(), version);
    }

    /**
     *  Test MNStorage.archive() functionality
     */
    public void testArchive(Node node, String version) {
        
        MNCallAdapter callAdapterRH = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        String currentUrl = callAdapterRH.getNodeBaseServiceUrl();
        printTestHeader("testArchive() vs. node: " + currentUrl);

        Identifier pid = null;
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestDelete", true, subjectRH.getValue());
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1,SystemMetadata.class);
            pid = callAdapterRH.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            // try the archive
            Identifier archivedPid = callAdapterRH.archive(null, pid);

            checkEquals(callAdapterRH.getLatestRequestUrl(),
                    "pid returned from archive should match that given",
                    ((Identifier) dataPackage[0]).getValue(), archivedPid.getValue());

            SystemMetadata smd = callAdapterRH.getSystemMetadata(null, pid);
            checkTrue(callAdapterRH.getLatestRequestUrl(),
                    "sysmeta for archived object should have a status of archived",
                    smd.getArchived());
        } catch (BaseException e) {
            handleFail(
                    callAdapterRH.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("archive - test with a fake identifier")
    @WebTestDescription("this test calls archive with a fake identifier, "
            + "expecting a NotFound exception")
    public void testArchive_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testArchive_NotFound(nodeIterator.next(), version);
    }

    /**
     *  Test MNStorage.archive() functionality
     */
    public void testArchive_NotFound(Node node, String version) {
        
        MNCallAdapter callAdapterRH = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = callAdapterRH.getNodeBaseServiceUrl();
        printTestHeader("testArchive() vs. node: " + currentUrl);

        try {
            // try the archive
            Identifier fakePid = new Identifier();
            fakePid.setValue("fakeID." + ExampleUtilities.generateIdentifier());
            Identifier archivedPid = callAdapterRH.archive(null, fakePid);

            handleFail(callAdapterRH.getLatestRequestUrl(),
                    "member node should return NotFound if pid"
                            + "to be archived does not exist there.  Pid: " + archivedPid);
        } catch (NotFound e) {
            // expected outcome
        } catch (BaseException e) {
            handleFail(
                    callAdapterRH.getLatestRequestUrl(),
                    "Expected NotFound, got: " + e.getClass().getSimpleName() + ": "
                            + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("archive - test without a certificate")
    @WebTestDescription("this test creates an object then calls archive without "
            + "a certificate, expecting a NotAuthorized or InvalidToken exception")
    public void testArchive_NoCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testArchive_NoCert(nodeIterator.next(), version);
    }

    public void testArchive_NoCert(Node node, String version) {
       
        // subject under which to create the object to be archived
        MNCallAdapter callAdapterRH = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        MNCallAdapter callAdapterPublic = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapterRH.getNodeBaseServiceUrl();
        printTestHeader("testArchive_NoCert() vs. node: " + currentUrl);

        Identifier pid = null;
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestArchive", true, subjectRH.getValue());
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            pid = callAdapterRH.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            // try the archive
            callAdapterPublic.archive(null, pid);
            handleFail(callAdapterPublic.getLatestRequestUrl(),
                    "should not be able to archive an object if no certificate");
        } catch (InvalidToken na) {
            InputStream is = null;
            try {
                is = callAdapterRH.get(null, pid);
            } catch (BaseException e) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                                + e.getDescription());
            } catch (ClientSideException e1) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e1.getClass().getSimpleName() + ": " + e1.getMessage());
            } finally {
                IOUtils.closeQuietly(is);
            }

        } catch (NotAuthorized na) {
            InputStream is = null;
            try {
                is = callAdapterRH.get(null, pid);
            } catch (BaseException e) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                                + e.getDescription());
            }catch (ClientSideException e1) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e1.getClass().getSimpleName() + ": " + e1.getMessage());
            } finally {
                IOUtils.closeQuietly(is);
            }

        } catch (BaseException e) {
            handleFail(callAdapterRH.getLatestRequestUrl(),
                    "Expected InvalidToken or NotAuthorized, got: " + e.getClass().getSimpleName()
                            + ": " + e.getDetail_code() + ": " + e.getDescription());
        }

        catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("delete - test without a certificate")
    @WebTestDescription("this test creates an object then calls delete without "
            + "a certificate, expecting a NotAuthorized or InvalidToken exception")
    public void testDelete_NoCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testDelete_NoCert(nodeIterator.next(), version);
    }

    public void testDelete_NoCert(Node node, String version) {

        // subject under which to create the object to be archived
        MNCallAdapter callAdapterRH = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        Subject subjectRH = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        MNCallAdapter callAdapterPublic = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = callAdapterRH.getNodeBaseServiceUrl();
        printTestHeader("testDelete_NoCert() vs. node: " + currentUrl);

        Identifier pid = null;
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestDelete", true, subjectRH.getValue());
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(sysMetaV1,SystemMetadata.class);
            pid = callAdapterRH.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            // try the delete
            callAdapterPublic.delete(null, pid);
            handleFail(callAdapterPublic.getLatestRequestUrl(),
                    "should not be able to delete an object if no certificate");
        } catch (InvalidToken na) {
            InputStream is = null;
            try {
                is = callAdapterRH.get(null, pid);
            } catch (BaseException e) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                                + e.getDescription());
            }catch (ClientSideException e1) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got InvalidToken, but couldn't perform subsequent get(). Instead: "
                                + e1.getClass().getSimpleName() + ": " + e1.getMessage());
            } finally {
                IOUtils.closeQuietly(is);
            }

        } catch (NotAuthorized na) {
            InputStream is = null; 
            try {
                is = callAdapterRH.get(null, pid);
            } catch (BaseException e) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got NotAuthorized, but couldn't perform subsequent get(). Instead: "
                                + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": "
                                + e.getDescription());
            }catch (ClientSideException e1) {
                handleFail(callAdapterRH.getLatestRequestUrl(),
                        "Got NotAuthorized, but couldn't perform subsequent get(). Instead: "
                                + e1.getClass().getSimpleName() + ": " + e1.getMessage());
            } finally {
                IOUtils.closeQuietly(is);
            }

        } catch (BaseException e) {
            handleFail(
                    callAdapterPublic.getLatestRequestUrl(),
                    "Expected InvalidToken, got: " + e.getClass().getSimpleName() + ": "
                            + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

}
