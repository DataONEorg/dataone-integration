package org.dataone.integration.it;


import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.http.HttpException;
import org.apache.http.client.ClientProtocolException;
import org.dataone.client.D1RestClient;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.junit.BeforeClass;
import org.junit.Test;
/**
 * Test class to test client SSL connection scenarios
 * @author rnahf
 *
 */
public class ClientAuthTestCase { // extends ContextAwareTestCaseDataone {

	private static String currentUrl;
	
	@BeforeClass
	public static void settingContext() {
		System.setProperty("context.label", "DEV");
	}

	@Test
	public void testNoCertificateGeneral() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://repository.dataone.org/";
		
		D1RestClient rc = new D1RestClient(false);
		InputStream is = rc.doGetRequest(url);
	}

	@Test
	public void testNoCertificateMN() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://knb-test-1.dataone.org/knb/d1/mn";	
		
		D1RestClient rc = new D1RestClient(false);
		InputStream is = rc.doGetRequest(url);
	}
	
	@Test
	public void testCertificateGeneral() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://repository.dataone.org/";
		
		D1RestClient rc = new D1RestClient();
		InputStream is = rc.doGetRequest(url);
	}

	@Test
	public void testCertificateMN() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://knb-test-1.dataone.org/knb/d1/mn";	
		
		D1RestClient rc = new D1RestClient();
		 InputStream is = rc.doGetRequest(url);
	}
	
	
	
//	@Test
//	public void testNoCertificateMNs() 
//	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
//	{
//		Iterator<Node> it = getMemberNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			D1RestClient rc = new D1RestClient();
//			InputStream is = rc.doGetRequest(currentUrl);
//		}	
////		String url = "https://knb-test-1.dataone.org/knb/d1/mn";				
//	}

//	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return "A class to test connection to the specified nodes";
	}
	
}
