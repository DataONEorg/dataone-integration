package org.dataone.integration.it.testImplementations;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.rest.HttpMultipartRestClient;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.TestIterationEndingException;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;

public class CNAuthTestImplementations extends ContextAwareAdapter {

    protected static Log log = LogFactory.getLog(CNAuthTestImplementations.class);
    
    
    public CNAuthTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("setRightsHolder - tests that setRightsHolder works")
    @WebTestDescription("first uses setRightsHolder to change the rights holder, "
            + "then calls isAuthorized with an unauthorized subject, verifies that "
            + "if fails with a NotAuthorized exception, then calls isAuthorized with "
            + "the rights holder set earlier and verifies that there's no exception" )
    public void testSetRightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetRightsHolder(nodeIterator.next(), version);
    }

    /**
     * Creates an object with no AccessPolicy.  changes ownership to different
     * subject, then tests original rightsholder can't access/change, and the 
     * new one can.
     */
    public void testSetRightsHolder(Node node, String version) 
    {
        String currentUrl = node.getBaseURL();
        printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);

        CNCallAdapter callAdapterCN = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter callAdapterRightsHolder = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        CNCallAdapter callAdapterPerson = new CNCallAdapter(getSession("testPerson"), node, version);
        Subject originalOwnerSubject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        Subject inheritorSubject = ContextAwareTestCaseDataone.getSubject("testPerson");
        
        try {
            // create a new identifier for testing, owned by current subject and with null AP
            Identifier changeableObject = APITestUtils.buildIdentifier(
                    "TierTesting:setRH:" + ExampleUtilities.generateIdentifier()); 

            changeableObject = catc.createTestObject(callAdapterCN, changeableObject, null, cnSubmitter);
            
            if (changeableObject != null) {
                SystemMetadata smd = callAdapterCN.getSystemMetadata(null, changeableObject);
                Identifier response = callAdapterCN.setRightsHolder(
                        null, 
                        changeableObject,
                        inheritorSubject, 
                        smd.getSerialVersion().longValue());
            
                checkTrue(callAdapterCN.getLatestRequestUrl(),"1. setRightsHolder(...) returns a Identifier object", response != null);
                try {
                    callAdapterRightsHolder.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
                    handleFail(callAdapterRightsHolder.getLatestRequestUrl(), "2. isAuthorized to CHANGE as former rightsHolder should fail.");
                } 
                catch (NotAuthorized e) {
                    ; // expected
                }
                
                try {
                    callAdapterPerson.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
                } catch (NotAuthorized na) {
                    handleFail(callAdapterPerson.getLatestRequestUrl(),"3. testPerson should not be able to CHANGE the object");
                }
                
                try {
                    smd = callAdapterPerson.getSystemMetadata(null, changeableObject);
                    checkTrue(callAdapterPerson.getLatestRequestUrl(), "4. testPerson should be the rightsHolder",smd.getRightsHolder().equals(inheritorSubject));
                } catch (NotAuthorized na) {
                    handleFail(callAdapterPerson.getLatestRequestUrl(),"5. testPerson should now be able to get the systemmetadata");
                }
                // clean up step to try to put it back under the testRightsHolder subject.
                callAdapterCN.setRightsHolder(null, changeableObject, originalOwnerSubject, smd.getSerialVersion().longValue());
                
            } else {
                handleFail(callAdapterCN.getLatestRequestUrl(),"could not create object for testing setRightsHolder");
            }
        } 
        catch (BaseException e) {
            handleFail(callAdapterCN.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("setAccessPolicy - tests that setAccessPolicy works")
    @WebTestDescription("finds an object that can be modified, clears its AccessPolicy, "
            + "calls isAuthorized with a non-owner subject expecting a "
            + "NotAuthorized exception, then calls setAccessPolicy as the rights holder "
            + "to set the AccessPolicy to allow read for a specific person, "
            + "verifies that this was allowed, then tries isAuthorized again "
            + "as that specific person, making sure they now have access")
    public void testSetAccessPolicy(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testSetAccessPolicy(nodeIterator.next(), version);
    }

    /**
     * Tests the dataONE service API setAccessPolicy method, and requires a
     * designated object that the "testRightsHolder" can change.
     * Outline for the test:  find the object, clear the AP, try isAuthorized()
     * with non-owner (should fail), as owner setAccessPolicy(), try isAuthorized()
     * with non-owner client who should now have access.
     * 
     * on the first object returned from the Tier1 listObjects() method.  
     * Anything other than the boolean true is considered a test failure.
     */
    public void testSetAccessPolicy(Node node, String version) 
    {   
        CNCallAdapter callAdapterCN = new CNCallAdapter(getSession(cnSubmitter), node, version);
        CNCallAdapter callAdapterRH = new CNCallAdapter(getSession("testRightsHolder"), node, version);
        CNCallAdapter callAdapterPerson = new CNCallAdapter(getSession("testPerson"), node, version);
        CNCallAdapter callAdapterSubmitter = new CNCallAdapter(getSession("testSubmitter"), node, version);
        CNCallAdapter callAdapterPublic = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        Subject ownerSubject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");               
        Subject readerSubject = ContextAwareTestCaseDataone.getSubject("testPerson");
        
        boolean origObjectCacheSetting = Settings.getConfiguration().getBoolean("D1Client.useLocalCache");
        Settings.getConfiguration().setProperty("D1Client.useLocalCache", false);
        Settings.getConfiguration().setProperty("D1Client.CNode.create.timeouts", 10000);
        String currentUrl = callAdapterCN.getNodeBaseServiceUrl();
        printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);
        
        try {
            // need to procure the target test object and verify 
            // that we are the rightsHolder
            Identifier changeableObject = D1TypeBuilder.buildIdentifier(
                    String.format("TierTesting:%s:setAccess%s",
                            catc.createNodeAbbreviation(callAdapterCN.getNodeBaseServiceUrl()),
                            catc.getTestObjectSeriesSuffix()));
            
            SystemMetadata smd = null;
            try {
                smd = callAdapterCN.getSystemMetadata(null,changeableObject);
                if (!smd.getRightsHolder().equals(ownerSubject)) 
                    throw new TestIterationEndingException("the test object should be owned by "
                            + "the client subject");
            } 
            catch (NotFound e) {
                changeableObject = catc.createTestObject(callAdapterCN, changeableObject, null, cnSubmitter);
            }
                
            log.info("clear the AccessPolicy");
            long serialVersion = smd.getSerialVersion().longValue();
            boolean success = callAdapterCN.setAccessPolicy(null, changeableObject, 
                    new AccessPolicy(), serialVersion);

            // ensure blank policy with isAuthorized(), get()
            try {
                callAdapterPerson.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapterPerson.getLatestRequestUrl(),"1. isAuthorized by the reader should fail");
            } catch (NotAuthorized na) {
                // should fail
            }
            try {
                callAdapterPerson.get(null, changeableObject);
                handleFail(callAdapterPerson.getLatestRequestUrl(),"2. getting the newly created object as a reader should fail");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            log.info("allow read permission for client-who-is-the-object's-RightsHolder");
            smd = callAdapterRH.getSystemMetadata(null, changeableObject);
            serialVersion = smd.getSerialVersion().longValue();
            success = callAdapterRH.setAccessPolicy(null, changeableObject, 
                    AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject.getValue()},
                            new Permission[] {Permission.READ}), serialVersion);
            checkTrue(callAdapterRH.getLatestRequestUrl(),"3. testRightsHolder should be able to set the access policy",success);


            smd = callAdapterRH.getSystemMetadata(null, changeableObject);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(smd.getAccessPolicy(), os);
            log.info(os.toString());


            // test for success
            log.info("trying isAuthorized as testPerson");
            try {
                callAdapterPerson.isAuthorized(null, changeableObject, Permission.READ);
            } catch (NotAuthorized na) {
                handleFail(callAdapterPerson.getLatestRequestUrl(),"4. testPerson should be authorized to read this pid '" 
                        + changeableObject.getValue() + "'");
            }

            log.info("now trying get() as testPerson");
            try {
                callAdapterPerson.get(null, changeableObject);
            } catch (NotAuthorized na) {
                handleFail(callAdapterPerson.getLatestRequestUrl(),"5. testPerson should now be able to get the object");
            }

            log.info("now try to get as a known user with no rights to the object (should not be able)");


            try {
                InputStream is = callAdapterSubmitter.get(null, changeableObject);
                log.info(IOUtils.toString(is));
                handleFail(callAdapterSubmitter.getLatestRequestUrl(),"6. testSubmitter should not be able to get the object");

            } catch (NotAuthorized na) {
                // this is what we want
            }
            log.info("now try isAuthorized() on it as a known user with no rights to the object");
            try {
                callAdapterSubmitter.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapterSubmitter.getLatestRequestUrl(),"7. testSubmitter should not be authorized to read the object");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            log.info("finally test access against anonymous client");
            try {
                callAdapterPublic.get(null, changeableObject);
                handleFail(callAdapterPublic.getLatestRequestUrl(),"8. anonymous client (no certificate) should not be" +
                        "able to get the object");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            log.info("and test isAuthorized on it with certificateless client");
            try {
                callAdapterPublic.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapterPublic.getLatestRequestUrl(),"9. anonymous client (no certificate) should not be " +
                        "able to get successful response from isAuthorized()");
            } catch (NotAuthorized na) {
                // this is what we want
            }
            log.info("done.");

        } catch (TestIterationEndingException e) {
            handleFail(callAdapterRH.getLatestRequestUrl(),"No Objects available to test against: " + e.getMessage());
        } catch (BaseException e) {
            handleFail(callAdapterRH.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
        Settings.getConfiguration().setProperty("D1Client.useLocalCache", origObjectCacheSetting);
    }
}
