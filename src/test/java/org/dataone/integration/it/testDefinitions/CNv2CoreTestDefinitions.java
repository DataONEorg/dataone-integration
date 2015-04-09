package org.dataone.integration.it.testDefinitions;

/**
 * Defines the test methods required to test v2 CNCore API functionality
 */
public interface CNv2CoreTestDefinitions {

    // TODO no tests yet implemented for the following methods

    public void testGetCapabilities();

    public void testUpdateSystemMetadata();
    
    public void testUpdateSystemMetadata_NotAuthorized();

    public void testUpdateSystemMetadata_InvalidSystemMetadata();
    
}
