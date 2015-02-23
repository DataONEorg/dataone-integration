package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.QueryTestDefinitions;
import org.dataone.integration.it.testImplementations.QueryTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNQuery functionality for v2 of the API 
 */
public class CNQueryV2IT extends ContextAwareTestCaseDataone 
implements QueryTestDefinitions {

    @WebTestImplementation
    private QueryTestImplementations queryTestImpl;

    
    @Before
    public void setup() {
        queryTestImpl = new QueryTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of query API methods";
    }

    @Override
    @Test
    public void testListQueryEngines() {
        queryTestImpl.testListQueryEngines(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        queryTestImpl.testGetQueryEngineDescription(getCoordinatingNodeIterator(), "v2");

    }
}