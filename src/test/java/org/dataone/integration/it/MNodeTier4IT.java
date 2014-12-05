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

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.v1.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

public class MNodeTier4IT extends ContextAwareTestCaseDataone {

	private  String currentUrl;
	private String idPrefix = "testMNodeTier4:";

	
	@Test
	public void testTrue() {
		assertTrue(true);
	}
	


//	@Ignore("test not implemented")
//	@Test
	public void testReplicate() {
		
	}
	

	/**
	 *  Test MN.Replicate() functionality
	 */
	@Test
	public void testReplicate_NoCertificate() {

		setupClientSubject_NoCert();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

			NodeReference sourceNode = new NodeReference();
			sourceNode.setValue("bad");
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);				
				mn.replicate(null, (SystemMetadata) dataPackage[2], sourceNode);	
				handleFail(mn.getLatestRequestUrl(),"should not be able to initiate replication without a certificate");
			}
			catch (NotAuthorized na) {
				// expected behavior
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
						e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	/**
	 *  Test MN.Replicate() functionality
	 */
	@Test
	public void testReplicate_ValidCertificate_NotCN() {

		setupClientSubject("testPerson");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testReplicate_ValidCertificate_NotCN vs. node: " + currentUrl);

			NodeReference sourceNode = new NodeReference();
			sourceNode.setValue("bad");
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);				
				mn.replicate(null, (SystemMetadata) dataPackage[2], sourceNode);	
				handleFail(mn.getLatestRequestUrl(),"should not be able to initiate replication a certificate representing a CN");
			}
			catch (NotAuthorized na) {
				// expected behavior
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
						e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	/**
	 *  Test MN.Replicate() functionality
	 */
	@Ignore("need to create testCN certificate to run this subtest")
	@Test
	public void testReplicate_FaultyNodeReference() {

		setupClientSubject("testCN");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

			NodeReference sourceNode = new NodeReference();
			sourceNode.setValue("bad");
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);
				mn.replicate(null, (SystemMetadata) dataPackage[2], sourceNode);	
				handleFail(mn.getLatestRequestUrl(),"replicate call should not succeed with faulty node reference");
			}
			catch (InvalidRequest na) {
				// expected behavior ??
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected InvalidRequest, got: " +
						e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";

	}

}
