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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import edu.emory.mathcs.backport.java.util.Collections;

public class TestRunnerHttpServlet extends HttpServlet
{
	protected static Log log = LogFactory.getLog(TestRunnerHttpServlet.class);
//	private static final String TESTS_DIR = "/WEB-INF/tests";
//	private static final String RESULTS_FILE_TEMPLATE = "/results.html";
	private static final String RESULTS_FILE_HEAD = "/results_head.html";
//	private static final String RESULTS_FILE_DIV = "/results_div.html";
	
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
				executeJUnitRun(req ,rsp.getOutputStream());
			} 
			catch (ClassNotFoundException e) {
				throw new ServletException("Internal Configuration problem: Test classes Not Found",e);
			} catch (ValidityException e) {
				throw new ServletException("Internal Configuration problem: HTML templates not valid",e);
			} catch (ParsingException e) {
				throw new ServletException("Internal Configuration problem: HTML templates couldn't be parsed",e);
			}
			
		} else {
			// return error message, because we can't run
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Missing required parameter 'mNodeUrl'");
		}	
	}
	
	
	private void executeJUnitRun(HttpServletRequest req, ServletOutputStream out) 
	throws IOException, ClassNotFoundException, ValidityException, ParsingException 
	{	
		String mNodeBaseUrl = req.getParameter("mNodeUrl");
		String testObjectSeries = req.getParameter("testObjectSeries");	
		String maxTier = req.getParameter("maxTier"); 
		
		if (log.isDebugEnabled() ) {
			Map params = req.getParameterMap();
			log.debug(":::::::request parameters::::::");
			for (Object key: params.keySet()) {
				log.debug(String.format("param: %s = %s", key.toString(), params.get(key).toString()));
			}
		}


		// start putting the response on the outputstream

		// spit out the header
		out.write("<html>\n".getBytes("UTF-8"));
		out.write(IOUtils.toByteArray( this.getClass().getResourceAsStream(RESULTS_FILE_HEAD)));
		
		
		// spit out the visible header 
		out.write("  <body>\n".getBytes("UTF-8"));
		
		Element div = new Element("div");
		generateURLRow(div,mNodeBaseUrl);
		StreamableSerializer ss = new StreamableSerializer(out);
		ss.writeChild(div);
		ss.flush();
		
		
		log.info("setting system property '" + TestSettings.CONTEXT_MN_URL +
				"' to value '" + mNodeBaseUrl + "'");
		System.setProperty(TestSettings.CONTEXT_MN_URL, mNodeBaseUrl);
		
		// part of thread-aware way of passing form parameters to ContextAwareTestCaseDataone
		// the other part is in ContextAwareTestCaseDataone
		String threadPropertyBase = "mnwebtester.thread." + Thread.currentThread().getId();
		System.setProperty(threadPropertyBase + ".mn.baseurl", mNodeBaseUrl);
		if ( StringUtils.isNotEmpty( testObjectSeries ) )
			System.setProperty(threadPropertyBase + ".tierTesting.object.series", testObjectSeries);
		
		Settings.getResetConfiguration();
		
		
		// to test that system properties are being received
		if (debug) 
			System.setProperty("testSysProp", "setFromServlet");

	
		// setup and run through the junit tests
		
		JUnitCore junit = new JUnitCore();
		StreamingWebTestListener listener = new StreamingWebTestListener(out);
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
			if (isWithinTierLevelCutoff(maxTier, testCase)) {
				log.debug("running tests on: " + testCase.getSimpleName());
				result = junit.run(testCase);
			}
		}
		
		listener.finishReport();
		
		
		out.write("  </body>\n".getBytes("UTF-8"));
		out.write("</html>\n".getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	
	
	/** 
	 * if not working within the context of MNodeTier tests, will return true
	 * otherwise, compare number in the test to the maxTier number.
	 */
	public boolean isWithinTierLevelCutoff(String maxTier, Class testCase) 
	{
		if (maxTier == null) 
			return true;
		
		String testName = testCase.getSimpleName();
		if (testName.contains("MNodeTier")) {
			int index = testName.indexOf("Tier") + 4;
			Integer tcTier = Integer.valueOf(testName.substring(index,index+1));
			
			Integer tierMaxNum = Integer.valueOf(maxTier.replaceAll("\\D+",""));

			if (tcTier <= tierMaxNum) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
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
	
}
