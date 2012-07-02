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

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dataone.client.CNode;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.util.D1Url;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_misc_IT extends ContextAwareTestCaseDataone {


	private static String unicodeIdPrefix = "testCNodeTier1";
	
	protected String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "urn:node:cnDevUNM1");

	
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";

	
	private static String currentUrl;


	

    
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
    				eventLog = cn.getLogRecords(fromDate, null, null, null, null, null);

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
    			Identifier pid = null;
    			for (int i = 0; i<ol.sizeObjectInfoList(); i++) {
    				try {
    					cn.getSystemMetadata(ol.getObjectInfo(i).getIdentifier());
    					pid = ol.getObjectInfo(i).getIdentifier();
    					break;
    				} catch (BaseException be) {
    					;
    				}
    			}
    			if (pid != null) {
    				log.debug("   pid = " + pid.getValue());
    				ObjectLocationList response = cn.resolve(null,pid);
    				checkTrue(cn.getLatestRequestUrl(),"resolve(...) returns an ObjectLocationList object",
    					response != null && response instanceof ObjectLocationList);
    			}
    			else {
    				handleFail(currentUrl, "No public object available to test resolve against");
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


	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods that aren't metacat or identity manager";
	}
	
}
