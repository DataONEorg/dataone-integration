package org.dataone.integration.it.testDefinitions;

/**
 * Defines tests that show whether updating Node properties is possible - adding new
 * key/value pairs through the use of register() and updateNodeCapabilities().   
 * @author Andrei
 */
public interface NodeRegistryExtensibilityTestDefinitions {

    public void testRegister();
    public void testRegister_NoPropType();
    public void testUpdateNodeCapabilities();
    
}
