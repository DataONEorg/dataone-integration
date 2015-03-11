package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.QueryTestDefinitions;
import org.dataone.integration.it.testImplementations.QueryTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNQuery functionality for v1 of the API 
 */
public class MNQueryV1IT extends ContextAwareTestCaseDataone 
        implements QueryTestDefinitions {

    @WebTestImplementation
    private QueryTestImplementations queryTestImpl;

    @Before
    public void setup() {
        queryTestImpl = new QueryTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of query API methods";
    }

    @Override
    @Test
    public void testListQueryEngines() {
        queryTestImpl.testListQueryEngines(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        queryTestImpl.testGetQueryEngineDescription(getMemberNodeIterator(), "v1");
        
    }
}
