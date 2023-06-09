package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNReplicationTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNSystemMetadataChangedTestDefinitions;
import org.dataone.integration.it.testImplementations.MNReplicationTestImplementations;
import org.dataone.integration.it.testImplementations.MNSystemMetadataChangedMethodTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNReplication functionality for v2 of the API 
 */
public class MNReplicationV2IT extends ContextAwareTestCaseDataone 
        implements MNReplicationTestDefinitions {

    @WebTestImplementation
    private MNReplicationTestImplementations mnReplicationTestImpl;
    
    @Before
    public void setup() {
        mnReplicationTestImpl = new MNReplicationTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of replication API methods";
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
    @Ignore("Original test supposedly not implemented (see MNReplicationTestImplementations)")
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
    @Ignore("Original test supposedly not implemented (see MNReplicationTestImplementations)")
    public void testReplicate_FaultyNodeReference() {
        mnReplicationTestImpl.testReplicate_FaultyNodeReference(getMemberNodeIterator(), "v2");
    }

}