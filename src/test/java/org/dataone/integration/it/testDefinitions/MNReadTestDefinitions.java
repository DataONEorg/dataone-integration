package org.dataone.integration.it.testDefinitions;

/**
 * Defines the test methods required to test base MNRead functionality
 * between v1 and v2. There is a difference between v1 and v2 APIs, and
 * that is the <code>testSystemMetadataChanted*</code> methods, which were moved
 * from MNAuthorization into MNRead from v1 to v2. 
 * Those are sitting in {@link MNv2ReadTestDefinitions}
 */
public interface MNReadTestDefinitions {

    // see MNodeTier1*.java classes

    public void testSynchronizationFailed_NoCert();

    public void testGetReplica_PublicObject();

    public void testGetReplica_ValidCertificate_NotMN();

    public void testGetReplica_NoCertificate();

    public void testGetReplica_NotFound();

    public void testGetReplica_IdentifierEncoding();
}
