package org.dataone.integration.it;

/**
 * Tests MNCore functionality for v2 of the API 
 */
public class MNCoreV2IT extends CoreIT {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 or core API methods";
    }
    
    @Override
    public void testPing() {
        testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords() {
        testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_Slicing() {
        testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetCapabilities() {
        testGetCapabilities(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetCapabilities_HasCompatibleNodeContact() {
        testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetCapabilities_NodeIdentityValidFormat() {
        testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v2");
    }
}