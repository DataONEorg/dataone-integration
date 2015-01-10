package org.dataone.integration.it.testDefinitions;

/**
 * Defines the test methods required to test base MNStorage functionality
 * between v1 and v2. See {@link MNv2StorageTestDefinitions} for v2 additions.
 */
public interface MNStorageTestDefinitions {

    public void testCreate();

    public void testCreate_NoCert();

    public void testCreateData_IdentifierEncoding();

    public void testUpdate();

    public void testUpdate_badObsoletedByInfo();

    public void testUpdate_badObsoletesInfo();

    public void testUpdate_NoCert();

    public void testUpdate_NoRightsOnObsoleted();

    public void testArchive();

    public void testArchive_NotFound();

    public void testArchive_NoCert();

    public void testDelete_NoCert();

    // no tests:
    public void testGenerateIdentifier();
}
