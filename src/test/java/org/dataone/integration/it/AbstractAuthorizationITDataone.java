package org.dataone.integration.it;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.MNode;
import org.dataone.configuration.Settings;
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
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractAuthorizationITDataone extends ContextAwareTestCaseDataone {

	// this here defines the default
	// can be overwritten by property passed into base class
	protected String testObjectSeriesSuffix = "." + "11";
	
	
	private static String currentUrl;

	/**
	 * @return an Iterator<Node> object for the nodes under test
	 */
	protected abstract Iterator<Node> getNodeIterator();

	protected abstract D1Node instantiateD1Node(String baseUrl);


	@Before
	public void setUpTestObjectSeries() throws Exception {
		if (testObjectSeries != null) {
			testObjectSeriesSuffix = "." + testObjectSeries;
		}
	}
	
	private String checkExpectedIsAuthorizedOutcome(D1Node d1Node, Identifier pid, 
			String subjectLabel, Permission permission, String expectedOutcome) 
	{
//		log.debug("in: " + new Date().getTime());
		String testResult = String.format("isAuth('%-30s,'%-45s,'%-6s): ", 
				subjectLabel + "'", 
				pid.getValue() + "'", 
				permission.toString().toLowerCase().replace("_permission","") + "'"
				);
		String outcome = null;
		try {
			boolean booleanOutcome = d1Node.isAuthorized(null, pid, permission);
			outcome = booleanOutcome ? "true" : "false";
		}
		catch (BaseException e) {
			outcome = e.getClass().getSimpleName();
		}	
		
		if (expectedOutcome.contains(outcome)) {
			testResult += String.format("  PASSED ('%s')", expectedOutcome);
		} else {
			testResult += String.format("  FAILED!!  Expected: '%s'  got: '%s'", expectedOutcome, outcome);
		}
//		log.debug("out: " + new Date().getTime());
		return testResult;
	}


	 private Subject buildSubject(String value) {
		 Subject s = new Subject();
		 s.setValue(value);
		 return s;
		 
	 }
	 
	 private AccessRule buildAccessRule(String subjectString, Permission permission)
	 {
		 if (subjectString == null || permission == null) {
			 return null;
		 }
		 
		 AccessRule ar = new AccessRule();
		 ar.addSubject(buildSubject(subjectString));
		 ar.addPermission(permission);
		 return ar;
	 }
	
	 
	 private Identifier buildIdentifier(String value) {
		 Identifier id = new Identifier();
		 id.setValue(value);
		 return id;
	 }


	 /**
	  * Client behavior is funky for self-signed certs - for this client it gets
	  * through as public, but other clients are rejected.
	  * This test will test the downgraded permissions to make sure it only has
	  * access to public data, not it's own.
	  */
	 @Ignore("testing with the other tests against the various test objects")
	 @Test
	 public void testConnectionLayer_SelfSignedCert() 
	 {

		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl); 
//				 d1Node.ping();
//				 d1Node.listObjects(null);
//				 d1Node.getLogRecords(null);
				 setupClientSubject("testPerson_SelfSigned");
				 
				 
				 Identifier pid = procurePublicReadableTestObject(d1Node);
				 d1Node.isAuthorized(null, pid, Permission.READ);
				 handleFail(currentUrl, "ssl connection should not succeed with a self-signed certificate (untrusted CA): testPerson READ vs. " + pid.getValue());
			 } catch (BaseException be) {
				 if (!(be instanceof ServiceFailure)) {
				 handleFail(currentUrl,"a self-signed certificate should not be trusted and should throw a ServiceFailure. Got: " 
						 + be.getClass().getSimpleName() + ": " + 
						 be.getDetail_code() + ":: " + be.getDescription());
				 }
			 }
			 catch(Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 
	 @Test
	 public void testConnectionLayer_ExpiredCertificate() 
	 {
		 setupClientSubject("testPerson_Expired");
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl); 
				 d1Node.ping();
			 } catch (BaseException be) {
				 if (!(be instanceof ServiceFailure)) {
				 handleFail(currentUrl,"an Expired Certificate should throw a ServiceFailure. Got: " 
						 + be.getClass().getSimpleName() + ": " + 
						 be.getDetail_code() + ":: " + be.getDescription());
				 }
			 }
			 catch(Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 @Test
	 public void testConnectionLayer_dataoneCAtrusted() 
	 {
		 setupClientSubject("testSubmitter");
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl); 
				 d1Node.ping();
			 } catch (BaseException e) {
				 handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
						 e.getDetail_code() + ":: " + e.getDescription());
			 }
			 catch(Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 /*
	  * The authorization tests...................................
	  */

	 @Test
	 public void testIsAuthorized_NullPolicy_testPerson_is_RightsHolder() {

		 // TODO: check whether an object is created under the correct 
		 // rightsHolder
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
//		 String objectIdentifier = "TierTesting:testObject:RightsHolder_testPerson" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":RightsHolder_testPerson" + testObjectSeriesSuffix;
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureSpecialTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier),
						 "CN=testPerson,DC=dataone,DC=org");
		 

// testPerson is owner in this case, so don't need this block				 
//				 String clientSubject = "testRightsHolder";  // should always have access
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken OR ServiceFailure"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken OR ServiceFailure"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken OR ServiceFailure"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(currentUrl,tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
						handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
								e.getDetail_code() + ": " + e.getDescription());
						 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
			 
		 }
	 }
	 

	 @Test
	 public void testIsAuthorized_NullPolicy_testGroup_is_RightsHolder() {
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
//		 String objectIdentifier = "TierTesting:testObject:RightsHolder_testGroup" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":RightsHolder_testGroup" + testObjectSeriesSuffix;
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureSpecialTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier),
//  				 		 "testGroup");
				 "CN=testGroup,DC=dataone,DC=org");


				// run tests
// testPerson is owner in this case, so don't need testRightsHolder tests				 
//				 String clientSubject = "testRightsHolder");  // should always have access
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 } else {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 // BRL - group membership is transitive, so this is successful
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 		 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }


	 @Test
	 public void testIsAuthorized_AccessPolicy_is_Public_can_Read() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_PUBLIC;
		 Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Public_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":Public_READ" + testObjectSeriesSuffix;
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
	
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));


//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_AuthenticatedUser_can_Read() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_AUTHENTICATED_USER;
		 Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Authenticated_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":Authenticated_READ" + testObjectSeriesSuffix;				

				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));
				
				 ArrayList<String> results = new ArrayList<String>();
	
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);  // should always have access
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);					 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				//  TODO:  enable when ready to test				 				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());

				 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_VerifiedUser_can_Read() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_VERIFIED_USER;
		 Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Verified_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":Verified_READ" + testObjectSeriesSuffix;		
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testPerson_can_Read() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
		 Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);
				 
				 // get or create the test object

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testPerson_READ" + testObjectSeriesSuffix;		
				 
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 
				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);  // should always have access
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }

			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }

	 }
	 
	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testPerson_can_Write() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
		 Permission objectPermission = Permission.WRITE;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_WRITE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testPerson_WRITE" + testObjectSeriesSuffix;		
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject); // should always have access
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
			 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testPerson_can_ChangePerm() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
		 Permission objectPermission = Permission.CHANGE_PERMISSION;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_CHANGE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testPerson_CHANGE" + testObjectSeriesSuffix;		
				 
				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 
				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testGroup_can_Read() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
		 Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 
				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testGroup_READ" + testObjectSeriesSuffix;		
				 
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 

	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testGroup_can_Write() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
		 Permission objectPermission = Permission.WRITE;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_WRITE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testGroup_WRITE" + testObjectSeriesSuffix;						 

				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject); 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_testGroup_can_ChangePerm() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
		 Permission objectPermission = Permission.CHANGE_PERMISSION;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_CHANGE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":testGroup_CHANGE" + testObjectSeriesSuffix;		
				 
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 ArrayList<String> results = new ArrayList<String>();
				 
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

				//  TODO:  enable when ready to test				 
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingMappedID";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
//				 clientSubject = "testPerson_MissingSelf";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//				 
				 for (String result : results) {
					 if (result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 private  Identifier procureSpecialTestObject(D1Node d1Node, AccessRule accessRule, Identifier pid, String rightsHolderSubjectString) 
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
					 } else {
						 throw new TestIterationEndingException("the AccessRule of the returned object has either multiple subjects or multiple permissions");
					 }
				 } else {
					 throw new TestIterationEndingException("the AccessPolicy of the returned object is either null or has multiple AccessRules");
				 }
			 }
		 } 
		 catch (NotFound e) {
			 if (d1Node instanceof MNode) {
				 Node node = ((MNode) d1Node).getCapabilities();
				 if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
					 log.debug("procureTestObject: calling createTestObject");
					 identifier = createTestObject(d1Node, pid, accessRule, "testSubmitter",rightsHolderSubjectString);
				 }
			 } else {
				 throw e;
			 }
		 }
		 log.info(" ====>>>>> pid of procured test Object: " + identifier.getValue());
		 return identifier;

	 }

	 private String tablifyResults(Identifier testPid, List<String> results)
	 {
		 StringBuffer table = new StringBuffer("Failed 1 or more tests:\nisAuthorized() vs. ");
		 table.append(testPid.getValue() + docDescription(testPid) + "\n    ");
		 for (String result: results) {
//			 if (result.contains("FAILED!!")) {
				 table.append(result);	
				 table.append("\n    ");
	//		 }
		 }
		 return table.toString();		 
	 }


	 private String docDescription(Identifier pid) {
		 if (pid.getValue().contains("RightsHolder_")) {
			 String rh = pid.getValue().replaceAll(".+_", "").replaceAll("\\.[^.]*$","");
			 return String.format(" (null AccessPolicy & the RightsHolder = '%s')",rh);
		 }
		 String[] arSubjectPerm = pid.getValue().replaceAll(".*\\:", "").replaceAll("\\.[^.]*$","").split("_");
		 return String.format(" (w/ accessRule where '%s' allowed '%s')",arSubjectPerm[0],arSubjectPerm[1]);
	 }
	
}
