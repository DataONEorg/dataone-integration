package org.dataone.integration.it.testDefinitions;

/**
 * Defines the CNReplication test methods common to v1 and v2.
 */
public interface CNReplicationTestDefinitions {

    public void testIsNodeAuthorized_InvalidToken();

    public void testIsNodeAuthorized_NotAuthorized();

    public void testIsNodeAuthorized_InvalidRequest();

    public void testIsNodeAuthorized_NotFound();

    public void testUpdateReplicationMetadata();

    public void testUpdateReplicationMetadata_NotAuthorized();

    public void testUpdateReplicationMetadata_NotFound();

    public void testUpdateReplicationMetadata_InvalidRequest();

    public void testUpdateReplicationMetadata_VersionMismatch();

    public void testSetReplicationStatus_NotAuthorized();

    public void testSetReplicationStatus_InvalidRequest();

    public void testSetReplicationStatus_NotFound();

    public void testSetReplicationPolicy();

    public void testSetReplicationPolicy_NotAuthorized();

    public void testSetReplicationPolicy_NotFound();

    public void testSetReplicationPolicy_VersionMismatch();

    public void testSetReplicationPolicy_InvalidRequest();

    // TODO no test exists for this method:
    public void testDeleteReplicationMetadata();

}
