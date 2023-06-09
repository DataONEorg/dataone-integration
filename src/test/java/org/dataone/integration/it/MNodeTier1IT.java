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
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v1.itk.DataPackage;
import org.dataone.client.v1.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
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
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.util.DateTimeMarshaller;

import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.ResourceMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier1IT { //extends ContextAwareTestCaseDataone  {

//
//    private  String format_text_csv = "text/csv";
//    private  String currentUrl;
//    private static Vector<String> unicodeStringV;
//	private static Vector<String> escapedStringV;
//
//	@Override
//	protected String getTestDescription() {
//		return "Test Case that runs through the Member Node Tier 1 API methods";
//	}
//
//	@Before
//	public void setupIdentifierVectors() {
//		if (unicodeStringV == null) {
//			// get identifiers to check with
//			unicodeStringV = new Vector<String>();
//			escapedStringV = new Vector<String>();
//			//   TODO: test against Unicode characters when metacat supports unicode
//			InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
//			//InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
//			Scanner s = new Scanner(is,"UTF-8");
//			String[] temp;
//			int c = 0;
//			try{
//				while (s.hasNextLine()) {
//					String line = s.nextLine();
//					if (line.startsWith("common-") || line.startsWith("path-"))
//					{
//						if (line.contains("supplementary"))
//							continue;
//
//						temp = line.split("\t");
//
//						// identifiers can't contain spaces by default
//						if (temp[0].contains(" "))
//							continue;
//
//						log.info(c++ + "   " + line);
//						unicodeStringV.add(temp[0]);
//						escapedStringV.add(temp[1]);
//					}
//				}
//			} finally {
//				s.close();
//			}
//		}
//	}
//
//	@Test
//	public void testPing() {
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//		while (it.hasNext()) {
//
//
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testPing() vs. node: " + currentUrl);
//
//			try {
////				Assume.assumeTrue(APITestUtils.isTierImplemented(mn, "Tier5"));
//				Date localNow = new Date();
//				Date pingDate = mn.ping();
//
//				checkTrue(mn.getLatestRequestUrl(),"ping should return a valid date", pingDate != null);
//				// other invalid dates will be thrown as IOExceptions cast to ServiceFailures
//
//				checkTrue(mn.getLatestRequestUrl(), "returned date should be within 1 minute of time measured on test machine",
//						pingDate.getTime() - localNow.getTime() < 1000 * 60  &&
//						localNow.getTime() - pingDate.getTime() > -1000 * 60);
//
//			}
//			catch (BaseException e) {
//				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//						e.getDetail_code() + ":: " + e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//    /**
//     * Tests that getLogRecords() implements access restriction properly, testing
//     * the negative case - where client is not a CN and is public.
//     *
//     */
////    @Test
//    public void testGetLogRecords_AccessRestriction()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = new MNode(currentUrl);
//    		printTestHeader("testGetLogRecords_AccessRestriction(...) vs. node: " + currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//
//    		try {
//    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
//    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords without a client certificate" +
//    					"should return a Log datatype or NotAuthorized", eventLog != null);
//
//    			//check that the identifiers in the log entries are all public read
//    			if (eventLog.getLogEntryList().size() > 0) {
//    				LogEntry currentEntry = null;
//    				try {
//    					for (LogEntry le : eventLog.getLogEntryList()) {
//    						currentEntry = le;
//    						try {
//    							mn.describe(le.getIdentifier());
//    						}
//    						catch (NotFound e) {
//    	    	    			; // a semi-valid repsonse.  Sometimes logged objects have been deleted.
//    	    	    		}
//    					}
//    				}
//    				catch (NotAuthorized e) {
//    					handleFail(mn.getLatestRequestUrl(), String.format(
//    							"The returned log should not contain log entries which " +
//    							"are not publicly available.  Got entry %s for identifier %s",
//    							currentEntry.getEntryId(),
//    							currentEntry.getIdentifier().getValue())
//    							);
//    				}
//
//    			}
//
//
//    		}
//    		catch (NotAuthorized e) {
//    			; // a valid response, where access is restricted to CNs
//    		}
//
//
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
//     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
//     */
////    @Test
//    public void testGetLogRecords()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	//TODO: change to use a testCNAdmin certificate
//    	String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnStageUNM1");
//    	setupClientSubject(cnSubject);
////    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = new MNode(currentUrl);
//    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//
//    		try {
//    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
//    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords should return a log datatype", eventLog != null);
//
//    			Date fromDate = new Date();
//    			Thread.sleep(1000);
//    			Date toDate = new Date();
//    			eventLog = mn.getLogRecords(fromDate, toDate, Event.READ, "pidFilter" ,0, 10);
//    			checkTrue(mn.getLatestRequestUrl(),"getLogRecords(<parameters>) returns a Log", eventLog != null);
//    		}
//    		catch (NotAuthorized e) {
//    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
//    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Tests that count and start parameters are functioning, and getCount() and getTotal()
//     * are reasonable values.
//     */
////    @Test
//    public void testGetLogRecords_Slicing()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	// TODO: change to testCnAdmin subject when obtained
//    	String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnStageUNM1");
//    	setupClientSubject(cnSubject);
////    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = new MNode(currentUrl);
//    		printTestHeader("testGetLogRecords_Slicing(...) vs. node: " + currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//
//    		try {
//    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
//
//    			StringBuffer sb = new StringBuffer();
//    			int i = 0;
//    			if (eventLog.getCount() != eventLog.sizeLogEntryList())
//    				sb.append(++i + ". 'count' attribute should equal the number of LogEntry objects returned.  \n");
//
//    			if (eventLog.getTotal() < eventLog.getCount())
//    				sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned Log.  \n");
//
//    			if (eventLog.getTotal() < eventLog.sizeLogEntryList())
//    				sb.append(++i + "'total' attribute should be >= the number of LogEntry objects returned.  \n");
//
//
//    			// test that one can limit the count
//    			int halfCount = eventLog.sizeLogEntryList() / 2; // rounds down
//    			eventLog = mn.getLogRecords(null, null, null, null, 0, halfCount);
//
//    			if (eventLog.sizeLogEntryList() != halfCount)
//    				sb.append(++i + ". should be able to limit the number of returned LogEntry objects using 'count' parameter.");
//
//    			// TODO:  test that 'start' parameter does what it says
//
//    			// TODO: paging test
//
//
//    			if (i > 0) {
//    				handleFail(mn.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
//    			}
//
//    		}
//    		catch (NotAuthorized e) {
//    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
//    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tier 1 MNs might only have READ events, so will get the log records from
//     * a given period and if only one type, filter for a different one an expect
//     * zero returned.  If 2 types, just expect fewer records.
//     * Must be careful to check that all the records requested are returned.
//     */
////    @Test
//    public void testGetLogRecords_eventFiltering()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	// TODO: change to testCnAdmin subject when obtained
//    	String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnStageUNM1");
//    	setupClientSubject(cnSubject);
////    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetLogRecords_eventFiltering() vs. node: " + currentUrl);
//
//    		try {
//    			Date toDate = new Date();
//
//    			// using the paged-implementation to make sure we get them all.
//    			Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
//
//    			if (entries.getCount() == 0) {
//    				// try to create a log event
//    				// if it can't it will throw a TestIterationEndingException
//    				Identifier pid = procurePublicReadableTestObject(mn,null);
//    				mn.get(pid);
//    				toDate = new Date();
//    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
//    			}
//
//    			Event targetType = entries.getLogEntry(0).getEvent();
//    			Event otherType = null;
//
//    			for (LogEntry le : entries.getLogEntryList()) {
//    				if ( ! le.getEvent().equals(targetType) ) {
//    					otherType = le.getEvent();
//    					break;
//    				}
//    			}
//
//    			if (otherType == null) {
//    				if (targetType.equals(Event.READ)) {
//    					entries = mn.getLogRecords(null, null, toDate, Event.CREATE, null, 0, 0);
//    					checkEquals(mn.getLatestRequestUrl(),"Log contains only READ events, " +
//    							"so should get 0 CREATE events", String.valueOf(entries.getTotal()),"0");
//    				} else {
//    					entries = mn.getLogRecords(null, null, toDate, Event.READ, null, 0, 0);
//    					checkEquals(mn.getLatestRequestUrl(),"Log contains only " + targetType + " events, " +
//    							"so should get 0 READ events",String.valueOf(entries.getTotal()),"0");
//    				}
//    			} else {
//    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate ,targetType, null, null, null);
//    				boolean oneTypeOnly = true;
//    				Event unfilteredType = null;
//    				for (LogEntry le: entries.getLogEntryList()) {
//    					if (!le.getEvent().equals(targetType)) {
//    						oneTypeOnly = false;
//    						unfilteredType = le.getEvent();
//    						break;
//    					}
//    				}
//    				checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should contain only " +
//    						"logs of type " + targetType.xmlValue() + ". Got " + unfilteredType, oneTypeOnly);
//    			}
//    		}
//    		catch (NotAuthorized e) {
//    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
//    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Test that pidFilter only returns objects starting with the given string
//     * Want to find a negative case and to make sure it is filtered out when the
//     * filter is applied.
//     */
////    @Test
//    public void testGetLogRecords_pidFiltering()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	// TODO: change to testCnAdmin subject when obtained
//    	String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnStageUNM1");
//    	setupClientSubject(cnSubject);
////    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetLogRecords_pidFiltering() vs. node: " + currentUrl);
//
//    		try {
//    			Date t0 = new Date();
//    			Date toDate = t0;
////    			Date fromDate = t0;
//
//    			Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
//
//    			if (entries.getTotal() == 0) {
//    				// try to create a log event
//    				// if it can't it will throw a TestIterationEndingException
//    				Identifier pid = procurePublicReadableTestObject(mn,null);
//    				mn.get(pid);
//    				toDate = new Date();
//    				entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
//    			}
//
//    			// should have at least one log entry at this point
//    			if (entries.sizeLogEntryList() > 0) {
//    				Identifier targetIdentifier = entries.getLogEntry(0).getIdentifier();
//    				Identifier otherIdentifier = null;
//
//    				for (LogEntry le : entries.getLogEntryList()) {
//    					if (! le.getIdentifier().equals(targetIdentifier) ) {
//    						otherIdentifier = le.getIdentifier();
//    						break;
//    					}
//    				}
//
//    				if (otherIdentifier == null) {
//    					// create a new target that is non existent
//    					otherIdentifier = targetIdentifier;
//    					targetIdentifier = D1TypeBuilder.buildIdentifier(targetIdentifier.getValue()
//    							+ new Date().getTime());
//
//    					entries = mn.getLogRecords(null, null, t0,
//    							null, targetIdentifier.getValue(), 0, 0);
//    					checkEquals(mn.getLatestRequestUrl(),"Log should be empty for the derived identifier pattern " +
//    							targetIdentifier.getValue(),String.valueOf(entries.getTotal()),"0");
//
//    				}
//    				else {
//    					entries = mn.getLogRecords(null, null, toDate,
//    							null, targetIdentifier.getValue(), null, null);
//    					boolean oneTypeOnly = true;
//    					if (entries.sizeLogEntryList() > 0) {
//    						for (LogEntry le: entries.getLogEntryList()) {
//    							if (!le.getIdentifier().equals(targetIdentifier)) {
//    								oneTypeOnly = false;
//    								break;
//    							}
//    						}
////    	    				checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should " +
//    	    				//    						"contain only entries for the target identifier: " + targetIdentifier.getValue(),
//    	    				//    						oneTypeOnly);
//    						checkTrue(mn.getLatestRequestUrl(), "The optional pidFilter parameter is not filtering log records. " +
//    								"The log would otherwise contain only entries for the target identifier: " + targetIdentifier.getValue(),
//    								oneTypeOnly);
//    					} else {
//    						handleFail(mn.getLatestRequestUrl(), "should still get a LogEntry when applying 'pidFilter' parameter");
//    					}
//    				}
//    			}
//    			else {
//					handleFail(mn.getLatestRequestUrl(), "After successfully reading an object, should " +
//							"have at least one log record.  Got zero");
//				}
//    		}
//    		catch (NotAuthorized e) {
//    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
//    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//
////    @Test
//    public void testGetLogRecords_DateFiltering()
//    {
//    	Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
//    	// TODO: change to testCnAdmin subject when obtained
//    	String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnStageUNM1");
//    	setupClientSubject(cnSubject);
////    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = new MNode(currentUrl);
//    		printTestHeader("testGetLogRecords_DateFiltering(...) vs. node: " + currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//
//    		try {
//    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
//
//    			if (eventLog.getLogEntryList().size() == 0) {
//
//    				// read an existing object
//    				try {
//    					String objectIdentifier = "TierTesting:" +
//    					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//    					 	":Public_READ" + testObjectSeriesSuffix;
//    					Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//    					InputStream is = mn.get(null, id);
//    					is.close();
//    					Thread.sleep(1000); // just in case...
//    					eventLog = mn.getLogRecords(null, null, null, null, null, null);
//    				}
//    				catch (TestIterationEndingException e) {
//    					;  //
//    				}
//    			}
//
//    			if (eventLog.getLogEntryList().size() == 0) {
//    				// still zero?  something's probably wrong
//    				handleFail(mn.getLatestRequestUrl(),"the event log contains no entries after trying to read an object");
//
//    			} else {
//    				// try to find log entries with different dates, should be quick...
//    				LogEntry entry0 = eventLog.getLogEntry(0);
//    				Date fromDate = null;
//    				LogEntry excludedEntry = null;
//    				for (LogEntry le: eventLog.getLogEntryList()) {
//    					if (!le.getDateLogged().equals(entry0.getDateLogged())) {
//    						// which is earlier?  can't assume chronological order of the list
//    						if (le.getDateLogged().after(entry0.getDateLogged())) {
//    							fromDate = le.getDateLogged();
//    							excludedEntry = entry0;
//    						} else {
//    							fromDate = entry0.getDateLogged();
//    							excludedEntry = le;
//    						}
//    						break;
//    					}
//    				}
//
//
//    				if (excludedEntry == null) {
//    					handleFail(mn.getLatestRequestUrl(),"could not find 2 objects with different dateLogged times");
//    				}
//    				else {
//
//    					// call with a fromDate
//    					eventLog = mn.getLogRecords(fromDate, null, null, null, null, null);
//
//    					for (LogEntry le : eventLog.getLogEntryList()) {
//    						if (le.getEntryId().equals(excludedEntry.getEntryId())) {
//    							handleFail(mn.getLatestRequestUrl(),"entryID " + excludedEntry.getEntryId() +
//    									" at " + excludedEntry.getDateLogged() +
//    									" should not be in the event log where fromDate set to " + fromDate);
//    							break;
//    						}
//    					}
//    				}
//    			}
//    		}
//    		catch (NotAuthorized e) {
//    			handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
//    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//
//    @Test
//    public void testGetCapabilities() {
//    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);
//
//    		try {
//    			Node node = mn.getCapabilities();
//    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Tests that at least one of the node contacts is RFC2253 compatible,
//     * meaning that it could be represented by a CILogon issued certificate
//     */
//    @Test
//    public void testGetCapabilities_HasCompatibleNodeContact() {
//    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);
//
//    		try {
//    			Node node = mn.getCapabilities();
//    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
//
//    			List<Subject> contacts = node.getContactSubjectList();
//    			boolean found = false;
//    			if (contacts != null) {
//    				for (Subject s : contacts) {
//    					try {
//    						CertificateManager.getInstance().standardizeDN(s.getValue());
//    						found = true;
//    					} catch (IllegalArgumentException e) {
//    						; // this can happen legally, but means that it is not actionable
//    					}
//    				}
//    			}
//    			checkTrue(mn.getLatestRequestUrl(),"the node should have at least one contactSubject that conforms to RFC2253.", found);
//
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Tests that the nodeReference of the node is in the proper urn format.
//     */
//    @Test
//    public void testGetCapabilities_NodeIdentityValidFormat() {
//    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);
//
//    		try {
//    			Node node = mn.getCapabilities();
//    			checkTrue(mn.getLatestRequestUrl(),"getCapabilities returns a Node", node != null);
//
//    			NodeReference nodeRef = node.getIdentifier();
//    			checkTrue(mn.getLatestRequestUrl(),"the node identifier should conform to specification 'urn:node:[\\w_]{2,23}'",
//    					nodeRef.getValue().matches("^urn:node:[\\w_]{2,23}"));
//
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Tests the parameterless and parameterized listObject methods for proper returns.
//     */
//    @Test
//    public void testListObjects() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testListObjects() vs. node: " + currentUrl);
//
//    		try {
//    			ObjectList ol = procureObjectList(mn);
//    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
//
//    			Date fromDate = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
//				Date toDate = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
//				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
//				formatId.setValue(format_text_csv);
//    			Boolean replicaStatus = true;
//				ol = mn.listObjects(null, fromDate, toDate,
//						formatId, replicaStatus ,
//						Integer.valueOf(0),
//						Integer.valueOf(10));
//    			checkTrue(mn.getLatestRequestUrl(),"listObjects(<parameters>) returns an ObjectList", ol != null);
//    		}
//    		catch (TestIterationEndingException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getMessage() + ":: cause: "
//    					+ e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that count and start parameters are functioning, and getCount() and getTotal()
//     * are the correct values.
//     */
//    @Test
//    public void testListObjects_Slicing()
//    {
//    	setupClientSubject_NoCert();
//    	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = new MNode(currentUrl);
//    		printTestHeader("testListObjects_Slicing(...) vs. node: " + currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//
//    		try {
//    			ObjectList ol = mn.listObjects(null, null, null, null, null, null);
//    			// make sure the count is accurate
//    			StringBuffer sb = new StringBuffer();
//    			int i = 0;
//    			if (ol.getCount() != ol.sizeObjectInfoList())
//    				sb.append(++i + ". 'count' attribute should equal the number of ObjectInfos returned. [" +
//    						mn.getLatestRequestUrl() + "]  \n");
//
//    			if (ol.getTotal() < ol.getCount())
//    				sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned ObjectList. [" +
//    						mn.getLatestRequestUrl() + "]  \n");
//
//    			if (ol.getTotal() < ol.sizeObjectInfoList())
//    				sb.append(++i + "'total' attribute should be >= the number of ObjectInfos returned. [" +
//    						mn.getLatestRequestUrl() + "]  \n");
//
//
//    			// test that one can limit the count
//    			int halfCount = ol.sizeObjectInfoList() / 2; // rounds down
//    			ol = mn.listObjects(null, null, null, null, null, 0, halfCount);
//
//    			if (ol.sizeObjectInfoList() != halfCount)
//    				sb.append(++i + ". should be able to limit the number of returned ObjectInfos using " +
//    						"'count' parameter. [" + mn.getLatestRequestUrl() + "]  \n");
//
//    			// TODO:  test that 'start' parameter does what it says
//
//    			// TODO: paging test
//
//
//    			if (i > 0) {
//    				handleFail(currentUrl,"Slicing errors:\n" + sb.toString());
//    			}
//
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ": " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that the fromDate parameter successfully filters out records where
//     * the systemMetadataModified date/time is earler than fromDate.
//     */
//    @Test
//    public void testListObjects_FromDateTest() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testListObjects_FromDateTest() vs. node: " + currentUrl);
//
//    		try {
//    			ObjectList ol = procureObjectList(mn);
//    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
//    			if (ol.getTotal() == 0)
//    				throw new TestIterationEndingException("no objects found in listObjects");
//    			ObjectInfo oi0 = ol.getObjectInfo(0);
//    			Date fromDate = null;
//   				ObjectInfo excludedObjectInfo = null;
//   				for (ObjectInfo oi: ol.getObjectInfoList()) {
//   					if (!oi.getDateSysMetadataModified().equals(oi0.getDateSysMetadataModified())) {
//   						// which is earlier?  can't assume chronological order of objectlist
//   						if (oi.getDateSysMetadataModified().after(oi0.getDateSysMetadataModified())) {
//   							fromDate = oi.getDateSysMetadataModified();
//   							excludedObjectInfo = oi0;
//   						} else {
//   							fromDate = oi0.getDateSysMetadataModified();
//   							excludedObjectInfo = oi;
//   						}
//   						break;
//   					}
//   				}
//   				if (excludedObjectInfo == null) {
//   					// all objects in list have same date, so set the from date
//   					// to a future date
//   					long millisec = oi0.getDateSysMetadataModified().getTime() + 60000;
//   					fromDate = new Date(millisec);
//   					excludedObjectInfo = oi0;
//   				}
//
//   				// call listObjects with a fromDate
//   				ol = mn.listObjects(null, fromDate, null, null, null, null, null);
//
//   				if (ol.getObjectInfoList() != null) {
//   					// at least some objects returned
//   					// so we have to check that the excluded object was excluded
//   					for (ObjectInfo oi: ol.getObjectInfoList()) {
//   						if (oi.getIdentifier().equals(excludedObjectInfo.getIdentifier())) {
//   							handleFail(mn.getLatestRequestUrl(),String.format("identifier %s with sysMetaModified date of '%s'" +
//   									" should not be in the objectList where 'fromDate' parameter set to '%s'",
//   									excludedObjectInfo.getIdentifier().getValue(),
//   									DateTimeMarshaller.serializeDateToUTC(excludedObjectInfo.getDateSysMetadataModified()),
//   									DateTimeMarshaller.serializeDateToUTC(fromDate)
//   									));
//   						}
//   					}
//   				} // else the excluded object was definitely excluded - test passes
//
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//    /**
//     * Tests that the formatID parameter rightly returns no records
//     * when a fake format is given
//     */
//    @Test
//    public void testListObjects_FormatIdFilteringTestFakeFormat() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testListObjects_FormatIdFilteringFakeFormat() vs. node: " + currentUrl);
//    		try {
//    			// call listObjects with a fake format
//   				ObjectList ol = mn.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("fake_format"),
//   						null, null, null);
//   				if (ol.getTotal() != 0) {
//   					handleFail(mn.getLatestRequestUrl(),"filtering the object list by a fake " +
//   							"format should return zero objects");
//   				}
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that the formatID parameter successfully filters records by
//     * the given formatId.  It is an indirect test of the totals returned
//     * by list objects with and without a formatId filter.
//     */
//    @Test
//    public void testListObjects_FormatIdFilteringTest() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testListObjects_FormatIdFiltering() vs. node: " + currentUrl);
//
//    		try {
//    			ObjectList ol = mn.listObjects();
//    			checkTrue(mn.getLatestRequestUrl(),"listObjects() should return an ObjectList", ol != null);
//    			if (ol.getTotal() == 0)
//    				throw new TestIterationEndingException("no objects found in listObjects");
//
//    			int allTotal = ol.getTotal();
//
//    			ObjectFormatIdentifier formatA = ol.getObjectInfo(0).getFormatId();
//
//
//    			boolean foundAnother = false;
//    			int increment = 200;
//    			findAnotherFormat:
//    				for (int i=0; i<allTotal; i += increment) {
//    					ol = mn.listObjects(null, null, null, null, null, i, increment);
//    					for (ObjectInfo oi : ol.getObjectInfoList()) {
//    						if (!oi.getFormatId().equals(formatA)) {
//    							foundAnother = true;
//    							break findAnotherFormat;
//    						}
//    					}
//    				}
//    			if (!foundAnother) {
//    				throw new TestIterationEndingException("only one object format was found.  Can't test format filtering");
//    			}
//    			ol = mn.listObjects(null, null, null, formatA, null, null, null);
//    			checkTrue(mn.getLatestRequestUrl(),"objectList filtered by " +
//    			     formatA.getValue() + " should contain fewer objects than unfiltered",
//    				ol.getTotal() <  allTotal);
//
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//
//    @Test
//    public void testGet() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGet() vs. node: " + currentUrl);
//
//    		try {
//    			String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
// //   			Identifier id = procurePublicReadableTestObject(mn);
//    			InputStream is = mn.get(null,id);
//    			checkTrue(mn.getLatestRequestUrl(),"get() returns an objectStream", is != null);
//    		}
//    		catch (TestIterationEndingException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against:: " +
//    						e.getMessage());
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    @Test
//    public void testGet_NotFound() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGet() vs. node: " + currentUrl);
//
//    		try {
//    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
//    			InputStream is = mn.get(null,D1TypeBuilder.buildIdentifier(fakeID));
//    			handleFail(mn.getLatestRequestUrl(),"get(fakeID) should not return an objectStream.");
//    			is.close();
//    		}
//    		catch (NotFound nf) {
//    			;  // expected outcome
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * test getting data with challenging unicode identifiers.  Will try to
//     * differentiate between NotFound and ServiceFailure
//     */
//	@Test
//    public void testGet_IdentifierEncoding()
//    {
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGet_IdentifierEncoding() vs. node: " + currentUrl);
//
//
//			Vector<String> nodeSummary = new Vector<String>();
//			nodeSummary.add("Node Test Summary for node: " + currentUrl );
//
//			printTestHeader("  Node:: " + currentUrl);
//
//			for (int j=0; j<unicodeStringV.size(); j++)
//			{
//				String status = "OK   ";
//
//				log.info("");
//				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
//				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
//				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
//
//
//				try {
//
//					InputStream data = mn.get(null, D1TypeBuilder.buildIdentifier(idString));
//					handleFail(mn.getLatestRequestUrl(), "get() against the fake identifier (" +
//							idStringEscaped + ") should throw NotFound");
//					data.close();
//					status = "Error";
//				}
//				catch (NotFound nf) {
//					;
//				}
//				catch (ServiceFailure e) {
//					if (e.getDescription().contains("Providing message body")) {
//						if (e.getDescription().contains("404: NotFound:")) {
//							// acceptable result
//							;
//						}
//					}
//					else {
//						status = String.format("Error:: %s: %s: %s",
//								e.getClass().getSimpleName(),
//								e.getDetail_code(),
//								first100Characters(e.getDescription()));
//					}
//				}
//				catch (BaseException e) {
//					status = String.format("Error:: %s: %s: %s",
//							e.getClass().getSimpleName(),
//							e.getDetail_code(),
//							first100Characters(e.getDescription()));
//				}
//				catch(Exception e) {
//					status = "Error";
//					e.printStackTrace();
//					status = String.format("Error:: %s: %s",
//							e.getClass().getName(),
//							first100Characters(e.getMessage()));
//				}
//
//				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
//			}
//
//			for (String result : nodeSummary) {
//				if (result.contains("Error")) {
//					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
//					break;
//				}
//			}
//		}
//    }
//
//
//    @Test
//    public void testGetSystemMetadata() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
//
//    		try {
//    			String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
////    			Identifier id = procurePublicReadableTestObject(mn);
//    			SystemMetadata smd = mn.getSystemMetadata(null,id);
//    			checkTrue(mn.getLatestRequestUrl(),"getSystemMetadata() returns a SystemMetadata object", smd != null);
//    		}
//    		catch (IndexOutOfBoundsException e) {
//    			handleFail(currentUrl,"No Objects available to test against");
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    @Test
//    public void testGetSystemMetadata_NotFound() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
//
//    		try {
//    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
//    			mn.getSystemMetadata(null,D1TypeBuilder.buildIdentifier(fakeID));
//    			handleFail(mn.getLatestRequestUrl(),"getSystemMetadata(fakeID) should throw dataone NotFound.");
//    		}
//    		catch (NotFound nf) {
//    			;  // expected outcome
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//	@Test
//    public void testGetSystemMetadata_IdentifierEncoding()
//    {
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//		printTestHeader("Testing IdentifierEncoding - setting up identifiers to check");
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetSystemMetadata_IdentifierEncoding() vs. node: " + currentUrl);
//
//
//			Vector<String> nodeSummary = new Vector<String>();
//			nodeSummary.add("Node Test Summary for node: " + currentUrl );
//
//			printTestHeader("  Node:: " + currentUrl);
//
//			for (int j=0; j<unicodeStringV.size(); j++)
//			{
//				String status = "OK   ";
//
//				log.info("");
//				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
//				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
//				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
//
//
//				try {
//					mn.getSystemMetadata(null, D1TypeBuilder.buildIdentifier(idString));
//					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
//							idStringEscaped + ") should throw NotFound");
//
//					status = "Error";
//				}
//				catch (NotFound nf) {
//					;
//				}
//				catch (ServiceFailure e) {
//					if (e.getDescription().contains("Providing message body")) {
//						if (e.getDescription().contains("404: NotFound:")) {
//							// acceptable result
//							;
//						}
//					}
//					else {
//						status = String.format("Error:: %s: %s: %s",
//								e.getClass().getSimpleName(),
//								e.getDetail_code(),
//								first100Characters(e.getDescription()));
//					}
//				}
//				catch (BaseException e) {
//					status = String.format("Error:: %s: %s: %s",
//							e.getClass().getSimpleName(),
//							e.getDetail_code(),
//							first100Characters(e.getDescription()));
//				}
//				catch(Exception e) {
//					status = "Error";
//					e.printStackTrace();
//					status = String.format("Error:: %s: %s",
//							e.getClass().getName(),
//							first100Characters(e.getMessage()));
//				}
//
//				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
//			}
//
//			for (String result : nodeSummary) {
//				if (result.contains("Error")) {
//					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
//					break;
//				}
//			}
//	    }
//    }
//
//
//
//    @Test
//    public void testDescribe() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testDescribe() vs. node: " + currentUrl);
//
//    		try {
//    			String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
////    			Identifier id = procurePublicReadableTestObject(mn);
//    			DescribeResponse dr = mn.describe(null,id);
//    			checkTrue(mn.getLatestRequestUrl(),"describe() returns a DescribeResponse object", dr != null);
//    		}
//    		catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    @Test
//    public void testDescribe_NotFound() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testDescribe() vs. node: " + currentUrl);
//
//    		try {
//    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
//    			mn.describe(null,D1TypeBuilder.buildIdentifier(fakeID));
//    			handleFail(mn.getLatestRequestUrl(),"describe(fakeID) should return a d1 NotFound in the header.");
//
//    		}
//    		catch (NotFound nf) {
//    			;  // expected outcome
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//
//	@Test
//	public void testDescribe_IdentifierEncoding()
//	{
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testDescribe_IdentifierEncoding() vs. node: " + currentUrl);
//
//
//			Vector<String> nodeSummary = new Vector<String>();
//			nodeSummary.add("Node Test Summary for node: " + currentUrl );
//
//			printTestHeader("  Node:: " + currentUrl);
//
//			for (int j=0; j<unicodeStringV.size(); j++)
//			{
//				String status = "OK   ";
//
//				log.info("");
//				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
//				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
//				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
//
//
//
//
//				try {
//					mn.describe(null, D1TypeBuilder.buildIdentifier(idString));
//					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
//							idStringEscaped + ") should throw NotFound");
//
//					status = "Error";
//				}
//				catch (NotFound nf) {
//					;
//				}
//				catch (ServiceFailure e) {
//					if (e.getDescription().contains("Providing message body")) {
//						if (e.getDescription().contains("404: NotFound:")) {
//							// acceptable result
//							;
//						}
//					}
//					else {
//						status = String.format("Error:: %s: %s: %s",
//								e.getClass().getSimpleName(),
//								e.getDetail_code(),
//								first100Characters(e.getDescription()));
//					}
//				}
//				catch (BaseException e) {
//					status = String.format("Error:: %s: %s: %s",
//							e.getClass().getSimpleName(),
//							e.getDetail_code(),
//							first100Characters(e.getDescription()));
//				}
//				catch(Exception e) {
//					status = "Error";
//					e.printStackTrace();
//					status = String.format("Error:: %s: %s",
//							e.getClass().getName(),
//							first100Characters(e.getMessage()));
//				}
//
//				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
//			}
//
//			for (String result : nodeSummary) {
//				if (result.contains("Error")) {
//					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
//					break;
//				}
//			}
//	    }
//	}
//
//
//
//    @Test
//    public void testGetChecksum() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);
//
//    		try {
//    			String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
////    			Identifier id = procurePublicReadableTestObject(mn);
//    			Checksum cs = mn.getChecksum(null,id,CHECKSUM_ALGORITHM);
//    			checkTrue(mn.getLatestRequestUrl(),"getChecksum() returns a Checksum object", cs != null);
//    		}
//    		catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    @Test
//    public void testGetChecksum_NotFound() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);
//
//    		try {
//    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
//    			mn.getChecksum(null,D1TypeBuilder.buildIdentifier(fakeID), null);
//    			handleFail(mn.getLatestRequestUrl(),"getChecksum(fakeID) should return a NotFound");
//    		}
//    		catch (NotFound nf) {
//    			;  // expected outcome
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that a resource map can be parsed by the ResourceMapFactory.
//     */
//    @Test
//    public void testResourceMap_Parsing() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);
//
//    		try {
//    			ObjectList ol = mn.listObjects(null, null,
//    				D1TypeBuilder.buildFormatIdentifier("http://www.openarchives.org/ore/terms"),
//    					null, null, null);
//    			if (ol.sizeObjectInfoList() > 0) {
//    				ObjectInfo oiResMap = ol.getObjectInfoList().get(0);
//
//    				InputStream is = mn.get(oiResMap.getIdentifier());
//    				String resMapContent = IOUtils.toString(is);
//    				try {
//    					ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
//    				} catch (Exception e) {
//    					handleFail(mn.getLatestRequestUrl(), "should be able to parse the serialized resourceMap");
//    				}
//    			} else {
//    				handleFail(mn.getLatestRequestUrl(),"No resource maps " +
//    						"(formatId = 'http://www.openarchives.org/ore/terms' " +
//    						"returned from listObjects.  Cannot test.");
//    			}
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//    /**
//     * Iterates known resource map formats to find an exemplar for testing that
//     * the checksum and size in the ObjectInfo match what is in
//     * systemMetadata, and matches what is recalculated when retrieving the object.
//     *
//     */
//    @Test
//    public void testResourceMap_Checksum_Size_Consistency() {
//    	testContent_Checksum_Size_Consistency("RESOURCE");
//    }
//
//    /**
//     * Iterates known metadata formats to find an exemplar for testing that
//     * the checksum and size in the ObjectInfo match what is in
//     * systemMetadata, and matches what is recalculated when retrieving the object.
//     *
//     */
//    @Test
//    public void testMetadata_Checksum_Size_Consistency() {
//    	testContent_Checksum_Size_Consistency("METADATA");
//    }
//
//    /**
//     * Test to compare the checksum and size in the ObjectInfo match what is in
//     * systemMetadata, and matches what is recalculated when retrieving the object.
//     */
//    protected void testContent_Checksum_Size_Consistency(String formatType) {
//
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//
//       	StringBuffer formatsChecked = new StringBuffer("Formats Checked:");
//
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testContent_Checksum_Size_Consistency(" + formatType +
//    				") vs. node: " + currentUrl);
//    		boolean foundOne = false;
//    		try {
//    			ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
//    			for(ObjectFormat of : ofl.getObjectFormatList()) {
//    				if (of.getFormatType().equals(formatType)) {
//    					formatsChecked.append("\n" + of.getFormatId().getValue());
//    					log.info("   looking for objects with format: " + of.getFormatId().getValue());
//
//    					ObjectList ol = mn.listObjects(null, null,
//    							of.getFormatId(), null, null, null);
//
//    					//TODO: listObjects returns ids for things that are not readable...
//    					if (ol.sizeObjectInfoList() > 0) {
//        					foundOne = true;
//
//
//    						log.info(ol.sizeObjectInfoList() + " items found of type " +
//    						  of.getFormatId().getValue());
//
//    						ObjectInfo oi = ol.getObjectInfoList().get(0);
//    						SystemMetadata smd = mn.getSystemMetadata(oi.getIdentifier());
//    						checkEquals(mn.getLatestRequestUrl(),"objectInfo checksum should equal that of sysMeta",
//    								oi.getChecksum().getAlgorithm() + " : " + oi.getChecksum().getValue(),
//    								smd.getChecksum().getAlgorithm() + " : " + smd.getChecksum().getValue());
//    						checkEquals(mn.getLatestRequestUrl(),"objectInfo size should equal that of sysMeta",
//    								oi.getSize().toString(),
//    								smd.getSize().toString());
//
//
//    						InputStream is = mn.get(oi.getIdentifier());
//    						//calculate the checksum and length
//    						CountingInputStream cis = new CountingInputStream(is);
//    						Checksum calcCS = ChecksumUtil.checksum(cis,oi.getChecksum().getAlgorithm());
//    						long calcSize = cis.getByteCount();
//
//    						checkEquals(mn.getLatestRequestUrl(),"calculated checksum should equal that of sysMeta",
//    								calcCS.getValue(),
//    								smd.getChecksum().getValue());
//    						checkEquals(mn.getLatestRequestUrl(),"calculated size should equal that of sysMeta",
//    								String.valueOf(calcSize),
//    								smd.getSize().toString());
//
////    						break;
//    					}  // found at least one of that type
//    				}  // formatType matches
//    			} // for each type
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//
//    		if (!foundOne)
//    			handleFail(mn.getLatestRequestUrl(),"No objects of formatType " +
//    					formatType + "returned from listObjects.  Cannot test.\n" +
//    					formatsChecked.toString());
//    	} // while member node
//    }
//
//
//
//    /**
//     * Tests that a resource map can be parsed by the ResourceMapFactory.
//     * Method: for every formatID of type resource, pull an objectList of just that
//     * type (maximum of 20), and try to parse each one.
//     */
//    @Test
//    public void testResourceMapParsing() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testResourceMapParsing() vs. node: " + currentUrl);
//
//    		StringBuffer formatsChecked = new StringBuffer("Formats Checked:");
//
//    		try {
//    			boolean foundOne = false;
//    			ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
//    			for(ObjectFormat of : ofl.getObjectFormatList())
//    			{
//    				if (of.getFormatType().equals("RESOURCE")) {
//    					formatsChecked.append("\n" + of.getFormatId().getValue());
//
//    					ObjectList ol = mn.listObjects(null, null, of.getFormatId(),null, null, 20);
//    					if (ol.sizeObjectInfoList() > 0) {
//    						log.info(ol.sizeObjectInfoList() + " items found of type " +
//    	    						  of.getFormatId().getValue());
//    						for (ObjectInfo oi : ol.getObjectInfoList()) {
//    							String resMapContent;
//    							try {
//    								InputStream is = mn.get(oi.getIdentifier());
//    								foundOne = true;
//    								log.info("Found public resource map: " + oi.getIdentifier().getValue());
//    								resMapContent = IOUtils.toString(is);
//    								if (resMapContent != null) {
//    									ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
//    								} else {
//    									handleFail(mn.getLatestRequestUrl(),"got null content from the get request");
//    								}
//    							} catch (NotAuthorized e) {
//    								; // comes from the mn.get(), will keep trying...
//    							} catch (NullPointerException npe) {
//    								handleFail(mn.getLatestRequestUrl(),
//    										"Got NPE exception from the parsing library, which means that the " +
//    										"content could not be parsed into a ResourceMap.  One known cause " +
//    										"is relative resource URIs used for the resource map object, the aggregated resources," +
//    										" or the aggregation itself." );
//    							} catch (Exception e) {
//    								handleFail(mn.getLatestRequestUrl(),
//    										"Should be able to parse the serialized resourceMap.  Got exception: " +
//    											e.getClass().getSimpleName() + ": " +
//    											e.getMessage() +
//    											"at line number " + e.getStackTrace()[0].getLineNumber());
//    							}
//    						}
//    					}
//    				}
//    			}
//    			if (!foundOne) {
//    				handleFail(mn.getLatestRequestUrl(),"No public resource maps " +
//							"returned from listObjects.  Cannot test.\n" +
//    					formatsChecked.toString());
//    			}
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage() +
//    					"at line number " + e.getStackTrace()[0].getLineNumber());
//    		}
//    	}
//    }
//
//
//    /**
//     * Tests that the resource map's URLs point to cn/v1/resolve.
//     */
//    @Test
//    public void testResourceMap_ResolveURL() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testResourceMapParsing_ResolveURL() vs. node: " + currentUrl);
//
//    		StringBuffer formatsChecked = new StringBuffer("Formats Checked:");
//
//    		try {
//    			boolean foundOne = false;
//    			ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
//    			for(ObjectFormat of : ofl.getObjectFormatList())
//    			{
//    				if (of.getFormatType().equals("RESOURCE")) {
//    					formatsChecked.append("\n" + of.getFormatId().getValue());
//
//    					ObjectList ol = mn.listObjects(null, null, of.getFormatId(),null, null, 20);
//    					if (ol.sizeObjectInfoList() > 0) {
//    						log.info(ol.sizeObjectInfoList() + " items found of type " +
//    	    						  of.getFormatId().getValue());
//    						for (ObjectInfo oi : ol.getObjectInfoList()) {
//    							String resMapContent;
//    							try {
//    								InputStream is = mn.get(oi.getIdentifier());
//    								foundOne = true;
//    								log.info("Found public resource map: " + oi.getIdentifier().getValue());
//    								resMapContent = IOUtils.toString(is);
//    								resourceMapChecker(mn, oi.getIdentifier(), resMapContent);
//
//
//    							} catch (NotAuthorized e) {
//    								; // comes from the mn.get(), will keep trying...
//    							} catch (NullPointerException npe) {
//    								handleFail(mn.getLatestRequestUrl(),
//    										"Got NPE exception from the parsing library, which means that the " +
//    										"content could not be parsed into a ResourceMap.  One known cause " +
//    										"is relative resource URIs used for the resource map object, the aggregated resources," +
//    										" or the aggregation itself." );} catch (Exception e) {
//    								handleFail(mn.getLatestRequestUrl(),
//    										"Should be able to parse the serialized resourceMap.  Got exception: " +
//    											e.getClass().getSimpleName() + ": " +
//    											e.getMessage());
//    							}
//    						}
//    					}
//    				}
//    			}
//    			if (!foundOne) {
//    				handleFail(mn.getLatestRequestUrl(),"No public resource maps " +
//							"returned from listObjects.  Cannot test.\n" +
//    					formatsChecked.toString());
//    			}
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
////    @Test
//    public void foo() throws InvalidToken, NotAuthorized, NotImplemented,
//    ServiceFailure, NotFound, InsufficientResources, IOException,
//    OREException, URISyntaxException, OREParserException
//    {
//    	MNode mn = D1Client.getMN("https://mn-demo-5.test.dataone.org/knb/d1/mn");
//    	Identifier p = D1TypeBuilder.buildIdentifier("resourceMap_doi:10.5072/FK28K7J92");
//    	InputStream is = mn.get(p);
//    	String resMapContent = IOUtils.toString(is);
//    	resMapContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rdf:RDF"
//+ "   xmlns:cito=\"http://purl.org/spar/cito/\""
//+ "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
//+ "   xmlns:dcterms=\"http://purl.org/dc/terms/\""
//+ "   xmlns:foaf=\"http://xmlns.com/foaf/0.1/\""
//+ "   xmlns:ore=\"http://www.openarchives.org/ore/terms/\""
//+ "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
//+ "   xmlns:rdfs1=\"http://www.w3.org/2001/01/rdf-schema#\">"
//+ "  <rdf:Description rdf:about=\"http://foresite-toolkit.googlecode.com/#pythonAgent\">"
//+ "    <foaf:mbox>foresite@googlegroups.com</foaf:mbox>"
//+ "    <foaf:name>Foresite Toolkit (Python)</foaf:name>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"https://cn.dataone.org/cn/v1/resolve/scimeta_id\">"
//+ "    <cito:documents rdf:resource=\"https://cn-dev.test.dataone.org/cn/v1/resolve/scidata_id\"/>"
//+ "    <dcterms:identifier>scimeta_id</dcterms:identifier>"
//+ "    <dcterms:description>A reference to a science metadata document using a DataONE identifier.</dcterms:description>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/ResourceMap\">"
//+ "    <rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/>"
//+ "    <rdfs1:label>ResourceMap</rdfs1:label>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"https://cn.dataone.org/cn/v1/resolve/resource_map_id\">"
//+ "    <dcterms:identifier>resource_map_id</dcterms:identifier>"
//+ "    <dcterms:modified>2011-08-12T12:55:16Z</dcterms:modified>"
//+ "    <rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/ResourceMap\"/>"
//+ "    <dc:format>application/rdf+xml</dc:format>"
//+ "    <ore:describes rdf:resource=\"https://cn-dev.test.dataone.org/cn/v1/resolve/aggregation_id\"/>"
//+ "    <dcterms:created>2011-08-12T12:55:16Z</dcterms:created>"
//+ "    <dcterms:creator rdf:resource=\"http://foresite-toolkit.googlecode.com/#pythonAgent\"/>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/Aggregation\">"
//+ "    <rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/>"
//+ "    <rdfs1:label>Aggregation</rdfs1:label>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"https://cn-dev.test.dataone.org/cn/v1/resolve/aggregation_id\">"
//+ "    <rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/Aggregation\"/>"
//+ "    <dcterms:title>Simple aggregation of science metadata and data</dcterms:title>"
//+ "    <ore:aggregates rdf:resource=\"https://cn-dev.test.dataone.org/cn/v1/resolve/scidata_id\"/>"
//+ "    <ore:aggregates rdf:resource=\"https://cn.dataone.org/cn/v1/resolve/scimeta_id\"/>"
//+ "  </rdf:Description>"
//+ "  <rdf:Description rdf:about=\"https://cn-dev.test.dataone.org/cn/v1/resolve/scidata_id\">"
//+ "    <cito:isDocumentedBy rdf:resource=\"https://cn.dataone.org/cn/v1/resolve/scimeta_id\"/>"
//+ "    <dcterms:identifier>scidata_id</dcterms:identifier>"
//+ "    <dcterms:description>A reference to a science data object using a DataONE identifier</dcterms:description>"
//+ "  </rdf:Description>"
//+ "</rdf:RDF>";
//    	resMapContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rdf:RDF xmlns:cito=\"http://purl.org/spar/cito/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:ore=\"http://www.openarchives.org/ore/terms/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs1=\"http://www.w3.org/2001/01/rdf-schema#\" ><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71\"><dcterms:identifier>7f115193-ea2b-4486-ba72-afe51008dc71</dcterms:identifier><cito:isDocumentedBy>http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3</cito:isDocumentedBy><dcterms:description>Data object (7f115193-ea2b-4486-ba72-afe51008dc71)</dcterms:description><dc:title>Dataset: 5760e5ef-efef-4c97-b393-79850d1b67b3</dc:title></rdf:Description><rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/Aggregation\"><rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/><rdfs1:label>Aggregation</rdfs1:label></rdf:Description><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3\"><dcterms:identifier>5760e5ef-efef-4c97-b393-79850d1b67b3</dcterms:identifier><dc:title>Metadata: 5760e5ef-efef-4c97-b393-79850d1b67b3</dc:title><dcterms:description>Science metadata object (5760e5ef-efef-4c97-b393-79850d1b67b3) for Data object (7f115193-ea2b-4486-ba72-afe51008dc71)</dcterms:description><cito:documents>http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71</cito:documents></rdf:Description><rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/ResourceMap\"><rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/><rdfs1:label>ResourceMap</rdfs1:label></rdf:Description><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/28335b70-8d10-49f7-910f-9a84a14703d4\"><dcterms:creator rdf:resource=\"http://foresite-toolkit.googlecode.com/#pythonAgent\"/><dcterms:modified>2012-10-25T16:51:47Z</dcterms:modified><dc:format>application/rdf+xml</dc:format>" +
//    			"<ore:describes rdf:resource=\"https://cn.dataone.org/cn/v1/resolve/aggregate_id\"/><rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/ResourceMap\"/><dcterms:created>2012-10-25T16:51:47Z</dcterms:created></rdf:Description><rdf:Description rdf:about=\"http://foresite-toolkit.googlecode.com/#pythonAgent\"><foaf:mbox>foresite@googlegroups.com</foaf:mbox><foaf:name>Foresite Toolkit (Python)</foaf:name></rdf:Description>" +
//    			"<rdf:Description rdf:about=\"https://cn.dataone.org/cn/v1/resolve/aggregate_id\"><rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/Aggregation\"/><ore:aggregates rdf:resource=\"http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3\"/><ore:aggregates rdf:resource=\"http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71\"/></rdf:Description></rdf:RDF>";
//    	resMapContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rdf:RDF xmlns:cito=\"http://purl.org/spar/cito/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:ore=\"http://www.openarchives.org/ore/terms/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs1=\"http://www.w3.org/2001/01/rdf-schema#\" ><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71\"><dcterms:identifier>7f115193-ea2b-4486-ba72-afe51008dc71</dcterms:identifier><cito:isDocumentedBy>http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3</cito:isDocumentedBy><dcterms:description>Data object (7f115193-ea2b-4486-ba72-afe51008dc71)</dcterms:description></rdf:Description><rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/Aggregation\"><rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/><rdfs1:label>Aggregation</rdfs1:label></rdf:Description><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3\"><dcterms:identifier>5760e5ef-efef-4c97-b393-79850d1b67b3</dcterms:identifier><dcterms:description>Science metadata object (5760e5ef-efef-4c97-b393-79850d1b67b3) for Data object (7f115193-ea2b-4486-ba72-afe51008dc71)</dcterms:description><cito:documents>http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71</cito:documents></rdf:Description><rdf:Description rdf:about=\"http://www.openarchives.org/ore/terms/ResourceMap\"><rdfs1:isDefinedBy rdf:resource=\"http://www.openarchives.org/ore/terms/\"/><rdfs1:label>ResourceMap</rdfs1:label></rdf:Description><rdf:Description rdf:about=\"http://129.24.63.115/apps/dataone/v1/object/28335b70-8d10-49f7-910f-9a84a14703d4\"><dcterms:creator rdf:resource=\"http://foresite-toolkit.googlecode.com/#pythonAgent\"/><dcterms:modified>2012-10-25T16:51:47Z</dcterms:modified><dc:format>application/rdf+xml</dc:format>" +
//    			"<ore:describes rdf:resource=\"https://cn.dataone.org/cn/v1/resolve/aggregate_id\"/><rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/ResourceMap\"/><dcterms:created>2012-10-25T16:51:47Z</dcterms:created></rdf:Description><rdf:Description rdf:about=\"http://foresite-toolkit.googlecode.com/#pythonAgent\"><foaf:mbox>foresite@googlegroups.com</foaf:mbox><foaf:name>Foresite Toolkit (Python)</foaf:name></rdf:Description>" +
//    			"<rdf:Description rdf:about=\"https://cn.dataone.org/cn/v1/resolve/aggregate_id\"><rdf:type rdf:resource=\"http://www.openarchives.org/ore/terms/Aggregation\"/><ore:aggregates rdf:resource=\"http://129.24.63.115/apps/dataone/v1/object/5760e5ef-efef-4c97-b393-79850d1b67b3\"/><ore:aggregates rdf:resource=\"http://129.24.63.115/apps/dataone/v1/object/7f115193-ea2b-4486-ba72-afe51008dc71\"/></rdf:Description></rdf:RDF>";
//
//    	System.out.println(resMapContent);
//    	resourceMapChecker(mn,p,resMapContent);
//    }
//
//    private void resourceMapChecker(MNode mn, Identifier packageId, String resMapContent)
//    throws UnsupportedEncodingException, OREException, URISyntaxException, OREParserException
//    {
//    	Map<Identifier, Map<Identifier, List<Identifier>>> rm =
//		ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
//
// //   	checkTrue(mn.getLatestRequestUrl(),
// //   	    "packageId matches packageId used to call", rm.containsKey(packageId));
//
//    	if (rm != null) {
//    		Iterator<Identifier> it = rm.keySet().iterator();
//
//    		while (it.hasNext()) {
//    			Identifier pp = it.next();
//    			System.out.println("package: " + pp.getValue());
//    		}
//
//    		Map<Identifier, List<Identifier>> agg = rm.get(rm.keySet().iterator().next());
//    		Iterator<Identifier> itt  = agg.keySet().iterator();
//    		while (itt.hasNext()) {
//    			Identifier docs = itt.next();
//    			System.out.println("md: " + docs.getValue());
//    			//checkTrue("the identifier should start with https://cn.dataone.org/cn/v1/resolve","",true);
//    			List<Identifier> docd = agg.get(docs);
//    			for (Identifier id: docd) {
//    				System.out.println("data: " + id.getValue());
//    			}
//    		}
//    	} else {
//    		handleFail("","parseResourceMap returned null");
//    	}
//
//    }
//
//
//	@Test
//	public void testGetChecksum_IdentifierEncoding()
//	{
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetChecksum_IdentifierEncoding() vs. node: " + currentUrl);
//
//
//			Vector<String> nodeSummary = new Vector<String>();
//			nodeSummary.add("Node Test Summary for node: " + currentUrl );
//
//			printTestHeader("  Node:: " + currentUrl);
//
//			for (int j=0; j<unicodeStringV.size(); j++)
//			{
//				String status = "OK   ";
//
//				log.info("");
//				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
//				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
//				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
//
//				try {
//					mn.getChecksum(null, D1TypeBuilder.buildIdentifier(idString), null);
//					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
//							idStringEscaped + ") should throw NotFound");
//
//					status = "Error";
//				}
//				catch (NotFound nf) {
//					;
//				}
//				catch (ServiceFailure e) {
//					if (e.getDescription().contains("Providing message body")) {
//						if (e.getDescription().contains("404: NotFound:")) {
//							// acceptable result
//							;
//						}
//					}
//					else {
//						status = String.format("Error:: %s: %s: %s",
//								e.getClass().getSimpleName(),
//								e.getDetail_code(),
//								first100Characters(e.getDescription()));
//					}
//				}
//				catch (BaseException e) {
//					status = String.format("Error:: %s: %s: %s",
//							e.getClass().getSimpleName(),
//							e.getDetail_code(),
//							first100Characters(e.getDescription()));
//				}
//				catch(Exception e) {
//					status = "Error";
//					e.printStackTrace();
//					status = String.format("Error:: %s: %s",
//							e.getClass().getName(),
//							first100Characters(e.getMessage()));
//				}
//
//				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
//			}
//
//			for (String result : nodeSummary) {
//				if (result.contains("Error")) {
//					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
//					break;
//				}
//			}
//		}
//	}
//
//
//
//    @Test
//    public void testSynchronizationFailed_NoCert() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testSynchronizationFailed() vs. node: " + currentUrl);
//
//    		try {
//
//    			String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
////				Identifier id = procurePublicReadableTestObject(mn);
//    			SynchronizationFailed sf = new SynchronizationFailed("0","a message",id.getValue(),null);
//    			System.out.println(sf.serialize(SynchronizationFailed.FMT_XML));
//    			mn.synchronizationFailed(null,
//    					new SynchronizationFailed("0","a message",id.getValue(),null));
//    			checkTrue(mn.getLatestRequestUrl(),"synchronizationFailed() does not throw exception", true);
//    		}
//    		catch (NotAuthorized e) {
//    			; // this is an acceptable (and preferrable) outcome for calling without a client cert.
//    		}
//    		catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ":: " +
//    					e.getDetail_code() + " " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
// //TODO: refactor getReplica tests so Tier1 nodes are treated differently - make sure
// // the getReplica request is logged differently
//    // Tier2 nodes and above can't be tested
//
//
//	/**
//	 *  Test MNReplication.getReplica() functionality using a public object.
//	 *  Tier2 and higher member nodes are required to check that the caller is
//	 *  authorized to read the object by checking with the CN that it is a qualified
//	 *  member node.  Requests for public objects do not need to perform this
//	 *  check, because they are public.
//	 *
//	 *  Some member nodes may choose to qualify the client subject anyway, to make
//	 *  sure it is a member node.
//	 */
//	@Test
//	public void testGetReplica_PublicObject() {
//
//		String clientSubject = "cnStageUNM1";
//		setupClientSubject(clientSubject);
//
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetReplica() vs. node: " + currentUrl);
//
//			try {
//				String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier pid = procurePublicReadableTestObject(mn,
//						D1TypeBuilder.buildIdentifier(objectIdentifier));
//
//				InputStream is = mn.getReplica(null, pid);
//				checkTrue(mn.getLatestRequestUrl(), "Successful getReplica() call" +
//						"should yield a non-null inputStream.",
//						is != null);
//			}
//			catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),  "Should be able to retrieve " +
//						"a public object (as subject " + clientSubject +
//						").  If the node is checking the client subject against the " +
//						"CN for all getReplica requests, and the node is not " +
//						"registered to an environment, this failure can be ignored.  Got:" +
//						e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription()
//    					);
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//		}
//	}
//
//
//	/**
//	 *  Test MNReplication.getReplica() functionality.  This tests for expected
//	 *  exception when a non-MemberNode client calls the method.
//	 */
//	// TODO: is this doable in stand-alone mode?
//
////	@Test
//	public void testGetReplica_ValidCertificate_NotMN() {
//
//		setupClientSubject("testRightsHolder");
//
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetReplica_AuthenticateITKUser() vs. node: " + currentUrl);
//
//			try {
//				String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//				mn.getReplica(null, pid);
//				handleFail(mn.getLatestRequestUrl(),"with non-Node client certificate, getReplica() should throw exception");
//			}
//			catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//			catch (NotAuthorized e) {
//				// expected behavior
//			}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//		}
//	}
//
//
//	/**
//	 *  Test MNReplication.getReplica() functionality.  Normal usage is the caller
//	 *  being another MemberNode.  Others should fail.  This tests the latter case.
//	 */
////	@Test
//	public void testGetReplica_NoCertificate() {
//
//		setupClientSubject_NoCert();
//
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetReplica_NoCert() vs. node: " + currentUrl);
//
//			try {
//				String objectIdentifier = "TierTesting:" +
//					 	createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
//					 	":Public_READ" + testObjectSeriesSuffix;
//				Identifier pid = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
//				mn.getReplica(null, pid);
//				handleFail(mn.getLatestRequestUrl(),"with no client certificate, getReplica() should throw exception");
//			}
//			catch (IndexOutOfBoundsException e) {
//    			handleFail(mn.getLatestRequestUrl(),"No Objects available to test against");
//    		}
//			catch (InvalidToken e) {
//				// expected behavior
//			}
//			catch (NotAuthorized e) {
//				// also expected behavior
//			}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//		}
//	}
//
//
////    @Test
//    public void testGetReplica_NotFound() {
//    	setupClientSubject_NoCert();
//       	Iterator<Node> it = getMemberNodeIterator();
//    	while (it.hasNext()) {
//    		currentUrl = it.next().getBaseURL();
//    		MNode mn = D1Client.getMN(currentUrl);
//    		currentUrl = mn.getNodeBaseServiceUrl();
//    		printTestHeader("testGetReplica() vs. node: " + currentUrl);
//
//    		try {
//    			String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier();
//    			InputStream is = mn.get(null,D1TypeBuilder.buildIdentifier(fakeID));
//    			handleFail(mn.getLatestRequestUrl(),"getReplica(fakeID) should not return an objectStream.");
//    			is.close();
//    		}
//    		catch (NotFound nf) {
//    			;  // expected outcome
//    		}
//    		catch (BaseException e) {
//    			handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
//    					e.getDetail_code() + ":: " + e.getDescription());
//    		}
//    		catch(Exception e) {
//    			e.printStackTrace();
//    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//    		}
//    	}
//    }
//
//
//
////	@Test
//	public void testGetReplica_IdentifierEncoding()
//	{
//		setupClientSubject_NoCert();
//		Iterator<Node> it = getMemberNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			MNode mn = D1Client.getMN(currentUrl);
//			currentUrl = mn.getNodeBaseServiceUrl();
//			printTestHeader("testGetReplica_IdentifierEncoding() vs. node: " + currentUrl);
//
//
//			Vector<String> nodeSummary = new Vector<String>();
//			nodeSummary.add("Node Test Summary for node: " + currentUrl );
//
//			printTestHeader("  Node:: " + currentUrl);
//
//			for (int j=0; j<unicodeStringV.size(); j++)
//			{
//				String status = "OK   ";
//
//				log.info("");
//				log.info(j + "    unicode String:: " + unicodeStringV.get(j));
//				String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
//				String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);
//
//
//				try {
//					mn.getReplica(null, D1TypeBuilder.buildIdentifier(idString));
//					handleFail(mn.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
//							idStringEscaped + ") should throw NotFound");
//
//					status = "Error";
//				}
//				catch (NotFound nf) {
//					;
//				}
//				catch (ServiceFailure e) {
//					if (e.getDescription().contains("Providing message body")) {
//						if (e.getDescription().contains("404: NotFound:")) {
//							// acceptable result
//							;
//						}
//					}
//					else {
//						status = String.format("Error:: %s: %s: %s",
//								e.getClass().getSimpleName(),
//								e.getDetail_code(),
//								first100Characters(e.getDescription()));
//					}
//				}
//				catch (BaseException e) {
//					status = String.format("Error:: %s: %s: %s",
//							e.getClass().getSimpleName(),
//							e.getDetail_code(),
//							first100Characters(e.getDescription()));
//				}
//				catch(Exception e) {
//					status = "Error";
//					e.printStackTrace();
//					status = String.format("Error:: %s: %s",
//							e.getClass().getName(),
//							first100Characters(e.getMessage()));
//				}
//
//				nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
//			}
//
//			for (String result : nodeSummary) {
//				if (result.contains("Error")) {
//					handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
//					break;
//				}
//			}
//		}
//	}
//
//
//	private String first100Characters(String s) {
//		if (s.length() <= 100)
//			return s;
//
//		return s.substring(0, 100) + "...";
//
//	}
//
//	private String tablifyResults(Vector<String> results)
//	{
//		StringBuffer table = new StringBuffer("Failed 1 or more identifier encoding tests");
//		for (String result: results) {
//			table.append(result);
//			table.append("\n    ");
//		}
//		return table.toString();
//	}
}
