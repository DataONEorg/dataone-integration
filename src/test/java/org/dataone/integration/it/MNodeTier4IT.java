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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.D1Object;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

public class MNodeTier4IT extends ContextAwareTestCaseDataone {

	private static final String format_text_plain = "text/plain";

	private  String currentUrl;
	private String idPrefix = "testMNodeTier4:";

	
	@Test
	public void testTrue() {
		assertTrue(true);
	}
	
	/**
	 *  Test MNReplication.getReplica() functionality.  This tests the normal usage
	 *  where the caller is a MemberNode. Other callers should fail.
	 */
	@Ignore("cannot test until testMN certificate is created")
	@Test
	public void testGetReplica() {

		setupClientSubject_Writer();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica() vs. node: " + currentUrl);

			try {
				Identifier pid = procureTestObject(mn, new Permission[] {Permission.READ});			
				InputStream is = mn.getReplica(null, pid);
				checkTrue(currentUrl,"get() returns an objectStream", is != null);
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
		}
	}
	
	
	/**
	 *  Test MNReplication.getReplica() functionality.  This tests for expected 
	 *  exception when a non-MemberNode client calls the method.
	 */
	@Test
	public void testGetReplica_AuthenticatedITKUser() {

		setupClientSubject_Writer();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica_AuthenticateITKUser() vs. node: " + currentUrl);

			try {
				Identifier pid = procureTestObject(mn, new Permission[] {Permission.READ});			
				InputStream is = mn.getReplica(null, pid);
				checkTrue(currentUrl,"get() returns an objectStream", is != null);
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
			catch (InvalidToken e) {
				// expected behavior
			}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
		}
	}
	
	
	/**
	 *  Test MNReplication.getReplica() functionality.  Normal usage is the caller
	 *  being another MemberNode.  Others should fail.  This tests the latter case.   
	 */
	@Test
	public void testGetReplica_NoCertificate() {

		setupClientSubject_NoCert();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica_NoCert() vs. node: " + currentUrl);

			try {
				Identifier pid = procureTestObject(mn, new Permission[] {Permission.READ});			
				InputStream is = mn.getReplica(null, pid);
				handleFail(currentUrl,"with no client certificate, getReplica() should throw exception");
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
			catch (InvalidToken e) {
				// expected behavior
			}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
		}
	}

	@Ignore("test not implemented")
	@Test
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

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier4");				
				mn.replicate(null, (SystemMetadata) dataPackage[2], null);	
				handleFail(currentUrl,"should not be able to initiate replication without a certificate");
			}
			catch (NotAuthorized na) {
				// expected behavior
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Expected NotAuthorized, got: " +
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

//		setupClientSubject_CN();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier4");				
				mn.replicate(null, (SystemMetadata) dataPackage[2], null);	
				handleFail(currentUrl,"replicate call should not succeed with faulty node reference");
			}
			catch (InvalidRequest na) {
				// expected behavior ??
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Expected InvalidRequest, got: " +
						e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	
	/*
	 * creates the identifier, data inputstream, and sysmetadata for testing purposes
	 * the rightsHolder is set to the subject of the current certificate (user)
	 */
	private Object[] generateTestDataPackage(String idPrefix) 
	throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException
	{
		String identifierStr = ExampleUtilities.generateIdentifier();
		
		Identifier guid = new Identifier();
		guid.setValue(idPrefix + "." + identifierStr);

		// get some data bytes as an input stream
		byte[] contentBytes = "Plain text source".getBytes("UTF-8");

		// figure out who we are
		String ownerX500 = null;
		try {
			X509Certificate certificate = CertificateManager.getInstance().loadCertificate();			
			if (certificate != null) {
				ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
//				sysMeta.getRightsHolder().setValue(ownerX500);
//				sysMeta.getSubmitter().setValue(ownerX500);
			}
		} catch (Exception e) {
			ownerX500 = "MNodeTier3ITunknownCert";
		}
			
		D1Object d1o = new D1Object(guid, contentBytes, format_text_plain, ownerX500, "authNode");
		SystemMetadata sysMeta = d1o.getSystemMetadata();
		
		// match the submitter as the cert DN 
		
		sysMeta.setAccessPolicy(AccessUtil.createSingleRuleAccessPolicy(
				new String[] {Constants.SUBJECT_PUBLIC},
				new Permission[] {Permission.READ}));
		
		ByteArrayInputStream bis = new ByteArrayInputStream(d1o.getData());
		return new Object[]{guid,bis,sysMeta};
	}
	
	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";

	}

}