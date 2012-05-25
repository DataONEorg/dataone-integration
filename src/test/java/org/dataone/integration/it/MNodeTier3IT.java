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

import java.io.IOException;
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

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestCreate", true);
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);	
				
				
				checkEquals(mn.getLatestRequestUrl(),"pid of created object should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				InputStream theDataObject = mn.get(null,pid);
				String objectData = IOUtils.toString(theDataObject);
				checkTrue(mn.getLatestRequestUrl(),"should get back an object containing submitted text:" + objectData.substring(0, 1000),
						objectData.contains("IPCC Data Distribution Centre Results "));
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
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestCreate",true);
				
				mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);			
				handleFail(mn.getLatestRequestUrl(),"should not be able to create an object if no certificate");
			}
			catch (InvalidToken na) {
				// expected behavior
			}
			catch (NotAuthorized na) {
				// expected behavior
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected InvalidToken or NotAuthorized, got: " +
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
	@Ignore("ignoring to save time for local testing")
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
		setupClientSubject("testRightsHolder");
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
					if (line.contains("supplementary"))
						continue;
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
				String testLoc = "      ";
				try {
					//    			String unicode = unicodeString.get(j);
					log.info("");
					log.info(j + "    unicode String:: " + unicodeString.get(j));
					String idString = idPrefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
					String idStringEscaped = idPrefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);

					testLoc = "generate";
					Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(idString,false);

					checkEquals(mn.getLatestRequestUrl(), "ExampleUtilities.generateTestSciDataPackage() should produce" +
							"identifier same as the one provided", ((Identifier) dataPackage[0]).getValue(),
							idString);

					testLoc = "create";
					// rGuid is either going to be the escaped ID or the non-escaped ID
					Identifier rPid = mn.create(null, (Identifier) dataPackage[0], 
							(InputStream)dataPackage[1], (SystemMetadata)dataPackage[2]);
					log.info("    == returned Guid (rPid): " + rPid.getValue());

					checkEquals(mn.getLatestRequestUrl(),"pid returned from create should equal that given",
							((Identifier)dataPackage[0]).getValue(), rPid.getValue());
					
					testLoc = "get   ";
					Thread.sleep(1000);
					InputStream data = mn.get(null, rPid);
					checkTrue(mn.getLatestRequestUrl(), "get against the object should not equal null", null != data);
//					String str = IOUtils.toString(data);
//					checkTrue(mn.getLatestRequestUrl(),"should be able to read the content as created ('" + str.substring(0,100) + "...')",
//							str.indexOf("IPCC Data Distribution Centre Results ") != -1);
					data.close();
					testLoc = "      ";
				}
				catch (BaseException e) {
					status = "Error";
					handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() +
							": " + e.getDetail_code() + ": " + e.getDescription());
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}
				
				nodeSummary.add("Test " + j + ": " + status + ": " + testLoc + ": " + unicodeString.get(j));
			}
			System.out.println();
			for (int k=0; k<nodeSummary.size(); k++) 
			{
				System.out.println(nodeSummary.get(k));
			}
			System.out.println();
		}
    }


	
	/**
	 *  Test MNStorage.update() functionality
	 */
	@Test
	public void testUpdate() {

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);

				// prep for checking update time. 
				Thread.sleep(100);
				Date now = new Date();
				Thread.sleep(100);
				
				// create the new data package to update with. 
				dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				Identifier newPid = (Identifier) dataPackage[0];
				
				// do the update
				Identifier updatedPid = mn.update(null,
						originalPid,
						(InputStream) dataPackage[1],    // new data
						newPid,
						(SystemMetadata) dataPackage[2]  // new sysmeta
						);
		
				checkEquals(mn.getLatestRequestUrl(),"pid returned from update should match that given",
						newPid.getValue(), updatedPid.getValue());

				// check obsoletes and obsoletedBy fields
				 SystemMetadata updatedSysmeta = mn.getSystemMetadata(null, updatedPid);
				 checkEquals(mn.getLatestRequestUrl(),"sysmeta of updatePid should have the originalPid in obsoletes field",
						 updatedSysmeta.getObsoletes().getValue(),originalPid.getValue());
				 
				 checkTrue(mn.getLatestRequestUrl(), "MN should be setting the dateSystemMetadataModified property",
						 updatedSysmeta.getDateSysMetadataModified() != null);
				 
				 SystemMetadata oldSysmeta = mn.getSystemMetadata(null, originalPid);
				 checkEquals(mn.getLatestRequestUrl(),"sysmeta of original Pid should have new pid in obsoletedBy field",
						 oldSysmeta.getObsoletedBy().getValue(),updatedPid.getValue());
				 
				 // the old pid needs to be in a timebound listObject search
				 ObjectList ol = mn.listObjects(null, now, null, null, null, null, null);
				 boolean foundUpdatedSysmeta = false;
				 for (ObjectInfo oi : ol.getObjectInfoList()) {
					 if (oi.getIdentifier().getValue().equals(originalPid.getValue())) {
						 foundUpdatedSysmeta = true;
					 }
				 }
				 checkTrue(mn.getLatestRequestUrl(),"should find original pid in time-bound listObject " +
				 		"where start time is after original create and before update",foundUpdatedSysmeta);
				 
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + 
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

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// create the new data package to update with. 
				dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
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
					handleFail(mn.getLatestRequestUrl(),"should not be able to update with obsoletedBy " +
						"field set (for pid = " + updatedPid.getValue() + ")");		
				}
			}
			catch (InvalidSystemMetadata e) {
				// expected outcome
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + 
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

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				
				Identifier originalPid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// create the new data package to update with. 
				dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
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
				handleFail(mn.getLatestRequestUrl(),"should not be able to update with faulty " +
					"obsoletes information (for pid = " + updatedPid.getValue() + ")");		
			}
			catch (InvalidSystemMetadata e) {
				// expected outcome
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + 
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
			setupClientSubject("testRightsHolder");
			
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testUpdate_NoCert() vs. node: " + currentUrl);

			
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				SystemMetadata sysmeta = (SystemMetadata) dataPackage[2];
				Identifier pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], sysmeta);
				
				// create the new data package to update with
				dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestUpdate",true);
				
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
		
					handleFail(mn.getLatestRequestUrl(),"should not be able to update an object if no certificate " +
							"(updated  " + pid + " with " + updatedPid);
				} 
				catch (InvalidToken na) {
						// expected behavior
				}
				catch (NotAuthorized na) {
					// expected behavior
				}
				catch (BaseException e) {
					handleFail(mn.getLatestRequestUrl(),"Expected InvalidToken or NotAuthorized, got: " +
							e.getClass().getSimpleName() + ": " 
							+ e.getDetail_code() + ": " + e.getDescription());
				}
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Exception while setting up test (mn.create): " +
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
	 *  Test MNStorage.archive() functionality
	 */
	@Test
	public void testArchive() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testRightsHolder");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testArchive() vs. node: " + currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestDelete",true);
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				// try the archive
				Identifier archivedPid = mn.archive(null, pid);

				checkEquals(mn.getLatestRequestUrl(),"pid returned from archive should match that given",
						((Identifier)dataPackage[0]).getValue(), archivedPid.getValue());
				
				SystemMetadata smd = mn.getSystemMetadata(null, pid);
				checkTrue(mn.getLatestRequestUrl(), "sysmeta for archived object should be has status of archived",smd.getArchived());
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
	 *  Test MNStorage.archive() functionality
	 */
	@Test
	public void testArchive_NotFound() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			setupClientSubject("testRightsHolder");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testArchive() vs. node: " + currentUrl);

			try {
				// try the archive
				Identifier fakePid = new Identifier();
				fakePid.setValue("fakeID." + ExampleUtilities.generateIdentifier());
				Identifier archivedPid = mn.archive(null, fakePid);

				handleFail(mn.getLatestRequestUrl(),"member node should return NotFound if pid" +
						"to be archived does not exist there.  Pid: " + archivedPid);
			}
			catch (NotFound e) {
				// expected outcome
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected NotFound, got: " + e.getClass().getSimpleName() +
						": " + e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
	@Test
	public void testArchive_NoCert() 
	{
		Iterator<Node> it = getMemberNodeIterator();

		while ( it.hasNext() ) {
			// subject under which to create the object to be archived
			setupClientSubject("testRightsHolder");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testArchive_NoCert() vs. node: " + currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestArchive",true);
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				setupClientSubject_NoCert();
				// try the archive
				mn.archive(null, pid);
				handleFail(mn.getLatestRequestUrl(),"should not be able to archive an object if no certificate");
			}
			catch (InvalidToken na) {
				try {
					setupClientSubject("testRightsHolder");
					InputStream is = mn.get(null, pid);	
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				catch (BaseException e) {
					handleFail(mn.getLatestRequestUrl(),"Got InvalidToken, but couldn't perform subsequent get(). Instead: " +
							e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
							": " + e.getDescription());
				} 
				
			}
			catch (NotAuthorized na) {
				try {
					setupClientSubject("testRightsHolder");
					InputStream is = mn.get(null, pid);	
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				catch (BaseException e) {
					handleFail(mn.getLatestRequestUrl(),"Got InvalidToken, but couldn't perform subsequent get(). Instead: " +
							e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
							": " + e.getDescription());
				} 
				
			}
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),"Expected InvalidToken or NotAuthorized, got: " +
						e.getClass().getSimpleName() + ": " 
						+ e.getDetail_code() + ": " + e.getDescription());
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
			// subject under which to create the object to be archived
			setupClientSubject("testRightsHolder");

			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDelete_NoCert() vs. node: " + currentUrl);

			Identifier pid = null;
			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3TestDelete",true);
				pid = mn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);
				
				setupClientSubject_NoCert();
				// try the archive
				mn.delete(null, pid);
				handleFail(mn.getLatestRequestUrl(),"should not be able to delete an object if no certificate");
			}
			catch (InvalidToken na) {
				try {
					setupClientSubject("testRightsHolder");
					InputStream is = mn.get(null, pid);	
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				catch (BaseException e) {
					handleFail(mn.getLatestRequestUrl(),"Got InvalidToken, but couldn't perform subsequent get(). Instead: " +
							e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
							": " + e.getDescription());
				} 
				
			}
			catch (NotAuthorized na) {
				try {
					setupClientSubject("testRightsHolder");
					InputStream is = mn.get(null, pid);	
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				catch (BaseException e) {
					handleFail(mn.getLatestRequestUrl(),"Got NotAuthorized, but couldn't perform subsequent get(). Instead: " +
							e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
							": " + e.getDescription());
				} 
				
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
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";

	}

}
