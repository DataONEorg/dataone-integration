package org.dataone.integration.it.testImplementations;

import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;

public class AuthTestImplementations extends ContextAwareAdapter {

    public AuthTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    public void testIsAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testIsAuthorized(nodeIterator.next(), version);
    }
    
    /**
     * A basic test Tests the dataONE service API isAuthorized() method, checking for Read 
     * permission on the first object returned from the Tier1 listObjects() method.  
     * Anything other than the boolean true is considered a test failure.
     */
    public void testIsAuthorized(Node node, String version) 
    {
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
            
        try {
            String objectIdentifier = "TierTesting:" + 
                    catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl()) +
                    ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier pid = catc.procurePublicReadableTestObject(callAdapter, D1TypeBuilder.buildIdentifier(objectIdentifier));

            boolean success = callAdapter.isAuthorized(null, pid, Permission.READ);
            checkTrue(callAdapter.getLatestRequestUrl(),"isAuthorized response should never be false. [Only true or exception].", success);
        } 
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
}
