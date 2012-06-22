/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataone.integration.it;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1TypeBuilder;
import org.dataone.configuration.Settings;
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
import org.dataone.service.util.TypeMarshaller;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier2AuthorizationIT extends AbstractAuthorizationITDataone {
	
	private static String currentUrl;
	
	
	@Override
	protected Iterator<Node> getNodeIterator() 
	{
		return getCoordinatingNodeIterator();
	}
	
	@Override
	protected CNode instantiateD1Node(String baseUrl) 
	{ 
		return new CNode(baseUrl);
	}

	
	/**
	 * A basic test Tests the dataONE service API isAuthorized() method, checking for Read 
	 * permission on the first object returned from the Tier1 listObjects() method.  
	 * Anything other than the boolean true is considered a test failure.
	 */
	@Test
	public void testIsAuthorized() 
	{
		setupClientSubject("testPerson");
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(cn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(cn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//				Identifier pid = procurePublicReadableTestObject(cn);
				boolean success = cn.isAuthorized(null, pid, Permission.READ);
				checkTrue(cn.getLatestRequestUrl(),"isAuthorized response should never be false. [Only true or exception].", success);
			} 
    		catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	

	
	/**
	 * Creates an object with no AccessPolicy.  changes ownership to different
	 * subject, then tests original rightsholder can't access/change, and the 
	 * new one can.
	 */
	@Test
	public void testSetRightsHolder() 
	{
		Subject inheritorSubject = setupClientSubject("testPerson");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);

			try {
				Subject ownerSubject = setupClientSubject("testRightsHolder");
				
				// create a new identifier for testing, owned by current subject and with null AP
				Identifier changeableObject = APITestUtils.buildIdentifier(
						"TierTesting:setRH:" + ExampleUtilities.generateIdentifier()); 

				changeableObject = createTestObject(cn, changeableObject, null);
				
				if (changeableObject != null) {
					SystemMetadata smd = cn.getSystemMetadata(null, changeableObject);
					Identifier response = cn.setRightsHolder(
							null, 
							changeableObject,
							inheritorSubject, 
							smd.getSerialVersion().longValue());
				
					checkTrue(cn.getLatestRequestUrl(),"1. setRightsHolder(...) returns a Identifier object", response != null);
					try {
						cn.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
						handleFail(cn.getLatestRequestUrl(), "2. isAuthorized to CHANGE as former rightsHolder should fail.");
					} 
					catch (NotAuthorized e) {
						; // expected
					}
					
					setupClientSubject("testPerson");
					try {
						cn.isAuthorized(null, changeableObject, Permission.CHANGE_PERMISSION);
					} catch (NotAuthorized na) {
						handleFail(cn.getLatestRequestUrl(),"3. testPerson should now be able to CHANGE the object");
					}
					
					try {
						smd = cn.getSystemMetadata(null, changeableObject);
						checkTrue(cn.getLatestRequestUrl(), "4. testPerson should be the rightsHolder",smd.getRightsHolder().equals(inheritorSubject));
					} catch (NotAuthorized na) {
						handleFail(cn.getLatestRequestUrl(),"5. testPerson should now be able to get the systemmetadata");
					}
					// clean up step to try to put it back under the testRightsHolder subject.
					cn.setRightsHolder(null, changeableObject, ownerSubject, smd.getSerialVersion().longValue());
					
				} else {
					handleFail(cn.getLatestRequestUrl(),"could not create object for testing setRightsHolder");
				}
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
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
    @Test
	public void testSetAccessPolicy() 
    {	
       	boolean origObjectCacheSetting = Settings.getConfiguration().getBoolean("D1Client.useLocalCache");
    	Settings.getConfiguration().setProperty("D1Client.useLocalCache", false);
    	Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			try {
				Subject ownerSubject = setupClientSubject("testRightsHolder");				 
				
				// need to procure the target test object and verify 
				// that we are the rightsHolder
				Identifier changeableObject = D1TypeBuilder.buildIdentifier(
						String.format("TierTesting:%s:setAccess%s",
								createNodeAbbreviation(cn.getNodeBaseServiceUrl()),
								testObjectSeriesSuffix));
				
				SystemMetadata smd = null;
				try {
					smd = cn.getSystemMetadata(null,changeableObject);
					if (!smd.getRightsHolder().equals(ownerSubject)) 
						throw new TestIterationEndingException("the test object should be owned by "
								+ "the client subject");
				} 
				catch (NotFound e) {
					changeableObject = createTestObject(cn, changeableObject, null);
				}
					
				log.info("clear the AccessPolicy");
				long serialVersion = smd.getSerialVersion().longValue();
				boolean success = cn.setAccessPolicy(null, changeableObject, 
						new AccessPolicy(), serialVersion);

				// ensure blank policy with isAuthorized(), get()
				Subject readerSubject = setupClientSubject("testPerson");				
				try {
					cn.isAuthorized(null, changeableObject, Permission.READ);
					handleFail(cn.getLatestRequestUrl(),"1. isAuthorized by the reader should fail");
				} catch (NotAuthorized na) {
					// should fail
				}
				try {
					cn.get(null, changeableObject);
					handleFail(cn.getLatestRequestUrl(),"2. getting the newly created object as a reader should fail");
				} catch (NotAuthorized na) {
					// this is what we want
				}


				//					log.info("allow read permission for client-who-is-the-object's-RightsHolder");
				setupClientSubject("testRightsHolder");
				smd = cn.getSystemMetadata(null, changeableObject);
				serialVersion = smd.getSerialVersion().longValue();
				success = cn.setAccessPolicy(null, changeableObject, 
						AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject.getValue()},
								new Permission[] {Permission.READ}), serialVersion);
				checkTrue(cn.getLatestRequestUrl(),"3. testRightsHolder should be able to set the access policy",success);


				smd = cn.getSystemMetadata(null, changeableObject);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				TypeMarshaller.marshalTypeToOutputStream(smd.getAccessPolicy(), os);
				log.info(os.toString());


				// test for success
				log.info("trying isAuthorized as testPerson");
				setupClientSubject("testPerson");			
				try {
					cn.isAuthorized(null, changeableObject, Permission.READ);
				} catch (NotAuthorized na) {
					handleFail(cn.getLatestRequestUrl(),"4. testPerson should be authorized to read this pid '" 
							+ changeableObject.getValue() + "'");
				}

				log.info("now trying get() as testPerson");
				try {
					cn.get(null, changeableObject);
				} catch (NotAuthorized na) {
					handleFail(cn.getLatestRequestUrl(),"5. testPerson should now be able to get the object");
				}

				log.info("now try to get as a known user with no rights to the object (should not be able)");


				setupClientSubject("testSubmitter");
				try {
					InputStream is = cn.get(null, changeableObject);
					log.info(IOUtils.toString(is));
					handleFail(cn.getLatestRequestUrl(),"6. testSubmitter should not be able to get the object");

				} catch (NotAuthorized na) {
					// this is what we want
				}
				log.info("now try isAuthorized() on it as a known user with no rights to the object");
				try {
					cn.isAuthorized(null, changeableObject, Permission.READ);
					handleFail(cn.getLatestRequestUrl(),"7. testSubmitter should not be authorized to read the object");
				} catch (NotAuthorized na) {
					// this is what we want
				}

				log.info("finally test access against anonymous client");
				setupClientSubject_NoCert();
				try {
					cn.get(null, changeableObject);
					handleFail(cn.getLatestRequestUrl(),"8. anonymous client (no certificate) should not be" +
							"able to get the object");
				} catch (NotAuthorized na) {
					// this is what we want
				}

				log.info("and test isAuthorized on it with certificateless client");
				try {
					cn.isAuthorized(null, changeableObject, Permission.READ);
					handleFail(cn.getLatestRequestUrl(),"9. anonymous client (no certificate) should not be " +
							"able to get successful response from isAuthorized()");
				} catch (NotAuthorized na) {
					// this is what we want
				}
				log.info("done.");

			} catch (TestIterationEndingException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against: " + e.getMessage());
			} catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
		}
		Settings.getConfiguration().setProperty("D1Client.useLocalCache", origObjectCacheSetting);
	}
	
	

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
