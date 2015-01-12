package org.dataone.integration.it.testDefinitions;

import org.junit.Test;

/**
 * Test definitions for generalized DataONE authorization implementations
 * that provides thorough access-rule tests for any "read" method that authorizes
 * requests.
 * 
 * @author rnahf
 *
 */
public interface GeneralAuthorizationTestDefinitions {

    
    public void testIsAuthorized_NullPolicy_testPerson_is_RightsHolder();
    
    public void testIsAuthorized_NullPolicy_testGroup_is_RightsHolder();
    
    public void testIsAuthorized_AccessPolicy_is_Public_can_Read();
    
    public void testIsAuthorized_AccessPolicy_is_AuthenticatedUser_can_Read();
    
    public void testIsAuthorized_AccessPolicy_is_VerifiedUser_can_Read();
    
    public void testIsAuthorized_AccessPolicy_is_testPerson_can_Read();
    
    public void testIsAuthorized_AccessPolicy_is_testPerson_can_Write();
    
    public void testIsAuthorized_AccessPolicy_is_testPerson_can_ChangePerm();
    
    public void testIsAuthorized_AccessPolicy_is_testGroup_can_Read();
    
    public void testIsAuthorized_AccessPolicy_is_testGroup_can_Write();
    
    public void testIsAuthorized_AccessPolicy_is_testGroup_can_ChangePerm();
    
    public void testIsAuthorized_AccessPolicy_is_legacyAccount_can_Write();
    
    public void testIsAuthorized_ComplicatedAccessPolicy();
    
}
