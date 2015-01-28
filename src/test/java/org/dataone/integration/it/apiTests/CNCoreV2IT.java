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
        coreTestImpl = new CoreTestImplementations(this);
        cnCoreTestImpl = new CNCoreTestImplementations(this);
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of core API methods";
    }
    
    @Override
    @Test
    public void testPing() {
        coreTestImpl.testPing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        coreTestImpl.testGetLogRecords(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        coreTestImpl.testGetLogRecords_Slicing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        coreTestImpl.testGetLogRecords_dateFiltering(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreate() {
        cnCoreTestImpl.testCreate(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreateData_IdentifierEncoding() {
        cnCoreTestImpl.testCreateData_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListChecksumAlgorithms() {
        cnCoreTestImpl.testListChecksumAlgorithms(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListFormats() {
        cnCoreTestImpl.testListFormats(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat() {
        cnCoreTestImpl.testGetFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat_bogusFormat() {
        cnCoreTestImpl.testGetFormat_bogusFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListNodes() {
        cnCoreTestImpl.testListNodes(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier() {
        cnCoreTestImpl.testGenerateIdentifier(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier_badScheme() {
        cnCoreTestImpl.testGenerateIdentifier_badScheme(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testReserveIdentifier() {
        cnCoreTestImpl.testReserveIdentifier(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegisterSystemMetadata() {
        cnCoreTestImpl.testRegisterSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation() {
        cnCoreTestImpl.testHasReservation(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation_noReservation() {
        cnCoreTestImpl.testHasReservation_noReservation(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testGetCapabilities() {
//        cnCoreTestImpl.testGetCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testUpdateSystemMetadata() {
//        cnCoreTestImpl.testUpdateSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }
}