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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
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
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.EncodingUtilities;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier1IT extends ContextAwareTestCaseDataone {

    private static final String idPrefix = "mnTier1:";
    private static final String bogusId = "foobarbaz214";
    private static Identifier reservedIdentifier = null;

	private static final String badIdentifier = "ThisIdentifierShouldNotExist";
//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";
//	private static HashMap<String,String> StandardTests = new HashMap<String,String>();
	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	private static Vector<String> encodedPIDStrings = new Vector<String>();
	
	 private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;
  
	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 * @throws ServiceFailure 
	 */
	@Before
	public void setup() throws ServiceFailure {
//		prefetchObjects();
//		generateStandardTests();
	}


	public void prefetchObjects() throws ServiceFailure {
		if (listedObjects == null) {
			listedObjects = new Hashtable<String,ObjectList>();
			Iterator<Node> it = getCoordinatingNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				CNode cn = D1Client.getCN();
				try {
					ObjectList ol = cn.search(null, QUERYTYPE_SOLR, ""); 
					listedObjects.put(currentUrl, ol);
				} 
				catch (BaseException e) {
					handleFail(currentUrl,e.getDescription());
				}
				catch(Exception e) {
					log.warn(e.getClass().getName() + ": " + e.getMessage());
				}	
			}
		}
	}
	
	
	private ObjectInfo getPrefetchedObject(String currentUrl, Integer index)
	{
		if (index == null) 
			index = new Integer(0);
		if (index < 0) {
			// return off the right end of the list
			index = listedObjects.get(currentUrl).getCount() + index;
		}
		return listedObjects.get(currentUrl).getObjectInfo(index);
	}
	
	
	public void generateStandardTests() {

		if (testPIDEncodingStrings.size() == 0) {
			System.out.println(" * * * * * * * Unicode Test Strings * * * * * * ");
			
			InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
			Scanner s = new Scanner(is,"UTF-8");
			String[] temp;
			int c = 0;
			try
			{
				while (s.hasNextLine()) 
				{
					String line = s.nextLine();
					if (line.startsWith("common-") || line.startsWith("path-"))
					{
						System.out.println(c++ + "   " + line);
						temp = line.split("\t");
						if (temp.length > 1)
							testPIDEncodingStrings.add(temp[0]);
							encodedPIDStrings.add(temp[1]);
					}
				}	
				System.out.println("");
			} finally {
				s.close();
			}
			System.out.println("");
		}
	}
	
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


//    @Test
//    public void testGetLogRecords() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		Log response = cn.getLogRecords();
//		checkTrue(currentUrl,"getLogRecords(...) returns a Log object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }

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
        	   checkTrue(currentUrl,"getOperationStatistics returns a log datatype", eventLog != null);
        	   
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
 //       		     pid = ExampleUtilities.doCreateNewObject(cn, idPrefix);
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
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
    			log.debug("   pid = " + oi.getIdentifier());
    			String fragment = "CNodeTier1Test";
    			Identifier response = cn.generateIdentifier(null,"DOI",fragment);
    			checkTrue(currentUrl,"generateIdentifier(...) should return a Identifier object" +
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
    			ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
    			log.debug("   pid = " + oi.getIdentifier());
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
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);

    		try {
    			boolean response = false;
    			if (reservedIdentifier != null) {
    				response = cn.hasReservation(null,reservedIdentifier);
    			} else {
    				Identifier pid = new Identifier();
    				pid.setValue(ExampleUtilities.generateIdentifier());
    				cn.reserveIdentifier(null, pid );
    			}
    			// TODO:  test the return better
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


//    @Test
//    public void testCreate() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testCreate(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		Identifier response = cn.create();
//		checkTrue(currentUrl,"create(...) returns a Identifier object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testRegisterSystemMetadata() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testRegisterSystemMetadata(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		Identifier response = cn.registerSystemMetadata();
//		checkTrue(currentUrl,"registerSystemMetadata(...) returns a Identifier object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testListObjects() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testListObjects(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		ObjectList response = cn.listObjects();
//		checkTrue(currentUrl,"listObjects(...) returns a ObjectList object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testListObjects() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testListObjects(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		ObjectList response = cn.listObjects();
//		checkTrue(currentUrl,"listObjects(...) returns a ObjectList object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testGet() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testGet(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		InputStream response = cn.get();
//		checkTrue(currentUrl,"get(...) returns a InputStream object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testGetSystemMetadata() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testGetSystemMetadata(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		SystemMetadata response = cn.getSystemMetadata();
//		checkTrue(currentUrl,"getSystemMetadata(...) returns a SystemMetadata object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testDescribe() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testDescribe(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		DescribeResponse response = cn.describe();
//		checkTrue(currentUrl,"describe(...) returns a DescribeResponse object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }
//
//
//    @Test
//    public void testResolve() {
//	Iterator<Node> it = getCoordinatingNodeIterator();
//	while (it.hasNext()) {
//	    currentUrl = it.next().getBaseURL();
//	    CNode cn = new CNode(currentUrl);
//	    printTestHeader("testResolve(...) vs. node: " + currentUrl);
//	    
//	    try {
//		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//		log.debug("   pid = " + oi.getIdentifier());
//
//		ObjectLocationList response = cn.resolve();
//		checkTrue(currentUrl,"resolve(...) returns a ObjectLocationList object", response != null);
//		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//	    } 
//	    catch (IndexOutOfBoundsException e) {
//		handleFail(currentUrl,"No Objects available to test against");
//	    }
//	    catch (BaseException e) {
//		handleFail(currentUrl,e.getDescription());
//	    }
//	    catch(Exception e) {
//		e.printStackTrace();
//		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//	    }
//	}
//    }

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
