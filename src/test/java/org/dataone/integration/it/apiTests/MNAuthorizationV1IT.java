package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNSystemMetadataChangedTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.dataone.integration.it.testImplementations.MNReplicationTestImplementations;
import org.dataone.integration.it.testImplementations.V1IsAuthorizedAuthorizationTestImpl;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNAuthentication functionality for v1 of the API
 */
public class MNAuthorizationV1IT extends V1IsAuthorizedAuthorizationTestImpl
        implements AuthTestDefinitions, MNSystemMetadataChangedTestDefinitions {

    private AuthTestImplementations authTestImpl;
    private MNReplicationTestImplementations mnReplicationTestImpl;

    /* configure the tests from the abstract class */
    @Override
    protected Iterator<Node> getNodeIterator()
    {
        return getMemberNodeIterator();
    }

    @Before
    public void setup() {
        setApiVersion("v1");
        authTestImpl = new AuthTestImplementations(this);
        /* systemMetadataChangedTest implementations are in MNReplicationTestImpls */
        mnReplicationTestImpl = new MNReplicationTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged() {
        mnReplicationTestImpl.testSystemMetadataChanged(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Skipped in original tier tests. Ignoring to match WebTester results for now.")
    public void testSystemMetadataChanged_EarlierDate() {
        mnReplicationTestImpl.testSystemMetadataChanged_EarlierDate(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSystemMetadataChanged_authenticatedITKuser() {
        mnReplicationTestImpl.testSystemMetadataChanged_authenticatedITKuser(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("Skipped in original tier tests. Ignoring to match WebTester results for now.")
    public void testSystemMetadataChanged_withCreate() {
        mnReplicationTestImpl.testSystemMetadataChanged_withCreate(getMemberNodeIterator(), "v1");
    }
}
