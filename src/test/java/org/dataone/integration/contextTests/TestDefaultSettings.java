/**
 * 
 */
package org.dataone.integration.contextTests;


import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author rnahf
 *
 */
public class TestDefaultSettings extends ContextAwareTestCaseDataone {
	
	@Test
	public void testDefaultTestContext() {
		checkEquals("default context should equal 'LOCAL'",getTestContext(),DEFAULT_CONTEXT);
	}
	
	@Test
	public void  testDefaultTestSettingsUri() {
		String defaultUri = this.buildTestSettingsUri(DEFAULT_CONTEXT);
		checkEquals("testing default testSettingsURI",getTestSettingsUri(),defaultUri);
	}
	
	@Test
	public void testDefaultCnBaseURL() {
		checkTrue("testing default cn baseurl is found in settings file",getCnBaseURL() != null);
	}
	
	@Test
	public void testDefaultMnBaseURL() {
		checkTrue("testing default mn baseurl is found in settings file",getMnBaseURL() != null);
	}
	
	@Test
	public void testDefaultNodeListURI() {
		checkTrue("testing default nodelist URI is found in settings file",getNodeListURI() != null);
	}
}
