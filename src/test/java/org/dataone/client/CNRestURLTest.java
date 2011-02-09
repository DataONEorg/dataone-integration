package org.dataone.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Node.ResponseData;
import org.dataone.service.Constants;
import org.dataone.service.EncodingUtilities;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.AuthToken;
import org.junit.Test;
import org.junit.Assume;
import org.junit.Rule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class CNRestURLTest {
	private static final String cnUrl = "http://cn-dev.dataone.org/cn/";
	
	@Test
	public void testListObject_noParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"");
	}

	// TODO:  define and implement rest interface for search
//	@Test
	public void testListObjectSolr_noParams() throws IOException {

		testRunner( 400,
					Constants.RESOURCE_OBJECTS, 
					"qt=solr"	);
	}

	// TODO:  define and implement rest interface for search
//	@Test
	public void testListObjectSolr_minimalParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=solr&pageSize=10&start=0"	);
	}
	
	// TODO:  define and implement rest interface for search
//	@Test
	public void testListObjectSolr_incompleteParams() throws IOException {

		testRunner( 400,
					Constants.RESOURCE_OBJECTS, 
					"qt=solr&pageSize=10"	);
	}

	// TODO:  define and implement rest interface for search
//	@Test
	public void testListObjectSolr_unknownParams() throws IOException {

		testRunner( 400,
					Constants.RESOURCE_OBJECTS, 
					"qt=solr&Fred=boy"	);
	}

	// TODO:  define and implement rest interface for search
//	@Test
	public void testListObjectSolr_requiredPlusUnknownParams() throws IOException {

		testRunner( 400,
					Constants.RESOURCE_OBJECTS, 
					"qt=solr&pageSize=10&start=0&Fred=boy"	);
	}

	//==================   qt=path tests =======================//
	
	@Test
	public void testListObjectPath_noParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path");
	}
	
	@Test
	public void testListObjectPath_unknownParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path&Fred=boy"	);
	}


	@Test
	public void testListObjectPath_knownPlusUnknownParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path&start=0&count=10&Fred=boy");
	}

	// ==============  resolve url tests =======================//

	@Test
	public void testResolve_errorForwarding() throws IOException {

		testRunner( 404,
					Constants.RESOURCE_RESOLVE + "/a_bogus_ID",  
					"");
	}

	@Test
	public void testResolve_errorForwarding_nullID() throws IOException {

		testRunner( 500,
					Constants.RESOURCE_RESOLVE,  
					"");
	}

	@Test
	public void testResolve_errorForwarding_nullID_unknownParams() throws IOException {

		testRunner( 500,
					Constants.RESOURCE_RESOLVE,  
					"Fred=boy");
	}

	
	
	@Test
	public void testResolve_errorForwarding_unknownQueryParams() throws IOException {

		ResponseData rd = sendHttpGet(null,Constants.RESOURCE_OBJECTS, "qt=path&count=1");
		String ol = IOUtils.toString(rd.getContentStream());
		String someID = null;
		if (ol.contains("<identifier>"))
			someID = ol.substring(ol.indexOf("<identifier>")+12, ol.indexOf("</identifier>"));
		else
			fail("Can't get identifier from objectList");
		testRunner( 200,
					Constants.RESOURCE_RESOLVE + "/" + EncodingUtilities.encodeUrlPathSegment(someID),
					"Fred=boy");
	}

		
	private void testRunner(int expectedCode, String resourcePath, String param) throws IOException {
		System.out.println("===================================================================");
		ResponseData rd = sendHttpGet(null, resourcePath, param);
		int status = rd.getCode();
		
		System.out.println("*** expected  = " + expectedCode);
		System.out.println("*** http code = " + status);
		if (rd.getContentStream() != null) {
			String content = IOUtils.toString(rd.getContentStream());
			if (content.length() > 1000)
				System.out.println("*** contentStream = " + content.substring(0, 1000) + " ...");
			else
				System.out.println("*** contentStream = " + content);
		}
		if (rd.getErrorStream() != null) 
			System.out.println("*** errorStream = " + IOUtils.toString(rd.getErrorStream()));
		
		rd = sendHttpGet(null, resourcePath, param);
		Integer errorCode = null;
		if (rd.getContentStream() != null)
			errorCode = readErrorStreamForErrorCode(rd.getContentStream());	
		if (rd.getErrorStream() != null) 
			errorCode = readErrorStreamForErrorCode(rd.getErrorStream());

		System.out.println("*** errorCode = " + errorCode);	

		int code = status;
		if (errorCode != null)
			code = errorCode.intValue();

		System.out.println("=== code for testing = " + code);
		
		System.out.println();
//		assertTrue("expected Status " + expectedCode, status == expectedCode);
		assertEquals("expected Status ",expectedCode, code);
	}
	
	
	private ResponseData sendHttpGet(AuthToken token, String resource, 
			 String urlParameters) throws IOException {

		CNode cn = new CNode(cnUrl);
		String method = Constants.GET;
		ResponseData resData = cn.new ResponseData();
		HttpURLConnection connection = null;
		
		// append the token onto the parameter string.
		if (token != null) {
			if (urlParameters.trim().length() > 0)
				urlParameters += "&sessionid=" + token.getToken();
			else
				urlParameters = "sessionid=" + token.getToken();
		}

		// encode and assemble the url
		String restURL = cn.getNodeBaseServiceUrl() + resource;
		if (urlParameters.trim().length() > 0)
			if (!urlParameters.startsWith("?")) 
				restURL += "?";
		restURL += urlParameters;

		System.out.println("restURL: " + restURL);
		System.out.println("method: " + method);

		URL u = new URL(restURL);
		connection = (HttpURLConnection) u.openConnection();

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method);
		connection.connect();			

		try {
			InputStream content = connection.getInputStream();
			resData.setContentStream(content);
		} catch (IOException ioe) {
			System.out.println("tried to get content and failed.  Receiving an error stream instead.");
			// will set errorStream outside of catch
		}

		int code = connection.getResponseCode();
		resData.setCode(code);
		resData.setHeaderFields(connection.getHeaderFields());
		if (code != HttpURLConnection.HTTP_OK) 
			resData.setErrorStream(connection.getErrorStream());
			
		return resData;
	}
	
	private Integer readErrorStreamForErrorCode(InputStream errorStream) {
		
		Integer errorCode = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(errorStream);
			Element root = doc.getDocumentElement();
			root.normalize();
			errorCode = new Integer(getIntAttribute(root, "errorCode"));
		} catch (Exception e) {
			System.out.println("not able to get errorCode value from the given (error)Stream");
		}
		return errorCode;
	}
	
	private int getIntAttribute(Element e, String attName)
	throws NumberFormatException {
		String attText = e.getAttribute(attName);
		int x = Integer.parseInt(attText);
		return x;
	}

	
}
