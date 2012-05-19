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

package org.dataone.integration.it;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1Node;
import org.dataone.client.D1Object;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
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
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

/**
 * DO NOT ADD tests to this class directly!  Subclass, subclass, subclass!!! :-) 
 * 
 * This class is intended as a base class that implements the standard IntegrationTestContextParameters
 * 
 * There is a setUp routine that parses configurations into objects useful for running tests in a given
 * context.  It also sets up the logger, and errorCollector logic (see @Rule, and  protected "check" methods)
 * 
 * @author rnahf
 *
 */
public abstract class ContextAwareTestCaseDataone implements IntegrationTestContextParameters {

	public static final String QUERYTYPE_SOLR = "solr";
	public static final String CHECKSUM_ALGORITHM = "MD5";
	public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_EML_2_0_1;
//	public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_TEXT_PLAIN;
//	public static final String DEFAULT_TEST_OBJECTFORMAT = ExampleUtilities.FORMAT_EML_2_0_0;
	
	private  boolean alreadySetup = false;
	
	protected static Log log = LogFactory.getLog(ContextAwareTestCaseDataone.class);
	
	// variables to the context interface parameters
	protected  String testContext = null;
	protected  String cnBaseUrl = null;
	protected  String mnBaseUrl = null;
	protected  String nodelistUri = null;

	// this here defines the default
	// can be overwritten by property passed into base class
	protected String testObjectSeriesSuffix = "." + "14";
	protected  String testObjectSeries = null;

	protected String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "urn:node:cnDev");
	
	public  List<Node> memberNodeList = null;
	public  List<Node> coordinatingNodeList = new Vector<Node>();
	public  List<Node> monitorNodeList = new Vector<Node>();
	
	
	protected abstract String getTestDescription();
	
	public class TestIterationEndingException extends Exception
	{
		/**
		 * auto-generated by eclipse
		 */
		private static final long serialVersionUID = 5496467831748815597L;

		public TestIterationEndingException(String message)
		{
			super(message);
		}
		
		public TestIterationEndingException(String message, Exception cause) {
			super(message,cause);
		}
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
			
			testContext = Settings.getConfiguration().getString(TestSettings.CONTEXT_LABEL);			
			cnBaseUrl = Settings.getConfiguration().getString(PARAM_CN_URL);
			mnBaseUrl = Settings.getConfiguration().getString(PARAM_MN_URL);
			nodelistUri = Settings.getConfiguration().getString(PARAM_NODELIST_URI);
			
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
				memberNodeList = new Vector<Node>();
				memberNodeList.add(n);
				log.info("*** Adding MN to list: [" + n.getBaseURL() +"]");
			} else if (cnBaseUrl != null) {
				CNode cn = new CNode(cnBaseUrl);
				System.out.println("~~~ Context is solo CoordinatingNode: " + cn.getNodeBaseServiceUrl());
				memberNodeList = new Vector<Node>();
				Node n = new Node();
				n.setBaseURL(cnBaseUrl);
				coordinatingNodeList.add(n);	
			} else {
				// we will be testing multiple member nodes
				List<Node> allNodesList = new Vector<Node>();
				memberNodeList = new Vector<Node>();
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
					// use the context specified by D1Client
					CNode cn = D1Client.getCN();
					System.out.println("~~~ Context is from D1Client: " + cn.getNodeBaseServiceUrl());
					allNodesList = cn.listNodes().getNodeList();
				} 
				// divide into separate lists
				for(int i=0; i < allNodesList.size(); i++) {
					Node currentNode = allNodesList.get(i);
					if (currentNode.getType() == NodeType.CN) {
						coordinatingNodeList.add(currentNode);
						log.info("*** Adding CN to list: " + currentNode.getName() + " [" + currentNode.getBaseURL() +"]");
					} else if (currentNode.getType() == NodeType.MN) {
						memberNodeList.add(currentNode);
						log.info("*** Adding MN to list: " + currentNode.getName() + " [" + currentNode.getBaseURL() +"]");
					} else if (currentNode.getType() == NodeType.MONITOR) {
						monitorNodeList.add(currentNode);
						log.info("*** Adding MonitorNode to list: " + currentNode.getName() + " [" + currentNode.getBaseURL() +"]");
					} else {
						log.warn("Node from nodelist is not of recognizable type: [" +
								currentNode.getType() + "]. Removing from test list: " + 
								currentNode.getName() + ": " + currentNode.getBaseURL());
					}
				}			
			} // nodelist set up

			log.info("****************************************************");
		}  // settings already set up
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
	
	protected Iterator<Node> getCoordinatingNodeIterator() {
		return coordinatingNodeList.iterator();
	}
	
	protected Iterator<Node> getMonitorNodeIterator() {
		return monitorNodeList.iterator();
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
	 * uses the value of the property passed to setup the
	 * CertificateManager to use the certificate found at that path
	 * @return
	 */
	protected static Subject setupClientSubject(String subjectName) 
	{		
		// 1. set up the client certificate
		String testCertDirectory = (String) Settings.getConfiguration().getProperty("d1.test.cert.location");	
				
		CertificateManager cm = CertificateManager.getInstance();
		cm.setCertificateLocation(testCertDirectory + subjectName + ".crt");
		cm.loadCertificate();

		// 2. return the subject corresponding to the loaded certificate		
		Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
		
		log.info("client setup as Subject: " + clientSubject.getValue());
		return clientSubject;
	}
	

	protected ObjectList procureObjectList(D1Node d1Node) 
	throws TestIterationEndingException 
	{
		return procureObjectList(d1Node,false);
	}

    /**
     * get an ObjectList from listObjects as the current user, and if empty, 
     * try to create a public readable object.
     * @param d1Node
     * @param getAll - sets the start and count parameter to get the entire list.
     * @return
     * @throws TestIterationEndingException
     */
    protected ObjectList procureObjectList(D1Node d1Node, boolean getAll) 
    throws TestIterationEndingException 
    {
    	ObjectList objectList = null;
    	try {
    		if (getAll) {
    			objectList = d1Node.listObjects(null, null, null, null, null, 0, 0);
    			objectList = d1Node.listObjects(null, null, null, null, null, 0, objectList.getTotal());
    		} else {
    			objectList = d1Node.listObjects(null);
    		}
    	} catch (BaseException e) {
			throw new TestIterationEndingException("unexpected error thrown by listObjects()", e);
		}
    	if (objectList.getTotal() == 0) {
    		try {
				createPublicTestObject(d1Node,"");
				objectList = d1Node.listObjects(null);
				if (objectList.getTotal() == 0) {
					throw new TestIterationEndingException("could not find or create an object for use by listObjects().");
				}
			} catch (BaseException e) {
				throw new TestIterationEndingException("could not find or create an object for use by listObjects().", e);
			} catch (UnsupportedEncodingException e) {
				throw new TestIterationEndingException("could not find or create an object for use by listObjects().", e);
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
     * @param d1Node
     * @return Identifier for object to be used for testing
     * @throws TestIterationEndingException 
     */
	protected Identifier procurePublicReadableTestObject(D1Node d1Node, Identifier firstTry) 
	throws TestIterationEndingException
	{
		Identifier identifier = null;
		try {
			identifier  = procureTestObject(
					d1Node,
					D1TypeBuilder.buildAccessRule(
							Constants.SUBJECT_PUBLIC,
							Permission.READ),
					firstTry);
		}
		catch (Exception e) {
			; // fallback to plan B
		}		
		
		if (identifier == null) {

			try {
				ObjectList ol = d1Node.listObjects(null);
				if (ol != null && ol.getCount() > 0) {
					long start = (new Date()).getTime();
					for (ObjectInfo oi: ol.getObjectInfoList()) {
						try {
							SystemMetadata smd = d1Node.getSystemMetadata(null,oi.getIdentifier());
							if (AccessUtil.getPermissionMap(smd.getAccessPolicy())
									.containsKey(Constants.SUBJECT_PUBLIC)) 
							{
								identifier = oi.getIdentifier();
								break;
							} 
						} catch (BaseException e) {
							; 
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
			}
		}
		
		if (identifier == null)
			// nothing public found
			throw new TestIterationEndingException("could not create a test object and" +
					" could not find object with a public accessRule in reasonable amount of time");
		
		return identifier;
	}
    
    
	/**
	 * get an existing object for testing, and failing that attempt to create one (not all nodes will allow this).
	 * @param  d1Node - the MNode or CNode object from where to procure the object Identifier
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
	protected Identifier procureTestObject(D1Node d1Node, AccessRule accessRule, Identifier pid) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, 
	InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, 
	UnsupportedEncodingException, NotFound, TestIterationEndingException 
	{
		Identifier identifier = null;
		try {
			log.debug("procureTestObject: checking system metadata of requested object");
			SystemMetadata smd = d1Node.getSystemMetadata(null, pid);
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
			if (d1Node instanceof MNode) {
				Node node = ((MNode) d1Node).getCapabilities();
				if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
					log.debug("procureTestObject: calling createTestObject");
					identifier = createTestObject(d1Node, pid, accessRule);
				}
			} else {
				log.debug("procureTestObject: calling createTestObject");
				identifier = createTestObject(d1Node, pid, accessRule);
			}
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
	 * @param  d1Node - the MNode or CNode object from where to procure the object Identifier
	 * @param  permissionLevel - the permission-level of the object retrieved needed
	 * @param  subjectFilter - if not null, means the permissions specified have to be 
	 * 						   explicitly assigned in the systemmetadata accessPolicy
	 * 						   to the provided subject
	 * @param  checkUsingIsAuthorized - if true will use IsAuthorized(permissionLevel)
	 *                         instead of checking the systemMetadata
	 * @return - Identifier for the readable object, if found, otherwise null
	 */	
	@Deprecated
	protected Identifier getTestObject(D1Node d1Node, Subject subjectFilter, 
			Permission permissionLevel, boolean checkUsingIsAuthorized) 	
	{
		Identifier id = null;		
		try {
			ObjectList ol = d1Node.listObjects(null);
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
							SystemMetadata smd = d1Node.getSystemMetadata(null, id);

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
							d1Node.isAuthorized(null,id, permissionLevel);
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
			handleFail(d1Node.getNodeBaseServiceUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
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
	 */
	protected Identifier createPublicTestObject(D1Node d1Node, String idSuffix) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
	UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, 
	InvalidRequest, UnsupportedEncodingException, NotFound 
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
	
	protected Identifier createTestObject(D1Node d1Node, String idSuffix, AccessRule accessRule) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
	UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, 
	InvalidRequest, UnsupportedEncodingException, NotFound 
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

		return createTestObject(d1Node, pid, accessRule);
	}
	
	
	protected String createNodeAbbreviation(String baseUrl) {
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
	 */
	protected Identifier createTestObject(D1Node d1Node, Identifier pid, AccessRule accessRule) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, 
	InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, 
	UnsupportedEncodingException, NotFound
	{
		// the default is to do all of the creates under the testSubmitter subject
		// and assign rights to testRightsHolder
		if (d1Node instanceof MNode) {
			return createTestObject(d1Node, pid, accessRule,"testSubmitter","CN=testRightsHolder,DC=dataone,DC=org");
		} else {
			return createTestObject(d1Node, pid, accessRule,cnSubmitter,"CN=testRightsHolder,DC=dataone,DC=org");
		}
	}
	
	
	protected Identifier createTestObject(D1Node d1Node, Identifier pid, 
			AccessRule accessRule, String submitterSubjectLabel, String rightsHolderSubjectName) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, 
	InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, 
	UnsupportedEncodingException, NotFound
	{
		AccessPolicy policy = null;
		if (accessRule != null) {
			policy = new AccessPolicy();
			policy.addAllow(accessRule);
		}
		return createTestObject( d1Node, pid, policy, submitterSubjectLabel, rightsHolderSubjectName);	
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
	 */
	protected Identifier createTestObject(D1Node d1Node, Identifier pid, 
			AccessPolicy policy, String submitterSubjectLabel, String rightsHolderSubjectName) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, 
	InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, 
	UnsupportedEncodingException, NotFound
	{
		// remember who the client currently is
		X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
		String startingCertLoc = CertificateManager.getInstance().getCertificateLocation();
		
		Identifier retPid = null;
		
		try {
			setupClientSubject(submitterSubjectLabel);


			// prepare the data object for the create:
			// generate some data bytes as an input stream		

			ByteArrayInputStream objectInputStream = null;
			D1Object d1o = null;
			SystemMetadata sysMeta = null;
			try {
				// make the submitter the same as the cert DN 
				certificate = CertificateManager.getInstance().loadCertificate();
				String submitterX500 = CertificateManager.getInstance().getSubjectDN(certificate);

				//			if (d1Node instanceof MNode) {
				//				byte[] contentBytes = "Plain text source for test object".getBytes("UTF-8");
				//				objectInputStream = new ByteArrayInputStream(contentBytes);
				//				d1o = new D1Object(pid, contentBytes, ExampleUtilities.FORMAT_TEXT_PLAIN, submitterX500, "bogusAuthoritativeNode");
				//			} else {
				byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
				objectInputStream = new ByteArrayInputStream(contentBytes);
				d1o = new D1Object(pid, contentBytes,
						D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
						D1TypeBuilder.buildSubject(submitterX500),
						D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
				//			}
				sysMeta = d1o.getSystemMetadata();
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
			}

			// set the rightsHolder property
			Subject rightsHolder = new Subject();
			rightsHolder.setValue(rightsHolderSubjectName);
			sysMeta.setRightsHolder(rightsHolder);

			// build an AccessPolicy if given an AccessRule
			if (policy != null) {
				sysMeta.setAccessPolicy(policy);
			}

			// create the test object on the given mNode		
			if (log.isInfoEnabled()) {
				log.info("creating a test object.  pid = " + pid.getValue());
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
			} else {
				retPid = ((CNode)d1Node).create(null, pid, objectInputStream, sysMeta);
			}
			log.info("object created.  pid = " + retPid.getValue());
			checkEquals(d1Node.getNodeBaseServiceUrl(),
					"createTestObject(): returned pid from the create() should match what was given",
					pid.getValue(), retPid.getValue());

			
		}
		finally {
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
                	assertThat(message + "  [for host " + host + "]", s1, is(s2));
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
            		assertThat(message + "  [for host " + host + "]", b, is(true));
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
            		assertThat(message + "  [for host " + host + "]", b, is(false));
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
            		fail(message + "  [for host " + host + "]");
            	} else {
            		fail(message);
            	}
                return null;
            }
        });
    }
	
}
