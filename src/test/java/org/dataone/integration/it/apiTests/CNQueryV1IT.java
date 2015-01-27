package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.QueryTestDefinitions;
import org.dataone.integration.it.testImplementations.QueryTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNQuery functionality for v1 of the API 
 */
public class CNQueryV1IT extends ContextAwareTestCaseDataone 
implements QueryTestDefinitions {

    private QueryTestImplementations queryTestImpl;

    @Before
    public void setup() {
        queryTestImpl = new QueryTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of query API methods";
    }

    @Override
    @Test
    public void testListQueryEngines() {
        queryTestImpl.testListQueryEngines(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        queryTestImpl.testGetQueryEngineDescription(getCoordinatingNodeIterator(), "v1");

    }
}