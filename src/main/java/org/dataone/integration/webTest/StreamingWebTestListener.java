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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.apache.commons.lang.StringUtils;
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
    
    private enum lineStatus {
        TEST_PASS, TEST_WARN, TEST_FAIL, TEST_IGNORE, SUMMARY_PASS, SUMMARY_WARN,
        SUMMARY_FAIL, TC_HEADER, DESCRIPTION
    }


    public StreamingWebTestListener(OutputStream os) throws ValidityException, ParsingException, IOException {
        super();

        this.out = os;

        // to allow incremental rendering, start writing output to the output stream
        this.serializer = new StreamableSerializer(out);
        this.serializer.setIndent(2); // pretty-print output

    }

    public void testStarted(Description d) {
        if (currentTest != null) {
            report(currentTest);
            //			testList.add(currentTest);
        }
        testCaseName = d.getClassName();
        if (newRun) {
            currentTest = new AtomicTest(testCaseName);
            currentTest.setStatus("Header");
            currentTest.setMessage("SOURCE");
            report(currentTest);
            //			testList.add(currentTest);
            newRun = false;
        }
        currentTest = new AtomicTest(testCaseName + ": " + d.getMethodName());
        currentTest.setType("Test");
        currentTest.setStatus("Success");

        this.newRun = false;
    }

    public void testRunFinished(Result r) {
        if (currentTest != null) {
            report(currentTest);
            //			testList.add(currentTest);
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
            currentTest.setMessage("Tier Passed. [" + runSummary + "]");
        }
        this.newRun = true;
    }

    public void testIgnored(Description d) {
        if (currentTest != null) {
            report(currentTest);
            //			testList.add(currentTest);
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

    //	public ArrayList<AtomicTest> getTestList() {
    //		if (currentTest != null) {
    //			report(currentTest);
    ////			testList.add(currentTest);
    //			currentTest = null;
    //		}
    //		return testList;
    //	}

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
        if (testResult == null) {
            div.addAttribute(new Attribute("class", "greyDescr"));
        }
        else if (testResult.getStatus().equals("Success")) {
            if (testResult.getMessage() != null && testResult.getMessage().contains("Tier Passed")) {
                div.addAttribute(new Attribute("class","summaryPass"));
            } else {
                div.addAttribute(new Attribute("class", "testPass"));
            }
        }
        else if (testResult.getStatus().equals("Ignored")) {
            div.addAttribute(new Attribute("class", "testIgnore"));
        }
        else if (testResult.getStatus().equals("Failed")) {
            if (testResult.getMessage() != null && testResult.getMessage().contains("Failed Tier")) {
                div.addAttribute(new Attribute("class","summaryFail"));
            } else {
                div.addAttribute(new Attribute("class", "testFail"));
            }
        }
        else if (testResult.getStatus().equals("Error")) {
            div.addAttribute(new Attribute("class", "testWarn"));
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

        if (testResult.getMessage() != null && testResult.getMessage().contains("\n")) {
            Element formattedText = new Element("pre");
            formattedText.appendChild(testResult.getMessage().replace("\n","\r\n"));
            description.appendChild(formattedText);
        } else if (testResult.getMessage() == "SOURCE") {
            Element testCaseLink = new Element("a");
            String bareTestCaseName = StringUtils.substringAfterLast(testResult.getTestName(), ".");
            testCaseLink.addAttribute(new Attribute("href","webTesterSources/"+ bareTestCaseName + ".java"));
            testCaseLink.appendChild("test case source");
            Element allSourceLink = new Element("a");
            allSourceLink.addAttribute(new Attribute("href","webTesterSources"));
            allSourceLink.appendChild("all sources");

            description.appendChild(testCaseLink);
            description.appendChild(" / ");
            description.appendChild(allSourceLink);
            //			attachFormattedSourceLinks(description,testResult.getTestName());
        } else {
            description.appendChild(testResult.getMessage());
        }
        tr.appendChild(description);
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
        } else {
            // keep the TestCase segment
            improvedTestName = rawTestName.replace(".*\\.", "");
        }
        improvedTestName = improvedTestName.replaceFirst("_", " : ");

        // underscores in subtest section of method name get converted to spaces
        improvedTestName = improvedTestName.replaceAll("_", " ");
        //		improvedTestName = humaniseCamelCase(improvedTestName);
        return improvedTestName;
    }

    private void attachFormattedSourceLinks() {


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

