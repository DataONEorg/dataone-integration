package org.dataone.integration.it.apiTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 * Runs all MN v1 API tests.
 * Set the context.mn.baseurl variable before running (ex: https://mn-demo-6.test.dataone.org/knb/d1/mn).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    CNCoreV1IT.class,
    CNReadV1IT.class, 
    CNAuthorizationV1IT.class, 
    CNIdentityV1IT.class,
    CNReplicationV1IT.class, 
    CNRegisterV1IT.class, 
    CNAuthenticationV1V2IT.class, 
    })
public class CNv1TestSuite {
}