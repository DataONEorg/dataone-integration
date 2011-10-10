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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.After;
import org.junit.Test;

public class MNodeTier3IT extends ContextAwareTestCaseDataone {

	private static final String format_text_plain = "text/plain";

	private  String currentUrl;

	//  @Before
	//  public void setUp() throws Exception {
	//  }

	@After
	public void tearDown() throws Exception {
	}

	/**
	 *  Test MNStorage.create() functionality
	 */
	@Test
	public void testCreate() {

		setupClientSubject_Writer();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestCreate");				
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);	
				
				checkEquals(currentUrl,"pid of created object should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
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
	 *  Test MNStorage.create() functionality
	 */
	@Test
	public void testCreate_NoCert() {

		setupClientSubject_NoCert();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestCreate");
				
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);			
				handleFail(currentUrl,"should not be able to create an object if no certificate");
			}
			catch (NotAuthorized na) {
				// expected behavior
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
	 *  Test MNStorage.update() functionality
	 */
	@Test
	public void testUpdate() {

		setupClientSubject_Writer();

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate");
				SystemMetadata sysmeta = (SystemMetadata) dataPackage[2];
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], sysmeta);
				
				// create the new data package to update with
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate");
				
				// TODO: reinstated the checks when obsolete behavior refactored.
				// update the obsoletesList
				//	      newSysMeta.addObsolete(pid);

				// update the derivedFrom list
				//	      newSysMeta.addDerivedFrom(pid);
				
				// set the new pid on the sysmeta object
				// TODO: should the MN do this?
				sysmeta.setIdentifier((Identifier)dataPackage[0]);  
				
				
				// do the update
				Identifier updatedPid = mn.update(null,
						pid,                          // old pid
						(InputStream) dataPackage[1], // new data
						(Identifier) dataPackage[0],  // new pid
						sysmeta                       // modified sysmeta
						);
		
				checkEquals(currentUrl,"pid returned from update should match that given",
						((Identifier)dataPackage[0]).getValue(), updatedPid.getValue());

				// get the updated system metadata
				// SystemMetadata updatedSysMeta = mn.getSystemMetadata(null, updatedPid);
				//	      assertTrue(updatedSysMeta.getObsolete(0).getValue().equals(pid));
				//	      assertTrue(updatedSysMeta.getDerivedFrom(0).getValue().equals(pid));	      

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

	@Test
	public void testUpdate_NoCert() {

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject_Writer();
			
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate");
				SystemMetadata sysmeta = (SystemMetadata) dataPackage[2];
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], sysmeta);
				
				// create the new data package to update with
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate");
				
				// TODO: reinstated the checks when obsolete behavior refactored.
				// update the obsoletesList
				//	      newSysMeta.addObsolete(pid);

				// update the derivedFrom list
				//	      newSysMeta.addDerivedFrom(pid);
				
				// set the new pid on the sysmeta object
				// TODO: should the MN do this?
				sysmeta.setIdentifier((Identifier)dataPackage[0]);  
				
				
				setupClientSubject_NoCert();
				// do the update
				Identifier updatedPid = mn.update(null,
						pid,                          // old pid
						(InputStream) dataPackage[1], // new data
						(Identifier) dataPackage[0],  // new pid
						sysmeta                       // modified sysmeta
						);
		
				handleFail(currentUrl,"should not be able to create an object if no certificate");
			}
			catch (NotAuthorized na) {
				// expected behavior
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
	 *  Test MNStorage.delete() functionality
	 */
	@Test
	public void testDelete() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject_Writer();

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestDelete");
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// try the delete
				Identifier deletedPid = mn.delete(null, pid);

				checkEquals(currentUrl,"pid returned from delete should match that given",
						((Identifier)dataPackage[0]).getValue(), deletedPid.getValue());
				
				InputStream is = mn.get(null, pid);
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
	
	@Test
	public void testDelete_NoCert() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject_Writer();

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestDelete");
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				setupClientSubject_NoCert();
				// try the delete
				Identifier deletedPid = mn.delete(null, pid);
				handleFail(currentUrl,"should not be able to delete an object if no certificate");
			}
			catch (NotAuthorized na) {
				try {
					InputStream is = mn.get(null, pid);	
				}
				catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDescription());
				}
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


	/*
	 * creates the identifier, data inputstream, and sysmetadata for testing purposes
	 * the rightsHolder is set to the subject of the current certificate (user)
	 */
	private Object[] generateTestDataPackage(String idPrefix) 
	throws UnsupportedEncodingException
	{
		String identifierStr = ExampleUtilities.generateIdentifier();
		
		Identifier guid = new Identifier();
		guid.setValue(idPrefix + "." + identifierStr);

		// get some data bytes as an input stream
		InputStream textPlainSource = 
			new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));

		// build the system metadata object
		SystemMetadata sysMeta = 
			ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource,null);

		// match the submitter as the cert DN 
		try {
			X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
			String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
			sysMeta.getRightsHolder().setValue(ownerX500);
		} catch (Exception e) {
			// warn about this
			e.printStackTrace();
		}

		return new Object[]{guid,textPlainSource,sysMeta};
	}
	
	
	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";

	}

}
