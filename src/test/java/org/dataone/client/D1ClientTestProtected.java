package org.dataone.client;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Node.ResponseData;
import org.dataone.service.Constants;
import org.dataone.service.types.AuthToken;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;


public class D1ClientTestProtected {

	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();

	
	
	/**
     * test that trailing slashes do not affect the response of the node
     */
    @Test
    public void testTrailingSlashes()
    {
    	String baseURL = "http://localhost:8080/knb";
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
            InputStream is = null;
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
            
            AuthToken token = new AuthToken("public");

            //without trailing slash
            ResponseData rd1 = node.sendRequest(token, resource, 
                    Constants.GET, params, null, null, null);
            //with trailing slash
            ResponseData rd2 = node.sendRequest(token, resource + "/", 
                    Constants.GET, params, null, null, null);
            String rd1response = IOUtils.toString(rd1.getContentStream());
            String rd2response = IOUtils.toString(rd2.getContentStream());
            assertEquals(rd1response.trim(), rd2response.trim());
        }
        catch(Exception e)
        {
            errorCollector.addError(new Throwable(
                    "Unexpected Exception in testTrailingSlashes: " + e.getMessage()));
        }
    }
  
	
	
	
}
