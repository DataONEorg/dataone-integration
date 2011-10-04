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
import java.security.cert.X509Certificate;
import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
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

    private  String format_text_csv = "text/csv";
    private  String format_eml_200 = "eml://ecoinformatics.org/eml-2.0.0";
    private  String format_eml_201 = "eml://ecoinformatics.org/eml-2.0.1";
    private  String format_eml_210 = "eml://ecoinformatics.org/eml-2.1.0";
    private  String format_eml_211 = "eml://ecoinformatics.org/eml-2.1.1";

    private static final String idPrefix = "mnTier2:";
    private static final String bogusId = "foobarbaz214";

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
		setupClientSubject_Reader();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
		
			try {
				// should be a valid Identifier
				Identifier pid = mn.listObjects().getObjectInfo(0).getIdentifier();
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				checkTrue(currentUrl,"isAuthorized response should never be false. [Only true or exception].", success);
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
	 * without a client certificate.  As with testIsAuthorized, checking for Read 
	 * permission on the first object returned from the Tier1 listObjects() method.  
	 * Expecting the InvalidRequest exception to be returned.
	 */
	@Test
	public void testIsAuthorized_noCert() 
	{
		setupClientSubject_NoCert();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testIsAuthorized_noCert() vs. node: " + currentUrl);
		
			try {
				// should be a valid Identifier
				Identifier pid = mn.listObjects().getObjectInfo(0).getIdentifier();
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				handleFail(currentUrl,"isAuthorized response response should through exception if no session/token");
			} 
			catch (BaseException e) {
				checkTrue(currentUrl,e.getDescription(),e instanceof InvalidRequest);
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
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
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			setupClientSubject_Writer();
			
			try {
				// create the identifier
				Identifier guid = new Identifier();
				guid.setValue("mNodeTier2TestSetAccessPolicy." + ExampleUtilities.generateIdentifier());

				// get some data bytes as an input stream
				ByteArrayInputStream textPlainSource = 
					new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));

				// build the system metadata object
				SystemMetadata sysMeta = 
					ExampleUtilities.generateSystemMetadata(guid, "text/plain", textPlainSource, null);

				// make the submitter the same as the cert DN 
				try {
					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
					String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
					sysMeta.getRightsHolder().setValue(ownerX500);
				} catch (Exception e) {
					// warn about this?
					e.printStackTrace();
				}
			      
				// create a test object
				Identifier pid = mn.create(null, guid, textPlainSource, sysMeta);
				checkEquals(currentUrl,"0. returned pid from create should match what was given",
						guid.getValue(), pid.getValue());

				
				
				// try unsuccessfully to read it as the testReader
				setupClientSubject_Reader();
				try {
					mn.get(null, pid);
					handleFail(currentUrl,"1. getting the newly created object as reader should fail");
				} catch (NotFound nf) {
					// this is what we want
				}
				try {
					mn.isAuthorized(null, pid,Permission.READ);
					handleFail(currentUrl,"2. isAuthorized by the reader should fail by default");
				} catch (NotFound nf) {
					// this is what we want
				}
				
				
				
				// open up read permission to the testReader
				String readerSubject = CertificateManager.getInstance()
					.getSubjectDN(CertificateManager.getInstance().loadCertificate());
				
				setupClientSubject_Writer();
				boolean success = mn.setAccessPolicy(null, pid, 
						AccessUtil.createSingleRuleAccessPolicy(new String[] {readerSubject},
								new Permission[] {Permission.READ}));

				checkTrue(currentUrl,"3. writer should be able to set the access policy",success);

				// now try to read as the testReader
				setupClientSubject_Reader();
				try {
					mn.isAuthorized(null, pid, Permission.READ);
				} catch (NotAuthorized na) {
					handleFail(currentUrl,"4. testReader should be authorized to read this pid '" 
							+ pid.getValue() + "'");
				}
				
				try {
					mn.get(null, pid);
				} catch (NotFound nf) {
					handleFail(currentUrl,"5. testReader should now be able to get the object");
				}
				
				// now try to read as a known user with no rights to the object
				setupClientSubject_NoRights();
				try {
					mn.get(null, pid);
					handleFail(currentUrl,"6. testNoRights should not be able to get the object");
				} catch (NotFound nf) {
					// this is what we want
				}
				
				try {
					mn.isAuthorized(null, pid, Permission.READ);
					handleFail(currentUrl,"7. testNoRights should not be able to get the object");
				} catch (NotAuthorized na) {
					// this is what we want
				}
				
				
				// finally test permissions with certificateless client
				setupClientSubject_NoCert();
				try {
					mn.get(null, pid);
					handleFail(currentUrl,"8. anonymous client (no certificate) should not be" +
							"able to get the object");
				} catch (NotFound nf) {
					// this is what we want
				}
				
				try {
					mn.isAuthorized(null, pid, Permission.READ);
					handleFail(currentUrl,"9. anonymous client (no certificate) should not be " +
							"able to get successful response from isAuthorized()");
				} catch (NotAuthorized na) {
					// this is what we want
				}

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
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			setupClientSubject_Writer();
			
			try {
				// create the identifier
				Identifier guid = new Identifier();
				guid.setValue("mNodeTier2TestSetAccessPolicy." + ExampleUtilities.generateIdentifier());

				// get some data bytes as an input stream
				ByteArrayInputStream textPlainSource = 
					new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));

				// build the system metadata object
				SystemMetadata sysMeta = 
					ExampleUtilities.generateSystemMetadata(guid, "text/plain", textPlainSource, null);

				// make the submitter the same as the cert DN 
				try {
					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
					String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
					sysMeta.getRightsHolder().setValue(ownerX500);
				} catch (Exception e) {
					// warn about this?
					e.printStackTrace();
				}
			      
				// create a test object
				Identifier pid = mn.create(null, guid, textPlainSource, sysMeta);
				checkEquals(currentUrl,"pid returned from create should match that given",
						guid.getValue(), pid.getValue());

				// set access on the object
				boolean success = mn.setAccessPolicy(null, pid, buildPublicReadAccessPolicy());
				checkTrue(currentUrl,"setAccessPolicy should return true",success);


			} catch (BaseException e) {
				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
}
