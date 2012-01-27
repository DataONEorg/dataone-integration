package org.dataone.integration.it;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1Node;
import org.dataone.client.D1Object;
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
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
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
	
	// common object formats to test
  protected static final String format_text_plain  = "text/plain";
	protected static final String format_text_csv    = "text/csv";
  protected static final String format_eml_2_0_0   = "eml://ecoinformatics.org/eml-2.0.0";
  protected static final String format_eml_2_0_1   = "eml://ecoinformatics.org/eml-2.0.1";
  protected static final String format_eml_2_1_0   = "eml://ecoinformatics.org/eml-2.1.0";
  protected static final String format_eml_2_1_1   = "eml://ecoinformatics.org/eml-2.1.1";
  
  // paths to common science data and metadata examples for the above formats
  protected static final String scidata_text_plain = "/d1_testdocs/eml200/IPCC.200802107062739.1"; 
  protected static final String scidata_text_csv   = "/d1_testdocs/eml201/TPT001_018MHP2000R00_20110121.40.1.csv"; 
  protected static final String scimeta_eml_2_0_0  = "/d1_testdocs/eml201/dpennington.195.2"; 
  protected static final String scimeta_eml_2_0_1  = "/d1_testdocs/eml201/TPT001_018MHP2000R00_20110121.50.1.xml"; 
  protected static final String scimeta_eml_2_1_0  = "/d1_testdocs/eml201/peggym.130.4"; 
  // TODO: protected static final String scimeta_eml_2_1_1  = "need to get a 2.1.1 test doc"; 
	
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
		String testCertDirectory = (String) Settings.getConfiguration().getProperty("d1.test.cert.location");	
		
		URL url = ContextAwareTestCaseDataone.class.getClassLoader().getResource(testCertDirectory + subjectName +".crt");
		CertificateManager.getInstance().setCertificateLocation(url.getPath());
		
		String subjectDN = CertificateManager.getInstance().loadCertificate().getSubjectDN().toString();
		Subject subject = new Subject();
		subject.setValue(subjectDN);
		log.info("client setup as Subject: " + subjectDN);
		return subject;
	}
	
	/**
	 * creates the identifier, data inputstream, and sysmetadata for testing purposes
	 * the rightsHolder is set to the subject of the current certificate (user)
	 * 
	 * uses a default text/plain data source
	 */
    protected Object[] generateTestDataPackage(String idString, boolean isPrefix)
        throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException {
        
        return generateTestDataPackage(idString, isPrefix, format_text_plain);
    }
	
  /**
   * creates the identifier, data inputstream, and sysmetadata for testing purposes
   * the rightsHolder is set to the subject of the current certificate (user)
   */
    protected Object[] generateTestDataPackage(String idString,
        boolean isPrefix, String formatString)
        throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException {
        
        if (isPrefix) {
            idString += ExampleUtilities.generateIdentifier();
        }
        Identifier guid = new Identifier();
        guid.setValue(idString);

        byte[] contentBytes = null;
        InputStream fileStream = null;
        
        // choose a test object file based on the object format passed in
        if ( formatString == format_text_plain ) {
            fileStream = this.getClass().getResourceAsStream(scidata_text_plain);
            
        } else if ( formatString == format_eml_2_0_0 ) {
            fileStream = this.getClass().getResourceAsStream(scimeta_eml_2_0_0);

        } else if ( formatString == format_eml_2_0_1 ) {
            fileStream = this.getClass().getResourceAsStream(scimeta_eml_2_0_1);

        } else if ( formatString == format_eml_2_1_0 ) {
            fileStream = this.getClass().getResourceAsStream(scimeta_eml_2_1_0);
        
        //TODO: get an EML 2.1.1 test doc in place
        //} else if ( formatString == format_eml_2_1_1 ) {
        //    fileStream = this.getClass().getResourceAsStream(scimeta_eml_2_1_1);
            
        }
        
        contentBytes = IOUtils.toByteArray(fileStream);        
        
        // figure out who we are
        String ownerX500 = idString + "_unknownCert";
        try {
            X509Certificate certificate = CertificateManager.getInstance()
                    .loadCertificate();
            if (certificate != null) {
                ownerX500 = CertificateManager.getInstance().getSubjectDN(
                        certificate);
                // sysMeta.getRightsHolder().setValue(ownerX500);
                // sysMeta.getSubmitter().setValue(ownerX500);
            }
        } catch (Exception e) {
            // ignore
        }

        D1Object d1o = new D1Object(guid, contentBytes, formatString,
                ownerX500, "authNode");
        SystemMetadata sysMeta = d1o.getSystemMetadata();

        // match the submitter as the cert DN

        sysMeta.setAccessPolicy(AccessUtil.createSingleRuleAccessPolicy(
                new String[] { Constants.SUBJECT_PUBLIC },
                new Permission[] { Permission.READ }));

        ByteArrayInputStream bis = new ByteArrayInputStream(d1o.getData());
        return new Object[] { guid, bis, sysMeta };
    }  

	
	/**
	 * get an existing object from the node, if none available, try to 
	 * create one (not all nodes will allow this).
	 * @param  d1Node - the MNode or CNode object from where to procure the object Identifier
	 * @param  permissionLevel - the permission-level of the object retrieved needed
	 * @param  checkUsingIsAuthorized - if true will use IsAuthorized(permissionLevel)
	 *                         instead of checking the systemMetadata
	 * @return - Identifier for the readable object.
	 * @throws - IndexOutOfBoundsException  - when can't procure an object Identifier
	 */
	protected Identifier procureTestObject(D1Node d1Node, Permission permissionLevel, boolean checkUsingIsAuthorized) 
	{
		return procureTestObject(d1Node,null,permissionLevel, checkUsingIsAuthorized);
	}

	
	/**
	 * get an existing object from the member node, if none available, try to 
	 * create one (not all nodes will allow this).
	 * @param  d1Node - the MNode or CNode object from where to procure the object Identifier
	 * @param  permission - the permission-level of the object retrieved needed
	 * @param  subjectFilter - if not null, means the permissions specified have to be 
	 * 						   explicitly assigned in the systemmetadata accessPolicy
	 * 						   to the provided subject
	 * @param  checkUsingIsAuthorized - if true will use IsAuthorized(permissionLevel)
	 *                         instead of checking the systemMetadata
	 * @return - Identifier for the readable object.
	 * @throws - IndexOutOfBoundsException  - when can't procure an object Identifier
	 */
	protected Identifier procureTestObject(D1Node d1Node, Subject subjectFilter, 
			Permission permissionLevel, boolean checkUsingIsAuthorized) 
	{
		Identifier id = getTestObject(d1Node, subjectFilter, permissionLevel, checkUsingIsAuthorized);
		if (id == null) {
			log.debug("procureTestObject: calling createTestObject...");
			try {
				id = createTestObject(d1Node, null, permissionLevel, subjectFilter);
			} catch (BaseException e) {
				handleFail(d1Node.getNodeBaseServiceUrl(),e.getClass().getSimpleName() + ":: " + e.getDescription());
			} catch (UnsupportedEncodingException e) {
				log.error(e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
		if (id == null) {
			throw new IndexOutOfBoundsException("could not procure (get or create) suitable test object");
		}
		log.info(" ====>>>>> pid of procured test Object: " + id.getValue());
		return id;
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
		return createTestObject(d1Node, idSuffix, Permission.READ, subject);
	}
	
	/**
	 * create a test object, giving the specified subject the permissions specified.
	 * If permissions are null, then no access policy is setup.  If subject is null,
	 * the current user/subject will be used. 
	 * The object will be created by the testOwner, and client user will be reset
	 * to the one it was coming in.   
	 * @param d1Node - the node on which to create the object
	 * @param permission - the permission level to be granted to the starting/current user/subject
	 * @param subject - subject to assign the permissions to. If null defaults to current subject of client
	 * @param idSuffix - if not null, appends the specified string as suffix to auto-generated identifier
	 *                   (the autogenerated uses timestamps to give reasonable assurances of uniqueness)
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
	 * @throws NotFound 
	 */
	protected Identifier createTestObject(D1Node d1Node, String idSuffix, Permission permissionLevel, Subject subject) 
	throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, 
	UnsupportedType, InsufficientResources, InvalidSystemMetadata, NotImplemented, 
	InvalidRequest, UnsupportedEncodingException, NotFound 
	{
		// remember who the client currently is
		Subject startingClientSubjectsss = ClientIdentityManager.getCurrentIdentity();
		X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
		String startingCertLoc = CertificateManager.getInstance().getCertificateLocation();
		String startingClientSubjectName = null;
		if (certificate != null) {
			startingClientSubjectName = CertificateManager.getInstance().getSubjectDN(certificate);
		} else {
			startingClientSubjectName = Constants.SUBJECT_PUBLIC;
		}
		// following the testing rule of doing all creates as the testOwner subject
		setupClientSubject("testOwner");

		// create the identifier for the test object
		Identifier pid = new Identifier();
		String prefix = d1Node.getClass().getSimpleName() +  "TierTests.";
		if (idSuffix != null) {
			pid.setValue(prefix + ExampleUtilities.generateIdentifier() +
					"." + idSuffix);
		} else {
			pid.setValue(prefix + ExampleUtilities.generateIdentifier());
		}

		// prepare the data object for the create:
		// generate some data bytes as an input stream		
		byte[] contentBytes = "Plain text source for test object".getBytes("UTF-8");
		ByteArrayInputStream textPlainSource = new ByteArrayInputStream(contentBytes);
		
		D1Object d1o = null;
		SystemMetadata sysMeta = null;
		try {
			// make the submitter the same as the cert DN 
			certificate = CertificateManager.getInstance().loadCertificate();
			String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
		
			d1o = new D1Object(pid, contentBytes, format_text_plain, ownerX500, "authNode");
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
		
		
		// build the appropriate access policy based on given method parameters
		if (permissionLevel != null) {
			// will set up an accessPolicy
			AccessPolicy ap = null;
			Permission[] permissions = new Permission[] {permissionLevel};
			if (subject != null) {
				// map permissions to subject
				ap = AccessUtil.createSingleRuleAccessPolicy(
						new String[] {subject.getValue()},permissions);
			} else {
				// map permissions to startingClientSubject
				if (startingClientSubjectName.equals(Constants.SUBJECT_PUBLIC)) {
					// can only assign read permission to public
					ap = AccessUtil.createSingleRuleAccessPolicy(
							new String[] {Constants.SUBJECT_PUBLIC},
							new Permission[] {Permission.READ});
				} else {
					 ap = AccessUtil.createSingleRuleAccessPolicy(
							new String[] {startingClientSubjectName},permissions);
				} 
			}
			sysMeta.setAccessPolicy(ap);
		}
		
		// create the test object on the given mNode		
		log.info("creating a test object.  pid = " + pid.getValue());
		
		Identifier retPid = null;
		if (d1Node instanceof MNode) {
			retPid = ((MNode)d1Node).create(null, pid, textPlainSource, sysMeta);
		} else {
			retPid = ((CNode)d1Node).create(null, pid, textPlainSource, sysMeta);
		}
		checkEquals(d1Node.getNodeBaseServiceUrl(),
				"createTestObject(): returned pid from the create() should match what was given",
				pid.getValue(), retPid.getValue());

		// reset the client to the starting subject/certificate
		// (this should work for public user, too, since we create a public user by 
		// using a bogus certificate location)
		CertificateManager.getInstance().setCertificateLocation(startingCertLoc);
		if (log.isDebugEnabled()) {
			certificate = CertificateManager.getInstance().loadCertificate();
			String currentX500 = CertificateManager.getInstance().getSubjectDN(certificate);
			log.debug("current client certificate " + currentX500);
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
    public void checkTrue(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for host: " + host + ":: " + message, b, is(true));
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
            		assertThat("for host: " + host + ":: " + message, b, is(false));
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
            		fail("for host: " + host + ":: " + message);
            	} else {
            		fail(message);
            	}
                return null;
            }
        });
    }
	
}
