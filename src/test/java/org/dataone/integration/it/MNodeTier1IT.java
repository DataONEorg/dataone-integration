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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.MonitorList;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier1IT extends ContextAwareTestCaseDataone  {

    private static final String TEST_MN_ID = "c3p0";
    private static String format_text_csv = "text/csv";
    private static String format_eml_200 = "eml://ecoinformatics.org/eml-2.0.0";
    private static String format_eml_201 = "eml://ecoinformatics.org/eml-2.0.1";
    private static String format_eml_210 = "eml://ecoinformatics.org/eml-2.1.0";
    private static String format_eml_211 = "eml://ecoinformatics.org/eml-2.1.1";

    private static final String idPrefix = "mnTier1:";
    private static final String bogusId = "foobarbaz214";

    private static String currentUrl;
    
    private static Map<String,ObjectList> listedObjects;
    

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 1 API methods";
	}

	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 */
	@Before
	public void prefetchObjects() {
		log.debug("~~~~~ prefectching Objects");
		if (listedObjects == null) {
			listedObjects = new Hashtable<String,ObjectList>();
			Iterator<Node> it = getMemberNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				log.info("prefecthing objects from: " + currentUrl);
				MNode mn = D1Client.getMN(currentUrl);
				try {
					ObjectList ol = mn.listObjects(null, null, null, null, null, 0, 10);
					listedObjects.put(currentUrl, ol);
					log.info("~~~~ objects fetched: " + ol.getCount());
				} 
				catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
				}
				catch(Exception e) {
					log.warn(e.getClass().getName() + ": " + e.getMessage());
				}	
			}
		}
	}
	
	
	private ObjectInfo getPrefetchedObject(String currentUrl, int index)
	{
		if (listedObjects.get(currentUrl).getCount() == 0) {
			throw new IndexOutOfBoundsException();
		}
		if (index < 0) {
			// return off the right end of the list
			index = listedObjects.get(currentUrl).getCount() + index;
		}
		ObjectList ol = listedObjects.get(currentUrl);
		return ol.getObjectInfo(index);
	}
	
	
//    @Test
    public void testSetup() {
    	System.out.println("text/csv: " + format_text_csv);
    	System.out.println("text_eml_200: " + format_eml_200);
    	System.out.println("text_eml_201: " + format_eml_201);
    	System.out.println("text_eml_210: " + format_eml_210);
    	System.out.println("done");
    }
	
		
	@Test
	public void testMNCore_Ping() {
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testPing() vs. node: " + currentUrl);
		
			try {
				boolean pingSuccess = mn.ping();
				checkTrue(currentUrl,"ping response cannot be false. [Only true or exception].", pingSuccess);
			} 
			catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
    @Test
    public void testMNCore_GetLogRecords()
    {
       Iterator<Node> it = getMemberNodeIterator();
       while (it.hasNext()) {
    	   currentUrl = it.next().getBaseURL();
           MNode mn = D1Client.getMN(currentUrl);           
           printTestHeader("testGetLogRecords() vs. node: " + currentUrl);

           log.info("current time is: " + new Date());
           Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
           log.info("fromDate is: " + fromDate);

           try {
        	   Log eventLog = mn.getLogRecords(null, fromDate, null, null, null, null);
        	   checkTrue(currentUrl,"getOperationStatistics returns a log datatype", eventLog != null);
        	   
        	   // check that log events are created
        	   Identifier pid = new Identifier();
        	   pid.setValue(bogusId);
        	   boolean canCreate = false;
        	   try {
        		   canCreate = mn.isAuthorized(null, pid, Permission.WRITE);
        	   } catch (Exception e) {
        		   // do nothing - can't expect to create in Tier1 tests
        		   log.info("Cannot create objects so skipping more precise logging test "
        				   + "on node: " + currentUrl);
        	   }
        	   if (canCreate) { 
        		  pid = ExampleUtilities.doCreateNewObject(mn, idPrefix);
        		  Date toDate = new Date(System.currentTimeMillis());
        		  log.info("toDate is: " + toDate);
        		  
        		  eventLog = mn.getLogRecords(null, fromDate, toDate, Event.CREATE, null, null);
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
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	           
       }
    }
	
	
//    @Ignore("client implementation deferred")
//    @Test 
//    public void testGetObjectStatistics() {
//
//    }
	
    
    @Test
    public void testMNCore_GetOperationStatistics() {
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testGetOperationStatistics() vs. node: " + currentUrl);
		
				
			try {
				MonitorList ml = mn.getOperationStatistics(null, null, null, null, null, null);
				checkTrue(currentUrl,"getOperationStatistics returns a monitorList", ml != null);

				// test using all optional parameters
				Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				Subject requestor = new Subject();
				requestor.setValue("validSubject");
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
				ml = mn.getOperationStatistics(null, startTime, endTime, requestor, Event.READ, formatId);

			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}	
    }

    
//    @Ignore("client implementation deferred")
//    @Test
//    public void testGetStatus() {
//    	
//    }
    
    
    @Test
    public void testMNCore_GetCapabilities() {
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testGetCapabilities() vs. node: " + currentUrl);

    		try {
    			Node node = mn.getCapabilities();
    			checkTrue(currentUrl,"getCapabilities returns a Node", node != null);
    		} 
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testMNRead_Get() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);    			
    			log.debug("   pid = " + oi.getIdentifier());
    			InputStream is = mn.get(null,oi.getIdentifier());
    			checkTrue(currentUrl,"get() returns an objectStream", is != null);
    		}
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testMNRead_GetSystemMetadata() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);

    		try {
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);
    			log.debug("   pid = " + oi.getIdentifier());
    			SystemMetadata smd = mn.getSystemMetadata(null,oi.getIdentifier());
    			checkTrue(currentUrl,"getSystemMetadata() returns a SystemMetadata object", smd != null);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testMNRead_Describe() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testDescribe() vs. node: " + currentUrl);

    		try {
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);
    			log.debug("   pid = " + oi.getIdentifier());    				
    			DescribeResponse dr = mn.describe(null,oi.getIdentifier());
    			checkTrue(currentUrl,"describe() returns a DescribeResponse object", dr != null);	
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

    @Test
    public void testMNRead_GetChecksum() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testDescribe() vs. node: " + currentUrl);

    		try {
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);
    			log.debug("   pid = " + oi.getIdentifier());				
    			Checksum cs = mn.getChecksum(null,oi.getIdentifier(),CHECKSUM_ALGORITHM);
    			checkTrue(currentUrl,"getChecksum() returns a Checksum object", cs != null);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testMNRead_ListObjects() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ls = mn.listObjects();
    			checkTrue(currentUrl,"listObjects() returns an ObjectList", ls != null);
    			
    			Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
    			Boolean replicaStatus = true;
				ls = mn.listObjects(null, startTime, endTime, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(currentUrl,"listObjects(<parameters>) returns an ObjectList", ls != null);
    		} 
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    @Test
    public void testMNRead_SynchronizationFailed() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		printTestHeader("testSynchronizationFailed() vs. node: " + currentUrl);

    		try {
    			mn.synchronizationFailed(null, new SynchronizationFailed("0","a message"));
    			checkTrue(currentUrl,"synchronizationFailed() does not throw exception", true);
    		} 
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
}
