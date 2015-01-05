package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNCoreV1IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions{
    
    protected CoreTestImplementations coreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of core API methods";
    }
    
    @Override
    public void testPing() {
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }
}