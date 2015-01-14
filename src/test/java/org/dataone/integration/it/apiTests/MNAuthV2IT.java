package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNAuthentication functionality for v2 of the API 
 */
public class MNAuthV2IT extends ContextAwareTestCaseDataone 
        implements AuthTestDefinitions {

    private AuthTestImplementations authTestImpl;

    @Before
    public void setup() {
        authTestImpl = new AuthTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getMemberNodeIterator(), "v1");
    }

}