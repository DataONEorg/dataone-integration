package org.dataone.integration.webTest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
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
		System.out.println("args from main:");
		for(String s: args) {
			System.out.println("    " + s);
		}
		System.out.println();
		
		// using apache argument parser
		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("mNodeUrl").hasArg()
				.withDescription("url for the member node to be tests").create("mNodeUrl"));
		options.addOption(OptionBuilder.withArgName("file").hasArg()
				.withDescription("path to a nodeList.xml file").create("nodeListFile"));
		options.addOption(OptionBuilder.withArgName("d1 env").hasArg()
				.withDescription("one of dev|test|stage|prod referring to " +
				"which set of node in the nodeList.xml file to use").create("nodeListEnv"));
		
		Parser p = new GnuParser();
		CommandLine cl = null;
		try {
			cl = p.parse(options,args);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("num options" + cl.getOptions().length);
		for (Option o: cl.getOptions()) {
			System.out.println(o.getOpt() + " = " + o.getValue());
		}
		
		String mNodeUrl = null;
		String nodeListFile = null;
		String nodeListEnv = null;
		
		if (cl.hasOption("mNodeUrl")) {
			 System.setProperty("mNodeUrl", cl.getOptionValue("mNodeUrl"));
		}
		if (cl.hasOption("nodeListFile")) {
			System.setProperty("nodeListFile", cl.getOptionValue("nodeListFile"));
		}
		if (cl.hasOption("nodeListEnv")) {
			System.setProperty("nodeListEnv", cl.getOptionValue("nodeListEnv"));
		}
		JUnitCore.main("org.dataone.integration.SystemPropertyTest");
	}
}

