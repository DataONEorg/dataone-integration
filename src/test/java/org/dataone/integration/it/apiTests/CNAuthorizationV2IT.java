package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.it.testDefinitions.AuthAPITestDefinitions;
import org.dataone.integration.it.testDefinitions.CNAuthTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNAuthorizationSidTestDefinitions;
import org.dataone.integration.it.testImplementations.AuthAPITestImplementations;
import org.dataone.integration.it.testImplementations.CNAuthTestImplementations;
import org.dataone.integration.it.testImplementations.SidCNTestImplementations;
import org.dataone.integration.it.testImplementations.V2IsAuthorizedAuthorizationTestImpl;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNAuthorization functionality for v2 of the API
 */
public class CNAuthorizationV2IT extends V2IsAuthorizedAuthorizationTestImpl
        implements AuthAPITestDefinitions, CNAuthTestDefinitions, CNAuthorizationSidTestDefinitions {

    @WebTestImplementation
    private AuthAPITestImplementations authTestImpl;
    
    @WebTestImplementation
    private CNAuthTestImplementations cnAuthTestImpl;
    
    @WebTestImplementation
    private SidCNTestImplementations sidImpl;

    
    @Override
    protected Iterator<Node> getNodeIterator()
    {
        return getCoordinatingNodeIterator();
    }

    @Before
    public void setup() {
        authTestImpl = new AuthAPITestImplementations(this);
        cnAuthTestImpl = new CNAuthTestImplementations(this);
        sidImpl = new SidCNTestImplementations();
    }

    @After
    public void cleanUp() {
        sidImpl.cleanUp();
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of authentication API methods";
    }

    @Override
    @Test
    public void testIsAuthorized() {
        authTestImpl.testIsAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSetRightsHolder() {
        cnAuthTestImpl.testSetRightsHolderV2(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSetAccessPolicy() {
        cnAuthTestImpl.testSetAccessPolicyV2(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSidIsAuthorized() {
        sidImpl.testIsAuthorized();
    }
}
