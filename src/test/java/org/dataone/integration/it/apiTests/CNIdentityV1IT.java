package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNIdentityTestDefinitions;
import org.dataone.integration.it.testImplementations.CNIdentityTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class CNIdentityV1IT extends ContextAwareTestCaseDataone 
        implements CNIdentityTestDefinitions {

    private CNIdentityTestImplementations identityTestImpl;
    
    @Before
    public void setup() {
        identityTestImpl = new CNIdentityTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of identity API methods";
    }

    @Override
    @Test
    public void testRegisterAccount() {
        identityTestImpl.testRegisterAccount(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateAccount() {
        identityTestImpl.testUpdateAccount(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testVerifyAccount() {
        identityTestImpl.testVerifyAccount(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSubjectInfo() {
        identityTestImpl.testGetSubjectInfo(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSubjectInfo_UrlEncodingSpaces() {
        identityTestImpl.testGetSubjectInfo_UrlEncodingSpaces(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListSubjects() {
        identityTestImpl.testListSubjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListSubjects_Slicing() {
        identityTestImpl.testListSubjects_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testMapIdentity() {
        identityTestImpl.testMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testRequestMapIdentity() {
        identityTestImpl.testRequestMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetPendingMapIdentity() {
        identityTestImpl.testGetPendingMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testConfirmMapIdentity() {
        identityTestImpl.testConfirmMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDenyMapIdentity() {
        identityTestImpl.testDenyMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testRemoveMapIdentity() {
        identityTestImpl.testRemoveMapIdentity(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testCreateGroup() {
        identityTestImpl.testCreateGroup(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateGroup() {
        identityTestImpl.testUpdateGroup(getCoordinatingNodeIterator(), "v1");
    }
    
}
