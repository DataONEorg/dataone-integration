package org.dataone.integration.it;

/**
 * Tests MNCore functionality for v1 of the API 
 */
public class MNCoreV1IT extends CoreIT {

    @Override
    public void testPing() {
        testPing(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords() {
        testGetLogRecords(getMemberNodeIterator(), "v1");
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