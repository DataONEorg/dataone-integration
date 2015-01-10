package org.dataone.integration.it.testDefinitions;

/**
 * Defines the v2 additions to the MNRead functionality.
 * This consists of <code>testSystemMetadataChanged*</code> methods 
 * (which used to be in MNAuthorization in v1).
 * Base MNRead functionality is defined by {@link MNReadTestDefinitions}.
 */
public interface MNv2ReadTestDefinitions {
    
    // see MNodeTier1*.java classes
    
    public void testSystemMetadataChanged_EarlierDate();

    public void testSystemMetadataChanged_authenticatedITKuser();

    public void testSystemMetadataChanged_withCreate();
    
}
