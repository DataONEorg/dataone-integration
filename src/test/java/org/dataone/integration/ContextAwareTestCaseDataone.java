package org.dataone.integration;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

/**
 * DO NOT ADD tests to this class directly!  Subclass, subclass, subclass!!! :-) 
 * 
 * This class is intended as a base class that implements the standard IntegrationTestContextParameters
 * 
 * Mostly having to do with setting up the testing context.  This class sets default values for
 * some methods.  Subclasses can override these default values by overriding the getter methods.
 * 
 * @author rnahf
 *
 */
public class ContextAwareTestCaseDataone implements IntegrationTestContextParameters {

	// TODO: make sure files stored here are not deployed in the test-jar!! (security)
	private final static String CONTEXT_FILE_PATH = "/d1_testDocs/IT_contexts";
	protected static boolean debug = true;

	
	// variables to hold system properties / parameters passed in
	protected static String testContext = DEFAULT_CONTEXT;
	protected static String testSettingsUri = buildTestSettingsUri(testContext);
	protected static String cnBaseUrl;
	protected static String mnBaseUrl;
	protected static String nodelistUri;

	// object to hold properties in settings file
	private static Properties props;
	
	
	@Rule 
    public ErrorCollector errorCollector = new ErrorCollector();
	
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
			debug("Setting context....");
			debug("initial context: " + getTestContext());
			debug("initial settings file = " + getTestSettingsUri());

			// class default values (or subclass overridden values) 
			// provided by the getter methods.
			
			// now override with any set system properties
			testSettingsUri = System.getProperty(PARAM_TEST_SETTINGS_URI, getTestSettingsUri());
			debug("**settings file after checking sys props: " + getTestSettingsUri());
			// read the setting file to get property values
			// some of these (user accounts) are only found in the settings file
			props = new Properties();
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
			testContext = props.getProperty(PARAM_TEST_CONTEXT,getTestContext());
			cnBaseUrl = props.getProperty(PARAM_CN_URL,getCnBaseURL());
			mnBaseUrl = props.getProperty(PARAM_MN_URL,getMnBaseURL());
			nodelistUri = props.getProperty(PARAM_NODELIST_URI,getNodeListURI());
		
			// finally get any other defined system properties that get passed in at runtime
			debug("system prop context: " + System.getProperty(PARAM_TEST_CONTEXT));
			debug("settings file context: " + getTestContext());
			testContext = System.getProperty(PARAM_TEST_CONTEXT,getTestContext());
			debug("final context: " + testContext);
			cnBaseUrl = System.getProperty(PARAM_CN_URL,getCnBaseURL());
			mnBaseUrl = System.getProperty(PARAM_MN_URL,getMnBaseURL());
			nodelistUri = System.getProperty(PARAM_NODELIST_URI,getNodeListURI());
		
			debug("context: " + testContext);
			
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
		if (getDebug()) {
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
		return testContext;
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
	protected boolean getDebug() {
		return debug;
	}
	
    protected void printTestHeader(String methodName)
    {
        System.out.println("\n***************** running test for " + methodName + " *****************");
    }
    
    protected void checkEquals(final String message, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(message, s1, is(s2));
                return null;
            }
        });
    }
    
    protected void checkTrue(final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(message, true, is(b));
                return null;
            }
        });
    }
	
    protected void checkFalse(final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat(message, false, is(b));
                return null;
            }
        });
    }
	
}
