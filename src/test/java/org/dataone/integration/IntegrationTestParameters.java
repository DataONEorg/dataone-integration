package org.dataone.integration;

/**
 * Defines the parameter names used by dataone integration to configure tests 
 * implemented by AbstractTestCaseDataone and subclasses.
 * Also used by the TestRunners contained in org.dataone.integration.webTest package
 * 
 * Definitions:
 * TEST_CONTEXT - the context name (LOCAL,DEV,STAGING,PROD) plus any ad-hoc contexts
 * The context name will point to a similarly named settings file to get test properties 
 * 
 * NODELIST_URI  - a URI, (filepath or url) that designates a set of nodes for the given tests
 * 
 * CN_URL - the base url of a coordinating node to be tested. used for testing a single node
 * MN_URL - the base url of a member node to be tested. used for testing a single node.
 * 
 */
public interface IntegrationTestParameters {
	
	/**
	 * the names of system properties that AbstractTestCaseDataone will fetch and take action on
	 * during the test setup phase.
	 * 
	 * Also used by the TestRunners contained in org.dataone.integration.webTest package
	 */
	
	public final static String PARAM_TEST_CONTEXT = "dataone.test.context";  // the string that signals the context to run against
	public final static String DEFAULT_CONTEXT = "LOCAL";  // the default value for PARAM_TEST_CONTEXT
	
	public final static String PARAM_CN_URL = "dataone.test.cn.url";    // the base url for the cn within the environment 
	public final static String PARAM_MN_URL = "dataone.test.mn.url";    // the base url for the MN being tested
	public final static String PARAM_NODELIST_URI = "dataone.test.nodelist.uri";  // a uri (file or url) of the nodelist to use to set the environment

	
	// not sure if this one will ever be used, but possibly by deployed testrunners needing to
	// specify the location of secure settings information
	public final static String PARAM_TEST_SETTINGS_URI = "dataone.test.settings.uri";
	
}
