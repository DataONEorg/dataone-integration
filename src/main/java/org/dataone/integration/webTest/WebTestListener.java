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

import java.util.ArrayList;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Extended class used to catch the output from junit and put it in usable form.
 * 
 * @author rnahf
 *
 */
class WebTestListener extends RunListener 
{
	ArrayList<AtomicTest> testList = new ArrayList<AtomicTest>();
	private AtomicTest currentTest;
	private String testCaseName;
	private boolean newRun = true;
	
	
	public void testStarted(Description d) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		testCaseName = d.getClassName();
		if (newRun) {
			currentTest = new AtomicTest(testCaseName);
			currentTest.setStatus("Header");
			testList.add(currentTest);
			newRun = false;
		}			
		currentTest = new AtomicTest(testCaseName + ": " + d.getMethodName());
		currentTest.setType("Test");
		currentTest.setStatus("Success");
		
		this.newRun = false;
	}

	public void testRunFinished(Result r) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		currentTest = new AtomicTest(testCaseName);
		currentTest.setType("Summary");
		String runSummary = "RunCount=" + r.getRunCount() + 
							"   Failures/Errors=" + r.getFailureCount() +
							"   Ignored=" + r.getIgnoreCount();
		if(r.getFailureCount() > 0) {
			currentTest.setStatus("Failed");
			currentTest.setMessage("Failed Tier due to failures or exceptions. [" + runSummary + "]");
		} else if (r.getIgnoreCount() > 0) {
			currentTest.setStatus("Ignored");
			currentTest.setMessage("Tier Tentative Pass (Ignored Tests present). [" + runSummary + "]");
		} else {
			currentTest.setStatus("Success");
			currentTest.setMessage("Tier Passed (Ignored Tests present). [" + runSummary + "]");
		}
		this.newRun = true;
	}
	
	public void testIgnored(Description d) {
		if (currentTest != null) {
			testList.add(currentTest);
		}
		currentTest = new AtomicTest(d.getClassName() + ": " + d.getMethodName());
		currentTest.setType("Test");
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
		currentTest.setTrace(f.getTrace());
	}
	
	public ArrayList<AtomicTest> getTestList() {
		if (currentTest != null) {
			testList.add(currentTest);
			currentTest = null;
		}
		return testList;
	}
}

