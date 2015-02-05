package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.it.testDefinitions.AuthAPITestDefinitions;
import org.dataone.integration.it.testDefinitions.CNAuthTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthAPITestImplementations;
import org.dataone.integration.it.testImplementations.CNAuthTestImplementations;
import org.dataone.integration.it.testImplementations.V1IsAuthorizedAuthorizationTestImpl;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNAuthorization functionality for v1 of the API
 */
public class CNAuthorizationV1IT extends V1IsAuthorizedAuthorizationTestImpl
        implements AuthAPITestDefinitions, CNAuthTestDefinitions {

    private AuthAPITestImplementations authTestImpl;
    private CNAuthTestImplementations cnAuthTestImpl;

    @Override
    protected Iterator<Node> getNodeIterator()
    {
        return getCoordinatingNodeIterator();
    }

    @Before
    public void setup() {
        setApiVersion("v1");
        authTestImpl = new AuthAPITestImplementations(this);
        cnAuthTestImpl = new CNAuthTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSetRightsHolder() {
        cnAuthTestImpl.testSetRightsHolder(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSetAccessPolicy() {
        cnAuthTestImpl.testSetAccessPolicy(getCoordinatingNodeIterator(), "v1");
    }


}
