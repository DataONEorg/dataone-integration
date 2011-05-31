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

package org.dataone.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.MNode;
import org.dataone.eml.DataoneEMLParser;
import org.dataone.eml.EMLDocument;
import org.dataone.eml.EMLDocument.DistributionMetadata;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Checksum;
import org.dataone.service.types.ChecksumAlgorithm;
import org.dataone.service.types.DescribeResponse;
import org.dataone.service.types.Event;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.Log;
import org.dataone.service.types.LogEntry;
import org.dataone.service.types.Node;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectInfo;
import org.dataone.service.types.ObjectList;
import org.dataone.service.types.SystemMetadata;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeObjectLifecycleIT  {
    private static final String devPrincipal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
    private static final String pw = "kepler";
    private static final String prefix = "simpleApiTests:testid:";
    private static final String bogusId = "foobarbaz214";
    private static final String knownId = "repl:testID201120161032499";

    private List<Node> nodeList = null;
    private Hashtable nodeInfo = null;
    private static String currentUrl;
    //set this to false if you don't want to use the node list to get the urls for 
    //the test.  
    private static boolean useNodeList = false;
        
    @Rule 
    public ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setUp() throws Exception 
    {        
    	nodeInfo = new Hashtable();
    	nodeInfo.put("http://cn-dev.dataone.org/knb/d1/", new String[] {devPrincipal, pw});
//    	nodeInfo.put("http://gmn-dev.dyndns.org/mn/", new String[] {"public","public"});
//    	nodeInfo.put("http://dev-dryad-mn.dataone.org/mn/", new String[] {"",""});
//    	nodeInfo.put("http://daacmn.dataone.utk.edu/mn/", new String[] {"",""});
    }

    
    @Test
    public void exerciseNodeAPIs() throws IOException {
    	Enumeration<String> nodeBaseUrls = nodeInfo.keys();

		printHeader("Simple object lifecycle tests");
    	while( nodeBaseUrls.hasMoreElements() ) 
    	{
    		String currentBaseUrl = nodeBaseUrls.nextElement();
    		currentUrl = currentBaseUrl;
    		String[] currentNodeInfo = (String[]) nodeInfo.get(currentBaseUrl);
    		String logon = currentNodeInfo[0];
    		String cred = currentNodeInfo[1];
    		
    		printHeader("Node: " + currentBaseUrl);
    		MNode mn = new MNode(currentBaseUrl);

    		// objectlist - count
    		printSubHeader(" listObjects");
    		ObjectList ol = null;
    		try {
    			ol = mn.listObjects(null, null, null, null, false, 0, 10);
    		} catch (Exception e) {
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.listObjects: " + e.getMessage()));
    		}
    		int origObjectCount = ol.getTotal();
    		System.out.println("=========>> total from listObjects: " + origObjectCount);

    		// login if necessary
    		AuthToken token = null;
    		try { 
    			printSubHeader("login");
    			if (!logon.isEmpty()) {
    				token = mn.login(logon, cred);
    			} else
    				System.out.println("Skipping login, no credentials for current node");
    		} catch (Exception e) {
    			System.out.println("error in login: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.login: " + e.getMessage()));
    		}
    		
    		// assemble new object and systemMetadata (client-side)
    		String idString = prefix + ExampleUtilities.generateIdentifier();
    		Identifier newPid = new Identifier();
    		newPid.setValue(idString);

    		InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
    		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(newPid, ObjectFormat.TEXT_CSV, objectStream);
    		objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
    		Identifier rGuid = null;
    		

    		try {
    			printSubHeader("create");
    			rGuid = mn.create(token, newPid, objectStream, sysmeta);
    			assertThat("checking that returned guid matches given ", newPid.getValue(), is(rGuid.getValue()));
    			System.out.println("new object PID: " + rGuid.getValue());
    		} catch (Exception e) {
    			System.out.println("error in mn.create: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.create or mn.get: " + e.getMessage()));
    		}
    		
    		String createdDataObject = null;
    		try {
    			printSubHeader("get (using token)");
    			InputStream is = mn.get(token,newPid);
    			String x = IOUtils.toString(is);
    			createdDataObject = x;
    			System.out.println(" get response:  returned data stream length = " + x.length());
    			checkTrue("object returned is not null",x.length()>0);
    		} catch (Exception e) {
    			System.out.println("error in mn.get: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.get: " + e.getMessage()));
    		}
    		
       		try {
    			printSubHeader("setAccess");
    			System.out.println("Testing public access prior to making public...");
    			try {
    				InputStream is = mn.get(null,newPid);
    				errorCollector.addError(new Throwable(createAssertMessage() + 
    					" mn.get should have failed since the doc does not have public access."));
    			} catch (Exception e) {}
    			System.out.println("Setting access...");
    			mn.setAccess(token, newPid, "public", "read", "allow", "allowFirst");
    		} catch (Exception e) {
    			System.out.println("error in mn.setAccess: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.create or mn.get: " + e.getMessage()));
    		}

    		try {
    			printSubHeader("get (null token / public)");
    			InputStream is = mn.get(null,newPid);
    			String x = IOUtils.toString(is);
    			System.out.println("Testing get (public):  returned data stream length = " + x.length());
    			checkTrue("object returned is not null",x.length()>0);
    		} catch (Exception e) {
    			System.out.println("error in mn.get: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.get: " + e.getMessage()));
    		}

 
    		try {
        		printSubHeader("getSystemMetadata");
    			SystemMetadata smd = mn.getSystemMetadata(null, newPid);
    			System.out.println("systemMetadata object size: " + smd.getSize());
    			System.out.println(smd.toString());
    			checkTrue("pid in sysMeta document matches requested pid",
    					newPid.getValue().equals(smd.getIdentifier().getValue()));
//    			assertTrue("smd object not null", smd != null);
    		} catch (Exception e) {
    			System.out.println("error in mn.getSystemMetadata: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.getSystemMetadata: " + e.getMessage()));
    		}
    		
    		// describe on the object
    		try {
        		printSubHeader("describe");
        		DescribeResponse dr = mn.describe(token, newPid);
        		Checksum cs = dr.getDataONE_Checksum();
        		checkTrue("",cs.getValue().equals(sysmeta.getChecksum().getValue()));
        		checkTrue("",dr.getContent_Length() == sysmeta.getSize());
        		checkTrue("",dr.getDataONE_ObjectFormat().toString().equals(sysmeta.getObjectFormat().toString())); 
    		} catch (Exception e) {
    			System.out.println("error in mn.getSystemMetadata: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.describe: " + e.getMessage()));
    		}
    		
    		
    		
    		// getChecksum
    		try {
        		printSubHeader("getChecksum");
    			Checksum cs = mn.getChecksum(null, newPid);
    			System.out.println("checksum object value: " +  cs.getValue());
    			assertTrue("checksum object not null", cs != null);
    		} catch (Exception e) {
    			System.out.println("error in mn.getChecksum: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.checksum: " + e.getMessage()));
    		}
 
    		printSubHeader("checking new object list count");
    		int c = getCountFromListObjects( mn,  token);
    		System.out.println("=========>> total from listObjects: " + c);
    		checkEquals(" *~*~*~* objectlist should be one more than before the create", origObjectCount + 1, c); 
 
    		// search
    		// deferring this one - do MNs have search?

    		
    		// update - change guid
    		Identifier updatedPid = null;
    		try {
    			printSubHeader("update");
                //alter the document
                updatedPid = new Identifier();
                updatedPid.setValue(prefix + ExampleUtilities.generateIdentifier());
                createdDataObject = createdDataObject.replaceAll("61", "0");
                objectStream = IOUtils.toInputStream(createdDataObject);
                SystemMetadata updatedSysmeta = ExampleUtilities.generateSystemMetadata(updatedPid, ObjectFormat.TEXT_CSV, objectStream);
                objectStream = IOUtils.toInputStream(createdDataObject);
                
                //update the document
                System.out.println("d1 update new pid: "+ updatedPid.getValue() + " old pid: " + newPid.getValue());
                Identifier uPid = mn.update(token, rGuid, objectStream, updatedPid, updatedSysmeta);
                System.out.println("d1 updated success, id returned is " + uPid.getValue());

                //perform tests
                InputStream updatedData = mn.get(token, uPid);
                checkTrue("",null != updatedData);
                String str = IOUtils.toString(updatedData);
                checkTrue("updated values returned",str.indexOf("0 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
                updatedData.close();
    		} catch (Exception e) {
    			System.out.println("error in mn.update: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.update: " + e.getMessage()));
    		}
       		printSubHeader("checking object list count after update");
    		c = getCountFromListObjects( mn,  token);
    		System.out.println("=========>> total from listObjects: " + c);
    		checkEquals(" *~*~*~* objectlist should be +1 after the update", origObjectCount + 2, c); 
    		// but a search will not have the old object in the index
    		
    		
    		// delete
    		try {
    			printSubHeader("delete");
    			Identifier delId = mn.delete(token, newPid);
                checkTrue("",delId.getValue().equals(newPid.getValue()));
                System.out.println("Deleted original pid: " + newPid.getValue());
    		} catch (Exception e) {
    			System.out.println("error in mn.delete: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.delete: " + e.getMessage()));
    		}
  
      		printSubHeader("checking object list count after delete");
    		c = getCountFromListObjects( mn,  token);
    		System.out.println("=========>> total from listObjects: " + c);
    		checkEquals(" *~*~*~* objectlist should be same after delete as before delete (zero impact)", origObjectCount + 2, c);
    		
    		
    		
    		
    //		testCreateDescribedDataAndMetadata(mn,  token);

    		
    	}
    }
    
    // =============================================================================//
    
    private AuthToken doAccessDependentTests(MNode mn, String logon, String cred) throws ServiceFailure, NotImplemented {
    	
    	AuthToken token = mn.login(logon, cred);

    	printSubHeader("test create failure" + mn.getNodeBaseServiceUrl());
    	testFailedCreate(mn,token);
    	testGetLogRecords(mn,token);
    	return token;
    }
    
    
    // =============================================================================//
    
    private void doAccessIndependentTests(MNode mn, ObjectList ol) throws InvalidToken, ServiceFailure, NotAuthorized, NotFound, NotImplemented {
    	Identifier pid = ol.getObjectInfoList().get(0).getIdentifier();
    	System.out.println("    using pid: " + pid.getValue());
//    	testListObjects(mn, pid);
    }
    
    
    /**
     * test the failed creation of a doc
     */
    protected void testFailedCreate(MNode mn, AuthToken token)
    {
    	checkTrue("",true);

    	String idString = prefix + ExampleUtilities.generateIdentifier();
    	Identifier guid = new Identifier();
    	guid.setValue(idString);
    	InputStream objectStream = this.getClass().getResourceAsStream(
    			"/d1_testdocs/knb-lter-luq.76.2-broken.xml");
    	SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream);
    	objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-luq.76.2-broken.xml");

    	Identifier rGuid = null;

    	try {
    		rGuid = mn.create(token, guid, objectStream, sysmeta);
    		errorCollector.addError(new Throwable(createAssertMessage() + 
    				" Should have thrown exception since the xml file created was currupt"));
    	} catch (Exception e) {
    	}
    }



    protected void testGetLogRecords(MNode mn, AuthToken token)
    {
    	printSubHeader("testGetLogRecords");
    	System.out.println("current time is: " + new Date());
    	try
    	{
    		Date start = new Date(System.currentTimeMillis() - 500000);
    		String idString = prefix + ExampleUtilities.generateIdentifier();
    		Identifier guid = new Identifier();
    		guid.setValue(idString);
    		InputStream objectStream = this.getClass().getResourceAsStream(
    				"/d1_testdocs/knb-lter-cdr.329066.1.data");
    		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(
    				guid, ObjectFormat.TEXT_CSV, objectStream);
    		objectStream = this.getClass().getResourceAsStream(
    				"/d1_testdocs/knb-lter-cdr.329066.1.data");

    		Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
    		InputStream data = mn.get(token, rGuid);
    		String str = IOUtils.toString(data);
    		//System.out.println("str: " + str);
    		checkTrue("",str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
    		checkEquals("",guid.getValue(), rGuid.getValue());

    		//get the logs for the last minute
    		Date end = new Date(System.currentTimeMillis() + 500000);
    		System.out.println("start: " + start + " end: " + end);
    		Log log = mn.getLogRecords(token, start, end, Event.CREATE);
    		System.out.println("log size: " + log.sizeLogEntryList());
    		boolean isfound = false;
    		for(int i=0; i<log.sizeLogEntryList(); i++)
    		{ //check to see if our create event is in the log
    			LogEntry le = log.getLogEntry(i);
    			//System.out.println("le: " + le.getIdentifier().getValue());
    			//System.out.println("rGuid: " + rGuid.getValue());
    			if(le.getIdentifier().getValue().trim().equals(rGuid.getValue().trim()))
    			{
    				isfound = true;
    				System.out.println("log record found");
    				break;
    			}
    		}
    		System.out.println("isfound: " + isfound);
    		checkTrue("",isfound);

    	} 
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		errorCollector.addError(new Throwable(createAssertMessage() + 
    				" threw an unexpected exception: " + e.getMessage()));
    	}
    }
    
 
    /**
     * test the creation of the desribes and describedBy sysmeta elements
     */
    protected void testCreateDescribedDataAndMetadata(MNode mn, AuthToken token)
    {
    	try
    	{
    		//parse that document for distribution info
    		//Test EML 2.0.0
    		InputStream is = this.getClass().getResourceAsStream(
    		"/d1_testdocs/eml200/dpennington.195.2");
    		DataoneEMLParser parser = DataoneEMLParser.getInstance();
    		EMLDocument emld = parser.parseDocument(is);
    		checkEquals("",ObjectFormat.EML_2_0_0.toString(), emld.format.toString());
    		DistributionMetadata dm = emld.distributionMetadata.elementAt(0);
    		checkEquals("",ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
    		checkEquals("",dm.url, "ecogrid://knb/IPCC.200802107062739.1");
    		insertEMLDocsWithEMLParserOutput(mn, emld, "dpennington.195.2", token, is);

    		//Test EML 2.0.1
    		is = this.getClass().getResourceAsStream("/d1_testdocs/eml201/msucci.23.3");
    		parser = DataoneEMLParser.getInstance();
    		emld = parser.parseDocument(is);
    		checkEquals("",ObjectFormat.EML_2_0_1.toString(), emld.format.toString());
    		dm = emld.distributionMetadata.elementAt(0);
    		checkEquals("",ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
    		checkEquals("",dm.url, "ecogrid://knb/msucci.24.1");
    		insertEMLDocsWithEMLParserOutput(mn, emld, "msucci.23.3", token, is);

    		//Test EML 2.1.0
    		is = this.getClass().getResourceAsStream("/d1_testdocs/eml210/peggym.130.4");
    		parser = DataoneEMLParser.getInstance();
    		emld = parser.parseDocument(is);
    		checkEquals("",ObjectFormat.EML_2_1_0.toString(), emld.format.toString());
    		dm = emld.distributionMetadata.elementAt(0);
    		checkEquals("",ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
    		checkEquals("",dm.url, "ecogrid://knb/peggym.127.1");
    		dm = emld.distributionMetadata.elementAt(1);
    		checkEquals("",ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
    		checkEquals("",dm.url, "ecogrid://knb/peggym.128.1");
    		dm = emld.distributionMetadata.elementAt(2);
    		checkEquals("",ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
    		checkEquals("",dm.url, "ecogrid://knb/peggym.129.1");
    		insertEMLDocsWithEMLParserOutput(mn, emld, "peggym.130.4", token, is);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		errorCollector.addError(new Throwable(createAssertMessage() + 
    				" error in testCreateDescribedDataAndMetadata: " + e.getMessage()));
    	}
    }
   
    /**
     * test creation of data with challenging unicode identifier.
     * this also tests get() since it
     * is used to verify the inserted metadata
     */
//    @Test
    public void testCreateData_IdentifierEncoding() 
    {
    	printHeader("Testing IdentifierEncoding");

    	Vector<String> unicodeString = new Vector<String>();
    	Vector<String> escapedString = new Vector<String>();
//   TODO: test against Unicode characters when metacat supports unicode    	
//    	InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
    	InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
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
    				unicodeString.add(temp[0]);
    				escapedString.add(temp[1]);		
    			}
    		}
    	} finally {
    		s.close();
    	}

    	for(int i=0; i<nodeList.size(); i++)
    	{
    		currentUrl = nodeList.get(i).getBaseURL();

    		Vector<String> nodeSummary = new Vector<String>();
    		nodeSummary.add("Node Test Summary for node: " + currentUrl );

    		printHeader("  Node:: " + currentUrl);
    		MNode mn = new MNode(currentUrl);

    		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
            AuthToken token = null;
            try
            {
                token = mn.login(principal, "kepler");
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " Error loggin in for testCreateData_IdentifierEncoding: " + e.getMessage()));
            }
            
    		for (int j=0; j<unicodeString.size(); j++) 
    		{
    			String status = "OK   ";

    			//    			String unicode = unicodeString.get(j);
    			System.out.println();
    			System.out.println(j + "    unicode String:: " + unicodeString.get(j));
    			String idString = prefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
    			String idStringEscaped = prefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);

    			try
    			{
    				checkTrue("",true);
    				
    				Identifier guid = new Identifier();
    				guid.setValue(idString);
    				InputStream objectStream = this.getClass().getResourceAsStream(
    						"/d1_testdocs/knb-lter-cdr.329066.1.data");
    				SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream);
    				objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
    				Identifier rGuid = null;

    				// rGuid is either going to be the escaped ID or the non-escaped ID
    				try {
    					rGuid = mn.create(token, guid, objectStream, sysmeta);
    					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
    					mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
    					checkEquals("",guid.getValue(), rGuid.getValue());
    					InputStream data = mn.get(token, rGuid);
    					checkTrue("",null != data);
    					String str = IOUtils.toString(data);
    					checkTrue("",str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
    					data.close();
    				} catch (Exception e) {
    					status = "Error";
    					System.out.println("error message: " + e.getMessage());
    					//	e.printStackTrace();
    					errorCollector.addError(new Throwable(createAssertMessage() + 
    							" error in testCreateData: " + e.getMessage()));
    				} 
    			}
    			catch(Exception ee)
    			{
    				status = "Error";
    				ee.printStackTrace();
    				errorCollector.addError(new Throwable(createAssertMessage() + 
    						" unexpected error in testCreateData_unicodeIdentifier: " + ee.getMessage()));
    			}
    			nodeSummary.add("Test " + j + ": " + status + ":  " + unicodeString.get(j));
    		}
    		System.out.println();
    		for (int k=0; k<nodeSummary.size(); k++) 
    		{
    			System.out.println(nodeSummary.get(k));
    		}
    		System.out.println();
    	}
    }

    
 //   @Test
    public void testGetChecksumAuthTokenIdentifierTypeString() 
    {
        //create a doc
        //calculate checksum
        //create
        //getChecksum
        //check the two checksums
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = new MNode(currentUrl);

            try
            {
                printHeader("testGetChecksumAuthTokenIdentifierTypeString - node " + nodeList.get(i).getBaseURL());
                checkTrue("",true);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-luq.76.2.xml");
                String doc = IOUtils.toString(objectStream);
                Checksum checksum1 = new Checksum();
                String checksum1str = ExampleUtilities.checksum(IOUtils.toInputStream(doc), "MD5");
                checksum1.setValue(checksum1str);
                checksum1.setAlgorithm(ChecksumAlgorithm.MD5);
                System.out.println("Checksum1: " + checksum1.getValue());
                objectStream.close();
                
                String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
                AuthToken token = mn.login(principal, "kepler");
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                objectStream = IOUtils.toInputStream(doc);
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream);
                objectStream = IOUtils.toInputStream(doc);
                sysmeta.setChecksum(checksum1);
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals("",guid.getValue(), rGuid.getValue());
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateScienceMetadata: " + e.getMessage()));
                }
                
                try 
                {
                    Checksum checksum2 = mn.getChecksum(token, rGuid, "MD5");
                    System.out.println("Checksum2: " + checksum2.getValue());
                    checkEquals("",checksum1.getValue(), checksum2.getValue());
                } 
                catch (Exception e) 
                {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testGetChecksumAuthTokenIdentifierTypeString: " + e.getMessage()));
                }
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testGetChecksumAuthTokenIdentifierTypeString: " + e.getMessage()));
            }
        }
    }
    
    
 
    


    /**
     * this method is an example of how to use the EMLParser output to
     * create system metadata for eml files.
     * 
     * @param emld
     * @param file
     * @param token
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     */
    private void insertEMLDocsWithEMLParserOutput(MNode mn, EMLDocument emld, String file, AuthToken token, InputStream is) 
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
        UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented
    {
        String dirname;
        if(emld.format == ObjectFormat.EML_2_0_0)
            dirname = "eml200";
        else if(emld.format == ObjectFormat.EML_2_0_1)
            dirname = "eml201";
        else if(emld.format == ObjectFormat.EML_2_1_0)
            dirname = "eml210";
        else
            dirname = "";
        
        //create an ID for the metadata doc
        String idString = ExampleUtilities.generateIdentifier();
        Identifier mdId = new Identifier();
        mdId.setValue(idString);
        
        SystemMetadata mdSm = ExampleUtilities.generateSystemMetadata(mdId, emld.format, is);

        //get the document(s) listed in the EML distribution elements
        //for the sake of this method, we're just going to get them from the resources directory
        //in an actual implementation, this would get the doc from the server
        for(int i=0; i<emld.distributionMetadata.size(); i++)
        { 
            String url = emld.distributionMetadata.elementAt(i).url;
            if(url.startsWith("ecogrid://knb"))
            { //just handle ecogrid uris right now
                url = url.substring(url.indexOf("ecogrid://knb/") + "ecogrid://knb/".length(), url.length());
            }
            else
            {
                System.out.println("Attempting to describe " + url + ", however ");
                System.out.println("Describes/DescribesBy can only handle ecogrid:// urls at this time.");
                continue;
            }

            InputStream instream = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + url);
            
            //create Identifiers for each document
            idString = ExampleUtilities.generateIdentifier();
            idString += i;
            Identifier id = new Identifier();
            id.setValue(idString);
            //create system metadata for the dist documents with a describedBy tag
            SystemMetadata sm = ExampleUtilities.generateSystemMetadata(id, 
                    ObjectFormat.convert(emld.distributionMetadata.elementAt(i).mimeType), instream);
            //add desrviedBy
            sm.addDescribedBy(mdId);
            //add describes to the metadata doc's sm
            mdSm.addDescribe(id);
            //TODO: replace this with a call to the server eventually
            instream = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + url);

            Identifier createdDataId = mn.create(token, id, instream, sm);
            mn.setAccess(token, createdDataId, "public", "read", "allow", "allowFirst");
            checkEquals("",createdDataId.getValue(), id.getValue());
            System.out.println("Data ID: " + id.getValue());
        }
        
        //send the EML doc to create
        is = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + file);
        Identifier createdMdId = mn.create(token, mdId, is, mdSm);
        mn.setAccess(token, createdMdId, "public", "read", "allow", "allowFirst");
        checkEquals("",createdMdId.getValue(), mdId.getValue());
        System.out.println("Metadata ID: " + createdMdId.getValue());
    }
    
    private int getCountFromListObjects(MNode mn, AuthToken token)
    {
    	ObjectList ol = null;
		try {
			ol = mn.listObjects(null, null, null, null, false, 0, 10);
		} catch (Exception e) {
			errorCollector.addError(new Throwable(createAssertMessage() + 
					" error in getCountFromListObjects: " + e.getMessage()));
		}
		int count = ol.getTotal();
		System.out.println("total from listObjects: " + count);
		return count;
    }
    
    
    private static String createAssertMessage()
    {
        return "test failed at url " + currentUrl;
    }
    
    private void printHeader(String header)
    {
        System.out.println("\n***************** " + header + " *****************");
    }
    
    private void printSubHeader(String sh)
    {
        System.out.println("\n:::::::::: " +  sh + " ::::::::::::::::::");
    }
    
    private void checkEquals(final String msg, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(msg, s1, is(s2));
                //assertThat("assertion failed for host " + currentUrl, s1, is(s2 + "x"));
                return null;
            }
        });
    }
 
    private void checkEquals(final String msg, final int i1, final int i2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(msg, i1, is(i2));
                //assertThat("assertion failed for host " + currentUrl, s1, is(s2 + "x"));
                return null;
            }
        });
    }
   
    private void checkTrue(final String msg, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(msg, true, is(b));
                return null;
            }
        });
    }
}
