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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.ArrayUtil;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.runner.JUnitCore;


/**
 * This class is for running the d1_integration junit integration tests through the WebTester,
 * and so the scope of possible tests run may be limited programatically or by system properties.
 * 
 * mnwebetester.testCase.pattern=*MNodeTier*
 * mnwebtester.junitcore.sleep.seconds 
 * 
 * @author rnahf
 *
 */

public class TestRunnerHttpServlet extends HttpServlet
{
	protected static Log log = LogFactory.getLog(TestRunnerHttpServlet.class);
//	private static final String TESTS_DIR = "/WEB-INF/tests";
//	private static final String RESULTS_FILE_TEMPLATE = "/results.html";
	private static final String RESULTS_FILE_HEAD = "/results_head.html";
//	private static final String RESULTS_FILE_DIV = "/results_div.html";
	
	private String testSelectorClassNamePattern = Settings.getConfiguration()
		.getString("mnwebtester.testCase.pattern","*MNodeTier*");
	private static final String TEST_PACKAGE_DOMAIN = "org.dataone.integration";
//	private static String TEST_SELECTOR_PATTERN = "*MNodeTier*";
	
	private boolean debug = true;
	
	private int junitSleepSeconds = 0;
	
	private static Class<?>[] testClasses;
	
	/**
	 * a constructor to be used for unit testings
	 * @param isUnitTest
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public TestRunnerHttpServlet(boolean isUnitTest) throws IOException
	{
		this();		
		if (isUnitTest)
			testSelectorClassNamePattern = "*MockITCase";
	}
	
	public TestRunnerHttpServlet() throws IOException
	{
		super();
		testClasses = getClasses(TEST_PACKAGE_DOMAIN);
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
		String[] selectedTiers = req.getParameterValues("selectedTiers");
		
		Integer[] selectedTierLevels = deriveSelectedTierLevels(selectedTiers);
				
		if (log.isDebugEnabled() ) {
			Map<String, String[]> params = req.getParameterMap();
			log.debug(":::::::request parameters::::::");
			for (Entry<String,String[]> e: params.entrySet()) {
				log.debug(String.format("param: %s = %s", e.getKey(), StringUtils.join(e.getValue(),",")));
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

		for (@SuppressWarnings("rawtypes") Class testCase : getIntegrationTestClasses(testSelectorClassNamePattern)) {
			if (junitSleepSeconds != 0) {
				try {
					log.info("sleeping between test cases for " + junitSleepSeconds + " seconds...");
					Thread.sleep(junitSleepSeconds * 1000);
				} catch (InterruptedException e) {
					log.warn("sleep interrupted: " + e.getMessage());
				}
			}
			if (isASelectedTier(selectedTierLevels, testCase)) {
				log.debug("running tests on: " + testCase.getSimpleName());
				junit.run(testCase);
			}
		}
		
		listener.finishReport();
		
		
		out.write("  </body>\n".getBytes("UTF-8"));
		out.write("</html>\n".getBytes("UTF-8"));
		out.flush();
	}

	public static Integer[] deriveSelectedTierLevels(String[] selectedTiers) {

		Integer[] selectedTierLevels = null;
		if (selectedTiers != null) {
			selectedTierLevels = new Integer[selectedTiers.length];
			for (int i = 0; i< selectedTiers.length; i++) {
				selectedTierLevels[i] = Integer.valueOf(selectedTiers[i].replaceAll("\\D+", ""));
				log.debug("Selected Tier: [" + i + "]:" + selectedTierLevels[i]);
			}
		}
		return selectedTierLevels;
	}
	
	/** 
	 * if not working within the context of MNodeTier tests, will return true
	 * otherwise, compare number in the test to the number(s) in selectedTiers.
	 */
	public boolean isASelectedTier(Integer[] selectedTiers, Class<?> testCase) 
	{
		if (selectedTiers == null) 
			return true;
		
		String testName = testCase.getSimpleName();
		log.debug("testCase name: " + testName);
		if (testName.contains("MNodeTier")) {
			int index = testName.indexOf("Tier") + 4;
			Integer tcTier = Integer.valueOf(testName.substring(index,index+1));
			
			for (Integer selectedTier : selectedTiers) {
				if (tcTier == selectedTier) {
					log.debug("   is Selected : " + testName);
					return true;
				}
			}
			return false;
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
	private Class[] getIntegrationTestClasses(String pattern) 
	{
		log.debug("Java class Path: " + System.getProperty("java.class.path") );
		
		ArrayList<Class> matchingClasses = new ArrayList<Class>();
		
		log.debug("find classes in package: " + TEST_PACKAGE_DOMAIN);
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
		return matchingClasses.toArray(new Class<?>[matchingClasses.size()]);
	}
	
	@SuppressWarnings("rawtypes")
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
    private static Class<?>[] getClasses(String packageName) throws IOException 
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
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
    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        log.debug("findClasses [D]: " + directory.getName());
        for (File file : files) {

        	String filename = file.getName();
            if (file.isDirectory()) {
                assert !filename.contains(".");
                classes.addAll(findClasses(file, packageName + "." + filename));
            } else if (filename.endsWith("IT.class") || filename.endsWith("MockITCase.class")) {
            	log.debug("findClasses    :    " + filename);
            	if (!filename.contains("$")) {
            		String fullFileName = packageName + '.' + filename.substring(0, filename.length() - 6);
            		try {
						classes.add(Class.forName(fullFileName));
					} catch (Throwable t) {
						log.warn("Could not add class for " + fullFileName, t);
					}
            	}
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
