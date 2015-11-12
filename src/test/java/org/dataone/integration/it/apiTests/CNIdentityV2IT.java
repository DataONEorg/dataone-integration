package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNIdentityTestDefinitions;
import org.dataone.integration.it.testImplementations.CNIdentityTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNIdentity functionality for v2 of the API 
 */
public class CNIdentityV2IT extends ContextAwareTestCaseDataone implements
        CNIdentityTestDefinitions {

    @WebTestImplementation
    private CNIdentityTestImplementations identityTestImpl;

    
    @Before
    public void setup() {
        identityTestImpl = new CNIdentityTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of identity API methods";
    }

    @Override
    @Test
    public void testRegisterAccount_InvalidPerson() {
        identityTestImpl.testRegisterAccount_InvalidPerson(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegisterAccount_ExistingPerson() {
        identityTestImpl.testRegisterAccount_ExistingPerson(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateAccount_InvalidPerson() {
        identityTestImpl.testUpdateAccount_InvalidPerson(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testVerifyAccount_NotAuthorized() {
        identityTestImpl.testVerifyAccount_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testVerifyAccount_AlreadyVerified() {
        identityTestImpl.testVerifyAccount_AlreadyVerified(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSubjectInfo() {
        identityTestImpl.testGetSubjectInfo(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSubjectInfo_UrlEncodingSpaces() {
        identityTestImpl.testGetSubjectInfo_UrlEncodingSpaces(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListSubjects() {
        identityTestImpl.testListSubjects(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListSubjects_Slicing() {
        identityTestImpl.testListSubjects_Slicing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testMapIdentity() {
        identityTestImpl.testMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testRequestMapIdentity() {
        identityTestImpl.testRequestMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testGetPendingMapIdentity() {
        identityTestImpl.testGetPendingMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testConfirmMapIdentity() {
        identityTestImpl.testConfirmMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testDenyMapIdentity() {
        identityTestImpl.testDenyMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testRemoveMapIdentity() {
        identityTestImpl.testRemoveMapIdentity(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testCreateGroup() {
        identityTestImpl.testCreateGroup(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Original test code was disabled (commented out).")
    public void testUpdateGroup() {
        identityTestImpl.testUpdateGroup(getCoordinatingNodeIterator(), "v2");
    }

}