package org.dataone.integration.it.testDefinitions;

public interface SynchronizeMetadataTestDefinition {

    public void testSynchronize_NotAuthorized();
    
    public void testSynchronize_NotAuthorized_MN();
    
    public void testSynchronize_AuthorizedMN();
    
    public void testSynchronize_NotFound();
}
