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

    public void testListObjects();

    public void testListObjects_Slicing();

    public void testListObjects_FromDateTest();

    public void testListObjects_FormatIdFilteringTest();

    public void testListObjects_FormatIdFilteringTestFakeFormat();

}
