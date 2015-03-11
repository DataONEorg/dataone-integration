package org.dataone.integration.it.apiTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Runs all MN v1 API tests.
 * Set the context.mn.baseurl variable before running (ex: https://mn-demo-6.test.dataone.org/knb/d1/mn).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    CNCoreV2IT.class,
    CNReadV2IT.class, 
    CNAuthorizationV2IT.class, 
    CNIdentityV2IT.class,
    CNReplicationV2IT.class, 
    CNRegisterV2IT.class, 
    CNAuthenticationV1V2IT.class, 
    })
public class CNv2TestSuite {
}