package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.SSLTestDefinitions;
import org.dataone.integration.it.testImplementations.SSLTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Test;

public class MNAuthenticationV1V2IT extends ContextAwareTestCaseDataone 
implements  SSLTestDefinitions {

    @WebTestImplementation
    private SSLTestImplementations sslTestImpl;

    
    @Before
    public void setup() {
        sslTestImpl = new SSLTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "tests implementation of SSL";
    }



    @Override
    @Test
    public void testConnectionLayer_SelfSignedCert_JavaSSL() {
        sslTestImpl.testConnectionLayer_SelfSignedCert_JavaSSL(getMemberNodeIterator(), "v1");
    }
    
    @Override
    @Test
    public void testConnectionLayer_SelfSignedCert_curl() {
        sslTestImpl.testConnectionLayer_SelfSignedCert_curl(getMemberNodeIterator(), "v1");
    }



    @Override
    @Test
    public void testConnectionLayer_ExpiredCertificate() {
        sslTestImpl.testConnectionLayer_ExpiredCertificate(getMemberNodeIterator(), "v1");
    }



    @Override
    @Test
    public void testConnectionLayer_dataoneCAtrusted() {
        sslTestImpl.testConnectionLayer_dataoneCAtrusted(getMemberNodeIterator(), "v1");
    }

}
