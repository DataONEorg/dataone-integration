package org.dataone.integration.it;


public interface MNReadTestDefinitions {

    public void testSystemMetadataChanged_EarlierDate();
    
    public void testSystemMetadataChanged_authenticatedITKuser();

    public void testSystemMetadataChanged_withCreate();
    
    // TODO double-check if listObject methods belong here...
    public void testListObjects();
    
    public void testListObjects_Slicing();

    public void testListObjects_FromDateTest();

    public void testListObjects_FormatIdFilteringTest();
    
    public void testListObjects_FormatIdFilteringTestFakeFormat();
}
