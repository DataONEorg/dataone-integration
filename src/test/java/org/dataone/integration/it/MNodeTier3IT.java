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
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

public class MNodeTier3IT extends ContextAwareTestCaseDataone {

	private  String currentUrl;
	private String idPrefix = "testMNodeTier3:";

	/**
	 *  Test MNStorage.create() functionality
	 */
	@Test
	public void testCreate() {

		setupClientSubject("testWriter");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestCreate",true);				
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
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestCreate",true);
				
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);			
				handleFail(currentUrl,"should not be able to create an object if no certificate");
			}
			catch (InvalidToken na) {
				// expected behavior
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Expected InvalidToken, got: " +
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
//	@Ignore("ignoring to save time for local testing");
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
		setupClientSubject("testWriter");
		Iterator<Node> it = getMemberNodeIterator();
		printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");

		// get identifiers to check with
		Vector<String> unicodeString = new Vector<String>();
		Vector<String> escapedString = new Vector<String>();
		//   TODO: test against Unicode characters when metacat supports unicode    	
		InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
		//InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
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


					Object[] dataPackage = generateTestDataPackage(idStringEscaped,false);

					// rGuid is either going to be the escaped ID or the non-escaped ID
					Identifier rGuid = null;

					rGuid = mn.create(null, (Identifier) dataPackage[0], 
							(InputStream)dataPackage[1], (SystemMetadata)dataPackage[2]);
					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
//					mn.setAccessPolicy(null, rGuid, buildPublicReadAccessPolicy());
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

		setupClientSubject("testWriter");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

				// prep for checking update time. 
				Thread.sleep(100);
				Date now = new Date();
				Thread.sleep(100);
				
				// create the new data package to update with. 
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				Identifier newPid = (Identifier) dataPackage[0];
				
				// do the update
				Identifier updatedPid = mn.update(null,
						originalPid,
						(InputStream) dataPackage[1],    // new data
						newPid,
						(SystemMetadata) dataPackage[2]  // new sysmeta
						);
		
				checkEquals(currentUrl,"pid returned from update should match that given",
						newPid.getValue(), updatedPid.getValue());

				// check obsoletes and obsoletedBy fields
				 SystemMetadata updatedSysmeta = mn.getSystemMetadata(null, updatedPid);
				 checkEquals(currentUrl,"sysmeta of updatePid should have the originalPid in obsoletes field",
						 updatedSysmeta.getObsoletes().getValue(),originalPid.getValue());
				 
				 checkTrue(currentUrl, "MN should be setting the dateSystemMetadataModified property",
						 updatedSysmeta.getDateSysMetadataModified() != null);
				 
				 SystemMetadata oldSysmeta = mn.getSystemMetadata(null, originalPid);
				 checkEquals(currentUrl,"sysmeta of original Pid should have new pid in obsoletedBy field",
						 oldSysmeta.getObsoletedBy().getValue(),updatedPid.getValue());
				 
				 // the old pid needs to be in a timebound listObject search
				 ObjectList ol = mn.listObjects(null, now, null, null, null, null, null);
				 boolean foundUpdatedSysmeta = false;
				 for (ObjectInfo oi : ol.getObjectInfoList()) {
					 if (oi.getIdentifier().getValue().equals(originalPid.getValue())) {
						 foundUpdatedSysmeta = true;
					 }
				 }
				 checkTrue(currentUrl,"should find original pid in time-bound listObject " +
				 		"where start time is after original create and before update",foundUpdatedSysmeta);
				 
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
	public void testUpdate_badObsoletedByInfo() {

		setupClientSubject("testWriter");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// create the new data package to update with. 
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				Identifier newPid = (Identifier) dataPackage[0];
				
				//  incorrectly set the obsoletedBy property instead of obsoletes
				SystemMetadata smd = (SystemMetadata) dataPackage[2];
				smd.setObsoletedBy(originalPid);
				// do the update
				Identifier updatedPid = mn.update(null,
						originalPid,
						(InputStream) dataPackage[1],    // new data
						newPid, smd);
				smd = mn.getSystemMetadata(null, updatedPid);
				if (smd.getObsoletedBy() != null) {
					handleFail(currentUrl,"should not be able to update with obsoletedBy " +
						"field set (for pid = " + updatedPid.getValue() + ")");		
				}
			}
			catch (InvalidSystemMetadata e) {
				// expected outcome
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
	public void testUpdate_badObsoletesInfo() {

		setupClientSubject("testWriter");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// create the new data package to update with. 
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				Identifier newPid = (Identifier) dataPackage[0];
				
				//  incorrectly set the obsoletedBy property instead of obsoletes
				SystemMetadata smd = (SystemMetadata) dataPackage[2];
				Identifier phonyId = new Identifier();
				phonyId.setValue("phonyId");
				smd.setObsoletes(phonyId);
				// do the update
				Identifier updatedPid = mn.update(null,
						originalPid,
						(InputStream) dataPackage[1],    // new data
						newPid, smd);
				handleFail(currentUrl,"should not be able to update with faulty " +
					"obsoletes information (for pid = " + updatedPid.getValue() + ")");		
			}
			catch (InvalidSystemMetadata e) {
				// expected outcome
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
			setupClientSubject("testWriter");
			
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate_NoCert() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				SystemMetadata sysmeta = (SystemMetadata) dataPackage[2];
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], sysmeta);
				
				// create the new data package to update with
				dataPackage = generateTestDataPackage("mNodeTier3TestUpdate",true);
				
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
				try {
					Identifier updatedPid = mn.update(null,
						pid,                          // old pid
						(InputStream) dataPackage[1], // new data
						(Identifier) dataPackage[0],  // new pid
						sysmeta                       // modified sysmeta
						);
		
					handleFail(currentUrl,"should not be able to create an object if no certificate");
				} 
				catch (InvalidToken na) {
						// expected behavior
				}
				catch (BaseException e) {
					handleFail(currentUrl,"Expected InvalidToken, got: " +
						e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
						": " + e.getDescription());
				}
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Exception while setting up test (mn.create): " +
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
			setupClientSubject("testWriter");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete() vs. node: " + currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestDelete",true);
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// try the delete
				Identifier deletedPid = mn.delete(null, pid);

				checkEquals(currentUrl,"pid returned from delete should match that given",
						((Identifier)dataPackage[0]).getValue(), deletedPid.getValue());
				
				SystemMetadata smd = mn.getSystemMetadata(null, pid);
				checkTrue(currentUrl, "sysmeta for deleted object should be has status of archived",smd.getArchived());
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
	 *  Test MNStorage.delete() functionality
	 */
	@Test
	public void testDelete_NotFound() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testWriter");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete() vs. node: " + currentUrl);

			try {
				// try the delete
				Identifier fakePid = new Identifier();
				fakePid.setValue("fakeID." + ExampleUtilities.generateIdentifier());
				Identifier deletedPid = mn.delete(null, fakePid);

				handleFail(currentUrl,"member node should return NotFound if pid" +
						"to be deleted does not exist there.");
			}
			catch (NotFound e) {
				// expected outcome
			}
			catch (BaseException e) {
				handleFail(currentUrl,"Expected NotFound, got: " + e.getClass().getSimpleName() +
						": " + e.getDetail_code() + ": " + e.getDescription());
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
			setupClientSubject("testWriter");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete_NoCert() vs. node: " + currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestDelete",true);
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				setupClientSubject_NoCert();
				// try the delete
				Identifier deletedPid = mn.delete(null, pid);
				handleFail(currentUrl,"should not be able to delete an object if no certificate");
			}
			catch (InvalidToken na) {
				try {
					setupClientSubject("testWriter");
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
	
	@Ignore("cannot run test until we create a testCN certificate is generated")
	@Test
	public void testSystemMetadataChanged() {
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testWriter");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);
		
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3TestDelete",true);
		
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

				Date afterCreate = new Date();
				mn.systemMetadataChanged(null, pid, 10, afterCreate);
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
	

	@Test
	public void testSystemMetadataChanged_authenticatedITKuser() {
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testWriter");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSystemMetadataChanged_authITKuser() vs. node: " + currentUrl);
		
			try {
				Object[] dataPackage = generateTestDataPackage("mNodeTier3",true);
		
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

				Date afterCreate = new Date();
				mn.systemMetadataChanged(null, pid, 10, afterCreate);
			}
			catch (NotAuthorized e) {
				// expected response
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
	
	
	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";

	}

}
