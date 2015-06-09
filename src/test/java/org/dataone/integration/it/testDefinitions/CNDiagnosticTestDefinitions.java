package org.dataone.integration.it.testDefinitions;

public interface CNDiagnosticTestDefinitions {

    // TODO check the returned SubjectInfo more carefully
    public void testEchoCredentials();
    
    public void testEchoSystemMetadata();
    
    public void testEchoSystemMetadata_NotAuthorized();
    
    public void testEchoSystemMetadata_InvalidSysMeta_SerialVer();
    
    public void testEchoSystemMetadata_InvalidSysMeta_FormatId();
    
    public void testEchoSystemMetadata_InvalidSysMeta_NoPid();
    
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyPid();
    
    public void testEchoSystemMetadata_InvalidSysMeta_BadPid();
    
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
    
    
    
    
    
    
    
    // TODO TODO check the returned SubjectInfo more carefully
    // testEchoCredentials()
    
    // TODO send request with some incorrect params
    // so.... don't go through libclient callAdapters / MultipartCNode ???
    public void testEchoSystemMetadata_InvalidRequest();
    
    public void testEchoIndexedObject();
    
    public void testEchoIndexedObject_NotAuthorized();
    
    // TODO send request with some incorrect params
    // so.... don't go through libclient callAdapters / MultipartCNode ???
    public void testEchoIndexedObject_InvalidRequest();
    
    // TODO this can be expanded to include a ton of scenarios
    // in which sysmata is invalid...
    public void testEchoIndexedObject_InvalidSystemMetadata();
    
    //- Indicate if the formatId is one registered in the environment.
    //- UnsupportedType seems to be equivalent to "Unsupported formatId"
    
    // Need to create a test object of unsupported type to test this...
    public void testEchoIndexedObject_UnsupportedType();
    
    // Need to create a test scimeta of unsupported type to test this...
    public void testEchoIndexedObject_UnsupportedMetadataType();
    
}
