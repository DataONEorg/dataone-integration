package org.dataone.integration.it.testDefinitions;

/**
 * Defines a number of MN core API test methods that may run slowly 
 * and may need to be run separately from the other MN core tests. 
 */
public interface MNCoreSlowTestDefinitions {

    public void testGetLogRecords_AccessRestriction();

    public void testGetLogRecords();

    public void testGetLogRecords_Slicing();

    public void testGetLogRecords_eventFiltering();

    public void testGetLogRecords_pidFiltering();

    public void testGetLogRecords_dateFiltering();
    
}
