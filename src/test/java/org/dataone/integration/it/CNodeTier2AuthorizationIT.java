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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.dataone.service.util.EncodingUtilities;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier2AuthorizationIT extends ContextAwareTestCaseDataone {

	

	private static final String badIdentifier = "ThisIdentifierShouldNotExist";
//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";
//	private static HashMap<String,String> StandardTests = new HashMap<String,String>();
	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	private static Vector<String> encodedPIDStrings = new Vector<String>();
	
	 private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;
  
	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 * @throws ServiceFailure 
	 */
	@Before
	public void setup() throws ServiceFailure {
//		prefetchObjects();
//		generateStandardTests();
	}


	public void prefetchObjects() throws ServiceFailure {
		if (listedObjects == null) {
			listedObjects = new Hashtable<String,ObjectList>();
			Iterator<Node> it = getCoordinatingNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				CNode cn = D1Client.getCN();
				try {
					ObjectList ol = cn.search(null, QUERYTYPE_SOLR, ""); 
					listedObjects.put(currentUrl, ol);
				} 
				catch (BaseException e) {
					handleFail(currentUrl,e.getDescription());
				}
				catch(Exception e) {
					log.warn(e.getClass().getName() + ": " + e.getMessage());
				}	
			}
		}
	}
	
	
	private ObjectInfo getPrefetchedObject(String currentUrl, Integer index)
	{
		if (index == null) 
			index = new Integer(0);
		if (index < 0) {
			// return off the right end of the list
			index = listedObjects.get(currentUrl).getCount() + index;
		}
		return listedObjects.get(currentUrl).getObjectInfo(index);
	}
	
	
	public void generateStandardTests() {

		if (testPIDEncodingStrings.size() == 0) {
			System.out.println(" * * * * * * * Unicode Test Strings * * * * * * ");
			
			InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
			Scanner s = new Scanner(is,"UTF-8");
			String[] temp;
			int c = 0;
			try
			{
				while (s.hasNextLine()) 
				{
					String line = s.nextLine();
					if (line.startsWith("common-") || line.startsWith("path-"))
					{
						System.out.println(c++ + "   " + line);
						temp = line.split("\t");
						if (temp.length > 1)
							testPIDEncodingStrings.add(temp[0]);
							encodedPIDStrings.add(temp[1]);
					}
				}	
				System.out.println("");
			} finally {
				s.close();
			}
			System.out.println("");
		}
	}
	
	@Test
	public void testSetRightsHolder() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);

			try {
//				Identifier myObject = procureTestObject(cn,new Permission[] {Permission.CHANGE_PERMISSION});
				ObjectList oll = cn.search(null,"solr","rightsHolder=");
				if (oll.getTotal() == 0) {
					handleFail(currentUrl,"do not have an object that am rightsHolder to use to test setRightsHolder");
				} else {
					// TODO: create a real request
					Identifier pidToGiveAway = oll.getObjectInfo(0).getIdentifier();
					SystemMetadata smd = cn.getSystemMetadata(null, pidToGiveAway);
					BigInteger serialVersion = smd.getSerialVersion();
				 
					Subject inheritor = new Subject();
					inheritor.setValue("testReaderSubject");
					Identifier response = cn.setRightsHolder(null, 
							oll.getObjectInfo(0).getIdentifier(),
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {	
				Identifier pid = procureTestObject(mn, null, new Permission[]{Permission.READ});
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				checkTrue(currentUrl,"isAuthorized response should never be false. [Only true or exception].", success);
			} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized_noCert() vs. node: " + currentUrl);
			
			try {
				Identifier pid = procureTestObject(mn, null, new Permission[] {Permission.READ});
				boolean success = mn.isAuthorized(null, pid, Permission.WRITE);
				handleFail(currentUrl,"isAuthorized response should throw exception if no session/token");
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
			catch (BaseException e) {
				checkTrue(currentUrl,e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription(),e instanceof NotAuthorized);
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	@Test
	public void testIsAuthorized_PublicSymbolicPrincipal() throws SecurityException, NoSuchMethodException 
	{	
		runIsAuthorizedVsSubject(Constants.SUBJECT_PUBLIC,
				Permission.READ, this.getClass().getMethod("setupClientSubject_NoCert"));
	}

	@Test
	public void testIsAuthorized_AuthenticatedUserSymbolicPrincipal() throws SecurityException, NoSuchMethodException 
	{	
		runIsAuthorizedVsSubject(Constants.SUBJECT_AUTHENTICATED_USER,
				Permission.READ, this.getClass().getMethod("setupClientSubject_Reader"));
	}

	@Ignore("user verification not implemented yet")
	@Test
	public void testIsAuthorized_VerifiedUserSymbolicPrincipal() throws SecurityException, NoSuchMethodException 
	{	
		runIsAuthorizedVsSubject(Constants.SUBJECT_VERIFIED_USER,
				Permission.READ, this.getClass().getMethod("setupClientSubject_Reader"));
	}

	/** 
	 * Implementation for isAuthorized Tests for various types of users
	 * @param subject - the subject
	 * @param clientSetupMethod
	 */
	protected void runIsAuthorizedVsSubject(String policySubjectString, Permission
			permission, Method clientSetupMethod)
	{
		Subject policySubject = null;
		if (policySubjectString != null) {
			policySubject = new Subject();
			policySubject.setValue(policySubjectString);
		}

		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized_" + policySubject.getValue() + "() vs. node: " + currentUrl);

			try {				
				// become the desired user/client-subject
				clientSetupMethod.invoke(null, null);
				// get an appropriate test object
				Identifier pid = procureTestObject(mn, policySubject, new Permission[] {permission});

				// test for success
				log.info("1. trying isAuthorized({READ}) as '" + clientSetupMethod.getName() + "' vs. policy subject '" + policySubject.getValue() + "'");		
				try {
					mn.isAuthorized(null, pid, Permission.READ);
				} catch (NotAuthorized na) {
					handleFail(currentUrl,"1. " + policySubject.getValue() + " should be authorized to read this pid '" 
							+ pid.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
							+ na.getDetail_code() + ": " + na.getDescription());
				}

				log.info("2. trying get() as '" + clientSetupMethod.getName() + "' vs. policy subject '" + policySubject.getValue() + "'");
				try {
					mn.get(null, pid);
				} 
				catch (NotAuthorized na) {
					handleFail(currentUrl,"2. " + policySubject.getValue() + " should now be able to get the object - got NotAuthorized instead");
				}
				catch (NotFound na) {
					handleFail(currentUrl,"2. " + policySubject.getValue() + " should now be able to get the object - got NotFound instead");
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
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			try {
				setupClientSubject_Writer();
				Identifier changeableObject = procureTestObject(cn, null,
					new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION});
				if (changeableObject != null)
				{					
					log.info("clear the AccessPolicy");
					SystemMetadata smd = cn.getSystemMetadata(null,changeableObject);
					long serialVersion = smd.getSerialVersion().longValue();
					boolean success = cn.setAccessPolicy(null, changeableObject, 
							new AccessPolicy(), serialVersion);

					// ensure blank policy with get, isAuthorized
					setupClientSubject_Reader();				
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


					log.info("allow read permission for testReader");
					setupClientSubject_Writer();
					smd = cn.getSystemMetadata(null, changeableObject);
					serialVersion = smd.getSerialVersion().longValue();
					success = cn.setAccessPolicy(null, changeableObject, 
							AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject},
									new Permission[] {Permission.READ}), serialVersion);
					checkTrue(currentUrl,"3. testWriter should be able to set the access policy",success);


					// test for success
					log.info("trying isAuthorized as the testReader");
					setupClientSubject_Reader();			
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"4. testReader should be authorized to read this pid '" 
								+ changeableObject.getValue() + "'");
					}

					log.info("now trying get as the testReader");
					try {
						cn.get(null, changeableObject);
					} catch (NotAuthorized na) {
						handleFail(currentUrl,"5. testReader should now be able to get the object");
					}

					log.info("now try to get as a known user with no rights to the object (should not be able)");
					setupClientSubject_NoRights();
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

					log.info("finally test access against anonymous client");
					setupClientSubject_NoCert();
					try {
						cn.get(null, changeableObject);
						handleFail(currentUrl,"8. anonymous client (no certificate) should not be" +
						"able to get the object");
					} catch (NotAuthorized na) {
						// this is what we want
					}

					log.info("and test isAuthorized on it with certificateless client");
					try {
						cn.isAuthorized(null, changeableObject, Permission.READ);
						handleFail(currentUrl,"9. anonymous client (no certificate) should not be " +
						"able to get successful response from isAuthorized()");
					} catch (NotAuthorized na) {
						// this is what we want
					}
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
				setupClientSubject_Writer();
				Identifier changeableObject = procureTestObject(cn, null, 
						new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION}); //getOrCreateChangeableObject(mn);
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

	
//  /** 
//  * Implementation for setAccess Tests for various types of users
//  * @param subject - the subject that 
//  * @param clientSetupMethod
//  */
//	private void runSetAccessForSymbolicPrincipal(String subject, Method clientSetupMethod)
//	{
// Iterator<Node> it = getMemberNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			printTestHeader("testSetAccess_" + subject + "() vs. node: " + currentUrl);
//
//			try {
//				setupClientSubject_Writer();
//				Identifier changeableObject = procureTestObject(mn, 
//						new Permission[] {Permission.READ, Permission.WRITE, Permission.CHANGE_PERMISSION}); //getOrCreateChangeableObject(mn);
//				if (changeableObject != null)
//				{
//					log.info("clear the AccessPolicy");
//					mn.setAccessPolicy(null, changeableObject, new AccessPolicy());
//
//					// ensure subject under test isn't authorized with get, isAuthorized
//					// prior to setting up the symbolic principal in accessPolicy
//					clientSetupMethod.invoke(null, null);// setupClientSubject_Reader();				
//					try {
//						mn.isAuthorized(null, changeableObject, Permission.READ);
//						handleFail(currentUrl,"1. isAuthorized by " + subject + " should fail");
//					} catch (NotAuthorized na) {
//						// should fail
//					}
//					try {
//						mn.get(null, changeableObject);
//						handleFail(currentUrl,"2. getting the object as " + subject + " should fail");
//					} catch (NotAuthorized na) {
//						// this is what we want
//					}
//
//					try {
//						setupClientSubject_Writer();
//						mn.setAccessPolicy(null, changeableObject, 
//								AccessUtil.createSingleRuleAccessPolicy(
//										new String[]{subject},
//										new Permission[]{Permission.READ}));
//					}
//					catch (BaseException e) {
//						handleFail(currentUrl, "3. testWriter should be able to set the access policy, " +
//								"but got: " + e.getClass().getSimpleName() + ": " + e.getDescription());
//					}
//
//					// test for success
//					log.info("trying isAuthorized as " + clientSetupMethod.getName() + " (as " + subject + ")");
//					clientSetupMethod.invoke(null,null);  //ClientSubject_NoCert();			
//					try {
//						mn.isAuthorized(null, changeableObject, Permission.READ);
//					} catch (NotAuthorized na) {
//						handleFail(currentUrl,"4. " + subject + " should be authorized to read this pid '" 
//								+ changeableObject.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
//								+ na.getDetail_code() + ": " + na.getDescription());
//					}
//
//					log.info("trying get as " + clientSetupMethod.getName() + " (as " + subject + ")");
//					try {
//						mn.get(null, changeableObject);
//					} catch (NotAuthorized na) {
//						handleFail(currentUrl,"5. " + subject + " should now be able to get the object");
//					}
//				} else {
//					handleFail(currentUrl,"No object available to setAccessPolicy with");
//				}
//			} catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			} catch (BaseException e) {
//				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + e.getDescription());
//			} catch (Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
//			}
//			
//		}
//	}	
	
	
	
	// ---------------------------------------------------------------------------------------
//	@Test
//	public void testIsAuthorized() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testIsAuthorized(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.isAuthorized();
//				checkTrue(currentUrl,"isAuthorized(...) returns a boolean object", response != null);
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
//
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
