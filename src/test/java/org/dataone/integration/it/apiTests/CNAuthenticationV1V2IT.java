package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.SSLTestDefinitions;
import org.dataone.integration.it.testImplementations.SSLTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class CNAuthenticationV1V2IT extends ContextAwareTestCaseDataone 
implements  SSLTestDefinitions {

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
    public void testConnectionLayer_SelfSignedCert() {
        sslTestImpl.testConnectionLayer_SelfSignedCert(getCoordinatingNodeIterator(), "v1");
    }



    @Override
    @Test
    public void testConnectionLayer_ExpiredCertificate() {
        sslTestImpl.testConnectionLayer_ExpiredCertificate(getCoordinatingNodeIterator(), "v1");
    }



    @Override
    @Test
    public void testConnectionLayer_dataoneCAtrusted() {
        sslTestImpl.testConnectionLayer_dataoneCAtrusted(getCoordinatingNodeIterator(), "v1");
    }

}
