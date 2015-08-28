package org.dataone.integration.it.testDefinitions;

public interface CNUpdateSystemMetadataTestDefinitions {

    public void testUpdateSystemMetadata_NotFound();
    
    public void testUpdateSystemMetadata_NotAuthorized();

    public void testUpdateSystemMetadata_NotAuthorizedMN();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion();

    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoPid();
    
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedIdentifier();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedSize();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedChecksum();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedSubmitter();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedDateUploaded();
    
    public void testUpdateSystemMetadata_InvalidSystemMetadata_ModifiedOriginMN();
    
    public void testUpdateSystemMetadata_InvalidSysmeta_ModifiedSeriesId();
    
    public void testUpdateSystemMetadata_NotAuthorized_RightsHolder();

    public void testUpdateSystemMetadata_CN();

}
