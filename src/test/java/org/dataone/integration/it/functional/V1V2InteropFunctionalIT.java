package org.dataone.integration.it.functional;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.V1V2InteropFunctionalTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class V1V2InteropFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    V1V2InteropFunctionalTestImplementations interopImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs metadata management scenarios including v1 and v2 nodes.";
    }

    @Before
    public void setup() {
        interopImpl.setup();
    }
    
    @Test
    public void testV2CreateV1UpdateSysmeta() {
        interopImpl.testV2CreateV1UpdateSysmeta();
    }
    
    @Test
    public void testV2CreateV1Update() {
        interopImpl.testV2CreateV1Update();
    }
    
    @Test
    public void testV1CreateV2Update() {
        interopImpl.testV1CreateV2Update();
    }
    
    @Test
    public void testV2CreateReplicate() {
        interopImpl.testV2CreateReplicate();
    }
    
    @Test
    public void testV2CreateV1GetSysmetaSameNode() {
        interopImpl.testV2CreateV1GetSysmetaSameNode();
    }
    
    @Test
    public void testV1CreateV2GetSysmeta() {
        interopImpl.testV1CreateV2GetSysmeta();
    }
    
    @Test
    public void testV1CreateReplicate() {
        interopImpl.testV1CreateReplicate();
    }
    
    @Test
    public void testV1CreateV2Query() {
        interopImpl.testV1CreateV2Query();
    }

    @Test
    public void testV2CreateV1Query() {
        interopImpl.testV2CreateV1Query();
    }
}
