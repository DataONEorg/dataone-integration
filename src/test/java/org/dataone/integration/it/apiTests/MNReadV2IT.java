package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReadSidTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNReadTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNv2SystemMetadataChangedTestDefinitions;
import org.dataone.integration.it.testDefinitions.ReadTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReadTestImplementations;
import org.dataone.integration.it.testImplementations.MNSystemMetadataChangedMethodTestImplementations;
import org.dataone.integration.it.testImplementations.ReadTestImplementations;
import org.dataone.integration.it.testImplementations.SidMNTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNRead functionality for v2 of the API 
 */
public class MNReadV2IT extends ContextAwareTestCaseDataone 
        implements ReadTestDefinitions, MNReadTestDefinitions, MNReadSidTestDefinitions, MNv2SystemMetadataChangedTestDefinitions {

    @WebTestImplementation
    private ReadTestImplementations readTestImpl;
    @WebTestImplementation
    private MNReadTestImplementations mnReadTestImpl;
    @WebTestImplementation
    private SidMNTestImplementations sidImpl;
    @WebTestImplementation
    private MNSystemMetadataChangedMethodTestImplementations mnSysmetaChangedImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of read API methods";
    }
    
    @Before 
    public void setup() {
        readTestImpl = new ReadTestImplementations(this);
        mnReadTestImpl = new MNReadTestImplementations(this);
        sidImpl = new SidMNTestImplementations();
        mnSysmetaChangedImpl = new MNSystemMetadataChangedMethodTestImplementations(this);
    }

    @After
    public void cleanUp() {
        sidImpl.cleanUp();
    }
    
    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }
    
    @Override
    @Test
    public void testGet() {
        readTestImpl.testGet(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe() {
        readTestImpl.testDescribe(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects() {
        readTestImpl.testListObjects(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSynchronizationFailed_NoCert() {
        mnReadTestImpl.testSynchronizationFailed_NoCert(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetReplica_PublicObject() {
        mnReadTestImpl.testGetReplica_PublicObject(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetReplica_ValidCertificate_NotMN() {
        mnReadTestImpl.testGetReplica_ValidCertificate_NotMN(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetReplica_NoCertificate() {
        mnReadTestImpl.testGetReplica_NoCertificate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetReplica_NotFound() {
        mnReadTestImpl.testGetReplica_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetReplica_IdentifierEncoding() {
        mnReadTestImpl.testGetReplica_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSidGet() {
        sidImpl.testGet();
    }

    @Override
    @Test
    public void testSidGetSystemMetadata() {
        sidImpl.testGetSystemMetadata();
    }

    @Override
    @Test
    public void testSidDescribe() {
        sidImpl.testDescribe();
    }

    @Override
    @Test
    public void testSidListObjects() {
        sidImpl.testListObjects();
    }
    
    @Override
    @Test
    public void testSystemMetadataChanged() {
        mnSysmetaChangedImpl.testSystemMetadataChanged(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testSystemMetadataChanged_EarlierDate() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_EarlierDate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_authenticatedITKuser() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_authenticatedITKuser(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_NotAuthPuplic() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_NotAuthPuplic(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testSystemMetadataChanged_NotAuthRightsHolder() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_NotAuthRightsHolder(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testSystemMetadataChanged_NotFoundAuthCN() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_InvalidPid(getMemberNodeIterator(), "v2");
    }
    
}
