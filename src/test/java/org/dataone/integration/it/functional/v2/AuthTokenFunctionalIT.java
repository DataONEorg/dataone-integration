package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.AuthTokenTestImplementation;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class AuthTokenFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    private AuthTokenTestImplementation testImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs some basic authentication tests using tokens.";
    }

    @Before
    public void setup() {
        testImpl = new AuthTokenTestImplementation(this);
    }
    
    @Test
    public void testCnIsAuthorized() {
        testImpl.testCnIsAuthorized(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testMnIsAuthorized() {
        testImpl.testMnIsAuthorized(getMemberNodeIterator(), "v2");
    }
    
}
