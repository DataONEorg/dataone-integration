package org.dataone.integration.it;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.MNode;
import org.dataone.integration.it.ContextAwareTestCaseDataone.TestIterationEndingException;
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
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractAuthorizationITDataone extends ContextAwareTestCaseDataone {

	protected static String testObjectSeriesSuffix = ".4";
	private static String currentUrl;

	/**
	 * @return an Iterator<Node> object for the nodes under test
	 */
	protected abstract Iterator<Node> getNodeIterator();

	protected abstract D1Node instantiateD1Node(String baseUrl);


	
	private void checkExpectedIsAuthorizedOutcome(D1Node d1Node, Identifier pid, 
			String subjectLabel, Permission permission, String expectedOutcome) 
	{
		String testDescription = "isAuthorized test [" + subjectLabel + " - " +
		permission.xmlValue() + " vs. " + pid.getValue() + "]";
		try {
			boolean trueOutcome = d1Node.isAuthorized(null, pid, permission);
			if (!trueOutcome || !expectedOutcome.equals("true")) {
				handleFail(currentUrl,testDescription + 
						"\nExpected: '" + expectedOutcome + "'\n   got: '" + trueOutcome + "'");

			}
		} catch (BaseException e) {
			checkEquals(currentUrl,"isAuthorized test [" + subjectLabel + " - " +
					permission.xmlValue() + " vs. " + pid.getValue() + "]", 
					e.getClass().getSimpleName(),  expectedOutcome);
		}	


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


	 @Test
	 public void testConnectionLayer_Untrusted_SelfSignedCertificate() 
	 {
		 setupClientSubject("testPerson_SelfSigned");
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl); 
				 d1Node.ping();
//				 d1Node.listObjects(null);
				 handleFail(currentUrl, "ping should not succeed with a self-signed certificate (untrusted CA).");
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
	 public void testIsAuthorized_vs_NullPolicy_personOwner() {

		 // TODO: check whether an object is created under the correct 
		 // rightsHolder
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
		 String objectIdentifier = "TierTesting:testObject:RightsHolder_Person" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureSpecialTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier),
						 "CN=testPerson,DC=dataone,DC=org");
		 

// testPerson is owner in this case, so don't need this block				 
//				 String clientSubject = "testRightsHolder");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 
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
	 public void testIsAuthorized_vs_NullPolicy_groupOwner() {

		 // TODO: check whether an object is created under the correct 
		 // rightsHolder
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
		 String objectIdentifier = "TierTesting:testObject:RightsHolder_Group" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureSpecialTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier),
  				 		 "testGroup");
//			 "CN=testGroup,DC=dataone,DC=org");


				// run tests
// testPerson is owner in this case, so don't need testRightsHolder tests				 
//				 String clientSubject = "testRightsHolder");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 } else {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 
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
	 public void testIsAuthorized_vs_PublicRead() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_PUBLIC;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testObject:Public_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

	
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");


//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 
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
	 public void testIsAuthorized_vs_AuthenticatedRead() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_AUTHENTICATED_USER;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testObject:Authenticated_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 ;
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));
				 
	
				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);					 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");


//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
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
	 public void testIsAuthorized_vs_VerifiedRead() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = Constants.SUBJECT_VERIFIED_USER;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testObject:Verified_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
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
	 public void testIsAuthorized_vs_TestPersonREAD() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testObject:testPerson_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object

				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 Subject s = setupClientSubject(clientSubject);  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 

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
	 public void testIsAuthorized_vs_TestPersonWRITE() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.WRITE;
		 String objectIdentifier = "TierTesting:testObject:testPerson_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject); // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

			 
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
	 public void testIsAuthorized_vs_TestPersonCHANGE() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.CHANGE_PERMISSION;
		 String objectIdentifier = "TierTesting:testObject:testPerson_CHANGE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
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
	 public void testIsAuthorized_vs_TestGroupREAD() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testGroup";
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testObject:testGroup_READ" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 ;
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
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
	 public void testIsAuthorized_vs_TestGroupWRITE() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testGroup";
		 Permission objectPermission = Permission.WRITE;
		 String objectIdentifier = "TierTesting:testObject:testGroup_WRITE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 ;
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject); 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);	
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 
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
	 public void testIsAuthorized_vs_TestGroupCHANGE() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "testGroup";
		 Permission objectPermission = Permission.CHANGE_PERMISSION;
		 String objectIdentifier = "TierTesting:testObject:testGroup_CHANGE" + testObjectSeriesSuffix;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 try {
				 currentUrl = it.next().getBaseURL();
				 D1Node d1Node = instantiateD1Node(currentUrl);

				 // get or create the test object
				 ;
				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureTestObject(d1Node,
						 buildAccessRule(objectSubjectString,objectPermission),
						 buildIdentifier(objectIdentifier));

				 // run tests
				 String clientSubject = "testRightsHolder";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized");
				 
				 
//				 clientSubject = "testPerson_Expired";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail		 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
//
//				 clientSubject = "testPerson_SelfSigned";
//				 setupClientSubject(clientSubject);
//				 // bad credentials should always fail			 
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");


				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "InvalidToken");
				 
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
	
}
