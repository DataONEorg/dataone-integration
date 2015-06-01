package org.dataone.integration.it.testDefinitions;

/**
 * Contains tests used to test all nodes for proper SSL Certificate trust
 * relationship set up.
 * 
 * (Used to be part of Tier2 tests)
 * @author rnahf
 *
 */
public interface SSLTestDefinitions {
    
    
    /**
     * Client behavior is funky for self-signed certs - for this client it gets
     * through as public, but other clients are rejected.
     * This test will test the downgraded permissions to make sure it only has
     * access to public data, not it's own.
     */
    public void testConnectionLayer_SelfSignedCert_JavaSSL();
    
    public void testConnectionLayer_SelfSignedCert_curl();
    
    public void testConnectionLayer_ExpiredCertificate();
    
    public void testConnectionLayer_dataoneCAtrusted();
    

}
