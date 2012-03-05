package org.dataone.integration.webTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import edu.emory.mathcs.backport.java.util.Collections;

public class TestRunnerHttpServlet extends HttpServlet
{
	protected static Log log = LogFactory.getLog(TestRunnerHttpServlet.class);
//	private static final String TESTS_DIR = "/WEB-INF/tests";
	private static final String RESULTS_FILE_TEMPLATE = "/results.html";
	
	private static String TEST_SELECTOR_PATTERN = Settings.getConfiguration()
		.getString("mnwebtester.testCase.pattern","*MNodeTier*");
	private static final String TEST_PACKAGE = "org.dataone.integration";
//	private static String TEST_SELECTOR_PATTERN = "*MNodeTier*";
	
	private boolean debug = true;
	
	private int junitSleepSeconds = 0;
	
	/**
	 * a constructor to be used for unit testings
	 * @param isUnitTest
	 */
	public TestRunnerHttpServlet(boolean isUnitTest)
	{
		super();
		junitSleepSeconds = Settings.getConfiguration()
			.getInt("mnwebtester.junitcore.sleep.seconds",junitSleepSeconds);
		if (isUnitTest)
			TEST_SELECTOR_PATTERN = "*MockITCase";
	}
	
	public TestRunnerHttpServlet()
	{
		super();
		junitSleepSeconds = Settings.getConfiguration()
			.getInt("mnwebtester.junitcore.sleep.seconds",junitSleepSeconds);
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
		log.debug("current thread: " + Thread.currentThread().getId());
		
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
		
		log.info("setting system property '" + TestSettings.CONTEXT_MN_URL +
				"' to value '" + mNodeBaseUrl + "'");
		System.setProperty(TestSettings.CONTEXT_MN_URL, mNodeBaseUrl);
		String threadProperty = "mnwebtester.thread." + Thread.currentThread().getId() + ".mn.baseurl";
		System.setProperty(threadProperty, mNodeBaseUrl);
		Configuration c = Settings.getResetConfiguration();
		
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

		JUnitCore junit = new JUnitCore();
		
		// see:
		// http://stackoverflow.com/questions/1302815/how-to-get-access-to-the-current-junitcore-to-add-a-listener
		WebTestListener listener = new WebTestListener();
		junit.addListener(listener);

		Result result = null;
		for (Class testCase : getIntegrationTestClasses(TEST_SELECTOR_PATTERN)) {
			if (junitSleepSeconds == 0) {
				try {
					log.info("sleeping between test cases for " + junitSleepSeconds + " seconds...");
					Thread.sleep(junitSleepSeconds * 1000);
				} catch (InterruptedException e) {
					log.warn("sleep interrupted: " + e.getMessage());
				}
			}
			log.debug("running tests on: " + testCase.getSimpleName());
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
			
			int rowIndex = 1;
			for(AtomicTest test : testList) {
				div = new Element("div");
				generateTestReportLine(test,div,rowIndex++);
				body.appendChild(div);
			}
		}
		serializer.write(resultsDoc);
		out.close();
	}

	private void generateURLRow(Element div, String url) {
		
		div.addAttribute(new Attribute("class", "greyDescr"));
		
		Element table = new Element("table");
		Element tr = new Element("tr");
		Element name = new Element("th");
		Element rundate = new Element("td");
		
		name.appendChild("Member Node Url: " + url );
		tr.appendChild(name);
		
		Date date = new Date();
		rundate.appendChild(date.toString());
		tr.appendChild(rundate);
		
		table.appendChild(tr);
		div.appendChild(table);
	}

	
	private void generateEmptyRow(Element div) {
		
		div.addAttribute(new Attribute("class", "grey"));
		Element linebreak = new Element("br");
		div.appendChild(linebreak);
	}
	
	
	
	
	/**
	 * adapted from webTester in MN package...
	 * @param testResult
	 * @param div
	 */
	private void generateTestReportLine(AtomicTest testResult, Element div, int rowIndex) {

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
		else if (testResult.getStatus().equals("Header")) {
			div.addAttribute(new Attribute("class", "greyHeader"));
		}
		else {
			div.addAttribute(new Attribute("class", "greyDescr"));
		}

		// add contents to the table row...	
		name.appendChild(formatTestName(testResult.getTestName()));
		tr.appendChild(name);

		if (testResult.getMessage() != null) {
			description.appendChild(testResult.getMessage().replaceAll("\n", "  <br>\n"));
		} else {
			description.appendChild(testResult.getMessage());
		}
		tr.appendChild(description);
		table.appendChild(tr);
		div.appendChild(table);

		// add trace information if any (in separate table in the div)
		if (testResult.getTrace() != null) {		
			// append toggle link to existing description
			description.appendChild(buildTraceViewControl(rowIndex));
			// add another table to the div
			div.appendChild(buildTraceView(rowIndex, testResult));
		}
	}
	
	
	/*
	 * Want to make test names human readable, but the test case also 
	 * passes through this, so need to format differently.  The basic idea
	 * here is to keep the last segment of the raw name, where the segments
	 * are {package}.{package}.{testCase}: {method_subtest}
	 */
	private String formatTestName(String rawTestName)
	{
		String improvedTestName = null;
		if (rawTestName.contains(":")) {
			// keep just the method and subtest
			improvedTestName = rawTestName.replaceAll(".*\\: ", "");
			improvedTestName = improvedTestName.replace("_", " : ");
		} else {
			// keep the TestCase segment
			improvedTestName = rawTestName.replace(".*\\.", "");
		}
		// underscores in subtest section of method name get converted to spaces
		improvedTestName = improvedTestName.replaceAll("_", " ");
//		improvedTestName = humaniseCamelCase(improvedTestName);
		return improvedTestName;
	}
	

	private Element buildTraceViewControl(int rowID)
	{
		Element toggleText = new Element("a");
		toggleText.addAttribute(new Attribute("id", "toggleControl"+rowID));
		toggleText.addAttribute(new Attribute("href", 
				"javascript:toggleTrace('traceContent"+rowID + "','toggleControl"+ rowID + "');"));
		toggleText.appendChild("show trace");
		return toggleText;
	}
	
	
	private Element buildTraceView(int rowID, AtomicTest testResult)
	{
		Element traceTable = new Element("table");
		Element traceRow = new Element("tr");
		traceRow.appendChild(new Element("td"));
		
		// create the hide-able text
		Element traceData = new Element("td");
		
		Element traceDiv = new Element("div");
		traceDiv.addAttribute(new Attribute("id","traceContent" + rowID));
		traceDiv.addAttribute(new Attribute("style","display: none;"));
		
		// shorten the stack trace to remove precursor JUnit runner frames
		// use the testName to find the last line to include from the stackTrace
		//   the rest is JUnitCore stuff 
		String searchString = testResult.getTestName().replace(": ",".");		
		int methodNameStart = testResult.getTrace().indexOf(searchString);
		// lookahead to end of the line ( all lines end with ")" )
		int lastChar = testResult.getTrace().indexOf(")", methodNameStart);
		
		String conciseTrace = testResult.getTrace().substring(0, lastChar+1).replace("\n","\r\n");
		
		Element formattedText = new Element("pre");
		formattedText.appendChild(conciseTrace);
		
		traceDiv.appendChild(formattedText);
		traceData.appendChild(traceDiv);
		traceRow.appendChild(traceData);
		traceTable.appendChild(traceRow);
		
		
		return traceTable;
	}
	
	@SuppressWarnings("rawtypes")
	private static Class[] getIntegrationTestClasses(String pattern) 
	throws ClassNotFoundException, IOException 
	{
		log.debug("Java class Path: " + System.getProperty("java.class.path") );
		
		ArrayList<Class> matchingClasses = new ArrayList<Class>();
		
		Class[] testClasses = getClasses(TEST_PACKAGE);  // gets classes in subpackages, too
		log.debug("find classes in package: " + TEST_PACKAGE);
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
		Collections.sort(matchingClasses, new NameComparator());
		return matchingClasses.toArray(new Class[matchingClasses.size()]);
	}
	
	static class NameComparator implements Comparator<Class> {
	    @Override
	    public int compare(Class c1, Class c2) {
	        return c1.getSimpleName().compareTo(c2.getSimpleName());
	    }
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
	 * Converts a camelCase to a more human form, with spaces. E.g. 'Camel case'
	 *
	 */
	private String humaniseCamelCase(String word) {
	    Pattern pattern = Pattern.compile("([A-Z]|[a-z])[a-z]*");

	    Vector<String> tokens = new Vector<String>();
	    Matcher matcher = pattern.matcher(word);
	    String acronym = "";
	    while(matcher.find()) {
	        String found = matcher.group();
	        if(found.matches("^[A-Z]$")) {
	            acronym += found;
	        } else {
	            if(acronym.length() > 0) {
	                //we have an acronym to add before we continue
	                tokens.add(acronym);
	                acronym  = "";
	            }
	            tokens.add(found.toLowerCase());
	        }
	    }
	    if(acronym.length() > 0) {
	        tokens.add(acronym);
	    }
	    if (tokens.size() > 0) {
	    	String firstToken = tokens.remove(0);
	    	String humanisedString = firstToken.substring(0, 1).toUpperCase() 
	    		+ firstToken.substring(1);       
	        for (String s : tokens) {
	            humanisedString +=  " " + s;
	        }
	        return humanisedString;
	    }

	    return word;
	}
	
//	
//	/**
//	 * extended class used to catch the output from junit and put it in usable form
//	 * @author rnahf
//	 *
//	 */
//	class TestStartListener extends RunListener
//	{
//		ArrayList<AtomicTest> testList = new ArrayList<AtomicTest>();
//		private AtomicTest currentTest;
//		private String testCaseName;
//		private boolean newRun = true;
//		
//		public void testStarted(Description d) {
//			if (currentTest != null) {
//				testList.add(currentTest);
//			}
//			testCaseName = d.getClassName();
//			if (newRun) {
//				currentTest = new AtomicTest(testCaseName);
//				currentTest.setStatus("Header");
//				testList.add(currentTest);
//				newRun = false;
//			}			
//			currentTest = new AtomicTest(testCaseName + ": " + d.getMethodName());
//			currentTest.setType("Test");
//			currentTest.setStatus("Success");
//			
//			this.newRun = false;
//		}
//
//		public void testRunFinished(Result r) {
//			if (currentTest != null) {
//				testList.add(currentTest);
//			}
//			currentTest = new AtomicTest(testCaseName);
//			currentTest.setType("Summary");
//			String runSummary = "RunCount=" + r.getRunCount() + 
//								"   Failures/Errors=" + r.getFailureCount() +
//								"   Ignored=" + r.getIgnoreCount();
//			if(r.getFailureCount() > 0) {
//				currentTest.setStatus("Failed");
//				currentTest.setMessage("Failed Tier due to failures or exceptions. [" + runSummary + "]");
//			} else if (r.getIgnoreCount() > 0) {
//				currentTest.setStatus("Ignored");
//				currentTest.setMessage("Tier Tentative Pass (Ignored Tests present). [" + runSummary + "]");
//			} else {
//				currentTest.setStatus("Success");
//				currentTest.setMessage("Tier Passed (Ignored Tests present). [" + runSummary + "]");
//			}
//			this.newRun = true;
//		}
//		
//		public void testIgnored(Description d) {
//			if (currentTest != null) {
//				testList.add(currentTest);
//			}
//			currentTest = new AtomicTest(d.getClassName() + ": " + d.getMethodName());
//			currentTest.setType("Test");
//			currentTest.setStatus("Ignored");
//			currentTest.setMessage(d.getAnnotation(org.junit.Ignore.class).value());
//		}
//		
//		public void testFailure(Failure f) {
//			Throwable t = f.getException();
//			if (t instanceof java.lang.AssertionError) {
//				currentTest.setStatus("Failed");
//			} else {
//				currentTest.setStatus("Error");
//			}
//			currentTest.setMessage( t.getClass().getSimpleName() + ": " + f.getMessage());		
//			currentTest.setTrace(f.getTrace());
//		}
//		
//		public ArrayList<AtomicTest> getTestList() {
//			if (currentTest != null) {
//				testList.add(currentTest);
//				currentTest = null;
//			}
//			return testList;
//		}
//	}
//	
//	
//	class AtomicTest
//	{
//		private String type;
//		private String testName;
//		private String status;
//		private String message;
//		private String trace;
//		
//		public AtomicTest(String name)
//		{
//			setTestName(name);
//		}
//		
//		
//		public void setTestName(String packageQualifiedName) {
//			testName = packageQualifiedName.replaceAll("org.dataone.integration.it.", "");
//		}
//		public String getTestName() {
//			return testName;
//		}
//		
//		
//		public void setStatus(String status) {
//			this.status = status;
//		}
//		public String getStatus() {
//			return status;
//		}
//		public boolean wasSuccessful() {
//			return getStatus().equals("Success");
//		}
//		
//		
//		public void setMessage(String message) {
//			this.message = message;
//		}
//		public String getMessage() {
//			return message;
//		}
//		
//		public void setType(String type) {
//			this.type = type;
//		}
//		public String getType() {
//			return type;
//		}
//		
//		public void setTrace(String trace) {
//			this.trace = trace;
//		}
//		public String getTrace() {
//			return this.trace;
//		}
//
//	}	
}
