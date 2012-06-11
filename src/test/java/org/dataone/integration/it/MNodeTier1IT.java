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
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import org.dataone.client.D1Client;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier1IT extends ContextAwareTestCaseDataone  {


    private  String format_text_csv = "text/csv";
    private  String currentUrl;
    private Vector<String> unicodeStringV;
	private Vector<String> escapedStringV;

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 1 API methods";
	}
	
	@Before
	public void setupIdentifierVectors() {
		if (unicodeStringV == null) {
			// get identifiers to check with
			unicodeStringV = new Vector<String>();
			escapedStringV = new Vector<String>();
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
						unicodeStringV.add(temp[0]);
						escapedStringV.add(temp[1]);	
					}
				}
			} finally {
				s.close();
			}
		}
	}
	
	@Test
	public void testPing() {
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			
			
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testPing() vs. node: " + currentUrl);
			
			try {
//				Assume.assumeTrue(APITestUtils.isTierImplemented(mn, "Tier5"));
				Date pingDate = mn.ping();
				
				checkTrue(mn.getLatestRequestUrl(),"ping should return a valid date", pingDate != null);
				// other invalid dates will be thrown as IOExceptions cast to ServiceFailures
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
	
    /**
     * Tests that getLogRecords() implements access restriction properly, testing
     * the negative case - where client is not a CN.
     * 
     */
    @Test
    public void testGetLogRecords_AccessRestriction()
    {
    	
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);   			
    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords without a client certificate" +
    					"should return a Log datatype or NotAuthorized", eventLog != null);
    			
    			//check that the identifiers in the log entries are all public read
    			if (eventLog.getLogEntryList().size() > 0) {
    				LogEntry currentEntry = null;
    				try {
    					for (LogEntry le : eventLog.getLogEntryList()) {
    						currentEntry = le;
    						mn.describe(le.getIdentifier());
    					}
    				}
    				catch (NotAuthorized e) {
    					handleFail(mn.getLatestRequestUrl(), String.format(
    							"The returned log should not contain log entries which " +
    							"are not publicly available.  Got entry %s for identifier %s",
    							currentEntry.getEntryId(),
    							currentEntry.getIdentifier().getValue())
    							);
    				}
    			}
    			
    			
    		}
    		catch (NotAuthorized e) {
    			; // a valid response, where access is restricted to CNs
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
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
     */
    @Test
    public void testGetLogRecords()
    {
    	//TODO: change to use a testCNAdmin certificate
//    	setupClientSubject("cn-sandbox-unm-1");
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);   			
    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords should return a log datatype", eventLog != null);
    			
    			Date fromDate = new Date();
    			Thread.sleep(1000);
    			Date toDate = new Date();
    			eventLog = mn.getLogRecords(fromDate, toDate, Event.READ, "pidFilter" ,0, 10);
    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords(<parameters>) returns a Log", eventLog != null);
    		}
    		catch (NotAuthorized e) {
    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
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
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
    @Test
    public void testGetLogRecords_Slicing()
    {
    	// TODO: change to testCnAdmin subject when obtained
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testGetLogRecords_Slicing(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);   			
    			// make sure the count is accurate
    			checkEquals(mn.getLatestRequestUrl(),"getLogRecords().getCount() should" +
    					" equal the number of returned LogEntries", 
    					String.valueOf(eventLog.getCount()),
    					String.valueOf(eventLog.getLogEntryList().size())
    					);
    			
    			
    			// heuristic tests on total attribute
    			checkTrue(mn.getLatestRequestUrl(),"total attribute should be >= count",
    					eventLog.getTotal() >= eventLog.getCount()
    					);
    			checkTrue(mn.getLatestRequestUrl(), "total attribute should be >= the number of items returned",
    					eventLog.getTotal() >= eventLog.sizeLogEntryList()
    					);

    			// test that one can limit the count
    			int halfCount = eventLog.getLogEntryList().size() / 2; // rounds down
    			eventLog = mn.getLogRecords(null, null, null, null, null, 0, halfCount);
    			checkEquals(mn.getLatestRequestUrl(), "Should be able to limit " +
    					"the number of returned entries using 'count' parameter.",
    					String.valueOf(eventLog.getLogEntryList().size()),
    					String.valueOf(halfCount));
    			
    			// TODO:  test that 'start' parameter does what it says

    			// TODO: paging test
    			
    			
    		}
    		catch (NotAuthorized e) {
    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
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
     * Tier 1 MNs might only have READ events, so will get the log records from
     * a given period and if only one type, filter for a different one an expect 
     * zero returned.  If 2 types, just expect fewer records.
     * Must be careful to check that all the records requested are returned.
     */
    @Test
    public void testGetLogRecords_eventFiltering()
    {
    	// TODO: change to testCnAdmin subject when obtained
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetLogRecords_eventFiltering() vs. node: " + currentUrl);

    		try {
    			Date toDate = new Date();

    			// using the paged-implementation to make sure we get them all.
    			Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);

    			if (entries.getCount() == 0) {
    				// try to create a log event
    				// if it can't it will throw a TestIterationEndingException
    				Identifier pid = procurePublicReadableTestObject(mn,null);
    				mn.get(pid);
    				toDate = new Date();
    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    			}

    			Event targetType = entries.getLogEntry(0).getEvent();
    			Event otherType = null;

    			for (LogEntry le : entries.getLogEntryList()) {
    				if ( ! le.getEvent().equals(targetType) ) {
    					otherType = le.getEvent();
    					break;
    				}
    			}

    			if (otherType == null) {
    				if (targetType.equals(Event.READ)) {
    					entries = mn.getLogRecords(null, null, toDate, Event.CREATE, null, 0, 0);
    					checkEquals(mn.getLatestRequestUrl(),"Log contains only READ events, " +
    							"so should get 0 CREATE events", String.valueOf(entries.getTotal()),"0");
    				} else {
    					entries = mn.getLogRecords(null, null, toDate, Event.READ, null, 0, 0);
    					checkEquals(mn.getLatestRequestUrl(),"Log contains only " + targetType + " events, " +
    							"so should get 0 READ events",String.valueOf(entries.getTotal()),"0");
    				}
    			} else {
    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate ,targetType, null, null, null);
    				boolean oneTypeOnly = true;
    				Event unfilteredType = null;
    				for (LogEntry le: entries.getLogEntryList()) {
    					if (!le.getEvent().equals(targetType)) {
    						oneTypeOnly = false;
    						unfilteredType = le.getEvent();
    						break;
    					}
    				}
    				checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should contain only " +
    						"logs of type " + targetType.xmlValue() + ". Got " + unfilteredType, oneTypeOnly);
    			}
    		}
    		catch (NotAuthorized e) {
    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
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
     * Test that pidFilter only returns objects starting with the given string
     * Want to find a negative case and to make sure it is filtered out when the
     * filter is applied.
     */
    @Test
    public void testGetLogRecords_pidFiltering()
    {
    	// TODO: change to testCnAdmin subject when obtained
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetLogRecords_pidFiltering() vs. node: " + currentUrl);

    		try {
    			Date t0 = new Date();
    			Date toDate = t0;
    			Date fromDate = t0;

    			Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, 0, 0);

    			if (entries.getTotal() == 0) {
    				// try to create a log event
    				// if it can't it will throw a TestIterationEndingException
    				Identifier pid = procurePublicReadableTestObject(mn,null);
    				mn.get(pid);
    				toDate = new Date();
    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    			}

    			// should have at least one log entry at this point
    			if (entries.sizeLogEntryList() > 0) {
    				Identifier targetIdentifier = entries.getLogEntry(0).getIdentifier();
    				Identifier otherIdentifier = null;

    				for (LogEntry le : entries.getLogEntryList()) {
    					if (! le.getIdentifier().equals(targetIdentifier) ) {
    						otherIdentifier = le.getIdentifier();
    						break;
    					}
    				}

    				if (otherIdentifier == null) {
    					// create a new target that is non existent
    					otherIdentifier = targetIdentifier;
    					targetIdentifier = D1TypeBuilder.buildIdentifier(targetIdentifier.getValue()
    							+ new Date().getTime());

    					entries = mn.getLogRecords(null, null, t0, 
    							null, targetIdentifier.getValue(), 0, 0);
    					checkEquals(mn.getLatestRequestUrl(),"Log should be empty for the derived identifier pattern " +
    							targetIdentifier.getValue(),String.valueOf(entries.getTotal()),"0");

    				} 
    				else {
    					entries = mn.getLogRecords(null, null, toDate, 
    							null, targetIdentifier.getValue(), null, null);
    					boolean oneTypeOnly = true;

    					for (LogEntry le: entries.getLogEntryList()) {
    						if (!le.getIdentifier().equals(targetIdentifier)) {
    							oneTypeOnly = false;
    							break;
    						}
    					}
    				 
    				//    				checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should " +
    				//    						"contain only entries for the target identifier: " + targetIdentifier.getValue(),
    				//    						oneTypeOnly);
    				checkTrue(mn.getLatestRequestUrl(), "The optional pidFilter parameter is not filtering log records. " +
    						"The log would otherwise contain only entries for the target identifier: " + targetIdentifier.getValue(),
    						oneTypeOnly);
    				}
    			}
    			else {
					handleFail(mn.getLatestRequestUrl(), "After successfully reading an object, should " +
							"have at least one log record.  Got zero");
				}
    		}
    		catch (NotAuthorized e) {
    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
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



    @Test
    public void testGetLogRecords_DateFiltering()
    {
    	// TODO: change to testCnAdmin subject when obtained
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testGetLogRecords_DateFiltering(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
    			
    			if (eventLog.getLogEntryList().size() == 0) {
    				
    				// read an existing object
    				try {
    					String objectIdentifier = "TierTesting:" + 
    					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
    					 	":Public_READ" + testObjectSeriesSuffix;
    					Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
    					InputStream is = mn.get(null, id);
    					is.close();
    					Thread.sleep(1000); // just in case...
    					eventLog = mn.getLogRecords(null, null, null, null, null, null);
    				}
    				catch (TestIterationEndingException e) {
    					;  // 
    				}
    			}
    				
    			if (eventLog.getLogEntryList().size() == 0) {
    				// still zero?  something's probably wrong
    				handleFail(mn.getLatestRequestUrl(),"the event log contains no entries after trying to read an object");
    				
    			} else {
    				// try to find log entries with different dates, should be quick...
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
    					handleFail(mn.getLatestRequestUrl(),"could not find 2 objects with different dateLogged times");
    				} 
    				else {

    					// call with a fromDate
    					eventLog = mn.getLogRecords(fromDate, null, null, null, null, null);

    					for (LogEntry le : eventLog.getLogEntryList()) {
    						if (le.getEntryId().equals(excludedEntry.getEntryId())) {
    							handleFail(mn.getLatestRequestUrl(),"entryID " + excludedEntry.getEntryId() +
    									" at " + excludedEntry.getDateLogged() + 
    									" should not be in the event log where fromDate set to " + fromDate);
    							break;
    						}
    					}
    				}
    			} 
    		} 
    		catch (NotAuthorized e) {
    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
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
    
    
    
    @Test
    public void testGetCapabilities() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);

    		try {
    			Node node = mn.getCapabilities();
    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
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
    
    /**
     * Tests that at least one of the node contacts is RFC2253 compatible, 
     * meaning that it could be represented by a CILogon issued certificate
     */
    @Test
    public void testGetCapabilities_HasCompatibleNodeContact() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);

    		try {
    			Node node = mn.getCapabilities();
    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
    		
    			List<Subject> contacts = node.getContactSubjectList();
    			boolean found = false;
    			if (contacts != null) {
    				for (Subject s : contacts) {
    					try {
    						String standardizedName = CertificateManager.getInstance().standardizeDN(s.getValue());
    						found = true;
    					} catch (IllegalArgumentException e) {
    						; // this can happen legally, but means that it is not actionable
    					}
    				}
    			}
    			checkTrue(mn.getLatestRequestUrl(),"the node should have at least one contactSubject that conforms to RFC2253.", found);
    			
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
    
    /**
     * Tests that the nodeReference of the node is in the proper urn format.
     */
    @Test
    public void testGetCapabilities_NodeIdentityValidFormat() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);

    		try {
    			Node node = mn.getCapabilities();
    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
    		
    			NodeReference nodeRef = node.getIdentifier();
    			checkTrue(mn.getLatestRequestUrl(),"the node identifier should conform to specification 'urn:node:[\\w_]{2,23}'",
    					nodeRef.getValue().matches("^urn:node:[\\w_]{2,23}"));
    			
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
    
    /**
     * Tests the parameterless and parameterized listObject methods for proper returns.
     */
    @Test
    public void testListObjects() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(mn);
    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
    			
    			Date fromDate = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date toDate = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
    			Boolean replicaStatus = true;
				ol = mn.listObjects(null, fromDate, toDate, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(mn.getLatestRequestUrl(),"listObjects(<parameters>) returns an ObjectList", ol != null);
    		} 
    		catch (TestIterationEndingException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getMessage() + ":: cause: "
    					+ e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
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
   
    
    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
    @Test
    public void testListObjects_Slicing()
    {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testListObjects_Slicing(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			ObjectList ol = mn.listObjects();
    			// make sure the count is accurate
    			checkEquals(mn.getLatestRequestUrl(),"listObjects().getCount() should" +
    					" equal the number of returned ObjectInfos", 
    					String.valueOf(ol.getCount()),
    					String.valueOf(ol.getObjectInfoList().size())
    					);
    			
    			
    			// heuristic test on total attribute
    			checkTrue(mn.getLatestRequestUrl(),"total attribute should be >= count",
    					ol.getTotal() >= ol.getCount()
    					);

    			// test that one can limit the count
    			int halfCount = ol.getObjectInfoList().size() / 2; // rounds down
    			ol = mn.listObjects(null, null, null, null, null, 0, halfCount);
    			checkEquals(mn.getLatestRequestUrl(), "Should be able to limit " +
    					"the number of returned ObjectInfos using 'count' parameter.",
    					String.valueOf(ol.getObjectInfoList().size()),
    					String.valueOf(halfCount));
    			
    			// TODO:  test that 'start' parameter does what it says

    			// TODO: paging test
    			
    			
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
     * Tests that the fromDate parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than fromDate.
     */
    @Test
    public void testListObjects_FromDateTest() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(mn);
    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
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
   				ol = mn.listObjects(null, fromDate, null, null, null, null, null);

   				if (ol.getObjectInfoList() != null) {
   					// at least some objects returned
   					// so we have to check that the excluded object was excluded
   					for (ObjectInfo oi: ol.getObjectInfoList()) {
   						if (oi.getIdentifier().equals(excludedObjectInfo.getIdentifier())) {
   							handleFail(mn.getLatestRequestUrl(),String.format("identifier %s with sysMetaModified date of '%s'" +
   									" should not be in the objectList where 'fromDate' parameter set to '%s'", 
   									excludedObjectInfo.getIdentifier().getValue(),
   									DateTimeMarshaller.serializeDateToUTC(excludedObjectInfo.getDateSysMetadataModified()),
   									DateTimeMarshaller.serializeDateToUTC(fromDate)
   									));
   						}
   					}
   				} // else the excluded object was definitely excluded - test passes

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
    
    
    /**
     * Tests that the formatID parameter successfully filters records by
     * the given formatId
     */
    @Test
    public void testListObjects_FormatIdFilteringTest() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = mn.listObjects(null, null, null, null, null, 0, 0);
    			if (ol.getTotal() == 0) {
    				// procure also creates!
    				ol = procureObjectList(mn);
    			} else {
    				ol = APITestUtils.pagedListObjects(mn, null, null, null, null, 0, ol.getTotal());
    			}
    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
    			if (ol.getTotal() == 0)
    				throw new TestIterationEndingException("no objects found in listObjects");
    			ObjectFormatIdentifier excludedFormat = ol.getObjectInfo(0).getFormatId();
    			for (ObjectInfo oi : ol.getObjectInfoList()) {
    				if (!oi.getFormatId().equals(excludedFormat)) {
    					ObjectList ol2 = mn.listObjects(null, null, null, oi.getFormatId(),
    	   						null, null, null);
    	   				checkTrue(mn.getLatestRequestUrl(),"objectList filtered by " + oi.getFormatId().getValue() +
    	   				      " should contain fewer objects than unfiltered", 
    	   				      ol2.getTotal() <  ol.getTotal()
    	   				      );
    	   				break;
    				}
    			}
    			
   				// call listObjects with a fake format
   				ol = mn.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("fake_format"),
   						null, null, null);
   				if (ol.getTotal() != 0) {
   					handleFail(mn.getLatestRequestUrl(),"filtering the object list by a fake " +
   							"format should return zero objects");
   				}
   				
   				
   				
   				
   				
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
    
    
    
    @Test
    public void testGet() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
 //   			Identifier id = procurePublicReadableTestObject(mn);
    			InputStream is = mn.get(null,id);
    			checkTrue(mn.getLatestRequestUrl(),"get() returns an objectStream", is != null);
    		}
    		catch (TestIterationEndingException e) {
    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against:: " + 
    						e.getMessage());
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testGet_NotFound() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
    			InputStream is = mn.get(null,D1TypeBuilder.buildIdentifier(fakeID));
    			handleFail(mn.getLatestRequestUrl(),"get(fakeID) should not return an objectStream.");
    			is.close();
    		}
    		catch (NotFound nf) {
    			;  // expected outcome
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    /**
     * test getting data with challenging unicode identifiers.  Will try to 
     * differentiate between NotFound and ServiceFailure
     */
	@Test
    public void testGet_IdentifierEncoding() 
    {
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();
		
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGet_IdentifierEncoding() vs. node: " + currentUrl);
			

			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeStringV.size(); j++) 
			{
				String status = "OK   ";
				
				log.info("");
				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
				
				
				try {
					
					InputStream data = mn.get(null, D1TypeBuilder.buildIdentifier(idString));
					handleFail(mn.getLatestRequestUrl(), "get() against the fake identifier (" +
							idStringEscaped + ") should throw NotFound");
					data.close();
					status = "Error";
				}
				catch (NotFound nf) {
					;
				}
				catch (ServiceFailure e) {
					if (e.getDescription().contains("Providing message body")) {
						if (e.getDescription().contains("404: NotFound:")) {
							// acceptable result
							;
						}
					} 
					else {
						status = String.format("Error:: %s: %s: %s",
								e.getClass().getSimpleName(),
								e.getDetail_code(),
								first100Characters(e.getDescription()));
					}
				}
				catch (BaseException e) {
					status = String.format("Error:: %s: %s: %s",
							e.getClass().getSimpleName(),
							e.getDetail_code(),
							first100Characters(e.getDescription()));
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					status = String.format("Error:: %s: %s",
							e.getClass().getName(),
							first100Characters(e.getMessage()));
				}

				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
			}
			
			for (String result : nodeSummary) {
				if (result.contains("Error")) {
					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
					break;
				}
			}
		}
    }
    
    
    @Test
    public void testGetSystemMetadata() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
    		
    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(mn);
    			SystemMetadata smd = mn.getSystemMetadata(null,id);
    			checkTrue(mn.getLatestRequestUrl(),"getSystemMetadata() returns a SystemMetadata object", smd != null);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
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

    
    @Test
    public void testGetSystemMetadata_NotFound() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);

    		try {
    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
    			SystemMetadata smd = mn.getSystemMetadata(null,D1TypeBuilder.buildIdentifier(fakeID));
    			handleFail(mn.getLatestRequestUrl(),"getSystemMetadata(fakeID) should not throw dataone NotFound.");
    		}
    		catch (NotFound nf) {
    			;  // expected outcome
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
       			

	@Test
    public void testGetSystemMetadata_IdentifierEncoding() 
    {
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();
		printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
	
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetSystemMetadata_IdentifierEncoding() vs. node: " + currentUrl);
			

			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeStringV.size(); j++) 
			{
				String status = "OK   ";
				
				log.info("");
				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
				
				
				try {
					mn.getSystemMetadata(null, D1TypeBuilder.buildIdentifier(idString));
					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
							idStringEscaped + ") should throw NotFound");

					status = "Error";
				}
				catch (NotFound nf) {
					;
				}
				catch (ServiceFailure e) {
					if (e.getDescription().contains("Providing message body")) {
						if (e.getDescription().contains("404: NotFound:")) {
							// acceptable result
							;
						}
					} 
					else {
						status = String.format("Error:: %s: %s: %s",
								e.getClass().getSimpleName(),
								e.getDetail_code(),
								first100Characters(e.getDescription()));
					}
				}
				catch (BaseException e) {
					status = String.format("Error:: %s: %s: %s",
							e.getClass().getSimpleName(),
							e.getDetail_code(),
							first100Characters(e.getDescription()));
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					status = String.format("Error:: %s: %s",
							e.getClass().getName(),
							first100Characters(e.getMessage()));
				}

				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
			}
			
			for (String result : nodeSummary) {
				if (result.contains("Error")) {
					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
					break;
				}
			}
	    }
    }
					
					

    @Test
    public void testDescribe() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl); 
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testDescribe() vs. node: " + currentUrl);
		
    		try {
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(mn);
    			DescribeResponse dr = mn.describe(null,id);
    			checkTrue(mn.getLatestRequestUrl(),"describe() returns a DescribeResponse object", dr != null);	
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
 
    
    @Test
    public void testDescribe_NotFound() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testDescribe() vs. node: " + currentUrl);

    		try {
    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
    			mn.describe(null,D1TypeBuilder.buildIdentifier(fakeID));
    			handleFail(mn.getLatestRequestUrl(),"describe(fakeID) should return a d1 NotFound in the header.");
    			
    		}
    		catch (NotFound nf) {
    			;  // expected outcome
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    

	@Test
	public void testDescribe_IdentifierEncoding() 
	{
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testDescribe_IdentifierEncoding() vs. node: " + currentUrl);


			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeStringV.size(); j++) 
			{
				String status = "OK   ";

				log.info("");
				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);




				try {	
					mn.describe(null, D1TypeBuilder.buildIdentifier(idString));
					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
							idStringEscaped + ") should throw NotFound");

					status = "Error";
				}
				catch (NotFound nf) {
					;
				}
				catch (ServiceFailure e) {
					if (e.getDescription().contains("Providing message body")) {
						if (e.getDescription().contains("404: NotFound:")) {
							// acceptable result
							;
						}
					} 
					else {
						status = String.format("Error:: %s: %s: %s",
								e.getClass().getSimpleName(),
								e.getDetail_code(),
								first100Characters(e.getDescription()));
					}
				}
				catch (BaseException e) {
					status = String.format("Error:: %s: %s: %s",
							e.getClass().getSimpleName(),
							e.getDetail_code(),
							first100Characters(e.getDescription()));
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					status = String.format("Error:: %s: %s",
							e.getClass().getName(),
							first100Characters(e.getMessage()));
				}

				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
			}
			
			for (String result : nodeSummary) {
				if (result.contains("Error")) {
					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
					break;
				}
			}
	    }
	}
					

    
    @Test
    public void testGetChecksum() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);

    		try {   
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    			Identifier id = procurePublicReadableTestObject(mn);
    			Checksum cs = mn.getChecksum(null,id,CHECKSUM_ALGORITHM);
    			checkTrue(mn.getLatestRequestUrl(),"getChecksum() returns a Checksum object", cs != null);
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
    	

    @Test
    public void testGetChecksum_NotFound() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);

    		try {
    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
    			mn.getChecksum(null,D1TypeBuilder.buildIdentifier(fakeID), null);
    			handleFail(mn.getLatestRequestUrl(),"getChecksum(fakeID) should return a NotFound");
    		}
    		catch (NotFound nf) {
    			;  // expected outcome
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    
	@Test
	public void testGetChecksum_IdentifierEncoding() 
	{
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetChecksum_IdentifierEncoding() vs. node: " + currentUrl);


			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeStringV.size(); j++) 
			{
				String status = "OK   ";

				log.info("");
				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);		

				try {
					mn.getChecksum(null, D1TypeBuilder.buildIdentifier(idString), null);
					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
							idStringEscaped + ") should throw NotFound");

					status = "Error";
				}
				catch (NotFound nf) {
					;
				}
				catch (ServiceFailure e) {
					if (e.getDescription().contains("Providing message body")) {
						if (e.getDescription().contains("404: NotFound:")) {
							// acceptable result
							;
						}
					} 
					else {
						status = String.format("Error:: %s: %s: %s",
								e.getClass().getSimpleName(),
								e.getDetail_code(),
								first100Characters(e.getDescription()));
					}
				}
				catch (BaseException e) {
					status = String.format("Error:: %s: %s: %s",
							e.getClass().getSimpleName(),
							e.getDetail_code(),
							first100Characters(e.getDescription()));
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					status = String.format("Error:: %s: %s",
							e.getClass().getName(),
							first100Characters(e.getMessage()));
				}

				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
			}
			
			for (String result : nodeSummary) {
				if (result.contains("Error")) {
					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
					break;
				}
			}
		}
	}


    
    @Test
    public void testSynchronizationFailed_NoCert() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testSynchronizationFailed() vs. node: " + currentUrl);
 		
    		try {
    			
    			String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//				Identifier id = procurePublicReadableTestObject(mn);
    			SynchronizationFailed sf = new SynchronizationFailed("0","a message",id.getValue(),null);
    			System.out.println(sf.serialize(SynchronizationFailed.FMT_XML));
    			mn.synchronizationFailed(null, 
    					new SynchronizationFailed("0","a message",id.getValue(),null));
    			checkTrue(mn.getLatestRequestUrl(),"synchronizationFailed() does not throw exception", true);
    		}
    		catch (NotAuthorized e) {
    			; // this is an acceptable (and preferrable) outcome for calling without a client cert.
    		}
    		catch (IndexOutOfBoundsException e) {
    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ":: " + 
    					e.getDetail_code() + " " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
 //TODO: refactor getReplica tests so Tier1 nodes are treated differently - make sure 
 // the getReplica request is logged differently
    // Tier2 nodes and above can't be tested
    
    
	/**
	 *  Test MNReplication.getReplica() functionality.  This tests the normal usage
	 *  where the caller is a MemberNode. Other callers should fail.
	 */
//	@Ignore("cannot test a passing situation in standalone mode")
//	@Test
	public void testGetReplica() {

		setupClientSubject("testMN");

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
	
	
	/**
	 *  Test MNReplication.getReplica() functionality.  This tests for expected 
	 *  exception when a non-MemberNode client calls the method.
	 */
	// TODO: is this doable in stand-alone mode?
	
//	@Test
	public void testGetReplica_ValidCertificate_NotMN() {

		setupClientSubject("testRightsHolder");

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica_AuthenticateITKUser() vs. node: " + currentUrl);

			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
				InputStream is = mn.getReplica(null, pid);
				handleFail(mn.getLatestRequestUrl(),"with non-Node client certificate, getReplica() should throw exception");
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
    		}
			catch (NotAuthorized e) {
				// expected behavior
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
	
	
	/**
	 *  Test MNReplication.getReplica() functionality.  Normal usage is the caller
	 *  being another MemberNode.  Others should fail.  This tests the latter case.   
	 */
//	@Test
	public void testGetReplica_NoCertificate() {

		setupClientSubject_NoCert();

		Iterator<Node> it = getMemberNodeIterator();  	

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica_NoCert() vs. node: " + currentUrl);

			try {
				String objectIdentifier = "TierTesting:" + 
					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
					 	":Public_READ" + testObjectSeriesSuffix;
				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
				InputStream is = mn.getReplica(null, pid);
				handleFail(mn.getLatestRequestUrl(),"with no client certificate, getReplica() should throw exception");
			}
			catch (IndexOutOfBoundsException e) {
    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
    		}
			catch (InvalidToken e) {
				// expected behavior
			}
			catch (NotAuthorized e) {
				// also expected behavior
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
	

//    @Test
    public void testGetReplica_NotFound() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetReplica() vs. node: " + currentUrl);

    		try {
    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
    			InputStream is = mn.get(null,D1TypeBuilder.buildIdentifier(fakeID));
    			handleFail(mn.getLatestRequestUrl(),"getReplica(fakeID) should not return an objectStream.");
    			is.close();
    		}
    		catch (NotFound nf) {
    			;  // expected outcome
    		}
    		catch (BaseException e) {
    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    

//	@Test
	public void testGetReplica_IdentifierEncoding() 
	{
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();

		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testGetReplica_IdentifierEncoding() vs. node: " + currentUrl);


			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );

			printTestHeader("  Node:: " + currentUrl);

			for (int j=0; j<unicodeStringV.size(); j++) 
			{
				String status = "OK   ";

				log.info("");
				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);


				try {
					mn.getReplica(null, D1TypeBuilder.buildIdentifier(idString));
					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
							idStringEscaped + ") should throw NotFound");

					status = "Error";
				}
				catch (NotFound nf) {
					;
				}
				catch (ServiceFailure e) {
					if (e.getDescription().contains("Providing message body")) {
						if (e.getDescription().contains("404: NotFound:")) {
							// acceptable result
							;
						}
					} 
					else {
						status = String.format("Error:: %s: %s: %s",
								e.getClass().getSimpleName(),
								e.getDetail_code(),
								first100Characters(e.getDescription()));
					}
				}
				catch (BaseException e) {
					status = String.format("Error:: %s: %s: %s",
							e.getClass().getSimpleName(),
							e.getDetail_code(),
							first100Characters(e.getDescription()));
				}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					status = String.format("Error:: %s: %s",
							e.getClass().getName(),
							first100Characters(e.getMessage()));
				}

				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
			}
			
			for (String result : nodeSummary) {
				if (result.contains("Error")) {
					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
					break;
				}
			}
		}
	}

	
	private String first100Characters(String s) {
		if (s.length() <= 100) 
			return s;
		
		return s.substring(0, 100) + "...";
		
	}

	private String tablifyResults(Vector<String> results)
	{
		StringBuffer table = new StringBuffer("Failed 1 or more identifier encoding tests");
		for (String result: results) {
			table.append(result);	
			table.append("\n    ");
		}
		return table.toString();		 
	}
}
