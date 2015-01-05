package org.dataone.integration.it;


public interface ReadTestDefinitions {

    public void testGet();

    public void testGet_NotFound();

    public void testGet_IdentifierEncoding();

    public void testGetSystemMetadata();

    public void testGetSystemMetadata_NotFound();

    public void testGetSystemMetadata_IdentifierEncoding();

    public void testDescribe();

    public void testDescribe_NotFound();

    public void testDescribe_IdentifierEncoding();

    public void testGetChecksum();

    public void testGetChecksum_NotFound();

    public void testGetChecksum_IdentifierEncoding();
    
}
