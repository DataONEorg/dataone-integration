package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReadTestDefinitions;
import org.dataone.integration.it.testDefinitions.ReadTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReadTestImplementations;
import org.dataone.integration.it.testImplementations.ReadTestImplementations;
import org.dataone.integration.webTest.WebTestDescription;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNRead functionality for v1 of the API
 */
@WebTestDescription("Tests for the MNRead API that use ListObjects to locate a public object "
        + "that can be used to exercise all of the read (HTTP.GET) methods. It will create"
        + "test objects on Tier 3 nodes, but only if there is no public content already in the system.")
public class MNReadV1IT extends ContextAwareTestCaseDataone
        implements ReadTestDefinitions, MNReadTestDefinitions {

    private ReadTestImplementations readTestImpl;
    private MNReadTestImplementations mnReadTestImpl;


    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of read API methods";
    }

    @Before
    public void setup() {
        readTestImpl = new ReadTestImplementations(this);
        mnReadTestImpl = new MNReadTestImplementations(this);
    }

    @Override
    @Test
    public void testGet() {
        readTestImpl.testGet(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe() {
        readTestImpl.testDescribe(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects() {
        readTestImpl.testListObjects(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSynchronizationFailed_NoCert() {
        mnReadTestImpl.testSynchronizationFailed_NoCert(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_PublicObject() {
        mnReadTestImpl.testGetReplica_PublicObject(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Skipped in original tier tests. Ignoring to match WebTester results for now.")
    public void testGetReplica_ValidCertificate_NotMN() {
        mnReadTestImpl.testGetReplica_ValidCertificate_NotMN(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_NoCertificate() {
        mnReadTestImpl.testGetReplica_NoCertificate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_NotFound() {
        mnReadTestImpl.testGetReplica_NotFound(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Skipped in original tier tests. Ignoring to match WebTester results for now.")
    public void testGetReplica_IdentifierEncoding() {
        mnReadTestImpl.testGetReplica_IdentifierEncoding(getMemberNodeIterator(), "v1");
    }
}
