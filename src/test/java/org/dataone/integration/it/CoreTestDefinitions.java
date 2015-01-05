package org.dataone.integration.it;



/**
 * Base class for testing the core API functionality, meaning MNCore and CNCore
 * across API versions.
 */
public interface CoreTestDefinitions {

    // Tests that need to be implemented by subclasses:

    /**
     * Implementers should make use of the {@link CoreTests#testPing(Iterator, String)} method.
     */
    public void testPing();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords_AccessRestriction(Iterator, String)} method.
     */
    public void testGetLogRecords_AccessRestriction();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords(Iterator, String)} method.
     */
    public void testGetLogRecords();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords_Slicing(Iterator, String)} method.
     */
    public void testGetLogRecords_Slicing();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords_eventFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_eventFiltering();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords_pidFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_pidFiltering();

    /**
     * Implementers should make use of the {@link CoreTests#testGetLogRecords_dateFiltering(Iterator, String)} method.
     */
    public void testGetLogRecords_dateFiltering();


    // TODO: capabilities testing methods don't apply to CNs
    //       these should probably not be a common test
    //       but shouldn't duplicate code in CN IT subclasses either...


    /**
     * Implementers should make use of the {@link CoreTests#testGetCapabilities(Iterator, String)} method.
     */
    public void testGetCapabilities();

    /**
     * Implementers should make use of the {@link CoreTests#testGetCapabilities_HasCompatibleNodeContact(Iterator, String)} method.
     */
    public void testGetCapabilities_HasCompatibleNodeContact();

    /**
     * Implementers should make use of the {@link CoreTests#testGetCapabilities_NodeIdentityValidFormat(Iterator, String)} method.
     */
    public void testGetCapabilities_NodeIdentityValidFormat();



}
