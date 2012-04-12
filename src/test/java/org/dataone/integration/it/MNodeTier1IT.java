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

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
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
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.DateTimeMarshaller;
import org.junit.Assume;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier1IT extends ContextAwareTestCaseDataone  {


    private  String format_text_csv = "text/csv";

    private static final String idPrefix = "mnTier1:";
    private static final String bogusId = "foobarbaz214";

    private  String currentUrl;
    

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 1 API methods";
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
				
				checkTrue(currentUrl,"ping should return a valid date", pingDate != null);
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
	
	
    @Test
    public void testGetLogRecords()
    {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getMemberNodeIterator();
       while (it.hasNext()) {
    	   currentUrl = it.next().getBaseURL();
           MNode mn = D1Client.getMN(currentUrl);  
           currentUrl = mn.getNodeBaseServiceUrl();
           printTestHeader("testGetLogRecords() vs. node: " + currentUrl);

           log.info("current time is: " + new Date());
           Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
           log.info("fromDate is: " + fromDate);

           try {
        	   Log eventLog = mn.getLogRecords(null, fromDate, null, null, null, null);
        	   checkTrue(currentUrl,"getOperationStatistics returns a log datatype", eventLog != null);

        	   // check that log events are created
        	   Node node = mn.getCapabilities();
        	   if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
//        		   Identifier pid = ExampleUtilities.doCreateNewObject(mn, idPrefix);
        		   Identifier pid = null;
        		   try {
        			   pid = createPublicTestObject(mn, null);
        		   } catch (BaseException be) {
        			   throw new TestIterationEndingException("Could not create a test object for the getLogRecords() test.", be);
        		   }
        		   Date toDate = new Date(System.currentTimeMillis());
        		   log.info("toDate is: " + toDate);

        		   eventLog = mn.getLogRecords(null, fromDate, toDate, Event.CREATE, null, null);
        		   log.info("log size: " + eventLog.sizeLogEntryList());
        		   boolean isfound = false;
        		   for (int i=0; i<eventLog.sizeLogEntryList(); i++)
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
        	   } else {
        		   // do nothing - can't expect to create in Tier1 tests
        		   log.info("Cannot create objects so skipping more precise logging test "
        				   + "on node: " + currentUrl);
        	   }
           }
           catch (TestIterationEndingException e) {
        	   handleFail(currentUrl, e.getMessage() + ":: cause: "
        			   + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
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
    public void testGetLogRecords_DateSlicing()
    {
    	// can be anyone
    	setupClientSubject("testRightsHolder");
    	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = new MNode(currentUrl);
    		printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);  
    		currentUrl = mn.getNodeBaseServiceUrl();

    		try {
    			Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
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
    				handleFail(currentUrl,"could not find 2 objects with different dateLogged times");
    			} else {
   				
    				// call with a fromDate
    				eventLog = mn.getLogRecords(null, fromDate, null, null, null, null);

    				for (LogEntry le : eventLog.getLogEntryList()) {
    					if (le.getEntryId().equals(excludedEntry.getEntryId())) {
    						handleFail(currentUrl,"entryID " + excludedEntry.getEntryId() +
    								" should not be in the event log where fromDate set to " + fromDate);
    						break;
    					}
    				}
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
    			checkTrue(currentUrl,"getCapabilities returns a Node", node != null);
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
    			checkTrue(currentUrl,"getCapabilities returns a Node", node != null);
    		
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
    			checkTrue(currentUrl,"the node should have at least one contactSubject that conforms to RFC2253.", found);
    			
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
    			checkTrue(currentUrl,"getCapabilities returns a Node", node != null);
    		
    			NodeReference nodeRef = node.getIdentifier();
    			checkTrue(currentUrl,"the node identifier should conform to specification 'urn:node:[\\w_]{2,23}'",
    					nodeRef.getValue().matches("^urn:node:[\\w_]{2,23}"));
    			
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
     * Tests the parameterless and parameterized listObject methods for propert returns.
     */
    @Test
    public void testListObjects() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(mn);//.listObjects(null);
    			checkTrue(currentUrl,"listObjects() should return an ObjectList", ol != null);
    			
    			Date startTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000);
				Date endTime = new Date(System.currentTimeMillis() - 1 * 60 * 1000);
				ObjectFormatIdentifier formatId = new ObjectFormatIdentifier();
				formatId.setValue(format_text_csv);
    			Boolean replicaStatus = true;
				ol = mn.listObjects(null, startTime, endTime, 
						formatId, replicaStatus , 
						Integer.valueOf(0),
						Integer.valueOf(10));
    			checkTrue(currentUrl,"listObjects(<parameters>) returns an ObjectList", ol != null);
    		} 
    		catch (TestIterationEndingException e) {
    			handleFail(currentUrl, e.getMessage() + ":: cause: "
    					+ e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
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
     * Tests that the fromDate parameter successfully filters out records where
     * the systemMetadataModified date/time is earler than fromDate.
     */
    @Test
    public void testListObjects_FromDateTest() {
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testListObjects() vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(mn);
    			checkTrue(currentUrl,"listObjects() should return an ObjectList", ol != null);
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
   							handleFail(currentUrl,String.format("identifier %s with sysMetaModified date of '%s'" +
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
     * Tests that the formatID parameter successfully filters records by
     * the given formatId
     */
    @Test
    public void testListObjects_FormatIdFilteringTest() {
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
    				ol = mn.listObjects(null, null, null, null, null, 0, ol.getTotal());
    			}
    			checkTrue(currentUrl,"listObjects() should return an ObjectList", ol != null);
    			if (ol.getTotal() == 0)
    				throw new TestIterationEndingException("no objects found in listObjects");
    			ObjectFormatIdentifier excludedFormat = ol.getObjectInfo(0).getFormatId();
    			for (ObjectInfo oi : ol.getObjectInfoList()) {
    				if (!oi.getFormatId().equals(excludedFormat)) {
    					ObjectList ol2 = mn.listObjects(null, null, null, oi.getFormatId(),
    	   						null, null, null);
    	   				checkTrue(currentUrl,"objectList filtered by " + oi.getFormatId().getValue() +
    	   				      " should contain fewer objects than unfiltered", 
    	   				      ol2.getTotal() <  ol.getTotal()
    	   				      );
    	   				break;
    				}
    			}
    			
   				// call listObjects with a fake format
   				ol = mn.listObjects(null, null, null, D1TypeBuilder.buildFormatIdentifier("fake_format"),
   						null, null, null);
   				if (ol.getTotal() == 0) {
   					handleFail(currentUrl,"filtering the object list by a fake " +
   							"format should return zero objects");
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
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGet() vs. node: " + currentUrl);

    		try {
    			Identifier id = procurePublicReadableTestObject(mn);
    			InputStream is = mn.get(null,id);
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
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetSystemMetadata() vs. node: " + currentUrl);
    		
    		try {
    			Identifier id = procurePublicReadableTestObject(mn);
    			SystemMetadata smd = mn.getSystemMetadata(null,id);
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
    		MNode mn = D1Client.getMN(currentUrl); 
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testDescribe() vs. node: " + currentUrl);
		
    		try {
    			Identifier id = procurePublicReadableTestObject(mn);
    			DescribeResponse dr = mn.describe(null,id);
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
    public void testGetChecksum() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testGetChecksum() vs. node: " + currentUrl);

    		try {   
    			Identifier id = procurePublicReadableTestObject(mn);
    			Checksum cs = mn.getChecksum(null,id,CHECKSUM_ALGORITHM);
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
    public void testSynchronizationFailed_NoCert() {
    	setupClientSubject_NoCert();
       	Iterator<Node> it = getMemberNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		MNode mn = D1Client.getMN(currentUrl);
    		currentUrl = mn.getNodeBaseServiceUrl();
    		printTestHeader("testSynchronizationFailed() vs. node: " + currentUrl);
 		
    		try {
    			Identifier id = procurePublicReadableTestObject(mn);
    			SynchronizationFailed sf = new SynchronizationFailed("0","a message",id.getValue(),null);
    			System.out.println(sf.serialize(SynchronizationFailed.FMT_XML));
    			mn.synchronizationFailed(null, 
    					new SynchronizationFailed("0","a message",id.getValue(),null));
    			checkTrue(currentUrl,"synchronizationFailed() does not throw exception", true);
    		}
    		catch (NotAuthorized e) {
    			; // this is an acceptable (and preferrable) outcome for calling without a client cert.
    		}
    		catch (IndexOutOfBoundsException e) {
    			handleFail(currentUrl,"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + 
    					e.getDetail_code() + " " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
}
