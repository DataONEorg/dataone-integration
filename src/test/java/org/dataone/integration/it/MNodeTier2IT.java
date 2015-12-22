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

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v1.MNode;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.testImplementations.AbstractAuthITDataoneIsAuthzd;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.SystemMetadata;
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
public class MNodeTier2IT extends AbstractAuthITDataoneIsAuthzd
implements AuthAPITestDefinitions
{

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
	protected CommonCallAdapter(String baseUrl) 
	{ 
		return new CommonCallAdapter(MULTIPART_REST_CLIENT,node,"v2");
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
		    Node node = it.next();
			currentUrl = node.getBaseURL();
			//TODO: parameterize "v2"
			CommonCallAdapter mn = new CommonCallAdapter(MULTIPART_REST_CLIENT,node,"v2");
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
				
			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));

				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				checkTrue(mn.getLatestRequestUrl(),"isAuthorized response should never be false. [Only true or exception].", success);
			} 
    		catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
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
	@Test
	public void testSystemMetadataChanged() {
		Iterator<Node> it = getMemberNodeIterator();
		
		setupClientSubject("testRightsHolder");
		
		String[] cNodeIds = new String[]{"cnDevUNM1","cnSandboxORC1", "cnStageUNM1"};
		
		while ( it.hasNext() ) {
				
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);
		
			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
				
				SystemMetadata smd = mn.getSystemMetadata(null, pid);
				if (new Date().getTime() - smd.getDateUploaded().getTime() > 5000) {
					// probably synced by now, assuming no changes until
					// after sync are happening.
					Date afterCreate = new Date();

					int success = 0;
					int invReq = 0;
					int notAuth = 0;
					int other = 0;
					for (String cNodeId : cNodeIds) {
						setupClientSubject(cNodeId);
						try {
							mn.systemMetadataChanged(null, pid, 10, afterCreate);
							success++;
						} catch (InvalidRequest e) {
							invReq++;
						} catch (NotAuthorized e) {
							notAuth++;
						} catch (Exception e) {
							other++;
							handleFail(mn.getLatestRequestUrl(), "unexpected exception: " +
									"systemMetadataChanged should only throw InvalidRequest" +
									"or NotAuthorized exceptions if the service is not failing.  Got: " +
									e.getClass() + "; called with client subject " + cNodeId + ": " + e.getMessage());
						}
					}
					checkTrue(mn.getLatestRequestUrl(),
								"the test should return at least one success or InvalidRequest",
								success + invReq > 0);
					checkTrue(mn.getLatestRequestUrl(),
							"the test should only return return success or InvalidRequest for one CN (environment)",
							success + invReq == 1);
					log.info("success = " + success);
					log.info("InvalidRequest = " + invReq);
					log.info("NotAuthorized = " + notAuth);
					log.info("other = " + other);

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
	}
	
	
//	@Test
	public void testSystemMetadataChanged_EarlierDate() {
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("cnDevUNM1");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);
		
			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));


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
	}
	
	/**
	 * This test tries to have a non-CN subject call the method.  should fail.
	 */
	@Test
	public void testSystemMetadataChanged_authenticatedITKuser() {
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testPerson");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged_authITKuser() vs. node: " + currentUrl);
		
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3",true);
		
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

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
	}


	
//	@Ignore("do not know how to test in stand-alone mode.")
//	@Test
	public void testSystemMetadataChanged_withCreate() {
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("cnDevUNM1");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);
		
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestDelete",true);
		
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

				Date afterCreate = new Date();
				mn.systemMetadataChanged(null, pid, 10, afterCreate);
			}	
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected InvalidToken, got: " +
						e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
						": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}


    @Override
    protected CommonCallAdapter instantiateD1Node(String baseUrl) {
        return new CommonCallAdapter(MULTIPART_REST_CLIENT, "v2");
    }
	
	/**
	 * the bulk of tests are in the AbstractAuthorizationITDataone base class
	 */
	
}
