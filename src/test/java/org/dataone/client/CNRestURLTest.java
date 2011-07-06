package org.dataone.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.dataone.configuration.Settings;
import org.dataone.service.Constants;
import org.dataone.service.D1Url;
import org.dataone.service.EncodingUtilities;
import org.junit.Test;
import org.junit.Ignore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;



public class CNRestURLTest {
	private static final String cnUrl = Settings.getConfiguration().getString("context.cn.baseurl");

	@Test
	public void testTrue() {
		
	}
	@Ignore("not adapted for v0.6.x")
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

	@Ignore("not adapted for v0.6.x")
	@Test
	public void testListObjectPath_noParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path");
	}

	
	@Ignore("not adapted for v0.6.x")
	@Test
	public void testListObjectPath_unknownParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path&Fred=boy"	);
	}

	@Ignore("not adapted for v0.6.x")
	@Test
	public void testListObjectPath_knownPlusUnknownParams() throws IOException {

		testRunner( 200,
					Constants.RESOURCE_OBJECTS, 
					"qt=path&start=0&count=10&Fred=boy");
	}

	// ==============  resolve url tests =======================//
	
	
	@Ignore("not adapted for v0.6.x")
	@Test
	public void testResolve_errorForwarding() throws IOException {

		testRunner( 404,
					Constants.RESOURCE_RESOLVE + "/a_bogus_ID",  
					"");
	}
	
	
	@Ignore("not adapted for v0.6.x")
	@Test
	public void testResolve_errorForwarding_nullID() throws IOException {

		testRunner( 404,
					Constants.RESOURCE_RESOLVE,  
					"");
	}
	
	
	@Ignore("not adapted for v0.6.x")
	@Test
	public void testResolve_errorForwarding_nullID_unknownParams() throws IOException {

		testRunner( 404,
					Constants.RESOURCE_RESOLVE,  
					"Fred=boy");
	}

	
	@Ignore("not adapted for v0.6.x")	
	@Test
	public void testResolve_errorForwarding_unknownQueryParams() throws IOException {

		RestClient rc = new RestClient();
		D1Url url = new D1Url(cnUrl);
		url.addNextPathElement(Constants.RESOURCE_OBJECTS);
		url.addPreEncodedNonEmptyQueryParams("qt=path&count=1");
		
		HttpResponse response = rc.doGetRequest(url.getUrl());
		
		String ol = IOUtils.toString(response.getEntity().getContent());
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
		
		RestClient rc = new RestClient();
		D1Url url = new D1Url(cnUrl);
		url.addNextPathElement(resourcePath);
		url.addPreEncodedNonEmptyQueryParams(param);
		
		HttpResponse response = rc.doGetRequest(url.getUrl());
		int status = response.getStatusLine().getStatusCode();
		
		System.out.println("*** expected  = " + expectedCode);
		System.out.println("*** http code = " + status);
	
		InputStream responseStream = response.getEntity().getContent();

		String content = IOUtils.toString(responseStream);
		if (content.length() > 1000)
			System.out.println("*** contentStream = " + content.substring(0, 1000) + " ...");
		else
			System.out.println("*** contentStream = " + content);


		HttpResponse response2 = rc.doGetRequest(url.getUrl());	
		InputStream is2 = response2.getEntity().getContent();
		Integer errorCode = readErrorStreamForErrorCode(is2);	

		System.out.println("*** errorCode = " + errorCode);	

		int code = status;
		if (errorCode != null)
			code = errorCode.intValue();

		System.out.println("=== code for testing = " + code);
		
		System.out.println();
//		assertTrue("expected Status " + expectedCode, status == expectedCode);
		assertEquals("expected Status ",expectedCode, code);
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
