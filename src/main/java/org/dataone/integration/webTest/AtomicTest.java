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

