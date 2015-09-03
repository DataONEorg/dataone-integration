package org.dataone.integration.it.testDefinitions;

public interface CNUpdateSystemMetadataTestDefinitions {

    public void testUpdateSystemMetadata_NotFound();
    
    public void testUpdateSystemMetadata_NotAuthorized();

    public void testUpdateSystemMetadata_NotAuthorizedMN();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion();

    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoPid();
    
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId();
    
    public void testUpdateSystemMetadata_NotAuthorized_RightsHolder();

    public void testUpdateSystemMetadata_CN();

}
