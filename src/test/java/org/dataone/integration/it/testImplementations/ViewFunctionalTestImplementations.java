package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.OptionList;

public class ViewFunctionalTestImplementations extends ContextAwareAdapter {

    public ViewFunctionalTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    public void testView(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testView(nodeIterator.next(), version);        
    }
    
    private void testView(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testView_" + ExampleUtilities.generateIdentifier());
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = ContextAwareTestCaseDataone.getSubject("testRightsHolder");
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        try {
            pid = catc.procureTestObject(callAdapter, accessRule, pid);
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to create a test object for testView functional test: " 
                    + e.getCause().getMessage());
        }
        
        InputStream is = null;
        try {
            is = callAdapter.view(null, "default", pid);
        
            // TODO check the returned stream
            // for the correct value
            
            // ... what is that?
            // ... what is "default" supposed to return?
            
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testView functional test: " 
                    + e.getCause().getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void testListViews(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListViews(nodeIterator.next(), version);   
    }
    
    private void testListViews(Node node, String version) {
        
        CommonCallAdapter callAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, "v2");
        
        try {
            OptionList views = callAdapter.listViews(null);
        
            boolean foundDefault = false;
            for (String viewType : views.getOptionList())
                if (viewType.equals("default")) {
                    foundDefault = true;
                    break;
                }
            
            assertTrue("testListViews() should return at least a \"default\" view option type", 
                    foundDefault);
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(callAdapter.getNodeBaseServiceUrl(), 
                    "Unable to run testListViews functional test: " 
                    + e.getMessage() + ", "
                    + e.getCause() == null ? "" : e.getCause().getMessage());
        }
    }
}
