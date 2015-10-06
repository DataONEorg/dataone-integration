package org.dataone.integration.it.testDefinitions;

/**
 * Defines the test methods required to test CNIdentity API functionality
 * across v1/v2.
 */
public interface CNIdentityTestDefinitions {

    public void testRegisterAccount_InvalidPerson();

    public void testRegisterAccount_ExistingPerson();
    
    public void testUpdateAccount_InvalidPerson();

    public void testVerifyAccount_NotAuthorized();
    
    public void testVerifyAccount_AlreadyVerified();
    
    public void testGetSubjectInfo();

    public void testGetSubjectInfo_UrlEncodingSpaces();

    public void testListSubjects();

    public void testListSubjects_Slicing();

    public void testMapIdentity();

    public void testRequestMapIdentity();

    public void testGetPendingMapIdentity();

    public void testConfirmMapIdentity();

    public void testDenyMapIdentity();

    public void testRemoveMapIdentity();

    public void testCreateGroup();

    public void testUpdateGroup();
}
