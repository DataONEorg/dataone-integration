package org.dataone.integration.it;

public interface MNReadTestDefinitions {

    // see MNodeTier1*.java classes
    
    public void testSystemMetadataChanged_EarlierDate();

    public void testSystemMetadataChanged_authenticatedITKuser();

    public void testSystemMetadataChanged_withCreate();

    public void testSynchronizationFailed_NoCert();

    public void testGetReplica_PublicObject();

    public void testGetReplica_ValidCertificate_NotMN();

    public void testGetReplica_NoCertificate();

    public void testGetReplica_NotFound();

    public void testGetReplica_IdentifierEncoding();
}
