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

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

import org.dataone.client.CNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier2AuthorizationIT extends AbstractAuthorizationITDataone {
	
	private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;
 
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
				
				Identifier pid = procurePublicReadableTestObject(cn);
				boolean success = cn.isAuthorized(null, pid, Permission.READ);
				checkTrue(currentUrl,"isAuthorized response should never be false. [Only true or exception].", success);
			} 
    		catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	

	
	/**
	 * Requirements: an object whose rightsHolder can be changed without
	 * upsetting other authorization tests.	
	 * The cardinality of the rightsHolder field is [1..1].  Approach is to change
	 * to 
	 */
	@Ignore("not ready yet")
	@Test
	public void testSetRightsHolder() {
		setupClientSubject("testSubmitter");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);

			try {
//				Identifier myObject = procureTestObject(cn,new Permission[] {Permission.CHANGE_PERMISSION});
				
				Identifier pid = getOwnedObject(cn);
				// TODO:  if nothing from existing, will need to create one
				
				if (pid == null) {
					handleFail(currentUrl,"do not have an object that am rightsHolder to use to test setRightsHolder");
				} else {
					// TODO: create a real request
					Identifier pidToGiveAway = pid;
					SystemMetadata smd = cn.getSystemMetadata(null, pidToGiveAway);
					BigInteger serialVersion = smd.getSerialVersion();
				 
					Subject inheritor = new Subject();
					inheritor.setValue("CN=testSubmitter,DC=dataone,DC=org");
					Identifier response = cn.setRightsHolder(null, 
							pid,
							inheritor, serialVersion.longValue());
				
					checkTrue(currentUrl,"setRightsHolder(...) returns a Identifier object", response != null);
				}
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	
	private Identifier getOwnedObject(CNode cn) {

//		Subject me = ClientAuthUtils.whoami();
//		Subject me2 = getCurrentClientSubject();
		Identifier pid = null;
		try {
			
			ObjectList ol = cn.listObjects(null);
			for (ObjectInfo oi: ol.getObjectInfoList())
			{
				if (cn.isAuthorized(null, oi.getIdentifier(), Permission.CHANGE_PERMISSION))
				{
					SystemMetadata smd = cn.getSystemMetadata(null, oi.getIdentifier());
					boolean noCPinAP = true;
					for (AccessRule ar: smd.getAccessPolicy().getAllowList())
					{
						for (Permission p: ar.getPermissionList()) {
							if (p == Permission.CHANGE_PERMISSION) {
								noCPinAP = false;
								break;
							}
						}
					}
					if (noCPinAP) {
						pid = oi.getIdentifier();
						break;
					}
				}			
			}
		} catch (InvalidToken e) {
			//allow object list to be null
		} catch (ServiceFailure e) {
			//allow object list to be null
		} catch (NotAuthorized e) {
			//allow object list to be null
		} catch (InvalidRequest e) {
			//allow object list to be null
		} catch (NotImplemented e) {
			//allow object list to be null
		} catch (NotFound e) {
			//allow object list to be null
		}
		return pid;
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
	@Ignore("test needs review")
    @Test
	public void testSetAccessPolicy() 
    {	

       	Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			try {
				Subject ownerSubject = setupClientSubject("testRightsHolder");				 
				
				Identifier changeableObject = procureTestObject(
						cn, 
						AccessUtil.createAccessRule(
								new Subject[]{ownerSubject},
								new Permission[]{Permission.CHANGE_PERMISSION}),
						APITestUtils.buildIdentifier("TierTesting:testObject:setAccess." + testObjectSeries)
						); 
				if (changeableObject != null){
					
					log.info("clear the AccessPolicy");
					SystemMetadata smd = cn.getSystemMetadata(null,changeableObject);
					long serialVersion = smd.getSerialVersion().longValue();
					boolean success = cn.setAccessPolicy(null, changeableObject, 
							new AccessPolicy(), serialVersion);

					// ensure blank policy with isAuthorized(), get()
					Subject readerSubject = setupClientSubject("testPerson");				
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"1. isAuthorized by the reader should fail");
					} catch (NotAuthorized na) {
						// should fail
					}
					try {
						cn.get(null, changeableObject);
						handleFail(currentUrl,"2. getting the newly created object as reader should fail");
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
					checkTrue(currentUrl,"3. testRightsHolder should be able to set the access policy",success);


					// test for success
					log.info("trying isAuthorized as the testReader");
					setupClientSubject("testReader");			
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"4. testReader should be authorized to read this pid '" 
								+ changeableObject.getValue() + "'");
					}

					log.info("now trying get() as the testReader");
					try {
						cn.get(null, changeableObject);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"5. testReader should now be able to get the object");
					}

					log.info("now try to get as a known user with no rights to the object (should not be able)");
					setupClientSubject("testNoRights");
					try {
						cn.get(null, changeableObject);
						handleFail(currentUrl,"6. testNoRights should not be able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}
					log.info("now try isAuthorized() on it as a known user with no rights to the object");
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"7. testNoRights should not be able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}

//					log.info("finally test access against anonymous client");
//					setupClientSubject_NoCert();
//					try {
//						cn.get(null, changeableObject);
//						handleFail(currentUrl,"8. anonymous client (no certificate) should not be" +
//						"able to get the object");
//					} catch (NotAuthorized na) {
//						// this is what we want
//					}
//
//					log.info("and test isAuthorized on it with certificateless client");
//					try {
//						cn.isAuthorized(null, changeableObject, Permission.READ);
//						handleFail(currentUrl,"9. anonymous client (no certificate) should not be " +
//						"able to get successful response from isAuthorized()");
//					} catch (NotAuthorized na) {
//						// this is what we want
//					}
				}
			} catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			} catch (BaseException e) {
				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
    
    /**
	 * Tests the dataONE service API setAccessPolicy method, calling it with
	 * no subject/certificate, after first calling the Tier 3 create method, 
	 * to setup an object whose access policy can be manipulated for the test. 
	 * <p>
	 * Anything other than the boolean true is considered a test failure.
	 */
	@Ignore("test needs review")
    @Test
	public void testSetAccessPolicy_NoCert() 
    {	
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSetAccessPolicy_NoCert() vs. node: " + currentUrl);

			try {
				String testingSubject = "testRightsHolder";
				setupClientSubject(testingSubject);
				
				Identifier id = new Identifier(); 
				id.setValue("setAccessPolicyTestObject");
				
				Identifier changeableObject = procureTestObject(cn, 
						AccessUtil.createAccessRule(new String[]{testingSubject},
								new Permission[]{Permission.CHANGE_PERMISSION}), id);
				
				if (changeableObject != null) 
				{	
					setupClientSubject_NoCert();
					log.info("  subject cleared");
					// set access on the object
					SystemMetadata smd = cn.getSystemMetadata(null, changeableObject);
					boolean success = cn.setAccessPolicy(null, changeableObject, 
							AccessUtil.createSingleRuleAccessPolicy(new String[]{"foo"},
									new Permission[]{Permission.READ}),
									smd.getSerialVersion().longValue());
					handleFail(currentUrl,"with no client certificate, setAccessPolicy should throw exception");
				}
			} catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			} catch (BaseException e) {
				checkEquals(currentUrl, "with no client certificate: " + e.getDetail_code() + ": " + e.getDescription(), 
						e.getClass().getSimpleName(), "NotAuthorized");
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
		}
    }

	
  /** 
  * Implementation for setAccess Tests for various types of users
  * @param subject - the subject that 
  * @param clientSetupMethod
  */
	private void runSetAccessForSymbolicPrincipal(String subject, String clientSubjectName)
	{
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetAccess_" + subject + "() vs. node: " + currentUrl);

			try {
				setupClientSubject(clientSubjectName);
				
				Identifier id = new Identifier(); id.setValue("setAccessPolicyTestObject");
				
				Identifier changeableObject = procureTestObject(cn, 
						AccessUtil.createAccessRule(new String[]{clientSubjectName},
								new Permission[]{Permission.CHANGE_PERMISSION}), id);
				
				if (changeableObject != null)
				{
					log.info("clear the AccessPolicy");
					cn.setAccessPolicy(null, changeableObject, new AccessPolicy(), 1);

					// ensure subject under test isn't authorized with get, isAuthorized
					// prior to setting up the symbolic principal in accessPolicy
					setupClientSubject("NoCert");
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"1. isAuthorized by " + subject + " should fail");
					} catch (NotAuthorized na) {
						// should fail
					}
					try {
						cn.get(null, changeableObject);
						handleFail(currentUrl,"2. getting the object as " + subject + " should fail");
					} catch (NotAuthorized na) {
						// this is what we want
					}

					try {
						setupClientSubject("testWriter");
						cn.setAccessPolicy(null, changeableObject, 
								AccessUtil.createSingleRuleAccessPolicy(
										new String[]{subject},
										new Permission[]{Permission.READ}), 1);
					}
					catch (BaseException e) {
						handleFail(currentUrl, "3. testWriter should be able to set the access policy, " +
								"but got: " + e.getClass().getSimpleName() + ": " + e.getDescription());
					}

					// test for success
					log.info("trying isAuthorized as " + clientSubjectName + " (as " + subject + ")");
					setupClientSubject("NoCert");
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"4. " + subject + " should be authorized to read this pid '" 
								+ changeableObject.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
								+ na.getDetail_code() + ": " + na.getDescription());
					}

					log.info("trying get as " + clientSubjectName + " (as " + subject + ")");
					try {
						cn.get(null, changeableObject);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"5. " + subject + " should now be able to get the object");
					}
				} else {
					handleFail(currentUrl,"No object available to setAccessPolicy with");
				}
			} catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			} catch (BaseException e) {
				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
			
		}
	}	
	

//
//	@Test
//	public void testSetAccessPolicy() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSetAccessPolicy(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.setAccessPolicy();
//				checkTrue(currentUrl,"setAccessPolicy(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
