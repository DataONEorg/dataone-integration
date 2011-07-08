package org.dataone.configuration;

import java.net.URL;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestSettings {
	private static Log log = LogFactory.getLog(TestSettings.class);
	
    public static String STD_CONFIG_PATH      = Settings.STD_CONFIG_PATH;   
	public static String CONTEXT_OVERRIDE_URI = "context.override.settings.uri";
	public static String CONTEXT_LABEL        = "context.label";
	public static String CONTEXT_NODELIST_URI = "context.nodelist.uri";
	public static String CONTEXT_MN_URL       = "context.mn.baseurl";
	

	
	public static Configuration getConfiguration() throws ConfigurationException {
		
		CompositeConfiguration configuration = new CompositeConfiguration();

		// look for certain context properties in System and Environment properties



		
		

		
		// ----- get and process the optional properties file ------- //
		String propsFile = System.getProperty(CONTEXT_OVERRIDE_URI, System.getenv(CONTEXT_OVERRIDE_URI));
		
		if (propsFile != null && propsFile.trim().length() > 0) {
			log.info("overriding properties file detected: " + propsFile);
    	
			URL url = TestSettings.class.getClassLoader().getResource(propsFile);
		
			DefaultConfigurationBuilder factory = new DefaultConfigurationBuilder();
			factory.setURL(url);
			Configuration config = null;
			try {
				config = factory.getConfiguration();
				configuration.addConfiguration(config);
//				configuration.addConfiguration(new PropertiesConfiguration(url));
			} catch (ConfigurationException e) {
				log.error("Problem loading optional overriding configurationat: '" + 
						url + "':: " + e.getMessage());
				throw e;
			}
		} 
		
		
		// TODO: find a better way to set the default context - we don't want to put config files
		// in d1_common_java (too many rebuilds), but then we shouldn't really be setting the 
		// default context here - it needs to be managed in the same package that holds
		// the context files.  Yes?
		
		// now load designated context-specific properties file
		// the context passed in from the system properties (or environment) take precedence
		String context =   System.getProperty(CONTEXT_LABEL, System.getenv(CONTEXT_LABEL));
		if (context == null) {
			// look in overriding configuration
			context = configuration.getString(CONTEXT_LABEL);
			if (context == null) {
				String mnBaseUrl = System.getProperty(CONTEXT_MN_URL,System.getenv(CONTEXT_MN_URL));
				String nodelistFile = System.getProperty(CONTEXT_NODELIST_URI,System.getenv(CONTEXT_NODELIST_URI));
				if (mnBaseUrl != null) {
					context = "SINGLE_MN";
				} else {
					if (nodelistFile != null) {
						context = "CUSTOM_NODELIST";
					}
				}
			}
				context = "LOCAL";
		}
		URL url = Settings.class.getClassLoader().getResource(STD_CONFIG_PATH + "/default." + context + ".test.properties");
		if (url != null ) {
			try {
				configuration.addConfiguration(new PropertiesConfiguration(url));
			} catch (ConfigurationException e) {
				System.out.println("configuration exception on optional context: " + url + ": " + e.getMessage());
				log.error("ConfigurationException encountered while loading configuration: " + url, e);
			}
		}

		return configuration;
		
	}
}
