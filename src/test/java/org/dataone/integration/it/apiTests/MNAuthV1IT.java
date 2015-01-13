package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNSystemMetadataChangedTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.dataone.integration.it.testImplementations.MNReadTestImplementations;
import org.dataone.integration.it.testImplementations.MNReplicationTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class MNAuthV1IT extends ContextAwareTestCaseDataone 
        implements AuthTestDefinitions, MNSystemMetadataChangedTestDefinitions {

    private AuthTestImplementations authTestImpl;
    private MNReplicationTestImplementations mnReplicationTestImpl;
    
    @Before
    public void setup() {
        authTestImpl = new AuthTestImplementations(this);
        mnReplicationTestImpl = new MNReplicationTestImplementations(this);
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
        mnReplicationTestImpl.testSystemMetadataChanged_EarlierDate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_authenticatedITKuser() {
        mnReplicationTestImpl.testSystemMetadataChanged_authenticatedITKuser(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_withCreate() {
        mnReplicationTestImpl.testSystemMetadataChanged_withCreate(getMemberNodeIterator(), "v1");
    }
    
}
