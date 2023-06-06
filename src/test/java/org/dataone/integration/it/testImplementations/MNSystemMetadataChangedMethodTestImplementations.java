package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

public class MNSystemMetadataChangedMethodTestImplementations extends ContextAwareAdapter
{
    public MNSystemMetadataChangedMethodTestImplementations(
            ContextAwareTestCaseDataone catc) {
        super(catc);
    }


    /**
     * Test the call from CN to MN.  The MN is supposed to reply before scheduling
     * it's own call to the CN.  MNs should return 'true' (no excpetion) if the 
     * object is on their node.  Otherwise, an InvalidRequest may be thrown, but
     * no guarantees.
     * 
     * This test poses as CNs from 3 different environments - only one should not
     * return a NotAuthorized
     * 
     */
    @WebTestName("systemMetadataChanged - tests that systemMetadataChanged works")
    @WebTestDescription("this test poses as CNs from 3 different environments, all "
            + "making a call to the MN. The MN is supposed to reply before scheduling "
            + "it's own call to the CN. MNs should return 'true' if the object is "
            + "on their node. The test checks that only one CN returns success ('true'), "
            + "but allows for one CN to throw an InvalidRequest exception if the object "
            + "is not on that node.")
    public void testSystemMetadataChanged(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged(Node node, String version) {

//        String[] certLabels = {"cnDevUNM1", "cnDevUNM2", "cnSandboxORC1", "cnStageUNM1"};
        String[] certLabels = {"cnDevUNM1", "cnSandboxUNM1", "cnStageUNM1"};
        MNCallAdapter[] cNodeSessions = new MNCallAdapter[]{
                new MNCallAdapter(getSession(certLabels[0]), node, version),
                new MNCallAdapter(getSession(certLabels[1]), node, version),
                new MNCallAdapter(getSession(certLabels[2]), node, version)};
//                new MNCallAdapter(getSession(certLabels[2]), node, version),
//                new MNCallAdapter(getSession(certLabels[3]), node, version)};

        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + 
                    createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
                    ":Public_READ" + getTestObjectSeriesSuffix();
            Identifier pid = this.catc.procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));

            SystemMetadata smd = mn.getSystemMetadata(null, pid);
            if (new Date().getTime() - smd.getDateUploaded().getTime() > 5000) {
                // probably synced by now, assuming no changes until
                // after sync are happening.
                Date afterCreate = new Date();

                int success = 0;
                int invReq = 0;
                int notAuth = 0;
                int other = 0;
                for (int i=0; i<cNodeSessions.length; i++) {
                    MNCallAdapter cNodeSession = cNodeSessions[i];
                    try {
                        cNodeSession.systemMetadataChanged(null, pid, 10, afterCreate);
                        log.info("success with cert : " + certLabels[i]);
                        success++;
                    } catch (InvalidRequest e) {
                        log.info("InvalidRequest with cert : " + certLabels[i]);
                        invReq++;
                    } catch (NotAuthorized e) {
                        log.info("NotAuthorized with cert : " + certLabels[i]);
                        notAuth++;
                    } catch (Exception e) {
                        log.info("Exception with cert : " + certLabels[i]);
                        other++;
                        handleFail(cNodeSession.getLatestRequestUrl(), "unexpected exception: " +
                                "systemMetadataChanged should only throw InvalidRequest" +
                                "or NotAuthorized exceptions if the service is not failing.  Got: " +
                                e.getClass() + ": " + e.getMessage());
                    }
                    
                    checkTrue(cNodeSession.getLatestRequestUrl(),
                        "the test should return at least one success, InvalidRequest, or InvalidRequest each round " 
                		+ "success=" + success + ", invReq=" + invReq + ", notAuth=" + notAuth + ", other=" + other,
                        success + invReq + notAuth > 0);
                    
                }
                // log the results before checking for failure
                log.info("success = " + success);
                log.info("InvalidRequest = " + invReq);
                log.info("NotAuthorized = " + notAuth);
                log.info("other = " + other);
                
                
                checkTrue(mn.getLatestRequestUrl(),
                        "the test should only return success or InvalidRequest for one CN (environment) "
                        + "success=" + success + ", invReq=" + invReq + ", notAuth=" + notAuth + ", other=" + other,
                        success + invReq == 1);
                

            } else {
                handleFail(mn.getLatestRequestUrl(),"systemMetadataChanged() will likely fail because" +
                        " the object is probably new and not synced, and not known to " +
                        "the CN");
            }

        }   
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    @WebTestName("systemMetadataChanged - tests with a date before the last modified date")
    @WebTestDescription("this test tries calling systemMetadataChanged with a date that "
            + "is earlier than the existing last-modified date on the system metadata, "
            + "making sure no exception is thrown as a result")
    public void testSystemMetadataChanged_EarlierDate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_EarlierDate(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_EarlierDate(Node node, String version) {
  
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, version);
        String currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + 
                    createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
                    ":Public_READ" + getTestObjectSeriesSuffix();
            Identifier pid = this.catc.procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));


            Date modDate = mn.getSystemMetadata(null, pid).getDateSysMetadataModified();
            mn.systemMetadataChanged(null, pid, 10, new Date(modDate.getTime()-10000));
        }   
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   

    }
    
    /**
     * This test tries to have a non-CN subject call the method.  should fail.
     */
    @WebTestName("systemMetadataChanged - tests with non-CN subject")
    @WebTestDescription("this test tries calling systemMetadataChanged with "
            + "a non-CN subject, making sure it results in a NotAuthorized exception")
    public void testSystemMetadataChanged_authenticatedITKuser(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_authenticatedITKuser(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_authenticatedITKuser(Node node, String version) {

        MNCallAdapter mn = new MNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged_authITKuser() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3",true);

            Identifier pid = mn.create(null,(Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], (org.dataone.service.types.v1.SystemMetadata) dataPackage[2]);

            Date afterCreate = new Date();
            mn.systemMetadataChanged(null, pid, 10, afterCreate);
        }
        catch (NotAuthorized e) {
            // expected response
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),"Expect an ITK client to receive NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
                    ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }

    @WebTestName("systemMetadataChanged - tests systemMetadataChanged together with create method")
    @WebTestDescription("this test first calls create to produce a new object, "
            + "then calls systemMetadataChanged with the new object's identifier, "
            + "making sure no exception is thrown")
    public void testSystemMetadataChanged_withCreate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_withCreate(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_withCreate(Node node, String version) 
    {
        String currentUrl = node.getBaseURL();
        MNCallAdapter cca = new MNCallAdapter(getSession(cnSubmitter), node, version);
        currentUrl = cca.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestDelete",true);

            Identifier pid = cca.create(null,(Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], (org.dataone.service.types.v1.SystemMetadata) dataPackage[2]);

            Thread.sleep(5000);
            Date afterCreate = new Date();
            cca.systemMetadataChanged(null, pid, 10, afterCreate);
        }
        catch (BaseException e) {
            handleFail(cca.getLatestRequestUrl(),"This call to create should not throw "
                    + "an exception, got: " +  e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("systemMetadataChanged - NotAuthorized without certificate")
    @WebTestDescription("this test systemMetadataChanged without a certificate, "
            + "making sure a NotAuthorized or InvalidToken exception is thrown")
    public void testSystemMetadataChanged_NotAuthPuplic(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_NotAuthPuplic(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_NotAuthPuplic(Node node, String version) 
    {
        String currentUrl = node.getBaseURL();
        MNCallAdapter mn = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged_NotAuthPuplic() vs. node: " + currentUrl);

        try {
            mn.systemMetadataChanged(null, D1TypeBuilder.buildIdentifier("bogusPid"), 10, new Date());
        }
        catch (InvalidToken | NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "NotAuthorized or InvalidToken exception if called without a certificate. Got: " 
                    +  e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "NotAuthorized or InvalidToken exception if called without a certificate. Got: " 
                    +  e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("systemMetadataChanged - NotAuthorized with the rightsHolder certificate")
    @WebTestDescription("this test systemMetadataChanged with the rightsHolder certificate, "
            + "making sure a NotAuthorized exception is thrown")
    public void testSystemMetadataChanged_NotAuthRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_NotAuthRightsHolder(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_NotAuthRightsHolder(Node node, String version) 
    {
        String currentUrl = node.getBaseURL();
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged_NotAuthRightsHolder() vs. node: " + currentUrl);

        try {
            mn.systemMetadataChanged(null, D1TypeBuilder.buildIdentifier("bogusPid"), 10, new Date());
        }
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "NotAuthorized exception if called with the rightsHolder certificate. Got: " 
                    +  e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "NotAuthorized exception if called with the rightsHolder certificate. Got: " 
                    +  e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @WebTestName("systemMetadataChanged - NotFound with bogus pid")
    @WebTestDescription("this test systemMetadataChanged with a bogus pid, "
            + "making sure a InvalidRequest exception is thrown")
    public void testSystemMetadataChanged_InvalidPid(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_InvalidPid(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_InvalidPid(Node node, String version) 
    {
        String currentUrl = node.getBaseURL();
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, version);
        currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged_InvalidPid() vs. node: " + currentUrl);

        try {
            mn.systemMetadataChanged(null, D1TypeBuilder.buildIdentifier("bogusPid"), 10, new Date());
        }
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "InvalidRequest exception if called with a bogus pid. Got: " 
                    +  e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            handleFail(mn.getLatestRequestUrl(),"This call to systemMetadataChanged should throw a "
                    + "InvalidRequest exception if called with a bogus pid. Got: "  
                    +  e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
