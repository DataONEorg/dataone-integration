package org.dataone.integration.webTest;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;


public class TestingTestRunnerServlet {

	@Test
	public void callServletTest() throws IOException
	{

		//	        ResourceLoader fsrl = new FileSystemResourceLoader();
		ServletContext sc = new MockServletContext("src/main/webapp", null);
		//	        MockFilterConfig fc = new MockFilterConfig(ProxyWebApplicationContextLoader.SERVLET_CONTEXT, "ResolveFilter");

		MockHttpServletRequest request = new MockHttpServletRequest(sc, null, "/some/path?mNodeUrl=NodeFromMockClient");
		request.setParameter("mNodeUrl","nodeFromMockClientRequest");
		request.addHeader("accept", (Object) "text/xml");
		request.setMethod("GET");

		TestRunnerHttpServlet servlet = new TestRunnerHttpServlet();

		MockHttpServletResponse response = new MockHttpServletResponse();
		
        // need to wrap the response to examine
//        BufferedHttpResponseWrapper responseWrapper =
//                new BufferedHttpResponseWrapper((HttpServletResponse) response);
	
		try {
			servlet.doGet(request, response);
		} catch (ServletException se) {
			fail("servlet exception at servlet.doGet(): " + se);
		} catch (IOException ioe) {
			fail("IO exception at servlet.goGet(): " + ioe);
		}
		System.out.println(response.getContentAsString());
	}

}
