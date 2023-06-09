package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.V1V2InteropFunctionalTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class V1V2InteropFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    V1V2InteropFunctionalTestImplementations interopImpl = new V1V2InteropFunctionalTestImplementations();

    @Override
    protected String getTestDescription() {
        return "Test Case that runs metadata management scenarios including v1 and v2 nodes.";
    }

    @Before
    public void setup() {
        interopImpl.setup(getCoordinatingNodeIterator());
    }

    @Test
    public void testV2CreateV1GetSysmetaSameNode() {
        interopImpl.testV2CreateV1GetSysmetaSameNode();
    }

    @Test
    public void testV1CreateV2GetSysmetaSameNode() {
        interopImpl.testV1CreateV2GetSysmetaSameNode();
    }

    @Test
    public void testV2CreateV1DeleteSameNode() {
        interopImpl.testV2CreateV1DeleteSameNode();
    }

    @Test
    public void testV1CreateV2DeleteSameNode() {
        interopImpl.testV1CreateV2DeleteSameNode();
    }

    @Test
    public void testV2CreateV1ListObjectsSameNode() {
        interopImpl.testV2CreateV1ListObjectsSameNode();
    }

    @Test
    public void testV1CreateV2ListObjectsSameNode() {
        interopImpl.testV1CreateV2ListObjectsSameNode();
    }

    @Test
    public void testV2CreateV1GetSameNode() {
        interopImpl.testV2CreateV1GetSameNode();
    }

    @Test
    public void testV2CreateV1UpdateSameNode() {
        interopImpl.testV2CreateV1UpdateSameNode();
    }

    @Test
    public void testV1CreateV2UpdateSameNode() {
        interopImpl.testV1CreateV2UpdateSameNode();
    }

    @Test
    public void testV1CreateV2GetSysMeta() {
        interopImpl.testV1CreateV2GetSysmeta();
    }

    @Test
    public void testV1CreateV2Query() {
        interopImpl.testV1CreateV2Query();
    }

    public void testV2CreateV1Query() {
        interopImpl.testV2CreateV1Query();
    }

    @Test
    public void testV2CreateV1GetSysMeta() {
        interopImpl.testV2CreateV1GetSysMeta();
    }

    @Test
    public void testV1CreateV2Get() {
        interopImpl.testV1CreateV2Get();
    }

    @Test
    public void testV2CreateV1Delete() {
        interopImpl.testV2CreateV1Delete();
    }

    @Test
    public void testV1CreateV2Delete() {
        interopImpl.testV1CreateV2Delete();
    }

    @Test
    public void testV2CreateV1ListObjects() {
        interopImpl.testV2CreateV1ListObjects();
    }

    @Test
    public void testV1CreateV2ListObjects() {
        interopImpl.testV1CreateV2ListObjects();
    }
    
    @Test
    public void testV2CreateCnArchive() {
        interopImpl.testV2CreateCnArchive();
    }
    
    @Test
    public void testV2CreateCnSetAccessPolicy() {
        interopImpl.testV2CreateCnSetAccessPolicy();
    }
    
    @Test
    public void testV2CreateCnSetObsoletedBy() {
        interopImpl.testV2CreateCnSetObsoletedBy();
    }
    
    @Test
    public void testV2CreateCnSetReplicationPolicy() {
        interopImpl.testV2CreateCnSetReplicationPolicy();
    }
    
    @Test
    public void testV2CreateCnSetRightsHolder() {
        interopImpl.testV2CreateCnSetRightsHolder();
    }
}
