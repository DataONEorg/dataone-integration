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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.DescribeResponse;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeObjectLifecycleIT extends ContextAwareTestCaseDataone {
//    private static final String devPrincipal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
//    private static final String pw = "kepler";
    private static final String prefix = "simpleApiTests:testid:";
    private static final String bogusId = "foobarbaz214";
//    private static final String knownId = "repl:testID201120161032499";

//    private List<Node> nodeList = null;
 //   private Hashtable nodeInfo = null;
    private static String currentUrl;
    //set this to false if you don't want to use the node list to get the urls for 
    //the test.  
//    private static boolean useNodeList = false;
        
 
//    @Before
    public void setUp() throws Exception 
    {        
//    	nodeInfo = new Hashtable();
//    	nodeInfo.put("http://cn-dev.dataone.org/knb/d1/", new String[] {devPrincipal, pw});
//    	nodeInfo.put("http://gmn-dev.dyndns.org/mn/", new String[] {"public","public"});
//    	nodeInfo.put("http://dev-dryad-mn.dataone.org/mn/", new String[] {"",""});
//    	nodeInfo.put("http://daacmn.dataone.utk.edu/mn/", new String[] {"",""});
    }

	@Ignore("test not adapted for v0.6.x")
    @Test
    public void exerciseNodeAPIs() throws IOException {
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("Simple object lifecycle test vs. node: " + currentUrl);
		
		
//		Enumeration<String> nodeBaseUrls = nodeInfo.keys();

		printHeader("Simple object lifecycle tests");
//    	while( nodeBaseUrls.hasMoreElements() ) 
//    	{
//    		String currentBaseUrl = nodeBaseUrls.nextElement();
//    		currentUrl = currentBaseUrl;
//    		String[] currentNodeInfo = (String[]) nodeInfo.get(currentBaseUrl);
//    		String logon = currentNodeInfo[0];
//    		String cred = currentNodeInfo[1];
    		


    		// objectlist - count
			int origObjectCount = 0;
    		printSubHeader(" listObjects");
    		try {
    			ObjectList ol = mn.listObjects(null, null, null, null, false, 0, 10);
    			origObjectCount = ol.getTotal();
        		log.info("=========>> total from listObjects: " + origObjectCount);
    		} catch (Exception e) {
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.listObjects: " + e.getMessage()));
    		}
    		

    		// login if necessary
    		Session token = null;
//    		try { 
//    			printSubHeader("login");
//    			if (!logon.isEmpty()) {
//    				token = null; //mn.login(logon, cred);
//    			} else
//    				System.out.println("Skipping login, no credentials for current node");
//    		} catch (Exception e) {
//    			System.out.println("error in login: " + e.getMessage());
//    			errorCollector.addError(new Throwable(createAssertMessage() + 
//    					" error in mn.login: " + e.getMessage()));
//    		}
    		
    		// assemble new object and systemMetadata (client-side)
    		String idString = prefix + ExampleUtilities.generateIdentifier();
    		Identifier newPid = new Identifier();
    		newPid.setValue(idString);

    		InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
    		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(newPid, "text-csv", objectStream,null);
    		objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
    		Identifier rGuid = null;
    		

    		try {
    			printSubHeader("create");
    			rGuid = mn.create(token, newPid, objectStream, sysmeta);
    			assertThat("checking that returned guid matches given ", newPid.getValue(), is(rGuid.getValue()));
    			log.info("new object PID: " + rGuid.getValue());
    		} catch (Exception e) {
    			log.error("error in mn.create: " + e.getMessage());
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
    			mn.setAccessPolicy(token, newPid, buildPublicReadAccessPolicy());
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
        		checkTrue("",dr.getDataONE_ObjectFormatIdentifier().toString().equals(sysmeta.getFormatId().toString())); 
    		} catch (Exception e) {
    			System.out.println("error in mn.getSystemMetadata: " + e.getMessage());
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.describe: " + e.getMessage()));
    		}
    		
    		
    		
    		// getChecksum
    		try {
        		printSubHeader("getChecksum");
    			Checksum cs = mn.getChecksum(null, newPid, "MD5");
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
                SystemMetadata updatedSysmeta = ExampleUtilities.generateSystemMetadata(updatedPid,
                		"text/csv", objectStream,null);
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
    		checkEquals(" *~*~*~* objectlist should be same after delete as before delete (zero impact)",
    				origObjectCount + 2, c);		   		
    	}
    }
    

    
    private int getCountFromListObjects(MNode mn, Session token)
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


	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
