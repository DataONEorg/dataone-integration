package org.dataone.integration.it.functional;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.ViewFunctionalTestImplementations;
import org.junit.BeforeClass;
import org.junit.Test;

public class MNViewFunctionalIT extends ContextAwareTestCaseDataone {

    private ViewFunctionalTestImplementations viewImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs MN view API methods and checks results for correctness.";
    }

    @BeforeClass
    public void setup() {
        viewImpl = new ViewFunctionalTestImplementations(this);
    }
    
    @Test
    public void testView() {
        viewImpl.testView(getMemberNodeIterator(), "v2");
    }
}
