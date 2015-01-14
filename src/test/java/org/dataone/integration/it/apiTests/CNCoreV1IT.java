package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNCoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CNCoreTestImplementations;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNCoreV1IT extends ContextAwareTestCaseDataone 
implements CoreTestDefinitions, CNCoreTestDefinitions {
    
    private CoreTestImplementations coreTestImpl;
    private CNCoreTestImplementations cnCoreTestImpl;
    
    @Before 
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
        cnCoreTestImpl = new CNCoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of core API methods";
    }
    
    @Override
    @Test
    public void testPing() {
        this.coreTestImpl.testPing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreate() {
        this.cnCoreTestImpl.testCreate(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreateData_IdentifierEncoding() {
        this.cnCoreTestImpl.testCreateData_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListChecksumAlgorithms() {
        this.cnCoreTestImpl.testListChecksumAlgorithms(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListFormats() {
        this.cnCoreTestImpl.testListFormats(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetFormat() {
        this.cnCoreTestImpl.testGetFormat(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetFormat_bogusFormat() {
        this.cnCoreTestImpl.testGetFormat_bogusFormat(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListNodes() {
        this.cnCoreTestImpl.testListNodes(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGenerateIdentifier() {
        this.cnCoreTestImpl.testGenerateIdentifier(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGenerateIdentifier_badScheme() {
        this.cnCoreTestImpl.testGenerateIdentifier_badScheme(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testReserveIdentifier() {
        this.cnCoreTestImpl.testReserveIdentifier(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testRegisterSystemMetadata() {
        this.cnCoreTestImpl.testRegisterSystemMetadata(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testHasReservation() {
        this.cnCoreTestImpl.testHasReservation(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testHasReservation_noReservation() {
        this.cnCoreTestImpl.testHasReservation_noReservation(getCoordinatingNodeIterator(), "v1");
    }
}