package org.dataone.integration.it.testDefinitions;

import org.dataone.integration.it.testImplementations.CoreTestImplementations;

/**
 * Defines the test methods required to test MNCore API functionality
 * across v1/v2.
 */
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
