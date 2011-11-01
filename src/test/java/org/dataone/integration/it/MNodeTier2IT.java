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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.junit.Test;

/**
 * Test the dataONE Tier2 Member Node service APIs. (MNAuthorization).
 * Tier2 methods (and higher) require authorized clients, so each method is 
 * tested at least for both success and failure due to lack of client credentials.
 * <p>
 * Member nodes under test are assumed to accept 3 certificates signed by the 
 * dataONE certificate authority ("d1CA").  They are:
 * <ol>
 * <li> CN=testUserWriter,DC=dataone,DC=org </li>
 * <li> CN=testUserReader,DC=dataone,DC=org </li>
 * <li> CN=testUserNoRights,DC=dataone,DC=org </li>
 * </ol>
 * Testing for proper behavior in the context of anonymous clients is 
 * accomplished by pointing the client's CertificateManager to a bogus 
 * location for loading the client certificate.  Therefore, some error/log output
 * will be generated warning that "/bogus/certificate/location" was not found.
 *  
 * @author Rob Nahf
 */
public class MNodeTier2IT extends ContextAwareTestCaseDataone  {

    private static String currentUrl;
        

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 2 API (Authorization) methods";
	}


	/**
	 * Tests the dataONE service API isAuthorized() method, checking for Read 
	 * permission on the first object returned from the Tier1 listObjects() method.  
	 * Anything other than the boolean true is considered a test failure.
	 */
	@Test
	public void testIsAuthorized() 
	{
		setupClientSubject_Writer();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {	
				Identifier pid = procureTestObject(mn, new Permission[]{Permission.READ});
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				checkTrue(currentUrl,"isAuthorized response should never be false. [Only true or exception].", success);
			} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	/**
	 * Tests the dataONE service API isAuthorized method, but trying the call 
	 * without a client certificate.  Checking for Write permission on the first
	 * object returned from the Tier1 listObjects() method.  
	 * Expect the NotAuthorized exception to be returned, since anonymous clients
	 * "most certainly" can't submit content to a member node.
	 */
	@Test
	public void testIsAuthorizedforWrite_noCert() 
	{
		setupClientSubject_NoCert();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testIsAuthorized_noCert() vs. node: " + currentUrl);
			
			try {
				Identifier pid = procureTestObject(mn, new Permission[] {Permission.READ});
				boolean success = mn.isAuthorized(null, pid, Permission.WRITE);
				handleFail(currentUrl,"isAuthorized response should throw exception if no session/token");
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
			catch (BaseException e) {
				checkTrue(currentUrl,e.getClass().getSimpleName() + ": " + e.getDescription(),e instanceof NotAuthorized);
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	

	/**
	 * search the objectlist for an object whose AccessPolicy can be changed.
	 * try to create one if none found, and return null if not able.
	 */
//	private Identifier getOrCreateChangeableObject(MNode mn) 
//	{
//		setupClientSubject_Writer();
//		Identifier id = null;
//		try {
//			ObjectList ol = mn.listObjects();
//			for(int i=0; i<= ol.sizeObjectInfoList(); i++) {
//				id = ol.getObjectInfo(i).getIdentifier();
//				try {
//					mn.isAuthorized(null,id, Permission.CHANGE_PERMISSION);
//					break;
//				} catch (NotAuthorized na) {}							
//				id = null;
//			}
//			
//			if (id == null) {
//				//try creating one
//				// create the identifier
//				Identifier pid = new Identifier();
//				pid.setValue("mNodeTier2TestSetAccessPolicy." + ExampleUtilities.generateIdentifier());
//
//				// get some data bytes as an input stream
//				ByteArrayInputStream textPlainSource = 
//					new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));
//
//				// build the system metadata object
//				SystemMetadata sysMeta = 
//					ExampleUtilities.generateSystemMetadata(pid, "text/plain", textPlainSource, null);
//
//				// make the submitter the same as the cert DN 
//				try {
//					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
//					String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
//					sysMeta.getRightsHolder().setValue(ownerX500);
//				} catch (Exception e) {
//					// warn about this?
//					e.printStackTrace();
//				}
//			      
//				log.info("create a test object");
//				Identifier retPid = mn.create(null, pid, textPlainSource, sysMeta);
//				checkEquals(currentUrl,"0. returned pid from create should match what was given",
//						pid.getValue(), retPid.getValue());
//				id = retPid;
//			}
//		} 
//		catch (BaseException e) {
//			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
//		}
//		catch(Exception e) {
//			log.warn(e.getClass().getName() + ": " + e.getMessage());
//		}
//		if (id == null) {
//			log.info(" ====>>>>> pid with changePermission for restWriter: null");
//		} else {
//			log.info(" ====>>>>> pid with changePermission for restWriter: " + id.getValue());
//		}
//		return id;
//	}
	
	

    /**
	 * Tests the dataONE service API setAccessPolicy method, first calling the
	 * Tier 3 create method, to setup an object whose access policy can be 
	 * manipulated for the test. 
	 * on the first object returned from the Tier1 listObjects() method.  
	 * Anything other than the boolean true is considered a test failure.
	 */
    @Test
	public void testSetAccessPolicy() 
    {	
    	setupClientSubject_Reader();
    	String readerSubject = CertificateManager.getInstance()
    		.getSubjectDN(CertificateManager.getInstance().loadCertificate());
	
    	
    	Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			try {
				setupClientSubject_Writer();
				Identifier changeableObject = procureTestObject(mn, 
					new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION}); //getOrCreateChangeableObject(mn);
				if (changeableObject != null)
				{					
					log.info("clear the AccessPolicy");
					boolean success = mn.setAccessPolicy(null, changeableObject, new AccessPolicy());

					// ensure blank policy with get, isAuthorized
					setupClientSubject_Reader();				
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"1. isAuthorized by the reader should fail");
					} catch (NotAuthorized na) {
						// should fail
					}
					try {
						mn.get(null, changeableObject);
						handleFail(currentUrl,"2. getting the newly created object as reader should fail");
					} catch (NotAuthorized na) {
						// this is what we want
					}


					log.info("allow read permission for testReader");
					setupClientSubject_Writer();
					success = mn.setAccessPolicy(null, changeableObject, 
							AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject},
									new Permission[] {Permission.READ}));
					checkTrue(currentUrl,"3. testWriter should be able to set the access policy",success);


					// test for success
					log.info("trying isAuthorized as the testReader");
					setupClientSubject_Reader();			
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"4. testReader should be authorized to read this pid '" 
								+ changeableObject.getValue() + "'");
					}

					log.info("now trying get as the testReader");
					try {
						mn.get(null, changeableObject);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"5. testReader should now be able to get the object");
					}

					log.info("now try to get as a known user with no rights to the object (should not be able)");
					setupClientSubject_NoRights();
					try {
						mn.get(null, changeableObject);
						handleFail(currentUrl,"6. testNoRights should not be able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}
					log.info("now try isAuthorized() on it as a known user with no rights to the object");
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"7. testNoRights should not be able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}

					log.info("finally test access against anonymous client");
					setupClientSubject_NoCert();
					try {
						mn.get(null, changeableObject);
						handleFail(currentUrl,"8. anonymous client (no certificate) should not be" +
						"able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}

					log.info("and test isAuthorized on it with certificateless client");
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"9. anonymous client (no certificate) should not be " +
						"able to get successful response from isAuthorized()");
					} catch (NotAuthorized na) {
						// this is what we want
					}
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
    
    /**
	 * Tests the dataONE service API setAccessPolicy method, calling it with
	 * no subject/certificate, after first calling the Tier 3 create method, 
	 * to setup an object whose access policy can be manipulated for the test. 
	 * <p>
	 * Anything other than the boolean true is considered a test failure.
	 */
    @Test
	public void testSetAccessPolicy_NoCert() 
    {	
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testSetAccessPolicy_NoCert() vs. node: " + currentUrl);

			setupClientSubject_Writer();
			Identifier changeableObject = procureTestObject(mn, 
					new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION}); //getOrCreateChangeableObject(mn);
			if (changeableObject != null) {
				try {	
					setupClientSubject_NoCert();
					log.info("  subject cleared");
					// set access on the object
					boolean success = mn.setAccessPolicy(null, changeableObject, 
							AccessUtil.createSingleRuleAccessPolicy(new String[]{"foo"},
									new Permission[]{Permission.READ}));
					handleFail(currentUrl,"with no client certificate, setAccessPolicy should throw exception");


				} catch (BaseException e) {

					checkEquals(currentUrl, "with no client certificate: " + e.getDetail_code() + ": " + e.getDescription(), 
							e.getClass().getSimpleName(), "NotAuthorized");
				} catch (Exception e) {
					e.printStackTrace();
					handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
				}
			} else {
				handleFail(currentUrl,"No object available to setAccessPolicy with");
			}
		}
	}
    
    
    @Test
	public void testSetAccess_Public() throws SecurityException, NoSuchMethodException 
    {	
    	runSetAccessForSymbolicPrincipal(org.dataone.service.util.Constants.SUBJECT_PUBLIC,
    			this.getClass().getMethod("setupClientSubject_NoCert"));
    }
    
    @Test
	public void testSetAccess_AuthenticatedUser() throws SecurityException, NoSuchMethodException 
    {	
    	runSetAccessForSymbolicPrincipal(org.dataone.service.util.Constants.SUBJECT_AUTHENTICATED_USER,
    			ContextAwareTestCaseDataone.class.getMethod("setupClientSubject_Reader"));
    }
    
    @Test
	public void testSetAccess_VerifiedUser() throws SecurityException, NoSuchMethodException 
    {	
    	runSetAccessForSymbolicPrincipal(org.dataone.service.util.Constants.SUBJECT_VERIFIED_USER,
    			this.getClass().getMethod("setupClientSubject_Reader"));
    }
    
    
    /** 
     * Implementation for setAccess Tests for various types of users
     * @param subject - the subject that 
     * @param clientSetupMethod
     */
	private void runSetAccessForSymbolicPrincipal(String subject, Method clientSetupMethod)
	{
    Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testSetAccess_" + subject + "() vs. node: " + currentUrl);

			try {
				setupClientSubject_Writer();
				Identifier changeableObject = procureTestObject(mn, 
						new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION}); //getOrCreateChangeableObject(mn);
				if (changeableObject != null)
				{
					log.info("clear the AccessPolicy");
					mn.setAccessPolicy(null, changeableObject, new AccessPolicy());

					// ensure subject under test isn't authorized with get, isAuthorized
					// prior to setting up the symbolic principal in accessPolicy
					clientSetupMethod.invoke(null, null);// setupClientSubject_Reader();				
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"1. isAuthorized by " + subject + " should fail");
					} catch (NotAuthorized na) {
						// should fail
					}
					try {
						mn.get(null, changeableObject);
						handleFail(currentUrl,"2. getting the object as " + subject + " should fail");
					} catch (NotAuthorized na) {
						// this is what we want
					}

					try {
						setupClientSubject_Writer();
						mn.setAccessPolicy(null, changeableObject, 
								AccessUtil.createSingleRuleAccessPolicy(
										new String[]{subject},
										new Permission[]{Permission.READ}));
					}
					catch (BaseException e) {
						handleFail(currentUrl, "3. testWriter should be able to set the access policy, " +
								"but got: " + e.getClass().getSimpleName() + ": " + e.getDescription());
					}

					// test for success
					log.info("trying isAuthorized as " + clientSetupMethod.getName() + " (as " + subject + ")");
					clientSetupMethod.invoke(null,null);  //ClientSubject_NoCert();			
					try {
						mn.isAuthorized(null, changeableObject, Permission.READ);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"4. " + subject + " should be authorized to read this pid '" 
								+ changeableObject.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
								+ na.getDetail_code() + ": " + na.getDescription());
					}

					log.info("trying get as " + clientSetupMethod.getName() + " (as " + subject + ")");
					try {
						mn.get(null, changeableObject);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"5. " + subject + " should now be able to get the object");
					}
				} else {
					handleFail(currentUrl,"No object available to setAccessPolicy with");
				}
				
			} catch (BaseException e) {
				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
			
		}
	}
}
