package org.dataone.integration.it.testDefinitions;

public interface CNRegisterTestDefinitions {

    public void testRegister();

    public void testRegister_IdentifierNotUnique();

    public void testUpdateNodeCapabilities();

    public void testUpdateNodeCapabilities_NotFound();

    public void testUpdateNodeCapabilities_NotAuthorized();

    public void testUpdateNodeCapabilities_updatingOtherField();
    
}
