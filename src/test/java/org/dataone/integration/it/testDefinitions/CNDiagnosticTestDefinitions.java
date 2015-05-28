package org.dataone.integration.it.testDefinitions;

public interface CNDiagnosticTestDefinitions {

    public void testEchoCredentials();
    
    public void testEchoSystemMetadata();
    
    public void testEchoSystemMetadata_NotAuthorized();
    
    public void testEchoSystemMetadata_InvalidSysMeta();
    
    // ID in the sysmeta?
    // we're just echoing the given sysmeta, right?
    // the sysmeta doesn't need to already exist? or does it?
    public void testEchoSystemMetadata_IdentifierNotUnique();

    // ? what causes this ? 
    // no doc
    public void testEchoSystemMetadata_InvalidRequest();
    
    public void testEchoIndexedObject();
    
    public void testEchoIndexedObject_NotAuthorized();
    
    // ? what causes this ?
    // "The structure of the request is invalid"
    // sounds like it should be a ServiceFailure wrapped around a tomcat error...
    public void testEchoIndexedObject_InvalidRequest();
    
    // this can be expanded to include a ton of scenarios
    // in which sysmata is invalid...
    public void testEchoIndexedObject_InvalidSystemMetadata();
    
    // Need to create a test object of unsupported type to test this...
    public void testEchoIndexedObject_UnsupportedType();
    
    // Need to create a test scimeta of unsupported type to test this...
    public void testEchoIndexedObject_UnsupportedMetadataType();
    
    // is this testable?
    // do we explicitly check the object size to return this error?
    // probably not - that could kill the heap itself
    // unless we just use the size from sysmeta???
    // in which case... may be fake-able (probably/hopefully not verified) <<<<<<<<<<<<<< TODO <<<
    public void testEchoIndexedObject_InusfficientResources();
    
}
