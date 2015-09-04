package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNCoreSidTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNCoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNv2CoreTestDefinitions;
import org.dataone.integration.it.testDefinitions.CoreTestDefinitions;
import org.dataone.integration.it.testImplementations.CNCoreTestImplementations;
import org.dataone.integration.it.testImplementations.CNUpdateSystemMetadataTestImplementations;
import org.dataone.integration.it.testImplementations.CoreTestImplementations;
import org.dataone.integration.it.testImplementations.SidCNTestImplementations;
import org.dataone.integration.it.testImplementations.SynchronizeMetadataTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests CNCore functionality for v2 of the API 
 */
public class CNCoreV2IT extends ContextAwareTestCaseDataone
implements CoreTestDefinitions, CNCoreTestDefinitions, CNv2CoreTestDefinitions, CNCoreSidTestDefinitions {

    @WebTestImplementation
    private CoreTestImplementations coreTestImpl;

    @WebTestImplementation
    private CNCoreTestImplementations cnCoreTestImpl;

    @WebTestImplementation
    private SidCNTestImplementations sidImpl;

    @WebTestImplementation
    private CNUpdateSystemMetadataTestImplementations updateSysMetaImpl;
    
    @WebTestImplementation
    private SynchronizeMetadataTestImplementations syncMetaImpl;
    
    @Before
    public void setup() {
        coreTestImpl = new CoreTestImplementations(this);
        cnCoreTestImpl = new CNCoreTestImplementations(this);
        sidImpl = new SidCNTestImplementations();
        updateSysMetaImpl = new CNUpdateSystemMetadataTestImplementations(this);
        syncMetaImpl = new SynchronizeMetadataTestImplementations(this);
    }
    
    @After
    public void cleanUp() {
        sidImpl.cleanUp();
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

    @Test
    public void testRegisterSystemMetadata_NotAuthorized() {
        cnCoreTestImpl.testRegisterSystemMetadata_NotAuthorized(getCoordinatingNodeIterator(), "v2");
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
    public void testGetCapabilities() {
        coreTestImpl.testGetCapabilities(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSidCreate() {
        sidImpl.testCreate();
    }

    @Override
    @Test
    public void testDelete() {
        sidImpl.testDelete();
    }

    @Override
    @Test
    public void testSidArchive() {
        sidImpl.testArchive();
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_NotFound() {
        updateSysMetaImpl.testUpdateSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_NotAuthorized() {
        updateSysMetaImpl.testUpdateSystemMetadata_NotAuthorized(getCoordinatingNodeIterator(), "v2");  
    }

    @Override
    @Test
    @Ignore("Currently there's no MN certificate available to test with.")
    public void testUpdateSystemMetadata_NotAuthorizedMN() {
        updateSysMetaImpl.testUpdateSystemMetadata_NotAuthorizedMN(getCoordinatingNodeIterator(), "v2");        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidSystemMetadata_NoSerialVersion(getCoordinatingNodeIterator(), "v2");        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_NoPid() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_NoPid(getCoordinatingNodeIterator(), "v2");        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_PidMismatch(getCoordinatingNodeIterator(), "v2");        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(getCoordinatingNodeIterator(), "v2");      
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(getCoordinatingNodeIterator(), "v2");       
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSize(getCoordinatingNodeIterator(), "v2");       
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(getCoordinatingNodeIterator(), "v2");        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(getCoordinatingNodeIterator(), "v2");
        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId() {
        updateSysMetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(getCoordinatingNodeIterator(), "v2");
        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_NotAuthorized_RightsHolder() {
        updateSysMetaImpl.testUpdateSystemMetadata_CN(getCoordinatingNodeIterator(), "v2");
        
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_CN() {
        updateSysMetaImpl.testUpdateSystemMetadata_CN(getCoordinatingNodeIterator(), "v2");
        
    }

    @Override
    @Test
    public void testSynchronize_NotAuthorized() {
        syncMetaImpl.testSynchronize_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("No MN cert to test with")
    public void testSynchronize_NotAuthorized_MN() {
        syncMetaImpl.testSynchronize_NotAuthorized_MN(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("No MN cert to test with")
    public void testSynchronize_AuthorizedMN() {
        syncMetaImpl.testSynchronize_AuthorizedMN(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("No MN cert to test with")
    public void testSynchronize_NotFound() {
        syncMetaImpl.testSynchronize_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    
}