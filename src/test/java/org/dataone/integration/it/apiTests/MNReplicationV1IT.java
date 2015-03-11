package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReplicationTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReplicationTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNReplication functionality for v1 of the API 
 */
public class MNReplicationV1IT extends ContextAwareTestCaseDataone 
    implements MNReplicationTestDefinitions {

    @WebTestImplementation
    private MNReplicationTestImplementations mnReplicationTestImpl;
    
    @Before
    public void setup() {
        mnReplicationTestImpl = new MNReplicationTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of replication API methods";
    }

    @Override
    @Test
    @Ignore("Test implementation exists but claims to not be implemented (see MNReplicationTestImplementations)")
    public void testReplicate() {
        mnReplicationTestImpl.testReplicate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testReplicate_NoCertificate() {
        mnReplicationTestImpl.testReplicate_NoCertificate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testReplicate_ValidCertificate_NotCN() {
        mnReplicationTestImpl.testReplicate_ValidCertificate_NotCN(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test implementation exists but claims to not be implemented (see MNReplicationTestImplementations)")
    public void testReplicate_FaultyNodeReference() {
        mnReplicationTestImpl.testReplicate_FaultyNodeReference(getMemberNodeIterator(), "v1");
    }
}