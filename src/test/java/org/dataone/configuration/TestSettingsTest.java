package org.dataone.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;


public class TestSettingsTest {
	
	/**
	 * Need this one to clean up set system properties that will interfere 
	 * with how the tests should behave.
	 */
	@Before
	public void setUp() {
		clearSetSystemProperties();
	}
	
	public void clearSetSystemProperties() {
		System.clearProperty(TestSettings.CONTEXT_LABEL);
		System.clearProperty(TestSettings.CONTEXT_MN_URL);
		System.clearProperty(TestSettings.CONTEXT_CN_URL);
		System.clearProperty(TestSettings.CONTEXT_NODELIST_URI);
		System.clearProperty(TestSettings.CONTEXT_OVERRIDE_URI);
	}
	
	@After
	public void cleanUp() {
		clearSetSystemProperties();
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
	public void testLoadingContextSpecificByCN_URL() 
	{
		String cnUrl = "http://cn-dev.dataone.org/cn";
		System.setProperty(TestSettings.CONTEXT_CN_URL, cnUrl);
		
		String propValue = Settings.getResetConfiguration().getString(TestSettings.CONTEXT_LABEL);
		assertEquals("SINGLE_CN", propValue);
		
		propValue = Settings.getConfiguration().getString(TestSettings.CONTEXT_CN_URL);
		assertEquals(cnUrl, propValue);
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
	
	@Ignore("haven't universalized this test yet")
	@Test
	public void testOptionalFile_overrides_fullPath() {
		
		// copy file to /tmp/
		Date d = new Date();
		File tmpDir = new File(Constants.TEMP_DIR);
		File outputFile = new File(tmpDir, "propFile." + d.getTime() + ".properties");
		String afp = outputFile.getAbsolutePath();
		InputStream is = TestSettingsTest.class.getClassLoader().getResourceAsStream("org/dataone/configuration/overriding.properties");
		
		String filePath = "file:/Users/rnahf/projects/d1_integration/target/test-classes/org/dataone/configuration/overriding.properties";
		System.setProperty(TestSettings.CONTEXT_OVERRIDE_URI, filePath);
		String propValue = Settings.getResetConfiguration().getString("dataone.it.propertyFileName");
		assertEquals("superSpecialOverridingProperties",propValue);
	}
	
	@Test
	public void testBadParameterCombinationHandling() {
		
		System.setProperty(TestSettings.CONTEXT_LABEL, "foo");
		System.setProperty(TestSettings.CONTEXT_MN_URL, "bar");
		String propValue = Settings.getResetConfiguration().getString("dataone.it.propertyFileName");
		assertTrue("a common property was non loaded", propValue == null);
	}

	
	@Test
	public void testBadFilenameExceptionHandling() {
		System.setProperty(TestSettings.CONTEXT_OVERRIDE_URI, "blippity blap");
		String propValue = Settings.getResetConfiguration().getString("dataone.it.propertyFileName");
		assertTrue("bad url causes no properties to be loaded", propValue == null);
	}
		
	
	/**
	 * this test should fail to load any test configurations into the configuration,
	 * and also hide the value of D1Client.CN_URL (that contains the production value) 
	 */
	@Test
	public void testContextSafetyOnError() {
		System.setProperty(TestSettings.CONTEXT_OVERRIDE_URI, "blippity blap");
		// get a value from the default-common settings
		String propValue = Settings.getResetConfiguration().getString("dataone.it.propertyFileName");
		
		String prodCNurl = Settings.getResetConfiguration().getString("D1Client.CN_URL");		
		assertNull("D1Client.CN_URL value should be null",prodCNurl);
	}	
	
}
