package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNQueryTestDefinitions;
import org.dataone.integration.it.testDefinitions.QueryTestDefinitions;
import org.dataone.integration.it.testImplementations.QueryTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNQuery functionality for v2 of the API 
 */
public class MNQueryV2IT extends ContextAwareTestCaseDataone 
implements QueryTestDefinitions {

    @WebTestImplementation
    private QueryTestImplementations queryTestImpl;

    @Before
    public void setup() {
        queryTestImpl = new QueryTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of query API methods";
    }

    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }
    
    @Override
    @Test
    public void testListQueryEngines() {
        queryTestImpl.testListQueryEngines(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        queryTestImpl.testGetQueryEngineDescription(getMemberNodeIterator(), "v2");

    }
}