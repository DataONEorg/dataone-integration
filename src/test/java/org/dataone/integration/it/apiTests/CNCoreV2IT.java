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
        this.coreTestImpl.testPing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_AccessRestriction() {
        this.coreTestImpl.testGetLogRecords_AccessRestriction(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords() {
        this.coreTestImpl.testGetLogRecords(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_Slicing() {
        this.coreTestImpl.testGetLogRecords_Slicing(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_eventFiltering() {
        this.coreTestImpl.testGetLogRecords_eventFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_pidFiltering() {
        this.coreTestImpl.testGetLogRecords_pidFiltering(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetLogRecords_dateFiltering() {
        this.coreTestImpl.testGetLogRecords_dateFiltering(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreate() {
        this.cnCoreTestImpl.testCreate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("need a subject able to call create()")
    public void testCreateData_IdentifierEncoding() {
        this.cnCoreTestImpl.testCreateData_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListChecksumAlgorithms() {
        this.cnCoreTestImpl.testListChecksumAlgorithms(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListFormats() {
        this.cnCoreTestImpl.testListFormats(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat() {
        this.cnCoreTestImpl.testGetFormat(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetFormat_bogusFormat() {
        this.cnCoreTestImpl.testGetFormat_bogusFormat(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListNodes() {
        this.cnCoreTestImpl.testListNodes(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier() {
        this.cnCoreTestImpl.testGenerateIdentifier(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGenerateIdentifier_badScheme() {
        this.cnCoreTestImpl.testGenerateIdentifier_badScheme(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testReserveIdentifier() {
        this.cnCoreTestImpl.testReserveIdentifier(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testRegisterSystemMetadata() {
        this.cnCoreTestImpl.testRegisterSystemMetadata(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation() {
        this.cnCoreTestImpl.testHasReservation(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testHasReservation_noReservation() {
        this.cnCoreTestImpl.testHasReservation_noReservation(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testGetCapabilities() {
//        this.cnCoreTestImpl.testGetCapabilities(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("Not yet implemented")
    public void testUpdateSystemMetadata() {
//        this.cnCoreTestImpl.testUpdateSystemMetadata(getMemberNodeIterator(), "v2");
    }
}