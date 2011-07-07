package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;


public class MockITCase { 
	
	
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
