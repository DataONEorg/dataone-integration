package org.dataone.integration.it.testDefinitions;

/**
 * Defines the <code>systemMetadataChanged*</code> test methods 
 * (they're in the MNAuthorization API in v1 but in MNReplication in v2).
 */
public interface MNSystemMetadataChangedTestDefinitions {
    
    // see MNodeTier1*.java classes
    
    public void testSystemMetadataChanged();
    
    public void testSystemMetadataChanged_EarlierDate();

    public void testSystemMetadataChanged_authenticatedITKuser();

    public void testSystemMetadataChanged_withCreate();
    
}
