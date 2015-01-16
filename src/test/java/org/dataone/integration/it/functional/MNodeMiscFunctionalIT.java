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
 * 
 * $Id$
 */

package org.dataone.integration.it.functional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v1.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Functional tests for MN api calls that involve interactions with CNs
 * @author rnahf
 *
 */
public class MNodeMiscFunctionalIT extends ContextAwareTestCaseDataone {

	String currentUrl;
	
	Map<MNode,Identifier> createdObjectMap;
	List<MNode> syncedMNs;
	
	int maxWaitMinutes = 5;
	
	/*
	 * creates test objects on each MN, and waits for them to appear on the CN
	 */
//	@Before
	public void createTestObjects() throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented
	{
		if (createdObjectMap == null) {
			setupClientSubject_NoCert();
			Iterator<Node> it = getMemberNodeIterator();

			createdObjectMap = new HashMap<MNode,Identifier>();

			int mnCount = 0;
			
			CNode cn = D1Client.getCN();
			
			// create or find the test objects on the MNs
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				MNode mn = D1Client.getMN(currentUrl);

				try {
					Identifier pid = procureTestObject(mn,
							APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ),
							APITestUtils.buildIdentifier(
									"FunctionalTest:smdChange:" + cn.lookupNodeId(currentUrl) )
							);
					createdObjectMap.put(mn,pid);
					
					log.info(String.format("creating test object '%s' on MN '%s' ",pid.getValue(),currentUrl));
					mnCount++;
				}
				catch (BaseException e) {
					e.printStackTrace();
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
				catch (TestIterationEndingException e) {
					e.printStackTrace();
				}
			}
			
			// make sure all test objects are known to the cn.
			// wait until all are accounted for
			Date startWait = new Date();
			int syncedCount = 0;
			syncedMNs = new ArrayList<MNode>();
			while (syncedCount < mnCount) {
				for (MNode mn : createdObjectMap.keySet()) {
					if (!syncedMNs.contains(mn)) {
						try {
							cn.get(null, createdObjectMap.get(mn));
//							cn.describe(null, createdObjectMap.get(mn));
							syncedMNs.add(mn);
							syncedCount++;
						} 
						catch (NotFound e) {
							; // not synced yet
						} 
					}
				}
				
				Date now = new Date();
				if (startWait.getTime() + 10 * 60 * 1000 < now.getTime()) {
					// exceeded reasonable time for sync
					// proceed with what we've got
					break;
				}
			}
			
		}
	}
	
	/**
	 * reserve an ID, then try to create the object.  Should succeed.
	 * This test checks that an MN checks with the CN before creating object.
	 */
	@Ignore("not tested")
	@Test
	public void testCreateUsingReserveId() {
		setupClientSubject("testSubmitter");
    	
		// let's do this for each CN in the environment...
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testCreateUsingReserveId using node: " + currentUrl);

			Iterator<Node> mit = getMemberNodeIterator();

			while (mit.hasNext()) {
				MNode mn = new MNode(it.next().getBaseURL());

				boolean isReserved = false;
				try {
					Identifier pid = new Identifier();
					pid.setValue("MNodeMiscFunc:" + ExampleUtilities.generateIdentifier());

					Identifier response = cn.reserveIdentifier(null,pid);
					checkTrue(cn.getLatestRequestUrl(),"reserveIdentifier(...) should return the given identifier",
							response.equals(pid));
					isReserved = true;

					// ensures that the reservation was made
					try {
						response = cn.reserveIdentifier(null,pid);
					}
					catch (IdentifierNotUnique e) {
						if (isReserved) {
							// then got the desired outcome
						} else {
							handleFail(cn.getLatestRequestUrl(),e.getDescription());
							continue;
						}
					}


					// successful reservation, so now try the create
					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
					String submitterX500 = CertificateManager.getInstance().getSubjectDN(certificate);

					byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
					InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
					D1Object d1o = new D1Object(pid, contentBytes,
							D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
							D1TypeBuilder.buildSubject(submitterX500),
							D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));

					SystemMetadata sysMeta = d1o.getSystemMetadata();
					Identifier rPid = mn.create(pid, objectInputStream, sysMeta);

					checkEquals(mn.getLatestRequestUrl(),"pid of created object should equal that given",
							rPid.getValue(), pid.getValue());

					InputStream theDataObject = mn.get(null,pid);
					String objectData = IOUtils.toString(theDataObject);
					checkTrue(mn.getLatestRequestUrl(),"should get back an object containing submitted text:" + objectData.substring(0, 1000),
							objectData.contains("IPCC Data Distribution Centre Results "));
				
				
				}
				// handle fails per CN-MN combination
				catch (BaseException e) {
					handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ":: " + e.getDescription());
				}
				catch(Exception e) {
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}
			}
    	}
	}
	
	
	
	/**
	 * reserve an ID using one subject, then try to create with another. Should fail.
	 */
	@Ignore("not tested")
	@Test
	public void testCreateUsingReserveId_NotReserver() {

    	
		// let's do this for each CN in the environment...
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testCreateUsingReserveId using node: " + currentUrl);

			Iterator<Node> mit = getMemberNodeIterator();

			while (mit.hasNext()) {
				MNode mn = new MNode(it.next().getBaseURL());

				boolean isReserved = false;
				try {
					setupClientSubject("testSubmitter");
					
					Identifier pid = new Identifier();
					pid.setValue("MNodeMiscFunc:" + ExampleUtilities.generateIdentifier());

					Identifier response = cn.reserveIdentifier(null,pid);
					checkTrue(cn.getLatestRequestUrl(),"reserveIdentifier(...) should return the given identifier",
							response.equals(pid));
					isReserved = true;

					// ensures that the reservation was made
					try {
						response = cn.reserveIdentifier(null,pid);
					}
					catch (IdentifierNotUnique e) {
						if (isReserved) {
							// then got the desired outcome
						} else {
							handleFail(cn.getLatestRequestUrl(),e.getDescription());
							continue;
						}
					}


					// successful reservation, so now try the create
					setupClientSubject("testRightsHolder");
					
					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
					String submitterX500 = CertificateManager.getInstance().getSubjectDN(certificate);

					byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
					InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
					D1Object d1o = new D1Object(pid, contentBytes,
							D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
							D1TypeBuilder.buildSubject(submitterX500),
							D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));

					SystemMetadata sysMeta = d1o.getSystemMetadata();
					Identifier rPid = mn.create(pid, objectInputStream, sysMeta);

					handleFail(mn.getLatestRequestUrl(),"testRightsHolder subject should not be able to " +
							"create the object using pid " + pid.getValue() + " when the identifier was " +
									"reserved by the testSubmitter subject");

				}
				catch (NotAuthorized e) {
					// expected outcome
					;
				}
				// handle fails per CN-MN combination
				catch (BaseException e) {
					handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ":: " + e.getDescription());
				}
				catch(Exception e) {
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}
			}
    	}
	}
	
	/**
	 * Nodes of all Tiers should do this, but stand-alone tests won't work
	 * for Tier2+ nodes, which also need to check with CN via isNodeAuthorized().
	 * Can it be tested without initiating replication? via systemMetadata change?
	 */
	@Ignore("not tested")
	@Test
	public void testGetReplica_CallLoggedAsReplicate() {
		
	}

	/**
	 * if content is restricted, mn.getReplica() needs to check that the caller 
	 * (another MN) is supposed to be making this request, using cn.isNodeAuthorized().
	 * This test does not initiate a replication cycle, so it will test that
	 * the appropriate exception is returned.  cn should throw NotAuthorized back 
	 * to the mn, and the mn should throw NotAuthorized back to the client.
	 * 
	 *   Tier 1 nodes, (all public content) will not test the same.  They will
	 *   not fail.
	 */
	@Ignore("not tested")
//	@Test
	public void testGetReplica_isNodeAuthorized() {
		setupClientSubject("testMN"); // an unregistered MN

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica() vs. node: " + currentUrl);

			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
				InputStream is = mn.getReplica(null, pid);
				checkTrue(mn.getLatestRequestUrl(),"get() returns an objectStream", is != null);
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
		}
	}
	
	
	
	
	@Override
	protected String getTestDescription() {
		return "Tests for the appropriate update in systemMetadata of the Authoritative membernode";
	}
}
