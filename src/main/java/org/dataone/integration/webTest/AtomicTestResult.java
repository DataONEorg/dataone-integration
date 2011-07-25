package org.dataone.integration.webTest;

public class AtomicTestResult
{
	private String testName;
	private String status;
	private String message;

	public AtomicTestResult(String name)
	{
		setTestName(name);
	}


	public void setTestName(String packageQualifiedName) {
		testName = packageQualifiedName.replaceAll("org.dataone.integration.it.", "");
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

