package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReadTestDefinitions;
import org.dataone.integration.it.testDefinitions.ReadTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReadTestImplementations;
import org.dataone.integration.it.testImplementations.ReadTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class MNReadV1IT extends ContextAwareTestCaseDataone 
        implements ReadTestDefinitions, MNReadTestDefinitions{

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
        readTestImpl.testGet(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe() {
        readTestImpl.testDescribe(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSynchronizationFailed_NoCert() {
        mnReadTestImpl.testSynchronizationFailed_NoCert(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_PublicObject() {
        mnReadTestImpl.testGetReplica_PublicObject(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_ValidCertificate_NotMN() {
        mnReadTestImpl.testGetReplica_ValidCertificate_NotMN(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_NoCertificate() {
        mnReadTestImpl.testGetReplica_NoCertificate(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_NotFound() {
        mnReadTestImpl.testGetReplica_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetReplica_IdentifierEncoding() {
        mnReadTestImpl.testGetReplica_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }
}
