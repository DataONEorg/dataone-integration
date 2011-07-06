package org.dataone.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Test to make sure this package can access the common resources found in
 * the d1_test_resources package.  
 * 
 * @author rnahf
 */
public class CommonResourcesTest {

	@Test
	public final void testTest()
	{
		assertTrue("one".equals("one"));
	}
	/*
	 * this should work even without explicit dependency on d1_test_resources
	 * as it is inherited via d1_common_java dependency via d1_libclient_java dependency
	 */
	@Test
	public final void testCommonResourcesAvailability() throws IOException
	{
		String resource = "/D1shared/selfTest/simpleDummyResource.txt";
		InputStream is = this.getClass().getResourceAsStream(resource);
		if (is == null)
			fail("could not find resource in d1_test_resouces package: " + resource);
	}
}