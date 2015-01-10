package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNCoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNCoreV1IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, CNCoreTestDefinitions {
    
    private CoreTestImplementations coreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of core API methods";
    }
    
    @Override
    @Test
    public void testPing() {
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }
}