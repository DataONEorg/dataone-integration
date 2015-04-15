package org.dataone.integration.it.testDefinitions;


public interface UpdateSystemMetadataTestDefinitions {

    public void testUpdateSystemMetadata_NotFound();
    
    public void testUpdateSystemMetadata_NotAuthorized();

    public void testUpdateSystemMetadata_NotAuthorizedMN();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata();

    public void testUpdateSystemMetadata_InvalidRequestPidMismatch();
    
    public void testUpdateSystemMetadata_InvalidRequestSysmetaUnmodified();
    
    public void testUpdateSystemMetadata_RightsHolder();

    public void testUpdateSystemMetadata_CN();

}
