package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.LogAggregationFunctionalTestImplementations;
import org.dataone.integration.it.testImplementations.MNSystemMetadataMutabilityImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class MNSystemMetadataMutabilityIt extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    MNSystemMetadataMutabilityImplementations sysmetaImpl;

    @Override
    protected String getTestDescription() {
        return "Test Case that runs tests against mutability of fields in sysmeta.";
    }

    @Before
    public void setup() {
        sysmetaImpl = new MNSystemMetadataMutabilityImplementations();
        sysmetaImpl.setup(getCoordinatingNodeIterator());
    }

    @Test
    public void testRegisterSystemMetadata_dateModified() {
        sysmetaImpl.testRegisterSystemMetadata_dateModified();
    }
    
    @Test
    public void testSetReplicationStatus_dateModified() {
        sysmetaImpl.testSetReplicationStatus_dateModified();
    }
    
    @Test
    public void testSetReplicationMetadata_dateModified() {
        sysmetaImpl.testUpdateReplicationMetadata_dateModified();
    }
    
    @Test
    public void testDeleteReplicationMetacat_dateModified() {
        sysmetaImpl.testDeleteReplicationMetacat_dateModified();
    }
        
    /*
     
    The following calls shouldn't change
    sysmeta.dateModified 
     
    CN.create
    CN.updateSystemMetadata
    CN.setReplicationStatus
    CN.setReplicationMetadata
    CN.setReplicationPolicy
    CN.setAccessPolicy
    CN.setObsoletedBy
    CN.setArchive
    CN.setRightsHolder
    CN.registerSystemMetadata
    
    
    */
}
