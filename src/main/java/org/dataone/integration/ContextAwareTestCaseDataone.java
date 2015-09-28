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

package org.dataone.integration;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dataone.client.D1Node;
import org.dataone.client.D1NodeFactory;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.rest.HttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.client.rest.RestClient;
import org.dataone.client.v1.CNode;
import org.dataone.client.v1.MNode;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.dspace.foresite.ResourceMap;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

/**
 * This class is intended as a base class that implements the standard IntegrationTestContextParameters
 *
 * There is a setUp routine that parses configurations into objects useful for running tests in a given
 * context.  It also sets up the logger, and errorCollector logic (see @Rule, and  protected "check" methods)
 *
 * @author rnahf
 *
 */
public abstract class ContextAwareTestCaseDataone implements IntegrationTestContextParameters {

    protected static Log log = LogFactory.getLog(ContextAwareTestCaseDataone.class);


    public static final String QUERYTYPE_SOLR = "solr";
    public static final String CHECKSUM_ALGORITHM = "MD5";
    public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_EML_2_0_1;
//	public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_TEXT_PLAIN;
//	public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_EML_2_0_0;
    public static final String RESOURCE_MAP_FORMAT_ID = "http://www.openarchives.org/ore/terms";

    public static String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "cnDevUNM1");

    private static Map<String,MultipartRestClient> sessionMap = new HashMap<String,MultipartRestClient>();
    private static Map<String,Subject> subjectMap = new HashMap<String, Subject>();

    private static final MultipartRestClient MULTIPART_REST_CLIENT = getSession(null);
    
    /* a map that acts as a cache for checking if member nodes are reachable */
    protected static Map<String,Long> lastAliveMap = new HashMap<>();

    // context-related instance variables
    private boolean alreadySetup = false;

    // variables to the context interface parameters
    protected  String testContext = null;
    protected  String cnBaseUrl = null;
    protected  String mnBaseUrl = null;
    protected  String nodelistUri = null;
    protected  String referenceContext = null;
    protected  String referenceCnBaseUrl = null;

    public List<Node> memberNodeList = new Vector<Node>();
    public List<Node> coordinatingNodeList = new Vector<Node>();
    public List<Node> monitorNodeList = new Vector<Node>();
    
    // multinode contexts gained through iterating through a nodelst are expensive 
    // to set up because of checks that the listed nodes are in service, which may timeout.
    // these tests (currently) are not run in a multi-threaded execution context
    // so we can cache these values.
    private static boolean multiNodeExists = false;
    private static List<Node> multiNodeMemberNodeList = new Vector<Node>();
    private static List<Node> multiNodeCoordinatingNodeList = new Vector<Node>();
    private static List<Node> multiNodeMonitorNodeList = new Vector<Node>();

    // this here defines the default
    // can be overwritten by property passed into base class
    protected String testObjectSeriesSuffix = "." + "14";
    protected String testObjectSeries = null;

    protected static boolean failOnMissingNodes = false;

    protected abstract String getTestDescription();

    public String getTestObjectSeriesSuffix() {
        return this.testObjectSeriesSuffix;
    }

    public String getTestObjectSeries() {
        return this.testObjectSeries;
    }


    /**
     * sets static variables based on properties returned from org.dataone.configuration.Settings object
     * assigns only once (the first test)
     *
     * @throws Exception
     */
    @Before
    public void setUpContext() throws Exception {

        log.debug("Current Thread: " + Thread.currentThread().getId());
        // skip setUp steps if already run
        if (!alreadySetup) {
            alreadySetup = true;

            // bind the context static vars to Settings properties
            testContext = Settings.getConfiguration().getString(CONTEXT_LABEL);
            cnBaseUrl = Settings.getConfiguration().getString(CONTEXT_CN_URL);
            mnBaseUrl = Settings.getConfiguration().getString(CONTEXT_MN_URL);
            nodelistUri = Settings.getConfiguration().getString(CONTEXT_NODELIST_URI);

            log.info("****************************************************");
            log.info("***  context label:   " + testContext);

            // for running under servlet context (MNWebTester), the TestRunnerHttpServlet will copy the
            // PARAM_MN_URL to a property containing the thread ID, to avoid any concurrency
            // issues (settings getting changed by another client).

            String urlThrIdUrl = System.getProperty("mnwebtester.thread."
                    + Thread.currentThread().getId() + ".mn.baseurl");

            if (urlThrIdUrl != null) {
                log.info("*** mn.baseurl obtained from thread.X.mn.baseurl property");
                mnBaseUrl = urlThrIdUrl;

            } else if (mnBaseUrl != null) {
                log.info("*** mn.baseurl set from context.mn.baseurl property");
            }

            String urlThrIdObjectSeries = System.getProperty("mnwebtester.thread."
                    + Thread.currentThread().getId() + ".tierTesting.object.series");

            if (urlThrIdObjectSeries != null) {
                log.info("*** testObjectSeries obtained from thread.X.tierTesting.object.series property");
                testObjectSeries = urlThrIdObjectSeries;
            }



            log.info("****************************************************");

            if (mnBaseUrl != null) {
                // the context is standalone member node
                System.out.println("~~~ Context is solo MemberNode: " + mnBaseUrl);
                Node n = new Node();
                n.setBaseURL(mnBaseUrl);
                n.setType(NodeType.MN);
                memberNodeList = new Vector<Node>();
                memberNodeList.add(n);
                log.info("*** Adding MN to list: [" + n.getBaseURL() +"]");
            } else if (cnBaseUrl != null) {
                System.out.println("~~~ Context is solo CoordinatingNode: " + cnBaseUrl);
                Node n = new Node();
                n.setBaseURL(cnBaseUrl);
                n.setType(NodeType.CN);
                coordinatingNodeList = new Vector<Node>();
                coordinatingNodeList.add(n);
            } else {
                setupMultipleNodes();


            } // nodelist set up

            //  now set up the reference cnBaseUrl, if it's provided
            referenceCnBaseUrl = Settings.getConfiguration().getString(REFERENCE_CONTEXT_CN_URL);
            if (referenceCnBaseUrl == null) {
                referenceContext = Settings.getConfiguration().getString(REFERENCE_CONTEXT_LABEL,null);
                referenceCnBaseUrl = TestSettings.getReferenceCnBaseUrl(referenceContext);
            }

            log.info("****************************************************");
        }  // settings already set up
    }


    /*
     * sets static variables based on the values of other static variables
     * Parses a nodelist into separate lists for each node type,
     * either from context label or nodelist URI property
     *
     */
    private void setupMultipleNodes()
    throws IOException, InstantiationException, IllegalAccessException,
    JiBXException, ServiceFailure, NotImplemented, ClientSideException
    {
        if (!multiNodeExists) {
            // building a low-timeout httpClient for determining if a
            // node is reachable or not
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(10000)
                    .setConnectionRequestTimeout(10000)
                    .setSocketTimeout(10000)
                    .build();
            RestClient rc = new RestClient(
            HttpClients.custom().setDefaultRequestConfig(config).build()
            );
            parseContextNodeList(rc);
            multiNodeExists = true;
        }
        memberNodeList = multiNodeMemberNodeList;
        coordinatingNodeList = multiNodeCoordinatingNodeList;
        monitorNodeList = multiNodeMonitorNodeList;
    }

    /*
     * this routine sets the static node lists for reuse
     */
    private void parseContextNodeList(RestClient rc) 
    throws IOException, InstantiationException, IllegalAccessException,
    JiBXException, ServiceFailure, NotImplemented, ClientSideException
    {
        // we will be testing multiple member nodes
        List<Node> allNodesList = new Vector<Node>();
        
        if (nodelistUri != null) {
            // the list of member nodes is in this NodeList.xml file
            System.out.println("~~~ Context is ad-hoc NodeList at: " + nodelistUri);
            InputStream is = null;
            try {
                URL url = new URL(nodelistUri);
                is = url.openStream();
            } catch (Exception e) {
                is = ContextAwareTestCaseDataone.class.getClassLoader()
                        .getResourceAsStream(nodelistUri);
            }
            NodeList nl = TypeMarshaller.unmarshalTypeFromStream(NodeList.class, is);
            allNodesList = nl.getNodeList();
        } else {
            // use the context specified by D1Client.properties
            String cnBaseUrl = Settings.getConfiguration().getString("D1Client.CN_URL");
            System.out.println("~~~ Context is from d1client.properties: " + cnBaseUrl);
            if (cnBaseUrl != null) 
                allNodesList = getNodeList(cnBaseUrl);
        }
        
        // divide into separate lists
        for(int i=0; i < allNodesList.size(); i++) {
            Node currentNode = allNodesList.get(i);

            if (currentNode.getType() == NodeType.CN) {
                // test the baseUrl
                try {
                    isNodeAlive(rc, currentNode.getBaseURL());
                    multiNodeCoordinatingNodeList.add(currentNode);
                    log.info("*** Adding CN to list: " + currentNode.getName() +
                            " [ " + currentNode.getBaseURL() +" ]");
                }
                catch (Exception e) {
                    if (failOnMissingNodes)
                        handleFail(MULTIPART_REST_CLIENT.getLatestRequestUrl(), "Context Setup error: Could not reach node at " +
                                currentNode.getIdentifier().getValue() +
                                " for testing. Skipping further test cases for this node");
                    log.warn("*** Failed to add CN to list: " + currentNode.getName() +
                            " [ " + currentNode.getBaseURL() +
                            " ].  Could not reach the node:" + MULTIPART_REST_CLIENT.getLatestRequestUrl()
                            );
                }
            } else if (currentNode.getType() == NodeType.MN) {
                try {
                    isNodeAlive(rc, currentNode.getBaseURL());
                    multiNodeMemberNodeList.add(currentNode);
                    log.info("*** Adding MN to list: " + currentNode.getName() +
                            " [ " + currentNode.getBaseURL() +" ]");
                }
                catch (Exception e) {
                    if (failOnMissingNodes)
                        handleFail(MULTIPART_REST_CLIENT.getLatestRequestUrl(), "Could not reach node at " +
                                currentNode.getIdentifier().getValue() +
                                " for testing. Skipping further test cases for this node");

                    log.warn("*** Failed to add MN to list: " + currentNode.getName() +
                            " [ " + currentNode.getBaseURL() +
                            " ].  Could not reach the node:" + MULTIPART_REST_CLIENT.getLatestRequestUrl()
                            );
                }
            } else if (currentNode.getType() == NodeType.MONITOR) {
                multiNodeMonitorNodeList.add(currentNode);
                log.info("*** Adding MonitorNode to list: " + currentNode.getName() +
                        " [ " + currentNode.getBaseURL() +" ]");
            } else {
                log.warn("Node from nodelist is not of recognizable type: [" +
                        currentNode.getType() + "]. Removing from test list: " +
                        currentNode.getName() + ": " + currentNode.getBaseURL());
            }
        }
    }

    /*
     * Returns true or throws an exception
     * Exceptions are thrown if a response cannot be returned from the baseurl
     */
    private boolean isNodeAlive(RestClient rc, String baseURL) 
    throws ClientProtocolException, IOException {
        
        log.info("isNodeAlive for Node: " + baseURL + " ...");
        Long latestCheck = lastAliveMap.get(baseURL);
        Date now = new Date();
        if (latestCheck == null /* never checked */ || (now.getTime() - latestCheck > 600000 /* >10 min */ )) {
            // check the node
            log.info("... calling node ...");
            HttpResponse resp = null;
            try {
               resp = rc.doGetRequest(baseURL, null);
               lastAliveMap.put(baseURL, new Long(new Date().getTime()));
               return true;
            } 
            finally {
                if (resp != null) 
                    EntityUtils.consumeQuietly(resp.getEntity());
                log.info("... called node");
            }
        } else {
            log.info("... lastAlive still fresh (using cached timestamp)");
            return true;
        }
    }


    private List<Node> getNodeList(String baseURL)
    throws ClientSideException, NotImplemented, ServiceFailure {
        
        if (baseURL == null) 
            throw new ClientSideException("baseURL parameter to getNodeList cannot be null.");

        try {
            org.dataone.client.v1.CNode cnv1 =
                    D1NodeFactory.buildNode(org.dataone.client.v1.CNode.class,
                            MULTIPART_REST_CLIENT, URI.create(baseURL));
            return cnv1.listNodes().getNodeList();
        }
        catch (Exception e) {
            try {
                org.dataone.client.v2.CNode cnv2 =
                    D1NodeFactory.buildNode(org.dataone.client.v2.CNode.class,
                            MULTIPART_REST_CLIENT, URI.create(baseURL));
                NodeList v1nodelist = TypeFactory.convertTypeFromType(cnv2.listNodes(),NodeList.class);
                
                return v1nodelist.getNodeList();
            } 
            catch (InstantiationException | IllegalAccessException | 
                    InvocationTargetException | NoSuchMethodException e1) {
                e1.printStackTrace();
                throw new ClientSideException("Error converting v2.NodeList to v1.NodeList",e1);
            }
        }
    }


    @Before
    public void setUpTestObjectSeries() throws Exception {
        if (testObjectSeries != null) {
            testObjectSeriesSuffix = "." + testObjectSeries;
        }
    }

    protected Iterator<Node> getMemberNodeIterator() {
        return memberNodeList.iterator();
    }

    /**
     * Uses getMemberNodeIterator() but includes only v2 Nodes.
     */
    protected Iterator<Node> getV2MemberNodeIterator() {
        Iterator<Node> memberNodeIterator = memberNodeList.iterator();
        List<Node> v2MNs = new ArrayList<Node>();
        while (memberNodeIterator.hasNext()) {
            Node mn = memberNodeIterator.next();
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), mn, "v2");
            try {
                mnCallAdapter.ping();
                v2MNs.add(mn);
                log.info("MN included in test: " + mn.getBaseURL());
            } catch (Exception e) {
                log.info("MN excluded from test: " + mn.getBaseURL());
            }
        }
        return v2MNs.iterator();
    }
    
    protected Iterator<Node> getCoordinatingNodeIterator() {
        return coordinatingNodeList.iterator();
    }

    protected Iterator<Node> getMonitorNodeIterator() {
        return monitorNodeList.iterator();
    }

    /**
     * returns the reference CN baseUrl passed in on reference.context.label
     * or reference.cn.baseUrl
     * @return
     */
    protected String getReferenceContextCnUrl() {
            return referenceCnBaseUrl;
    }

//	/**
//	 * uses the property "test.subject.writer.certLocation" to setup
//	 * the CertificateManager to use the certificate found at that path
//	 * @return
//	 */
//	public static Subject setupClientSubject_Writer(){
//		return setupClientSubject("test.subject.writer.certLocation");
//	}
//
//	/**
//	 * uses the property "test.subject.reader.certLocation" to setup
//	 * the CertificateManager to use the certificate found at that path
//	 * @return
//	 */
//	public static Subject setupClientSubject_Reader(){
//		return setupClientSubject("test.subject.reader.certLocation");
//	}
//
//	/**
//	 * uses the property "test.subject.norights.certLocation" to setup
//	 * the CertificateManager to use the certificate found at that path
//	 * @return
//	 */
//	public static Subject setupClientSubject_Writer_Expired(){
//		return setupClientSubject("test.subject.writerexpired.certLocation");
//	}
//
//	/**
//	 * uses the property "test.subject.norights.certLocation" to setup
//	 * the CertificateManager to use the certificate found at that path
//	 * @return
//	 */
//	public static Subject setupClientSubject_NoRights(){
//		return setupClientSubject("test.subject.norights.certLocation");
//	}

    /**
     * uses a bad certificate location "/bogus/certificate/location" to setup the client
     * with no certificate
     * @return
     */
    public static void setupClientSubject_NoCert(){
        CertificateManager.getInstance().setCertificateLocation("/bogus");
    }

    /**
     * Uses the given {@code certificateFilename} to setup the
     * CertificateManager to use the certificate found at that path
     * @return the {@link Subject} of the certificate loaded
     */
    public static Subject setupClientSubject(String certificateFilename)
    {
        // 1. set up the client certificate
        String testCertDirectory = (String) Settings.getConfiguration().getProperty("d1.test.cert.location");
        log.info("certificate directory: " + testCertDirectory);
        log.info("certificate filename: " + certificateFilename);
        
        CertificateManager cm = CertificateManager.getInstance();
        cm.setCertificateLocation(testCertDirectory + certificateFilename + ".crt");
        cm.loadCertificate();

        // 2. return the subject corresponding to the loaded certificate
        Subject clientSubject = ClientIdentityManager.getCurrentIdentity();

        
        log.info("client setup as Subject: " + clientSubject.getValue());
        return clientSubject;
    }

    /**
     * Returns the cached {@link Subject} for the given {@code subjectName}.
     * This corresponds with the {@code certificateFilename} used in the call to
     * {@link ContextAwareTestCaseDataone#getSession(String)}.
     * <b>WARNING:</b> Will return null if the {@link Subject} was not already
     * cached. This method cannot call {@link #getSession(String)} to cache it
     * because of the side effects of that call (loading the certificate into
     * {@link CertificateManager}).
     *
     * @param certificateFilename
     *          the name of the certificate file, without the extension,
     *          (does not necessarily match the CN (common name) in the
     *          {@link Subject} of the certificate)
     * @return the {@link Subject} for the given {@code subjectName}
     */
    public static Subject getSubject(String certificateFilename) {
        return subjectMap.get(certificateFilename);
    }

    /**
     * Returns a {@link MultipartRestClient} set up with a certificate
     * from the given {@code certificateFilename}.
     * @param certificateFilename the name of the certificate file, without the extension
     *          (does NOT always match the actual subject CN in the certificate)
     * @return a {@link MultipartRestClient}
     */
    public static MultipartRestClient getSession(String certificateFilename) {
        if (certificateFilename == null) {
            certificateFilename = Constants.SUBJECT_PUBLIC;
        }
        if ( ! sessionMap.containsKey(certificateFilename) ) {
            if (certificateFilename.equals(Constants.SUBJECT_PUBLIC)) {
                setupClientSubject_NoCert();
            } else {
                Subject subject = setupClientSubject(certificateFilename);
                subjectMap.put(certificateFilename, subject);
            }
            try {
                sessionMap.put(certificateFilename, new HttpMultipartRestClient());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClientSideException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return sessionMap.get(certificateFilename);
    }



    public ObjectList procureObjectList(CommonCallAdapter cca)
    throws TestIterationEndingException
    {
        return procureObjectList(cca,false);
    }

    /**
     * get an ObjectList from listObjects as the current user, and if empty,
     * try to create a public readable object.
     * @param cca
     * @param getAll - sets the start and count parameter to get the entire list.
     * @return
     * @throws TestIterationEndingException
     */
    public ObjectList procureObjectList(CommonCallAdapter cca, boolean getAll)
    throws TestIterationEndingException
    {
        ObjectList objectList = null;
        try {
            if (getAll) {
                objectList = cca.listObjects(null, null, null, null, 0, 0);
                objectList = cca.listObjects(null, null, null, null, 0, objectList.getTotal());
            } else {
                objectList = cca.listObjects(null, null, null, null, null, null);
            }
        } catch (BaseException e) {
            throw new TestIterationEndingException("unexpected error thrown by listObjects(): " + e.getMessage(), e);
        } catch (ClientSideException e) {
            throw new TestIterationEndingException("unexpected error thrown by listObjects(): " + e.getMessage(), e);
        }
        if (objectList.getTotal() == 0) {
            try {
                createPublicTestObject(cca,"");
                objectList = cca.listObjects(null, null, null, null, null, null);
                if (objectList.getTotal() == 0) {
                    throw new TestIterationEndingException("could not find or create an object for use by listObjects().");
                }
            } catch (BaseException e) {
                throw new TestIterationEndingException("could not find or create an object for use by listObjects(): " + e.getMessage(), e);
            } catch (UnsupportedEncodingException e) {
                throw new TestIterationEndingException("could not find or create an object for use by listObjects(): " + e.getMessage(), e);
            } catch (ClientSideException e) {
                throw new TestIterationEndingException("could not find or create an object for use by listObjects(): " + e.getMessage(), e);
            }
        }
        return objectList;
    }



//	/**
//	 * get an existing object from the node, if none available, try to
//	 * create one (not all nodes will allow this).
//	 * @param  d1Node - the MNode or CNode object from where to procure the object Identifier
//	 * @param  permissionLevel - the permission-level of the object retrieved needed
//	 * @param  checkUsingIsAuthorized - if true will use IsAuthorized(permissionLevel)
//	 *                         instead of checking the systemMetadata
//	 * @return - Identifier for the readable object.
//	 * @throws - IndexOutOfBoundsException  - when can't procure an object Identifier
//	 */
//	protected Identifier procureTestObject(D1Node d1Node, Permission permissionLevel, boolean checkUsingIsAuthorized)
//	{
//
//		return procureTestObject(d1Node,null,permissionLevel, checkUsingIsAuthorized);
//	}

    /**
     * A method primarily for READ interface testing, that has to efficiently get
     * a public object from Tier1, Tier2, and Tier3+ nodes.  First does a procureTestObject
     * then failing that, does a listObjects(), and subsequent getSysMeta to see
     * if there is any object that satisfies the criteria (PublicReadable)
     *
     * @param cca
     * @return Identifier for object to be used for testing
     * @throws TestIterationEndingException
     */
    public Identifier procurePublicReadableTestObject(CommonCallAdapter cca, Identifier firstTry)
    throws TestIterationEndingException
    {
        Identifier identifier = null;
        try {
            identifier  = procureTestObject(
                    cca,
                    D1TypeBuilder.buildAccessRule(
                            Constants.SUBJECT_PUBLIC,
                            Permission.READ),
                    firstTry);
        }
        catch (Exception e) {
            ; // fallback to plan B
        }

        // didn't get an identifier from procureObjectList, so will loop through
        // listObjects until we find a suitable one.
        // criteria: must be declared public; can't be too big; can't look forever
        // (if too big, can get a heap exception  - it's already happened!)
        BaseException latestException = null;
        if (identifier == null) {
            try {
                ObjectList ol = cca.listObjects(null, null, null, null, null, null);
                if (ol != null && ol.getCount() > 0) {

                    // start time of this search-loop
                    long start = (new Date()).getTime();

                    // if the object size is less that this size,
                    // stop looking for a smaller one.
                    BigInteger sizeGoodEnoughLimit = new BigInteger("3000000");
                    BigInteger objectSize = new BigInteger("999888777666");

                    for (ObjectInfo oi: ol.getObjectInfoList()) {
                        if (cca instanceof CNode) {
                            if (!oi.getFormatId().getValue().startsWith("eml:"))
                                continue;
                        }
                        try {
                            SystemMetadata smd = null;
                            smd = cca.getSystemMetadata(null,oi.getIdentifier());

                            if (AccessUtil.getPermissionMap(smd.getAccessPolicy())
                                    .containsKey(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC)))
                            {
                                // if the current item is smaller than the previous one, use it.
                                if (oi.getSize().compareTo(objectSize) < 1) {
                                    identifier = oi.getIdentifier();
                                    objectSize = oi.getSize();
                                }
                                // if smaller than the good enough limit, finish the search
                                if (oi.getSize().compareTo(sizeGoodEnoughLimit) < 1) {
                                    break;
                                }
                                 else {
                                    log.debug(String.format(
                                            "Size-limit exceeded: pid = %s, node = %s, size = %s, limit=%s",
                                            oi.getIdentifier().getValue(),
                                            cca.getNodeId(),
                                            oi.getSize().toString(),
                                            sizeGoodEnoughLimit.toString())
                                            );
                                }
                            }
                        } catch (BaseException e) {
                            latestException = e;
                        }
                        // don't search forever...
                        long now = (new Date()).getTime();
                        if (now > start + 30 * 1000)
                            break;
                    }
                }
                else {
                    // empty object list
                    throw new TestIterationEndingException("could not create a test object and objectList is empty");
                }
            }
            catch (BaseException be) {  // from initial list objects
                throw new TestIterationEndingException("could not create a test object " +
                        "and listObjects() threw exception:: " + be.getClass().getSimpleName() +
                        " :: " + be.getDescription(),be);
            } catch (ClientSideException e1) {
                throw new TestIterationEndingException("could not create a test object " +
                        "and listObjects() threw exception:: " + e1.getClass().getSimpleName() +
                        " :: " + e1.getMessage(),e1);
            }
        }

        if (identifier == null)
            // nothing public found
            if (latestException instanceof NotAuthorized) {
                throw new TestIterationEndingException("could not create a test object and" +
                " could not find object with a public accessRule in reasonable amount of time");
            } else if (latestException != null){ // other problems, probably with parsing the sysmetadata leading to exceptions
                throw new TestIterationEndingException("could not create a test object and" +
                " attempts to find an object with a public accessRule gave the following exception: " +
                latestException.getClass().getSimpleName() + ":: " + latestException.getDescription(),
                latestException);
            } else {
                throw new TestIterationEndingException("could not create a test object and" +
                        " could not find object with a public accessRule.");
            }
        return identifier;
    }


    /**
     * get an existing object for testing, and failing that attempt to create one (not all nodes will allow this).
     * @param  cca - the MNode or CNode object from where to procure the object Identifier
     * @param  accessRule - specifies the AccessPolicy's AccessRule contained by
     *                      the object returned by the pid parameter.  (it makes it so on create,
     *                      and checks for it on get).  If null, requires a null or empty
     *                      AccessPolicy for the returned object.

     * @param  pid - the Identifier of the object to get or create.  Cannot be null.

     * @return - Identifier for the readable object.
     * @throws NotFound
     * @throws UnsupportedEncodingException
     * @throws InvalidRequest
     * @throws NotImplemented
     * @throws InvalidSystemMetadata
     * @throws InsufficientResources
     * @throws UnsupportedType
     * @throws IdentifierNotUnique
     * @throws NotAuthorized
     * @throws ServiceFailure
     * @throws InvalidToken
     * @throws TestIterationEndingException - - when can't procure an object Identifier
     */
    public Identifier procureTestObject(CommonCallAdapter cca, AccessRule accessRule, Identifier pid)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, TestIterationEndingException
    {
        Identifier identifier = null;
        try {
            log.debug("procureTestObject: checking system metadata of requested object");
            SystemMetadata smd = cca.getSystemMetadata(null, pid);

            if (accessRule == null) {
                // need the accessPolicy to be null, or contain no accessrules
                if (smd.getAccessPolicy() == null || smd.getAccessPolicy().sizeAllowList() == 0) {
                    identifier = pid;
                } else {
                    throw new TestIterationEndingException("returned object doesn't have the expected accessRules");
                }
            } else {
                if (smd.getAccessPolicy() != null && smd.getAccessPolicy().sizeAllowList() == 1) {
                    AccessRule ar = smd.getAccessPolicy().getAllow(0);
                    if (ar.sizePermissionList() == 1 && ar.sizeSubjectList() == 1) {
                        identifier = pid;
                        if (!ar.getPermission(0).equals(accessRule.getPermission(0)))
                            throw new TestIterationEndingException("the AccessRule (permission) of the returned object does not match what's requested");
                        if (!ar.getSubject(0).equals(accessRule.getSubject(0)))
                            throw new TestIterationEndingException("the AccessRule (subject) of the returned object does not match what's requested");

                    } else {
                        throw new TestIterationEndingException("the AccessRule of the returned object has either multiple subjects or multiple permissions");
                    }
                } else {
                    throw new TestIterationEndingException("the AccessPolicy of the returned object is either null or has multiple AccessRules");
                }
            }
        }
//		catch (NotAuthorized e) {
//			identifier = pid;
//			// give it a go...
//		}
        catch (NotFound e) {
            try {
                if (cca instanceof MNode) {
                    Node node = ((MNode) cca).getCapabilities();
                    if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
                        log.debug("procureTestObject: calling createTestObject");
                        identifier = createTestObject(cca, pid, accessRule);
                    }
                } else {
                    log.debug("procureTestObject: calling createTestObject");
                    identifier = createTestObject(cca, pid, accessRule);
                }
            } catch (ClientSideException e1) {
                throw new TestIterationEndingException("unexpected client-side exception encountered when trying to creat e test object", e1);
            }
        } catch (ClientSideException e) {
            throw new TestIterationEndingException("unexpected client-side exception encountered when trying to procure a test object", e);
        }
        log.info(" ====>>>>> pid of procured test Object: " + identifier.getValue());
        return identifier;

//		Identifier id = getTestObject(d1Node, subjectFilter, permissionLevel, checkUsingIsAuthorized);
//		if (id == null) {
//			log.debug("procureTestObject: calling createTestObject...");
//			try {
//				id = createTestObject(d1Node, null, permissionLevel, subjectFilter);
//			} catch (BaseException e) {
//				handleFail(d1Node.getNodeBaseServiceUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
//			} catch (UnsupportedEncodingException e) {
//				log.error(e.getClass().getName() + ": " + e.getMessage());
//				e.printStackTrace();
//			}
//		}
//		if (id == null) {
//			throw new IndexOutOfBoundsException("could not procure (get or create) suitable test object");
//		}
//		log.info(" ====>>>>> pid of procured test Object: " + id.getValue());
//		return id;
    }



    /**
     * gets a test object from the specified node.
     * @param  cca - the MNode or CNode object from where to procure the object Identifier
     * @param  permissionLevel - the permission-level of the object retrieved needed
     * @param  subjectFilter - if not null, means the permissions specified have to be
     * 						   explicitly assigned in the systemmetadata accessPolicy
     * 						   to the provided subject
     * @param  checkUsingIsAuthorized - if true will use IsAuthorized(permissionLevel)
     *                         instead of checking the systemMetadata
     * @return - Identifier for the readable object, if found, otherwise null
     */
    @Deprecated
    public Identifier getTestObject(CommonCallAdapter cca, Subject subjectFilter,
            Permission permissionLevel, boolean checkUsingIsAuthorized)
    {
        Identifier id = null;
        try {
            ObjectList ol = cca.listObjects(null, null, null, null, null, null);
            if (ol.getTotal() > 0) {
                if (subjectFilter != null || (permissionLevel != Permission.READ && checkUsingIsAuthorized == false)) {
                    // will need to pull sysmeta and examine the accessPolicy for both situations
                    if (checkUsingIsAuthorized) {
                        log.debug("getTestObject: subject not null, so need to check accessPolicy of each objectInfo until success...");
                    } else {
                        log.debug("getTestObject: checking accessPolicy of each objectInfo until success...");
                    }
                    for(int i=0; i< ol.sizeObjectInfoList(); i++) {
                        id = ol.getObjectInfo(i).getIdentifier();
                        try {
                            SystemMetadata smd = cca.getSystemMetadata(null, id);

                            HashMap<Subject,Set<Permission>> permMap = AccessUtil.getPermissionMap(smd.getAccessPolicy());
                            if (permMap.containsKey(subjectFilter)) {
                                // this readable object contains the subject of interest
                                if (permMap.get(subjectFilter).contains(permissionLevel)) {
                                    log.debug("getTestObject: found one!!! breaking...");
                                    break;
                                }
                            }
                        } catch (BaseException be) {
                            // letting us bypass unexpected errors due to, perhaps inconsistency
                            // between listObject availability and getSMD availability
                        }
                        // only reach here if break not called, and only break on success
                        id = null;
                    }
                }
                else if (permissionLevel == Permission.READ)
                {
                    // listObjects implied READ permission for current user as with isAuthorized()
                    log.debug("getTestObject: using the objectList to get object");
                    id = ol.getObjectInfo(0).getIdentifier();
                }
                else if (checkUsingIsAuthorized)
                {
                    log.debug("getTestObject: using isAuthorized() to check permissions");
                    for(int i=0; i< ol.sizeObjectInfoList(); i++) {
                        id = ol.getObjectInfo(i).getIdentifier();
                        try {
                            cca.isAuthorized(null,id, permissionLevel);
                            break;  // found one!!
                        } catch (NotAuthorized na) {
                            // it's ok to fail here
                        }
                        id = null;
                    }
                }
                else {
                    //
                }
            }
        }
        catch (BaseException e) {
            handleFail(cca.getNodeBaseServiceUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            log.warn(e.getClass().getName() + ": " + e.getMessage());
        }
        return id;
    }


    /**
     * Creates a test object on the specified member node that is public readable.
     *
     * @param d1Node - the node on which to create the object
     * @param idSuffix - if not null, appends the specified string as suffix to auto-generated identifier
     *                   (the autogenerated uses timestamps to give reasonable assurances of uniqueness)
     * @return Identifier - of the created object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws UnsupportedEncodingException
     * @throws NotFound
     * @throws ClientSideException
     */
    public Identifier createPublicTestObject(D1Node d1Node, String idSuffix)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique,
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented,
    InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException
    {
        Subject subject = new Subject();
        subject.setValue(Constants.SUBJECT_PUBLIC);
        AccessRule ar = new AccessRule();
        ar.addPermission(Permission.READ);
        ar.addSubject(subject);
        return createTestObject(d1Node, idSuffix, ar);
    }



//	/**
//	 * create a test object, giving the specified subject the permissions specified.
//	 * If permissions are null, then no access policy is setup.  If subject is null,
//	 * the current user/subject will be used.
//	 * The object will be created by the testRightsHolder, and client user will be reset
//	 * to the one it was coming in.
//	 * @param d1Node - the node on which to create the object
//	 * @param permission - the permission level to be granted to the starting/current user/subject
//	 * @param subject - subject to assign the permissions to. If null defaults to current subject of client
//	 * @param idSuffix - if not null, appends the specified string as suffix to auto-generated identifier
//	 *                   (the autogenerated uses timestamps to give reasonable assurances of uniqueness)
//	 * @return Identifier - the identifier of the object created
//	 * @throws InvalidRequest
//	 * @throws NotImplemented
//	 * @throws InvalidSystemMetadata
//	 * @throws InsufficientResources
//	 * @throws UnsupportedType
//	 * @throws IdentifierNotUnique
//	 * @throws NotAuthorized
//	 * @throws ServiceFailure
//	 * @throws InvalidToken
//	 * @throws UnsupportedEncodingException
//	 * @throws NotFound
//	 */
//	protected Identifier createTestObject(D1Node d1Node, String idSuffix, Permission permissionLevel, Subject subject)
//	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique,
//	UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented,
//	InvalidRequest, UnsupportedEncodingException, NotFound
//	{
//		AccessRule = new AccessRule();
//
//
//		return createTestObject(d1Node, pid, accessRule);
//	}

    public Identifier createTestObject(D1Node d1Node, String idSuffix, AccessRule accessRule, ReplicationPolicy replPolicy)
        throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique,
        UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented,
        InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException
    {
        // create the identifier for the test object
        Identifier pid = new Identifier();

        String nodeAbbrev = createNodeAbbreviation(d1Node.getNodeBaseServiceUrl());

        String prefix = d1Node.getClass().getSimpleName() +  "TierTests." + nodeAbbrev + ".";
        if (idSuffix != null) {
            pid.setValue(prefix + ExampleUtilities.generateIdentifier() +
                    "." + idSuffix);
        } else {
            pid.setValue(prefix + ExampleUtilities.generateIdentifier());
        }

        return createTestObject(d1Node, pid, accessRule, replPolicy);
    }
    
    public Identifier createTestObject(D1Node d1Node, String idSuffix, AccessRule accessRule)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique,
    UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented,
    InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException
    {
        // create the identifier for the test object
        Identifier pid = new Identifier();

        String nodeAbbrev = createNodeAbbreviation(d1Node.getNodeBaseServiceUrl());

        String prefix = d1Node.getClass().getSimpleName() +  "TierTests." + nodeAbbrev + ".";
        if (idSuffix != null) {
            pid.setValue(prefix + ExampleUtilities.generateIdentifier() +
                    "." + idSuffix);
        } else {
            pid.setValue(prefix + ExampleUtilities.generateIdentifier());
        }

        return createTestObject(d1Node, idSuffix, accessRule, null);
    }


    public String createNodeAbbreviation(String baseUrl) {
        String nodeAbbrev = baseUrl.replaceFirst("https{0,1}://", "").replaceFirst("\\..+", "");
        return nodeAbbrev;
    }


    /**
     * Convenience method for creating test Object that submits as the
     * @param d1Node
     * @param pid
     * @param accessRule
     * @return
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws UnsupportedEncodingException
     * @throws NotFound
     * @throws ClientSideException
     */
    public Identifier createTestObject(D1Node d1Node, Identifier pid, AccessRule accessRule, String submitterSubject,
            ReplicationPolicy replPolicy)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        // the default is to do all of the creates under the testSubmitter subject
        // and assign rights to testRightsHolder
        if (d1Node instanceof MNode) {
            return createTestObject(d1Node, pid, accessRule, "testSubmitter", "CN=testRightsHolder,DC=dataone,DC=org", replPolicy);
        } else {
            return createTestObject(d1Node, pid, accessRule, submitterSubject, "CN=testRightsHolder,DC=dataone,DC=org", replPolicy);
        }
    }

    public Identifier createTestObject(D1Node d1Node, Identifier pid, AccessRule accessRule, String submitterSubject)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        return createTestObject(d1Node, pid, accessRule, submitterSubject, (ReplicationPolicy) null);
    }
    
    public Identifier createTestObject(D1Node d1Node, Identifier pid, AccessRule accessRule, ReplicationPolicy replPolicy) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        return createTestObject(d1Node, pid, accessRule, cnSubmitter, replPolicy);
    }
    
    public Identifier createTestObject(D1Node d1Node, Identifier pid, AccessRule accessRule) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException, NotFound, ClientSideException {
        return createTestObject(d1Node, pid, accessRule, cnSubmitter, (ReplicationPolicy) null);
    }

    public Identifier createTestObject(D1Node d1Node, Identifier pid,
            AccessRule accessRule, String submitterSubjectLabel, String rightsHolderSubjectName)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        return createTestObject(d1Node, pid, accessRule, submitterSubjectLabel, rightsHolderSubjectName, null);
    }

    public Identifier createTestObject(D1Node d1Node, Identifier pid,
            AccessRule accessRule, String submitterSubjectLabel, String rightsHolderSubjectName,
            ReplicationPolicy replPolicy)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        AccessPolicy policy = null;
        if (accessRule != null) {
            policy = new AccessPolicy();
            policy.addAllow(accessRule);
        }
        return createTestObject(d1Node, pid, policy, submitterSubjectLabel, rightsHolderSubjectName, replPolicy);
    }
    
    /**
     * Creates a test object according to the parameters provided.  The method becomes
     * the submitter client for the create, and restores the client subject/certificate
     * to what it was at the start of the method call.
     * remembers the starting client subject
     * @param d1Node - the node to create the object on
     * @param pid - the identifier for the create object
     * @param policy - the single access rule that will become the AccessPolicy
     *                     for the created object.  null results in null AccessPolicy
     * @param submitterSubjectLabel - label for the submitter subject, to be used as
     *                                the client subject, via setupClientSubject() method
     * @param rightsHolderSubjectName - string value for the rightsHolder subject in the
     *                                   systemMetadata
     * @return the Identifier for the created object
     * @throws InvalidToken
     * @throws ServiceFailure
     * @throws NotAuthorized
     * @throws IdentifierNotUnique
     * @throws UnsupportedType
     * @throws InsufficientResources
     * @throws InvalidSystemMetadata
     * @throws NotImplemented
     * @throws InvalidRequest
     * @throws UnsupportedEncodingException
     * @throws NotFound
     * @throws ClientSideException
     */
    public Identifier createTestObject(D1Node d1Node, Identifier pid,
            AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        return createTestObject(d1Node, pid, null, null, null, policy, submitterSubjectLabel,
                rightsHolderSubjectName, null);
    }
    
    public Identifier createTestObject(D1Node d1Node, Identifier pid,
            AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName,
            ReplicationPolicy replPolicy)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        return createTestObject(d1Node, pid, null, null, null, policy, submitterSubjectLabel,
                rightsHolderSubjectName, replPolicy);
    }
    
    public Identifier createTestObject(D1Node d1Node, Identifier pid, Identifier sid, 
            Identifier obsoletesId, Identifier obsoletedById,
            AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        return createTestObject(d1Node, pid, sid, obsoletesId, obsoletedById, policy,
                submitterSubjectLabel, rightsHolderSubjectName, null);
    }
    
    /**
     * Creates a test object according to the parameters provided.  
     * Also allows setting the SID and the obsoletes / obsoletedBy chain.
     * 
     * The method becomes the submitter client for the create, and restores the client 
     * subject/certificate to what it was at the start of the method call.
     * remembers the starting client subject
     * @param d1Node - the node to create the object on
     * @param pid - the identifier for the create object
     * @param sid - the series identifier for the given pid
     * @param policy - the single access rule that will become the AccessPolicy
     *                     for the created object.  null results in null AccessPolicy
     * @param submitterSubjectLabel - label for the submitter subject, to be used as
     *                                the client subject, via setupClientSubject() method
     * @param rightsHolderSubjectName - string value for the rightsHolder subject in the
     *                                   systemMetadata
     * @return the Identifier for the created object
     */
    public Identifier createTestObject(D1Node d1Node, Identifier pid, Identifier sid, 
            Identifier obsoletesId, Identifier obsoletedById,
            AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName,
            ReplicationPolicy replPolicy)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        // remember who the client currently is
        X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
        String startingCertLoc = CertificateManager.getInstance().getCertificateLocation();
        
        Identifier retPid = null;
        SystemMetadata sysMeta = null;
        
        try {
            setupClientSubject(submitterSubjectLabel);

            // prepare the data object for the create:
            // generate some data bytes as an input stream

            ByteArrayInputStream objectInputStream = null;
            D1Object d1o = null;
            
            // make the submitter the same as the cert DN
            certificate = CertificateManager.getInstance().loadCertificate();
            String submitterX500 = CertificateManager.getInstance().getSubjectDN(certificate);
            
            try {
                //			if (d1Node instanceof MNode) {
                //				byte[] contentBytes = "Plain text source for test object".getBytes("UTF-8");
                //				objectInputStream = new ByteArrayInputStream(contentBytes);
                //				d1o = new D1Object(pid, contentBytes, ExampleUtilities.FORMAT_TEXT_PLAIN, submitterX500, "bogusAuthoritativeNode");
                //			} else {
                byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                objectInputStream = new ByteArrayInputStream(contentBytes);
                NodeReference nodeReference = D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode");
                if(d1Node instanceof MNCallAdapter) {
                    NodeReference nodeIdentifier = ((MNCallAdapter) d1Node).getNode().getIdentifier();
                    if (nodeIdentifier != null)
                        nodeReference = nodeIdentifier;
                    else {
                        try {
                            Node capabilities = ((MNCallAdapter) d1Node).getCapabilities();
                            nodeReference = capabilities.getIdentifier();
                        } catch (Exception e) {
                            log.warn("Unable to get a valid NodeReference for node at: " + d1Node.getNodeBaseServiceUrl() +
                                    " to use as the system metadata's authoritativeMemberNode for created object: " + 
                                    pid + " because the getCapabilities() call failed.", e);
                        }
                    }
                }
                Subject submitterSubject = D1TypeBuilder.buildSubject(submitterX500);
                d1o = new D1Object(pid, contentBytes,
                        D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                        submitterSubject,
                        nodeReference);
                d1o.getSystemMetadata().setSubmitter(submitterSubject);
                sysMeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                sysMeta.setSeriesId(sid);
                sysMeta.setReplicationPolicy(replPolicy);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new ServiceFailure("0000","client misconfiguration related to checksum algorithms");
            } catch (NotFound e) {
                // method misconfiguration related to choice of object format
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
                throw new ServiceFailure("0000","client misconfiguration related to reading of content byte[]");
            } catch (InstantiationException | IllegalAccessException |
                    InvocationTargetException e) {
                log.error("Unable to convert v1 SystemMetadata to v2 SystemMetadata.");
                e.printStackTrace();
            }

            // set the rightsHolder property
            Subject rightsHolder = new Subject();
            rightsHolder.setValue(rightsHolderSubjectName);
            sysMeta.setRightsHolder(rightsHolder);

            // build an AccessPolicy if given an AccessRule
            if (policy != null) {
                sysMeta.setAccessPolicy(policy);
            }

            // CN.create() - but not MN.create() - allows metadata to contain the following
            if(d1Node instanceof CommonCallAdapter && d1Node.getNodeType() == NodeType.CN) {
                sysMeta.setObsoletes(obsoletesId);
                sysMeta.setObsoletedBy(obsoletedById);
            }
            
            // create the test object on the given mNode
            if (log.isInfoEnabled()) {
                log.info("creating a test object.  pid = " + pid.getValue() + " on " + d1Node.getNodeBaseServiceUrl());
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    TypeMarshaller.marshalTypeToOutputStream(sysMeta, os);
                } catch (JiBXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("SystemMetadata for pid: " + pid.getValue() + "\n"
                        + os.toString());
            }

            if (d1Node instanceof MNode) {
                retPid = ((MNode)d1Node).create(null, pid, objectInputStream, sysMeta);
            } else if (d1Node instanceof CNode) {
                retPid = ((CNode)d1Node).create(null, pid, objectInputStream, sysMeta);
            } else if (d1Node instanceof CommonCallAdapter) {
                retPid = ((CommonCallAdapter)d1Node).create(null, pid, objectInputStream, sysMeta);
            } else {
                throw new ClientSideException("Do not have a handler for D1Node of type " + d1Node.getClass().getName());
            }
            log.info("object created.  pid = " + retPid.getValue());
            checkEquals(d1Node.getNodeBaseServiceUrl(),
                    "createTestObject(): returned pid from the create() should match what was given",
                    pid.getValue(), retPid.getValue());
        } catch (BaseException be) {
            be.printStackTrace();
            throw new ClientSideException("Unable to create test object!", be);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // reset the client to the starting subject/certificate
            // (this should work for public user, too, since we create a public user by
            // using a bogus certificate location)
            CertificateManager.getInstance().setCertificateLocation(startingCertLoc);
            if (log.isDebugEnabled()) {
                certificate = CertificateManager.getInstance().loadCertificate();
                String currentX500 = CertificateManager.getInstance().getSubjectDN(certificate);
                log.debug("current client certificate " + currentX500);
            }
        }

        
        return retPid;
    }

    public SystemMetadata createTestSysmeta(D1Node d1Node, Identifier pid, Identifier sid, 
            Identifier obsoletesId, Identifier obsoletedById,
            AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName)
    throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
    InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
    UnsupportedEncodingException, NotFound, ClientSideException
    {
        X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
        String startingCertLoc = CertificateManager.getInstance().getCertificateLocation();
        SystemMetadata sysMeta = null;
        
        try {
            setupClientSubject(submitterSubjectLabel);
            ByteArrayInputStream objectInputStream = null;
            D1Object d1o = null;
            certificate = CertificateManager.getInstance().loadCertificate();
            String submitterX500 = CertificateManager.getInstance().getSubjectDN(certificate);
            
            try {
                byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                objectInputStream = new ByteArrayInputStream(contentBytes);
                d1o = new D1Object(pid, contentBytes,
                        D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                        D1TypeBuilder.buildSubject(submitterX500),
                        D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                sysMeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                sysMeta.setSeriesId(sid);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new ServiceFailure("0000","client misconfiguration related to checksum algorithms");
            } catch (NotFound e) {
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
                throw new ServiceFailure("0000","client misconfiguration related to reading of content byte[]");
            } catch (InstantiationException | IllegalAccessException |
                    InvocationTargetException e) {
                log.error("Unable to convert v1 SystemMetadata to v2 SystemMetadata.");
                e.printStackTrace();
            }

            Subject rightsHolder = new Subject();
            rightsHolder.setValue(rightsHolderSubjectName);
            sysMeta.setRightsHolder(rightsHolder);

            if (policy != null)
                sysMeta.setAccessPolicy(policy);

            if(d1Node instanceof CommonCallAdapter && d1Node.getNodeType() == NodeType.CN) {
                sysMeta.setObsoletes(obsoletesId);
                sysMeta.setObsoletedBy(obsoletedById);
            }
        } catch (BaseException be) {
            be.printStackTrace();
            throw new ClientSideException("Unable to create test object!", be);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // reset certificate
            CertificateManager.getInstance().setCertificateLocation(startingCertLoc);
            if (log.isDebugEnabled()) {
                certificate = CertificateManager.getInstance().loadCertificate();
                String currentX500 = CertificateManager.getInstance().getSubjectDN(certificate);
                log.debug("current client certificate " + currentX500);
            }
        }
        
        return sysMeta;
    }
    
    /**
     * create an accessPolicy that assigns read permission to public subject.
     * This is a common test scenario...
     */
    protected static AccessPolicy buildPublicReadAccessPolicy() {

        AccessRule ar = new AccessRule();
        ar.addPermission(Permission.READ);

        Subject sub = new Subject();
        sub.setValue("public");
        ar.addSubject(sub);

        AccessPolicy ap = new AccessPolicy();
        ap.addAllow(ar);
        return ap;
    }



    /**
     * creates a header line with the text: "*** running test for <string> ***"
     * into the log (INFO level)
     * @param methodName
     */
    public static void printTestHeader(String methodName)
    {
        log.info("\n***************** running test for " + methodName + " *****************");
    }


    /**
     * Tests should use the error collector to handle JUnit assertions
     * and keep going.  The check methods in this class use this errorCollector
     * the check methods
     */
    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    /**
     * performs the junit assertThat method using the errorCollector
     * to record the error and keep going
     *
     * @param message
     * @param s1
     * @param s2
     */
    public void checkEquals(final String host, final String message, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>()
        {
            public Object call() throws Exception
            {
                if (host != null) {
                    assertThat(message + "  [for host " + host + " ]", s1, is(s2));
                } else {
                    assertThat(message, s1, is(s2));
                }
                return null;
            }
        });
    }


    /**
     * performs the equivalent of the junit assertTrue method
     * using the errorCollector to record the error and keep going
     *
     * @param message
     * @param s1
     * @param s2
     */
    public void checkTrue(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>()
        {
            public Object call() throws Exception
            {
                if (host != null) {
                    assertThat(message + "  [for host " + host + " ]", b, is(true));
                } else {
                    assertThat(message, b, is(true));
                }
                return null;
            }
        });
    }

    /**
     * performs the equivalent of the junit assertFalse method
     * using the errorCollector to record the error and keep going
     *
     * @param message
     * @param s1
     * @param s2
     */
    public  void checkFalse(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>()
        {
            public Object call() throws Exception
            {
                if (host != null) {
                    assertThat(message + "  [for host " + host + " ]", b, is(false));
                } else {
                    assertThat(message, b, is(false));
                }
                return null;
            }
        });
    }

    /**
     * performs the equivalent of the junit fail method
     * using the errorCollector to record the error and keep going
     *
     * @param host
     * @param message
     */
    public void handleFail(final String host, final String message)
    {
        errorCollector.checkSucceeds(new Callable<Object>()
        {
            public Object call() throws Exception
            {
                if (host != null) {
                    fail(message + "  [for host " + host + " ]");
                } else {
                    fail(message);
                }
                return null;
            }
        });
    }

    /**
     * Returns the {@link Identifier} for a resource map document, either one 
     * that existed in the system, or one that was newly-created. 
     * ({@link ObjectFormatIdentifier} type: RESOURCE, id: "http://www.openarchives.org/ore/terms")
     * 
     * @param cca the {@link CommonCallAdapter} to use for making the listObjects() call
     * @return the Identifier of a resource map
     * 
     * @throws ClientSideException if no resource map identifier could be located
     */
    public Identifier procureResourceMap(CommonCallAdapter cca) throws ClientSideException {
        return procureResourceMap(cca, null);
    }
    
    /**
     * Returns the {@link Identifier} for a resource map document, either one 
     * that existed in the system, or one that was newly-created. 
     * ({@link ObjectFormatIdentifier} type: RESOURCE, id: "http://www.openarchives.org/ore/terms")
     * 
     * @param cca the {@link CommonCallAdapter} to use for making the listObjects() call
     * @param packagePid if specified, will skip looking for an existing resource map and try to create 
     *              one with this given {@link Identifier} 
     * @return the Identifier of a resource map
     * 
     * @throws ClientSideException if no resource map identifier could be located
     */
    public Identifier procureResourceMap(CommonCallAdapter cca, Identifier packagePid) throws ClientSideException {
        
        ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
        formatID.setValue(RESOURCE_MAP_FORMAT_ID);
        
        Identifier resourceMapPid = null;
        
        if(packagePid == null) {
            ObjectList resourceObjInfo = new ObjectList();
            try {
                resourceObjInfo = cca.listObjects(null, null, null, formatID, null, null);
            } catch (InvalidRequest | InvalidToken | NotAuthorized | NotImplemented | ServiceFailure
                    | ClientSideException e) {
                e.printStackTrace();
                throw new ClientSideException("Unable to fetch a list of resource objects for MNPackage testing. "
                        + "Call to listObjects() failed on :" + cca.getNodeBaseServiceUrl(), e);
            }
            
            List<ObjectInfo> objectInfoList = resourceObjInfo.getObjectInfoList();
            for (ObjectInfo objectInfo : objectInfoList) {
                Identifier id = objectInfo.getIdentifier();
                
                InputStream is = null;
                try {
                    is = cca.get(null, id);
                    resourceMapPid = objectInfo.getIdentifier();
                    break;
                } catch (Exception e) {
                    continue;
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        
        // no existing resource map? create a package
        if (resourceMapPid == null) {
            resourceMapPid = createPackage(cca, packagePid, null, null, null);
        }
        
        if (resourceMapPid == null)
            throw new ClientSideException("Unable to fetch a resource map for MNPackage testing.");
        
        return resourceMapPid;
    }
    
    /**
     * Returns the {@link Identifier} for a newly-created package, meaning the same
     * Identifier as that of the resource map document.
     * ({@link ObjectFormatIdentifier} type: RESOURCE, id: "http://www.openarchives.org/ore/terms")
     * 
     * @param cca the {@link CommonCallAdapter} to use for making the listObjects() call
     * @param packagePid the pid of this package / resource map (may be null)
     * @param packageSid the seriesId of this package / resource map (may be null)
     * @param obsoletes the Identifier of the package / resource map that this one is obsoleting (may be null)
     * @param obsoletedBy the Identifier of the package / resource map that this one is obsoleted by (may be null)
     * 
     * @return the Identifier of the package / resource map
     * 
     * @throws ClientSideException if no resource map identifier could be located
     */
    public Identifier createPackage(CommonCallAdapter cca, Identifier packagePid, Identifier packageSid, Identifier obsoletes, Identifier obsoletedBy) throws ClientSideException {
        
        ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
        formatID.setValue(RESOURCE_MAP_FORMAT_ID);
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
//        Subject subject = getSubject("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.READ);
        
        Identifier scimetaPid = null;
        Identifier dataObjPid = null;
        Identifier resourceMapPid = null;
        if(packagePid != null)
            resourceMapPid = packagePid;
        else
            resourceMapPid = D1TypeBuilder.buildIdentifier("testPackage_resourceMap_" + ExampleUtilities.generateIdentifier());
        
        
        // create science metadata object
        try {
            scimetaPid = createTestObject(cca, D1TypeBuilder.buildIdentifier("testPackage_scimeta_" + ExampleUtilities.generateIdentifier()), accessRule);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClientSideException("Unable to create object for MNPackage testing.", e);
        }
        
        // create data object
        try {
            dataObjPid = createTestObject(cca, "testPackage_dataObj_", accessRule);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClientSideException("Unable to create metadata for MNPackage testing.", e);
        }
    
        InputStream objectInputStream = null;
        
        // create resource map
        try {
//            DataPackage dataPackage = new DataPackage(resourceMapPid);
//            List<Identifier> dataIds = new ArrayList<Identifier>();
//            dataIds.add(dataObjPid);
//            dataPackage.insertRelationship(scimetaPid, dataIds);
//            
//            String resourceMapText = dataPackage.serializePackage();
//            assertNotNull(resourceMapText);
//            byte[] resourceMapBytes = resourceMapText.getBytes("UTF-8");
            
            Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
            List<Identifier> dataIds = new ArrayList<Identifier>();
            dataIds.add(dataObjPid);
            idMap.put(scimetaPid, dataIds);
            
            ResourceMapFactory rmf = ResourceMapFactory.getInstance();
            ResourceMap resourceMap = rmf.createResourceMap(resourceMapPid, idMap);
            String rdfXml = rmf.serializeResourceMap(resourceMap);
            byte[] resourceMapBytes = rdfXml.getBytes("UTF-8");
            
            objectInputStream = new ByteArrayInputStream(resourceMapBytes);
            D1Object d1o = new D1Object(resourceMapPid, resourceMapBytes,
                    D1TypeBuilder.buildFormatIdentifier(RESOURCE_MAP_FORMAT_ID),
                    D1TypeBuilder.buildSubject(subject.getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            
            SystemMetadata sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            sysmeta.setSeriesId(packageSid);
            sysmeta.setObsoletes(obsoletes);
            sysmeta.setObsoletedBy(obsoletedBy);
            
            // is this an MN.update()?
            if (cca instanceof MNCallAdapter && obsoletes != null)
                ((MNCallAdapter)cca).update(null, obsoletes, objectInputStream, sysmeta.getIdentifier(), sysmeta);
            else // or a regular MN or CN create()?
                cca.create(null, sysmeta.getIdentifier(), objectInputStream, sysmeta);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClientSideException("Unable to create resource map for MNPackage testing, because : " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(objectInputStream);
        }
        
        return resourceMapPid;
    }

}
