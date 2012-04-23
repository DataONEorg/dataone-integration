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

class AtomicTest
{
	private String type;
	private String testName;
	private String status;
	private String message;
	private String trace;

	public AtomicTest(String name)
	{
		setTestName(name);
	}


	public void setTestName(String packageQualifiedName) {
		//testName = packageQualifiedName.replaceAll(prefixToRemove, "");
		// remove package name
		testName = packageQualifiedName; //.replaceAll(".*\\.", "");
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

	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

	public void setTrace(String trace) {
		this.trace = trace;
	}
	public String getTrace() {
		return this.trace;
	}
}	

