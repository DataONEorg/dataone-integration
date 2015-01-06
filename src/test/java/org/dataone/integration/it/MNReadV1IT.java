package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;

public class MNReadV1IT extends ContextAwareTestCaseDataone implements ReadTestDefinitions, MNReadTestDefinitions{

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of read API methods";
    }
    
    protected ReadTestImplementations readTestImpl;
    
    @Before 
    public void setup() {
        this.readTestImpl = new ReadTestImplementations(this);
        // TODO an MN Read implementation
    }

    @Override
    public void testGet() {
        readTestImpl.testGet(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testDescribe() {
        readTestImpl.testDescribe(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testListObjects() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testSystemMetadataChanged_EarlierDate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testSystemMetadataChanged_authenticatedITKuser() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testSystemMetadataChanged_withCreate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testSynchronizationFailed_NoCert() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetReplica_PublicObject() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetReplica_ValidCertificate_NotMN() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetReplica_NoCertificate() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetReplica_NotFound() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetReplica_IdentifierEncoding() {
        // TODO Auto-generated method stub
        
    }

}
