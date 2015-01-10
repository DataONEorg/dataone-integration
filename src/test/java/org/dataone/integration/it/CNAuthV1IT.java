package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNAuthTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.dataone.integration.it.testImplementations.CNAuthTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class CNAuthV1IT extends ContextAwareTestCaseDataone 
        implements AuthTestDefinitions, CNAuthTestDefinitions {
    
    private AuthTestImplementations authTestImpl;
    private CNAuthTestImplementations cnAuthTestImpl;
    
    @Before
    public void setup() {
        authTestImpl = new AuthTestImplementations(this);
        cnAuthTestImpl = new CNAuthTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of authentication API methods";
    }
    
    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getCoordinatingNodeIterator(), "v1");
    }
    
    @Override
    @Test
    public void testSetRightsHolder() {
        cnAuthTestImpl.testSetRightsHolder(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSetAccessPolicy() {
        cnAuthTestImpl.testSetAccessPolicy(getCoordinatingNodeIterator(), "v1");
    }
}
