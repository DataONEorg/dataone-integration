package org.dataone.integration.webTest;


import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
