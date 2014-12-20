package org.dataone.integration.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Iterator;

import org.dataone.configuration.Settings;
import org.dataone.integration.CommonCallAdapter;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.junit.Test;

/**
 * Base class for testing the core API functionality, maning MNCore and CNCore
 * across API versions.
 */
public abstract class CoreIT extends ContextAwareTestCaseDataone {

    // Test methods that need to be implemented:

    /**
     * Implementers can make use of the {@link CoreIT#testPing(Iterator, String)} method.
     */
    @Test
    public abstract void testPing();

    @Test
    public abstract void testGetLogRecords_AccessRestriction();
    
    @Test
    public abstract void testGetLogRecords();

    @Test
    public abstract void testGetLogRecords_Slicing();

    @Test
    public abstract void testGetLogRecords_eventFiltering();

    @Test
    public abstract void testGetLogRecords_pidFiltering();

    @Test
    public abstract void testGetLogRecords_dateFiltering();

    /**
     * Will test the ping call for all nodes. Requires an iterator to go through
     * all the nodes (this may iterate across either MN or CN nodes). Also requires
     * a version string so it knows against which version of API it should test ping. 
     * 
     * @param nodeIterator 
     *      an {@link Iterator} accross MN or CN {@link Node}s
     * @param version 
     *      either "v1" or "v2", to match the API version being tested
     */
    protected void testPing(Iterator<Node> nodeIterator, String version) {
        setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            pingImpl(nodeIterator.next(), version);
        }
    }

    /**
     * Will run the ping command and test for proper execution.
     * Makes use of {@link CommonCallAdapter} to call ping on the correct node type
     * (MN or CN) against the correct API version.
     * @param node 
     * @param version
     */
    private void pingImpl(Node node, String version) {

        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);

        String currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            //          Assume.assumeTrue(APITestUtils.isTierImplemented(mn, "Tier5"));
            Date localNow = new Date();
            Date pingDate = callAdapter.ping();

            assertTrue(callAdapter.getLatestRequestUrl() + " ping should return a valid date", pingDate != null);
            // other invalid dates will be thrown as IOExceptions cast to ServiceFailures

            assertTrue(callAdapter.getLatestRequestUrl()
                    + " returned date should be within 1 minute of time measured on test machine", pingDate.getTime()
                    - localNow.getTime() < 1000 * 60
                    && localNow.getTime() - pingDate.getTime() > -1000 * 60);

        } catch (BaseException e) {
            fail(callAdapter.getLatestRequestUrl() + " " + e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ":: " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            fail(currentUrl + " " + e.getClass().getName() + ": " + e.getMessage());
        }

    }

    /**
     * Tests that getLogRecords() implements access restricitons properly, testing
     * the negative case - where client is not a CN and is public.
     * Runs tests across all nodes in the given nodeIterator.
     * 
     * @param nodeIterator 
     *      an {@link Iterator} accross MN or CN {@link Node}s
     * @param version 
     *      either "v1" or "v2", to match the API version being tested
     */
    protected void testGetLogRecords_AccessRestriction(Iterator<Node> nodeIterator, String version) {
        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
        setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            testGetLogRecords_AccessRestriction(nodeIterator.next(), version);
        }
    }

    /**
     * Tests that getLogRecords() implements access restriction properly, testing
     * the negative case - where client is not a CN and is public.
     */
    private void testGetLogRecords_AccessRestriction(Node node, String version)
    {
        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetLogRecords_AccessRestriction(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Log eventLog = callAdapter.getLogRecords(null, null, null, null, null, null, null);
            checkTrue(callAdapter.getLatestRequestUrl(), "getLogRecords without a client certificate"
                    + "should return a Log datatype or NotAuthorized", eventLog != null);

            //check that the identifiers in the log entries are all public read
            if (eventLog.getLogEntryList().size() > 0) {
                LogEntry currentEntry = null;
                try {
                    for (LogEntry le : eventLog.getLogEntryList()) {
                        currentEntry = le;
                        try {
                            // TODO where does the session come from?
                            callAdapter.describe(null, le.getIdentifier());
                        } catch (NotFound e) {
                            ; // a semi-valid repsonse.  Sometimes logged objects have been deleted.
                        }
                    }
                } catch (NotAuthorized e) {
                    handleFail(
                            callAdapter.getLatestRequestUrl(),
                            String.format("The returned log should not contain log entries which "
                                    + "are not publicly available.  Got entry %s for identifier %s",
                                    currentEntry.getEntryId(), currentEntry.getIdentifier().getValue()));
                }
            }
        } catch (NotAuthorized e) {
            ; // a valid response, where access is restricted to CNs
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + e.getDetail_code()
                    + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
     * Runs tests across all nodes in the given nodeIterator.
     * 
     * @param nodeIterator 
     *      an {@link Iterator} accross MN or CN {@link Node}s
     * @param version 
     *      either "v1" or "v2", to match the API version being tested
     */
    protected void testGetLogRecords(Iterator<Node> nodeIterator, String version) {
        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn",
                "urn:node:cnStageUNM1");
        setupClientSubject(cnSubject);
        while (nodeIterator.hasNext()) {
            testGetLogRecords(nodeIterator.next(), version);
        }
    }

    /**
     * Tests that getLogRecords() returns Log object, using the simplest case: no parameters.
     * Also tests with all parameters are set.  Passes the tests by returning a Log object.
     */
    private void testGetLogRecords(Node node, String version) {

        //TODO: change to use a testCNAdmin certificate

        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            Log eventLog = callAdapter.getLogRecords(null, null, null, null, null, null, null);
            checkTrue(callAdapter.getLatestRequestUrl(), "getLogRecords should return a log datatype", eventLog != null);

            Date fromDate = new Date();
            Thread.sleep(1000);
            Date toDate = new Date();
            // TODO need a Session...
            eventLog = callAdapter.getLogRecords(null, fromDate, toDate, Event.READ.toString(), "pidFilter", 0, 10);
            checkTrue(callAdapter.getLatestRequestUrl(), "getLogRecords(<parameters>) returns a Log", eventLog != null);
        } catch (NotAuthorized e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "Should not get a NotAuthorized when connecting"
                            + "with a cn admin subject . Check NodeList and MN configuration.  Msg details:"
                            + e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

    //    /**
    //     * Tests that count and start parameters are functioning, and getCount() and getTotal()
    //     * are reasonable values.
    //     */
    //    //    @Test
    //    public void testGetLogRecords_Slicing()
    //    {
    //        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
    //        // TODO: change to testCnAdmin subject when obtained
    //        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "urn:node:cnStageUNM1");
    //        setupClientSubject(cnSubject);
    ////      setupClientSubject_NoCert();
    //        Iterator<Node> it = getMemberNodeIterator();
    //        while (it.hasNext()) {
    //            currentUrl = it.next().getBaseURL();
    //            MNode mn = new MNode(currentUrl);
    //            printTestHeader("testGetLogRecords_Slicing(...) vs. node: " + currentUrl);  
    //            currentUrl = mn.getNodeBaseServiceUrl();
    //
    //            try {
    //                Log eventLog = mn.getLogRecords(null, null, null, null, null, null);            
    //                
    //                StringBuffer sb = new StringBuffer();
    //                int i = 0;
    //                if (eventLog.getCount() != eventLog.sizeLogEntryList())
    //                    sb.append(++i + ". 'count' attribute should equal the number of LogEntry objects returned.  \n");
    //            
    //                if (eventLog.getTotal() < eventLog.getCount())
    //                    sb.append(++i + ". 'total' attribute should be >= the 'count' attribute in the returned Log.  \n");
    //
    //                if (eventLog.getTotal() < eventLog.sizeLogEntryList())
    //                    sb.append(++i + "'total' attribute should be >= the number of LogEntry objects returned.  \n");
    //
    //
    //                // test that one can limit the count
    //                int halfCount = eventLog.sizeLogEntryList() / 2; // rounds down
    //                eventLog = mn.getLogRecords(null, null, null, null, 0, halfCount);
    //
    //                if (eventLog.sizeLogEntryList() != halfCount)
    //                    sb.append(++i + ". should be able to limit the number of returned LogEntry objects using 'count' parameter.");
    //                                    
    //                // TODO:  test that 'start' parameter does what it says
    //
    //                // TODO: paging test
    //                
    //                
    //                if (i > 0) {
    //                    handleFail(mn.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
    //                }               
    //                
    //            }
    //            catch (NotAuthorized e) {
    //                handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    //                        "with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch (BaseException e) {
    //                handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch(Exception e) {
    //                e.printStackTrace();
    //                handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    //            }              
    //        }
    //    }
    //    
    //    
    //    /**
    //     * Tier 1 MNs might only have READ events, so will get the log records from
    //     * a given period and if only one type, filter for a different one an expect 
    //     * zero returned.  If 2 types, just expect fewer records.
    //     * Must be careful to check that all the records requested are returned.
    //     */
    ////    @Test
    //    public void testGetLogRecords_eventFiltering()
    //    {
    //        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
    //        // TODO: change to testCnAdmin subject when obtained
    //        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "urn:node:cnStageUNM1");
    //        setupClientSubject(cnSubject);
    ////      setupClientSubject_NoCert();
    //        Iterator<Node> it = getMemberNodeIterator();
    //        while (it.hasNext()) {
    //            currentUrl = it.next().getBaseURL();
    //            MNode mn = D1Client.getMN(currentUrl);  
    //            currentUrl = mn.getNodeBaseServiceUrl();
    //            printTestHeader("testGetLogRecords_eventFiltering() vs. node: " + currentUrl);
    //
    //            try {
    //                Date toDate = new Date();
    //
    //                // using the paged-implementation to make sure we get them all.
    //                Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    //
    //                if (entries.getCount() == 0) {
    //                    // try to create a log event
    //                    // if it can't it will throw a TestIterationEndingException
    //                    Identifier pid = procurePublicReadableTestObject(mn,null);
    //                    mn.get(pid);
    //                    toDate = new Date();
    //                    entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    //                }
    //
    //                Event targetType = entries.getLogEntry(0).getEvent();
    //                Event otherType = null;
    //
    //                for (LogEntry le : entries.getLogEntryList()) {
    //                    if ( ! le.getEvent().equals(targetType) ) {
    //                        otherType = le.getEvent();
    //                        break;
    //                    }
    //                }
    //
    //                if (otherType == null) {
    //                    if (targetType.equals(Event.READ)) {
    //                        entries = mn.getLogRecords(null, null, toDate, Event.CREATE, null, 0, 0);
    //                        checkEquals(mn.getLatestRequestUrl(),"Log contains only READ events, " +
    //                                "so should get 0 CREATE events", String.valueOf(entries.getTotal()),"0");
    //                    } else {
    //                        entries = mn.getLogRecords(null, null, toDate, Event.READ, null, 0, 0);
    //                        checkEquals(mn.getLatestRequestUrl(),"Log contains only " + targetType + " events, " +
    //                                "so should get 0 READ events",String.valueOf(entries.getTotal()),"0");
    //                    }
    //                } else {
    //                    entries = APITestUtils.pagedGetLogRecords(mn, null, toDate ,targetType, null, null, null);
    //                    boolean oneTypeOnly = true;
    //                    Event unfilteredType = null;
    //                    for (LogEntry le: entries.getLogEntryList()) {
    //                        if (!le.getEvent().equals(targetType)) {
    //                            oneTypeOnly = false;
    //                            unfilteredType = le.getEvent();
    //                            break;
    //                        }
    //                    }
    //                    checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should contain only " +
    //                            "logs of type " + targetType.xmlValue() + ". Got " + unfilteredType, oneTypeOnly);
    //                }
    //            }
    //            catch (NotAuthorized e) {
    //                handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    //                        "with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch (BaseException e) {
    //                handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch(Exception e) {
    //                e.printStackTrace();
    //                handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    //            }
    //        }
    //    }
    //    
    //    /**
    //     * Test that pidFilter only returns objects starting with the given string
    //     * Want to find a negative case and to make sure it is filtered out when the
    //     * filter is applied.
    //     */
    ////    @Test
    //    public void testGetLogRecords_pidFiltering()
    //    {
    //        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
    //        // TODO: change to testCnAdmin subject when obtained
    //        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "urn:node:cnStageUNM1");
    //        setupClientSubject(cnSubject);
    ////      setupClientSubject_NoCert();
    //        Iterator<Node> it = getMemberNodeIterator();
    //        while (it.hasNext()) {
    //            currentUrl = it.next().getBaseURL();
    //            MNode mn = D1Client.getMN(currentUrl);  
    //            currentUrl = mn.getNodeBaseServiceUrl();
    //            printTestHeader("testGetLogRecords_pidFiltering() vs. node: " + currentUrl);
    //
    //            try {
    //                Date t0 = new Date();
    //                Date toDate = t0;
    ////              Date fromDate = t0;
    //
    //                Log entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    //
    //                if (entries.getTotal() == 0) {
    //                    // try to create a log event
    //                    // if it can't it will throw a TestIterationEndingException
    //                    Identifier pid = procurePublicReadableTestObject(mn,null);
    //                    mn.get(pid);
    //                    toDate = new Date();
    //                    entries = APITestUtils.pagedGetLogRecords(mn, null, toDate, null, null, null, null);
    //                }
    //
    //                // should have at least one log entry at this point
    //                if (entries.sizeLogEntryList() > 0) {
    //                    Identifier targetIdentifier = entries.getLogEntry(0).getIdentifier();
    //                    Identifier otherIdentifier = null;
    //
    //                    for (LogEntry le : entries.getLogEntryList()) {
    //                        if (! le.getIdentifier().equals(targetIdentifier) ) {
    //                            otherIdentifier = le.getIdentifier();
    //                            break;
    //                        }
    //                    }
    //
    //                    if (otherIdentifier == null) {
    //                        // create a new target that is non existent
    //                        otherIdentifier = targetIdentifier;
    //                        targetIdentifier = D1TypeBuilder.buildIdentifier(targetIdentifier.getValue()
    //                                + new Date().getTime());
    //
    //                        entries = mn.getLogRecords(null, null, t0, 
    //                                null, targetIdentifier.getValue(), 0, 0);
    //                        checkEquals(mn.getLatestRequestUrl(),"Log should be empty for the derived identifier pattern " +
    //                                targetIdentifier.getValue(),String.valueOf(entries.getTotal()),"0");
    //
    //                    } 
    //                    else {
    //                        entries = mn.getLogRecords(null, null, toDate, 
    //                                null, targetIdentifier.getValue(), null, null);
    //                        boolean oneTypeOnly = true;
    //                        if (entries.sizeLogEntryList() > 0) {
    //                            for (LogEntry le: entries.getLogEntryList()) {
    //                                if (!le.getIdentifier().equals(targetIdentifier)) {
    //                                    oneTypeOnly = false;
    //                                    break;
    //                                }
    //                            }
    ////                          checkTrue(mn.getLatestRequestUrl(), "Filtered log for the time period should " +
    //                            //                          "contain only entries for the target identifier: " + targetIdentifier.getValue(),
    //                            //                          oneTypeOnly);
    //                            checkTrue(mn.getLatestRequestUrl(), "The optional pidFilter parameter is not filtering log records. " +
    //                                    "The log would otherwise contain only entries for the target identifier: " + targetIdentifier.getValue(),
    //                                    oneTypeOnly);
    //                        } else {
    //                            handleFail(mn.getLatestRequestUrl(), "should still get a LogEntry when applying 'pidFilter' parameter");
    //                        }
    //                    }
    //                }
    //                else {
    //                    handleFail(mn.getLatestRequestUrl(), "After successfully reading an object, should " +
    //                            "have at least one log record.  Got zero");
    //                }
    //            }
    //            catch (NotAuthorized e) {
    //                handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    //                        "with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch (BaseException e) {
    //                handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch(Exception e) {
    //                e.printStackTrace();
    //                handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    //            }              
    //        }
    //    }
    //
    //
    //
    ////    @Test
    //    public void testGetLogRecords_DateFiltering()
    //    {
    //        Settings.getConfiguration().setProperty("D1Client.D1Node.getLogRecords.timeout", "60000");
    //        // TODO: change to testCnAdmin subject when obtained
    //        String cnSubject = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "urn:node:cnStageUNM1");
    //        setupClientSubject(cnSubject);
    ////      setupClientSubject_NoCert();
    //        Iterator<Node> it = getMemberNodeIterator();
    //        while (it.hasNext()) {
    //            currentUrl = it.next().getBaseURL();
    //            MNode mn = new MNode(currentUrl);
    //            printTestHeader("testGetLogRecords_DateFiltering(...) vs. node: " + currentUrl);  
    //            currentUrl = mn.getNodeBaseServiceUrl();
    //
    //            try {
    //                Log eventLog = mn.getLogRecords(null, null, null, null, null, null);
    //                
    //                if (eventLog.getLogEntryList().size() == 0) {
    //                    
    //                    // read an existing object
    //                    try {
    //                        String objectIdentifier = "TierTesting:" + 
    //                            createNodeAbbreviation(mn.getNodeBaseServiceUrl()) +
    //                            ":Public_READ" + testObjectSeriesSuffix;
    //                        Identifier id = procurePublicReadableTestObject(mn,D1TypeBuilder.buildIdentifier(objectIdentifier));
    //                        InputStream is = mn.get(null, id);
    //                        is.close();
    //                        Thread.sleep(1000); // just in case...
    //                        eventLog = mn.getLogRecords(null, null, null, null, null, null);
    //                    }
    //                    catch (TestIterationEndingException e) {
    //                        ;  // 
    //                    }
    //                }
    //                    
    //                if (eventLog.getLogEntryList().size() == 0) {
    //                    // still zero?  something's probably wrong
    //                    handleFail(mn.getLatestRequestUrl(),"the event log contains no entries after trying to read an object");
    //                    
    //                } else {
    //                    // try to find log entries with different dates, should be quick...
    //                    LogEntry entry0 = eventLog.getLogEntry(0);
    //                    Date fromDate = null;
    //                    LogEntry excludedEntry = null;
    //                    for (LogEntry le: eventLog.getLogEntryList()) {
    //                        if (!le.getDateLogged().equals(entry0.getDateLogged())) {
    //                            // which is earlier?  can't assume chronological order of the list
    //                            if (le.getDateLogged().after(entry0.getDateLogged())) {
    //                                fromDate = le.getDateLogged();
    //                                excludedEntry = entry0;
    //                            } else {
    //                                fromDate = entry0.getDateLogged();
    //                                excludedEntry = le;
    //                            }
    //                            break;
    //                        }
    //                    }
    //                    
    //                    
    //                    if (excludedEntry == null) {
    //                        handleFail(mn.getLatestRequestUrl(),"could not find 2 objects with different dateLogged times");
    //                    } 
    //                    else {
    //
    //                        // call with a fromDate
    //                        eventLog = mn.getLogRecords(fromDate, null, null, null, null, null);
    //
    //                        for (LogEntry le : eventLog.getLogEntryList()) {
    //                            if (le.getEntryId().equals(excludedEntry.getEntryId())) {
    //                                handleFail(mn.getLatestRequestUrl(),"entryID " + excludedEntry.getEntryId() +
    //                                        " at " + excludedEntry.getDateLogged() + 
    //                                        " should not be in the event log where fromDate set to " + fromDate);
    //                                break;
    //                            }
    //                        }
    //                    }
    //                } 
    //            } 
    //            catch (NotAuthorized e) {
    //                handleFail(mn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    //                        "with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch (BaseException e) {
    //                handleFail(mn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    //                        e.getDetail_code() + ": " + e.getDescription());
    //            }
    //            catch(Exception e) {
    //                e.printStackTrace();
    //                handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    //            }              
    //        }
    //    }
    
}
