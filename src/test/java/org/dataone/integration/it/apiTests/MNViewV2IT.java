package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.ViewTestDefinitions;
import org.dataone.integration.it.testImplementations.ViewTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Test;

public class MNViewV2IT extends ContextAwareTestCaseDataone implements ViewTestDefinitions{

    @WebTestImplementation
    private ViewTestImplementations viewTestImpl;
    
    
    @Before
    public void setup() {
        viewTestImpl = new ViewTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of view API methods";
    }

    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }
    
    @Override
    @Test
    public void testView_NotAuthorized() {
        viewTestImpl.testView_NotAuthorized(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testView_InvalidTheme() {
        viewTestImpl.testView_InvalidTheme(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testView_NotFound() {
        viewTestImpl.testView_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListViews() {
        viewTestImpl.testListViews(getMemberNodeIterator(), "v2");
    }
    
    
}
