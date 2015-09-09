package org.dataone.integration.it.testDefinitions;

public interface MNv2SystemMetadataChangedTestDefinitions {
    
    public void testSystemMetadataChanged();
    
    public void testSystemMetadataChanged_EarlierDate();

    public void testSystemMetadataChanged_authenticatedITKuser();

    public void testSystemMetadataChanged_NotAuthPuplic();

    public void testSystemMetadataChanged_NotAuthRightsHolder();

    public void testSystemMetadataChanged_NotFoundAuthCN();
        
}
