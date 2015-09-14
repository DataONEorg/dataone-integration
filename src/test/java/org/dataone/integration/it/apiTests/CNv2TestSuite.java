package org.dataone.integration.it.apiTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Runs all CN v2 API tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    CNCoreV2IT.class,
    CNReadV2IT.class, 
    CNAuthorizationV2IT.class, 
    CNIdentityV2IT.class,
    CNReplicationV2IT.class, 
    CNRegisterV2IT.class, 
    CNViewV2IT.class,
    CNAuthenticationV1V2IT.class, 
    })
public class CNv2TestSuite {
}