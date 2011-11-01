package org.dataone.integration.it;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.junit.AfterClass;
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

	public static final String QUERYTYPE_SOLR = "SOLR";
	public static final String CHECKSUM_ALGORITHM = "MD5";
	
	
	private  boolean alreadySetup = false;
	
	protected static Log log = LogFactory.getLog(ContextAwareTestCaseDataone.class);
	
	// variables to the context interface parameters
	protected  String testContext = null;
	protected  String cnBaseUrl = null;
	protected  String mnBaseUrl = null;
	protected  String nodelistUri = null;

	public  List<Node> memberNodeList = null;
	public  List<Node> coordinatingNodeList = new Vector<Node>();
	public  List<Node> monitorNodeList = new Vector<Node>();
	
	protected abstract String getTestDescription();
	
//	/**
//	 * this is needed for running from a servlet, which keeps the
//	 * runner (and this class) initialized after execution.
//	 * 
//	 * @throws Exception
//	 */
//	@AfterClass
//	public static void teardown() throws Exception {
//		alreadySetup = false;
//	}
	
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
			
			String urlThrID = System.getProperty("mnwebtester.thread." 
					+ Thread.currentThread().getId() + ".mn.baseurl");
			if (urlThrID != null) {
				log.info("*** mn.baseurl obtained from thread.X.mn.baseurl property");
				mnBaseUrl = urlThrID;
			} else if (mnBaseUrl != null) {
				log.info("*** mn.baseurl set from context.mn.baseurl property");
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
					URL url = new URL(nodelistUri);
					InputStream is = url.openStream();
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
	
	protected Iterator<Node> getMemberNodeIterator() {
		return memberNodeList.iterator();
	}
	
	protected Iterator<Node> getCoordinatingNodeIterator() {
		return coordinatingNodeList.iterator();
	}
	
	protected Iterator<Node> getMonitorNodeIterator() {
		return monitorNodeList.iterator();
	}
	
	/**
	 * uses the property "test.subject.writer.certLocation" to setup
	 * the CertificateManager to use the certificate found at that path
	 * @return
	 */
	public static Subject setupClientSubject_Writer(){
		return setupClientSubject("test.subject.writer.certLocation");
	}
	
	/**
	 * uses the property "test.subject.reader.certLocation" to setup
	 * the CertificateManager to use the certificate found at that path
	 * @return
	 */
	public static Subject setupClientSubject_Reader(){
		return setupClientSubject("test.subject.reader.certLocation");
	}
	
	/**
	 * uses the property "test.subject.norights.certLocation" to setup
	 * the CertificateManager to use the certificate found at that path
	 * @return
	 */
	public static Subject setupClientSubject_NoRights(){
		return setupClientSubject("test.subject.norights.certLocation");
	}
	
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
	protected static Subject setupClientSubject(String certificatePathKeyName) 
	{
		String certPath = (String) Settings.getConfiguration().getProperty(certificatePathKeyName);	
		URL url = ContextAwareTestCaseDataone.class.getClassLoader().getResource(certPath);
		CertificateManager.getInstance().setCertificateLocation(url.getPath());
		String subjectDN = CertificateManager.getInstance().loadCertificate().getSubjectDN().toString();//getSession(null);
		Subject subject = new Subject();
		subject.setValue(subjectDN);
		log.info("client setup as Subject: " + subjectDN);
		return subject;
	}
	
	
	/**
	 * get an existing object from the member node, if none available, try to 
	 * create one (not all MN's will allow this).
	 * @param  mNode - the MNode object from where to procure the object Identifier
	 * @param  permission - the permission-level of the object retrieved needed
	 * @return - Identifier for the readable object.
	 * @throws - IndexOutOfBoundsException  - when can't procure an object Identifier
	 */
	protected Identifier procureTestObject(MNode mNode, Permission[] permissions) 
	{
		Identifier id = null;
		try {
			ObjectList ol = mNode.listObjects();
			if (ol.getTotal() > 0) {
				// found one
				if (permissions.length == 1 && permissions[0] == Permission.READ) {
					id = ol.getObjectInfo(0).getIdentifier();
				} else {
					for(int i=0; i<= ol.sizeObjectInfoList(); i++) {
						id = ol.getObjectInfo(i).getIdentifier();
						// check all permissions 
						try {
							for (Permission p : permissions) {
								mNode.isAuthorized(null,id, p);
							}
							break;  // the outer for loop
						} catch (NotAuthorized na) {
							// effectively break out of the inner for loop (give up on current pid)
						}
						id = null;
					}
				}
			} else {
				id = createTestObject(mNode,permissions);
			}
		} 
		catch (BaseException e) {
			handleFail(mNode.getNodeBaseServiceUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
		}
		catch(Exception e) {
			log.warn(e.getClass().getName() + ": " + e.getMessage());
		}
		if (id == null) {
			throw new IndexOutOfBoundsException();
		} 
		log.info(" ====>>>>> pid of procured test Object: " + id.getValue());
		return id;
	}

	/**
	 * create a test object, giving the current user/subject the permissions specified.
	 * The object will be created by the testUserWriter, and client user will be reset
	 * to the one it was coming in.   
	 * @param mNode - the member node on which to create the object
	 * @param permission - the permissions to be granted to the starting/current user/subject
	 * @return Identifier - the identifier of the object created
	 * @throws InvalidRequest 
	 * @throws NotImplemented 
	 * @throws InvalidSystemMetadata 
	 * @throws InsufficientResources 
	 * @throws UnsupportedType 
	 * @throws IdentifierNotUnique 
	 * @throws NotAuthorized 
	 * @throws ServiceFailure 
	 * @throws InvalidToken 
	 * @throws UnsupportedEncodingException 
	 */
	protected Identifier createTestObject(MNode mNode, Permission[] permissions) throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, UnsupportedEncodingException 
	{
		// remember who the client currently is
		X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
		String startingCertLoc = CertificateManager.getInstance().getCertificateLocation();
		String startingClientSubject = null;
		if (certificate != null) {
			startingClientSubject = CertificateManager.getInstance().getSubjectDN(certificate);
		} else {
			startingClientSubject = Constants.SUBJECT_PUBLIC;
		}
		// do the create as the test Writer subject
		setupClientSubject_Writer();
		
		Identifier pid = new Identifier();
		pid.setValue("MNodeTier1IT." + ExampleUtilities.generateIdentifier());

		// get some data bytes as an input stream
		ByteArrayInputStream textPlainSource = 
			new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));

		// build the system metadata object
		SystemMetadata sysMeta = 
			ExampleUtilities.generateSystemMetadata(pid, "text/plain", textPlainSource, null);
		
		try {
			// make the submitter the same as the cert DN 
			certificate = CertificateManager.getInstance().loadCertificate();
			String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
			sysMeta.getRightsHolder().setValue(ownerX500);
			
			// extend the appropriate access to starting client subject
			File certloc = new File(startingCertLoc);
			AccessPolicy ap = null;
			if (certloc.exists()) {
				 ap = AccessUtil.createSingleRuleAccessPolicy(
						new String[] {startingClientSubject},permissions);
			} else {
				// it's the public user and public can only be given Permission.READ
				ap = buildPublicReadAccessPolicy();
			}
			sysMeta.setAccessPolicy(ap);

		} catch (Exception e) {
			// warn about this?
			e.printStackTrace();
			throw new IndexOutOfBoundsException("could not set AccessPolicy: " + e.getMessage());
		}
	      
		log.info("create a test object");
		
		Identifier retPid = mNode.create(null, pid, textPlainSource, sysMeta);
		checkEquals(mNode.getNodeBaseServiceUrl(),
				"procureTestObject(): returned pid from the create() should match what was given",
				pid.getValue(), retPid.getValue());

		// reset the client to the starting subject/certificate
		// (this should work for public user, too, since we create a public user by 
		// using a bogus certificate location)
		CertificateManager.getInstance().setCertificateLocation(startingCertLoc);
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

	
//	/**
//	 * create an accessPolicy that assigns read permission to public subject.
//	 * This is a common test scenario...
//	 */
//	protected static AccessPolicy buildSingleRuleAccessPolicy(String[] subjectStrings, Permission[] permissions) {
//	}
//		
//	protected static AccessPolicy buildSingleRuleAccessPolicy(Subject[] subject, Permission[] permissions) {
//
//		AccessRule ar = new AccessRule();
//		for (Permission permission: permissions) {
//			ar.addPermission(permission);
//		}
//
//		for (Object subject : subjectStrings) {			
//			Subject s = null;
//			if (subject instanceof String) {
//				s = new Subject();
//				s.setValue((String)subject);
//			} else if (subject instanceof Subject) {
//				s = (Subject) subject;
//			}
//			ar.addSubject(s);
//		}
//		
//		AccessPolicy ap = new AccessPolicy();
//		ap.addAllow(ar);
//    	return ap;
//	}
	
	
	/**
	 * creates a header line with the text: "*** running test for <string> ***" 
	 * into the log (INFO level)
	 * @param methodName
	 */
	protected void printTestHeader(String methodName)
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
    protected void checkEquals(final String host, final String message, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                if (host != null) {
                	assertThat("for host: " + host + ":: " + message, s1, is(s2));
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
    protected void checkTrue(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for host: " + host + ":: " + message, true, is(b));
            	} else {
            		assertThat(message, true, is(b));
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
    protected void checkFalse(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for host: " + host + ":: " + message, false, is(b));
            	} else {
            		assertThat(message, false, is(b));
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
    protected void handleFail(final String host, final String message)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		fail("for host: " + host + ":: " + message);
            	} else {
            		fail(message);
            	}
                return null;
            }
        });
    }
	
}
