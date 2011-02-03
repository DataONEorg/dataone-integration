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
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.eml.DataoneEMLParser;
import org.dataone.eml.EMLDocument;
import org.dataone.eml.EMLDocument.DistributionMetadata;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
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
 * @author Matthew Jones
 */
public class D1ClientTest  {

    private static final String TEST_MN_URL = "http://cn-dev.dataone.org/knb/d1/";
    private static final String TEST_MN_ID = "http://cn-dev.dataone.org/knb/d1";
    private static final String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
    private static final String password = "kepler";
    private static final String prefix = "knb:testid:";
    private static final String bogusId = "foobarbaz214";

    private List<Node> nodeList = null;
    private static String currentUrl;
    //set this to false if you don't want to use the node list to get the urls for 
    //the test.  
    private static boolean useNodeList = false;
        
    @Rule 
    public ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setUp() throws Exception 
    {
        if (nodeList == null || nodeList.size() == 0 || !useNodeList) {
            nodeList = new Vector<Node>();
            Node n = new Node();
            n.setBaseURL(TEST_MN_URL);
            nodeList.add(n);
        }
    }
    
    /**
     * test the failed creation of a doc
     */
    @Test
    public void testFailedCreate()
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            try
            {
                printHeader("testFailedCreate - node " + nodeList.get(i).getBaseURL());
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2-broken.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-luq.76.2-broken.xml");
                
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " Should have thrown exception since the xml file created was currupt"));
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
    @Test
    public void testGetLogRecords()
    {
       for(int j=0; j<nodeList.size(); j++)
       {
           currentUrl = nodeList.get(j).getBaseURL();
           MNode mn = D1Client.getMN(currentUrl);
           
           printHeader("testGetLogRecords - node " + nodeList.get(j).getBaseURL());
           System.out.println("current time is: " + new Date());
           try
           {
               Date start = new Date(System.currentTimeMillis() - 500000);
               AuthToken token = mn.login(principal, password);

               String idString = prefix + ExampleUtilities.generateIdentifier();
               Identifier guid = new Identifier();
               guid.setValue(idString);
               InputStream objectStream = this.getClass().getResourceAsStream(
                       "/d1_testdocs/knb-lter-cdr.329066.1.data");
               SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(
                       guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
               objectStream = this.getClass().getResourceAsStream(
                   "/d1_testdocs/knb-lter-cdr.329066.1.data");

               Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
               InputStream data = mn.get(token, rGuid);
               String str = IOUtils.toString(data);
               //System.out.println("str: " + str);
               checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
               checkEquals(guid.getValue(), rGuid.getValue());

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
    @Test
    public void testSetAccess()
    {
        for(int j=0; j<nodeList.size(); j++)
        {
            currentUrl = nodeList.get(j).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printHeader("testListObjects - node " + nodeList.get(j).getBaseURL());
            System.out.println("current time is: " + new Date());
            try
            {
                Date date1 = new Date(System.currentTimeMillis() - 1000000);
                AuthToken token = mn.login(principal, password);
                //AuthToken token = new AuthToken("public");
                //create a document we know is in the system
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                
                checkEquals(rGuid.getValue(), guid.getValue());
                
                AuthToken pubToken = new AuthToken("public");
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
                mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
                
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
    @Test
    public void testListObjects()
    {
        for(int j=0; j<nodeList.size(); j++)
        {
            currentUrl = nodeList.get(j).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printHeader("testListObjects - node " + nodeList.get(j).getBaseURL());
            System.out.println("current time is: " + new Date());
            try
            {
                Date date1 = new Date(System.currentTimeMillis() - 1000000);
                AuthToken token = mn.login(principal, password);
                //AuthToken token = new AuthToken("public");
                //create a document we know is in the system
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");

                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                
                checkEquals(rGuid.getValue(), guid.getValue());
                
                //make the inserted documents public
                mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");

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
                
                idString = prefix + ExampleUtilities.generateIdentifier();
                guid = new Identifier();
                guid.setValue(idString);
                objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");

                rGuid = mn.create(token, guid, objectStream, sysmeta);
                System.out.println("inserted doc with id " + rGuid.getValue());

                checkEquals(guid.getValue(), rGuid.getValue());
                
                //make the inserted documents public
                mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");

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
                token = new AuthToken("public");
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
    @Test
    public void testGetSystemMetadata()
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testGetSystemMetadata - node " + nodeList.get(i).getBaseURL());
            try
            {
                //create a document
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                checkEquals(guid.getValue(), rGuid.getValue());
                //System.out.println("create success, id returned is " + rGuid.getValue());

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
    @Test
    public void testUpdate()
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testUpdate - node " + nodeList.get(i).getBaseURL());
            try 
            {
                //create a document
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                System.out.println("d1 create");
                Identifier rGuid = mn.create(token, guid, objectStream, sysmeta);
                System.out.println("d1 created " + rGuid.getValue());
                checkEquals(guid.getValue(), rGuid.getValue());
                //System.out.println("create success, id returned is " + rGuid.getValue());

                //get the document
                InputStream data = mn.get(token, rGuid);
                checkTrue(null != data);
                String str = IOUtils.toString(data);
                checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
                data.close();

                //alter the document
                Identifier newguid = new Identifier();
                newguid.setValue(prefix + ExampleUtilities.generateIdentifier());
                str = str.replaceAll("61", "0");
                objectStream = IOUtils.toInputStream(str);
                SystemMetadata updatedSysmeta = ExampleUtilities.generateSystemMetadata(newguid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = IOUtils.toInputStream(str);
                
                //update the document
                System.out.println("d1 update newguid: "+ newguid.getValue() + " old guid: " + rGuid);
                Identifier nGuid = mn.update(token, newguid, objectStream, rGuid, updatedSysmeta);
                System.out.println("d1 updated success, id returned is " + nGuid.getValue());

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
    @Test
    public void testFailedCreateData() {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testFailedCreateData - node " + nodeList.get(i).getBaseURL());
            /*try 
        {
            System.out.println();
            assertTrue(1==1);
            //AuthToken token = new AuthToken("public");
            AuthToken token = d1.login(principal, password);

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
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString + ".1.5.2");
                System.out.println("guid is " + guid.getValue());
                //InputStream objectStream = IOUtils.toInputStream("x,y,z\n1,2,3\n");
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/BAYXXX_015ADCP015R00_20051215.50.9.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
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
    @Test
    public void testGet() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testGet - node " + nodeList.get(i).getBaseURL());
            try
            {
                //create a document
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                //InputStream objectStream = IOUtils.toInputStream("<?xml version=\"1.0\"?><test></test>");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;
                rGuid = mn.create(token, guid, objectStream, sysmeta);
                checkEquals(guid.getValue(), rGuid.getValue());

                //try to get it as public.  this should fail
                AuthToken publicToken = new AuthToken("public");
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
                mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
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
    @Test
    public void testCreateDescribedDataAndMetadata()
    {
        try
        {
            for(int j=0; j<nodeList.size(); j++)
            {
                currentUrl = nodeList.get(j).getBaseURL();
                MNode mn = D1Client.getMN(currentUrl);

                AuthToken token = mn.login(principal, password);


                //parse that document for distribution info
                //Test EML 2.0.0
                InputStream is = this.getClass().getResourceAsStream(
                        "/d1_testdocs/eml200/dpennington.195.2");
                DataoneEMLParser parser = DataoneEMLParser.getInstance();
                EMLDocument emld = parser.parseDocument(is);
                checkEquals(ObjectFormat.EML_2_0_0.toString(), emld.format.toString());
                DistributionMetadata dm = emld.distributionMetadata.elementAt(0);
                checkEquals(ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/IPCC.200802107062739.1");
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml200/dpennington.195.2");
                insertEMLDocsWithEMLParserOutput(mn, emld, "dpennington.195.2", token, is);
                
                //Test EML 2.0.1
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml201/msucci.23.3");
                parser = DataoneEMLParser.getInstance();
                emld = parser.parseDocument(is);
                checkEquals(ObjectFormat.EML_2_0_1.toString(), emld.format.toString());
                dm = emld.distributionMetadata.elementAt(0);
                checkEquals(ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/msucci.24.1");
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml201/msucci.23.3");
                insertEMLDocsWithEMLParserOutput(mn, emld, "msucci.23.3", token, is);
                
                //Test EML 2.1.0
                is = this.getClass().getResourceAsStream("/d1_testdocs/eml210/peggym.130.4");
                parser = DataoneEMLParser.getInstance();
                emld = parser.parseDocument(is);
                checkEquals(ObjectFormat.EML_2_1_0.toString(), emld.format.toString());
                dm = emld.distributionMetadata.elementAt(0);
                checkEquals(ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/peggym.127.1");
                dm = emld.distributionMetadata.elementAt(1);
                checkEquals(ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
                checkEquals(dm.url, "ecogrid://knb/peggym.128.1");
                dm = emld.distributionMetadata.elementAt(2);
                checkEquals(ObjectFormat.TEXT_PLAIN.toString(), dm.mimeType);
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
    @Test
    public void testCreateData() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testCreateData - node " + nodeList.get(i).getBaseURL());
            try
            {
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
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
 
    // TODO: implementation of this REST call format has been deferred.
//    @Test
    public void testCreateData_CurrentRestFormat() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testCreateData - node " + nodeList.get(i).getBaseURL());
            try
            {
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                Identifier rGuid = null;

                try {
                    rGuid = mn.create2(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateData: " + e.getClass() + ": " + e.getMessage()));
                }

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
    @Test
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
    		MNode mn = D1Client.getMN(currentUrl);

            AuthToken token = null;
            try
            {
                token = mn.login(principal, password);
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
    				checkTrue(true);
    				
    				Identifier guid = new Identifier();
    				guid.setValue(idString);
    				InputStream objectStream = this.getClass().getResourceAsStream(
    						"/d1_testdocs/knb-lter-cdr.329066.1.data");
    				SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
    				objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-cdr.329066.1.data");
    				Identifier rGuid = null;

    				// rGuid is either going to be the escaped ID or the non-escaped ID
    				try {
    					rGuid = mn.create(token, guid, objectStream, sysmeta);
    					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
    					mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
    					checkEquals(guid.getValue(), rGuid.getValue());
    					InputStream data = mn.get(token, rGuid);
    					checkTrue(null != data);
    					String str = IOUtils.toString(data);
    					checkTrue(str.indexOf("61 66 104 2 103 900817 \"Planted\" 15.0  3.3") != -1);
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

    
    @Test
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
            MNode mn = D1Client.getMN(currentUrl);

            try
            {
                printHeader("testGetChecksumAuthTokenIdentifierTypeString - node " + nodeList.get(i).getBaseURL());
                checkTrue(true);
                
                InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-luq.76.2.xml");
                String doc = IOUtils.toString(objectStream);
                Checksum checksum1 = new Checksum();
                String checksum1str = ExampleUtilities.checksum(IOUtils.toInputStream(doc), "MD5");
                checksum1.setValue(checksum1str);
                checksum1.setAlgorithm(ChecksumAlgorithm.M_D5);
                System.out.println("Checksum1: " + checksum1.getValue());
                objectStream.close();
                
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                objectStream = IOUtils.toInputStream(doc);
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = IOUtils.toInputStream(doc);
                sysmeta.setChecksum(checksum1);
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                } catch (Exception e) {
                    errorCollector.addError(new Throwable(createAssertMessage() + 
                            " error in testCreateScienceMetadata: " + e.getMessage()));
                }
                
                try 
                {
                    Checksum checksum2 = mn.getChecksum(token, rGuid, "MD5");
                    System.out.println("Checksum2: " + checksum2.getValue());
                    checkEquals(checksum1.getValue(), checksum2.getValue());
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
     * test creation of science metadata.  this also tests get() since it
     * is used to verify the inserted metadata
     */
    @Test
    public void testSemiColonIdentifiers() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try
            {
                printHeader("testSemiColonIdentifiers - node " + nodeList.get(i).getBaseURL());
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = "some;id;with;semi;colons;" + new Date().getTime();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
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
    @Test
    public void testCreateScienceMetadata() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try
            {
                printHeader("testCreateScienceMetadata - node " + nodeList.get(i).getBaseURL());
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
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
    
    @Test
    public void testDelete() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            printHeader("testDelete - node " + nodeList.get(i).getBaseURL());
            
            try
            {
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try 
                {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
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
    
    @Test
    public void testDescribe() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);
            
            printHeader("testDescribe - node " + nodeList.get(i).getBaseURL());
            try
            {
                checkTrue(true);
                AuthToken token = mn.login(principal, password);
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                InputStream objectStream = this.getClass().getResourceAsStream(
                        "/d1_testdocs/knb-lter-luq.76.2.xml");
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.EML_2_1_0, objectStream, TEST_MN_ID);
                objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-luq.76.2.xml");
                Identifier rGuid = null;

                try 
                {
                    rGuid = mn.create(token, guid, objectStream, sysmeta);
                    checkEquals(guid.getValue(), rGuid.getValue());
                    DescribeResponse dr = mn.describe(token, rGuid);
                    Checksum cs = dr.getDataONE_Checksum();
                    checkTrue(cs.getValue().equals(sysmeta.getChecksum().getValue()));
                    checkTrue(dr.getContent_Length() == sysmeta.getSize());
                    checkTrue(dr.getDataONE_ObjectFormat().toString().equals(sysmeta.getObjectFormat().toString()));                    
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
                        " unexpected error in testDescribe: " + e.getMessage()));
            }
        }
    }

    @Test
    public void testGetNotFound() 
    {
        for(int i=0; i<nodeList.size(); i++)
        {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            try {
                printHeader("testGetNotFound - node " + nodeList.get(i).getBaseURL());
                AuthToken token = mn.login(principal, password);
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
    
    /**
     * Create a test data object on the given member node, and return the Identifier that was created for that object.
     * @param mn Member node on which to create the object
     * @param token a valid authentication token
     * @return the Identifer of the created object
     */
    private Identifier createDataObject(MNode mn, AuthToken token) {
        String idString = prefix + ExampleUtilities.generateIdentifier();
        Identifier guid = new Identifier();
        guid.setValue(idString);
        InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-cdr.329066.1.data");
        SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
        objectStream = this.getClass().getResourceAsStream(
            "/d1_testdocs/knb-lter-cdr.329066.1.data");
        Identifier rGuid = null;
        try {
            rGuid = mn.create(token, guid, objectStream, sysmeta);
            mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
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
        
        SystemMetadata mdSm = ExampleUtilities.generateSystemMetadata(mdId, emld.format, is, TEST_MN_ID);

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
                    ObjectFormat.convert(emld.distributionMetadata.elementAt(i).mimeType), instream, TEST_MN_ID);
            //add desrviedBy
            sm.addDescribedBy(mdId);
            //add describes to the metadata doc's sm
            mdSm.addDescribe(id);
            //TODO: replace this with a call to the server eventually
            instream = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + url);

            Identifier createdDataId = mn.create(token, id, instream, sm);
            mn.setAccess(token, createdDataId, "public", "read", "allow", "allowFirst");
            checkEquals(createdDataId.getValue(), id.getValue());
            System.out.println("Data ID: " + id.getValue());
        }
        
        //send the EML doc to create
        is = this.getClass().getResourceAsStream("/d1_testdocs/" + dirname + "/" + file);
        Identifier createdMdId = mn.create(token, mdId, is, mdSm);
        mn.setAccess(token, createdMdId, "public", "read", "allow", "allowFirst");
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
    
    private void printHeader(String methodName)
    {
        System.out.println("\n***************** running test for " + methodName + " *****************");
    }
    
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
