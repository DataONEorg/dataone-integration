package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNReplicationTestDefinitions;
import org.dataone.integration.it.testImplementations.CNReplicationTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNReplication functionality for v1 of the API 
 */
public class CNReplicationV1IT extends ContextAwareTestCaseDataone implements
        CNReplicationTestDefinitions {

    @WebTestImplementation
    private CNReplicationTestImplementations replicationTestImpl;

    
    @Before
    public void setup() {
        replicationTestImpl = new CNReplicationTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of replication API methods.";
  }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testIsNodeAuthorized_InvalidToken() {
        replicationTestImpl.testIsNodeAuthorized_InvalidToken(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testIsNodeAuthorized_NotAuthorized() {
        replicationTestImpl.testIsNodeAuthorized_NotAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testIsNodeAuthorized_InvalidRequest() {
        replicationTestImpl.testIsNodeAuthorized_InvalidRequest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testIsNodeAuthorized_NotFound() {
        replicationTestImpl.testIsNodeAuthorized_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testUpdateReplicationMetadata() {
        replicationTestImpl.testUpdateReplicationMetadata(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testUpdateReplicationMetadata_NotAuthorized() {
        replicationTestImpl.testUpdateReplicationMetadata_NotAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testUpdateReplicationMetadata_NotFound() {
        replicationTestImpl.testUpdateReplicationMetadata_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testUpdateReplicationMetadata_InvalidRequest() {
        replicationTestImpl.testUpdateReplicationMetadata_InvalidRequest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testUpdateReplicationMetadata_VersionMismatch() {
        replicationTestImpl.testUpdateReplicationMetadata_VersionMismatch(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationStatus_NotAuthorized() {
        replicationTestImpl.testSetReplicationStatus_NotAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationStatus_InvalidRequest() {
        replicationTestImpl.testSetReplicationStatus_InvalidRequest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationStatus_NotFound() {
        replicationTestImpl.testSetReplicationStatus_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationPolicy() {
        replicationTestImpl.testSetReplicationPolicy(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationPolicy_NotAuthorized() {
        replicationTestImpl.testSetReplicationPolicy_NotAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationPolicy_NotFound() {
        replicationTestImpl.testSetReplicationPolicy_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationPolicy_VersionMismatch() {
        replicationTestImpl.testSetReplicationPolicy_VersionMismatch(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Test exists but claims to not be implemented (see CNReplicationTestImplementation)")
    public void testSetReplicationPolicy_InvalidRequest() {
        replicationTestImpl.testSetReplicationPolicy_InvalidRequest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("No test exists for this yet")
    public void testDeleteReplicationMetadata() {
//        replicationTestImpl.testDeleteReplicationMetadata(getCoordinatingNodeIterator(), "v1");
    }
}