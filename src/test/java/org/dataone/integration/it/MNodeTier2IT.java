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
 * <li> DC=dataone,DC=org,CN=testRightsHolder </li>
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
				
				Identifier pid = procurePublicReadableTestObject(mn);
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
	
	/**
	 * the bulk of tests are in the AbstractAuthorizationITDataone base class
	 */
	
}
