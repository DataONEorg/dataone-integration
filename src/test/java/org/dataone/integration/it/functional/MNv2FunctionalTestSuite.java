package org.dataone.integration.it.functional;

import org.dataone.integration.it.apiTests.MNUpdateSystemMetadataIT;
import org.dataone.integration.it.functional.v2.MNPackageFunctionalIT;
import org.dataone.integration.it.functional.v2.MNViewFunctionalIT;
import org.dataone.integration.it.testImplementations.SidMNTestImplementations;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ 
    
    SidMNTestImplementations.class,                     // 12  Tests
    MNPackageFunctionalIT.class,                        //   Tests
    MNViewFunctionalIT.class,                           //   Tests
    MNUpdateSystemMetadataIT.class     //   Tests

    // rest handled in CNv2FunctionalTestSuite
    })
public class MNv2FunctionalTestSuite {
    
}