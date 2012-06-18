/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.configuration;

import java.net.MalformedURLException;
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
	public static String CONTEXT_CN_URL       = "context.cn.baseurl";
	public static String REFERENCE_CN_URL       = "reference.cn.baseurl";
	public static String REFERENCE_CONTEXT_LABEL       = "reference.context.label";
	

	/**
	 * set up the test configurations, following some simple logic:
	 * 1. if CONTEXT_OVERRIDE_URI supplied as parameter (via system or environment)
	 *       load that file first (will override any other properties loaded later)
	 * 2. if one of CONTEXT_LABEL, CONTEXT_NODELIST_URI or CONTEXT_MN_URL, CONTEXT_CN_URL
	 *       load default test properties files supplied with the code
	 *   (resources/org/dataone/configuration/context.<CONTEXT_LABEL_VALUE>.test.properties)
	 * 3. load the default.common.properties file
	 * 
	 * @return the Configuration object
	 * 
	 * @throws ConfigurationException - when anything goes wrong, so as not to 
	 *              revert to a production setting when mistakes in test settings.
	 */
	public static Configuration getConfiguration() throws ConfigurationException {
		
		CompositeConfiguration configuration = new CompositeConfiguration();
		
		// ----- get and process the optional properties file ------- //
		configuration.setProperty(CONTEXT_OVERRIDE_URI,		
				System.getProperty(CONTEXT_OVERRIDE_URI, System.getenv(CONTEXT_OVERRIDE_URI)));
		String propsFile = configuration.getString(CONTEXT_OVERRIDE_URI);
		
		if (propsFile != null) {
			log.info("overriding properties file detected: " + propsFile);    	
			loadConfigurationFile(configuration, propsFile, false);
		}
			
		// consolidate sys/env properties into the configuration
		// these override any in the override file loaded from above
		configuration.setProperty(CONTEXT_LABEL, 
				System.getProperty(CONTEXT_LABEL, System.getenv(CONTEXT_LABEL)));
		configuration.setProperty(CONTEXT_MN_URL,
				System.getProperty(CONTEXT_MN_URL,System.getenv(CONTEXT_MN_URL)));
		configuration.setProperty(CONTEXT_CN_URL,
				System.getProperty(CONTEXT_CN_URL,System.getenv(CONTEXT_CN_URL)));
		configuration.setProperty(CONTEXT_NODELIST_URI,
				System.getProperty(CONTEXT_NODELIST_URI,System.getenv(CONTEXT_NODELIST_URI)));
		configuration.setProperty(REFERENCE_CN_URL,
				System.getProperty(REFERENCE_CN_URL,System.getenv(REFERENCE_CN_URL)));
		configuration.setProperty(REFERENCE_CONTEXT_LABEL,
				System.getProperty(REFERENCE_CONTEXT_LABEL,System.getenv(REFERENCE_CONTEXT_LABEL)));
				
		
		
		// now load designated context-specific properties file
		// the context passed in from the system properties (or environment) take precedence
		String contextLabel = determineContext(configuration);
		if (contextLabel != null) {
			String fileName = STD_CONFIG_PATH + "/context." + contextLabel + ".test.properties";
			log.info("attempting to load context-specific configuration file (context " + 
					contextLabel + "): " + fileName);

			if (contextLabel.equals("SINGLE_MN") || 
					contextLabel.equals("SINGLE_CN") ||
					contextLabel.equals("CUSTOM_NODELIST")) {
				try {
					loadConfigurationFile(configuration, fileName,true);
				} catch (ConfigurationException ce)  {
					// let it fail to load and keep going, 
					// because there's not always one there for these labels...
				}
			} else if (contextLabel != null) {
				log.info("attempting to load context-specific configuration file (context " + 
						contextLabel + "): " + fileName);
				loadConfigurationFile(configuration, fileName,false);
			}
		}
		
		// ------ finally load the common configurations  ----- //
		String fileName = STD_CONFIG_PATH + "/default.common.test.properties";
		loadConfigurationFile(configuration,fileName,false);
		
		return configuration;
	}

	
	/**
	 * This method is used to extract the reference CN url from either of the two
	 * reference properties.  These properties do not change the CN url that's
	 * loaded into the Settings configuration. 
	 * @param referenceContextLabel
	 * @return
	 * @throws ConfigurationException
	 */
	public static String getReferenceCnBaseUrl(String referenceContextLabel) 
	throws ConfigurationException 
	{
		String fileName = STD_CONFIG_PATH + "/context." + referenceContextLabel + ".test.properties";
		log.info("attempting to load context-specific configuration file (context " + 
				referenceContextLabel + "): " + fileName);
		
		CompositeConfiguration configuration = new CompositeConfiguration();
		loadConfigurationFile(configuration, fileName,false);
		
		return configuration.getString("D1Client.CN_URL");
	}
	
	
	private static String determineContext(Configuration config) throws ConfigurationException
	{		
		String label = config.getString(CONTEXT_LABEL);
		String mnBaseUrl = config.getString(CONTEXT_MN_URL);
		String cnBaseUrl = config.getString(CONTEXT_CN_URL);
		String nodelistFile = config.getString(CONTEXT_NODELIST_URI);

		int count = 0;
		if (label != null) count++;
		if (mnBaseUrl != null) count++;
		if (cnBaseUrl != null) count++;
		if (nodelistFile != null) count++;
		if (count > 1) {
			throw new ConfigurationException("Can only set one of properties: " + 
					CONTEXT_LABEL + ", " + CONTEXT_MN_URL + ", " + CONTEXT_CN_URL +
					", or " + CONTEXT_NODELIST_URI + 
					".  Number received: " + count);
		}
		
		// CONTEXT_MN_URL overrides CONTEXT_LABEL
		if (mnBaseUrl != null) {
			label = "SINGLE_MN";
		} 
		// CONTEXT_CN_URL overrides CONTEXT_LABEL
		if (cnBaseUrl != null) {
			label = "SINGLE_CN";
		}
		if (nodelistFile != null) {
			label = "CUSTOM_NODELIST";
		}
		config.setProperty(CONTEXT_LABEL, label);
		// can be null...
		return label;
	}
	
	
	private static void loadConfigurationFile(CompositeConfiguration configuration, String filename, boolean warnOnly) 
	throws ConfigurationException  
	{
		URL url = null;
		if (filename.startsWith("file:")) {
			try {
				url = new URL(filename);
			} catch (MalformedURLException e) {
			}
		} else {
			url = TestSettings.class.getClassLoader().getResource(filename);
		}
		log.info("resource: " + filename);
		log.info("configuration url: " + url);
		
		try {
			configuration.addConfiguration(new PropertiesConfiguration(url));	
		} catch (ConfigurationException e) {
			String msg = "Problem loading testing configuration at: '" + 
				url + "':: " + e.getMessage();
			
			if (warnOnly)
				log.warn(msg);
			else 
				log.error(msg);
			
			throw e;
		}
	}
}
