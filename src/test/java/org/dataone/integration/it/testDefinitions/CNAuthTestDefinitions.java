package org.dataone.integration.it.testDefinitions;


/**
 * test definitions for the methods:
 * setAccessPolicy
 * setRightsHolder
 * 
 * @author rnahf
 */
public interface CNAuthTestDefinitions {

    /**
     * Creates an object with no AccessPolicy.  changes ownership to different
     * subject, then tests original rightsholder can't access/change, and the 
     * new one can.
     */
    public void testSetRightsHolder();

    
    /**
     * Tests the dataONE service API setAccessPolicy method, and requires a
     * designated object that the "testRightsHolder" can change.
     * Outline for the test:  find the object, clear the AP, try isAuthorized()
     * with non-owner (should fail), as owner setAccessPolicy(), try isAuthorized()
     * with non-owner client who should now have access.
     * 
     * on the first object returned from the Tier1 listObjects() method.  
     * Anything other than the boolean true is considered a test failure.
     */
    public void testSetAccessPolicy();
}
