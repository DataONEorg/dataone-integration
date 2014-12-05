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

import java.util.Date;
import java.util.Iterator;

import org.dataone.client.v1.CNode;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.Node;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_logAgg_IT extends ContextAwareTestCaseDataone {
	
	protected String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "urn:node:cnDevUNM1");
	
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



	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods that are implemented by the log_aggregation component";
	}
	
}
