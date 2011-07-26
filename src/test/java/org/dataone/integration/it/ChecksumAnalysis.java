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
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithm;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test the DataONE Java client methods.
 * @author Matthew Jones, Rob Nahf
 */
public class ChecksumAnalysis extends ContextAwareTestCaseDataone {
    private static final String devPrincipal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
    private static final String pw = "kepler";
    private static final String prefix = "simpleApiTests:testid:";

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
    	nodeInfo.put("http://cn-ucsb-1.dataone.org/knb/d1/", new String[] {devPrincipal, pw});
//    	nodeInfo.put("http://gmn-dev.dyndns.org/mn/", new String[] {"public","public"});
//    	nodeInfo.put("http://dev-dryad-mn.dataone.org/mn/", new String[] {"",""});
//    	nodeInfo.put("http://daacmn.dataone.utk.edu/mn/", new String[] {"",""});
    }

   @ Test
   public void testTest()
   {
	   assertTrue("yep",true);
   }
    
//    @Test
    public void checkChecksum() throws IOException {
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
    		MNode mn = D1Client.getMN(currentBaseUrl);
    		

    		// objectlist - count
    		printSubHeader(" listObjects");
    		ObjectList ol = null;
    		try {
    			int start = 0;
    			int total = 1; 
    			int count = 200;
    			int c = 0;
    			while (start < total) {
    				ol = mn.listObjects(null, null, null, null, false, start, count);
    				if (total == 1)
    					System.out.println("Stated total = " + ol.getTotal());
    				total = ol.getTotal();
    				
    				start += count;
    			
    				List<ObjectInfo> oil = ol.getObjectInfoList();
    				Iterator it = oil.iterator();
    				ObjectInfo oi = null;
    				while (it.hasNext()) {
    					oi = (ObjectInfo) it.next();
    					reportChecksumStats(mn, oi,c);
    					c++;
    				}
    			}
    			System.out.println("counted: " + c);
    		} catch (Exception e) {
    			errorCollector.addError(new Throwable(createAssertMessage() + 
    					" error in mn.listObjects: " + e.getMessage()));
    		}
    	}
    }
    
    public void reportChecksumStats(MNode mn, ObjectInfo oi, int index) throws IOException, BaseException {
    	InputStream is = mn.get(null,oi.getIdentifier());
    	byte[] obj = IOUtils.toByteArray(is);
    	String oiStatus = evaluateChecksum(oi.getChecksum(), obj);

    	// getChecksum calculates the checksum on the server and sends the results
    	// defaults to MD5, but we'll try to use what's in the objectInfo.    	
    	String gStatus = evaluateChecksum(mn.getChecksum(null, oi.getIdentifier(), oi.getChecksum().getAlgorithm().toString()),obj);
    	
    	if (index == 0) {
    		String header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
        			"index",
        			"Identifier",
        			"csAlg",
        			"oiCsStatus",
        			"gCsStatus",
        	    	"oi.objectFormat",
        	    	"oi.size",
        	    	"obj.byteLength",
        	    	"oi.sysMetadataModified");
        	System.out.println(header);
    	}
    	String row = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
    			index,
    			oi.getIdentifier().getValue(),
    			oi.getChecksum().getAlgorithm(),
    			oiStatus,
    			gStatus,    	    	
    	    	oi.getObjectFormat(),
    	    	oi.getSize(),
    	    	obj.length,
    	    	oi.getDateSysMetadataModified());
    	System.out.println(row);
    }
    
    public String evaluateChecksum(Checksum cs, byte[] object) 
    {
    	String algStr = null;
    	try {
    		algStr = cs.getAlgorithm().toString();
    		String calcCs;

    		calcCs = ExampleUtilities.checksum(object, algStr);

    		if (cs.getValue().equals(calcCs)) 
    			return "calc:" + algStr;

    		byte[][] newlineAdded = { object, "\n".getBytes() };
    		String recalcCs = ExampleUtilities.checksum(newlineAdded, algStr);
    		if (cs.getValue().equals(recalcCs))
    			return "recalc:" + algStr;

    	} catch (NoSuchAlgorithmException e) {
    		return "noSuchAlgorithm: " + algStr;
    	}
    	return "noMatch";

    }
    
 //   @Test
    public void testGetChecksumSessionIdentifierTypeString() 
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
                printHeader("testGetChecksumSessionIdentifierTypeString - node " + nodeList.get(i).getBaseURL());
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
                Session token = null; // mn.login(principal, "kepler");
                String idString = prefix + ExampleUtilities.generateIdentifier();
                Identifier guid = new Identifier();
                guid.setValue(idString);
                objectStream = IOUtils.toInputStream(doc);
                SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, 
                		"eml://ecoinformatics.org/eml-2.1.0", objectStream);
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
                            " error in testGetChecksumSessionIdentifierTypeString: " + e.getMessage()));
                }
            }
            catch(Exception e)
            {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testGetChecksumSessionIdentifierTypeString: " + e.getMessage()));
            }
        }
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
