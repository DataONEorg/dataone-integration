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

/**
 * @author rnahf
 *
 */
public class ContextTester extends ContextAwareTestCaseDataone {
	
	protected boolean getDebug() {
		return true;
	}
	
	@Test
	public void testDefaultTestContext() {
		debug("context: "+ getTestContext());
		checkTrue("testing that context is set by system properties",getTestContext().startsWith("some"));
	}
	
	@Ignore("can't test with other tests")
	@Test
	public void  testTestSettingsUri() {
		debug("testSettingsUri: "+ getTestSettingsUri());
		checkTrue("testing that testSettingsURI is set by system properties",getTestSettingsUri().startsWith("some"));
	}
	
	@Test
	public void testDefaultCnBaseURL() {
		debug("CN URL: "+ getCnBaseURL());
		checkTrue("testing that cn baseurl is set by system properties",getCnBaseURL().startsWith("some"));
	}
	
	@Test
	public void testDefaultMnBaseURL() {
		debug("MN URL: "+ getMnBaseURL());
		checkTrue("testing that mn baseurl is set by system properties",getMnBaseURL().startsWith("some"));
	}
	
	@Test
	public void testDefaultNodeListURI() {
		debug("nodelist: "+ getNodeListURI());
		checkTrue("testing that nodelist URI is set by system properties",getNodeListURI().startsWith("some"));
	}
}
