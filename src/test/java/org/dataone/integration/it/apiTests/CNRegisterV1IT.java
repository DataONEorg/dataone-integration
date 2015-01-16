package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNRegisterTestDefinitions;
import org.dataone.integration.it.testImplementations.CNRegisterTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNRegister functionality for v1 of the API 
 */
public class CNRegisterV1IT extends ContextAwareTestCaseDataone
        implements CNRegisterTestDefinitions {

    private CNRegisterTestImplementations registerTestImpl;

    @Before
    public void setup() {
        registerTestImpl = new CNRegisterTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of register API methods";
    }

    @Override
    @Test
    @Ignore("don't want to keep creating new phantom nodes")
    public void testRegister() {
        registerTestImpl.testRegister(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testRegister_IdentifierNotUnique() {
        registerTestImpl.testRegister_IdentifierNotUnique(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("No test implemented for this yet.")
    public void testGetNodeCapabilities() {
//        registerTestImpl.testGetNodeCapabilities(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities() {
        registerTestImpl.testUpdateNodeCapabilities(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_NotFound() {
        registerTestImpl.testUpdateNodeCapabilities_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_NotAuthorized() {
        registerTestImpl.testUpdateNodeCapabilities_NotAuthorized(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testUpdateNodeCapabilities_updatingOtherField() {
        registerTestImpl.testUpdateNodeCapabilities_updatingOtherField(getCoordinatingNodeIterator(), "v1");
    }
}
