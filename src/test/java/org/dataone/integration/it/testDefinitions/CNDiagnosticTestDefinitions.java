package org.dataone.integration.it.testDefinitions;

public interface CNDiagnosticTestDefinitions {

    // TODO check the returned SubjectInfo more carefully
    public void testEchoCredentials();
    
    public void testEchoSystemMetadata();
    
    public void testEchoSystemMetadata_NotAuthorized();

    public void testEchoSystemMetadata_InvalidSysMeta_NoPid();

    public void testEchoSystemMetadata_InvalidSysMeta_EmptyPid();
    
    public void testEchoSystemMetadata_InvalidSysMeta_BadPid();
    
    public void testEchoSystemMetadata_InvalidSysMeta_SerialVer();
    
    public void testEchoSystemMetadata_InvalidSysMeta_FormatId();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoSize();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoChecksum();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoSubmitter();
    
    public void testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder();
    
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder();
    
    public void testEchoSystemMetadata_InvalidSysMeta_AccessPolicy();
    
    public void testEchoSystemMetadata_InvalidSysMeta_ReplNum();
    
    public void testEchoSystemMetadata_InvalidSysMeta_ReplAllow();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoOriginMN();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoAuthMN();
    
    public void testEchoIndexedObject();
    
    public void testEchoIndexedObject_NotAuthorized();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoPid();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyPid();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_BadPid();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_SerialVer();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_FormatId();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSize();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoChecksum();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplNum();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplAllow();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN();
    
    public void testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN();
    
    public void testEchoIndexedObject_UnsupportedType();
    
    public void testEchoIndexedObject_UnsupportedMetadataType();
    
    // not very testable
    // public void testEchoIndexedObject_InusfficientResources();
}
