package org.dataone.integration.it.testDefinitions;

import org.dataone.integration.it.testImplementations.CoreTestImplementations;

/**
 * Base class for testing the core API functionality, meaning MNCore and CNCore
 * across API versions.
 */
public interface CoreTestDefinitions {

    // Tests that need to be implemented by subclasses:

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testPing(Iterator, String)} method.
     */
    public void testPing();

}
