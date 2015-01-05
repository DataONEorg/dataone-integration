package org.dataone.integration.it;

public interface MNCoreTestDefinitions {

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetCapabilities(Iterator, String)} method.
     */
    public void testGetCapabilities();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetCapabilities_HasCompatibleNodeContact(Iterator, String)} method.
     */
    public void testGetCapabilities_HasCompatibleNodeContact();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetCapabilities_NodeIdentityValidFormat(Iterator, String)} method.
     */
    public void testGetCapabilities_NodeIdentityValidFormat();


    
}
