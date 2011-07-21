package org.dataone.integration.webTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestRunnerHttpServlet extends HttpServlet
{
	protected static Log log = LogFactory.getLog(TestRunnerHttpServlet.class);
//	private static final String TESTS_DIR = "/WEB-INF/tests";
	private static final String RESULTS_FILE_TEMPLATE = "/results.html";
	
//	private static String TEST_SELECTOR_PATTERN = Settings.getConfiguration().getString("webTest.mn.testCase.pattern");
	private static String TEST_SELECTOR_PATTERN = "*MNodeTier*";
	
	private boolean debug = true;
	
	/**
	 * a constructor to be used for unit testings
	 * @param isUnitTest
	 */
	public TestRunnerHttpServlet(boolean isUnitTest)
	{
		super();
		if (isUnitTest)
			TEST_SELECTOR_PATTERN = "*MockITCase";
	}
	
	public TestRunnerHttpServlet()
	{
		super();
	}
	
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse rsp)
	throws ServletException, IOException {
		log.debug("Entered TestRunnerHttpServlet.doPost()");
		
		doGet(req,rsp);
	} 
	
	/**
	 * Handles the get call to the servlet and triggers the junit tests run
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse rsp)
	throws ServletException, IOException {
		log.debug("Entered TestRunnerHttpServlet.doGet()");
		
		if (req.getParameter("mNodeUrl") != null) {
			try {
				rsp.setContentType("text/html; charset=UTF-8");
				executeJUnitRun(req.getParameter("mNodeUrl"), rsp.getOutputStream());
			} catch (ClassNotFoundException e) {
				throw new ServletException("Internal Configuration problem: Test classes Not Found",e);
			}
			
		} else {
			// return error message, because we can't run
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing required parameter 'mNodeUrl'");
		}	
	}
	
	
	private void executeJUnitRun(String mNodeBaseUrl, ServletOutputStream out) 
	throws IOException, ClassNotFoundException 
	{
		
		Serializer serializer = new Serializer(out);
		serializer.setIndent(2); // pretty-print output
		
		
		System.setProperty(TestSettings.CONTEXT_MN_URL, mNodeBaseUrl);
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

		JUnitCore junit = new JUnitCore();

		
		// see:
		// http://stackoverflow.com/questions/1302815/how-to-get-access-to-the-current-junitcore-to-add-a-listener
		TestStartListener listener = new TestStartListener();
		junit.addListener(listener);

		Result result = null;
		for (Class testCase : getIntegrationTestClasses(TEST_SELECTOR_PATTERN)) {
			log.debug("running tests on: " + testCase.getSimpleName());
			System.out.println(testCase.getSimpleName());
			result = junit.run(testCase);
		}
		
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

		
//		System.out.println("\nRun Statistics");	
//		
//	    System.out.println("runTime = " + result.getRunTime() + "\n");
//	    System.out.println("runCount = " + result.getRunCount());
//	    System.out.println("failureCount = " + result.getFailureCount());
//	    System.out.println("ignoreCount = " + result.getIgnoreCount());
//	    System.out.println("\nFailures");
//	    for (Failure f: result.getFailures()) {
//	    	System.out.println("  TestHeader = " + f.getTestHeader());
//	    	System.out.println("    Message = " + f.getMessage());
//	    	System.out.println("    Description = " + f.getDescription());
//	    	System.out.println("    Exception = " + f.getException());
//	    }
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

	
	
	@SuppressWarnings("rawtypes")
	private static Class[] getIntegrationTestClasses(String pattern) 
	throws ClassNotFoundException, IOException 
	{
		log.debug("Java class Path: " + System.getProperty("java.class.path") );
		
		ArrayList<Class> matchingClasses = new ArrayList<Class>();
//		matchingClasses.add(org.dataone.integration.MNodeTier1IT.class);
//		matchingClasses.add(org.dataone.integration.MNodeTier2IT.class);
//		matchingClasses.add(org.dataone.integration.MNodeTier3IT.class);
		
		Class[] testClasses = getClasses("org.dataone.integration");  // gets classes in subpackages, too

		log.debug("testClass.pattern = " + pattern);
		for(Class testClass : testClasses) {
			String className = testClass.getName();
			log.debug("testCase: " + className);
			// process classes to exclude first
			if (pattern == null || compareToStarPattern(pattern,className)) {
				if (!className.equals("TestRunnerHttpServlet") ||
						className.contains("webTest")) {
					log.info("Registering class: " + testClass.getName());
					matchingClasses.add(testClass);
				}
			}
		}
		return matchingClasses.toArray(new Class[matchingClasses.size()]);
	}
	
	
	
	/**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
            	if (!file.getName().contains("$"))
            		classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
	
	private static boolean compareToStarPattern(String pattern, String className) {

		if (pattern.startsWith("*")) {
			pattern = pattern.substring(1);
			if (pattern.endsWith("*")) {
				pattern = pattern.substring(0,pattern.length()-1);
				log.debug("  pattern: contains " + pattern);
				return className.contains(pattern);
			} else {
				log.debug("  pattern: endsWith " + pattern);
				return className.endsWith(pattern);
			}
		} else {
			if (pattern.endsWith("*")) {
				pattern = pattern.substring(0,pattern.length()-1);
				log.debug("  pattern: startsWith " + pattern);
				return className.startsWith(pattern);
			} else {
				log.debug("  pattern: equals " + pattern);
				return className.equals(pattern);
			}
		}
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
		private String testCaseName;
		
		public void testStarted(Description d) {
			if (currentTest != null) {
				testList.add(currentTest);
			}
			testCaseName = d.getClassName();
			currentTest = new AtomicTest(d.getClassName() + ": " + d.getMethodName());
			currentTest.setStatus("Success");
		}

		public void testRunFinished(Result r) {
			if (currentTest != null) {
				testList.add(currentTest);
			}
			currentTest = new AtomicTest(testCaseName);
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
//			testList.add(currentTest);
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
