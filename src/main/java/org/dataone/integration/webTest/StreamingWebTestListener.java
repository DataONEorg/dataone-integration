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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Extended class used to catch the output from junit, put it in usable form,
 * and write the result to the provided output stream.
 *
 * The junit test classes take a long time to complete, so streaming allows
 * incremental output across http, so the browser can start rendering earlier.
 *
 * the signaling of a new test prompts the output of the previous test, so users
 * must call finishReport() to get the last test result.
 *
 * @author rnahf
 *
 */
class StreamingWebTestListener extends RunListener
{
    protected static Log logger = LogFactory.getLog(StreamingWebTestListener.class);
    //	ArrayList<AtomicTest> testList = new ArrayList<AtomicTest>();
    private AtomicTest currentTest;
    private String testCaseName;
    private boolean newRun = true;

    private OutputStream out;
    private StreamableSerializer serializer;
    private int testRowIndex = 1;
    private int assumptions = 0;
    private int failures = 0;
    private int warnings = 0;
    private int errors = 0;


    public StreamingWebTestListener(OutputStream os) throws ValidityException, ParsingException, IOException {
        super();

        this.out = os;

        // to allow incremental rendering, start writing output to the output stream
        this.serializer = new StreamableSerializer(out);
        this.serializer.setIndent(2); // pretty-print output
    }


    /*
     * this helper performs the reflection needed to find the test implementation
     * method where the annotations.  Uses @WebTestImplementation annotation
     */
    private Method findTestMethodImplementation(Description d) {
        Method implMethod = null;
        try {
            Class<?> testClass= d.getTestClass();
            Field[] f = testClass.getDeclaredFields();
            for (int i=0; i<f.length; i++) {
                if (f[i].getAnnotation(WebTestImplementation.class) != null) {
                    Class<?> implClass = f[i].getType();
                    
                    try {
                        implMethod = implClass.getMethod(d.getMethodName(), Iterator.class, String.class);
                    } catch (NoSuchMethodException noMeth) {
                        continue;
                    }
                    
                    if (implMethod != null)
                        break;
                }
            }
        }
        catch (SecurityException e) {
            ;
        }
        return implMethod;
    }



    /**
     * testStarted reports any previous test and creates a new 'current test'
     * If it is considered a new run, it also reports a header line with the name
     * of the test case.
     */
    @Override
    public void testStarted(Description d) {

        if (currentTest != null) {
            report(currentTest);
        }

        testCaseName = d.getClassName();
        if (newRun) {
            currentTest = new AtomicTest(testCaseName);
            currentTest.setStatus("runHeader");
            if (d.getTestClass().getAnnotation(WebTestDescription.class) != null)
                currentTest.setTestDescription(d.getTestClass().getAnnotation(WebTestDescription.class).value());
            report(currentTest);
            newRun = false;
        }
        Method implMethod = findTestMethodImplementation(d);
        currentTest = new AtomicTest(d.getMethodName());
        if (implMethod != null) {
            Annotation a = implMethod.getAnnotation(WebTestName.class);
            if (a != null)
                currentTest.setTestLabel(((WebTestName)a).value());
            a = implMethod.getAnnotation(WebTestDescription.class);
            if (a != null)
                currentTest.setTestDescription(((WebTestDescription)a).value());
        }
        currentTest.setStatus("testStarted");

//        this.newRun = false;
    }


    /**
     * testIgnored reports any previous test and creates a new 'current test'
     * If it is considered a new run, it also reports a header line with the name
     * of the test case.
     */
    @Override
    public void testIgnored(Description d) {
        if (currentTest != null) {
            report(currentTest);
        }

        testCaseName = d.getClassName();

        if (newRun) {
            currentTest = new AtomicTest(testCaseName);
            currentTest.setStatus("runHeader");
            currentTest.setMessage("SOURCE");
            report(currentTest);
            newRun = false;
        }

        Method implMethod = findTestMethodImplementation(d);
        currentTest = new AtomicTest(d.getMethodName());
        if (implMethod != null) {
            Annotation a = implMethod.getAnnotation(WebTestName.class);
            if (a != null)
                currentTest.setTestLabel(((WebTestName)a).value());
            a = implMethod.getAnnotation(WebTestDescription.class);
            if (a != null)
                currentTest.setTestDescription(((WebTestDescription)a).value());
        }
        currentTest.setStatus("testIgnored");
        currentTest.setMessage(d.getAnnotation(org.junit.Ignore.class).value());
    }


    /**
     * testFailure modifies the status, message, and stackTrace of the current test
     */
    @Override
    public void testFailure(Failure f) {
        Throwable t = f.getException();
        if (t instanceof java.lang.AssertionError) {
            currentTest.setStatus("testFailure");
            this.failures++;
        } else if (t instanceof org.dataone.integration.TestIterationEndingException) {
            currentTest.setStatus("testWarning");
            this.warnings++;
        } else {
            currentTest.setStatus("testError");
            this.errors++;
        }
        currentTest.setMessage( t.getClass().getSimpleName() + ": " + f.getMessage());
        currentTest.setTrace(f.getTrace());
    }


    /**
     * testAssumptionFailure modifies the status, message, and stackTrace of the current test
     * It is called when the org.junit.runner.AssumptionViolationException is thrown
     * and is used to indicate when a precondition for the test is not met.
     */
    @Override
    public void testAssumptionFailure(Failure f) {
        Throwable t = f.getException();

        this.assumptions++;
        currentTest.setStatus("testAssumptionBye");
        currentTest.setMessage( t.getClass().getSimpleName() + ": " + f.getMessage());
        currentTest.setTrace(f.getTrace());

    }



    /**
     * testRunFinished reports the previous test and creates a new current test
     * that is the summary of the run / testcase.  It can get the count of failures
     *  ignored tests, and total tests run from the Result parameter, but can't
     *  get the testAssumptionFailures, so gets it from a counter property maintained
     *  in this class.
     */
    @Override
    public void testRunFinished(Result r) {
        if (currentTest != null) {
            report(currentTest);
        }

        currentTest = new AtomicTest(testCaseName);
        String runSummary = "RunCount=" + r.getRunCount() +
                "  Failures=" + this.failures +
                "  Errors=" + this.errors +
                "  Warnings=" + this.warnings +
                "  Assumptions=" +  this.assumptions +
                "  Ignored=" + r.getIgnoreCount();
        if (this.failures > 0) {
            currentTest.setStatus("summaryFail");
            currentTest.setMessage("Failed Test Case due to failures. [" + runSummary + "]");
        } else if (this.errors + this.warnings > 0) {
            // want to warn if either the node isn't ready to test fully, or tests
            // are in error, somehow (needs debugging then).
            currentTest.setStatus("summaryWarning");
            currentTest.setMessage("Could not run all tests. [" + runSummary + "]");
        } else if (r.getRunCount() == r.getIgnoreCount()) {
            currentTest.setStatus("summaryAllIgnored");
            currentTest.setMessage("Tier Not Tested [" + runSummary + "]");
        } else {
            currentTest.setStatus("summaryPass");
            currentTest.setMessage("Tier Passed. [" + runSummary + "]");
        }
        this.newRun = true;
        this.failures = 0;
        this.assumptions = 0;
        this.warnings = 0;
        this.errors = 0;

    }

    /**
     * this reports any currentTest that hasn't been flushed, usually the
     * run summary line, but it can also be called
     */
    public void finishReport() {
        if (this.currentTest != null)
            report(this.currentTest);
    }


    protected void report(AtomicTest test) {

        try {
            Element div = new Element("div");
            generateTestReportLine(test,div,testRowIndex++);
            this.serializer.writeChild(div);
            this.serializer.flush();
            this.out.write(this.serializer.getLineSeparator().getBytes());
            this.out.flush();
        }
        catch (IOException e) {
            String msg = "IOException trying to serialize results for " + test.getTestName();
            logger.error(msg, e);
            try {
                this.out.write(new String("<div>" + msg + "</div>").getBytes("UTF-8"));
            }
            catch (IOException e1) {
                logger.error("Could not report IOException to outputStream for test" + test.getTestName(),e1);
            }
        }

    }

    /*
     * this method fill the div with contents of the AtomicTest with the result
     * for each test / test header.
     *
     * adapted from webTester in MN package...
     *
     * see src/main/webapp/results_head.html for html formatting class definitions
     */
    private void generateTestReportLine(AtomicTest testResult, Element div, int rowIndex) {

        // make an html table
        Element table = new Element("table");
        Element tr = new Element("tr");
        Element nameColumn = new Element("th");
        Element messageColumn = new Element("td");

        // set format class based on the status
        if (testResult == null || testResult.getStatus() == null) {
            div.addAttribute(new Attribute("class", "uncategorized"));
        }
        else if (testResult.getStatus().equals("testStarted")) {
            div.addAttribute(new Attribute("class", "testPass"));
        }
        else if (testResult.getStatus().equals("testIgnored")) {
            div.addAttribute(new Attribute("class", "testIgnored"));
        }
        else if (testResult.getStatus().equals("testFailure")) {
            div.addAttribute(new Attribute("class", "testFailure"));
        }
        else if (testResult.getStatus().equals("testAssumptionFailure")) {
            div.addAttribute(new Attribute("class", "testAssumptionFailure"));
        }
        else if (testResult.getStatus().equals("summaryFail")) {
            div.addAttribute(new Attribute("class", "summaryFail"));
        }
        else if (testResult.getStatus().equals("summaryPass")) {
            div.addAttribute(new Attribute("class", "summaryPass"));
        }
        else if (testResult.getStatus().equals("summaryAllIgnored")) {
            div.addAttribute(new Attribute("class", "summaryAllIgnored"));
        }
        else if (testResult.getStatus().equals("summaryHasAssumptionViolations")) {
            div.addAttribute(new Attribute("class", "summaryHasAssumptionViolations"));
        }
        else if (testResult.getStatus().equals("runHeader")) {
            div.addAttribute(new Attribute("class", "runHeader"));
        }
        else {
            div.addAttribute(new Attribute("class", "uncategorized"));
        }

        // add contents to the table row...

        // first the test label
        nameColumn.appendChild(formatTestName(testResult));
        tr.appendChild(nameColumn);

        // add the test description annotation value if
        if (testResult.getStatus() != null && testResult.getTestDescription() != null) //getStatus().startsWith("test"))
            messageColumn.appendChild(formatDescriptionMouseover(testResult.getTestDescription()));

        // uniformly handle multi-line messages
        if (testResult.getMessage() != null && testResult.getMessage().contains("\n")) {
            Element formattedText = new Element("pre");
            formattedText.appendChild(testResult.getMessage().replace("\n","\r\n"));
            messageColumn.appendChild(formattedText);
        }
        else {
            messageColumn.appendChild(testResult.getMessage());
        }
        tr.appendChild(messageColumn);
        table.appendChild(tr);
        //		Element dateText = new Element("td");
        //		dateText.appendChild(new Date().toString());
        //		Element dateRow = new Element("tr");
        //		dateRow.appendChild(dateText);
        //		table.appendChild(dateRow);

        div.appendChild(table);

        // add trace information if any (in separate table in the div)
        if (testResult.getTrace() != null) {
            // append toggle link to existing description
            messageColumn.appendChild(buildTraceViewControl(rowIndex));
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
    private String formatTestName(AtomicTest t)
    {
        String finalLabel = t.getTestLabel();
        if (finalLabel == null) {

            String improvedTestName = null;
            if (t.getTestName().contains(":")) {
                // keep just the method and subtest
                finalLabel = t.getTestName().replaceAll(".*\\: ", "");
            } else {
                // keep the TestCase segment
                finalLabel = t.getTestName().replace(".*\\.", "");
            }
            finalLabel = finalLabel.replaceFirst("_", " : ");

            // underscores in subtest section of method name get converted to spaces
            finalLabel = finalLabel.replaceAll("_", " ");
            //		improvedTestName = humaniseCamelCase(improvedTestName);
        }
        return finalLabel;
    }

    private Element formatDescriptionMouseover(String description) {
        Element formattedHelp = new Element("span");
        formattedHelp.addAttribute(new Attribute("class", "dropt"));
        formattedHelp.addAttribute(new Attribute("title", ""));
        Element helpIcon = new Element("img");
        helpIcon.addAttribute(new Attribute("src","images/help.png"));
        formattedHelp.appendChild(helpIcon);
        Element mouseOverText = new Element("span");
        mouseOverText.addAttribute(new Attribute("style", "width: 500px;"));
        mouseOverText.appendChild(description == null ? "no test description available" : description);
        formattedHelp.appendChild(mouseOverText);
        return formattedHelp;
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

}

