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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.MNode;
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
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractAuthorizationITDataone extends ContextAwareTestCaseDataone {

//	// this here defines the default
//	// can be overwritten by property passed into base class
	// default is now in ContextAwareTestCaseDataone
//	protected String testObjectSeriesSuffix = "." + "12";
	
	
	private static String currentUrl;

	/**
	 * @return an Iterator<Node> object for the nodes under test
	 */
	protected abstract Iterator<Node> getNodeIterator();

	protected abstract D1Node instantiateD1Node(String baseUrl);
	
	/**
	 * used to determine which tests to run, based on Permission
	 * (used to generalize for READ-only methods, like query)
	 * @param p
	 * @return
	 */
	protected abstract boolean runTest(Permission p);
	
	
	protected String checkExpectedIsAuthorizedOutcome(D1Node d1Node, Identifier pid, 
	String subjectLabel, Permission permission, String expectedOutcome) 
	{
//		log.debug("in: " + new Date().getTime());
		String testResult = null;
		if (runTest(permission)) {
			testResult = String.format("assert client subject: %-30s is allowed to %-6s", 
				subjectLabel, 
				permission.toString().toLowerCase().replace("_permission","")
				);

			String outcome = runAuthTest(d1Node, pid, permission);

			if (expectedOutcome.contains(outcome)) {
				testResult += String.format("  PASSED ('%s')", expectedOutcome);
			} else {
				testResult += String.format("  FAILED!!  Expected: '%s'  got: '%s'", expectedOutcome, outcome);
			}
		}
//		log.debug("out: " + new Date().getTime());
		return testResult;
	}

	
	protected abstract String runAuthTest(D1Node d1Node, Identifier pid, Permission permission);


	
	/**
	 * a label or description of the test
	 * for example "/isAuthorized/{pid}?action={permission}"
	 * @return
	 */
	protected abstract String getTestServiceEndpoint();
	
	
	private String tablifyResults(Identifier testPid, List<String> results)
	{
		StringBuffer table = new StringBuffer("Failed 1 or more tests:\n    " +
				getTestServiceEndpoint() + " vs. the test item:\n    ");
		table.append(testPid.getValue() + docDescription(testPid) + "\n\n    ");
		for (String result: results) {
			if (result != null) {
				table.append(result);	
				table.append("\n    ");
			}
		}
		return table.toString();		 
	}


	private String docDescription(Identifier pid) {
		if (pid.getValue().contains("RightsHolder_")) {
			String rh = pid.getValue().replaceAll(".+_", "").replaceAll("\\.[^.]*$","");
			return String.format(" (null AccessPolicy & the RightsHolder = '%s')",rh);
		}
		String[] arSubjectPerm = pid.getValue().replaceAll(".*\\:", "").replaceAll("\\.[^.]*$","").split("_");
		return String.format(" (item has accessRule where subject '%s' is allowed up to and including '%s')",arSubjectPerm[0],arSubjectPerm[1]);
	}



	
	protected Subject buildSubject(String value) {
		Subject s = new Subject();
		s.setValue(value);
		return s;
		 
	}
	 
	 protected AccessRule buildAccessRule(String subjectString, Permission permission)
	 {
		 if (subjectString == null || permission == null) {
			 return null;
		 }
		 
		 AccessRule ar = new AccessRule();
		 ar.addSubject(buildSubject(subjectString));
		 ar.addPermission(permission);
		 return ar;
	 }
	
	 
	 protected Identifier buildIdentifier(String value) {
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
//				 d1Node.ping();
//				 d1Node.listObjects(null);
//				 d1Node.getLogRecords(null);
				 setupClientSubject("testPerson_SelfSigned");
				 
				 String objectIdentifier = "TierTesting:" + 
						 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
						 	":Public_READ" + testObjectSeriesSuffix;
				 Identifier pid = procurePublicReadableTestObject(d1Node,buildIdentifier(objectIdentifier));
				 
				 d1Node.isAuthorized(null, pid, Permission.READ);
				 handleFail(d1Node.getLatestRequestUrl(), "ssl connection should not succeed with a self-signed certificate (untrusted CA): testPerson READ vs. " + pid.getValue());
			 } catch (BaseException be) {
				 if (!(be instanceof ServiceFailure)) {
				 handleFail(d1Node.getLatestRequestUrl(),"a self-signed certificate should not be trusted and should throw a ServiceFailure. Got: " 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
				 d1Node.ping();
			 } catch (BaseException be) {
				 if (!(be instanceof ServiceFailure)) {
				 handleFail(d1Node.getLatestRequestUrl(),"an Expired Certificate should throw a ServiceFailure. Got: " 
						 + be.getClass().getSimpleName() + ": " + 
						 be.getDetail_code() + ":: " + be.getDescription());
				 }
			 }
			 catch(Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
		 setupClientSubject_NoCert();
	 }

	 
	 @Test
	 public void testConnectionLayer_dataoneCAtrusted() 
	 {
		 setupClientSubject("testSubmitter");
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
				 d1Node.ping();
			 } catch (BaseException e) {
				 handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {

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
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(d1Node.getLatestRequestUrl(),tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
						handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true OR NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true OR NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true OR NotAuthorized"));
				 } else {
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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

				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try { 
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
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }

			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));

				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
			 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);			 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
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
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized OR true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 
	 @Test
	 public void testIsAuthorized_AccessPolicy_is_legacyAccount_can_Write() {
		 String procuringSubjectString = "testRightsHolder";
		 String objectSubjectString = "CN=someLegacyAcct,DC=somewhere,DC=org";
		 Permission objectPermission = Permission.WRITE;
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
				 // get or create the test object
				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":legacyAcct_WRITE" + testObjectSeriesSuffix;		
				 
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
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
				 } else {
					 // MNodes need subjectInfo to get verified status
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
					 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "NotAuthorized")); 
				 }
				 
				 clientSubject = "testEQPerson1";
				 setupClientSubject(clientSubject);				 
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.WRITE,             "true"));
				 results.add(checkExpectedIsAuthorizedOutcome(d1Node, testObject, clientSubject, Permission.READ,              "true"));
				 
				 clientSubject = "testEQPerson3";
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
//				 clientSubject = "testPerson_Missing_EQ_IDs";
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
					 if (result != null && result.contains("FAILED!!")) {
						 handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
						 break;
					 }
				 }
				 
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }

	 private String buildTestFQDN(String cn) {
		 return String.format("CN=%s,DC=dataone,DC=org",cn);
	 }
	 
	 @Test
	 public void testIsAuthorized_ComplicatedAccessPolicy() {
		 
		 String procuringSubjectString = "testRightsHolder";
		 
		 // set up the complicated access policy
		 // 1. aa,bb, => READ
		 // 2. testPerson => READ
		 // 3. ee,ff,testPerson => READ, WRITE
		 AccessPolicy complicatedPolicy = new AccessPolicy();
		 complicatedPolicy.addAllow(AccessUtil.createAccessRule(
				 new String[] {buildTestFQDN("testGroupie")},
				 new Permission[] {Permission.READ, Permission.WRITE}));
		 complicatedPolicy.addAllow(AccessUtil.createAccessRule(
				 new String[] {buildTestFQDN("testPerson")},
				 new Permission[] {Permission.READ}));
		 complicatedPolicy.addAllow(AccessUtil.createAccessRule(
				 new String[] {buildTestFQDN("cc"),
						       buildTestFQDN("testPerson")},
				 new Permission[] {Permission.WRITE}));

		 
		 
		 
		 Iterator<Node> it = getNodeIterator();
		 while (it.hasNext()) {
			 currentUrl = it.next().getBaseURL();
			 D1Node d1Node = instantiateD1Node(currentUrl);
			 
			 try {
				 String objectIdentifier = "TierTesting:" + 
				 	createNodeAbbreviation(d1Node.getNodeBaseServiceUrl()) +
				 	":ComplicatedPolicy" + testObjectSeriesSuffix;				

				 setupClientSubject(procuringSubjectString);
				 Identifier testObject = procureSpecialTestObject(d1Node,
						 complicatedPolicy,
						 buildIdentifier(objectIdentifier), false);
				 
				 String clientSubject = "testPerson";
				 setupClientSubject(clientSubject);
				 
				 ArrayList<String> results = new ArrayList<String>();
				 
				 // TODO:  set up the proper tests
				 
				 String outcome;
				 outcome = runAuthTest(d1Node, testObject, Permission.READ);
				 if (! outcome.equals("true"))
					 results.add(String.format("FAILED!! %s should be allowed %s access" +
					 		" to %s. isAuthorized method did not process the 2nd or " +
					 		"3rd AccessRule.  Got %s",
					 		clientSubject,
					 		Permission.READ.xmlValue(),
					 		testObject.getValue(),
					 		outcome));
				 
				 outcome = runAuthTest(d1Node, testObject, Permission.WRITE);
				 if (! outcome.equals("true"))
					 results.add(String.format("FAILED!! %s should be allowed %s access " +
					 		"to %s. isAuthorized method did not apply WRITE permission " +
					 		"to 2nd Subject in the AccessRule. Got %s",
					 		clientSubject,
					 		Permission.WRITE.xmlValue(),
					 		testObject.getValue(),
					 		outcome));
					 
				 outcome = runAuthTest(d1Node, testObject, Permission.CHANGE_PERMISSION);
				 if (! outcome.equals("NotAuthorized"))
					 results.add(String.format("FAILED!! %s should NOT be allowed %s access " +
					 		"to %s. Got %s",
					 		clientSubject,
					 		Permission.CHANGE_PERMISSION.xmlValue(),
					 		testObject.getValue(),
					 		outcome));
				 
				 
				 clientSubject = "testGroupie";
				 setupClientSubject(clientSubject);
				 
				 outcome = runAuthTest(d1Node, testObject, Permission.WRITE);
				 if (! outcome.equals("true"))
					 results.add(String.format("FAILED!! %s should be allowed %s access " +
					 		"to %s. isAuthorized method did not apply WRITE permission " +
					 		"to 2nd Permission in the AccessRule. Got %s",
					 		clientSubject,
					 		Permission.WRITE.xmlValue(),
					 		testObject.getValue(),
					 		outcome));
				 
				 outcome = runAuthTest(d1Node, testObject, Permission.CHANGE_PERMISSION);
				 if (! outcome.equals("NotAuthorized"))
					 results.add(String.format("FAILED!! %s should NOT be allowed %s access " +
					 		"to %s. Got %s",
					 		clientSubject,
					 		Permission.CHANGE_PERMISSION.xmlValue(),
					 		testObject.getValue(),
					 		outcome));
			 
				 for (String result : results) {
					 if (result != null && result.contains("FAILED!!")) {
						 StringBuffer res = new StringBuffer();
						 for (String r : results) {
							 res.append(r + "\n");
						 }
						 handleFail(currentUrl,res.toString());
						 break;
					 }
				 }
			 } catch (BaseException e) {
					handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
							e.getDetail_code() + ": " + e.getDescription());
					 	 

			 } catch (Exception e) {
				 e.printStackTrace();
				 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			 }
		 }
	 }
	 
	 
	 private Identifier procureSpecialTestObject(D1Node d1Node, AccessPolicy accessPolicy, Identifier pid, boolean exactPolicy) 
	 throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType, 
	 InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest, 
	 UnsupportedEncodingException, NotFound, TestIterationEndingException 
	 {
		 Identifier identifier = null;
		 try {
			 log.debug("procureTestObject: checking system metadata of requested object");
			 SystemMetadata smd = d1Node.getSystemMetadata(null, pid);	 
		 
			 if (exactPolicy) {
				 if (accessPolicy == null) {
					 // need the accessPolicy to be null, or contain no accessrules
					 if (smd.getAccessPolicy() == null || smd.getAccessPolicy().sizeAllowList() == 0) {
						 identifier = pid;
					 } else {
						 throw new TestIterationEndingException("returned object doesn't have the expected accessRules");
					 }
				 } else {
					 if (smd.getAccessPolicy() != null && smd.getAccessPolicy().sizeAllowList() == accessPolicy.sizeAllowList()) {
						 // keeping it simple by requiring exact match to accessPolicy
						 boolean mismatch = false;
						 try {
							 policyCompare:
								 for (int i = 0; i< accessPolicy.sizeAllowList(); i++) {
									 AccessRule ar = accessPolicy.getAllow(i);

									 for (int j = 0; j < ar.sizeSubjectList(); j++) {
										 if (!ar.getSubject(j).equals(smd.getAccessPolicy().getAllow(i).getSubject(j))) {
											 mismatch = true;
											 break policyCompare;
										 }
									 }
									 for (int k = 0; k < ar.sizePermissionList(); k++) {
										 if (!ar.getPermission(k).equals(smd.getAccessPolicy().getAllow(i).getPermission(k))) {
											 mismatch = true;
											 break policyCompare;
										 }
									 }
								 }
						 } catch (Exception e) {
							 // assume it's because there's a null pointer or index out of bounds
							 // due to differences in list sizes
							 throw new TestIterationEndingException("the AccessPolicy of the returned object " +
									 "doesn't match the one required. Got exception: " + e.getClass() + " " + e.getMessage());
						 }
						 if (mismatch) {
							 throw new TestIterationEndingException("the AccessPolicy of the returned object doesn't match the one required");
						 }	
						 else {
							 identifier = pid;
						 }
					 }
					 else {	
						 throw new TestIterationEndingException("the AccessPolicy of the returned object " +
						 "does not match requirements. The number of allowRules differs.");
					 }
				 }
			 } else {
				 identifier = pid;
			 }
		 } 
		 catch (NotFound e) {
			 if (d1Node instanceof MNode) {
				 Node node = ((MNode) d1Node).getCapabilities();
				 if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
					 log.debug("procureTestObject: calling createTestObject");
					 identifier = createTestObject(d1Node, pid, accessPolicy, "testSubmitter","CN=testRightsHolder,DC=dataone,DC=org");
				 }
			 } else {
				 identifier = createTestObject(d1Node, pid, accessPolicy, cnSubmitter, "CN=testRightsHolder,DC=dataone,DC=org");
				// throw e;
			 }
		 }
		 return identifier;
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
				 identifier = createTestObject(d1Node,pid,accessRule,cnSubmitter,rightsHolderSubjectString);
				// throw e;
			 }
		 }
		 log.info(" ====>>>>> pid of procured test Object: " + identifier.getValue());
		 return identifier;

	 }


	
}
