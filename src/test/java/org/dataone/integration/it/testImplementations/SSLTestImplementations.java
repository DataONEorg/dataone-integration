package org.dataone.integration.it.testImplementations;

import java.util.Iterator;
import java.util.List;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.util.Constants;

public class SSLTestImplementations extends ContextAwareAdapter {

    public SSLTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
        // TODO Auto-generated constructor stub
    }

    @WebTestName("connection layer - test with self-signed certificate")
    @WebTestDescription("this test uses a self-signed certificate to make a call to "
            + "getLogRecords and verifies that the number of results returned is 0")
    public void testConnectionLayer_SelfSignedCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testConnectionLayer_SelfSignedCert(nodeIterator.next(), version);
    }

    public void testConnectionLayer_SelfSignedCert(Node node, String version)
    {
        String currentUrl = node.getBaseURL();
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession("testPerson_SelfSigned"), node, version);

        try {
            Log logRecords = callAdapter.getLogRecords(null, null, null, null, null, 0, 0);
            List<LogEntry> logEntryList = logRecords.getLogEntryList();
            
            checkTrue(callAdapter.getLatestRequestUrl(), "Accessing log records with a self-signed certificate "
                    + "should return an empty log entry list", logEntryList.size() == 0);
        } catch (BaseException be) {
            handleFail(callAdapter.getLatestRequestUrl(),"a self-signed certificate should " +
            		"be able to make a getLogRecords call (and just return 0 results). Got: "
                    + be.getClass().getSimpleName() + ": " +
                    be.getDetail_code() + ":: " + be.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
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
