package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;

/**
 * Tests MNCore functionality for v1 of the API 
 */
public class MNCoreV1IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, MNCoreTestDefinitions
{

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of core API methods";
    }
    
    protected CoreTestImplementations coreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }
    
    
    @Override
    public void testPing() {
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities() {
        this.coreTestImpl.testGetCapabilities(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities_HasCompatibleNodeContact() {
        this.coreTestImpl.testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v1");
    }

    @Override
    public void testGetCapabilities_NodeIdentityValidFormat() {
        this.coreTestImpl.testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v1");
    }
}