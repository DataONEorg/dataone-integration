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
import org.dataone.client.CNode;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier1IT extends ContextAwareTestCaseDataone {

	private  String format_text_csv = "text/csv";
	
    private static Identifier reservedIdentifier = null;

//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";

	
	 private static String currentUrl;
  



	
	/**
	 * tests that a valid date is returned from ping
	 */
	@Test
	public void testPing() {
		setupClientSubject_NoCert();
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testPing() vs. node: " + currentUrl);
		
			try {
				Date pingDate = cn.ping();
				
				checkTrue(currentUrl,"ping() should return a valid date", pingDate != null);
				// other invalid dates will be thrown as IOExceptions cast to ServiceFailures
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
	 * tests that a valid ObjectFormatList is returned from listFormats()
	 */
    @Test
    public void testListFormats() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListFormats(...) vs. node: " + currentUrl);

    		try {
    			ObjectFormatList response = cn.listFormats();
    			checkTrue(currentUrl,"listFormats(...) returns an ObjectFormatList object",
    					response instanceof ObjectFormatList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    /**
     * tests that getFormat returns a valid ObjectFormat.  calls listFormats first
     * and fails if there aren't any in the list.
     */
    @Test
    public void testGetFormat() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

    		try {
    			ObjectFormatList ofList = cn.listFormats();
    			if (ofList.getCount() > 0) {
    				ObjectFormatIdentifier formatId = ofList.getObjectFormat(0).getFormatId();
    				ObjectFormat response = cn.getFormat(formatId);
    				checkTrue(currentUrl,"getFormat(...) returns an ObjectFormat object", 
    						response instanceof ObjectFormat);
    			} else {
    				handleFail(currentUrl,"no formats in format list to use for testing getFormat()");
    			}
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    /**
     * tests that a bogus formatID throws a NotFound exception
     */
    @Test
    public void testGetFormat_bogusFormat() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

    		try {
    			ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
    			formatId.setValue("aBogusFormat");
    			ObjectFormat response = cn.getFormat(formatId);
    		
    			handleFail(currentUrl,"getFormat(...) with a bogus formatID should " +
    					"throw an exception.");
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (NotFound e) {
    			// the desired outcome
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }


    @Test
    public void testListChecksumAlgorithms() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListChecksumAlgorithms(...) vs. node: " + currentUrl);

    		try {
    			ChecksumAlgorithmList response = cn.listChecksumAlgorithms();
    			checkTrue(currentUrl,"listChecksumAlgorithms(...) returns a valid ChecksumAlgorithmList object", 
    					response instanceof ChecksumAlgorithmList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }


    @Test
    public void testGetLogRecords()
    {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
       while (it.hasNext()) {
    	   currentUrl = it.next().getBaseURL();
    	   CNode cn = new CNode(currentUrl);
    	   printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    	   currentUrl = cn.getNodeBaseServiceUrl();
     

           log.info("current time is: " + new Date());
           Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
           log.info("fromDate is: " + fromDate);

           try {
        	   Log eventLog = cn.getLogRecords(null, fromDate, null, null, null, null);
        	   checkTrue(currentUrl,"getLogRecords should return a log datatype", eventLog != null);
        	   
        	   // check that log events are created
        	   Identifier pid = new Identifier();
        	   pid.setValue("aBogusIdentifier");
        	   boolean canCreate = false;
        	   try {
        		   canCreate = cn.isAuthorized(null, pid, Permission.WRITE);
        	   } catch (Exception e) {
        		   // do nothing - can't expect to create in Tier1 tests
        		   log.info("Cannot create objects so skipping more precise logging test "
        				   + "on node: " + currentUrl);
        	   }
        	   if (canCreate) { 
        		     pid = ExampleUtilities.doCreateNewObject(cn, "TierTesting:");
        		   Date toDate = new Date(System.currentTimeMillis());
        		   log.info("toDate is: " + toDate);

        		   eventLog = cn.getLogRecords(null, fromDate, toDate, Event.CREATE, null, null);
        		   log.info("log size: " + eventLog.sizeLogEntryList());
        		   boolean isfound = false;
        		   for(int i=0; i<eventLog.sizeLogEntryList(); i++)
        		   { //check to see if our create event is in the log
        			   LogEntry le = eventLog.getLogEntry(i);
        			   log.debug("le: " + le.getIdentifier().getValue());
        			   log.debug("rGuid: " + pid.getValue());
        			   if(le.getIdentifier().getValue().trim().equals(pid.getValue().trim()))
        			   {
        				   isfound = true;
        				   log.info("log record found");
        				   break;
        			   }
        		   }
        		   log.info("isfound: " + isfound);
        		   checkTrue(currentUrl, "newly created object is in the log", isfound); 
        	   }
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
     * tests that a valid nodelist is returned
     */
    @Test
    public void testListNodes() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListNodes(...) vs. node: " + currentUrl);

    		try {
    			NodeList response = cn.listNodes();
    			checkTrue(currentUrl,"listNodes(...) returns a valid NodeList object", 
    					response instanceof NodeList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    /**
     * tests that reserveIdentifier works, then tries again to make sure
     * that the identifier cannot be reserved again.
     */
    @Test
    public void testReserveIdentifier() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testReserveIdentifier(...) vs. node: " + currentUrl);

    		boolean isReserved = false;
    		try {
    			Identifier pid = new Identifier();
    			pid.setValue(ExampleUtilities.generateIdentifier());

    			Identifier response = cn.reserveIdentifier(null,pid);
    			checkTrue(currentUrl,"reserveIdentifier(...) should return the given identifier",
    					response.equals(pid));
    			isReserved = true;
    			// try again - should fail
    			response = cn.reserveIdentifier(null,pid);

    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (IdentifierNotUnique e) {
    			if (isReserved) {
    				// then got the desired outcome
    			} else {
    				handleFail(currentUrl,e.getDescription());
    			}
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    /**
     * test generating an Identifier containing the given fragment.
     * Specifies the DOI syntax
     */
    @Test
    public void testGenerateIdentifier() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

    		try {
    			String fragment = "CNodeTier1Test";
    			Identifier response = cn.generateIdentifier(null,"DOI",fragment);
    			checkTrue(currentUrl,"generateIdentifier(...) should return an Identifier object" +
    					"containing the given fragment", response.getValue().contains(fragment));
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    /**
     * tests validity of scheme names by trying an invalid scheme.
     * It should throw an error.
     */
    @Test
    public void testGenerateIdentifier_badScheme() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

    		try {
    			String fragment = "CNodeTier1Test";
    			Identifier response = cn.generateIdentifier(null,"bloip",fragment);
    			handleFail(currentUrl,"generateIdentifier(...) with a bogus scheme should" +
    					"throw an exception (should not have reached here)");
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (InvalidRequest e) {
    			// the expected outcome indicating good behavior :-)
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    
    @Test
    public void testHasReservation() {
    	
    	Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);
    		
    		try {
    			boolean response = false;
    			if (reservedIdentifier != null) {
    				response = cn.hasReservation(null,clientSubject,reservedIdentifier);
    			} else {
    				Identifier pid = new Identifier();
    				pid.setValue(ExampleUtilities.generateIdentifier());
    				cn.reserveIdentifier(null, pid );
    				response = cn.hasReservation(null,clientSubject,pid);
    			}
    			checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testHasReservation_noReservation() {
    	
    	Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
    	
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);

    		try {
    			boolean response = false;
    			Identifier pid = new Identifier();
    			pid.setValue(ExampleUtilities.generateIdentifier());
    			response = cn.hasReservation(null,clientSubject, pid);

    			checkTrue(currentUrl,"response cannot be false. [Only true or exception].", 
    					response);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    
	@Test
	public void testCreate() {

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("cNodeTier1TestCreate",true);				
				Identifier pid = cn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);	
				
				checkEquals(currentUrl,"pid of created object should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				InputStream theDataObject = cn.get(null,pid);
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
     * test creation of data with challenging unicode identifier.
     */
	@Ignore("need to debug for CN implementation")
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
		setupClientSubject("testRightsHolder");
		Iterator<Node> it = getMemberNodeIterator();
		printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");

		// get identifiers to check with
		Vector<String> unicodeString = new Vector<String>();
		Vector<String> escapedString = new Vector<String>();
		InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
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
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testCreateData_IdentifierEncoding() vs. node: " + currentUrl);		

			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			String idPrefix = "testCNodeTier1:";
			
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

					rGuid = cn.create(null, (Identifier) dataPackage[0], 
							(InputStream)dataPackage[1], (SystemMetadata)dataPackage[2]);
					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
					checkEquals(currentUrl,"guid returned from create should equal that given",
							((Identifier)dataPackage[0]).getValue(), rGuid.getValue());
					InputStream data = cn.get(null, rGuid);
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
    

	@Test
	public void testRegisterSystemMetadata() {

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = generateTestDataPackage("cNodeTier1TestCreate",true);
				SystemMetadata smd = (SystemMetadata) dataPackage[2];
				Identifier pid = cn.registerSystemMetadata(null,(Identifier) dataPackage[0], smd);	
				
				checkEquals(currentUrl,"pid of registered sysmetadata should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				SystemMetadata smdReturned = cn.getSystemMetadata(null,pid);
				checkEquals(currentUrl,"should be able to get registered sysmeta",
						smdReturned.getIdentifier().getValue(),
						smd.getIdentifier().getValue());
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
     * Tests the parameterless and parameterized listObject methods for propert returns.
     */
    @Test
    public void testListObjects() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = cn.listObjects(null);
    			checkTrue(currentUrl,"listObjects() should return an ObjectList", ol != null);
    			
    			Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
    			Boolean replicaStatus = true;
				ol = cn.listObjects(null, startTime, endTime, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(currentUrl,"listObjects(<parameters>) returns an ObjectList", ol != null);
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
     * Tests that the startTime parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than startTime.
     */
    @Test
    public void testListObjects_StartTimeTest() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = cn.listObjects(null);
    			checkTrue(currentUrl,"listObjects() should return an ObjectList", ol != null);
    			
    			ObjectInfo oi0 = ol.getObjectInfo(0);
    			Date startTime = null;
   				ObjectInfo excludedObjectInfo = null;
   				for (ObjectInfo oi: ol.getObjectInfoList()) {
   					if (!oi.getDateSysMetadataModified().equals(oi0.getDateSysMetadataModified())) {
   						// which is earlier?  can't assume chronological order of objectlist
   						if (oi.getDateSysMetadataModified().after(oi0.getDateSysMetadataModified())) {
   							startTime = oi.getDateSysMetadataModified();
   							excludedObjectInfo = oi0;
   						} else {
   							startTime = oi0.getDateSysMetadataModified();
   							excludedObjectInfo = oi;
   						}
   						break;
   					}
   				}
   				if (excludedObjectInfo == null) {
    				handleFail(currentUrl,"could not find 2 objects with different sysmeta modified dates");
    			} else {
   				
    				// call listObjects with a startTime
    				ol = cn.listObjects(null, startTime, null, null, null, null, null);

    				for (ObjectInfo oi: ol.getObjectInfoList()) {
    					if (oi.getIdentifier().equals(excludedObjectInfo.getIdentifier())) {
    						handleFail(currentUrl,"identifier " + excludedObjectInfo.getIdentifier() +
    								" should not be in the objectList where startTime set to " + startTime);
    					}
    				}
    			}
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
    
    
    @Test
    public void testGet() {
 //   	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			Identifier id = procurePublicReadableTestObject(cn);
    			InputStream is = cn.get(null,id);
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

    @Test
    public void testGetSystemMetadata() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
    		
    		try {
    			Identifier id = procurePublicReadableTestObject(cn);
    			SystemMetadata smd = cn.getSystemMetadata(null,id);
    			checkTrue(currentUrl,"getSystemMetadata() returns a SystemMetadata object", smd != null);
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
    
    
    @Test
    public void testDescribe() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl); 
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testDescribe() vs. node: " + currentUrl);
		
    		try {
    			Identifier id = procurePublicReadableTestObject(cn);
    			DescribeResponse dr = cn.describe(null,id);
    			checkTrue(currentUrl,"describe() returns a DescribeResponse object", dr != null);	
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
    


    @Test
    public void testResolve() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testResolve(...) vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(cn);
    			Identifier pid = ol.getObjectInfo(0).getIdentifier();
    			log.debug("   pid = " + pid.getValue());

    			ObjectLocationList response = cn.resolve(null,pid);
    			checkTrue(currentUrl,"resolve(...) returns a ObjectLocationList object",
    					response != null && response instanceof ObjectLocationList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    

	@Test
	public void testGetChecksum() {
    	setupClientSubject_NoCert();
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testGetChecksum(...) vs. node: " + currentUrl);

			try {   
    			Identifier id = procurePublicReadableTestObject(cn);
    			Checksum cs = cn.getChecksum(null,id);
    			checkTrue(currentUrl,"getChecksum() returns a Checksum object", cs != null);
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
	
 
	@Test
	public void testSearch() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSearch(...) vs. node: " + currentUrl);

			try {
				ObjectList response = cn.search(null,"SOLR","*:*");
				checkTrue(currentUrl,"search(...) returns a ObjectList object", response != null);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}



	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
