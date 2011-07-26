package org.dataone.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.util.Constants;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;


public class D1ClientTestProtected {
	private static final String baseURL = "http://cn-dev.dataone.org";
	private static final String cnUrl = "http://cn-dev.dataone.org/cn/";
	private static final String mnUrl = "http://cn-dev.dataone.org/knb/d1/";

	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();

	
	
	private HttpResponse executeGetRequest(String resource, String queryString) 
	throws ClientProtocolException, IOException {
		
		RestClient rc = new RestClient();
		String url = cnUrl + resource;
		if (queryString != null) {
			url += "?" + queryString;
		}
		return rc.doGetRequest(url);
	}
	
	
	private void runBadParametersTests(String resource, String urlParameters) 
	throws ServiceFailure, ClientProtocolException, IOException
	{
		HttpResponse rd = executeGetRequest(resource,urlParameters);
		
		// First handle any errors that were generated	
		int code = rd.getStatusLine().getStatusCode();
		System.out.println("code = " + code);
		if (code != 200) {
			InputStream errorStream = rd.getEntity().getContent();
			try {
				D1Node.deserializeAndThrowException(code,errorStream);
				assertTrue("Bad parameters should have thrown an exception", false);
			} catch (InvalidRequest e) {
				System.out.println("=== Exception code: " + e.getCode());
				System.out.println("=== Exception type: " + e.getClass());
				System.out.println("=== exception msg:  " + e.getMessage());
				assertTrue("Bad parameters should result in invalidRequest exception",true);
			} catch (BaseException e) {
				System.out.println("=== Exception code: " + e.getCode());
				System.out.println("=== Exception type: " + e.getClass());
				System.out.println("=== exception msg:  " + e.getMessage());
				assertTrue("Unexpected dataone exception: " + e.getCode() + " " + e.getClass(), false);
			} catch (Exception e) {
				System.out.println("=== Exception type: " + e.getClass());
				System.out.println("=== exception msg:  " + e.getMessage());
				assertTrue("Unexpected exception: " + e.getClass(), false);
			}
		} else {
			assertTrue("Exception should have been thrown.", false);
		}
	}

	
	@Test
	public void testBadParametersVsListObjects() throws ServiceFailure, ClientProtocolException, IOException
	{
		String resource = Constants.RESOURCE_OBJECTS;
		String urlParameters = "xxx=XXX&yyy=YYY";

		runBadParametersTests(resource, urlParameters);
	}
	
	
	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Test
    public void testTrailingSlashes_Metacat()
    {
    	String baseURL = "http://cn-dev.dataone.org/knb/d1";
    	MNode node = new MNode(baseURL);

    	String localhostName = "";
    	try {
			 localhostName = InetAddress.getLocalHost().getHostName();
			 System.out.println("Localhost Name: " + localhostName);
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}
    	// need to add Assume test to see if the test can be run:
    	// don't always have a localhost set up.
 		try {
 			if (!localhostName.contains("cn-dev")) {
 				URL u = new URL(node.getNodeBaseServiceUrl());
 				HttpURLConnection connection = null;	
 				connection = (HttpURLConnection) u.openConnection();
 				connection.connect();
 			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			System.out.println("Web server '" + baseURL + "' assumed not set up. Skipping test.");
			Assume.assumeTrue(false);
		}
    	
    	try
        {
            String resource = Constants.RESOURCE_OBJECTS;
            String params = "";

            if (!params.equals("")) {
                params += "&";
            }

            params += "replicaStatus=false";
            params += "&";
            params += "start=0";
            params += "&";
            params += "count=100";
            
//            AuthToken token = new AuthToken("public");

            //without trailing slash
            HttpResponse resp1 = executeGetRequest(resource,params);
            //with trailing slash
            HttpResponse resp2 = executeGetRequest(resource + "/",params);

            String rd1response = IOUtils.toString(resp1.getEntity().getContent());            
            String rd1Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd1response);
            String rd2response = IOUtils.toString(resp2.getEntity().getContent());
            String rd2Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd2response);
            assertEquals(rd1Total, rd2Total);
        }
        catch(Exception e)
        {
            errorCollector.addError(new Throwable(
                    "Unexpected Exception in testTrailingSlashes: " + e.getMessage()));
        }
    }

    
	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Test
    public void testTrailingSlashes_CN()
    {
    	String baseURL = "http://cn-dev.dataone.org/cn";
    	MNode node = new MNode(baseURL);

    	String localhostName = "";
    	try {
			 localhostName = InetAddress.getLocalHost().getHostName();
			 System.out.println("Localhost Name: " + localhostName);
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}
    	// need to add Assume test to see if the test can be run:
    	// don't always have a localhost set up.
 		try {
 			if (!localhostName.contains("cn-dev")) {
 				URL u = new URL(node.getNodeBaseServiceUrl());
 				HttpURLConnection connection = null;	
 				connection = (HttpURLConnection) u.openConnection();
 				connection.connect();
 			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			System.out.println("Web server '" + baseURL + "' assumed not set up. Skipping test.");
			Assume.assumeTrue(false);
		}
    	
    	try
        {
            String resource = Constants.RESOURCE_OBJECTS;
            String params = "";


            System.out.println(resource);
            
 //           AuthToken token = new AuthToken("public");

            //without trailing slash
            HttpResponse resp1 = executeGetRequest(resource,params);
            //with trailing slash
            HttpResponse resp2 = executeGetRequest(resource + "/",params);
            
            String rd1response = IOUtils.toString(resp1.getEntity().getContent());            
            String rd1Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd1response);
            String rd2response = IOUtils.toString(resp2.getEntity().getContent());
            String rd2Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd2response);
            assertEquals(rd1Total, rd2Total);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        	errorCollector.addError(new Throwable(
                    "Unexpected Exception in testTrailingSlashes: " + e.getMessage()));
        }
    }
    
	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Ignore("need to implement public tokens for 0.6.2")
    @Test
    public void testTrailingSlashes_CN_publicToken()
    {
    	String baseURL = "http://cn-dev.dataone.org/cn";
    	MNode node = new MNode(baseURL);

    	String localhostName = "";
    	try {
			 localhostName = InetAddress.getLocalHost().getHostName();
			 System.out.println("Localhost Name: " + localhostName);
		} catch (UnknownHostException e2) {
			e2.printStackTrace();
		}
    	// need to add Assume test to see if the test can be run:
    	// don't always have a localhost set up.
 		try {
 			if (!localhostName.contains("cn-dev")) {
 				URL u = new URL(node.getNodeBaseServiceUrl());
 				HttpURLConnection connection = null;	
 				connection = (HttpURLConnection) u.openConnection();
 				connection.connect();
 			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			System.out.println("Web server '" + baseURL + "' assumed not set up. Skipping test.");
			Assume.assumeTrue(false);
		}
    	
    	try
        {
            String resource = Constants.RESOURCE_OBJECTS;
            String params = "";


            System.out.println(resource);
            
//            AuthToken token = new AuthToken("public");

            //without trailing slash
            HttpResponse resp1 = executeGetRequest(resource,params);
            //with trailing slash
            HttpResponse resp2 = executeGetRequest(resource + "/",params);
            
            String rd1response = IOUtils.toString(resp1.getEntity().getContent());            
            String rd1Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd1response);
            String rd2response = IOUtils.toString(resp2.getEntity().getContent());
            String rd2Total = D1ClientTestProtected.extractObjectListTotalAttribute(rd2response);
            assertEquals(rd1Total, rd2Total);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        	errorCollector.addError(new Throwable(
                    "Unexpected Exception in testTrailingSlashes: " + e.getMessage()));
        }
    }
    
  
    protected static String extractObjectListTotalAttribute(String ol) {
    	Pattern pat = Pattern.compile("total=\"\\d+\"");

		Matcher mat = pat.matcher(ol);
		String totalPattern = null;
		if (mat.find())
			totalPattern = mat.group();
		return totalPattern;
    }
	
}
