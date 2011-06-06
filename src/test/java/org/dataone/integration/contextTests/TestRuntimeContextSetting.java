/**
 * 
 */
package org.dataone.integration.contextTests;


import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/**
 * Need to be able to set parameters via system properties.  Will use TestRunnerHttpServlet
 * @author rnahf
 *
 */
public class TestRuntimeContextSetting extends ContextAwareTestCaseDataone {
	private static final String MY_CONTEXT = "MY_CONTEXT"; 
	
	protected boolean getDebug() {
		return true;
	}
	
	@Ignore("not working yet - System Properties not getting picked up, probably due to everything in same process")	
	@Test
	public void testDefinedParametersExceptSettingsFile() {
		System.setProperty(PARAM_TEST_CONTEXT, "someContext");
		System.setProperty(PARAM_MN_URL, "someMN");
		System.setProperty(PARAM_CN_URL, "someCN");
		System.setProperty(PARAM_NODELIST_URI, "someNodelist");
//		System.setProperty(PARAM_TEST_SETTINGS_URI, "LOCAL");

		// use JUnitcore to run a test case.
		JUnitCore junit = new JUnitCore();
		Result result = junit.run(org.dataone.integration.contextTests.ContextTester.class);
		//  should pass all tests
		debug("failures: " + result.getFailureCount());
		checkTrue("should pass all tests (failure count should be zero)",result.getFailureCount() == 0);
	}
	
	@Ignore("Can't test without a real settings file to grab from somewhere")
	@Test
	public void testSettingsFileParameter() {
		
	}
}
