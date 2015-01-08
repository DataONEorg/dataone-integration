package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;

public class MNReadV2IT extends ContextAwareTestCaseDataone implements ReadTestDefinitions, MNReadTestDefinitions{

    protected ReadTestImplementations readTestImpl;
    protected MNReadTestImplementations mnReadTestImpl;
    
    
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
    public void testGet() {
        readTestImpl.testGet(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testDescribe() {
        readTestImpl.testDescribe(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testListObjects() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testSystemMetadataChanged_EarlierDate() {
        mnReadTestImpl.testSystemMetadataChanged_EarlierDate(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testSystemMetadataChanged_authenticatedITKuser() {
        mnReadTestImpl.testSystemMetadataChanged_authenticatedITKuser(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testSystemMetadataChanged_withCreate() {
        mnReadTestImpl.testSystemMetadataChanged_withCreate(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testSynchronizationFailed_NoCert() {
        mnReadTestImpl.testSynchronizationFailed_NoCert(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetReplica_PublicObject() {
        mnReadTestImpl.testGetReplica_PublicObject(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetReplica_ValidCertificate_NotMN() {
        mnReadTestImpl.testGetReplica_ValidCertificate_NotMN(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetReplica_NoCertificate() {
        mnReadTestImpl.testGetReplica_NoCertificate(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetReplica_NotFound() {
        mnReadTestImpl.testGetReplica_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetReplica_IdentifierEncoding() {
        mnReadTestImpl.testGetReplica_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }
}
