package org.dataone.integration.it;



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

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords_AccessRestriction(Iterator, String)} method.
     */
    public void testGetLogRecords_AccessRestriction();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords(Iterator, String)} method.
     */
    public void testGetLogRecords();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords_Slicing(Iterator, String)} method.
     */
    public void testGetLogRecords_Slicing();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords_eventFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_eventFiltering();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords_pidFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_pidFiltering();

    /**
     * Implementers should make use of the {@link CoreTestImplementations#testGetLogRecords_dateFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_dateFiltering();

}
