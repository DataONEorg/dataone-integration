package org.dataone.integration;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;

/**
 * Named to avoid processing by maven-surefire-plugin or maven-failsafe-plugin
 * This class contains common methods useful for both unit and integration tests
 * 
 * @author rnahf
 *
 */
public abstract class AbstractTestCaseDataone implements IntegrationTestParameters {

	// TODO: make sure files stored here are not deployed in the test-jar!! (security)
	private final static String CONTEXT_FILE_PATH = "/d1_testDocs/IT_contexts";
	protected static boolean debug;

	
	// variables to hold system properties / parameters passed in
	protected static String textContext = DEFAULT_CONTEXT;
	protected static String testSettingsUri = buildTestSettingsUri(textContext);
	protected static String cnBaseUrl;
	protected static String mnBaseUrl;
	protected static String nodelistUri;

	// object to hold properties in settings file
	private static Properties props;
	
	/**
	 * 
	 * @param contextName
	 * @return
	 */
	protected static String buildTestSettingsUri(String contextName) {
		return CONTEXT_FILE_PATH + "/default." + contextName + ".test.properties";
	}
	
	
	
	/**
	 * Can get parameters from 4 directions.  Listed in order of precedence
	 * 1) system properties
	 * 2) a settings file
	 * 3) class defaults
	 * 4) inherited base class defaults
	 * 
	 * * a parameter passed in on system properties will override the one
	 * in the settings file which overrides the class default value which 
	 * overrides base class defaults
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		// skip setUp steps if already run
		if (props == null) {
			System.out.println("context: " + getTestContext());
			System.out.println("settings file = " + getTestSettingsUri());

			// class default values (or subclass overridden values) 
			// provided by the getter methods.
			
			// now override with any set system properties
			testSettingsUri = System.getProperty(PARAM_TEST_SETTINGS_URI, getTestSettingsUri());
			
			// read the setting file to get property values
			// some of these (user accounts) are only found in the settings file
			Properties props = new Properties();
			try {
				File settings = new File(testSettingsUri);
				InputStream propertiesStream = null;
				if (settings.canRead()) {
					propertiesStream = new FileInputStream(settings);
				} else {
					propertiesStream = this.getClass().getResourceAsStream(testSettingsUri);
				} 
				props.load(propertiesStream);
			} catch (FileNotFoundException e) {
				System.out.println("FNF: Can't find settings file: " +  e.getMessage());
				throw e;
			} catch (IOException e) {
				System.out.println("IO Exception: Can't load contents of settings file: " + e.getMessage());
				throw e;
			}
			
			// now set interface-defined parameter variables
			// (the second parameter is the default if not found in the settings file)
			textContext = props.getProperty(PARAM_TEST_CONTEXT,getTestContext());
			cnBaseUrl = props.getProperty(PARAM_CN_URL,getCnBaseURL());
			mnBaseUrl = props.getProperty(PARAM_MN_URL,getMnBaseURL());
			nodelistUri = props.getProperty(PARAM_NODELIST_URI,getNodeListURI());
		}
		
		// have the settings, now to decide how to initiate the tests (which nodes, users to create)
		if (getTestContext() != null) {
			//set the cn based on the environment found in the nodelist.
		}
		
		
	}
	
    /**
     * Return one of the properties based on its key value.
     * @param key the String name of a property key
     * @return the value of that property
     */
    public String getSetting(String key) {
        if (key.equals(PARAM_TEST_CONTEXT)) {
        	return getTestContext();
        }
        if (key.equals(PARAM_CN_URL)) {
        	return getCnBaseURL();
        }
        if (key.equals(PARAM_MN_URL)) {
        	return getMnBaseURL();
        }
        if (key.equals(PARAM_NODELIST_URI)) {
        	return getNodeListURI();
        }
        return props.getProperty(key);
        
    }
	
	
	public void debug(String message) {
		if (debug) {
			System.out.println(message);
		}
	}
	/**
	 * returns the uri (file or url) where the settings file to 
	 * be used is located.
	 * @return
	 */
	protected String getTestSettingsUri() {
		return testSettingsUri;
	}
	protected  String getTestContext() {
		return textContext;
	}
	protected  String getCnBaseURL() {
		return cnBaseUrl;
	}
	protected  String getMnBaseURL() {
		return mnBaseUrl;
	}
	protected  String getNodeListURI() {
		return nodelistUri;
	}

}
