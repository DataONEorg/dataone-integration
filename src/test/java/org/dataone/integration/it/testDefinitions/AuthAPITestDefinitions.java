package org.dataone.integration.it.testDefinitions;

public interface AuthAPITestDefinitions {

    
    /**
     * A basic test Tests the dataONE service API isAuthorized() method, checking for Read 
     * permission on the first object returned from the Tier1 listObjects() method.  
     * Anything other than the boolean true is considered a test failure.
     */
    public void testIsAuthorized();
    
}
