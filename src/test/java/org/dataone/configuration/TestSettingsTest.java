package org.dataone.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SystemConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;


public class TestSettingsTest {
	
	/**
	 * Need this one to clean up set system properties that will interfere 
	 * with how the tests should behave.
	 */
	@Before
	public void clearSetSystemProperties() {
		System.clearProperty(TestSettings.CONTEXT_LABEL);
		System.clearProperty(TestSettings.CONTEXT_MN_URL);
		System.clearProperty(TestSettings.CONTEXT_NODELIST_URI);
		System.clearProperty(TestSettings.CONTEXT_OVERRIDE_URI);
	}
	
	
	/**
	 * Test that these unit tests can get to a fresh state using the @Before method
	 * and Settings.getResetConfiguration().
	 */
	@Test
	public void testResetConfiguration()  {

		System.setProperty(TestSettings.CONTEXT_NODELIST_URI, "foo");
		String propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_NODELIST_URI);
		assertEquals("foo", propValue);
		
		System.clearProperty(TestSettings.CONTEXT_NODELIST_URI);
		System.setProperty(TestSettings.CONTEXT_MN_URL, "bar");
		
		propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_NODELIST_URI);
		assertNull(propValue);
		propValue = Settings.getConfiguration().getString(TestSettings.CONTEXT_MN_URL);
		assertEquals("bar",propValue);
				
	}
	
	
	@Test
	public void testLoadingCommonConfigurations() 
	{		
		String propValue = Settings.getResetConfiguration().getString("D1Client.CN_URL");
		assertEquals("", propValue);
		
		propValue = Settings.getConfiguration().getString("dataone.it.propertyFileName");
		assertEquals("defaultCommonTestProperties", propValue);
	}
	
	
	/**
	 * Test lookup from default properties file
	 */
	@Test
	public void testLoadingContextSpecificByLabel() 
	{
		System.setProperty(TestSettings.CONTEXT_LABEL, "DEV");
		
		String propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_LABEL);
		assertEquals("DEV", propValue);	
	}

	
	@Test
	public void testLoadingContextSpecificByMN_URL() 
	{
		String mnUrl = "http://cn-dev.dataone.org/knb/d1/mn";
		System.setProperty(TestSettings.CONTEXT_MN_URL, mnUrl);
		
		String propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_LABEL);
		assertEquals("SINGLE_MN", propValue);
		
		propValue = Settings.getConfiguration().getString(TestSettings.CONTEXT_MN_URL);
		assertEquals(mnUrl, propValue);
	}

	
	@Test
	public void testLoadingContextSpecificByNodelistUri() 
	{
		String nodelist = "http://cn-dev.dataone.org/cn/node";
		System.setProperty(TestSettings.CONTEXT_NODELIST_URI, nodelist);
		
		String propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_LABEL);
		assertEquals("CUSTOM_NODELIST", propValue);
		
		propValue = Settings.getConfiguration().getString(TestSettings.CONTEXT_NODELIST_URI);
		assertEquals(nodelist, propValue);
	}
	
	
	/**
	 * Include an optional properties file using System property:
	 */
	@Test
	public void testOptionalFile_overrides() {
		System.setProperty(TestSettings.CONTEXT_OVERRIDE_URI, "org/dataone/configuration/overriding.properties");
		String propValue = Settings.getResetConfiguration().getString("dataone.it.propertyFileName");
		assertEquals("superSpecialOverridingProperties",propValue);
	}
	
	
	@Ignore("need different test - not throwing exception by design")
	@Test
	public void testBadParameterCombinationHandling() {
		
		System.setProperty(TestSettings.CONTEXT_LABEL, "foo");
		System.setProperty(TestSettings.CONTEXT_MN_URL, "bar");
		try {
			Settings.getConfiguration();
			fail("should not get here, should throw exception");
		} catch (Exception e) {
			assertEquals(ConfigurationException.class,e);
		}
	}
	
}
