package org.dataone.integration.webTest;

import java.util.ArrayList;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class WebTestListener extends RunListener 
{
	ArrayList<AtomicTestResult> testList = new ArrayList<AtomicTestResult>();
	private AtomicTestResult currentTest;
	private String testCaseName;
	
	public void testStarted(Description d) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		testCaseName = d.getClassName();
		currentTest = new AtomicTestResult(d.getClassName() + ": " + d.getMethodName());
		currentTest.setStatus("Success");
	}

	public void testRunFinished(Result r) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		currentTest = new AtomicTestResult(testCaseName);
		String runSummary = "RunCount=" + r.getRunCount() + 
							"   Failures/Errors=" + r.getFailureCount() +
							"   Ignored=" + r.getIgnoreCount();
		if(r.getFailureCount() > 0) {
			currentTest.setStatus("Failed");
			currentTest.setMessage("Failed Tier due to failures. [" + runSummary + "]");
		} else if (r.getIgnoreCount() > 0) {
			currentTest.setStatus("Ignored");
			currentTest.setMessage("Tier Tentative Pass (Ignored Tests present). [" + runSummary + "]");
		} else {
			currentTest.setStatus("Success");
			currentTest.setMessage("Tier Passed (Ignored Tests present). [" + runSummary + "]");
		}
	}
	
	public void testIgnored(Description d) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		currentTest = new AtomicTestResult(d.getClassName() + ": " + d.getMethodName());
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
	
	public ArrayList<AtomicTestResult> getTestList() {
		return testList;
	}
}
