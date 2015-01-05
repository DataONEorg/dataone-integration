package org.dataone.integration.it;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNCoreV1IT extends CoreTests {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of core API methods";
    }
    
    @Override
    public void testPing() {
        testPing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        testGetLogRecords_AccessRestriction(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords() {
        testGetLogRecords(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_Slicing() {
        testGetLogRecords_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        testGetLogRecords_eventFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        testGetLogRecords_pidFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        testGetLogRecords_dateFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities() {
        // CN nodes don't need to support getCapabilities, test passes TODO:   rethink how this is done...
    }

    @Override
    public void testGetCapabilities_HasCompatibleNodeContact() {
        // CN nodes don't need to support getCapabilities, test passes
    }

    @Override
    public void testGetCapabilities_NodeIdentityValidFormat() {
        // CN nodes don't need to support getCapabilities, test passes
    }
}