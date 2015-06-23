package org.dataone.integration.it.testDefinitions;

public interface MNPackageTestDefinitions {

    public void testGetPackage_NotAuthorized();
    
    public void testGetPackage_NotFound();
    
    public void testGetPackage_UnsupportedType();
    
    public void testGetPackage();
    
}
