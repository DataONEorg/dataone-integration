package org.dataone.integration.it.testDefinitions;


/**
 * test definitions for the method systemMetadataChanged
 * 
 * This method changed APIs between V1 and V2, so will have stand-alone test definitions
 * @author rnahf
 *
 */
public interface SystemMetadataChangedMethodTestDefnitions {

    /**
     * Test the call from CN to MN.  The MN is supposed to reply before scheduling
     * it's own call to the CN.  MNs should return 'true' (no excpetion) if the 
     * object is on their node.  Otherwise, an InvalidRequest may be thrown, but
     * no guarantees.
     * 
     * This test poses as CNs from 3 different environments - only one should not
     * return a NotAuthorized
     * 
     */
    public void testSystemMetadataChanged();
    
//    public void testSystemMetadataChanged_EarlierDate();
    
    /**
     * This test tries to have a non-CN subject call the method.  should fail.
     */
    public void testSystemMetadataChanged_authenticatedITKuser();
    
//  @Ignore("do not know how to test in stand-alone mode.")
//  @Test
//    public void testSystemMetadataChanged_withCreate();
}
