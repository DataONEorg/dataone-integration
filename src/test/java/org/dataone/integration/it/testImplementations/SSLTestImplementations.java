package org.dataone.integration.it.testImplementations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.util.D1Url;

public class SSLTestImplementations extends ContextAwareAdapter {

    public SSLTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
        // TODO Auto-generated constructor stub
    }


//    @WebTestName("Authentication - test for valid return")
//    @WebTestDescription("This test calls getLogRecords (as an examplar Tier1 method that implements Authentication) using " +
//    		"a self-signed client certificate.  ")

    @WebTestName("Authentication - test with self-signed certificate")
    @WebTestDescription("This is an imprecise test for the worst case of allowing a self-signed" +
    		"certificate to make a useful connection.  This test calls getLogRecords (as an exemplar " +
    		"Tier 1 method that implements Authentication) using a self-signed certificate and " +
    		"verifies that the number of results returned is 0, or throws a " +
    		"connection error.  (The SSL handshake by Java seems to self-downgrade to a 'public' connection, instead" +
    		"of not connecting. Other clients (i.e. curl) may fail to make a connection.")
    public void testConnectionLayer_SelfSignedCert_JavaSSL(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testConnectionLayer_SelfSignedCert_JavaSSL(nodeIterator.next(), version);
    }

    public void testConnectionLayer_SelfSignedCert_JavaSSL(Node node, String version)
    {
        String currentUrl = node.getBaseURL();
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession("urn_node_cnStageUNM1_SS1"), node, version);

        try {
            Log logRecords = callAdapter.getLogRecords(null, null, null, null, null, null, null);
            List<LogEntry> logEntryList = logRecords.getLogEntryList();
            System.out.println("total:" + logRecords.getTotal());
            
            checkTrue(callAdapter.getLatestRequestUrl(), "Accessing log records with a self-signed certificate "
                    + "should return either NotAuthorized or an empty log entry list. Got "
                    + logEntryList.size() + " log entries.", logEntryList.size() == 0);
            
        } catch (NotAuthorized na) {
            ; // is this an acceptable response?
//      } catch (ServiceFailure sf) {
// TODO: find out why we let self-signed certificates through at the authentication layer
//         (why do our servers trust self-signed certificates?)
//            ; 
        } catch (BaseException be) {
            handleFail(callAdapter.getLatestRequestUrl(),"a self-signed certificate against a method" +
            		"that implements Authorization should return either NotAuthorized or " +
            		"an empty log entry list. Got exception: "
                    + be.getClass().getSimpleName() + ": " +
                    be.getDetail_code() + ":: " + be.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "a self-signed certificate against a method" +
                    "that implements Authorization should return either NotAuthorized or " +
                    "an empty log entry list. Got exception: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("Authentication - test running curl with self-signed certificate")
    @WebTestDescription("This is a test that uses curl to test whether or not a self-signed" +
    		"certificate can fake a trusted connection. This test calls getLogRecords (as an exemplar " +
            "Tier 1 method that implements Authentication) using a self-signed certificate and " +
            "verifies that the number of results returned is 0, or throws a " +
            "connection error.")
    public void testConnectionLayer_SelfSignedCert_curl(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testConnectionLayer_SelfSignedCert_curl(nodeIterator.next(), version);
    }

    public void testConnectionLayer_SelfSignedCert_curl(Node node, String version)
    {
        D1Url url = new D1Url();
        url.setBaseUrl(node.getBaseURL());
        url.addNextPathElement(version);
        url.addNextPathElement("log");
//        url.addNonEmptyParamPair("start",0);
//        url.addNonEmptyParamPair("count",0);
        
        String testCertDirectory = (String) Settings.getConfiguration().getProperty("d1.test.cert.location");
        File cert = new File(testCertDirectory + "urn_node_cnStageUNM1_SS.crt");
        if (!cert.exists()) {
            handleFail("", "Cannot find the certificate required for the test: " + cert.getAbsolutePath());
        }
        ProcessBuilder pb = new ProcessBuilder(
                "curl",
                "-v",
                "-E",
                cert.getAbsolutePath(),
                url.getUrl());
        
        String responseString = null;
        try {
            pb.redirectErrorStream(true); // redirects stderr to stdout
            Process p = pb.start();
            InputStream is = p.getInputStream();  // contains stdout

            byte[] responseBytes = IOUtils.toByteArray(is);
            responseString = new String(responseBytes, "UTF-8");
        } catch (IOException e) {
            handleFail(url.getUrl(), "IOException making the test call or preparing the response.");
        }
        
        
        if (responseString.contains("http://ns.dataone.org/service/types")) {
            if (responseString.contains("<logEntry>")) {
                handleFail(url.getUrl(), "Should not get any logEntry elements under any circumstance!");
            } else {
                // flitered at the authorization phase.  maybe good, maybe bad
                handleFail(url.getUrl(), "Should not get a dataone Log response unless the authentication " +
                		"layer downgrades the connection to public.  The full response:\n" + responseString);
            }
        }
        else if (responseString.contains("alert unknown ca") &&
                responseString.contains("error")) {
            // connection is closed; this is good
        } else if (responseString.contains("Certificate type not set, assuming PKCS#12 format.")) {
            handleFail(url.getUrl(), "The test is  likely running on a Mac and not recognizing the " +
            		"certificate, so the test is inconclusive. The full response:\n" + responseString);
        } else {
            handleFail(url.getUrl(), "Don't know how we got here.  This is an unhandled response, and" +
            		"the test is inconclusive.  The full response:\n" + responseString);
        }
 
 
        

//            
//            Log logRecords = null;
//            List<LogEntry> logEntryList = logRecords.getLogEntryList();
//            System.out.println("total:" + logRecords.getTotal());
//            
//            checkTrue(callAdapter.getLatestRequestUrl(), "Accessing log records with a self-signed certificate "
//                    + "should return either NotAuthorized or an empty log entry list. Got "
//                    + logEntryList.size() + " log entries.", logEntryList.size() == 0);
//        } catch (NotAuthorized na) {
//            ; // is this an acceptable response?
////      } catch (ServiceFailure sf) {
//// TODO: find out why we let self-signed certificates through at the authentication layer
////         (why do our servers trust self-signed certificates?)
////            ; 
//        } catch (BaseException be) {
//            handleFail(callAdapter.getLatestRequestUrl(),"a self-signed certificate against a method" +
//                    "that implements Authorization should return either NotAuthorized or " +
//                    "an empty log entry list. Got exception: "
//                    + be.getClass().getSimpleName() + ": " +
//                    be.getDetail_code() + ":: " + be.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl, "a self-signed certificate against a method" +
//                    "that implements Authorization should return either NotAuthorized or " +
//                    "an empty log entry list. Got exception: " + e.getClass().getName() + ": " + e.getMessage());
//        }
    }
    
    
    @WebTestName("connection layer - test with expired certificate")
    @WebTestDescription("this test uses an expired certificate to make a call to "
            + "ping and verifies that it results in a ServiceFailure exception")
    public void testConnectionLayer_ExpiredCertificate(Iterator<Node> nodeIterator, String version) 
    {          
        while (nodeIterator.hasNext())
            testConnectionLayer_ExpiredCertificate(nodeIterator.next(), version);       
    }

    public void testConnectionLayer_ExpiredCertificate(Node node, String version) 
    {    
        String currentUrl = node.getBaseURL();
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession("testPerson_Expired"), node, version);

        try {
            callAdapter.ping();
        } catch (BaseException be) {
            if (!(be instanceof ServiceFailure)) {
            handleFail(callAdapter.getLatestRequestUrl(),"an Expired Certificate should throw a ServiceFailure. Got: "
                    + be.getClass().getSimpleName() + ": " +
                    be.getDetail_code() + ":: " + be.getDescription());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("connection layer - test DataONE CA trusted certificate")
    @WebTestDescription("this test uses a DataONE CA trusted certificate "
            + "to make a call to ping and verifies that it does not result in"
            + "any exception")
    public void testConnectionLayer_dataoneCAtrusted(Iterator<Node> nodeIterator, String version) {

        while (nodeIterator.hasNext())
            testConnectionLayer_dataoneCAtrusted(nodeIterator.next(), version);
    }

    
    public void testConnectionLayer_dataoneCAtrusted(Node node, String version) 
    {    
        String currentUrl = node.getBaseURL();
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession("testSubmitter"), node, version);

        try {
            callAdapter.ping();
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    
    
    }
}
