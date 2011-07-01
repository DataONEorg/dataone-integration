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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
import org.dataone.client.D1Client;
import org.dataone.client.D1RestClient;
import org.dataone.client.MNode;
import org.dataone.client.ObjectFormatCache;
import org.dataone.eml.EMLDocument;
import org.dataone.service.D1Url;
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
import org.dataone.service.types.Event;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.Log;
import org.dataone.service.types.LogEntry;
import org.dataone.service.types.Node;
import org.dataone.service.types.Session;
import org.dataone.service.types.SystemMetadata;
import org.dataone.service.types.util.ServiceTypeUtil;
import org.jibx.runtime.JiBXException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Matthew Jones
 */
public class MNodeTier1IT extends ContextAwareTestCaseDataone  {

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
		return "Test Case that runs through the Member Node Tier 1 API methods";
	}
    
	
	@BeforeClass
	public static void overrideContext() {
		System.setProperty(PARAM_MN_URL, "http://cn-dev.dataone.org/knb/d1/mn");
		System.out.println("overrode the Context");
	}
	
    @Test
    public void testSetup() {
    	System.out.println("text/csv: " + format_text_csv);//.getFormatName());
    	System.out.println("text_eml_200: " + format_eml_200);//.getFormatName());
    	System.out.println("text_eml_201: " + format_eml_201);//.getFormatName());
    	System.out.println("text_eml_210: " + format_eml_210);//.getFormatName());
    	System.out.println("done");
    }
	
	
	
	
	@Test
	public void testPing() {
		
	}
	
	
    @Test
    public void testGetLogRecords()
    {
       Iterator<Node> it = getMemberNodeIterator();
       while (it.hasNext()) {
    	   currentUrl = it.next().getBaseURL();

           MNode mn = D1Client.getMN(currentUrl);
           
           printTestHeader("testGetLogRecords - node:" + currentUrl);
           log.info("current time is: " + new Date());
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
               log.info("start: " + start + " end: " + end);
               Log logRecord = mn.getLogRecords(token, start, end, Event.CREATE, null, null);
               log.info("log size: " + logRecord.sizeLogEntryList());
               boolean isfound = false;
               for(int i=0; i<logRecord.sizeLogEntryList(); i++)
               { //check to see if our create event is in the log
                   LogEntry le = logRecord.getLogEntry(i);
                   log.debug("le: " + le.getIdentifier().getValue());
                   log.debug("rGuid: " + rGuid.getValue());
                   if(le.getIdentifier().getValue().trim().equals(rGuid.getValue().trim()))
                   {
                       isfound = true;
                       System.out.println("log record found");
                       break;
                   }
               }
               log.info("isfound: " + isfound);
               checkTrue(isfound);

           } 
           catch(BaseException be) 
           {
        	   be.printStackTrace();
        	   errorCollector.addError(new Throwable(createAssertMessage() + 
                       " threw an unexpected dataone exception: " + be.getClass().getName()));
           } 
           catch(Exception e)
           {
               e.printStackTrace();
               errorCollector.addError(new Throwable(createAssertMessage() + 
                       " threw an unexpected exception: " + e.getMessage()));
           }
       }
    }
	
	
    @Test 
    public void testGetObjectStatistics() {
    	
    }
	
    
    @Test
    public void testGetOperationStatistics() {
    	
    }
	
    
    @Test
    public void testGetStatus() {
    	
    }
    
    
    @Test
    public void getCapabilities() {
    	
//    }
//    
//    
//    @Test
//    public void testNodeResponse() {
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
//                	 System.out.println(IOUtils.toString(is));
                	 org.dataone.service.types.NodeList nl = 
                		 (org.dataone.service.types.NodeList) ServiceTypeUtil.deserializeServiceType(
                				 org.dataone.service.types.NodeList.class, is);
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
    
	@SuppressWarnings("rawtypes")
	protected void serializeServiceType(Class type, Object object,
			OutputStream out) throws JiBXException {
		ServiceTypeUtil.serializeServiceType(type, object, out);
	}
    

    
    private static String createAssertMessage()
    {
        return "test failed at url " + currentUrl;
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
