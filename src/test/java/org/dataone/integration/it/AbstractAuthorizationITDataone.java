package org.dataone.integration.it;

import java.util.Iterator;

import org.dataone.client.D1Node;
import org.dataone.service.exceptions.BaseException;
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
	  * implementing class needs to set this to either NodeType.MN or .CN
	  * for running the tests
	  * @return
	  */
//	 protected abstract NodeType getNodeType();
	 
	 /**
	  * @return an Iterator<Node> object for the nodes under test
	  */
	 protected abstract Iterator<Node> getNodeIterator();
	 
	 protected abstract D1Node instantiateD1Node(String baseUrl);

	 
	 private void checkExpectedIsAuthorizedOutcome(D1Node d1Node, Identifier pid, 
			 Permission permission, String expectedOutcome) 
//	 throws BaseException
	 {
		 try {
			 boolean outcome = d1Node.isAuthorized(null, pid, permission);
			 if (outcome && !expectedOutcome.equals("true")) {
				 handleFail(currentUrl,"isAuthorized should not have returned true." +
				 		"  Expected: " + expectedOutcome);
				 
			 }
		} catch (BaseException e) {
			checkEquals(currentUrl,"isAuthorized test: ", 
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

	 
	 
	 
	 /*
	  * The authorization tests...................................
	  */

	 @Ignore("test not ready - need to refactor how object is created")
	 @Test
	 public void testIsAuthorized_vs_NullPolicy_personOwner() {

		 // TODO: check whether an object is created under the correct 
		 // rightsHolder
		 
		 String procuringSubjectString = "testPerson";
		 String objectSubjectString = null;
		 Permission objectPermission = null;
		 String objectIdentifier = "TierTesting:RightsHolder_Person";
		 
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
//				 setupClientSubject("testOwner");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");
				 
				 
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
		 String objectIdentifier = "TierTesting:RightsHolder_Group";
		 
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
// testPerson is owner in this case, so don't need testOwner tests				 
//				 setupClientSubject("testOwner");  // should always have access
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
//				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");
				 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }


	 @Test
	 public void testIsAuthorized_vs_PublicRead() {
		 
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = Constants.SUBJECT_PUBLIC;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:Public_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");


				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");
	
				 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 @Test
	 public void testIsAuthorized_vs_AuthenticatedRead() {
		 
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = Constants.SUBJECT_AUTHENTICATED_USER;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:Authenticated_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");


				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");
				 
				 
			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Test
	 public void testIsAuthorized_vs_VerifiedRead() {
		 
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = Constants.SUBJECT_VERIFIED_USER;
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:Verified_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");


			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 @Test
	 public void testIsAuthorized_vs_TestPersonREAD() {
		 
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testPerson_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");


			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }

	 }
	 
	 
	 @Test
	 public void testIsAuthorized_vs_TestPersonWRITE() {
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.WRITE;
		 String objectIdentifier = "TierTesting:testPerson_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");


			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Test
	 public void testIsAuthorized_vs_TestPersonCHANGE() {
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = "testPerson";
		 Permission objectPermission = Permission.CHANGE_PERMISSION;
		 String objectIdentifier = "TierTesting:testPerson_CHANGE";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");


			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 @Test
	 public void testIsAuthorized_vs_TestGroupREAD() {
		 String procuringSubjectString = "testOwner";
		 String objectSubjectString = "testGroup";
		 Permission objectPermission = Permission.READ;
		 String objectIdentifier = "TierTesting:testPerson_READ";
		 
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
				 setupClientSubject("testOwner");  // should always have access
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "true");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testMappedPerson");				 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testPerson_Validated");			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");
				 
				 setupClientSubject("testPersonGroupie"); 
				 // to test access as a group member (of testPerson)
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "true");

				 setupClientSubject("testSubmitter");
				 // the designated no-rights subject
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 setupClientSubject_NoCert();	 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "NotAuthorized");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "NotAuthorized");
				 
				 
				 setupClientSubject("testPerson_Expired");
				 // bad credentials should always fail		 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");

				 setupClientSubject("testPerson_SelfSigned");	
				 // bad credentials should always fail			 
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.CHANGE_PERMISSION, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.WRITE, "InvalidToken");
				 checkExpectedIsAuthorizedOutcome(d1Node, testObject, Permission.READ, "InvalidToken");


			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 @Ignore("test not written")
	 @Test
	 public void testIsAuthorized_vs_TestGroupWRITE() {

	 }
	 
	 @Ignore("test not written")	 
	 @Test
	 public void testIsAuthorized_vs_TestGroupCHANGE() {

	 }

	 
//	 /**
//	  * Finds or creates an object with PUBLIC READ access, then ensures
//	  * that an anonymous client can READ it.
//	  */
//	 @Test
//	 public void testIsAuthorized_PublicSymbolicPrincipal() 
//	 {	
//		 AccessRule accessRule = AccessUtil.createAccessRule(
//				 new String[]{Constants.SUBJECT_PUBLIC}, new Permission[]{Permission.READ});
//		 runIsAuthorizedVsSubject( accessRule, "NoCert", "NoCert", null);
//	 }
//	 
//	 /**
//	  * Finds or creates an object where AUTHENTICATED_USER has READ access, then
//	  * ensures that the "testNoRights" client can READ it.  (By design, the testNoRights
//	  * subject is not on accessPolicies, and is not part of any group, or mapped
//	  * to any other identity.)
//	  */
//	 @Test
//	 public void testIsAuthorized_AuthenticatedUserSymbolicPrincipal()
//	 {	
//		 AccessRule accessRule = AccessUtil.createAccessRule(
//				 new String[]{Constants.SUBJECT_AUTHENTICATED_USER}, new Permission[]{Permission.READ});
//		 runIsAuthorizedVsSubject( accessRule, "testOwner", "testNoRights",null);
//	 }
//
//
//	 @Ignore("user verification not implemented yet")
//	 @Test
//	 public void testIsAuthorized_VerifiedUserSymbolicPrincipal()
//	 {	
//		 AccessRule accessRule = AccessUtil.createAccessRule(
//				 new String[]{Constants.SUBJECT_VERIFIED_USER}, new Permission[]{Permission.READ});
//		 runIsAuthorizedVsSubject(accessRule, "testOwner", "testReader", null);
//	 }
//
//	 /**
//	  * assuming that testMappedReader and testReaderGroup are not in the
//	  * accessPolicy of the object procured.
//	  */
//	 @Test
//	 public void testIsAuthorized_MappedIdentity()
//	 {	
//		 AccessRule accessRule = AccessUtil.createAccessRule(
//				 new String[]{"testReader"}, new Permission[]{Permission.READ});
//		 runIsAuthorizedVsSubject( accessRule, "testOwner", "testMappedReader", null);
//	 }
//
//	 /**
//	  * assumes that testReader and testMappedReader are not in the accessPolicy
//	  * of the object procured
//	  */
//	 @Test
//	 public void testIsAuthorized_ViaGroupMembership()
//	 {	
//		 AccessRule accessRule = AccessUtil.createAccessRule(
//				 new String[]{"testReaderGroup"}, new Permission[]{Permission.READ});
//		 Identifier pid = new Identifier();
//		 pid.setValue("testAuthByGroup");
//		 runIsAuthorizedVsSubject( accessRule, "testOwner", "testReader",pid);
//	 }
//
//
//	 private void runIsAuthorizedVsSubject(AccessRule accessRule,
//			 String string, String string2, Identifier pid) {
//		 // TODO Auto-generated method stub
//
//	 }
//
//
//		public void testIsAuthorized_Vs_NullAP_GroupOwner() { }
////	   		bad certs (null,expired,nonTrusted)
////	   		testOwnerGroup R,W,CP
////	   		testOwnerGroupie R,W,CP
////	   		testOutsider R,W,CP
////	   		testSubmitter R,W,CP
//	    public void testIsAuthorized_Vs_NullAP_PersonOwner() { }
////	   		bad certs (null,expired,nonTrusted)
////	   		testOwner R,W,CP
////	        testMappedOwner R,W,CP	    
////	   		testOwnerGroupie R,W,CP
////	   		testOutsider R,W,CP
////	   		testSubmitter R,W,CP
//	    public void testIsAuthorized_Vs_PublicRead() { }
////   		bad certs (null,expired,nonTrusted)
////   		testOwner R,W,CP
////      x    testMappedOwner R,W,CP ?	    
////   	x	testOwnerGroupie R,W,CP ?
////   		testOutsider R,W,CP
////   	x	testSubmitter R,W,CP	    
//	    public void testIsAuthorized_Vs_AuthenticatedRead() { }
////		bad certs (null,expired,nonTrusted)
////		testOwner R,W,CP
////  x    testMappedOwner R,W,CP ?	    
////	x	testOwnerGroupie R,W,CP ?
////		testOutsider R,W,CP
////	x	testSubmitter R,W,CP	
//	    
//	    public void testIsAuthorized_Vs_VerifiedRead() { }
//	    public void testIsAuthorized_Vs_Read() { }
//	    public void testIsAuthorized_Vs_Write() { }
//	    public void testIsAuthorized_Vs_Change() { }
//	    public void testIsAuthorized_Vs_GroupRead() { }
//	    public void testIsAuthorized_Vs_GroupWrite() { }
//	    public void testIsAuthorized_Vs_GroupChange() { }
	   

	    
	    
//	 
//	 
//	 /** 
//     * Implementation for isAuthorized Tests for various types of users
//     * @param objectAccessRule - the accessRule containing the subject and permission
//     *           required in the systemmMetadata.accessPolicy 
//     * @param procuringSubjectName - the client subject used to search for the
//     *           objects with (listObjects, getSystemMetadata)
//     * @param testingSubjectName - the client subject to run the test under
//	 * @param testObject - pass in a valid pid to bypass the procurement of the identifier
//	 *              and test isAuthorized against the provided pid.
//     */
//	protected void runIsAuthorizedVsSubject(String procuringSubjectName, String testingSubjectName, 
//			Permission testPermission, Identifier testObject, AccessRule objectAccessRule,
//			boolean expectPass)
//	{
//		String policySubjectName = objectAccessRule.sizeSubjectList() > 0 ?
//				objectAccessRule.getSubject(0).getValue() : null;
//		String policyPermission = objectAccessRule.sizePermissionList() > 0 ?
//				objectAccessRule.getPermission(0).xmlValue() : null;
//		
//		
//		Iterator<Node> it = getNodeIterator();
//
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//
//			D1Node d1Node = instantiateD1Node(currentUrl);
//			
//			currentUrl = d1Node.getNodeBaseServiceUrl();
//			printTestHeader("testIsAuthorized_" + policySubjectName + "() vs. node: " + currentUrl);
//			String testDescription = String.format("Authorization test: isAuthorized('%s','%s',%s) " +
//					"vs. AccessPolicy('%s','%s') : expecting %s",
//					testingSubjectName,testObject.getValue(),testPermission.xmlValue(),
//					policySubjectName, policyPermission, 
//					expectPass ? "Pass" : "NotAuthorized"
//					);
//			printTestHeader(testDescription);			
//			log.info(testDescription);
//			
//			try {				
//				// change to the subject used for procuring an object
//				if (procuringSubjectName.equals("NoCert") || procuringSubjectName == null)
//					setupClientSubject_NoCert();
//				else 
//					setupClientSubject(procuringSubjectName);
//
//				// get an appropriate test object
//				 testObject = procureTestObject(d1Node, objectAccessRule, testObject);
//					
//				// change to the subject used for testing, if necessary
//				if (!testingSubjectName.equals(procuringSubjectName)) {
//					if (testingSubjectName.equals("NoCert") || testingSubjectName == null)
//						setupClientSubject_NoCert();
//					else 
//						setupClientSubject(testingSubjectName);
//				}
//				boolean success = false;
//				try {
//					success = d1Node.isAuthorized(null, testObject, testPermission);
//				} catch (NotAuthorized na) {
//					success = false;
//				}
//				checkTrue(currentUrl,"failed assumption: " + testDescription,
//						success == expectPass);
//	
//			} catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			} catch (BaseException e) {
//				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + 
//						e.getDetail_code() + ": " + e.getDescription());
//			} catch (Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
//			}
//			
//		} // while it.hasNext()
//	}   

	
}
