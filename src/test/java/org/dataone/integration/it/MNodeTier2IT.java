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

import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Test the dataONE Tier2 Member Node service APIs. (MNAuthorization).
 * Tier2 methods (and higher) require authorized clients, so each method is 
 * tested at least for both success and failure due to lack of client credentials.
 * <p>
 * Member nodes under test are assumed to accept the following certificates signed 
 * by the dataONE certificate authority ("d1CA").  They are:
 * <ol>
 * <li> DC=dataone,DC=org,CN=testOwner </li>
 * <li> DC=dataone,DC=org,CN=testWriter </li>
 * <li> DC=dataone,DC=org,CN=testReader </li>
 * <li> DC=dataone,DC=org,CN=testNoRights </li>
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
		setupClientSubject("testReader");
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {

				Identifier pid = procureTestObject(mn, Permission.READ, true);  // should be getting an object via listObjects
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
				Identifier pid = procureTestObject(mn, null, Permission.READ, true);
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
	
	/**
	 * Finds or creates an object with PUBLIC READ access, then ensures
	 * that an anonymous client can READ it.
	 */
    @Test
	public void testIsAuthorized_PublicSymbolicPrincipal() 
    {	
    	runIsAuthorizedVsSubject(Constants.SUBJECT_PUBLIC,
    			Permission.READ, "NoCert", "NoCert");
    }
    
    /**
     * Finds or creates an object where AUTHENTICATED_USER has READ access, then
     * ensures that the "testNoRights" client can READ it.  (By design, the testNoRights
     * subject is not on accessPolicies, and is not part of any group, or mapped
     * to any other identity.)
     */
    @Test
	public void testIsAuthorized_AuthenticatedUserSymbolicPrincipal()
    {	
    	runIsAuthorizedVsSubject(Constants.SUBJECT_AUTHENTICATED_USER,
    			Permission.READ, "testOwner", "testNoRights");
    }
    
    
    @Ignore("user verification not implemented yet")
    @Test
	public void testIsAuthorized_VerifiedUserSymbolicPrincipal()
    {	
		runIsAuthorizedVsSubject(Constants.SUBJECT_VERIFIED_USER,
    			Permission.READ, "testOwner", "testReader");
    }
    
    /**
     * assuming that testMappedReader and testReaderGroup are not in the
     * accessPolicy of the object procured.
     */
    @Test
	public void testIsAuthorized_MappedIdentity()
    {	
    	runIsAuthorizedVsSubject("testReader", Permission.READ,
    			"testOwner", "testMappedReader");
    }
    
    /**
     * assumes that testReader and testMappedReader are not in the accessPolicy
     * of the object procured
     */
    @Test
	public void testIsAuthorized_ViaGroupMembership()
    {	
    	runIsAuthorizedVsSubject("testReaderGroup", Permission.READ,
    			"testOwner", "testReader");
    	//TODO: limit procureTestObject to those with singleAccessRule APs
    }
    
    /** 
     * Implementation for isAuthorized Tests for various types of users
     * @param policySubjectName - the subject in the systemmMetadata.accessPolicy that 
     *           needs to have the permission specified in the permission parameter
     * @param permission - the permission needed for the specified policySubjectString
     * @param testingSubjectName - the client subject to run the test under
     */
	protected void runIsAuthorizedVsSubject(String policySubjectName, Permission
			permission, String procuringSubjectName, String testingSubjectName)
	{
		Subject policySubject = null;
		if (policySubjectName != null) {
			policySubject = new Subject();
			policySubject.setValue(policySubjectName);
		}
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized_" + policySubject.getValue() + "() vs. node: " + currentUrl);

			try {				
				// change to the subject used for procuring an object
				if (procuringSubjectName.equals("NoCert") || procuringSubjectName == null)
					setupClientSubject_NoCert();
				else 
					setupClientSubject(procuringSubjectName);

				// get an appropriate test object
				Identifier pid = procureTestObject(mn, policySubject, permission, false);
					
				// change to the subject used for testing, if necessary
				if (!testingSubjectName.equals(procuringSubjectName)) {
					if (testingSubjectName.equals("NoCert") || testingSubjectName == null)
						setupClientSubject_NoCert();
					else 
						setupClientSubject(testingSubjectName);
				}
				
				// test for success
				log.info("1. trying isAuthorized({READ}) as '" + testingSubjectName + "' vs. policy subject '" + policySubject.getValue() + "'");		
				try {
					mn.isAuthorized(null, pid, Permission.READ);
				} catch (NotAuthorized na) {
					handleFail(currentUrl,"1. " + policySubject.getValue() + " should be authorized to read this pid '" 
							+ pid.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
							+ na.getDetail_code() + ": " + na.getDescription());
				}

				log.info("2. trying get() as '" + testingSubjectName + "' vs. policy subject '" + policySubject.getValue() + "'");
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

}
