package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreSlowTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.it.testImplementations.MNCoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests some of the slower MNCore functionality for v1 of the API.
 * The other MNCore v1 methods are in {@link MNCoreV1IT}.
 */
public class MNCoreV1SlowIT extends ContextAwareTestCaseDataone 
        implements MNCoreSlowTestDefinitions {
    
    private CoreTestImplementations coreTestImpl;
    private MNCoreTestImplementations mnCoreTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 core API methods "
                + "that may run slowly";
    }
    
    @Before 
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
        mnCoreTestImpl = new MNCoreTestImplementations(this);
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        mnCoreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        mnCoreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        mnCoreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        mnCoreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        mnCoreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        mnCoreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v1");
    }

}