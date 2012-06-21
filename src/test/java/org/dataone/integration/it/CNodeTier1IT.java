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
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.DescribeResponse;
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
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.D1Url;
import org.dataone.service.util.DateTimeMarshaller;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.StringUtils;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1IT extends ContextAwareTestCaseDataone {

	private  String format_text_csv = "text/csv";

//	private static Identifier reservedIdentifier = null;

//	private static String unicodeIdPrefix = "testCNodeTier1:Unicode:";
	private static String unicodeIdPrefix = "testCNodeTier1";
	
	protected String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "urn:node:cnDevUNM1");

	
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";

	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	private static Vector<String> encodedPIDStrings = new Vector<String>();
	
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
				
				checkTrue(cn.getLatestRequestUrl(),"ping() should return a valid date", pingDate != null);
				// other invalid dates will be thrown as IOExceptions cast to ServiceFailures
			} 
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
    

	
	/**
	 * tests that a valid ObjectFormatList is returned from listFormats()
	 */
    @Test
    public void testListFormats() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListFormats(...) vs. node: " + currentUrl);

    		try {
    			ObjectFormatList response = cn.listFormats();
    			checkTrue(cn.getLatestRequestUrl(),"listFormats(...) returns an ObjectFormatList object",
    					response instanceof ObjectFormatList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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

    /**
     * tests that getFormat returns a valid ObjectFormat.  calls listFormats first
     * and fails if there aren't any in the list.
     */
    @Test
    public void testGetFormat() {
    	setupClientSubject_NoCert();
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
    				checkTrue(cn.getLatestRequestUrl(),"getFormat(...) returns an ObjectFormat object", 
    						response instanceof ObjectFormat);
    			} else {
    				handleFail(cn.getLatestRequestUrl(),"no formats in format list to use for testing getFormat()");
    			}
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
    
    /**
     * tests that a bogus formatID throws a NotFound exception
     */
    @Test
    public void testGetFormat_bogusFormat() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

    		try {
    			ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
    			formatId.setValue("aBogusFormat");
    			cn.getFormat(formatId);
    		
    			handleFail(cn.getLatestRequestUrl(),"getFormat(...) with a bogus formatID should " +
    					"throw an exception.");
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (NotFound e) {
    			// the desired outcome
    		}
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


    @Test
    public void testListChecksumAlgorithms() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListChecksumAlgorithms(...) vs. node: " + currentUrl);

    		try {
    			ChecksumAlgorithmList response = cn.listChecksumAlgorithms();
    			checkTrue(cn.getLatestRequestUrl(),"listChecksumAlgorithms(...) returns a valid ChecksumAlgorithmList object", 
    					response instanceof ChecksumAlgorithmList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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

    
    /**
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * 
     */
    @Test
    public void testGetLogRecords()
    {
    	setupClientSubject(cnSubmitter);
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    		currentUrl = cn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = cn.getLogRecords(null, null, null, null, null, null);
    			checkTrue(cn.getLatestRequestUrl(),"getLogRecords should return a log datatype", eventLog != null);
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}	           
    	}
    }

    
    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
    @Test
    public void testGetLogRecords_Slicing()
    {
    	setupClientSubject(cnSubmitter);
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetLogRecords_Slicing(...) vs. node: " + currentUrl);  
    		currentUrl = cn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = cn.getLogRecords(null, null, null, null, null, null);   			
    			
    			StringBuffer sb = new StringBuffer();
    			int i = 0;
    			if (eventLog.getCount() != eventLog.sizeLogEntryList())
    				sb.append(++i + ". 'count' attribute should equal the number of LogEntry objects returned.  \n");
    		
    			if (eventLog.getTotal() < eventLog.getCount())
    				sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned Log.  \n");

    			if (eventLog.getTotal() < eventLog.sizeLogEntryList())
    				sb.append(++i + "'total' attribute should be >= the number of LogEntry objects returned.  \n");


    			// test that one can limit the count
    			int halfCount = eventLog.sizeLogEntryList() / 2; // rounds down
    			eventLog = cn.getLogRecords(null, null, null, null, 0, halfCount);

    			if (eventLog.sizeLogEntryList() != halfCount)
    				sb.append(++i + ". should be able limit the number of returned LogEntry objects using 'count' parameter.");
    				    			
    			// TODO:  test that 'start' parameter does what it says

    			// TODO: paging test
    			
    			
    			if (i > 0) {
    				handleFail(cn.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
    			}   			
    			
    		}
    		catch (NotAuthorized e) {
    			handleFail(cn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}	           
    	}
    }
	
    
    
    /**
     * Tests that setting fromDate parameter for getLogRecords() excludes earlier
     * records.
     * 
     */
    @Test
    public void testGetLogRecords_DateFiltering()
    {
    	setupClientSubject(cnSubmitter);
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    		currentUrl = cn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = cn.getLogRecords(null, null, null, null, null, null);
    			int allEventsCount = eventLog.getTotal();
    			
    			
    			LogEntry entry0 = eventLog.getLogEntry(0);
    			Date fromDate = null;
    			LogEntry excludedEntry = null;
   				for (LogEntry le: eventLog.getLogEntryList()) {
   					if (!le.getDateLogged().equals(entry0.getDateLogged())) {
   						// which is earlier?  can't assume chronological order of the list
   						if (le.getDateLogged().after(entry0.getDateLogged())) {
   							fromDate = le.getDateLogged();
   							excludedEntry = entry0;
   						} else {
   							fromDate = entry0.getDateLogged();
   							excludedEntry = le;
   						}
   						break;
   					}
   				}
   				if (excludedEntry == null) {
    				handleFail(cn.getLatestRequestUrl(),"could not find 2 objects with different dateLogged times");
    			} else {
   				
    				// call with a fromDate
    				eventLog = cn.getLogRecords(null, fromDate, null, null, null, null);

    				for (LogEntry le : eventLog.getLogEntryList()) {
    					if (le.getEntryId().equals(excludedEntry.getEntryId())) {
    						handleFail(cn.getLatestRequestUrl(),"entryID " + excludedEntry.getEntryId() +
    								" should not be in the event log where fromDate set to " + fromDate);
    						break;
    					}
    				}
    			}
    		} 
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
    			checkTrue(cn.getLatestRequestUrl(),"listNodes(...) returns a valid NodeList object", 
    					response instanceof NodeList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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

    /**
     * tests that reserveIdentifier works, then tries again to make sure
     * that the identifier cannot be reserved again.
     */
    @Test
    public void testReserveIdentifier() {
    	setupClientSubject("testSubmitter");
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
    			checkTrue(cn.getLatestRequestUrl(),"reserveIdentifier(...) should return the given identifier",
    					response.equals(pid));
    			isReserved = true;
    			// try again - should fail
    			response = cn.reserveIdentifier(null,pid);

    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (IdentifierNotUnique e) {
    			if (isReserved) {
    				// then got the desired outcome
    			} else {
    				handleFail(cn.getLatestRequestUrl(),e.getDescription());
    			}
    		}
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

    /**
     * test generating an Identifier containing the given fragment.
     * Specifies the UUID syntax
     */
    @Test
    public void testGenerateIdentifier() {
    	setupClientSubject("testSubmitter");
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

    		try {
    			String fragment = "CNodeTier1Test";
    			Identifier response = cn.generateIdentifier(null,"UUID",fragment);
    			//TODO: UUID isn't supporting the fragment concept, so can't check 
    			// this yet.
//    			checkTrue(cn.getLatestRequestUrl(),"generateIdentifier(...) should return an Identifier object" +
//    					" containing the given fragment: '" + fragment + "' Got: '" + response.getValue() + "'",
//    					response.getValue().contains(fragment));
    			checkTrue(cn.getLatestRequestUrl(),"generateIdentifier(...) should return a UUID-style" +
    					"identifier with 5 hexidecimal segments separated by '-'s.  Got: " +
    					response.getValue(),
    					StringUtils.countOccurrencesOf(response.getValue(),"-") >=4);
    					
	
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
    			cn.generateIdentifier(null,"bloip",fragment);
    			handleFail(cn.getLatestRequestUrl(),"generateIdentifier(...) with a bogus scheme should" +
    					"throw an exception (should not have reached here)");
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (InvalidRequest e) {
    			// the expected outcome indicating good behavior :-)
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    /**
     * Creates an identifier and reserves it.  Then checks that the client indeed
     * has the reservation (hasReservation());
     */
    @Test
    public void testHasReservation() {
    	
    	setupClientSubject("testSubmitter");
    	Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);
    		
    		try {
    			boolean response = false;
//    			if (reservedIdentifier != null) {
//    				response = cn.hasReservation(null,clientSubject,reservedIdentifier);
//    			} else {
    				Identifier pid = new Identifier();
    				pid.setValue(ExampleUtilities.generateIdentifier());
    				cn.reserveIdentifier(null, pid );
    				response = cn.hasReservation(null,clientSubject,pid);
//    			}
    			checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    /**
     * Generates a new identifier, and tries hasReservation() without first reserving it.
     * Expect a NotFound exception.  
     */
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

    			checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", 
    					response);
    		}
    		catch (NotFound e) {
    			; // this is desired behavior
    		}
//    		catch (NotAuthorized e) {
//    			; // this is also acceptable
//    		}
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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

  
	@Ignore("need a subject able to call cn.create()")
	@Test
	public void testCreate() {

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getCoordinatingNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage("cNodeTier1TestCreate",true);				
				Identifier pid = cn.create(null,(Identifier) dataPackage[0],
						(InputStream) dataPackage[1], (SystemMetadata) dataPackage[2]);	
				
				checkEquals(cn.getLatestRequestUrl(),"pid of created object should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				InputStream theDataObject = cn.get(null,pid);
				String objectData = IOUtils.toString(theDataObject);
				checkTrue(cn.getLatestRequestUrl(),"should get back an object containing submitted text:" + objectData,
						objectData.contains("Plain text source"));
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
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
	@Ignore("need a subject able to call cn.create()")
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
		setupClientSubject("testRightsHolder");
		Iterator<Node> it = getCoordinatingNodeIterator();
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
			
			for (int j=0; j<unicodeString.size(); j++) 
			{
				String status = "OK   ";
				try {
					//    			String unicode = unicodeString.get(j);
					System.out.println();
					System.out.println(j + "    unicode String:: " + unicodeString.get(j));
					String idString = unicodeIdPrefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
//					String idStringEscaped = unicodeIdPrefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);


					Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage(idString,false);

					// rGuid is either going to be the escaped ID or the non-escaped ID
					Identifier rGuid = null;

					rGuid = cn.create(null, (Identifier) dataPackage[0], 
							(InputStream)dataPackage[1], (SystemMetadata)dataPackage[2]);
					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
					checkEquals(cn.getLatestRequestUrl(),"guid returned from create should equal that given",
							((Identifier)dataPackage[0]).getValue(), rGuid.getValue());
					InputStream data = cn.get(null, rGuid);
					checkTrue(cn.getLatestRequestUrl(), "get against the object should not equal null", null != data);
					String str = IOUtils.toString(data);
					checkTrue(cn.getLatestRequestUrl(),"should be able to read the content as created ('" + str + "')",
							str.indexOf("Plain text source") != -1);
					data.close();
				}
				catch (BaseException e) {
					status = "Error";
					handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() +
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

		Iterator<Node> it = getCoordinatingNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testCreate() vs. node: " + currentUrl);

			try {
				Object[] dataPackage = ExampleUtilities.generateTestSciMetaDataPackage("cNodeTier1TestCreate",true);
				SystemMetadata smd = (SystemMetadata) dataPackage[2];
				Identifier pid = cn.registerSystemMetadata(null,(Identifier) dataPackage[0], smd);	
				
				checkEquals(cn.getLatestRequestUrl(),"pid of registered sysmetadata should equal that given",
						((Identifier)dataPackage[0]).getValue(), pid.getValue());
				
				SystemMetadata smdReturned = cn.getSystemMetadata(null,pid);
				checkEquals(cn.getLatestRequestUrl(),"should be able to get registered sysmeta",
						smdReturned.getIdentifier().getValue(),
						smd.getIdentifier().getValue());
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
						+ e.getDetail_code() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}

    
    /**
     * Tests the parameterless and parameterized listObject methods for proper returns.
     */
    @Test
    public void testListObjects() {
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = cn.listObjects(null);
    			checkTrue(cn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
    			
    			Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
    			Boolean replicaStatus = true;
				ol = cn.listObjects(null, startTime, endTime, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(cn.getLatestRequestUrl(),"listObjects(<parameters>) returns an ObjectList", ol != null);
    		} 
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

    @Test
    public void testListObjects_Slicing()
    {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListObjects_Slicing(...) vs. node: " + currentUrl);  
    		currentUrl = cn.getNodeBaseServiceUrl();

    		try {
    			ObjectList ol = cn.listObjects();
    			// make sure the count is accurate
    			StringBuffer sb = new StringBuffer();
    			int i = 0;
    			if (ol.getCount() != ol.sizeObjectInfoList())
    				sb.append(++i + ". 'count' attribute should equal the number of ObjectInfos returned.  \n");
    		
    			if (ol.getTotal() < ol.getCount())
    				sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned ObjectList.  \n");

    			if (ol.getTotal() < ol.sizeObjectInfoList())
    				sb.append(++i + "'total' attribute should be >= the number of ObjectInfos returned.  \n");


    			// test that one can limit the count
    			int halfCount = ol.sizeObjectInfoList() / 2; // rounds down
    			ol = cn.listObjects(null, null, null, null, null, 0, halfCount);

    			if (ol.sizeObjectInfoList() != halfCount)
    				sb.append(++i + ". should be able to limit the number of returned ObjectInfos using 'count' parameter.");
    				    			
    			// TODO:  test that 'start' parameter does what it says

    			// TODO: paging test
    			
    			
    			if (i > 0) {
    				handleFail(cn.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
    			}
    			
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}	           
    	}
    }
    
    @Ignore("test in progress")
    @Test
    public void testListObjects_formatID_filter() {
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = cn.listObjects(null);
    			checkTrue(cn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
    			
    			Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(ExampleUtilities.FORMAT_EML_2_1_0);
    			Boolean replicaStatus = true;
				ol = cn.listObjects(null, startTime, endTime, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(cn.getLatestRequestUrl(),"listObjects(<parameters>) returns an ObjectList", ol != null);
    		} 
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
    
    
    /**
     * Tests that the fromDate parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than fromDate.
     */
    @Test
    public void testListObjects_FromDateTest() {
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(cn);
    			checkTrue(cn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
    			if (ol.getTotal() == 0)
    				throw new TestIterationEndingException("no objects found in listObjects");
    			ObjectInfo oi0 = ol.getObjectInfo(0);
    			Date fromDate = null;
   				ObjectInfo excludedObjectInfo = null;
   				for (ObjectInfo oi: ol.getObjectInfoList()) {
   					if (!oi.getDateSysMetadataModified().equals(oi0.getDateSysMetadataModified())) {
   						// which is earlier?  can't assume chronological order of objectlist
   						if (oi.getDateSysMetadataModified().after(oi0.getDateSysMetadataModified())) {
   							fromDate = oi.getDateSysMetadataModified();
   							excludedObjectInfo = oi0;
   						} else {
   							fromDate = oi0.getDateSysMetadataModified();
   							excludedObjectInfo = oi;
   						}
   						break;
   					}
   				}
   				if (excludedObjectInfo == null) {
   					// all objects in list have same date, so set the from date
   					// to a future date
   					long millisec = oi0.getDateSysMetadataModified().getTime() + 60000;
   					fromDate = new Date(millisec);
   					excludedObjectInfo = oi0;
   				}

   				// call listObjects with a fromDate
   				ol = cn.listObjects(null, fromDate, null, null, null, null, null);

   				for (ObjectInfo oi: ol.getObjectInfoList()) {
   					if (oi.getIdentifier().equals(excludedObjectInfo.getIdentifier())) {
   						handleFail(cn.getLatestRequestUrl(),String.format("identifier %s with sysMetaModified date of '%s'" +
   								" should not be in the objectList where 'fromDate' parameter set to '%s'", 
   								excludedObjectInfo.getIdentifier().getValue(),
   								DateTimeMarshaller.serializeDateToUTC(excludedObjectInfo.getDateSysMetadataModified()),
   								DateTimeMarshaller.serializeDateToUTC(fromDate)
   						));
   					}
   				}

    		} 
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
    
    
    
    /**
     * Tests that an object identifier returned by listObjects is retrievable
     * using cn.get().  Since only science metadata objects are gettable on 
     * the CNs, the listObjects() call filters on one of the objectFormatIds
     * for science metadata.  The test assumes that data replication between
     * the CNs has occurred.
     */
    @Test
    public void testGet() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(cn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(cn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(cn);
    			InputStream is = cn.get(null,id);
    			checkTrue(cn.getLatestRequestUrl(),"get() returns an objectStream", is != null);
    		}
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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

    @Test
    public void testGetSystemMetadata() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
    		
    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(cn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(cn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(cn);
    			SystemMetadata smd = cn.getSystemMetadata(null,id);
    			checkTrue(cn.getLatestRequestUrl(),"getSystemMetadata() returns a SystemMetadata object", smd != null);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
    
    
    @Test
    public void testDescribe() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl); 
    		currentUrl = cn.getNodeBaseServiceUrl();
    		printTestHeader("testDescribe() vs. node: " + currentUrl);
		
    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(cn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(cn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(cn);
    			DescribeResponse dr = cn.describe(null,id);
    			checkTrue(cn.getLatestRequestUrl(),"describe() returns a DescribeResponse object", dr != null);	
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
    

    /**
     * This test procures an objectlist (if no objects in it, it creates one),
     * and checks for successful return of an ObjectLocationList from cn.resolve()
     * for the first item in the objectlist.  
     */
    @Test
    public void testResolve() {
    	setupClientSubject_NoCert();
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
    			checkTrue(cn.getLatestRequestUrl(),"resolve(...) returns an ObjectLocationList object",
    					response != null && response instanceof ObjectLocationList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
    
    /**
     * Checks that a Checksum object is returned from cn.getChecksum() for a
     * public readable object
     */
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
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(cn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(cn,D1TypeBuilder.buildIdentifier(objectIdentifier));
 //   			Identifier id = procurePublicReadableTestObject(cn);
    			Checksum cs = cn.getChecksum(null,id);
    			checkTrue(cn.getLatestRequestUrl(),"getChecksum() returns a Checksum object", cs != null);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
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
	
	/**
	 * Test runs a basic solr query ("q=*:*") using cn.search().  Successful if
	 * an ObjectList is returned. (can be empty)
	 */
	@Test
	public void testSearch() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSearch(...) vs. node: " + currentUrl);

			try {
				ObjectList response = cn.search(null,"solr","?q=*:*");
				checkTrue(cn.getLatestRequestUrl(),"search(...) returns a ObjectList object", response != null);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
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
	
	
	/**
	 * This test searches for identifiers containing the representative unicode
	 * characters, and is successful if ObjectList is returned (can be empty).
	 * The goal is to rule out encoding issues with the request and processing.
	 * It does NOT test that an object with those characters are found.
	 */
	@Test
	public void testSearch_Solr_unicodeTests() {
		
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
				if (line.startsWith("common-") || line.startsWith("query-"))
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
		
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSearch_Solr_unicodeTests(...) vs. node: " + currentUrl);
			
			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );
			
			for (int i=0; i<unicodeString.size(); i++) 
			{
				String status = "OK   ";

				//   String unicode = unicodeString.get(j);
				System.out.println();
				System.out.println(i + "    unicode String:: " + unicodeString.get(i));
//				String idSubStringEscaped =  escapedString.get(i);

				try {
					D1Url query = new D1Url("a","b");
					String wildcardPattern = unicodeIdPrefix + "*" + unicodeString.get(i) + "*";
					String solrEscapedWildcardPattern = ClientUtils.escapeQueryChars(wildcardPattern);
					query.addNonEmptyParamPair("q", "id:" + solrEscapedWildcardPattern);

					ObjectList response = cn.search(null,"solr",query);
					checkTrue(cn.getLatestRequestUrl(),"search(...) should return an ObjectList", response != null);
					
//					checkTrue(cn.getLatestRequestUrl(),"search(...) should ")
				}
				catch (IndexOutOfBoundsException e) {
					handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
				}
	    		catch (BaseException e) {
	    			status = "Error";
	    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
	    					e.getDetail_code() + ":: " + e.getDescription());
	    		}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}

				nodeSummary.add("Test " + i + ": " + status + ": " + unicodeString.get(i));
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
	 * Test ID encoding using the same resolve procedure
	 * (it tests create, get, getsystemMetadata, and resolve)
	 * @throws ServiceFailure 
	 */
	@Ignore("test not adapted for v1.0.0")
	@Test
	public void test_IdEncoding() throws ServiceFailure 
	{
		setupClientSubject("testRightsHolder");
		Iterator<Node> it = getCoordinatingNodeIterator();
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
		
		Vector<String> testIDs  = new Vector<String>();	
		Vector<String> encodedIDs  = new Vector<String>();	
//		printHeader("test_IdEncoding");
			
		for (int j=0; j<testPIDEncodingStrings.size(); j++) 
		{
			String idStringPrefix = "cn:testid:" + ExampleUtilities.generateIdentifier() + ":";
			testIDs.add(idStringPrefix + testPIDEncodingStrings.get(j));
			encodedIDs.add(idStringPrefix + encodedPIDStrings.get(j));
		}
		resolveRunner(testIDs,encodedIDs);
	}

	/*
	 * a general procedure for creating and testing the return from resolve for different
	 * identifier strings
	 */
	private void resolveRunner(Vector<String> ids, Vector<String> encodedIDs) throws ServiceFailure {

		Vector<String> summaryReport = new Vector<String>();
		
		CNode cn = D1Client.getCN();

		MNode mn = D1Client.getMN(mnBaseUrl);

		Session token = null; //mn.login(principal, "kepler");
		// run tests for each identifier
		Identifier pid = new Identifier();
		String status;
		String message;
		for (int j=0; j<ids.size(); j++) 
		{
			status = "OK   ";
			message = "-";
			try {
				String idString = ids.get(j);
				String encodedID = encodedIDs.get(j);
				pid.setValue(idString);
				System.out.println();
				System.out.println("*** ID string:  " + idString);

				//insert a data file
				Identifier rPid = createPublicTestObject(mn, idString);

				ObjectLocationList oll = cn.resolve(token, rPid);
				for (ObjectLocation ol : oll.getObjectLocationList()) {
					System.out.println("   === Location: " + ol.getNodeIdentifier().getValue()
							+ " (" + ol.getUrl() + ")");
					if (ol.getUrl().contains(encodedID))
						checkTrue(cn.getLatestRequestUrl(),"objectLocationList should contain the encodedID",true);
					else
					{
						status = "Fail";
						message = "encodedID not found: " + encodedID;
						handleFail(cn.getLatestRequestUrl()," encodedID not found: " + encodedID);
					}
				}
			}
			catch (Exception e) {

				status  = "error";
				message = e.getMessage();
				handleFail(cn.getLatestRequestUrl(),"error in resolveRunner: " + e.getMessage());
			}
			summaryReport.add(j + " " + status + " " + ids.get(j) + "  " + message);
		}

		System.out.println("*********  Test Summary ************");

		for (int j =0; j<summaryReport.size(); j++)
		{
			System.out.println(summaryReport.get(j));
		}
		System.out.println();
	}

	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods";
	}
	
}
