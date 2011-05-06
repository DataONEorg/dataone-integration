package org.dataone.integration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.junit.runner.JUnitCore;

public class SystemPropertyTestApp {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String urlVal = "unset";
		System.out.println("Hello from main");
		
		// using apache argument parser
		Options options = new Options();
		Parser p = new PosixParser();
		CommandLine cl = null;
		try {
			cl = p.parse(options,args);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		String mNodeUrl = null;
		String nodeListFile = null;
		String nodeListEnv = null;
		
		if (cl.hasOption("nodeUrl")) {
			 mNodeUrl = cl.getOptionValue("mNodeUrl");
		}
		if (cl.hasOption("nodeListFile")) {
			nodeListFile = cl.getOptionValue("nodeListFile");
		}
		if (cl.hasOption("nlEnvironment")) {
			nodeListEnv = cl.getOptionValue("nlEnvironment");
		}
		
		if (args.length > 0 && args[0].startsWith("url=")) {
			urlVal = args[0].substring(args[0].indexOf('=')+1); 
		}
		System.out.println("main: value after reading args: " + urlVal);
		System.setProperty("mNodeUrl", mNodeUrl);
		System.setProperty("nodeListFile", nodeListFile);
		JUnitCore.main("org.dataone.integration.SystemPropertyTest");
	}
}

