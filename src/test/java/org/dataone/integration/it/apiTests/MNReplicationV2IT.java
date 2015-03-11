package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReplicationTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNSystemMetadataChangedTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReplicationTestImplementations;
import org.dataone.integration.it.testImplementations.MNSystemMetadataChangedMethodTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNReplication functionality for v2 of the API 
 */
public class MNReplicationV2IT extends ContextAwareTestCaseDataone 
        implements MNReplicationTestDefinitions, MNSystemMetadataChangedTestDefinitions {

    @WebTestImplementation
    private MNReplicationTestImplementations mnReplicationTestImpl;
    @WebTestImplementation
    private MNSystemMetadataChangedMethodTestImplementations mnSysmetaChangedImpl;
    
    @Before
    public void setup() {
        mnReplicationTestImpl = new MNReplicationTestImplementations(this);
        mnSysmetaChangedImpl = new MNSystemMetadataChangedMethodTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of replication API methods";
    }

    @Override
    @Test
    @Ignore("Test implementation exists but claims to not be implemented (see MNReplicationTestImplementations)")
    public void testReplicate() {
        mnReplicationTestImpl.testReplicate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testReplicate_NoCertificate() {
        mnReplicationTestImpl.testReplicate_NoCertificate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testReplicate_ValidCertificate_NotCN() {
        mnReplicationTestImpl.testReplicate_ValidCertificate_NotCN(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Test implementation exists but claims to not be implemented (see MNReplicationTestImplementations)")
    public void testReplicate_FaultyNodeReference() {
        mnReplicationTestImpl.testReplicate_FaultyNodeReference(getMemberNodeIterator(), "v2");
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
    public void testSystemMetadataChanged_withCreate() {
        mnSysmetaChangedImpl.testSystemMetadataChanged_withCreate(getMemberNodeIterator(), "v2");
    }
}