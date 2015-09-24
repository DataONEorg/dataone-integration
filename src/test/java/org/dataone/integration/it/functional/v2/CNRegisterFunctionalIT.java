package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.NodeRegistryExtensibilityTestDefinitions;
import org.dataone.integration.it.testImplementations.NodeRegistryExtensibilityTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class CNRegisterFunctionalIT extends ContextAwareTestCaseDataone implements NodeRegistryExtensibilityTestDefinitions {

    private NodeRegistryExtensibilityTestImplementations extensibilityImpl;
    
    @Before
    public void setup() {
        extensibilityImpl = new NodeRegistryExtensibilityTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs CN register API methods to check if Node properties can be modified.";
    }

    @Override
    @Test
    public void testRegister() {
        extensibilityImpl.testRegister(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegister_NoPropType() {
        extensibilityImpl.testRegister_NoPropType(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateNodeCapabilities() {
        extensibilityImpl.testUpdateNodeCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    
}
