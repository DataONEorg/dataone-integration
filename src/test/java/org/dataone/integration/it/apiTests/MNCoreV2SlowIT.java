package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNCoreSlowTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.it.testImplementations.MNCoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests some of the slower MNCore functionality for v2 of the API.
 * The other MNCore v2 methods are in {@link MNCoreV2IT}.  
 */
public class MNCoreV2SlowIT extends ContextAwareTestCaseDataone 
        implements MNCoreSlowTestDefinitions {

    private MNCoreTestImplementations mnCoreTestImpl;

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 core API methods "
                + "that may run slowly";
    }

    @Before
    public void setup() {
        mnCoreTestImpl = new MNCoreTestImplementations(this);
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        mnCoreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        mnCoreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        mnCoreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        mnCoreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        mnCoreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        mnCoreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }

}
