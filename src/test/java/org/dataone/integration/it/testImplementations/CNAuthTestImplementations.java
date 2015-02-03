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
        Subject inheritorSubject = ContextAwareTestCaseDataone.setupClientSubject("testPerson");
        String currentUrl = node.getBaseURL();
        // TODO ^ this initialization is not like the others... is this ok?
        printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);

        CNCallAdapter callAdapter = null;
        try {
            Subject ownerSubject = ContextAwareTestCaseDataone.setupClientSubject("testRightsHolder");
            callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);

            // create a new identifier for testing, owned by current subject and with null AP
            Identifier changeableObject = APITestUtils.buildIdentifier(
                    "TierTesting:setRH:" + ExampleUtilities.generateIdentifier()); 

            changeableObject = catc.createTestObject(callAdapter, changeableObject, null);
            
            if (changeableObject != null) {
                SystemMetadata smd = callAdapter.getSystemMetadata(null, changeableObject);
                Identifier response = callAdapter.setRightsHolder(
                        null, 
                        changeableObject,
                        inheritorSubject, 
                        smd.getSerialVersion().longValue());
            
                checkTrue(callAdapter.getLatestRequestUrl(),"1. setRightsHolder(...) returns a Identifier object", response != null);
                try {
                    callAdapter.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
                    handleFail(callAdapter.getLatestRequestUrl(), "2. isAuthorized to CHANGE as former rightsHolder should fail.");
                } 
                catch (NotAuthorized e) {
                    ; // expected
                }
                
                callAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
                try {
                    callAdapter.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
                } catch (NotAuthorized na) {
                    handleFail(callAdapter.getLatestRequestUrl(),"3. testPerson should now be able to CHANGE the object");
                }
                
                try {
                    smd = callAdapter.getSystemMetadata(null, changeableObject);
                    checkTrue(callAdapter.getLatestRequestUrl(), "4. testPerson should be the rightsHolder",smd.getRightsHolder().equals(inheritorSubject));
                } catch (NotAuthorized na) {
                    handleFail(callAdapter.getLatestRequestUrl(),"5. testPerson should now be able to get the systemmetadata");
                }
                // clean up step to try to put it back under the testRightsHolder subject.
                callAdapter.setRightsHolder(null, changeableObject, ownerSubject, smd.getSerialVersion().longValue());
                
            } else {
                handleFail(callAdapter.getLatestRequestUrl(),"could not create object for testing setRightsHolder");
            }
        } 
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    
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
        Subject ownerSubject = ContextAwareTestCaseDataone.setupClientSubject("testRightsHolder");               
        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testRightsHolder"), node, version);

        boolean origObjectCacheSetting = Settings.getConfiguration().getBoolean("D1Client.useLocalCache");
        Settings.getConfiguration().setProperty("D1Client.useLocalCache", false);
        Settings.getConfiguration().setProperty("D1Client.CNode.create.timeouts", 10000);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);
        
        try {
            // need to procure the target test object and verify 
            // that we are the rightsHolder
            Identifier changeableObject = D1TypeBuilder.buildIdentifier(
                    String.format("TierTesting:%s:setAccess%s",
                            catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl()),
                            catc.getTestObjectSeriesSuffix()));
            
            SystemMetadata smd = null;
            try {
                smd = callAdapter.getSystemMetadata(null,changeableObject);
                if (!smd.getRightsHolder().equals(ownerSubject)) 
                    throw new TestIterationEndingException("the test object should be owned by "
                            + "the client subject");
            } 
            catch (NotFound e) {
                changeableObject = catc.createTestObject(callAdapter, changeableObject, null);
            }
                
            log.info("clear the AccessPolicy");
            long serialVersion = smd.getSerialVersion().longValue();
            boolean success = callAdapter.setAccessPolicy(null, changeableObject, 
                    new AccessPolicy(), serialVersion);

            // ensure blank policy with isAuthorized(), get()
            Subject readerSubject = ContextAwareTestCaseDataone.setupClientSubject("testPerson");               
            try {
                callAdapter.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapter.getLatestRequestUrl(),"1. isAuthorized by the reader should fail");
            } catch (NotAuthorized na) {
                // should fail
            }
            try {
                callAdapter.get(null, changeableObject);
                handleFail(callAdapter.getLatestRequestUrl(),"2. getting the newly created object as a reader should fail");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            // log.info("allow read permission for client-who-is-the-object's-RightsHolder");
            ContextAwareTestCaseDataone.setupClientSubject("testRightsHolder");
            smd = callAdapter.getSystemMetadata(null, changeableObject);
            serialVersion = smd.getSerialVersion().longValue();
            success = callAdapter.setAccessPolicy(null, changeableObject, 
                    AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject.getValue()},
                            new Permission[] {Permission.READ}), serialVersion);
            checkTrue(callAdapter.getLatestRequestUrl(),"3. testRightsHolder should be able to set the access policy",success);


            smd = callAdapter.getSystemMetadata(null, changeableObject);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TypeMarshaller.marshalTypeToOutputStream(smd.getAccessPolicy(), os);
            log.info(os.toString());


            // test for success
            log.info("trying isAuthorized as testPerson");
            ContextAwareTestCaseDataone.setupClientSubject("testPerson");           
            try {
                callAdapter.isAuthorized(null, changeableObject, Permission.READ);
            } catch (NotAuthorized na) {
                handleFail(callAdapter.getLatestRequestUrl(),"4. testPerson should be authorized to read this pid '" 
                        + changeableObject.getValue() + "'");
            }

            log.info("now trying get() as testPerson");
            try {
                callAdapter.get(null, changeableObject);
            } catch (NotAuthorized na) {
                handleFail(callAdapter.getLatestRequestUrl(),"5. testPerson should now be able to get the object");
            }

            log.info("now try to get as a known user with no rights to the object (should not be able)");


            ContextAwareTestCaseDataone.setupClientSubject("testSubmitter");
            try {
                InputStream is = callAdapter.get(null, changeableObject);
                log.info(IOUtils.toString(is));
                handleFail(callAdapter.getLatestRequestUrl(),"6. testSubmitter should not be able to get the object");

            } catch (NotAuthorized na) {
                // this is what we want
            }
            log.info("now try isAuthorized() on it as a known user with no rights to the object");
            try {
                callAdapter.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapter.getLatestRequestUrl(),"7. testSubmitter should not be authorized to read the object");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            log.info("finally test access against anonymous client");
            ContextAwareTestCaseDataone.setupClientSubject_NoCert();
            try {
                callAdapter.get(null, changeableObject);
                handleFail(callAdapter.getLatestRequestUrl(),"8. anonymous client (no certificate) should not be" +
                        "able to get the object");
            } catch (NotAuthorized na) {
                // this is what we want
            }

            log.info("and test isAuthorized on it with certificateless client");
            try {
                callAdapter.isAuthorized(null, changeableObject, Permission.READ);
                handleFail(callAdapter.getLatestRequestUrl(),"9. anonymous client (no certificate) should not be " +
                        "able to get successful response from isAuthorized()");
            } catch (NotAuthorized na) {
                // this is what we want
            }
            log.info("done.");

        } catch (TestIterationEndingException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against: " + e.getMessage());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
        Settings.getConfiguration().setProperty("D1Client.useLocalCache", origObjectCacheSetting);
    }
}
