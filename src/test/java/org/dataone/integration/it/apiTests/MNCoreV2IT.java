package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.it.testImplementations.SidMNTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNCore functionality for v2 of the API 
 */
public class MNCoreV2IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, MNCoreTestDefinitions
{
    @WebTestImplementation
    private CoreTestImplementations coreTestImpl;
    @WebTestImplementation
    private SidMNTestImplementations sidImpl;
    
    
    @Before 
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
        sidImpl = new SidMNTestImplementations();
    }
    
    @After
    public void cleanUp() {
        sidImpl.cleanUp();
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 or core API methods";
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
    public void testPing() {
        coreTestImpl.testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities() {
        coreTestImpl.testGetCapabilities(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities_HasCompatibleNodeContact() {
        coreTestImpl.testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities_NodeIdentityValidFormat() {
        coreTestImpl.testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v2");
    }
}