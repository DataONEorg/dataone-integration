package org.dataone.integration.it.apiTests;

import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNDiagnosticTestDefinitions;
import org.dataone.integration.it.testImplementations.CNDiagnosticTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class CNDiagnosticV2IT extends ContextAwareTestCaseDataone implements CNDiagnosticTestDefinitions {

    @WebTestImplementation
    private CNDiagnosticTestImplementations diagTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN diagnosis API methods";
    }
    
    @BeforeClass
    public static void setupCnURL() {
        Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-dev-ucsb-1.test.dataone.org/cn");
    }
    
    @Before
    public void setup() {
        diagTestImpl = new CNDiagnosticTestImplementations(this);
    }
    
    @Override
    @Test @Ignore
    public void testEchoCredentials() {
        diagTestImpl.testEchoCredentials(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoSystemMetadata() {
        diagTestImpl.testEchoSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_NotAuthorized() {
        diagTestImpl.testEchoSystemMetadata_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_SerialVer() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_SerialVer(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoPid(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptyPid(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_BadPid() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_BadPid(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_FormatId() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_FormatId(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoSize() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoSize(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoChecksum() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoChecksum(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoSubmitter() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoSubmitter(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptySubmitter(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoRightsHolder(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_EmptyRightsHolder(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_AccessPolicy() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_AccessPolicy(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_ReplNum() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_ReplNum(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_ReplAllow() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_ReplAllow(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoOriginMN() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoOriginMN(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test @Ignore
    public void testEchoSystemMetadata_InvalidSysMeta_NoAuthMN() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta_NoAuthMN(getCoordinatingNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testEchoIndexedObject() {
        diagTestImpl.testEchoIndexedObject(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_NotAuthorized() {
        diagTestImpl.testEchoIndexedObject_NotAuthorized(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoPid() { 
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoPid(getCoordinatingNodeIterator(), "v2"); 
    }
    
    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyPid() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_EmptyPid(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_BadPid() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_BadPid(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_SerialVer() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_SerialVer(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_FormatId() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_FormatId(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSize() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoSize(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoChecksum() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoChecksum(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoSubmitter(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_EmptySubmitter(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoRightsHolder(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_EmptyRightsHolder(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_AccessPolicy(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplNum() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_ReplNum(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_ReplAllow() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_ReplAllow(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoOriginMN(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN() {
        diagTestImpl.testEchoIndexedObject_InvalidSystemMetadata_NoAuthMN(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_UnsupportedType() {
        diagTestImpl.testEchoIndexedObject_UnsupportedType(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test @Ignore
    public void testEchoIndexedObject_UnsupportedMetadataType() {
        diagTestImpl.testEchoIndexedObject_UnsupportedMetadataType(getCoordinatingNodeIterator(), "v2");
    }
}
