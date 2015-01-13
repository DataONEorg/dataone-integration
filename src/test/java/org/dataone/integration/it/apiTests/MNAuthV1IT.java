package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNv1AuthAPITestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.dataone.integration.it.testImplementations.MNReadTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class MNAuthV1IT extends ContextAwareTestCaseDataone 
        implements AuthTestDefinitions, MNv1AuthAPITestDefinitions{

    private AuthTestImplementations authTestImpl;
    private MNReadTestImplementations mnReadTestImpl;
    
    @Before
    public void setup() {
        authTestImpl = new AuthTestImplementations(this);
        mnReadTestImpl = new MNReadTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getMemberNodeIterator(), "v1");
    }
    
    @Override
    @Test
    public void testSystemMetadataChanged_EarlierDate() {
        mnReadTestImpl.testSystemMetadataChanged_EarlierDate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_authenticatedITKuser() {
        mnReadTestImpl.testSystemMetadataChanged_authenticatedITKuser(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_withCreate() {
        mnReadTestImpl.testSystemMetadataChanged_withCreate(getMemberNodeIterator(), "v1");
    }
    
}
