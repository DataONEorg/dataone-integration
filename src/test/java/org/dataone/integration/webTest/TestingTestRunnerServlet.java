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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.Ignore;
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
        request.setParameter("selectedAPIs", "Mock");
        request.setParameter("selectedVersion", "V1");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        // call the servlet
        try {
            TestRunnerHttpServlet servlet = new TestRunnerHttpServlet(true);

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

//    @Test
//    public void testDeriveSelectedTierLevels() {
//        Integer[] levels = TestRunnerHttpServlet.deriveSelectedTierLevels(new String[]{"Tier 1","Tier 3","Tier4"});
//        assertTrue("the number of levels should be 3", levels.length == 3);
//        assertEquals("the first level is 1", new Integer(1), levels[0]);
//        assertEquals("the second level is 3", new Integer(3), levels[1]);
//        assertEquals("the third level is 4", new Integer(4), levels[2]);
//    }

//    @Ignore("need to update this due to new class names")
    @Test
    public void callServletTest_ITtestSelector() throws IOException
    {
        String mNodeUrl = "http://demo.test.dataone.org/knb/d1/mn";

        // set up mock objects
        ServletContext sc = new MockServletContext("src/main/webapp", null);
        MockHttpServletRequest request = new MockHttpServletRequest(sc, null,
                "/some/path?mNodeUrl=NodeFromMockClient");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("mNodeUrl",mNodeUrl);
        request.setParameter("selectedAPIs", new String[]{"MNCore","MNStorage"});
        request.setParameter("selectedVersion", "V1");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        // call the servlet
        try {
            TestRunnerHttpServlet servlet = new TestRunnerHttpServlet();

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
        assertTrue("response should contain 'MNCore'", responseString.contains("org.dataone.integration.it.apiTests.MNCore"));
        assertTrue("response should contain 'MNStorage'", responseString.contains("org.dataone.integration.it.apiTests.MNStorage"));
        assertFalse("response should NOT contain 'MNRead'", responseString.contains("org.dataone.integration.it.apiTests.MNRead"));
        assertFalse("response should NOT contain 'MNReplication'", responseString.contains("org.dataone.integration.it.apiTests.MNReplication"));
    }

    @Test
    public void testClassAnnotation() throws IOException {
        String mNodeUrl = "http://demo.test.dataone.org/knb/d1/mn";

        // set up mock objects
        ServletContext sc = new MockServletContext("src/main/webapp", null);
        MockHttpServletRequest request = new MockHttpServletRequest(sc, null,
                "/some/path?mNodeUrl=NodeFromMockClient");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setParameter("mNodeUrl",mNodeUrl);
        request.setParameter("selectedAPIs", "Mock");
        request.setParameter("selectedVersion", "V1");
        request.addHeader("accept", (Object) "text/xml");
        request.setMethod("GET");

        // call the servlet
        try {
            TestRunnerHttpServlet servlet = new TestRunnerHttpServlet(true);

            servlet.doGet(request, response);
        } catch (ServletException se) {
            fail("servlet exception at servlet.doGet(): " + se);
        } catch (IOException ioe) {
            fail("IO exception at servlet.goGet(): " + ioe);
        }

        // process the response - did it return anything meaningful?
        String responseString = response.getContentAsString();

        assertTrue("response should contain the test case class annotation",
                responseString.contains("TestCase class annotation"));
    }

}
