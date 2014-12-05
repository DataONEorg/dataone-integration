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


import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;


/**
 * Test class to test client SSL connection scenarios
 * vs. Member nodes and a reference server
 * @author rnahf
 *
 */
@Deprecated
public class ClientAuthIT extends ContextAwareTestCaseDataone {
//	private String aUrl = "https://knb-test-1.dataone.org/knb/d1/mn";	
	private static String currentUrl;
	
	/**
	 * Uncomment for locally running tests against DEV environment
	 */
	@BeforeClass
	public static void settingContext() {
		System.setProperty("context.label", "DEV");
	}

	@Test
	public void testNoCertificate_ReferenceServer() 
	throws BaseException, ClientSideException
	{	
		String url = "https://repository.dataone.org/";
		
		MultipartRestClient rc = new DefaultHttpMultipartRestClient();
		InputStream is = rc.doGetRequest(url,null);
	}

	@Ignore("ignore until we configure tests with certificates")
	@Test
	public void testCertificate_ReferenceServer() 
	throws BaseException, ClientSideException
	{	
		String url = "https://repository.dataone.org/";
		
		MultipartRestClient rc = new DefaultHttpMultipartRestClient();
		InputStream is = rc.doGetRequest(url,null);
	}
	
	
	@Test
	public void testNoCertificate_MNs() 
	throws IllegalStateException, ClientProtocolException, IOException, HttpException
	{
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			try {
				currentUrl = it.next().getBaseURL();
				MultipartRestClient rc = new DefaultHttpMultipartRestClient();
				log.info("CurrentURL = " + currentUrl);
				InputStream is = rc.doGetRequest(currentUrl,null);
			} 
			catch (ServiceFailure e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch (BaseException e) {
				log.info(currentUrl +":: exceptionThrown: " + 
						e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch(Exception e) {
				String msg = e.getClass().getSimpleName() + ":: " + e.getMessage();
				handleFail(currentUrl,msg);
				log.warn(currentUrl + ":: " + msg);
			}	
		}		
	}

	
	@Ignore("ignore until we configure tests with certificates")
	@Test
	public void testCertificate_MNs() 
	throws IllegalStateException, ClientProtocolException, IOException, HttpException
	{
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			try {
				currentUrl = it.next().getBaseURL();
				MultipartRestClient rc = new DefaultHttpMultipartRestClient();
				log.info("CurrentURL = " + currentUrl);
				InputStream is = rc.doGetRequest(currentUrl,null);
			} 
			catch (BaseException e) {
				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch(Exception e) {
				String msg = e.getClass().getSimpleName() + ":: " + e.getMessage();
				handleFail(currentUrl,msg);
				log.warn(currentUrl + ":: " + msg);
			}	
		}		
	}
	
	
	
	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return "A class to test connection to the specified nodes";
	}
	
}
