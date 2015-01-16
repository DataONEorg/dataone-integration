package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNQueryTestDefinitions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNQuery functionality for v2 of the API 
 */
public class MNQueryV2IT extends ContextAwareTestCaseDataone 
        implements MNQueryTestDefinitions {

    //private MNQueryTestImplementations mnQueryTestImpl;

    @Before
    public void setup() {
        //mnQueryTestImpl = new MNQueryTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of query API methods";
    }

    @Override
    @Test
    @Ignore("No test exists for this yet")
    public void testListQueryEngines() {
        // TODO Auto-generated method stub

    }

    @Override
    @Test
    @Ignore("No test exists for this yet")
    public void testQuery() {
        // TODO Auto-generated method stub

    }

    @Override
    @Test
    @Ignore("No test exists for this yet")
    public void testGetQueryEngineDescription() {
        // TODO Auto-generated method stub

    }
}