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
import org.junit.Ignore;


/**
 * Test class to test client SSL connection scenarios
 * vs. Member nodes and a reference server
 * @author rnahf
 *
 */
public class ClientAuthTestCase extends ContextAwareTestCaseDataone {
//	private String aUrl = "https://knb-test-1.dataone.org/knb/d1/mn";	
	private static String currentUrl;
	
	/**
	 * Uncomment for locally running tests against DEV environment
	 */
//	@BeforeClass
	public static void settingContext() {
		System.setProperty("context.label", "DEV");
	}

	@Test
	public void testNoCertificate_ReferenceServer() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://repository.dataone.org/";
		
		D1RestClient rc = new D1RestClient(false);
		InputStream is = rc.doGetRequest(url);
	}

	@Ignore("ignore until we configure tests with certificates")
	@Test
	public void testCertificate_ReferenceServer() 
	throws BaseException, IllegalStateException, ClientProtocolException, IOException, HttpException
	{	
		String url = "https://repository.dataone.org/";
		
		D1RestClient rc = new D1RestClient();
		InputStream is = rc.doGetRequest(url);
	}
	
	
	@Test
	public void testNoCertificate_MNs() 
	throws IllegalStateException, ClientProtocolException, IOException, HttpException
	{
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			try {
				currentUrl = it.next().getBaseURL();
				D1RestClient rc = new D1RestClient();
				log.info("CurrentURL = " + currentUrl);
				InputStream is = rc.doGetRequest(currentUrl);
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

	
	@Ignore("ignore until we configure tests with certificates")
	@Test
	public void testCertificate_MNs() 
	throws IllegalStateException, ClientProtocolException, IOException, HttpException
	{
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			try {
				currentUrl = it.next().getBaseURL();
				D1RestClient rc = new D1RestClient();
				log.info("CurrentURL = " + currentUrl);
				InputStream is = rc.doGetRequest(currentUrl);
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
