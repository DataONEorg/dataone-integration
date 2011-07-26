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
	
	
	@Test
	public void callServletTest() throws IOException
	{
		String mNodeUrl = "http://cn-dev.dataone.org/knb/d1/mn";
		
		// set up mock objects
		ServletContext sc = new MockServletContext("src/main/webapp", null);
		MockHttpServletRequest request = new MockHttpServletRequest(sc, null,
				"/some/path?mNodeUrl=NodeFromMockClient");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setParameter("mNodeUrl",mNodeUrl);
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
		assertTrue("Url successfully passed to servlet",responseString.contains(mNodeUrl));
	}

}
