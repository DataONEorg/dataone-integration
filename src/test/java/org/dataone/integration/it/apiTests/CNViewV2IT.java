package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.ViewTestDefinitions;
import org.dataone.integration.it.testImplementations.ViewTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class CNViewV2IT extends ContextAwareTestCaseDataone implements ViewTestDefinitions{

    @WebTestImplementation
    private ViewTestImplementations viewTestImpl;
    
    
    @Before
    public void setup() {
        viewTestImpl = new ViewTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of view API methods";
    }

    @Override
    @Test
    public void testView_NotAuthorized() {
        viewTestImpl.testView_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testView_InvalidTheme() {
        viewTestImpl.testView_InvalidTheme(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testView_NotFound() {
        viewTestImpl.testView_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListViews() {
        viewTestImpl.testListViews(getCoordinatingNodeIterator(), "v2");
    }
    
    
}
