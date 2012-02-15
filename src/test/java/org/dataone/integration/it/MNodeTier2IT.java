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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
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
public class MNodeTier2IT extends AbstractAuthorizationITDataone  {

    private static String currentUrl;
        

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 2 API (Authorization) methods";
	}

	
	@Override
	protected Iterator<Node> getNodeIterator() 
	{
		return getMemberNodeIterator();
	}
	
	@Override
	protected MNode instantiateD1Node(String baseUrl) 
	{ 
		return new MNode(baseUrl);
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
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {
				
				Identifier pid = procurePublicReadableTestObject(mn); //, Permission.READ, true);
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
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
	
	
//	/**
//	 * Tests the dataONE service API isAuthorized method, but trying the call 
//	 * without a client certificate.  Checking for Write permission on the first
//	 * object returned from the Tier1 listObjects() method.  
//	 * Expect the NotAuthorized exception to be returned, since anonymous clients
//	 * "most certainly" can't submit content to a member node.
//	 */
//	@Test
//	public void testIsAuthorizedforWrite_noCert() 
//	{
//		setupClientSubject_NoCert();
//		
//		Iterator<Node> it = getMemberNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testIsAuthorized_noCert() vs. node: " + currentUrl);
//			
//			try {
//				Identifier pid = procureTestObject(mn, null, Permission.READ, true);
//				boolean success = mn.isAuthorized(null, pid, Permission.WRITE);
//				handleFail(currentUrl,"isAuthorized response should throw exception if no session/token");
//			}
//			catch (IndexOutOfBoundsException e) {
//    			handleFail(currentUrl,"No Objects available to test against");
//    		}
//			catch (BaseException e) {
//				checkTrue(currentUrl,e.getClass().getSimpleName() + ": " + 
//						e.getDetail_code() + ": " + e.getDescription(),e instanceof NotAuthorized);
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}	
//		}
//	}
	
//	/**
//	 * Finds or creates an object with PUBLIC READ access, then ensures
//	 * that an anonymous client can READ it.
//	 */
//    @Test
//	public void testIsAuthorized_PublicSymbolicPrincipal() 
//    {	
//    	AccessRule accessRule = AccessUtil.createAccessRule(
//    			new String[]{Constants.SUBJECT_PUBLIC}, new Permission[]{Permission.READ});
//    	runIsAuthorizedVsSubject( accessRule, "NoCert", "NoCert", null);
//    }
    
//    /**
//     * Finds or creates an object where AUTHENTICATED_USER has READ access, then
//     * ensures that the "testNoRights" client can READ it.  (By design, the testNoRights
//     * subject is not on accessPolicies, and is not part of any group, or mapped
//     * to any other identity.)
//     */
//    @Test
//	public void testIsAuthorized_AuthenticatedUserSymbolicPrincipal()
//    {	
//    	AccessRule accessRule = AccessUtil.createAccessRule(
//    			new String[]{Constants.SUBJECT_AUTHENTICATED_USER}, new Permission[]{Permission.READ});
//    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testNoRights",null);
//    }
//    
//    
//    @Ignore("user verification not implemented yet")
//    @Test
//	public void testIsAuthorized_VerifiedUserSymbolicPrincipal()
//    {	
//    	AccessRule accessRule = AccessUtil.createAccessRule(
//    			new String[]{Constants.SUBJECT_VERIFIED_USER}, new Permission[]{Permission.READ});
//    	runIsAuthorizedVsSubject(accessRule, "testOwner", "testReader", null);
//    }
//    
//    /**
//     * assuming that testMappedReader and testReaderGroup are not in the
//     * accessPolicy of the object procured.
//     */
//    @Test
//	public void testIsAuthorized_MappedIdentity()
//    {	
//    	AccessRule accessRule = AccessUtil.createAccessRule(
//    			new String[]{"testReader"}, new Permission[]{Permission.READ});
//    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testMappedReader", null);
//    }
//    
//    /**
//     * assumes that testReader and testMappedReader are not in the accessPolicy
//     * of the object procured
//     */
//    @Test
//	public void testIsAuthorized_ViaGroupMembership()
//    {	
//    	AccessRule accessRule = AccessUtil.createAccessRule(
//    			new String[]{"testReaderGroup"}, new Permission[]{Permission.READ});
//    	Identifier pid = new Identifier();
//    	pid.setValue("testAuthByGroup");
//    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testReader",pid);
//    }



    
//    /** 
//     * Implementation for isAuthorized Tests for various types of users
//     * @param accessRule - the accessRule containing the subject and permission
//     *           required in the systemmMetadata.accessPolicy 
//     * @param procuringSubjectName - the client subject used to search for the
//     *           objects with (listObjects, getSystemMetadata)
//     * @param testingSubjectName - the client subject to run the test under
//	 * @param pid - pass in a valid pid to bypass the procurement of the identifier
//	 *              and test isAuthorized against the provided pid.
//     */
//	protected void runIsAuthorizedVsSubject(AccessRule accessRule, 
//			String procuringSubjectName, String testingSubjectName, Identifier pid)
//	{
//		String policySubjectName = accessRule.getSubject(0).getValue();
//		
//		Iterator<Node> it = getMemberNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testIsAuthorized_" + policySubjectName + "() vs. node: " + currentUrl);
//
//			try {				
//				// change to the subject used for procuring an object
//				if (procuringSubjectName.equals("NoCert") || procuringSubjectName == null)
//					setupClientSubject_NoCert();
//				else 
//					setupClientSubject(procuringSubjectName);
//
//				// get an appropriate test object
//				 pid = procureTestObject(mn, accessRule, pid);
//					
//				// change to the subject used for testing, if necessary
//				if (!testingSubjectName.equals(procuringSubjectName)) {
//					if (testingSubjectName.equals("NoCert") || testingSubjectName == null)
//						setupClientSubject_NoCert();
//					else 
//						setupClientSubject(testingSubjectName);
//				}
//				
//				// test for success
//				log.info("1. trying isAuthorized({READ}) as '" + testingSubjectName + "' vs. policy subject '" + policySubjectName + "'");		
//				try {
//					mn.isAuthorized(null, pid, Permission.READ);
//				} catch (NotAuthorized na) {
//					handleFail(currentUrl,"1. " + policySubjectName + " should be authorized to read this pid '" 
//							+ pid.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
//							+ na.getDetail_code() + ": " + na.getDescription());
//				}
//
//				log.info("2. trying get() as '" + testingSubjectName + "' vs. policy subject '" + policySubjectName + "'");
//				try {
//					mn.get(null, pid);
//				} 
//				catch (NotAuthorized na) {
//					handleFail(currentUrl,"2. " + policySubjectName + " should now be able to get the object - got NotAuthorized instead");
//				}
//				catch (NotFound na) {
//					handleFail(currentUrl,"2. " + policySubjectName + " should now be able to get the object - got NotFound instead");
//				}
//	
//			} catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			} catch (BaseException e) {
//				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + 
//						e.getDetail_code() + ": " + e.getDescription());
//			} catch (Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
//			}
//			
//		}
//	}   

}
