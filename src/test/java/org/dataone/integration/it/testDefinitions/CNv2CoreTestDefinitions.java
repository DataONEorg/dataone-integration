package org.dataone.integration.it.testDefinitions;

/**
 * Defines the test methods required to test v2 CNCore API functionality
 */
public interface CNv2CoreTestDefinitions extends CNUpdateSystemMetadataTestDefinitions, SynchronizeMetadataTestDefinition {

    public void testGetCapabilities();
    
}
