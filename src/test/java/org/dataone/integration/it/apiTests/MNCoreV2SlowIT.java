package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNCoreSlowTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests some of the slower MNCore functionality for v2 of the API.
 * The other MNCore v2 methods are in {@link MNCoreV2IT}.  
 */
public class MNCoreV2SlowIT extends ContextAwareTestCaseDataone 
        implements MNCoreSlowTestDefinitions {

    @WebTestImplementation
    private CoreTestImplementations coreTestImpl;

    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 core API methods "
                + "that may run slowly";
    }
    
    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }

    @Before
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("causing severe delays in Sandbox environment for a node")
    public void testGetLogRecords_eventFiltering() {
        coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }

}
