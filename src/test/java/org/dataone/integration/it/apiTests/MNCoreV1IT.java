package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNCore functionality for v1 of the API 
 */
public class MNCoreV1IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, MNCoreTestDefinitions
{
    private CoreTestImplementations coreTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 1 of core API methods";
    }
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }
    
    
    @Override
    @Test
    public void testPing() {
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetCapabilities() {
        this.coreTestImpl.testGetCapabilities(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetCapabilities_HasCompatibleNodeContact() {
        this.coreTestImpl.testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetCapabilities_NodeIdentityValidFormat() {
        this.coreTestImpl.testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v1");
    }
}