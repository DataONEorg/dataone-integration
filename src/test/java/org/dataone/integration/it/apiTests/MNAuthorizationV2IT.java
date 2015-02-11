package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.it.testDefinitions.AuthTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthTestImplementations;
import org.dataone.integration.it.testImplementations.V2IsAuthorizedAuthorizationTestImpl;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNAuthentication functionality for v2 of the API
 */
public class MNAuthorizationV2IT extends V2IsAuthorizedAuthorizationTestImpl
        implements AuthTestDefinitions {

    private AuthTestImplementations authTestImpl;

    @Override
    protected Iterator<Node> getNodeIterator()
    {
        return getMemberNodeIterator();
    }

    @Before
    public void setup() {
        authTestImpl = new AuthTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getMemberNodeIterator(), "v2");
    }
}