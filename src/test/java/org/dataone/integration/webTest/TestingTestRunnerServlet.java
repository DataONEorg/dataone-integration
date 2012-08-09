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

package org.dataone.integration.webTest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;


public class TestingTestRunnerServlet {

//	@Test
//	public void testSettingsTest() {
//		System.setProperty(TestSettings.CONTEXT_OVERRIDE_URI, "org/dataone/configuration/overriding.properties");
//		String classNamePattern = Settings.getConfiguration().getString("webTest.mn.testCase.pattern");
//		assertEquals("unit testing catches overriding property for 'webTest.mn.testCase.pattern'",
//				"*MockItCase", classNamePattern);
//	}
	
	
//	@Test
	public void testMaxTierLogic() {
		
	}
	
	@Test
	public void callServletTest() throws IOException
	{
		String mNodeUrl = "http://demo.test.dataone.org/knb/d1/mn";
		
		// set up mock objects
		ServletContext sc = new MockServletContext("src/main/webapp", null);
		MockHttpServletRequest request = new MockHttpServletRequest(sc, null,
				"/some/path?mNodeUrl=NodeFromMockClient");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setParameter("mNodeUrl",mNodeUrl);
		request.setParameter("maxTier","Tier 4");
		request.addHeader("accept", (Object) "text/xml");
		request.setMethod("GET");
		
		// call the servlet
		TestRunnerHttpServlet servlet = new TestRunnerHttpServlet(true);
		try {
			servlet.doGet(request, response);
		} catch (ServletException se) {
			fail("servlet exception at servlet.doGet(): " + se);
		} catch (IOException ioe) {
			fail("IO exception at servlet.goGet(): " + ioe);
		}
		
		// process the response - did it return anything meaningful?
		String responseString = response.getContentAsString();
		System.out.println(responseString);
		assertTrue("Url should be successfully passed to servlet",responseString.contains(mNodeUrl));
		assertTrue("response should contain final summary line",responseString.contains("RunCount"));
	}

}
