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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
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
import org.junit.Test;

public class MNodeTier3IT extends ContextAwareTestCaseDataone {

	private static final String format_text_plain = "text/plain";

	private  String currentUrl;
	private String idPrefix = "testMNodeTier3:";

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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestCreate");				
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);	
				
				checkEquals(currentUrl,"pid of created object should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				InputStream theDataObject = mn.get(null,pid);
				String objectData = IOUtils.toString(theDataObject);
				checkTrue(currentUrl,"should get back an object containing submitted text:" + objectData,
						objectData.contains("Plain text source"));
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " 
						+ e.getDetail_code() + ": " + e.getDescription());
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testCreate_NoCert() vs. node: " + currentUrl);

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
				handleFail(currentUrl,"Expected NotAuthorized, got: " +
						e.getClass().getSimpleName() + ": " 
						+ e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}


    /**
     * test creation of data with challenging unicode identifier.
     */
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
		setupClientSubject_Writer();
		Iterator<Node> it = getMemberNodeIterator();
		printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");

		// get identifiers to check with
		Vector<String> unicodeString = new Vector<String>();
		Vector<String> escapedString = new Vector<String>();
		//   TODO: test against Unicode characters when metacat supports unicode    	
		//    	InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
		InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
		Scanner s = new Scanner(is,"UTF-8");
		String[] temp;
		int c = 0;
		try{
			while (s.hasNextLine()) {
				String line = s.nextLine();
				if (line.startsWith("common-") || line.startsWith("path-"))
				{
					System.out.println(c++ + "   " + line);
					temp = line.split("\t");
					unicodeString.add(temp[0]);
					escapedString.add(temp[1]);		
				}
			}
		} finally {
			s.close();
		}
		
		
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testCreateData_IdentifierEncoding() vs. node: " + currentUrl);
			

			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeString.size(); j++) 
			{
				String status = "OK   ";
				try {
					//    			String unicode = unicodeString.get(j);
					System.out.println();
					System.out.println(j + "    unicode String:: " + unicodeString.get(j));
					String idString = idPrefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
					String idStringEscaped = idPrefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);


					Object[] dataPackage = generateTestDataPackage(idPrefix);

					// rGuid is either going to be the escaped ID or the non-escaped ID
					Identifier rGuid = null;

					rGuid = mn.create(null, (Identifier) dataPackage[0], 
							(InputStream)dataPackage[1], (SystemMetadata)dataPackage[2]);
					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
					mn.setAccessPolicy(null, rGuid, buildPublicReadAccessPolicy());
					checkEquals(currentUrl,"guid returned from create should equal that given",
							((Identifier)dataPackage[0]).getValue(), rGuid.getValue());
					InputStream data = mn.get(null, rGuid);
					checkTrue(currentUrl, "get against the object should not equal null", null != data);
					String str = IOUtils.toString(data);
					checkTrue(currentUrl,"should be able to read the content as created ('" + str + "')",
							str.indexOf("Plain text source") != -1);
					data.close();
				}
				catch (BaseException e) {
					status = "Error";
					handleFail(currentUrl,e.getClass().getSimpleName() +
							": " + e.getDetail_code() + ": " + e.getDescription());
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}	
				nodeSummary.add("Test " + j + ": " + status + ":  " + unicodeString.get(j));
			}
			System.out.println();
			for (int k=0; k<nodeSummary.size(); k++) 
			{
				log.info(nodeSummary.get(k));
			}
			System.out.println();
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
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
				handleFail(currentUrl,e.getClass().getSimpleName() + 
						": " + e.getDetail_code() + ": " + e.getDescription());
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate_NoCert() vs. node: " + currentUrl);

			
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
				handleFail(currentUrl,"Expected NotAuthorized, got: " +
						e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
						": " + e.getDescription());
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete() vs. node: " + currentUrl);

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
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
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
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete_NoCert() vs. node: " + currentUrl);

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
			catch (InvalidToken na) {
				try {
					setupClientSubject_Writer();
					InputStream is = mn.get(null, pid);	
				}
				catch (BaseException e) {
					handleFail(currentUrl,"Got InvalidToken, but couldn't perform subsequent get(). Instead: " +
							e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
							": " + e.getDescription());
				}
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Expected InvalidToken, got: " +
						e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
						": " + e.getDescription());
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
			} else {
				ownerX500 = "MNodeTeir3ITunknownCert";
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
