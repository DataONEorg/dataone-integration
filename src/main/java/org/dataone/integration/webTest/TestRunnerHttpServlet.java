package org.dataone.integration.webTest;

import java.io.File;
import java.io.IOException;
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
import nu.xom.Serializer;


import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestRunnerHttpServlet extends HttpServlet
{
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
			Element htmlReport = executeJUnitRun(req.getParameter("mNodeUrl"), rsp.getOutputStream());
			
		} else {
			// return error message, because we can't run
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing required parameter 'mNodeUrl'");
		}
		
		
		
		
	}
	
	
	private Element executeJUnitRun(String mNodeBaseUrl, ServletOutputStream out) throws IOException {
		
		Serializer serializer = new Serializer(out);
		
		
		System.setProperty("mNodeUrl", mNodeBaseUrl);
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

		JUnitCore junit = new JUnitCore();

		TestStartListener listener = new TestStartListener();
		junit.addListener(listener);
		
		Result result = junit.run(org.dataone.integration.webTest.MockITSystemProperty.class);
		
		ArrayList<AtomicTest> testList = listener.getTestList();

		Element body = new Element("body");
		Document doc = new Document(body);
		for(AtomicTest test : testList) {
			Element div = new Element("div");
			generateTestReport(test,div);
			body.appendChild(div);
		}
		serializer.write(doc);
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
	    return body;
	}
	
	private void write(Writer w, String s) throws IOException {
		w.write(s);
	}
	private void writeln(Writer w, String s) throws IOException {
		w.write(s + "\n");
		w.flush();
	}
			
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
			currentTest.setStatus("Failed");
			currentTest.setMessage( t.getClass().getName() + ": " + f.getMessage());

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
	
	
	/**
	 * lifted directly from webTester in MN package...
	 * @param testResult
	 * @param url
	 * @param div
	 */
	private void generateTestReport(AtomicTest testResult, Element div) {

		Element table = new Element("table");
		Element tr = new Element("tr");
		Element name = new Element("th");
		Element description = new Element("td");
		Element exception = new Element("td");


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
		else {
			div.addAttribute(new Attribute("class", "violet"));
		}

		// add contents to the table row...
		name.appendChild(testResult.getTestName());
		tr.appendChild(name);

		description.appendChild(testResult.getMessage());
		tr.appendChild(description);

//		if (!testResult.wasSuccessful()) {
//			String details = testResult.getMessage();
//			StringBuilder buffer = new StringBuilder();
//
//			// fail 'gracefully' if the test's code is broken
//			if (details == null) {
//				buffer.append("THIS TEST IS BROKEN - DEV PLEASE FIX ME");
//			}
//			
//			}
//
//			exception.appendChild(buffer.toString());
//		}
//		else {
//			exception.appendChild("");
//		}
//
//		tr.appendChild(exception);

		table.appendChild(tr);
		div.appendChild(table);
	}
	
}
