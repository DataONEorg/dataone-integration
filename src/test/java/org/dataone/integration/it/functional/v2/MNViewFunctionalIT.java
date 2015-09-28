package org.dataone.integration.it.functional.v2;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.ViewFunctionalTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Test;

public class MNViewFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    private ViewFunctionalTestImplementations viewImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs MN view API methods and checks results for correctness.";
    }

    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }
    
    @Before
    public void setup() {
        viewImpl = new ViewFunctionalTestImplementations(this, getCoordinatingNodeIterator());
    }
    
    @Test
    public void testView_Scimeta() {
        viewImpl.testView_Scimeta(getMemberNodeIterator(), "v2");
    }
    
    @Test
    public void testView_ResMap() {
        viewImpl.testView_ResMap(getMemberNodeIterator(), "v2");
    }
    
    @Test
    public void testListViews() {
        viewImpl.testListViews(getMemberNodeIterator(), "v2");
    }
    
    @Test
    public void testListViewsExist() {
        viewImpl.testListViewsExist(getMemberNodeIterator(), "v2");
    }
}
