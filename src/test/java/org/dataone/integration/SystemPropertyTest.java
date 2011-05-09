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
 * use the values defined in the test class, unless superceded
 * by those passed in as SystemProperties
 * 
 * @author rnahf
 */
public class SystemPropertyTest {
	
	// static variables for setting up the nodes to be tested
	private static String mNodeUrl; 
	private static String nodeListFile; 
	private static String nodeListEnv; 
	
	private List<Node> nodeList = null;	
	private Hashtable nodeInfo = null;
	private static String currentUrl;
	//set this to false if you don't want to use the node list to get the urls for 
	//the test.  
	private static boolean useNodeList = false;
	
	
	
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
	
	}

	@Test
	public void testSystemProperty()
	{
		System.out.println("testSystemProperty: mNodeUrl = " + mNodeUrl);
		System.out.println("testSystemProperty: nodeListFile = " + nodeListFile);
		System.out.println("testSystemProperty: nodeListEnv = " + nodeListEnv);
	}
}
