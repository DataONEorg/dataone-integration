package org.dataone.integration.it.apiTests;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;

import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreSidTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNCoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.it.testImplementations.SidMNTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests MNCore functionality for v2 of the API 
 */
public class MNCoreV2IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, MNCoreTestDefinitions, MNCoreSidTestDefinitions
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

    @Override
    @Test
    @Ignore("API is unclear on PID|SID filter behavior; may only accept PIDs")
    public void testSidGetLogRecords() {
        sidImpl.testGetLogRecords();
    }
}