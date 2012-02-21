package org.dataone.integration.it;

import java.util.Iterator;

import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractAuthorizationITDataone extends ContextAwareTestCaseDataone {

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
				 		"  Expected: '" + expectedOutcome + "' got: '" + trueOutcome + "'");
				 
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

//	 @Test
	 public void testConnectionLayer_clientCertificatesTrusted() 
	 {
		 
	 }

	 
//	 @Test
	 public void testConnectionLayer_InvalidTokenThrown() 
	 {
		 
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
		 String objectIdentifier = "TierTesting:testObject_thisShouldBeNew:RightsHolder_Person";
		 
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
		 

// testPerson is owner in this case, so don't need this block				 
//				 String clientSubject = "testRightsHolder");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 
			 } catch (BaseException e) {
						handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
								e.getDetail_code() + ": " + e.getDescription());
						 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
			 
		 }
	 }
	 
	 @Ignore("test not ready - need to refactor how object is created")
	 @Test
	 public void testIsAuthorized_vs_NullPolicy_groupOwner() {

		 // TODO: check whether an object is created under the correct 
		 // rightsHolder
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
		 String objectIdentifier = "TierTesting:testObject:RightsHolder_Group";
		 
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
// testPerson is owner in this case, so don't need testRightsHolder tests				 
//				 String clientSubject = "testRightsHolder");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 } else {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 
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
//		 String objectIdentifier = "TierTesting:testObject:Public_READ";
		 String objectIdentifier = "dave.3";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");


				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 
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
		 String objectIdentifier = "TierTesting:testObject:Authenticated_READ";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);					 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");


				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
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
		 String objectIdentifier = "TierTesting:testObject:Verified_READ";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
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
		 String objectIdentifier = "TierTesting:testObject:testPerson_READ";
		 
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
				 setupClientSubject(clientSubject);  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 

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
		 String objectIdentifier = "TierTesting:testObject:testPerson_READ";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

			 
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
		 String objectIdentifier = "TierTesting:testObject:testPerson_CHANGE";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
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
		 String objectIdentifier = "TierTesting:testObject:testGroup_READ";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized"); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
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
		 String objectIdentifier = "TierTesting:testObject:testGroup_WRITE";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);	
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized"); 
				 }
				 
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 
				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 
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
		 String objectIdentifier = "TierTesting:testObject:testPerson_CHANGE";
		 
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
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testPerson";
				 setupClientSubject(clientSubject);		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 
				 clientSubject = "testPerson_NoSubjectInfo";
				 setupClientSubject(clientSubject);
				 // CNodes can lookup subject info so results same as above
				 if (d1Node instanceof CNode) {
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");
				 } else {
					 // MNodes need subjectInfo to get verified status
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
					 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized"); 
				 }
				 
				 clientSubject = "testMappedPerson";
				 setupClientSubject(clientSubject);				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "true");

				 clientSubject = "testSubmitter";
				 setupClientSubject(clientSubject);
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 clientSubject = "NoCert";
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "NotAuthorized");
				 
				 
				 clientSubject = "testPerson_Expired";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");

				 clientSubject = "testPerson_SelfSigned";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");


				 clientSubject = "testPerson_InvalidVsSchema";
				 setupClientSubject(clientSubject);
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingMappedID";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
				 clientSubject = "testPerson_MissingSelf";
				 setupClientSubject(clientSubject);	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ, "InvalidToken");
				 
			 } catch (BaseException e) {
					handleFail(currentUrl,e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
 

	
}
