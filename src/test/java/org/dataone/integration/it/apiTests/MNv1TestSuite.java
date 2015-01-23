package org.dataone.integration.it.apiTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs all MN v1 API tests.
 * Set the context.mn.baseurl variable before running (ex: https://mn-demo-6.test.dataone.org/knb/d1/mn).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    MNCoreV1IT.class,
    MNCoreV1SlowIT.class,
    MNReadV1IT.class, 
    MNAuthV1IT.class, 
    MNQueryV1IT.class,
    MNReplicationV1IT.class, 
    MNStorageV1IT.class })
public class MNv1TestSuite {
}