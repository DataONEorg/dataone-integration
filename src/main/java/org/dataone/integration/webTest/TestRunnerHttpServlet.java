package org.dataone.integration.webTest;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.*;

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
			executeJUnitRun(req.getParameter("mNodeUrl"), rsp.getWriter());
		} else {
			// return error message, because we can't run
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing required parameter 'mNodeUrl'");
		}
	}
	
	
	private void executeJUnitRun(String mNodeBaseUrl, Writer out) throws IOException {
		
		System.setProperty("mNodeUrl", mNodeBaseUrl);
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

		JUnitCore junit = new JUnitCore();

		TestStartListener listener = new TestStartListener();
		junit.addListener(listener);
		
		Result result = junit.run(org.dataone.integration.webTest.SystemPropertyTest.class);
		
		ArrayList<String> testList = listener.getTestList();
		for(String test : testList) {
			writeln(out, test);
		}
		writeln(out, "\nRun Statistics");	
		
	    writeln(out, "runTime = " + result.getRunTime() + "\n");
	    writeln(out,"runCount = " + result.getRunCount());
	    writeln(out,"failureCount = " + result.getFailureCount());
	    writeln(out,"ignoreCount = " + result.getIgnoreCount());
	    writeln(out,"\nFailures");
	    for (Failure f: result.getFailures()) {
	    	writeln(out,"  TestHeader = " + f.getTestHeader());
	    	writeln(out,"    Message = " + f.getMessage());
	    	writeln(out,"    Description = " + f.getDescription());
	    	writeln(out,"    Exception = " + f.getException());
	    }
	    out.flush();
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
		ArrayList<String> testList = new ArrayList<String>();
//		public void testRunStarted(Description d) {
//			testList.add(d.getClassName() + ":" + d.getMethodName());
//		}
		public void testStarted(Description d) {
			testList.add("Started: " + d.getClassName() + ":" + d.getMethodName());
		}
		public void testIgnored(Description d) {
			testList.add("Ignored: " + d.getClassName() + ":" + d.getMethodName());
		}
		public void testFailure(Failure f) {
			testList.add("Header: " + f.getTestHeader() +
					"\nMessage: " + f.getMessage() + 
					"\nDescription: " + f.getDescription() +
					"\nException: " + f.getException() +
					"\nTrace: " + f.getTrace());
		}
		
		
		public ArrayList<String> getTestList() {
			return testList;
		}
		
	}
}
