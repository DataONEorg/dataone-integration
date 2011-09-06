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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
import org.dataone.client.D1Client;
import org.dataone.client.D1RestClient;
import org.dataone.client.MNode;
import org.dataone.client.ObjectFormatCache;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.LogEntry;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.D1Url;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Test the DataONE Java client methods.
 * @author Matthew Jones
 */
public class MNodeIT extends ContextAwareTestCaseDataone  {

    private static final String TEST_MN_ID = "c3p0";
    private static String format_text_csv = "text/csv";
    private static String format_eml_200 = "eml://ecoinformatics.org/eml-2.0.0";
    private static String format_eml_201 = "eml://ecoinformatics.org/eml-2.0.1";
    private static String format_eml_210 = "eml://ecoinformatics.org/eml-2.1.0";
    private static String format_eml_211 = "eml://ecoinformatics.org/eml-2.1.1";

    private static final String idPrefix = "knb:testid:";
    private static final String bogusId = "foobarbaz214";

    private static String currentUrl;

    

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node service api";
	}
    
    
//    @Test
    public void testSetup() {
    	System.out.println("text/csv: " + format_text_csv);//.getFormatName());
    	System.out.println("text_eml_200: " + format_eml_200);//.getFormatName());
    	System.out.println("text_eml_201: " + format_eml_201);//.getFormatName());
    	System.out.println("text_eml_210: " + format_eml_210);//.getFormatName());
    	System.out.println("done");
    }
    
    /**
     * test the failed creation of a doc
     */
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testFailedCreate()
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            try
            {
                printTestHeader("testFailedCreate - node " + memberNodeList.get(i).getBaseURL());
                checkTrue(true);
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2-broken.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-luq.76.2-broken.xml");
                
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " Should have thrown exception since the xml file created was currupt"));
                    // just incase both statements above do not throw exceptions...
                    mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                } catch (Exception e) {
                }
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testFailedCreate: " + e.getMessage()));
            }
        }
    }

    /**
     * test the getLogRecords call
     */

	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testGetLogRecords()
    {
       for(int j=0; j<memberNodeList.size(); j++)
       {
           currentUrl = memberNodeList.get(j).getBaseURL();
           MNode mn = D1Client.getMN(currentUrl);
           
           printTestHeader("testGetLogRecords - node " + memberNodeList.get(j).getBaseURL());
           System.out.println("current time is: " + new Date());
           try
           {
               Date start = new Date(System.currentTimeMillis() - 500000);
               Session token = null;

               String idString = idPrefix + ExampleUtilities.generateIdentifier();
               Identifier guid = new Identifier();
               guid.setValue(idString);
               InputStream objectStream = this.getClass().getResourceAsStream(
                       "/d1_testdocs/knb-lter-cdr.329066.1.data");
               SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(
                       guid, format_text_csv, objectStream, TEST_MN_ID);
               objectStream = this.getClass().getResourceAsStream(
                   "/d1_testdocs/knb-lter-cdr.329066.1.data");

               Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
               mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
               InputStream data = mn.get(token, rGuid);
               String str = IOUtils.toString(data);
               //System.out.println("str: " + str);
               checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
               checkEquals(guid.getValue(), rGuid.getValue());

               //get the logs for the last minute
               Date end = new Date(System.currentTimeMillis() + 500000);
               System.out.println("start: " + start + " end: " + end);
               Log log = mn.getLogRecords(token, start, end, Event.CREATE, null, null);
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
               checkTrue(isfound);

           } 
           catch(Exception e)
           {
               e.printStackTrace();
               errorCollector.addError(new Throwable(createAssertMessage() + 
                       " threw an unexpected exception: " + e.getMessage()));
           }
       }
    }
    
    /**
     * test setting access.  this is mainly a metacat test since other nodes
     * will not have implemented this.
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testsetAccessPolicy()
    {
        for(int j=0; j<memberNodeList.size(); j++)
        {
            currentUrl = memberNodeList.get(j).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printTestHeader("testListObjects - node " + memberNodeList.get(j).getBaseURL());
            System.out.println("current time is: " + new Date());
            try
            {
                Date date1 = new Date(System.currentTimeMillis() - 1000000);
                Session token = null;
                //Session token = new Session("public");
                //create a document we know is in the system
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                
                checkEquals(rGuid.getValue(), guid.getValue());
                
                Session pubToken = null; //new Session("public");
                //try to access as public (should not work)
                try
                {
                    mn.get(pubToken, rGuid);
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " mn.get should have failed since the doc does not have public access."));
                }
                catch(Exception e)
                {
                    
                }
                
                //make the inserted documents public
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                
                try
                {
                    mn.get(pubToken, rGuid);
                }
                catch(Exception e)
                {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " mn.get should not have failed to get the document since it is now public."));
                }
                
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " could not list object: " + e.getMessage()));
            }
        }
    }
    
    /**
     * list objects with specified params
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testListObjects()
    {
        for(int j=0; j<memberNodeList.size(); j++)
        {
            currentUrl = memberNodeList.get(j).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printTestHeader("testListObjects - node " + memberNodeList.get(j).getBaseURL());
            System.out.println("current time is: " + new Date());
            try
            {
                Date date1 = new Date(System.currentTimeMillis() - 1000000);
                Session token = null;
                //Session token = new Session("public");
                //create a document we know is in the system
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");

                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                
                checkEquals(rGuid.getValue(), guid.getValue());
                
                //make the inserted documents public
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());

                //get the objectList and make sure our created doc is in it
                ObjectList ol = mn.listObjects(token, null, null, null, false, 0, 100000);
                boolean isThere = false;
                
                checkTrue(ol.sizeObjectInfoList() > 0);
                
                //System.out.println("ol size: " + ol.sizeObjectInfoList());
                //System.out.println("guid: " + guid.getValue());
                for(int i=0; i<ol.sizeObjectInfoList(); i++)
                {
                    ObjectInfo oi = ol.getObjectInfo(i);
                    //System.out.println("oiid: " + oi.getIdentifier().getValue());
                    if(oi.getIdentifier().getValue().trim().equals(guid.getValue().trim()))
                    {
                        isThere = true;
                        //System.out.println("oi.checksum: " + oi.getChecksum().getValue() + 
                        //        "   sm.checksum: " + sysmeta.getChecksum().getValue());
                        break;
                    }
                }

                checkTrue(isThere);
                
                idString = idPrefix + ExampleUtilities.generateIdentifier();
                guid = new Identifier();
                guid.setValue(idString);
                objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");

                rGuid = mn.create(token, guid, objectStream, sysmeta);
                System.out.println("inserted doc with id " + rGuid.getValue());

                checkEquals(guid.getValue(), rGuid.getValue());
                
                //make the inserted documents public
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());

                Date date2 = new Date(System.currentTimeMillis() + 1000000);

                ObjectList ol2 = mn.listObjects(token, date1, date2, null, false, 0, 1000);
                boolean isthere = false;
                for(int i=0; i<ol2.sizeObjectInfoList(); i++)
                {
                    ObjectInfo oi = ol2.getObjectInfo(i);
                    if(oi.getIdentifier().getValue().trim().equals(rGuid.getValue().trim()))
                    {
                        isthere = true;
                        break;
                    }
                }
                System.out.println("1isthere: " + isthere);
                checkTrue(isthere);
                
                //test with a public token.  should get the same result since both docs are public
                token = null; //new Session("public");
                ol2 = mn.listObjects(token, null, null, null, false, 0, 100000);
                isthere = false;
                for(int i=0; i<ol2.sizeObjectInfoList(); i++)
                {
                    ObjectInfo oi = ol2.getObjectInfo(i);
                    if(oi.getIdentifier().getValue().trim().equals(rGuid.getValue().trim()))
                    {
                        isthere = true;
                        break;
                    }
                }
                System.out.println("2isthere: " + isthere);
                checkTrue(isthere);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " could not list object: " + e.getMessage()));
            }
        }
    }

    /**
     * get a systemMetadata resource
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testGetSystemMetadata()
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printTestHeader("testGetSystemMetadata - node " + memberNodeList.get(i).getBaseURL());
            try
            {
                //create a document
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                checkEquals(guid.getValue(), rGuid.getValue());
                //System.out.println("create success, id returned is " + rGuid.getValue());
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                //get the system metadata
                SystemMetadata sm = mn.getSystemMetadata(token, rGuid);
                checkTrue(guid.getValue().equals(sm.getIdentifier().getValue()));
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " error in getSystemMetadata: " + e.getMessage()));
            }
        }
    }

    /**
     * test the update of a resource
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testUpdate()
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printTestHeader("testUpdate - node " + memberNodeList.get(i).getBaseURL());
            try 
            {
                //create a document
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                System.out.println("d1 create");
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                System.out.println("d1 created " + rGuid.getValue());
                checkEquals(guid.getValue(), rGuid.getValue());
                //System.out.println("create success, id returned is " + rGuid.getValue());
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                //get the document
                InputStream data = mn.get(token, rGuid);
                checkTrue(null != data);
                String str = IOUtils.toString(data);
                checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
                data.close();

                //alter the document
                Identifier newguid = new Identifier();
                newguid.setValue(idPrefix + ExampleUtilities.generateIdentifier());
                str = str.replaceAll("61", "0");
                objectStream = IOUtils.toInputStream(str);
                SystemMetadata updatedSysmeta = ExampleUtilities.generateSystemMetadata(newguid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = IOUtils.toInputStream(str);
                
                //update the document
                System.out.println("d1 update newguid: "+ newguid.getValue() + " old guid: " + rGuid.getValue());
                Identifier nGuid = mn.update(token, rGuid, objectStream, newguid,updatedSysmeta);
                System.out.println("d1 updated success, id returned is " + nGuid.getValue());
                mn.setAccessPolicy(token, nGuid, buildPublicReadAccessPolicy());
                //perform tests
                data = mn.get(token, nGuid);
                checkTrue(null != data);
                str = IOUtils.toString(data);
                checkTrue(str.indexOf("0 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
                data.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " error in testUpdate: " + e.getMessage()));
            }
        }
    }

    /**
     * test the error state where metacat fails if the id includes a .\d on
     * the end.
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testFailedCreateData() {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printTestHeader("testFailedCreateData - node " + memberNodeList.get(i).getBaseURL());
            /*try 
        {
            System.out.println();
            assertTrue(1==1);
            //Session token = new Session("public");
            Session token = d1.login(principal, password);

            InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/BAYXXX_015ADCP015R00_20051215.50.9.xml");
            SystemMetadata sysmeta = getSystemMetadata(
                "/d1_testdocs/BAYXXX_015ADCP015R00_20051215.50.9_SYSMETA.xml");
            Identifier guid = sysmeta.getIdentifier();
            System.out.println("inserting with guid " + guid.getValue());
            Identifier rGuid = new Identifier();

            //insert
            rGuid = d1.create(token, guid, objectStream, sysmeta);
            assertEquals(guid.getValue(), rGuid.getValue());

            //get
            InputStream data = d1.get(token, rGuid);
            assertNotNull(data);
            String str = IOUtils.toString(data);
            System.out.println("output: " + str);
            assertTrue(str.indexOf("BAYXXX_015ADCP015R00_20051215.50.9") != -1);
            data.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            fail("Error inserting: " + e.getMessage());
        }*/
            try
            {
                checkTrue(true);
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString + ".1.5.2");
                System.out.println("guid is " + guid.getValue());
                //InputStream objectStream = IOUtils.toInputStream("x,y,z\n1,2,3\n");
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/BAYXXX_015ADCP015R00_20051215.50.9.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/BAYXXX_015ADCP015R00_20051215.50.9.xml");
                Identifier rGuid = null;

                //insert
                rGuid = mn.create(token, guid, objectStream, sysmeta);
                checkEquals(guid.getValue(), rGuid.getValue());

                //get
                InputStream data = mn.get(token, rGuid);
                checkTrue(null != data);
                String str = IOUtils.toString(data);
                checkTrue(str.indexOf("BAYXXX_015ADCP015R00_20051215.50.9") != -1);
                data.close();
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " error in testFailedCreateData: " + e.getMessage()));
            }
        }
    }
    
    /**
     * test various create and get scenarios with different access rules
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testGet() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printTestHeader("testGet - node " + memberNodeList.get(i).getBaseURL());
            try
            {
                //create a document
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                //InputStream objectStream = IOUtils.toInputStream("<?xml version=\"1.0\"?><test></test>");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;
                rGuid = mn.create(token, guid, objectStream, sysmeta);
                checkEquals(guid.getValue(), rGuid.getValue());

                //try to get it as public.  this should fail
                Session publicToken = null; //new Session("public");
                //this test is commented out because of this issue:
                //https://trac.dataone.org/ticket/706
                /*try
                {
                    InputStream data = d1.get(publicToken, rGuid);
                    System.out.println("data: " + IOUtils.toString(data));
                    fail("Should have thrown an exception.  Public can't get this doc yet.");
                }
                catch(Exception e)
                {

                }*/

                //change the perms, then try to get it again
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                InputStream data = mn.get(publicToken, rGuid);
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " error in testGet: " + e.getMessage()));
            }
        }
    }
    
    /**
     * test the creation of the desribes and describedBy sysmeta elements
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testCreateDescribedDataAndMetadata()
    {
        try
        {
            for(int j=0; j<memberNodeList.size(); j++)
            {
                currentUrl = memberNodeList.get(j).getBaseURL();
                MNode mn = D1Client.getMN(currentUrl);

                Session token = null;


                //parse that document for distribution info
                //Test EML 2.0.0
                InputStream is = this.getClass().getResourceAsStream(
                        "/d1_testdocs/eml200/dpennington.195.2");
                DataoneEMLParser parser = DataoneEMLParser.getInstance();
                EMLDocument emld = parser.parseDocument(is);
                checkEquals(format_eml_200.toString(), emld.format.toString());
                DistributionMetadata dm = emld.distributionMetadata.elementAt(0);
                checkEquals(format_text_csv.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/IPCC.200802107062739.1");
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml200/dpennington.195.2");
                insertEMLDocsWithEMLParserOutput(mn, emld, "dpennington.195.2", token, is);
                
                //Test EML 2.0.1
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml201/msucci.23.3");
                parser = DataoneEMLParser.getInstance();
                emld = parser.parseDocument(is);
                checkEquals(format_eml_201.toString(), emld.format.toString());
                dm = emld.distributionMetadata.elementAt(0);
                checkEquals(format_text_csv.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/msucci.24.1");
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml201/msucci.23.3");
                insertEMLDocsWithEMLParserOutput(mn, emld, "msucci.23.3", token, is);
                
                //Test EML 2.1.0
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml210/peggym.130.4");
                parser = DataoneEMLParser.getInstance();
                emld = parser.parseDocument(is);
                checkEquals(format_eml_210.toString(), emld.format.toString());
                dm = emld.distributionMetadata.elementAt(0);
                checkEquals(format_text_csv.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/peggym.127.1");
                dm = emld.distributionMetadata.elementAt(1);
                checkEquals(format_text_csv.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/peggym.128.1");
                dm = emld.distributionMetadata.elementAt(2);
                checkEquals(format_text_csv.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/peggym.129.1");
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml210/peggym.130.4");
                insertEMLDocsWithEMLParserOutput(mn, emld, "peggym.130.4", token, is);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            errorCollector.addError(new Throwable(createAssertMessage() + 
                    " error in testCreateDescribedDataAndMetadata: " + e.getMessage()));
        }
    }
    
    /**
     * test creation of data.  this also tests get() since it
     * is used to verify the inserted metadata
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testCreateData() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printTestHeader("testCreateData - node " + memberNodeList.get(i).getBaseURL());
            try
            {
                checkTrue(true);
                Session token = null;
                Identifier rGuid = createDataObject(mn, token);

                try {
                    InputStream data = mn.get(token, rGuid);
                    checkTrue(null != data);
                    String str = IOUtils.toString(data);
                    checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
                    data.close();
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateData: " + e.getMessage()));
                } 
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testCreateData: " + e.getMessage()));
            }
        }
    }
 
  
    /**
     * test creation of data with challenging unicode identifier.
     * this also tests get() since it
     * is used to verify the inserted metadata
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testCreateData_IdentifierEncoding() 
    {
    	printTestHeader("Testing IdentifierEncoding");

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

    	for(int i=0; i<memberNodeList.size(); i++)
    	{
    		currentUrl = memberNodeList.get(i).getBaseURL();

    		Vector<String> nodeSummary = new Vector<String>();
    		nodeSummary.add("Node Test Summary for node: " + currentUrl );

    		printTestHeader("  Node:: " + currentUrl);
    		MNode mn = D1Client.getMN(currentUrl);

            Session token = null;
            try
            {
                token = null;
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
    			String idString = idPrefix + ExampleUtilities.generateIdentifier() + "_" + unicodeString.get(j) ;
    			String idStringEscaped = idPrefix  + ExampleUtilities.generateIdentifier() + "_" + escapedString.get(j);

    			try
    			{
    				checkTrue(true);
    				
    				Identifier guid = new Identifier();
    				guid.setValue(idString);
    				InputStream objectStream = this.getClass().getResourceAsStream(
    						"/d1_testdocs/knb-lter-cdr.329066.1.data");
    				SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
    				objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
    				Identifier rGuid = null;

    				// rGuid is either going to be the escaped ID or the non-escaped ID
    				try {
    					rGuid = mn.create(token, guid, objectStream, sysmeta);
    					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
    					mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
    					checkEquals(guid.getValue(), rGuid.getValue());
    					InputStream data = mn.get(token, rGuid);
    					checkTrue(null != data);
    					String str = IOUtils.toString(data);
    					checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
    					data.close();
    				} catch (Exception e) {
    					status = "Error";
    					System.out.println("error message: " + e.getMessage());
    					e.printStackTrace();
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


	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testChecksum() 
    {
        //create a doc
        //calculate checksum
        //create
        //getChecksum
        //check the two checksums
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
           	printTestHeader("testChecksum.  node = " + memberNodeList.get(i).getBaseURL());
        	checkTrue(true);
            
            try
            {
               runChecksumTest(mn, "/d1_testdocs/checksumTestSet/sciMD-eml-201-NoLastLForCR.xml",null);
               runChecksumTest(mn, "/d1_testdocs/checksumTestSet/sciMD-eml-201-NoLastLForCR.xml","\n");
               runChecksumTest(mn, "/d1_testdocs/checksumTestSet/sciMD-eml-201-NoLastLForCR.xml","\r\n");
               runChecksumTest(mn, "/d1_testdocs/checksumTestSet/sciMD-eml-201-NoLastLForCR.xml","\n\n");
               runChecksumTest(mn, "/d1_testdocs/checksumTestSet/sciMD-eml-201-NoLastLForCR.xml","\r\n\r\n");
            } 
            catch(Exception e)
            {
            	errorCollector.addError(new Throwable(createAssertMessage() + 
            			" unexpected error in testGetChecksumSessionIdentifierTypeString: " + e.getMessage()));
            }
        }
    }
  
    
    private static String stringToUnicodeCharacters(String s) {
    	byte[] ba = s.getBytes();
    	
    	StringBuffer sb = new StringBuffer(s.length());
    	String b = null;
    	sb.append("0x");
    	for(int i=0;i<ba.length; i++)
    	{
    		b = Integer.toString(ba[i],16);  // converts to octal
    		for(int j=0; j< 2 - b.length(); j++) {
    			sb.append("0");
    		}
    		sb.append(b.toUpperCase());
    	}
    	return sb.toString();
    }   
    
 


    private void runChecksumTest(MNode mn, String resourceFile, String append) 
    throws NoSuchAlgorithmException, IOException, ServiceFailure, NotImplemented 
    {
    	System.out.println("******************************************");
    	System.out.println("Test file = " + resourceFile);
    	if (append != null)
    		System.out.println("File concatenations = " + stringToUnicodeCharacters(append));
    	else
    		System.out.println("File concatenations = nothing to concatenate");
    	
    	InputStream objectStream = this.getClass().getResourceAsStream(resourceFile);

    	objectStream = this.getClass().getResourceAsStream(resourceFile);
    	String doc = IOUtils.toString(objectStream);
    	objectStream.close();
    	
    	if (append != null)
    		doc += append;
    	
    	String checksum1str = ExampleUtilities.checksum(IOUtils.toInputStream(doc), CHECKSUM_ALGORITHM);
    	Checksum checksum1 = new Checksum();
    	checksum1.setValue(checksum1str);
    	checksum1.setAlgorithm(CHECKSUM_ALGORITHM);
    	System.out.println("Checksum 1: " + checksum1.getValue());
    	
    	Session token = null;
    	String idString = idPrefix + ExampleUtilities.generateIdentifier();
    	Identifier guid = new Identifier();
    	guid.setValue(idString);
    	objectStream = IOUtils.toInputStream(doc);
    	SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
    	objectStream = IOUtils.toInputStream(doc);
    	sysmeta.setChecksum(checksum1);
    	Identifier rGuid = null;

    	try {
    		rGuid = mn.create(token, guid, objectStream, sysmeta);
    		checkEquals(guid.getValue(), rGuid.getValue());
                mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
    	} catch (Exception e) {
    		errorCollector.addError(new Throwable(createAssertMessage() + 
    				" error in testChecksum: " + e.getMessage()));
    	}
                
    	try 
    	{
    		// getChecksum in metacat simply returns the checksum provided at create / update
    		// this better be the same, but doesn't mean much if it is
    		Checksum checksum2 = mn.getChecksum(token, rGuid, CHECKSUM_ALGORITHM);
    		System.out.println("getChecksum value: " + checksum2.getValue());
    		checkEquals(checksum1.getValue(), checksum2.getValue());
    		
    		// check that retrieved object has same checksum as submitted
    		InputStream objStream = mn.get(token,rGuid);
    		String checksum3str = ExampleUtilities.checksum(objStream, CHECKSUM_ALGORITHM);
    		System.out.println("retrieved object checksum: " + checksum3str);
    		checkEquals(checksum1.getValue(),checksum3str);
    		
    		
    	} 
    	catch (Exception e) 
    	{
    		errorCollector.addError(new Throwable(createAssertMessage() + 
    				" error in testChecksum: " + e.getMessage()));
    	}
    	
    	
    	
    	
    	
    }
    
    /**
     * test creation of science metadata.  this also tests get() since it
     * is used to verify the inserted metadata
     */
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testSemiColonIdentifiers() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try
            {
                printTestHeader("testSemiColonIdentifiers - node " + memberNodeList.get(i).getBaseURL());
                checkTrue(true);
                Session token = null;
                String idString = "some;id;with;semi;colons;" + new Date().getTime();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateScienceMetadata: " + e.getMessage()));
                }
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testCreateScienceMetadata: " + e.getMessage()));
            }
        }
    }
    
    /**
     * test creation of science metadata.  this also tests get() since it
     * is used to verify the inserted metadata
     */
	@Ignore("test not adapted for v0.6.x")
	@Test
    public void testCreateScienceMetadata() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try
            {
                printTestHeader("testCreateScienceMetadata - node " + memberNodeList.get(i).getBaseURL());
                checkTrue(true);
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateScienceMetadata: " + e.getMessage()));
                }


                try {
                    InputStream data = mn.get(token, rGuid);
                    checkTrue(null != data);
                    String str = IOUtils.toString(data);
                    checkTrue(str.indexOf("<shortName>LUQMetadata76</shortName>") != -1);
                    data.close();
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateScienceMetadata: " + e.getMessage()));
                } 
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testCreateScienceMetadata: " + e.getMessage()));
            }
        }
    }

	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testDelete() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            printTestHeader("testDelete - node " + memberNodeList.get(i).getBaseURL());
            
            try
            {
                checkTrue(true);
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try 
                {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                    Thread.sleep(2000);
                    Identifier delId = mn.delete(token, rGuid);
                    checkTrue(delId.getValue().equals(rGuid.getValue()));
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testDelete: " + e.getMessage()));
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testDelete: " + e.getMessage()));
            }
        }
    }

	
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testDescribe() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printTestHeader("testDescribe - node " + memberNodeList.get(i).getBaseURL());
            try
            {
                checkTrue(true);
                Session token = null;
                String idString = idPrefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_eml_210, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try 
                {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
                    DescribeResponse dr = mn.describe(token, rGuid);
                    Checksum cs = dr.getDataONE_Checksum();
                    checkTrue(cs.getValue().equals(sysmeta.getChecksum().getValue()));
                    checkTrue(dr.getContent_Length() == sysmeta.getSize());
                    checkTrue(dr.getDataONE_ObjectFormatIdentifier().toString().equals(sysmeta.getFmtid().toString()));                    
                } 
                catch (Exception e) 
                {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testGetChecksumSessionIdentifierTypeString: " + e.getMessage()));
                }
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testDescribe: " + e.getMessage()));
            }
        }
    }

	
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testGetNotFound() 
    {
        for(int i=0; i<memberNodeList.size(); i++)
        {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try {
                printTestHeader("testGetNotFound - node " + memberNodeList.get(i).getBaseURL());
                Session token = null;
                Identifier guid = new Identifier();
                guid.setValue(bogusId);
                InputStream data = mn.get(token, guid);
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " NotFound exception should have been thrown"));
            }  catch (NotFound e) {
                String error = e.serialize(BaseException.FMT_XML);
                System.out.println(error);
                checkTrue(error.indexOf("<error") != -1);
            } catch (Exception e) {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected exception in testGetNotFound: " + 
                        e.getMessage()));
            }
        }
    }

	
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testNodeResponse() {
    	 for(int i=0; i<memberNodeList.size(); i++)
         {
             currentUrl = memberNodeList.get(i).getBaseURL();
             MNode  mn = D1Client.getMN(currentUrl);
             
             try {
                 printTestHeader("testNodeResponse " + memberNodeList.get(i).getBaseURL());
             
                 D1Url url = new D1Url(mn.getNodeBaseServiceUrl());
                 
                 D1RestClient rc = new D1RestClient();
                 
                 InputStream is = null;
                 try {	
                	 is = rc.doGetRequest(url.getUrl());
                 } catch (BaseException be) {
                	 be.printStackTrace();
                 } catch (IllegalStateException e) {
                	 e.printStackTrace();
                 } catch (ClientProtocolException e) {
                	 e.printStackTrace();
                 } catch (IOException e) {
                	 e.printStackTrace();
                 } catch (HttpException e) {
                	 e.printStackTrace();
                 } 
                 
                 try {
                	 NodeList nl = 
                		  TypeMarshaller.unmarshalTypeFromStream(NodeList.class, is);
                 } catch (Exception e) {
                	 errorCollector.addError(new Throwable(createAssertMessage() + 
                             " failed to create NodeList: " + 
                             e.getMessage()));
                 }
             } catch (Exception e) {
                 errorCollector.addError(new Throwable(createAssertMessage() + 
                         " unexpected exception in testNodeResponse: " + 
                         e.getMessage()));
             }
         }
    }
     
    
    /**
     * Create a test data object on the given member node, and return the Identifier that was created for that object.
     * @param mn Member node on which to create the object
     * @param token a valid authentication token
     * @return the Identifer of the created object
     */
    private Identifier createDataObject(MNode mn, Session token) {
        String idString = idPrefix + ExampleUtilities.generateIdentifier();
        Identifier guid = new Identifier();
        guid.setValue(idString);
        InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-cdr.329066.1.data");
        SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, format_text_csv, objectStream, TEST_MN_ID);
        objectStream = this.getClass().getResourceAsStream(
            "/d1_testdocs/knb-lter-cdr.329066.1.data");
        Identifier rGuid = null;
        try {
            rGuid = mn.create(token, guid, objectStream, sysmeta);
            mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
            checkEquals(guid.getValue(), rGuid.getValue());
        } catch (Exception e) {
            errorCollector.addError(new Throwable(createAssertMessage() + 
                    " error in testCreateData: " + e.getClass() + ": " + e.getMessage()));
        }
        return rGuid;
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
     * @throws NotFound 
     * @throws InvalidRequest 
     */
    private void insertEMLDocsWithEMLParserOutput(MNode mn, EMLDocument emld, String file, Session token, InputStream is) 
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
        UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, NotFound, InvalidRequest
    {
        String dirname;
        if(emld.format.getFmtid().getValue() == format_eml_200)
            dirname = "eml200";
        else if(emld.format.getFmtid().getValue() == format_eml_201)
            dirname = "eml201";
        else if(emld.format.getFmtid().getValue() == format_eml_210)
            dirname = "eml210";
        else
            dirname = "";
        
        //create an ID for the metadata doc
        String idString = ExampleUtilities.generateIdentifier();
        Identifier mdId = new Identifier();
        mdId.setValue(idString);
        
        SystemMetadata mdSm = ExampleUtilities.generateSystemMetadata(mdId, emld.format.getFmtid().getValue(), is, TEST_MN_ID);

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
                    ObjectFormatCache.getInstance().getFormat(emld.distributionMetadata.elementAt(i).mimeType).getFmtid().getValue(),
                    instream, TEST_MN_ID);
            //add desrviedBy
 //           sm.addDescribedBy(mdId);
            //add describes to the metadata doc's sm
 //           mdSm.addDescribe(id);
            //TODO: replace this with a call to the server eventually
            instream = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + url);

            Identifier createdDataId = mn.create(token, id, instream, sm);
            mn.setAccessPolicy(token, createdDataId, buildPublicReadAccessPolicy());
            checkEquals(createdDataId.getValue(), id.getValue());
            System.out.println("Data ID: " + id.getValue());
        }
        
        //send the EML doc to create
        is = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + file);
        Identifier createdMdId = mn.create(token, mdId, is, mdSm);
        mn.setAccessPolicy(token, createdMdId, buildPublicReadAccessPolicy());
        checkEquals(createdMdId.getValue(), mdId.getValue());
        System.out.println("Metadata ID: " + createdMdId.getValue());
    }
    
    private static String createAssertMessage()
    {
        return "test failed at url " + currentUrl;
    }

    /** Generate a science metadata object for testing. */
/*    private static String generateScienceMetadata(Identifier guid) {
        String accessBlock = ExampleUtilities.getAccessBlock("public", true, true,
                false, false, false);
        String emldoc = ExampleUtilities.generateEmlDocument(
                "Test identifier manager",
                ExampleUtilities.EML2_1_0, null,
                null, "http://fake.example.com/somedata", null,
                accessBlock, null, null, null, null);
        return emldoc;
    }*/
    
    /**
     * get system metadata
     * @param metadataResourcePath
     * @return
     */
    /*private SystemMetadata getSystemMetadata(String metadataResourcePath)  {
        printHeader("testGetSystemMetadata");
        SystemMetadata  systemMetadata = null;
        InputStream inputStream = null;
        try {
            IBindingFactory bfact =
                    BindingDirectory.getFactory(org.dataone.service.types.SystemMetadata.class);

            IMarshallingContext mctx = bfact.createMarshallingContext();
            IUnmarshallingContext uctx = bfact.createUnmarshallingContext();

            inputStream = this.getClass().getResourceAsStream(metadataResourcePath);

            systemMetadata = (SystemMetadata) uctx.unmarshalDocument(inputStream, null);

        } catch (JiBXException ex) {
            ex.printStackTrace();
            systemMetadata = null;
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
               ex.printStackTrace();
            }
        }
        return systemMetadata;
    }*/
    
    
    private void checkEquals(final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat("assertion failed for host " + currentUrl, s1, is(s2));
                //assertThat("assertion failed for host " + currentUrl, s1, is(s2 + "x"));
                return null;
            }
        });
    }
    
    private void checkTrue(final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat("assertion failed for host " + currentUrl, true, is(b));
                return null;
            }
        });
    }
}
