package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNCoreSlowTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests some of the slower MNCore functionality for v1 of the API.
 * The other MNCore v1 methods are in {@link MNCoreV1IT}.
 */
public class MNCoreV1SlowIT extends ContextAwareTestCaseDataone
        implements MNCoreSlowTestDefinitions {

    @WebTestImplementation
    private CoreTestImplementations coreTestImpl;

    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 core API methods "
                + "that may run slowly";
    }

    @Before
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v1");
    }

}