package org.dataone.integration.it.testDefinitions;

public interface MNUpdateSystemMetadataTestDefinitions {

    public void testUpdateSystemMetadata_NotFound();
    
    public void testUpdateSystemMetadata_NotAuthorized();

    public void testUpdateSystemMetadata_NotAuthorizedMN();
    
    public void testUpdateSystemMetadata_NoSerialVersion();

    public void testUpdateSystemMetadata_InvalidRequest_NoPid();
    
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch();
    
    public void testUpdateSystemMetadata_InvalidRequest_SysmetaUnmodified();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN();
    
    public void testUpdateSystemMetadata_InvalidRequest_NullOriginMN();
    
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId();
    
    public void testUpdateSystemMetadata_RightsHolder();
    
    public void testUpdateSystemMetadata_MutableRightsHolder();
    
    public void testUpdateSystemMetadata_MutableFormat();
    
    public void testUpdateSystemMetadata_MutableAccessPolicy();
    
    public void testUpdateSystemMetadata_MutableReplPolicy();

//    public void testUpdateSystemMetadata_MutableAuthMN();
//    
//    public void testUpdateSystemMetadata_RightsHolderNonAuthMN();
    
}
