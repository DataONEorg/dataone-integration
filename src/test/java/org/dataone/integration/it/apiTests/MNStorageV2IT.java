package org.dataone.integration.it.apiTests;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNStorageSidTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNStorageTestDefinitions;
import org.dataone.integration.it.testDefinitions.MNv2StorageTestDefinitions;
import org.dataone.integration.it.testImplementations.MNStorageTestImplementations;
import org.dataone.integration.it.testImplementations.SidMNTestImplementations;
import org.dataone.integration.it.testImplementations.MNUpdateSystemMetadataTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNStorageV2IT extends ContextAwareTestCaseDataone
        implements MNStorageTestDefinitions, MNv2StorageTestDefinitions, MNStorageSidTestDefinitions {

    @WebTestImplementation
    private MNStorageTestImplementations mnStorageTestImpl;

    @WebTestImplementation
    private SidMNTestImplementations sidImpl;

    @WebTestImplementation
    private MNUpdateSystemMetadataTestImplementations updSysmetaImpl;

    
    @Before
    public void setup() {
        mnStorageTestImpl = new MNStorageTestImplementations(this);
        sidImpl = new SidMNTestImplementations();
        updSysmetaImpl = new MNUpdateSystemMetadataTestImplementations(this);
        List<Node> cns = IteratorUtils.toList(getCoordinatingNodeIterator());
        updSysmetaImpl.setup(cns.size() > 0 ? cns.get(0) : null);
    }

    @After
    public void cleanUp() {
        sidImpl.cleanUp();
    }
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN version 2 of storage API methods";
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
    public void testCreate() {
        mnStorageTestImpl.testCreate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testCreate_NoCert() {
        mnStorageTestImpl.testCreate_NoCert(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testCreateData_IdentifierEncoding() {
        mnStorageTestImpl.testCreateData_IdentifierEncoding(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdate() {
        mnStorageTestImpl.testUpdate(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdate_badObsoletedByInfo() {
        mnStorageTestImpl.testUpdate_badObsoletedByInfo(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdate_badObsoletesInfo() {
        mnStorageTestImpl.testUpdate_badObsoletesInfo(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdate_NoCert() {
        mnStorageTestImpl.testUpdate_NoCert(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdate_NoRightsOnObsoleted() {
        mnStorageTestImpl.testUpdate_NoRightsOnObsoleted(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testArchive() {
        mnStorageTestImpl.testArchive(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testArchive_NotFound() {
        mnStorageTestImpl.testArchive_NotFound(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testArchive_NoCert() {
        mnStorageTestImpl.testArchive_NoCert(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDelete_NoCert() {
        mnStorageTestImpl.testDelete_NoCert(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("No test exists for this yet.")
    public void testGenerateIdentifier() {
        //mnStorageTestImpl.testGenerateIdentifier(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_RightsHolder() {
        updSysmetaImpl.testUpdateSystemMetadata_RightsHolder(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_NotFound() {
        updSysmetaImpl.testUpdateSystemMetadata_NotFound(getMemberNodeIterator(), "v2");
    }
    
    @Override
    public void testUpdateSystemMetadata_NotAuthorized() {
        updSysmetaImpl.testUpdateSystemMetadata_NotAuthorized(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    @Ignore("No MN test certificates to test with at the moment")
    public void testUpdateSystemMetadata_NotAuthorizedMN() {
        updSysmetaImpl.testUpdateSystemMetadata_NotAuthorizedMN(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_NoPid() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_NoPid(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_NoSerialVersion() {
        updSysmetaImpl.testUpdateSystemMetadata_NoSerialVersion(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_PidMismatch(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_SysmetaUnmodified() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidSystemMetadata_SysmetaUnmodified(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_MutableRightsHolder() {
        updSysmetaImpl.testUpdateSystemMetadata_MutableRightsHolder(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_MutableFormat() {
        updSysmetaImpl.testUpdateSystemMetadata_MutableFormat(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_MutableAccessPolicy() {
        updSysmetaImpl.testUpdateSystemMetadata_MutableAccessPolicy(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_MutableReplPolicy() {
        updSysmetaImpl.testUpdateSystemMetadata_MutableReplPolicy(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testSidCreate() {
        sidImpl.testCreate();
    }

    @Override
    @Test
    public void testSidUpdate() {
        sidImpl.testUpdate();
    }

    @Override
    @Test
    public void testSidDelete() {
        sidImpl.testDelete();
    }

    @Override
    @Test
    public void testSidArchive() {
        //TODO: implement test?
    	//sidImpl.testArchive();
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedIdentifier(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSize() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSize(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedChecksum(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSubmitter(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedDateUploaded(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedOriginMN(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_NullOriginMN() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_NullOriginMN(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId() {
        updSysmetaImpl.testUpdateSystemMetadata_InvalidRequest_ModifiedSeriesId(getMemberNodeIterator(), "v2");
    }

}