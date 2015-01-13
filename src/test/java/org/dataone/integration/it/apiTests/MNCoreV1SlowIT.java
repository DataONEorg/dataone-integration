package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreSlowTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class MNCoreV1SlowIT extends ContextAwareTestCaseDataone 
        implements MNCoreSlowTestDefinitions {
    
    private CoreTestImplementations coreTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 core API methods "
                + "that may run slowly";
    }
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v1");
    }

}