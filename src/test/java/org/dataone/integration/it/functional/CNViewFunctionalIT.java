package org.dataone.integration.it.functional;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.ViewFunctionalTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CNViewFunctionalIT extends ContextAwareTestCaseDataone {

    private ViewFunctionalTestImplementations viewImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs CN view API methods and checks results for correctness.";
    }

    @Before
    public void setup() {
        viewImpl = new ViewFunctionalTestImplementations(this);
    }
    
    @Test
    public void testView_Scimeta() {
        viewImpl.testView_Scimeta(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testView_ResMap() {
        viewImpl.testView_ResMap(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testView_Data() {
        viewImpl.testView_Data(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testListViews() {
        viewImpl.testListViews(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testListViewsExist() {
        viewImpl.testListViewsExist(getCoordinatingNodeIterator(), "v2");
    }
    
}
