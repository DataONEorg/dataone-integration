package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Logic for setting up the nodes to be tested
 * use the values defined in the test class, unless superceded
 * by those passed in as SystemProperties
 * 
 * @author rnahf
 */
public class MockITSystemProperty {
	
	// static variables for setting up the nodes to be tested
	private static String mNodeUrl; 
	private static String nodeListFile; 
	private static String nodeListEnv; 
	private static String testSysProp;
	

	@Before
	public void setUp() throws Exception 
	{
		mNodeUrl = "[default_from_test_class]: http://localhost/knb/d1";
		nodeListFile = "[default_from_test_class]";
		nodeListEnv = "[default_from_test_class]: staging";

		// override parameter values with SystemProperties if present
		mNodeUrl = System.getProperty("mNodeUrl", mNodeUrl);
		nodeListFile = System.getProperty("nodeListFile", nodeListFile);
		nodeListEnv = System.getProperty("nodeListEnv", nodeListEnv);
		testSysProp = System.getProperty("testSysProp", testSysProp);
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
	
	
	@Test
	public void testSystemProperty()
	{
		System.out.println("testSystemProperty(): mNodeUrl = " + mNodeUrl);
		System.out.println("testSystemProperty(): nodeListFile = " + nodeListFile);
		System.out.println("testSystemProperty(): nodeListEnv = " + nodeListEnv);
		System.out.println("testSystemProperty(): testSysProp = " + testSysProp);
		System.out.println();
	}
}
