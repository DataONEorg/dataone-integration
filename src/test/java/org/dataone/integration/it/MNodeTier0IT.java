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
import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.client.v1.MNode;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.util.D1Url;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier0IT extends ContextAwareTestCaseDataone  {

    private  String currentUrl;
    
	@Override
	protected String getTestDescription() {
		return "Test Case to run basic connectivity tests";
	}

	/**
	 * calling the baseUrl directly (without further path elements) should
	 * be the same as mn.getCapabilities()  - should return a nodelist.
	 */
	@Test
	public void testBaseUrlResponse() {
		setupClientSubject_NoCert();
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testBaseUrlResponse() vs. node: " + currentUrl);
		
			D1Url url = new D1Url(mn.getNodeBaseServiceUrl());
            MultipartRestClient rc = new DefaultHttpMultipartRestClient();

			try {
				InputStream is = rc.doGetRequest(url.getUrl(), null);
				log.info("called GET to " + url.getUrl());
				Node node = TypeMarshaller.unmarshalTypeFromStream(Node.class, is);
			} 
			catch (BaseException e) {
				handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,"failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}

	
}
