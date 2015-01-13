package org.dataone.integration.it.testDefinitions;

/**
 * Defines the MNReplication test methods common to v1 and v2.
 */
public interface MNReplicationTestDefinitions {

    public void testReplicate();

    public void testReplicate_NoCertificate();

    public void testReplicate_ValidCertificate_NotCN();

    public void testReplicate_FaultyNodeReference();

}
