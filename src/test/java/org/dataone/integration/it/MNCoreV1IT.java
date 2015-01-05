package org.dataone.integration.it;

/**
 * Tests MNCore functionality for v1 of the API 
 */
public class MNCoreV1IT extends CoreTests {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of core API methods";
    }
    
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
        testGetLogRecords_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities() {
        testGetCapabilities(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities_HasCompatibleNodeContact() {
        testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities_NodeIdentityValidFormat() {
        testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v1");
    }
}