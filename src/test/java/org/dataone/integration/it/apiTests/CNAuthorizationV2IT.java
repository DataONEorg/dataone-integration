package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.AuthAPITestDefinitions;
import org.dataone.integration.it.testDefinitions.CNAuthTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthAPITestImplementations;
import org.dataone.integration.it.testImplementations.CNAuthTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNAuthorization functionality for v2 of the API 
 */
public class CNAuthorizationV2IT extends ContextAwareTestCaseDataone 
        implements AuthAPITestDefinitions, CNAuthTestDefinitions {
    
    private AuthAPITestImplementations authTestImpl;
    private CNAuthTestImplementations cnAuthTestImpl;
    
    @Before
    public void setup() {
        authTestImpl = new AuthAPITestImplementations(this);
        cnAuthTestImpl = new CNAuthTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of authentication API methods";
    }
    
    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testSetRightsHolder() {
        cnAuthTestImpl.testSetRightsHolder(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSetAccessPolicy() {
        cnAuthTestImpl.testSetAccessPolicy(getCoordinatingNodeIterator(), "v2");
    }
}
