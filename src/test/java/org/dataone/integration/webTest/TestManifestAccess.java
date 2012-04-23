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

package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.dataone.configuration.Settings;
import org.junit.Ignore;
import org.junit.Test;


public class TestManifestAccess { 
	
	
	@Test
	public void testTrue() 
	{
		assertTrue(true);
	}

	@Ignore("need to generalize how to locate the deployed lib dir")
	@Test
	public void testManifestAsConfiguration() throws ConfigurationException, IOException {
		CompositeConfiguration cc = (CompositeConfiguration) Settings.getConfiguration();
		URL url = this.getClass().getResource("/d1_testdocs/EXAMPLE_MANIFEST.MF");
//		URL url = this.getClass().getClassLoader().getResource("/META-INF/MANIFEST.MF");
		cc.addConfiguration(new PropertiesConfiguration(url));

		System.out.println("manifest property: " + cc.getString("SCM-buildNumber"));
		
		System.out.println("manifest property: " + cc.getString("SCM-Revision"));
		String libDirString = "/Users/rnahf/software/workspace/d1_integration/target/d1_integration-1.0.0-SNAPSHOT/WEB-INF/lib";
		File lib = new File(libDirString);
		File[] files = lib.listFiles(new FileFilter()
			{ 
				public boolean accept(File file){
					if (file.getName().startsWith("d1_")) {
						return true;
					}
					return false;
				}
			}
		);
		for (File f : files) {
			JarFile jf = new JarFile(f);
			System.out.println(jf.getName());
			Manifest mf = jf.getManifest();
			Attributes attrs = mf.getMainAttributes();
			System.out.println("built-by = " + attrs.getValue("Built-By"));
		}
	}
}
