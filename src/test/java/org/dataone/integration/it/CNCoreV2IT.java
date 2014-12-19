package org.dataone.integration.it;

/**
 * Tests CNCore functionality for v2 of the API 
 */
public class CNCoreV2IT extends CoreIT {

    @Override
    public void testPing() {
        testPing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        testGetLogRecords_AccessRestriction(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords() {
        testGetLogRecords(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_Slicing() {
        // TODO Auto-generated method stub

    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        // TODO Auto-generated method stub

    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        // TODO Auto-generated method stub

    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        // TODO Auto-generated method stub

    }

    @Override
    protected String getTestDescription() {
        // TODO Auto-generated method stub
        return null;
    }
}