package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNDiagnosticTestDefinitions;
import org.dataone.integration.it.testImplementations.CNDiagnosticTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class CNDiagnosticV2IT extends ContextAwareTestCaseDataone implements CNDiagnosticTestDefinitions {

    @WebTestImplementation
    private CNDiagnosticTestImplementations diagTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN diagnosis API methods";
    }
    
    @Before 
    public void setup() {
        diagTestImpl = new CNDiagnosticTestImplementations(this);
    }
    
    @Override
    @Test
    public void testEchoCredentials() {
        diagTestImpl.testEchoCredentials(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata() {
        diagTestImpl.testEchoSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata_NotAuthorized() {
        diagTestImpl.testEchoSystemMetadata_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_SerialVer() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_SerialVer(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoPid(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptyPid(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_BadPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_BadPid(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_FormatId() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_FormatId(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoSize() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoSize(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoChecksum() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoChecksum(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoSubmitter() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_AccessPolicy() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_ReplNum() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_ReplNum(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_ReplAllow() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_ReplAllow(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoOriginMN() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidSysMeta_NoAuthMN() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoSystemMetadata_InvalidRequest() {
        diagTestImpl.testEchoSystemMetadata_InvalidRequest(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject() {
        diagTestImpl.testEchoIndexedObject(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject_NotAuthorized() {
        diagTestImpl.testEchoIndexedObject_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject_InvalidRequest() {
        diagTestImpl.testEchoIndexedObject_InvalidRequest(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject_InvalidSystemMetadata() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject_UnsupportedType() {
        diagTestImpl.testEchoIndexedObject_UnsupportedType(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoIndexedObject_UnsupportedMetadataType() {
        diagTestImpl.testEchoIndexedObject_UnsupportedMetadataType(getCoordinatingNodeIterator(), "v2");
    }
}
