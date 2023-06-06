package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.CNDiagnosticFunctionalTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class CNDiagnosticFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    private CNDiagnosticFunctionalTestImplementations diagImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs CN diagnostic API methods and checks results for correctness.";
    }

    @Before
    public void setup() {
        diagImpl = new CNDiagnosticFunctionalTestImplementations(this);
    }
    
    @Test
    public void testEchoCredentials_RightsHolder() {
        diagImpl.testEchoCredentials_RightsHolder(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testEchoCredentials_TestPerson() {
        diagImpl.testEchoCredentials_TestPerson(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testEchoSystemMetadata() {
        diagImpl.testEchoSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testEchoIndexedObject() {
        diagImpl.testEchoIndexedObject(getCoordinatingNodeIterator(), "v2");
    }
    
    @Test
    public void testEchoIndexedObject_resourceMap() {
        diagImpl.testEchoIndexedObject_resourceMap(getCoordinatingNodeIterator(), "v2");
    }
    
}
