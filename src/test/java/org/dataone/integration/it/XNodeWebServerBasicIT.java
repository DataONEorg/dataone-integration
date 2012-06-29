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
 * 
 * $Id$
 */

package org.dataone.integration.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.dataone.client.CNode;
import org.dataone.client.MNode;
import org.dataone.client.RestClient;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.util.Constants;
import org.dataone.service.util.D1Url;
import org.dataone.service.util.ExceptionHandler;
import org.junit.Ignore;
import org.junit.Test;


public class XNodeWebServerBasicIT extends ContextAwareTestCaseDataone {

	protected static Log log = LogFactory.getLog(XNodeWebServerBasicIT.class);
	protected String currentUrl;

	@Override
	protected String getTestDescription() {
		
		return "Tests various web server configurations (trailing slashes, etc.)";
	}
	
	
	
	private HttpResponse executeGetRequest(String baseUrl, String resource, String queryString) 
	throws ClientProtocolException, IOException {
		
		RestClient rc = new RestClient();
		D1Url url = new D1Url(baseUrl,resource);
		String urlString = url.getUrl();
		if (queryString != null) {
			urlString += "?" + queryString;
		}
		return rc.doGetRequest(urlString);
	}
	
	
	private void runBadParametersTests(String baseUrl, String resource, String urlParameters) 
	throws ServiceFailure, ClientProtocolException, IOException
	{

		HttpResponse rd = executeGetRequest(baseUrl,resource,urlParameters);
		
		// First handle any errors that were generated	
		int code = rd.getStatusLine().getStatusCode();
		log.info("code = " + code);
		if (code != 200) {
			try {
				ExceptionHandler.deserializeAndThrowException(rd);
				fail("Bad parameters should have thrown an exception");
			} catch (InvalidRequest e) {
				log.info("=== Exception code: " + e.getCode());
				log.info("=== Exception type: " + e.getClass());
				log.info("=== exception msg:  " + e.getMessage());
				assertTrue("Bad parameters should result in invalidRequest exception",true);
			} catch (BaseException e) {
				log.info("=== Exception code: " + e.getCode());
				log.info("=== Exception type: " + e.getClass());
				log.info("=== exception msg:  " + e.getMessage());
				assertTrue("Unexpected dataone exception: " + e.getCode() + " " + e.getClass()
						+ ": " + e.getDescription(), false);
			} catch (Exception e) {
				log.info("=== Exception type: " + e.getClass());
				log.info("=== exception msg:  " + e.getMessage());
				assertTrue("Unexpected exception: " + e.getClass(), false);
			}
		} else {
			assertTrue("Exception should have been thrown.", false);
		}
	}

	@Ignore("non deterministic test: extra parameter can be ignored")
	@Test
	public void testBadParameters_CNListObjects() throws ServiceFailure, ClientProtocolException, IOException
	{
    	Iterator<Node> it = getCoordinatingNodeIterator();
       	while (it.hasNext()) {
    		String currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testBaseParameters_CNListObjects() vs. node: " + currentUrl); 
		
    		String resource = Constants.RESOURCE_OBJECTS;
    		String urlParameters = "xxx=XXX&yyy=YYY";

    		runBadParametersTests(cn.getNodeBaseServiceUrl(), resource, urlParameters);
       	}
	}
	

	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Test
    public void testTrailingSlashes_CN()
    {
    	Iterator<Node> it = getCoordinatingNodeIterator();
       	while (it.hasNext()) {
    		String currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testTrailingSlashes_CN() vs. node: " + currentUrl);  
    		runTrailingSlashesTest(cn.getNodeBaseServiceUrl(),"");
       	}
    } 
      
	
	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Test
    public void testTrailingSlashes_MN()
    {
    	Iterator<Node> it = getMemberNodeIterator();

        String params = "replicaStatus=false";
        params += "&";
        params += "start=0";
        params += "&";
        params += "count=100";
    	
        currentUrl = it.next().getBaseURL();
		MNode mn = new MNode(currentUrl);
		printTestHeader("testTrailingSlashes_MN() vs. node: " + currentUrl);  
		runTrailingSlashesTest(mn.getNodeBaseServiceUrl(),params);
    }
    
    
    private void runTrailingSlashesTest(String baseUrl, String params) 
    {
    	try {
    		// assemble the actual call by hand, so we can mess with the format
    		String resource = Constants.RESOURCE_OBJECTS;

    		//without trailing slash
    		HttpResponse resp1 = executeGetRequest(baseUrl,resource,params);
    		String rd1response = IOUtils.toString(resp1.getEntity().getContent());            
    		String rd1Total = XNodeWebServerBasicIT.extractObjectListTotalAttribute(rd1response);

    		//now with trailing slash interposed so: path/?params
    		HttpResponse resp2 = executeGetRequest(baseUrl,resource + "/",params);			
    		String rd2response = IOUtils.toString(resp2.getEntity().getContent());
    		String rd2Total = XNodeWebServerBasicIT.extractObjectListTotalAttribute(rd2response);

    		assertEquals(rd1Total, rd2Total);
    	} 
    	catch(Exception e) {
    		e.printStackTrace();
    		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    	}
    }
    
  
    protected static String extractObjectListTotalAttribute(String ol) {
    	Pattern pat = Pattern.compile("total=\"\\d+\"");

		Matcher mat = pat.matcher(ol);
		String totalPattern = null;
		if (mat.find())
			totalPattern = mat.group();
		return totalPattern;
    }
}
