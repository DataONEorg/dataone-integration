package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNCoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNv2CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CNCoreTestImplementations;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNCore functionality for v2 of the API 
 */
public class CNCoreV2IT extends ContextAwareTestCaseDataone
implements CoreTestDefinitions, CNCoreTestDefinitions, CNv2CoreTestDefinitions{

    private CoreTestImplementations coreTestImpl;
    private CNCoreTestImplementations cnCoreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
        cnCoreTestImpl = new CNCoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of core API methods";
    }
    
    @Override
    @Test
    public void testPing() {
        this.coreTestImpl.testPing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        this.cnCoreTestImpl.testGetLogRecords(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        this.cnCoreTestImpl.testGetLogRecords_Slicing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        this.cnCoreTestImpl.testGetLogRecords_dateFiltering(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreate() {
        this.cnCoreTestImpl.testCreate(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreateData_IdentifierEncoding() {
        this.cnCoreTestImpl.testCreateData_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListChecksumAlgorithms() {
        this.cnCoreTestImpl.testListChecksumAlgorithms(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListFormats() {
        this.cnCoreTestImpl.testListFormats(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat() {
        this.cnCoreTestImpl.testGetFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat_bogusFormat() {
        this.cnCoreTestImpl.testGetFormat_bogusFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListNodes() {
        this.cnCoreTestImpl.testListNodes(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier() {
        this.cnCoreTestImpl.testGenerateIdentifier(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier_badScheme() {
        this.cnCoreTestImpl.testGenerateIdentifier_badScheme(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testReserveIdentifier() {
        this.cnCoreTestImpl.testReserveIdentifier(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegisterSystemMetadata() {
        this.cnCoreTestImpl.testRegisterSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation() {
        this.cnCoreTestImpl.testHasReservation(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation_noReservation() {
        this.cnCoreTestImpl.testHasReservation_noReservation(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testGetCapabilities() {
//        this.cnCoreTestImpl.testGetCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testUpdateSystemMetadata() {
//        this.cnCoreTestImpl.testUpdateSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }
}