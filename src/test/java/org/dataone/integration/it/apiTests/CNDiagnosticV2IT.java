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
    public void testEchoSystemMetadata_InvalidSysMeta() {
        diagTestImpl.testEchoSystemMetadata_InvalidSysMeta(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testEchoSystemMetadata_IdentifierNotUnique() {
        diagTestImpl.testEchoSystemMetadata_IdentifierNotUnique(getCoordinatingNodeIterator(), "v2");
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

    @Override
    @Test
    public void testEchoIndexedObject_InusfficientResources() {
        diagTestImpl.testEchoIndexedObject_InusfficientResources(getCoordinatingNodeIterator(), "v2");
    }
    
}
