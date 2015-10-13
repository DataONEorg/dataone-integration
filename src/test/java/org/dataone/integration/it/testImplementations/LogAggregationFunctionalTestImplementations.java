package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
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
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class LogAggregationFunctionalTestImplementations extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private CNCallAdapter cn;
    private List<Node> v2MNs;
//    private static final long LOG_AGG_WAIT = 10 * 60000;    // 10 minutes <--- FIXME this is currently 1 day in our environments...
    private static final long LOG_AGG_WAIT = 10;
    
    @Override
    protected String getTestDescription() {
        return "Runs create/read/etc methods agains MNs in the environment and verifies "
                + "that the corresponding logs end up on a CN.";
    }

    public void setup(Iterator<Node> cnIter) {

        List<Node> mnList = new ArrayList<Node>();
        v2MNs = new ArrayList<Node>();
        
        List<Node> cnList = IteratorUtils.toList(cnIter);
        assertTrue("Test requires at least one CN, but got zero CNs.", cnList.size() != 0);
            
        cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        
        try {
            for(Node n : cn.listNodes().getNodeList())
                if(n.getType() == NodeType.MN)
                    mnList.add(n);
        } catch (Exception e) {
            throw new AssertionError("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }
        
        for (Node mNode : mnList) {
            MNCallAdapter v2mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
            boolean v2support = false;
            
            try {
                v2mn.ping();    // ping v2 endpoint
                Node nodeCapabilities = v2mn.getCapabilities();
                List<Service> serviceList = v2mn.getCapabilities().getServices().getServiceList();
                for (Service service : serviceList) {
                    if ("MNCore".equals(service.getName())          // getLogRecords 
                            && "v2".equals(service.getVersion())
                            && service.getAvailable()) {
                        v2MNs.add(nodeCapabilities);
                        break;
                    }
                }
                v2support = true;
            } catch (Exception e1) {
                log.info("Unable to assess v2 capabilities for MN: " + v2mn.getNodeBaseServiceUrl() 
                        + " : " + e1.getClass().getSimpleName() + " : " + e1.getMessage());
            }
        }

        log.info("v2 MNs available:   " + v2MNs.size());
        for (Node n : v2MNs)
            log.info("v2 MN:   " + n.getBaseURL());
        
//        assertTrue("Test requires that the environment has at least 2 v2 MNs to work with that "
//                + "support logging for CN log aggregation. Found: " + v2MNs.size(), 
//                v2MNs.size() >= 2);
    }

    @WebTestName("getLogRecords() user-based access")
    @WebTestDescription("Tests that an authenticated user may only view their own records "
            + "or public records, and that a CN caller may view all records. Note: allowing "
            + "access to more than the CN is optional for an MN, so authentication failures for "
            + "non-CN subjects are ignored.")
    public void testMnGetLogRecords_Access() {

        MNCallAdapter mnCnCaller = new MNCallAdapter(getSession(cnSubmitter), v2MNs.get(0), "v2");
        MNCallAdapter mnPublicCaller = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), v2MNs.get(0), "v2");
        MNCallAdapter mnTestPersonCaller = new MNCallAdapter(getSession("testPerson"), v2MNs.get(0), "v2");
        MNCallAdapter mnTestRightsHolderCaller = new MNCallAdapter(getSession("testRightsHolder"), v2MNs.get(0), "v2");
        
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
            procureTestObject(mnCnCaller, publicAccessRule, publicPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + publicPid.getValue(), e);
        }
        try {
            procureTestObject(mnCnCaller, testPersonAccessRule, testPersonPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + testPersonPid.getValue(), e);
        }
        try {
            procureTestObject(mnCnCaller, testRightsHolderAccessRule, testRightsHolderPid);
        } catch (Exception e) {
            throw new AssertionError("testMnGetLogRecords_Access: Unable to get or create a test object "
                    + "with pid: " + testRightsHolderPid.getValue(), e);
        }

        ArrayList<String> errors = new ArrayList<String>();
        
        Log publicLog = null;
        try {
            publicLog = mnPublicCaller.getLogRecords(null, null, null, null, publicPid.getValue(), null, null);
        } catch (Exception e) {
            log.warn(mnPublicCaller.getLatestRequestUrl() + " Unable to fetch Log records for public subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log testPersonLog = null;
        try {
            testPersonLog = mnTestPersonCaller.getLogRecords(null, null, null, null, testPersonPid.getValue(), null, null);
        } catch (Exception e) {
            log.warn(mnTestPersonCaller.getLatestRequestUrl() + " Unable to fetch Log records for testPerson subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log testRightsHolderLog = null;
        try {
            testRightsHolderLog = mnTestRightsHolderCaller.getLogRecords(null, null, null, null, testRightsHolderPid.getValue(), null, null);
        } catch (Exception e) {
            log.warn(mnTestRightsHolderCaller.getLatestRequestUrl() + " Unable to fetch Log records for testRightsHolder subject. " 
                    + "Got exception: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log cnLog = null;
        try {
            cnLog = APITestUtils.pagedGetLogRecords(mnCnCaller, null, null, null, null, null, null);
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
        
        if (cnLog != null) {
            boolean publicPidFound = false;
            boolean testPersonPidFound = false;
            boolean testRightsHolderPidFound = false;
            
            for (LogEntry logEntry : cnLog.getLogEntryList()) {
                if (logEntry.getIdentifier().equals(publicPid))
                    publicPidFound = true;
                if (logEntry.getIdentifier().equals(testPersonPid))
                    testPersonPidFound = true;
                if (logEntry.getIdentifier().equals(testRightsHolderPid))
                    testRightsHolderPidFound = true;
            }
            if (!publicPidFound)
                errors.add("CN subject should have access to public-accessible object we created.");
            if (!testPersonPidFound)
                errors.add("CN subject should have access to testPerson-accessible object we created.");
            if (!testRightsHolderPidFound)
                errors.add("CN subject should have access to testRightsHolder-accessible object we created.");
        }
        
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

        MNCallAdapter mnCnCaller = new MNCallAdapter(getSession(cnSubmitter), v2MNs.get(0), "v2");
        MNCallAdapter mnPublicCaller = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), v2MNs.get(0), "v2");
        MNCallAdapter mnTestPersonCaller = new MNCallAdapter(getSession("testPerson"), v2MNs.get(0), "v2");
        MNCallAdapter mnTestRightsHolderCaller = new MNCallAdapter(getSession("testRightsHolder"), v2MNs.get(0), "v2");
        
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
        
//        Identifier publicPid = D1TypeBuilder.buildIdentifier("testQuery_Access_public-TEST-");
//        Identifier testPersonPid = D1TypeBuilder.buildIdentifier("testQuery_Access_testPerson");
//        Identifier testRightsHolderPid = D1TypeBuilder.buildIdentifier("testQuery_Access_testRightsHolder");
        Identifier publicPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_public-TEST1-");
        Identifier testPersonPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_testPerson-TEST1-");
        Identifier testRightsHolderPid = D1TypeBuilder.buildIdentifier("testMnQuery_Access_testRightsHolder-TEST1-");
        
        try {
            publicPid = procureTestObject(mnCnCaller, publicAccessRule, publicPid, cnSubmitter, "public");
            log.info("procured test object: " + publicPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + publicPid.getValue(), e);
        }
        try {
            getSession("testPerson");
            testPersonPid = procureTestObject(mnCnCaller, testPersonAccessRule, testPersonPid, cnSubmitter, getSubject("testPerson").getValue());
            log.info("procured test object: " + testPersonPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + testPersonPid.getValue(), e);
        }
        try {
            getSession("testRightsHolder");
            testRightsHolderPid = procureTestObject(mnCnCaller, testRightsHolderAccessRule, testRightsHolderPid, cnSubmitter, getSubject("testRightsHolder").getValue());
            log.info("procured test object: " + testRightsHolderPid.getValue() + " on " + mnCnCaller.getNodeId().getValue());
        } catch (Exception e) {
            throw new AssertionError("testMnQuery_Access: Unable to get or create a test object "
                    + "with pid: " + testRightsHolderPid.getValue(), e);
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
            errors.add("Query run by CN subject should retrieve a positive result count for public-created object.");
        if ( personObjContents.existingLogs == 0 ) 
            errors.add("Query run by CN subject should retrieve a positive result count for testPerson-created object.");
        if ( rightsHolderObjContents.existingLogs == 0 ) 
            errors.add("Query run by CN subject should retrieve a positive result count for testRightsHolder-created object.");
        
        // check result docs
        if ( publicObjContents.docsReturned == 0 ) 
            errors.add("Query run by CN subject should retrieve result docs for public-created object.");
        if ( personObjContents.docsReturned == 0 ) 
            errors.add("Query run by CN subject should retrieve result docs for testPerson-created object.");
        if ( rightsHolderObjContents.docsReturned == 0 ) 
            errors.add("Query run by CN subject should retrieve result docs for testRightsHolder-created object.");
        
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
    
        int numMNs = v2MNs.size();
        ArrayList<MNCallAdapter> mns = new ArrayList<MNCallAdapter>(numMNs);
        for (Node n : v2MNs) {
            mns.add(new MNCallAdapter(getSession(cnSubmitter), n, "v2"));
        }
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ArrayList<String> pids = new ArrayList<String>(numMNs);
        
        for (int i=0; i<numMNs; i++) {
            Identifier pid = null;
            try {
                String mnId = mns.get(i).getNodeId().getValue().replaceAll(":", "");
                pid = D1TypeBuilder.buildIdentifier("testGetLogRecords_CN_" + mnId);
                procureTestObject(mns.get(i), publicAccessRule, pid);
                pids.add(pid.getValue());
            } catch (Exception e) {
                throw new AssertionError("testGetLogRecords_CN: Unable to get or create a test object "
                        + "with pid: " + pid.getValue(), e);
            }
        }

        try {
            Thread.sleep(LOG_AGG_WAIT);
        } catch (InterruptedException e) {
            // no time for a sandwich :(
        }
        
        for (int i=0; i<numMNs; i++) {
            Log logRecords = null;
            try {
                logRecords = cn.getLogRecords(null, null, null, null, pids.get(i), null, null);
            } catch (Exception e) {
                throw new AssertionError(cn.getLatestRequestUrl() + " testGetLogRecords_CN: unable to fetch log records "
                        + "for pid " + pids.get(i) + " Got exception: " + e.getClass().getSimpleName() 
                        + " : " + e.getMessage(), e);
            }
            assertTrue("testGetLogRecords_CN: getLogRecords() call for pid " + pids.get(i) 
                    + " should have a total number of results greater than zero on CN " 
                    + cn.getNodeBaseServiceUrl() + ".", logRecords.getTotal() > 0);
            assertTrue("testGetLogRecords_CN: getLogRecords() call for pid " + pids.get(i) 
                    + " should contain more than zero log entries on CN " 
                    + cn.getNodeBaseServiceUrl() + ".", logRecords.getLogEntryList().size() > 0);
        }
    }

    @WebTestName("query() parameters")
    @WebTestDescription("Tests the query() call with different parameters - "
            + "verifies that a handful of parameters work for filtering down on the created object.")
    public void testQuery_Params() {
        
        MNCallAdapter mnCnCaller = new MNCallAdapter(getSession(cnSubmitter), v2MNs.get(0), "v2");
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testQuery_ParamOrder");
        try {
            procureTestObject(mnCnCaller, publicAccessRule, pid);
        } catch (Exception e) {
            throw new AssertionError("testQuery_Access: Unable to get or create a test object "
                    + "with pid: " + pid.getValue(), e);
        }
        
        String pidEncoded = null;
        String title = "PISCO: Physical Oceanography: moored temperature data: Terrace Point, California, USA (TPT001)";
        String titleEncoded = null;
        String author = "Margaret McManus";
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
            assertTrue("query made by as CN should return some results when filtering on pid", queryContents.existingLogs > 0);
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
            assertTrue("query made by as CN should return some results when filtering on title", queryContents.existingLogs > 0);
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
            assertTrue("query made by as CN should return some results when filtering on author", queryContents.existingLogs > 0);
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
            assertTrue("query made by as CN should return some results when filtering on "
                    + "identifier, title, and author", queryContents.existingLogs > 0);
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
            throw new AssertionError("testQuery_Access ran into " + errors.size() + " errors:\n" + errorString);
        }
        
    }
    
    @WebTestName("getLogRecords() create on MN test access on CN")
    @WebTestDescription("Tests the getLogRecords() call. Assumes that aggregation works, so"
            + "the testCnGetLogRecords_Aggregating should be working. After creating objects on MNs "
            + "and waiting, verifies that logs returned are based on the identity of the caller.")
    public void testCnGetLogRecords_Access() {
    
        int numMNs = v2MNs.size();
        ArrayList<MNCallAdapter> mns = new ArrayList<MNCallAdapter>(numMNs);
        ArrayList<String> mnIds = new ArrayList<String>(numMNs);
        for (Node n : v2MNs) {
            mns.add(new MNCallAdapter(getSession(cnSubmitter), n, "v2"));
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
        
        ArrayList<String> pids = new ArrayList<String>(numMNs);
        
        for (int i=0; i<numMNs; i++) {
            Identifier publicObjPid = null;
//            Identifier publicObjPid = null;
//            Identifier publicObjPid = null;
            
            try {
                String mnId = mnIds.get(i);
                publicObjPid = D1TypeBuilder.buildIdentifier("testCnGetLogRecords_Access_" + mnId);
                procureTestObject(mns.get(i), publicAccessRule, publicObjPid);
                pids.add(publicObjPid.getValue());
            } catch (Exception e) {
                throw new AssertionError("testGetLogRecords_CN: Unable to get or create a test object "
                        + "with pid: " + publicObjPid.getValue(), e);
            }
        }

        try {
            Thread.sleep(LOG_AGG_WAIT);
        } catch (InterruptedException e) {
            // no time for a sandwich :(
        }
        
//        for (int i=0; i<numMNs; i++) {
//            Log logRecords = null;
//            try {
//                logRecords = cn.getLogRecords(null, null, null, null, pids.get(i), null, null);
//            } catch (Exception e) {
//                throw new AssertionError(cn.getLatestRequestUrl() + " testGetLogRecords_CN: unable to fetch log records "
//                        + "for pid " + pids.get(i) + " Got exception: " + e.getClass().getSimpleName() 
//                        + " : " + e.getMessage(), e);
//            }
//            assertTrue("testGetLogRecords_CN: getLogRecords() call for pid " + pids.get(i) 
//                    + " should have a total number of results greater than zero on CN " 
//                    + cn.getNodeBaseServiceUrl() + ".", logRecords.getTotal() > 0);
//            assertTrue("testGetLogRecords_CN: getLogRecords() call for pid " + pids.get(i) 
//                    + " should contain more than zero log entries on CN " 
//                    + cn.getNodeBaseServiceUrl() + ".", logRecords.getLogEntryList().size() > 0);
//        }
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
                
                // failing on testQuery_Access
                
                
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
