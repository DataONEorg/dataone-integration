package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MNCore functionality for v2 of the API 
 */
public class MNCoreV2IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, MNCoreTestDefinitions
{
    private CoreTestImplementations coreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 or core API methods";
    }
    
    @Override
    @Test
    public void testPing() {
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities() {
        this.coreTestImpl.testGetCapabilities(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities_HasCompatibleNodeContact() {
        this.coreTestImpl.testGetCapabilities_HasCompatibleNodeContact(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetCapabilities_NodeIdentityValidFormat() {
        this.coreTestImpl.testGetCapabilities_NodeIdentityValidFormat(getMemberNodeIterator(), "v2");
    }
}