package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNRegisterTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNv2RegisterTestDefinitions;
import org.dataone.integration.it.testImplementations.CNRegisterTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNRegister functionality for v2 of the API 
 */
public class CNRegisterV2IT extends ContextAwareTestCaseDataone
        implements CNRegisterTestDefinitions, CNv2RegisterTestDefinitions {

    @WebTestImplementation
    private CNRegisterTestImplementations registerTestImpl;

    
    @Before
    public void setup() {
        registerTestImpl = new CNRegisterTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of register API methods";
    }

    @Override
    @Test
    @Ignore("don't want to keep creating new phantom nodes")
    public void testRegister() {
        registerTestImpl.testRegister(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegister_IdentifierNotUnique() {
        registerTestImpl.testRegister_IdentifierNotUnique(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetNodeCapabilities() {
        registerTestImpl.testGetNodeCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities() {
        registerTestImpl.testUpdateNodeCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_NotFound() {
        registerTestImpl.testUpdateNodeCapabilities_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_NotAuthorized() {
        registerTestImpl.testUpdateNodeCapabilities_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_updatingOtherField() {
        registerTestImpl.testUpdateNodeCapabilities_updatingOtherField(getCoordinatingNodeIterator(), "v2");
    }
}
