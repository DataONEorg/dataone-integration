package org.dataone.integration.webTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;


import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestRunnerHttpServlet extends HttpServlet
{
//	private static final String TESTS_DIR = "/WEB-INF/tests";
	private static final String RESULTS_FILE_TEMPLATE = "/results.html";
	
	private boolean debug = true;
	
	private void debug(String string) {
		if (debug)
			System.out.println(string);
	}
	
	
	/**
	 * Handles the get call to the servlet and triggers the junit tests run
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse rsp)
	throws ServletException, IOException {
				
		if (req.getParameter("mNodeUrl") != null) {
			executeJUnitRun(req.getParameter("mNodeUrl"), rsp.getOutputStream());
			
		} else {
			// return error message, because we can't run
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing required parameter 'mNodeUrl'");
		}	
	}
	
	
	private void executeJUnitRun(String mNodeBaseUrl, ServletOutputStream out) throws IOException {
		
		Serializer serializer = new Serializer(out);
		serializer.setIndent(2); // pretty-print output
		
		
		System.setProperty("mNodeUrl", mNodeBaseUrl);
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

		JUnitCore junit = new JUnitCore();

		TestStartListener listener = new TestStartListener();
		junit.addListener(listener);
		
		Result result = junit.run(org.dataone.integration.webTest.MockITSystemProperty.class);
		
		ArrayList<AtomicTest> testList = listener.getTestList();

		// use results template as basis for returned results
		InputStream resultsPg = this.getClass().getResourceAsStream(RESULTS_FILE_TEMPLATE);
		Builder builder = new Builder(false);
		Document resultsDoc = null;
		try {
			resultsDoc = builder.build(resultsPg);
		} catch (ValidityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Nodes nodes = resultsDoc.query("//div[@class = 'template']");
		
		
		if (nodes.size() > 0) {
			Element resultDiv = (Element) nodes.get(0);
			Element body = (Element) resultDiv.getParent();

			resultDiv.detach(); // We're just using it as a template, so remove it from output

			Element div = new Element("div");
			generateURLRow(div,mNodeBaseUrl);
			body.appendChild(div);
			
			for(AtomicTest test : testList) {
				div = new Element("div");
				generateTestReport(test,div);
				body.appendChild(div);
			}
		}
		serializer.write(resultsDoc);
		out.close();

		
		System.out.println("\nRun Statistics");	
		
	    System.out.println("runTime = " + result.getRunTime() + "\n");
	    System.out.println("runCount = " + result.getRunCount());
	    System.out.println("failureCount = " + result.getFailureCount());
	    System.out.println("ignoreCount = " + result.getIgnoreCount());
	    System.out.println("\nFailures");
	    for (Failure f: result.getFailures()) {
	    	System.out.println("  TestHeader = " + f.getTestHeader());
	    	System.out.println("    Message = " + f.getMessage());
	    	System.out.println("    Description = " + f.getDescription());
	    	System.out.println("    Exception = " + f.getException());
	    }
	}

	private void generateURLRow(Element div, String url) {
		
		div.addAttribute(new Attribute("class", "grey"));
		
		Element table = new Element("table");
		Element tr = new Element("tr");
		Element name = new Element("th");
		name.appendChild("Member Node Url: " + url );
		tr.appendChild(name);
		table.appendChild(tr);
		div.appendChild(table);
	}
	
	/**
	 * adapted from webTester in MN package...
	 * @param testResult
	 * @param div
	 */
	private void generateTestReport(AtomicTest testResult, Element div) {

		Element table = new Element("table");
		Element tr = new Element("tr");
		Element name = new Element("th");
		Element description = new Element("td");

		// set color based on status
		if (testResult.getStatus().equals("Success")) {
			div.addAttribute(new Attribute("class", "green"));
		} 
		else if (testResult.getStatus().equals("Ignored")) {
			div.addAttribute(new Attribute("class", "yellow"));
		} 
		else if (testResult.getStatus().equals("Failed")) {
			div.addAttribute(new Attribute("class", "red"));
		}
		else if (testResult.getStatus().equals("Error")) {
			div.addAttribute(new Attribute("class", "orange"));
		}
		else {
			div.addAttribute(new Attribute("class", "violet"));
		}

		// add contents to the table row...
		name.appendChild(testResult.getTestName());
		tr.appendChild(name);

		description.appendChild(testResult.getMessage());
		tr.appendChild(description);

		table.appendChild(tr);
		div.appendChild(table);
	}

	/**
	 * extended class used to catch the output from junit and put it in usable form
	 * @author rnahf
	 *
	 */
	class TestStartListener extends RunListener
	{
		ArrayList<AtomicTest> testList = new ArrayList<AtomicTest>();
		private AtomicTest currentTest;
		
		public void testStarted(Description d) {
			if (currentTest != null) {
				testList.add(currentTest);
			}
			currentTest = new AtomicTest(d.getClassName() + ": " + d.getMethodName());
			currentTest.setStatus("Success");
		}

		public void testIgnored(Description d) {
			if (currentTest != null) {
				testList.add(currentTest);
			}
			currentTest = new AtomicTest(d.getClassName() + ": " + d.getMethodName());
			currentTest.setStatus("Ignored");
			currentTest.setMessage(d.getAnnotation(org.junit.Ignore.class).value());
		}
		
		public void testFailure(Failure f) {
			Throwable t = f.getException();
			if (t instanceof java.lang.AssertionError) {
				currentTest.setStatus("Failed");
			} else {
				currentTest.setStatus("Error");
			}
			currentTest.setMessage( t.getClass().getSimpleName() + ": " + f.getMessage());
		}
		
		public ArrayList<AtomicTest> getTestList() {
			return testList;
		}
	}
	
	
	class AtomicTest
	{
		private String testName;
		private String status;
		private String message;
		
		public AtomicTest(String name)
		{
			setTestName(name);
		}
		
		
		public void setTestName(String packageQualifiedName) {
			testName = packageQualifiedName.replaceAll("org.dataone.integration.", "");
		}
		public String getTestName() {
			return testName;
		}
		
		
		public void setStatus(String status) {
			this.status = status;
		}
		public String getStatus() {
			return status;
		}
		public boolean wasSuccessful() {
			return getStatus().equals("Success");
		}
		
		
		public void setMessage(String message) {
			this.message = message;
		}
		public String getMessage() {
			return message;
		}
	}	
}
