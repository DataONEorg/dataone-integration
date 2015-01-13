package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNStorageTestDefinitions;
import org.dataone.integration.it.testImplementations.MNStorageTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNStorageV1IT extends ContextAwareTestCaseDataone
        implements MNStorageTestDefinitions {

    private MNStorageTestImplementations mnStorageTestImpl;
    
    @Before
    public void setup() {
        mnStorageTestImpl = new MNStorageTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of storage API methods";
    }

    @Override
    @Test
    public void testCreate() {
        mnStorageTestImpl.testCreate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testCreate_NoCert() {
        mnStorageTestImpl.testCreate_NoCert(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see MNStorageTestImplementations)")
    public void testCreateData_IdentifierEncoding() {
        mnStorageTestImpl.testCreateData_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdate() {
        mnStorageTestImpl.testUpdate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdate_badObsoletedByInfo() {
        mnStorageTestImpl.testUpdate_badObsoletedByInfo(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdate_badObsoletesInfo() {
        mnStorageTestImpl.testUpdate_badObsoletesInfo(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdate_NoCert() {
        mnStorageTestImpl.testUpdate_NoCert(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdate_NoRightsOnObsoleted() {
        mnStorageTestImpl.testUpdate_NoRightsOnObsoleted(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testArchive() {
        mnStorageTestImpl.testArchive(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testArchive_NotFound() {
        mnStorageTestImpl.testArchive_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testArchive_NoCert() {
        mnStorageTestImpl.testArchive_NoCert(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDelete_NoCert() {
        mnStorageTestImpl.testDelete_NoCert(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("No test exists for this yet.")
    public void testGenerateIdentifier() {
//        mnStorageTestImpl.testGenerateIdentifier(getMemberNodeIterator(), "v1");
    }
}
