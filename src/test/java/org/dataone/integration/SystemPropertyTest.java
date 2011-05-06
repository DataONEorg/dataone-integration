package org.dataone.integration;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.dataone.service.types.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;


/**
 * Logic for setting up the nodes to be tested
 * 1. nodeUrl from systemProperties
 * 2. nodeUrlFile from systemProperties
 * 3. nodeUrl from file
 * 4. nodeUrl from CN
 * 5. nodeUrl from provided filepath
 * 
 * @author rnahf
 */
public class SystemPropertyTest {
	
	// static variables for setting up the nodes to be tested
	private static String prop; 
	
	private List<Node> nodeList = null;	
	private Hashtable nodeInfo = null;
	private static String currentUrl;
	//set this to false if you don't want to use the node list to get the urls for 
	//the test.  
	private static boolean useNodeList = false;
	
	
	
	@Before
	public void setUp() throws Exception 
	{
//		Enumeration p = System.getProperties().propertyNames();
//		while (p.hasMoreElements())
//			System.out.println("name: " + p.nextElement());

		
		
		prop = System.getProperty("TestApp:testProperty", "can't locate");
		System.out.println("test setup: testProperty value: " + prop);
	}

	@Test
	public void testSystemProperty()
	{
		System.out.println("unit test: testProperty value: " + prop);
	}
}
