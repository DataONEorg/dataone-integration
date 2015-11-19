package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class LogAggregationFunctionalTestImplementations extends ContextAwareTestCaseDataone {

    private CNCallAdapter cn;
    private List<Node> mns;
    private List<Node> v1mns;
    private List<Node> v2mns;
    private List<Node> v1v2mns;
    private static final long LOG_AGG_WAIT = 4 * 60000;    // 5 minutes
    
    @Override
    protected String getTestDescription() {
        return "Runs create/read/etc methods agains MNs in the environment and verifies "
                + "that the corresponding logs end up on a CN.";
    }

    public void setup(Iterator<Node> cnIter) {

        mns = new ArrayList<Node>();
        v1mns = new ArrayList<Node>();
        v2mns = new ArrayList<Node>();
        v1v2mns = new ArrayList<Node>();
        
        List<Node> cnList = IteratorUtils.toList(cnIter);
        assertTrue("Test requires at least one CN, but got zero CNs.", cnList.size() != 0);
            
        log.info("Using CN " + cnList.get(0).getBaseURL() + " for tests");
        cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        
        List<Node> mnList = new ArrayList<Node>();
        try {
            for(Node n : cn.listNodes().getNodeList())
                if(n.getType() == NodeType.MN)
                    mnList.add(n);
        } catch (Exception e) {
            throw new AssertionError("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }
        
        for (Node mNode : mnList) {
            MNCallAdapter v1mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
            MNCallAdapter v2mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
            
            Node v1capabilities = null;
            Node v2capabilities = null;
            
            try {
                v1mn.ping();
                Node cap = v1mn.getCapabilities();
                List<Service> serviceList = cap.getServices().getServiceList();
                for (Service service : serviceList) {
                    if ("MNCore".equals(service.getName())          // getLogRecords 
                            && "v1".equals(service.getVersion())
                            && service.getAvailable()) {
                        v1capabilities = cap;
                        break;
                    }
                }
            } catch (Exception e1) {
                log.info("Unable to assess v1 capabilities for MN: " + v1mn.getNodeBaseServiceUrl() 
                        + " : " + e1.getClass().getSimpleName() + " : " + e1.getMessage());
            }
            
            try {
                v2mn.ping();
                Node cap = v2mn.getCapabilities();
                List<Service> serviceList = cap.getServices().getServiceList();
                for (Service service : serviceList) {
                    if ("MNCore".equals(service.getName())          // getLogRecords 
                            && "v2".equals(service.getVersion())
                            && service.getAvailable()) {
                        v2capabilities = cap;
                        break;
                    }
                }
            } catch (Exception e1) {
                log.info("Unable to assess v2 capabilities for MN: " + v1mn.getNodeBaseServiceUrl() 
                        + " : " + e1.getClass().getSimpleName() + " : " + e1.getMessage());
            }
            
            if (v1capabilities != null && v2capabilities != null) {
                mns.add(v2capabilities);
                v1v2mns.add(v2capabilities);
            } else if (v1capabilities != null) {
                mns.add(v1capabilities);
                v1mns.add(v1capabilities);
            } else if (v2capabilities != null) {
                mns.add(v2capabilities);
                v2mns.add(v2capabilities);
            }
        }

        log.info("MNs available:   " + mns.size());
        for (Node n : mns)
            log.info("MN:   " + n.getBaseURL());
        
        assertTrue("Test requires that the environment has at least one MN to work with that "
                + "supports logging for CN log aggregation. Found: " + mns.size(), 
                mns.size() >= 1);
    }

    /**
     * Helper method to return an {@link MNCallAdapter} for either a v1 or v2 MN in the system.
     * Will preferentially return one for an MN that supports v1 AND v2 APIs (which will call the v2 API),
     * followed by just v2, followed by just v1.  
     */
    private MNCallAdapter getMNCallAdapter(String certificateFilename) {
        
        if (v1v2mns.size() > 0)
            return new MNCallAdapter(getSession(certificateFilename), v1v2mns.get(0), "v2");
        if (v2mns.size() > 0)
            return new MNCallAdapter(getSession(certificateFilename), v2mns.get(0), "v2");
        if (v1mns.size() > 0) {
            log.warn("only v1 MN available for use! (" + v1mns.get(0).getBaseURL() + ") "
                    + "libclient used by tests calls the /v1/log and /v2/log endpoint with the"
                    + "idFilter parameter. The v1 MN may still be using the pidFilter parameter!");
            return new MNCallAdapter(getSession(certificateFilename), v1mns.get(0), "v1");
        }
        
        throw new AssertionError("Not enough (responsive) MNs to test with in this environment.");
    }
    
    @WebTestName("getLogRecords() user-based access")
    @WebTestDescription("Tests that an authenticated user may only view their own records "
            + "or public records, and that a CN caller may view all records. Note: allowing "
            + "access to more than the CN is optional for an MN, so authentication failures for "
            + "non-CN subjects are ignored.")
    public void testMnGetLogRecords_Access() {

        MNCallAdapter mnCnCaller = getMNCallAdapter(cnSubmitter);
        MNCallAdapter mnPublicCaller = getMNCallAdapter(Constants.SUBJECT_PUBLIC);
        MNCallAdapter mnTestPersonCaller = getMNCallAdapter("testPerson");
        MNCallAdapter mnTestRightsHolderCaller = getMNCallAdapter("testRightsHolder");
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testPersonAccessRule = new AccessRule();
        getSession("testPerson");
        testPersonAccessRule.addSubject(getSubject("testPerson"));
        testPersonAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testRightsHolderAccessRule = new AccessRule();
        getSession("testRightsHolder");
        testRightsHolderAccessRule.addSubject(getSubject("testRightsHolder"));
        testRightsHolderAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Identifier publicPid = D1TypeBuilder.buildIdentifier("testMnGetLogRecords_Access_public");
        Identifier testPersonPid = D1TypeBuilder.buildIdentifier("testMnGetLogRecords_Access_testPerson");
        Identifier testRightsHolderPid = D1TypeBuilder.buildIdentifier("testMnGetLogRecords_Access_testRightsHolder");
        try {
            publicPid = procureTestObject(mnCnCaller, publicAccessRule, publicPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + publicPid.getValue() + ", " + e.getClass().getSimpleName()
                    + " : " + e.getMessage(), e);
        }
        try {
            testPersonPid = procureTestObject(mnCnCaller, testPersonAccessRule, testPersonPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + testPersonPid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            testRightsHolderPid = procureTestObject(mnCnCaller, testRightsHolderAccessRule, testRightsHolderPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + testRightsHolderPid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        ArrayList<String> errors = new ArrayList<String>();
        
        Log publicLog = null;
        try {
            publicLog = mnPublicCaller.getLogRecords(null, null, null, null, publicPid.getValue(), null, null);
        } catch (NotAuthorized nf) {
            // expected
        } catch (Exception e) {
            log.warn(mnPublicCaller.getLatestRequestUrl() + " Unable to fetch Log records for public subject. " 
                    + "Expected a NotAuthorized (only CN or MN admin should have access) but got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log testPersonLog = null;
        try {
            testPersonLog = mnTestPersonCaller.getLogRecords(null, null, null, null, testPersonPid.getValue(), null, null);
        } catch (NotAuthorized nf) {
            // expected
        } catch (Exception e) {
            log.warn(mnTestPersonCaller.getLatestRequestUrl() + " Unable to fetch Log records for testPerson subject. " 
                    + "Expected a NotAuthorized (only CN or MN admin should have access) but got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log testRightsHolderLog = null;
        try {
            testRightsHolderLog = mnTestRightsHolderCaller.getLogRecords(null, null, null, null, testRightsHolderPid.getValue(), null, null);
        } catch (NotAuthorized nf) {
            // expected
        } catch (Exception e) {
            log.warn(mnTestRightsHolderCaller.getLatestRequestUrl() + " Unable to fetch Log records for testRightsHolder subject. " 
                    + "Expected a NotAuthorized (only CN or MN admin should have access) but got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log cnLogPublicPid = null;
        Log cnLogTestPersonPid = null;
        Log cnLogTestRightsHolderPid = null;
        try {
            cnLogPublicPid = mnCnCaller.getLogRecords(null, null, null, null, publicPid.getValue(), null, null);
        } catch (Exception e) {
            throw new AssertionError(mnCnCaller.getLatestRequestUrl() + " Unable to fetch Log records for CN subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            cnLogTestPersonPid = mnCnCaller.getLogRecords(null, null, null, null, testPersonPid.getValue(), null, null);
        } catch (Exception e) {
            throw new AssertionError(mnCnCaller.getLatestRequestUrl() + " Unable to fetch Log records for CN subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            cnLogTestRightsHolderPid = mnCnCaller.getLogRecords(null, null, null, null, testRightsHolderPid.getValue(), null, null);
        } catch (Exception e) {
            throw new AssertionError(mnCnCaller.getLatestRequestUrl() + " Unable to fetch Log records for CN subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        if (publicLog != null) {
            boolean publicPidFound = false;
            boolean testPersonPidFound = false;
            boolean testRightsHolderPidFound = false;
            
            for (LogEntry logEntry : publicLog.getLogEntryList()) {
                if (logEntry.getIdentifier().equals(publicPid))
                    publicPidFound = true;
                if (logEntry.getIdentifier().equals(testPersonPid))
                    testPersonPidFound = true;
                if (logEntry.getIdentifier().equals(testRightsHolderPid))
                    testRightsHolderPidFound = true;
            }
            if (!publicPidFound)
                errors.add("Public subject should have access to public-accessible object we created.");
            if (testPersonPidFound)
                errors.add("Public subject should NOT have access to testPerson-accessible object we created.");
            if (testRightsHolderPidFound)
                errors.add("Public subject should NOT have access to testRightsHolder-accessible object we created.");
        }
        
        if (testPersonLog != null) {
            boolean publicPidFound = false;
            boolean testPersonPidFound = false;
            boolean testRightsHolderPidFound = false;
            
            for (LogEntry logEntry : testPersonLog.getLogEntryList()) {
                if (logEntry.getIdentifier().equals(publicPid))
                    publicPidFound = true;
                if (logEntry.getIdentifier().equals(testPersonPid))
                    testPersonPidFound = true;
                if (logEntry.getIdentifier().equals(testRightsHolderPid))
                    testRightsHolderPidFound = true;
            }
            if (!publicPidFound)
                errors.add("testPerson subject should have access to public-accessible object we created.");
            if (testPersonPidFound)
                errors.add("testPerson subject should have access to testPerson-accessible object we created.");
            if (testRightsHolderPidFound)
                errors.add("testPerson subject should NOT have access to testRightsHolder-accessible object we created.");
        }
        
        if (testRightsHolderLog != null) {
            boolean publicPidFound = false;
            boolean testPersonPidFound = false;
            boolean testRightsHolderPidFound = false;
            
            for (LogEntry logEntry : testRightsHolderLog.getLogEntryList()) {
                if (logEntry.getIdentifier().equals(publicPid))
                    publicPidFound = true;
                if (logEntry.getIdentifier().equals(testPersonPid))
                    testPersonPidFound = true;
                if (logEntry.getIdentifier().equals(testRightsHolderPid))
                    testRightsHolderPidFound = true;
            }
            if (!publicPidFound)
                errors.add("testRightsHolder subject should have access to public-accessible object we created.");
            if (!testPersonPidFound)
                errors.add("testRightsHolder subject should NOT have access to testPerson-accessible object we created.");
            if (!testRightsHolderPidFound)
                errors.add("testRightsHolder subject should have access to testRightsHolder-accessible object we created.");
        }
        
        boolean publicPidFound = false;
        if (cnLogPublicPid != null && cnLogPublicPid.getLogEntryList() != null) {
            for (LogEntry logEntry : cnLogPublicPid.getLogEntryList())
                if (logEntry.getIdentifier().equals(publicPid))
                    publicPidFound = true;
        }
        if (!publicPidFound)
            errors.add("CN subject (" + getSubject(cnSubmitter).getValue() + ") should have access to public-accessible object we created on " + mnCnCaller.getNodeBaseServiceUrl() + "."
                    + " pid: " + publicPid.getValue());
        
        boolean testPersonPidFound = false;
        if (cnLogTestPersonPid != null && cnLogTestPersonPid.getLogEntryList() != null) {
            for (LogEntry logEntry : cnLogTestPersonPid.getLogEntryList())
                if (logEntry.getIdentifier().equals(testPersonPid))
                    testPersonPidFound = true;
        }
        if (!testPersonPidFound)
            errors.add("CN subject (" + getSubject(cnSubmitter).getValue() + ") should have access to testPerson-accessible object we created on " + mnCnCaller.getNodeBaseServiceUrl() + "."
                    + " pid: " + testPersonPid.getValue());
        
        boolean testRightsHolderPidFound = false;
        if (cnLogTestRightsHolderPid != null && cnLogTestRightsHolderPid.getLogEntryList() != null) {
            for (LogEntry logEntry : cnLogTestRightsHolderPid.getLogEntryList())
                if (logEntry.getIdentifier().equals(testRightsHolderPid))
                    testRightsHolderPidFound = true;
        }
        if (!testRightsHolderPidFound)
            errors.add("CN subject (" + getSubject(cnSubmitter).getValue() + ") should have access to testRightsHolder-accessible object we created on " + mnCnCaller.getNodeBaseServiceUrl() + "."
                    + " pid: " + testRightsHolderPid.getValue());
    
        if (errors.size() > 0) {
            String errorString = "";
            for (String err : errors)
                errorString += err + "\n";
            throw new AssertionError("testMnGetLogRecords_Access ran into " + errors.size() + " errors:\n" + errorString);
        }
    }

    @WebTestName("query() user-based access")
    @WebTestDescription("Tests that an authenticated user may only view their own records "
            + "or public records, and that a CN caller may view all records.")
    public void testMnQuery_Access() {

        MNCallAdapter mnCnCaller = getMNCallAdapter(cnSubmitter);
        MNCallAdapter mnPublicCaller = getMNCallAdapter(Constants.SUBJECT_PUBLIC);
        MNCallAdapter mnTestPersonCaller = getMNCallAdapter("testPerson");
        MNCallAdapter mnTestRightsHolderCaller = getMNCallAdapter("testRightsHolder");
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testPersonAccessRule = new AccessRule();
        getSession("testPerson");
        testPersonAccessRule.addSubject(getSubject("testPerson"));
        testPersonAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testRightsHolderAccessRule = new AccessRule();
        getSession("testRightsHolder");
        testRightsHolderAccessRule.addSubject(getSubject("testRightsHolder"));
        testRightsHolderAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Identifier publicPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_public_");
        Identifier testPersonPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_testPerson_");
        Identifier testRightsHolderPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_testRightsHolder_");
        
        try {
            publicPid = procureTestObject(mnCnCaller, publicAccessRule, publicPid, cnSubmitter, "public", null);
            log.info("procured test object: " + publicPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + publicPid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            getSession("testPerson");
            testPersonPid = procureTestObject(mnCnCaller, testPersonAccessRule, testPersonPid, cnSubmitter, getSubject("testPerson").getValue(), null);
            log.info("procured test object: " + testPersonPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + testPersonPid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            getSession("testRightsHolder");
            testRightsHolderPid = procureTestObject(mnCnCaller, testRightsHolderAccessRule, testRightsHolderPid, cnSubmitter, getSubject("testRightsHolder").getValue(), null);
            log.info("procured test object: " + testRightsHolderPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + testRightsHolderPid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // sysmeta for debug purposes:
        SystemMetadata sysmetaPublicObj = null;
        SystemMetadata sysmetaTestPersonObj = null;
        SystemMetadata sysmetaTestRightsHolderObj = null;
        try {
            sysmetaPublicObj = mnCnCaller.getSystemMetadata(null, publicPid);
            sysmetaTestPersonObj = mnCnCaller.getSystemMetadata(null, testPersonPid);
            sysmetaTestRightsHolderObj = mnCnCaller.getSystemMetadata(null, testRightsHolderPid);
        } catch (Exception e) {
            log.warn("failed to fetch sysmeta", e);
        }
        
        String publicPidEncoded = null;
        String testPersonPidEncoded = null;
        String testRightsHolderPidEncoded = null;
        try {
            publicPidEncoded = URLEncoder.encode(publicPid.getValue(), "UTF-8");
            testPersonPidEncoded = URLEncoder.encode(testPersonPid.getValue(), "UTF-8");
            testRightsHolderPidEncoded = URLEncoder.encode(testRightsHolderPid.getValue(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            throw new AssertionError("testMnQuery_Access() unable to encode identifiers using UTF-8", e1);
        }
        
        ArrayList<String> errors = new ArrayList<String>();
        
        InputStream publicQueryPublicObj = null;
        InputStream publicQueryPersonObj = null;
        InputStream publicQueryRightsHolderObj = null;
        LogContents publicObjContents = null; 
        LogContents personObjContents = null; 
        LogContents rightsHolderObjContents = null; 
        try {
            publicQueryPublicObj = mnPublicCaller.query(null, "solr", "q=identifier:" + publicPidEncoded);
            publicObjContents = getNumQueryContents(publicQueryPublicObj); 
            publicQueryPersonObj = mnPublicCaller.query(null, "solr", "q=identifier:" + testPersonPidEncoded);
            personObjContents = getNumQueryContents(publicQueryPersonObj); 
            publicQueryRightsHolderObj = mnPublicCaller.query(null, "solr", "q=identifier:" + testRightsHolderPidEncoded);
            rightsHolderObjContents = getNumQueryContents(publicQueryRightsHolderObj); 
        } catch (Exception e) {
            throw new AssertionError(mnPublicCaller.getLatestRequestUrl() + ", Unable to run solr query for public subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        
        // check result count
        if ( publicObjContents.existingLogs == 0 ) 
            errors.add("Query run by public subject should retrieve a positive result count for public-created object.");
        if ( personObjContents.existingLogs != 0 ) 
            errors.add("Query run by public subject should retrieve a zero result count for testPerson-created object.");
        if ( rightsHolderObjContents.existingLogs != 0 ) 
            errors.add("Query run by public subject should retrieve a zero result count for testRightsHolder-created object.");
        
        // check result docs
        if ( publicObjContents.docsReturned == 0 ) 
            errors.add("Query run by public subject should retrieve result docs for public-created object.");
        if ( personObjContents.docsReturned != 0 ) 
            errors.add("Query run by public subject should NOT retrieve result docs for testPerson-created object.");
        if ( rightsHolderObjContents.docsReturned != 0 ) 
            errors.add("Query run by public subject should NOT retrieve result docs for testRightsHolder-created object.");
        
        InputStream testPersonQueryPublicObj = null;
        InputStream testPersonQueryPersonObj = null;
        InputStream testPersonQueryRightsHolderObj = null;
        try {
            testPersonQueryPublicObj = mnTestPersonCaller.query(null, "solr", "q=identifier:" + publicPidEncoded);
            publicObjContents = getNumQueryContents(testPersonQueryPublicObj); 
            testPersonQueryPersonObj = mnTestPersonCaller.query(null, "solr", "q=identifier:" + testPersonPidEncoded);
            personObjContents = getNumQueryContents(testPersonQueryPersonObj); 
            testPersonQueryRightsHolderObj = mnTestPersonCaller.query(null, "solr", "q=identifier:" + testRightsHolderPidEncoded);
            rightsHolderObjContents = getNumQueryContents(testPersonQueryRightsHolderObj); 
        } catch (Exception e) {
            throw new AssertionError(mnTestPersonCaller.getLatestRequestUrl() + ", Unable to run solr query for testPerson subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        
        // check result count
        if ( publicObjContents.existingLogs == 0 )
            errors.add("Query run by testPerson subject should retrieve a positive result count for public-created object.");
        if ( personObjContents.existingLogs == 0 )
            errors.add("Query run by testPerson subject should retrieve a positive result count for testPerson-created object.");
        if ( rightsHolderObjContents.existingLogs != 0 )
            errors.add("Query run by testPerson subject should retrieve a zero result count for testRightsHolder-created object.");
        
        // check result docs
        if ( publicObjContents.docsReturned == 0 )
            errors.add("Query run by testPerson subject should retrieve result docs for public-created object.");
        if ( personObjContents.docsReturned == 0 )
            errors.add("Query run by testPerson subject should retrieve result docs for testPerson-created object.");
        if ( rightsHolderObjContents.docsReturned != 0 )
            errors.add("Query run by testPerson subject should NOT retrieve result docs for testRightsHolder-created object.");
        
        InputStream testRightsHolderQueryPublicObj = null;
        InputStream testRightsHolderQueryPersonObj = null;
        InputStream testRightsHolderQueryRightsHolderObj = null;
        try {
            testRightsHolderQueryPublicObj = mnTestRightsHolderCaller.query(null, "solr", "q=identifier:" + publicPidEncoded);
            publicObjContents = getNumQueryContents(testRightsHolderQueryPublicObj); 
            testRightsHolderQueryPersonObj = mnTestRightsHolderCaller.query(null, "solr", "q=identifier:" + testPersonPidEncoded);
            personObjContents = getNumQueryContents(testRightsHolderQueryPersonObj); 
            testRightsHolderQueryRightsHolderObj = mnTestRightsHolderCaller.query(null, "solr", "q=identifier:" + testRightsHolderPidEncoded);
            rightsHolderObjContents = getNumQueryContents(testRightsHolderQueryRightsHolderObj); 
        } catch (Exception e) {
            throw new AssertionError(mnTestRightsHolderCaller.getLatestRequestUrl() + ", Unable to run solr query for testRightsHolder subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        }
        
        // check result count
        if ( publicObjContents.existingLogs == 0 ) 
            errors.add("Query run by testRightsHolder subject should retrieve a positive result count for public-created object.");
        if ( personObjContents.existingLogs != 0 ) 
            errors.add("Query run by testRightsHolder subject should NOT retrieve a positive result count for testPerson-created object.");
        if ( rightsHolderObjContents.existingLogs == 0 ) 
            errors.add("Query run by testRightsHolder subject should retrieve a positive result count for testRightsHolder-created object.");
        
        // check result docs
        if ( publicObjContents.docsReturned == 0 ) 
            errors.add("Query run by testRightsHolder subject should retrieve result docs for public-created object.");
        if ( personObjContents.docsReturned != 0 ) 
            errors.add("Query run by testRightsHolder subject should NOT retrieve result docs for testPerson-created object.");
        if ( rightsHolderObjContents.docsReturned == 0 ) 
            errors.add("Query run by testRightsHolder subject should retrieve result docs for testRightsHolder-created object.");
        
        
        InputStream cnQueryPublicObj = null;
        InputStream cnQueryPersonObj = null;
        InputStream cnQueryRightsHolderObj = null;
        try {
            cnQueryPublicObj = mnCnCaller.query(null, "solr", "q=identifier:" + publicPidEncoded);
            publicObjContents = getNumQueryContents(cnQueryPublicObj); 
            cnQueryPersonObj = mnCnCaller.query(null, "solr", "q=identifier:" + testPersonPidEncoded);
            personObjContents = getNumQueryContents(cnQueryPersonObj); 
            cnQueryRightsHolderObj = mnCnCaller.query(null, "solr", "q=identifier:" + testRightsHolderPidEncoded);
            rightsHolderObjContents = getNumQueryContents(cnQueryRightsHolderObj);
        } catch (Exception e) {
            handleFail(mnCnCaller.getLatestRequestUrl(), "Unable to run solr query for testRightsHolder subject.");
        }
        
        // check result count
        if ( publicObjContents.existingLogs == 0 ) 
            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive result count for public-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
                    + "pid: " + publicPid.getValue());
        
        // FIXME the below were commented out because MN.query() doesn't currently support this
        //       it makes sense that this would be supported in the future, so leaving tests commented for now
        
//        if ( personObjContents.existingLogs == 0 ) 
//            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive result count for testPerson-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
//                    + "pid: " + testPersonPid.getValue());
//        if ( rightsHolderObjContents.existingLogs == 0 ) 
//            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive result count for testRightsHolder-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
//                    + "pid: " + testRightsHolderPid.getValue());
        
        // check result docs
        if ( publicObjContents.docsReturned == 0 ) 
            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve result docs for public-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
                    + "pid: " + publicPid.getValue());
//        if ( personObjContents.docsReturned == 0 ) 
//            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve result docs for testPerson-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
//                    + "pid: " + testPersonPid.getValue());
//        if ( rightsHolderObjContents.docsReturned == 0 ) 
//            errors.add("Query run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve result docs for testRightsHolder-created object on " + mnCnCaller.getNodeBaseServiceUrl() + ". "
//                    + "pid: " + testRightsHolderPid.getValue());
        
        if (errors.size() > 0) {
            String errorString = "";
            for (String err : errors)
                errorString += err + "\n";
            throw new AssertionError("testMnQuery_Access ran into " + errors.size() + " errors:\n" + errorString);
        }
    }
    
    
    @WebTestName("getLogRecords() create on MN read on CN")
    @WebTestDescription("Tests the getLogRecords() call. After creating objects on MNs "
            + "and waiting, verifies that logs are aggregated on the CN.")
    public void testCnGetLogRecords_Aggregating() {
    
        int numMNs = this.mns.size();
        ArrayList<MNCallAdapter> mnCallAdapters = new ArrayList<MNCallAdapter>(numMNs);
        for (Node n : this.mns)
            mnCallAdapters.add(new MNCallAdapter(getSession(cnSubmitter), n, "v1"));
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ArrayList<String> pids = new ArrayList<String>(numMNs);
        
        // create objects
        for (int i=0; i<numMNs; i++) {
            Identifier pid = null;
            try {
                String mnId = mnCallAdapters.get(i).getNodeId().getValue().replaceAll(":", "");
                pid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Aggregating_" + mnId + "_obj2");
                procureTestObject(mnCallAdapters.get(i), publicAccessRule, pid);
                pids.add(pid.getValue());
            } catch (Exception e) {
                throw new AssertionError("testCnGetLogRecords_Aggregating: Unable to get or create a test object "
                        + "with pid: " + pid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
            }
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // don't care
        }

        // check for log on originating MN
        for (int i=0; i<numMNs; i++) {
            Identifier pid = null;
            try {
                MNCallAdapter mn = mnCallAdapters.get(i);
                pid = D1TypeBuilder.buildIdentifier(pids.get(i));
                Log logRecords = mn.getLogRecords(null, null, null, null, pid.getValue(), null, null);
                assertTrue("testCnGetLogRecords_Aggregating: Should be able to get record for pid: " 
                        + pid.getValue() + " on originating mn " + mnCallAdapters.get(i).getNodeBaseServiceUrl(), 
                        logRecords != null && logRecords.getLogEntryList() != null && logRecords.getLogEntryList().size() > 0);
            } catch (Exception e) {
                throw new AssertionError("testCnGetLogRecords_Aggregating: Unable to get record for pid: " 
                        + pid.getValue() + " on originating mn " + mnCallAdapters.get(i).getNodeBaseServiceUrl(), e);
            }
        }
        
        try {
            log.info("testCnGetLogRecords_Aggregating:   "
                    + "waiting for log aggregation: (" + ((double)LOG_AGG_WAIT / 60000) + " minutes)");
            Thread.sleep(LOG_AGG_WAIT);
        } catch (InterruptedException e) {
            // no time for a sandwich :(
        }
        
        ArrayList<String> errors = new ArrayList<String>();
        
        // check for logs on CN
        for (int i=0; i<numMNs; i++) {
            Log logRecords = null;
            try {
                logRecords = cn.getLogRecords(null, null, null, null, pids.get(i), null, null);
            } catch (Exception e) {
                throw new AssertionError(cn.getLatestRequestUrl() + " testCnGetLogRecords_Aggregating: unable to fetch log records "
                        + "for pid " + pids.get(i) + " Got exception: " + e.getClass().getSimpleName() 
                        + " : " + e.getMessage(), e);
            }
            if (logRecords == null || logRecords.getTotal() == 0)
                errors.add("testCnGetLogRecords_Aggregating: getLogRecords() call for pid " + pids.get(i) 
                    + " should have a total number of results greater than zero on CN " 
                    + cn.getNodeBaseServiceUrl() + ". "
                    + "(waited " + ((double)LOG_AGG_WAIT / 60000) + " minutes for log aggregation)");
            if (logRecords == null || logRecords.getLogEntryList() == null || logRecords.getLogEntryList().size() == 0)
                errors.add("testCnGetLogRecords_Aggregating: getLogRecords() call for pid " + pids.get(i) 
                    + " should contain more than zero log entries on CN " 
                    + cn.getNodeBaseServiceUrl() + ". "
                    + "(waited " + ((double)LOG_AGG_WAIT / 60000) + " minutes for log aggregation)");
        }
        if (errors.size() > 0) {
            String errorString = "";
            for (String err : errors)
                errorString += err + "\n";
            throw new AssertionError("testCnGetLogRecords_Aggregating ran into " + errors.size() + " errors:\n" + errorString);
        }
    }

    @WebTestName("query() parameters")
    @WebTestDescription("Tests the query() call with different parameters - "
            + "verifies that a handful of parameters work for filtering down on the created object.")
    public void testQuery_Params() {
        
        MNCallAdapter mnCnCaller = getMNCallAdapter(cnSubmitter);
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testQuery_Params");
        try {
            procureTestObject(mnCnCaller, publicAccessRule, pid);
        } catch (Exception e) {
            throw new AssertionError("testQuery_Params: Unable to get or create a test object "
                    + "with pid: " + pid.getValue() + ", " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        String pidEncoded = null;
        String title = "\"PISCO: Physical Oceanography: moored temperature data: Terrace Point, California, USA (TPT001)\"";
        String titleEncoded = null;
        String author = "\"Margaret McManus\"";
        String authorEncoded = null;
        
        try {
            pidEncoded = URLEncoder.encode(pid.getValue(), "UTF-8");
            titleEncoded = URLEncoder.encode(title, "UTF-8");
            authorEncoded = URLEncoder.encode(author, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("testQuery_Params() unable to encode parameters", e);
        }
        
        //----------------------------------------------------------------------------------- DEBUG CODE
//        try {
//            SystemMetadata sysmeta = mnCnCaller.getSystemMetadata(null, pid);
//        } catch (Exception e) {
//            throw new AssertionError("getSysmeta() failed", e);
//        }
//        InputStream is = null;
//        FileOutputStream os = null;
//        try {
//            is = mnCnCaller.query(null, "solr", "q=identifier:" + pidEncoded);
//            os = new FileOutputStream(new File("C:\\Users\\Andrei\\stuff\\solrQuery.txt"));
//            IOUtils.copy(is, os);
//        } catch (Exception e) {
//            handleFail(mnCnCaller.getLatestRequestUrl(), "Unable to run solr query with params: identifier. " 
//                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
//        } finally {
//            IOUtils.closeQuietly(is);
//            IOUtils.closeQuietly(os);
//        }
        //----------------------------------------------------------------------------------------------
        ArrayList<String> errors = new ArrayList<String>();
        
        InputStream queryResult_id = null;
        try {
            queryResult_id = mnCnCaller.query(null, "solr", "q=identifier:" + pidEncoded);
            LogContents queryContents = getNumQueryContents(queryResult_id);
            if (queryContents.existingLogs == 0)
                errors.add("query made by as CN should return some results when filtering on pid");
        } catch (Exception e) {
            errors.add(mnCnCaller.getLatestRequestUrl() + " Unable to run solr query with params: identifier. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(queryResult_id);
        }
        
        InputStream queryResult_title = null;
        try {
            queryResult_title = mnCnCaller.query(null, "solr", "q=title:" + titleEncoded);
            LogContents queryContents = getNumQueryContents(queryResult_title);
            if (queryContents.existingLogs == 0)
                errors.add("query made by as CN should return some results when filtering on title");
        } catch (Exception e) {
            errors.add(mnCnCaller.getLatestRequestUrl() + " Unable to run solr query with params: title. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(queryResult_title);
        }
        
        InputStream queryResult_author = null;
        try {
            queryResult_author = mnCnCaller.query(null, "solr", "q=author:" + authorEncoded);
            LogContents queryContents = getNumQueryContents(queryResult_author);
            if (queryContents.existingLogs == 0)
                errors.add("query made by as CN should return some results when filtering on author");
        } catch (Exception e) {
            errors.add(mnCnCaller.getLatestRequestUrl() + " Unable to run solr query with params: author. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(queryResult_author);
        }
        
        // test data created:
        
        // identifier       testQuery_ParamOrder
        // author          Margaret McManus
        // title            PISCO: Physical Oceanography: moored temperature data: Terrace Point, California, USA (TPT001)
        
        // formatType       METADATA
        // formatId         eml://ecoinformatics.org/eml-2.0.1
        // submitter        ....cn....?         (in this case CN=urn:node:cnDevUCSB2,DC=dataone,DC=org)
        // rightsHolder     CN=testRightsHolder,DC=dataone,DC=org
        // authoritativeMN  mn created on       (MN Identifier:     in this case urn:node:mnDevUCSB2)
        // author*
        // text             PISCO moored temperature, TPT001 PISCO: Physical Oceanography:...
        
        InputStream queryResult_pidTitleAuthor = null;
        try {
            queryResult_pidTitleAuthor = mnCnCaller.query(null, "solr", "q=identifier:" + pidEncoded 
                    + "&q=title:" + titleEncoded
                    + "&q=author:" + authorEncoded);
            LogContents queryContents = getNumQueryContents(queryResult_pidTitleAuthor);
            if (queryContents.existingLogs == 0)
            errors.add("query made by as CN should return some results when filtering on identifier, title, and author");
        } catch (Exception e) {
            errors.add(mnCnCaller.getLatestRequestUrl() + " Unable to run solr query with params: identifier, title, author. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(queryResult_pidTitleAuthor);
        }
        
        
        // TODO to be continued...
        
        
        
        if (errors.size() > 0) {
            String errorString = "";
            for (String err : errors)
                errorString += err + "\n";
            throw new AssertionError("testQuery_Params ran into " + errors.size() + " errors:\n" + errorString);
        }
        
    }
    
    @WebTestName("getLogRecords() create on MN test access on CN")
    @WebTestDescription("Tests the getLogRecords() call. Assumes that aggregation works, so"
            + "the testCnGetLogRecords_Aggregating test should be working. After creating objects on MNs "
            + "and waiting, verifies that logs returned are based on the identity of the caller.")
    public void testCnGetLogRecords_Access() {
    
        int numMNs = this.mns.size();
        ArrayList<String> mnIds = new ArrayList<String>(numMNs);
        ArrayList<MNCallAdapter> mnCallAdapters = new ArrayList<MNCallAdapter>(numMNs);
        for (Node n : this.mns) {
            mnCallAdapters.add(new MNCallAdapter(getSession(cnSubmitter), n, "v1"));
            mnIds.add(n.getIdentifier().getValue().replaceAll(":", ""));
        }
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testPersonAccessRule = new AccessRule();
        getSession("testPerson");
        testPersonAccessRule.addSubject(getSubject("testPerson"));
        testPersonAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessRule testRightsHolderAccessRule = new AccessRule();
        getSession("testRightsHolder");
        testRightsHolderAccessRule.addSubject(getSubject("testRightsHolder"));
        testRightsHolderAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        for (int i=0; i<numMNs; i++) {
            Identifier publicObjPid = null;
            Identifier testPersonObjPid = null;
            Identifier testRightsHolderObjPid = null;
            
            try {
                String mnId = mnIds.get(i);
                publicObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_public_" + mnId);
                testPersonObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_testPerson_" + mnId);
                testRightsHolderObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_testRightsHolder_" + mnId);
                
                procureTestObject(mnCallAdapters.get(i), publicAccessRule, publicObjPid);
                procureTestObject(mnCallAdapters.get(i), testPersonAccessRule, testPersonObjPid);
                procureTestObject(mnCallAdapters.get(i), testRightsHolderAccessRule, testRightsHolderObjPid);
            } catch (Exception e) {
                throw new AssertionError("testCnGetLogRecords_Access: Unable to get or create a test object", e);
            }
        }

        try {
            log.info("testCnGetLogRecords_Access:   "
                    + "waiting for log aggregation: (" + ((double)LOG_AGG_WAIT / 60000) + " minutes)");
            Thread.sleep(LOG_AGG_WAIT);
        } catch (InterruptedException e) {
            // no time for a sandwich :(
            log.warn("log aggregation wait interrupted!", e);
        }
        
        ArrayList<String> errors = new ArrayList<String>();
        
        for (int i=0; i<numMNs; i++) {
            Log publicLogRecords = null;
            Log testPersonLogRecords = null;
            Log testRightsHolderLogRecords = null;
            
            String mnId = mnIds.get(i);
            Identifier publicObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_public_" + mnId);
            Identifier testPersonObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_testPerson_" + mnId);
            Identifier testRightsHolderObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_testRightsHolder_" + mnId);
            try {
                publicLogRecords = cn.getLogRecords(null, null, null, null, publicObjPid.getValue(), null, null);
                testPersonLogRecords = cn.getLogRecords(null, null, null, null, testPersonObjPid.getValue(), null, null);
                testRightsHolderLogRecords = cn.getLogRecords(null, null, null, null, testRightsHolderObjPid.getValue(), null, null);
            } catch (Exception e) {
                throw new AssertionError(cn.getLatestRequestUrl() + " testCnGetLogRecords_Aggregating: unable to fetch log records "
                        + " Got exception: " + e.getClass().getSimpleName() 
                        + " : " + e.getMessage(), e);
            }
            
            if (publicLogRecords.getLogEntryList() != null && publicLogRecords.getLogEntryList().size() == 0)
                errors.add("getLogRecords run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive number of results for public-created object ("
                        + publicObjPid.getValue() + ")");
            if (testPersonLogRecords.getLogEntryList() != null && testPersonLogRecords.getLogEntryList().size() == 0)
                errors.add("getLogRecords run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive number of results for testPerson-created object ("
                        + testPersonObjPid.getValue() + ")");
            if (testRightsHolderLogRecords.getLogEntryList() != null && testRightsHolderLogRecords.getLogEntryList().size() == 0)
                errors.add("getLogRecords run by CN subject (" + getSubject(cnSubmitter).getValue() + ") should retrieve a positive number of results for testRightsHolder-created object ("
                        + testRightsHolderObjPid.getValue() + ")");
        }
        
        if (errors.size() > 0) {
            String errorString = "";
            for (String err : errors)
                errorString += err + "\n";
            throw new AssertionError("testCnGetLogRecords_Access ran into " + errors.size() + " errors:\n" + errorString);
        }
    }
    
    /**
     * Returns the number of results in the given InputStream,
     * which should be the result of a CN.query() call.
     * <b>Closes the given InputStream when done.</b>
     * @param is the InputStream to examine
     * @param checkDoc whether to check the actual contents of the document 
     *  (as opposed to just the numFound in the response header)
     */
    private LogContents getNumQueryContents(InputStream is) {
        
        LogContents logResults = new LogContents();
        
        try {
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
                
//                try {
//                    FileOutputStream os = new FileOutputStream(new File("C:\\Users\\Andrei\\stuff\\logAgg.txt"));
//                    IOUtils.copy(is, os);
//                    return logResults;
//                } catch (Exception e2) {
//                    e2.printStackTrace();
//                }
                
            } catch (Exception e) {
                throw new AssertionError("getNumQueryContents() " + 
                        "unable to convert response to document, got exception: " 
                        + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String resultCountExp = "/response/result";
            org.w3c.dom.Node resultNode = (org.w3c.dom.Node) xPath.compile(resultCountExp).evaluate(doc, XPathConstants.NODE);
            org.w3c.dom.Node numFoundAttr = resultNode.getAttributes().getNamedItem("numFound");
            assertTrue("query response doesn't have valid numFound attribute.", numFoundAttr != null);
    
            String numFoundVal = numFoundAttr.getNodeValue(); 
            logResults.existingLogs = Integer.parseInt(numFoundVal);

            String docsExp = "/response/result/doc";
            XPathExpression xPathExpr = xPath.compile(docsExp);
            NodeList docs = (NodeList) xPathExpr.evaluate(doc, XPathConstants.NODESET);
            logResults.docsReturned = docs.getLength();
            
        } catch (XPathExpressionException e) {
            throw new AssertionError("getNumQueryContents() xpath expression error: "
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        log.info("query results: numFound = " + logResults.existingLogs + "   <doc>s returned = " + logResults.docsReturned);
        if (logResults.existingLogs != logResults.docsReturned)
            log.info("The numFound attribute doesn't match number of <doc> elements found. "
                    + "Subject used probably doesn't have access to them.");
        
        return logResults;
    }
    
    private class LogContents {
        public int existingLogs = 0;
        public int docsReturned = 0;
    }
}
