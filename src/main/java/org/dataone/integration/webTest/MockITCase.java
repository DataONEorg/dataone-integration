package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class MockITCase extends ContextAwareTestCaseDataone {
	
	@Override
	protected String getTestDescription() {
		return "a test class to exercise ability to report passes, fails, ignores, and exceptions";
	}

	
	@Test
	public void testTrue() 
	{
		assertTrue(true);
	}



	@Test
	public void testFalse()
	{
		assertTrue(1==2);
	}
	
	
	@Ignore
	@Test
	public void testIgnore() {
		;
	}

	@Test
	public void testException() {
		// should throw a numerical exception of some type
		float f = 12 / 0;
		f++;
	}
	

	
}
