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

package org.dataone.integration.it.testImplementations;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.TestIterationEndingException;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
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
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Test;

/**
 * This abstract class is the locus for the thorough set of authorization tests.
 * It has been generalized to work with any READ-only method that follows Dataone's
 * authorization rules.
 *
 * Originally written for isAuthorized, it also works for the search and query
 * API's
 */
public abstract class AbstractAuthorizationTestImplementations extends ContextAwareTestCaseDataone {


    private static String currentUrl;

    /**
     * @return an Iterator<Node> object for the nodes under test, taken from the
     * base class.
     */
    protected abstract Iterator<Node> getNodeIterator();


    protected abstract CommonCallAdapter instantiateD1Node(String subjectLabel, Node node);

    /**
     * Similar to {@link #instantiateD1Node(String, Node)}, but ensures that returned node
     * can create objects. This means in the case of CNs, we set it up with a CN certificate.
     */
    protected abstract CommonCallAdapter instantiateProcuringD1Node(String subjectLabel, Node node);
    
    /**
     * used to determine which tests to run, based on Permission
     * (used to generalize for READ-only methods, like query)
     * @param p
     * @return
     */
    protected abstract boolean runTest(Permission p);


    /**
     * The method that implements the authorization check.  This could be any
     * method in the DataONE API that differentiates responses based on (at least)
     * authorization criteria.
     * @param d1Node
     * @param pid
     * @param permission
     * @return a String containing either the word true, NotAuthorized, InvalidToken,
     *    or occasionally, one of the other DataONE exceptions.
     */
    protected abstract String runAuthTest(CommonCallAdapter d1Node, Identifier pid, Permission permission);


    protected String checkExpectedIsAuthorizedOutcome(CommonCallAdapter cca, Identifier pid,
    String subjectLabel, Permission permission, String expectedOutcome)
    {
//		log.debug("in: " + new Date().getTime());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String testResult = null;
        if (runTest(permission)) {
            testResult = String.format("assert client subject: %-30s is allowed to %-6s",
                subjectLabel,
                permission.toString().toLowerCase().replace("_permission","")
                );

            String outcome = runAuthTest(cca, pid, permission);

            if (expectedOutcome.contains(outcome)) {
                testResult += String.format("  PASSED ('%s')", expectedOutcome);
            } else {
                testResult += String.format("  FAILED!!  Expected: '%s'  got: '%s'", expectedOutcome, outcome);
            }
        }
//		log.debug("out: " + new Date().getTime());
        return testResult;
    }





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

     /*
      * The authorization tests...................................
      */

     @WebTestName("isAuthorized - test object has a null AccessPolicy and testPerson as rights holder")
     @WebTestDescription("this test uses an object created with testPerson as the rights holder "
             + "and a null AccessPolicy. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_NullPolicy_testPerson_is_RightsHolder() {

         // TODO: check whether an object is created under the correct
         // rightsHolder

         String procuringSubjectString = "testPerson";
         String objectSubjectString = null;
         Permission objectPermission = null;
//		 String objectIdentifier = "TierTesting:testObject:RightsHolder_testPerson" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {

                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);

                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":RightsHolder_testPerson" + getTestObjectSeriesSuffix();

                 // get or create the test object

                 Identifier testObject = procureSpecialTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier),
                         "CN=testPerson,DC=dataone,DC=org");


// testPerson is owner in this case, so don't need this block
//				 String clientSubject = "testRightsHolder";  // should always have access
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 ArrayList<String> results = new ArrayList<String>();

                 String clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


                 clientSubject = "testPerson_Expired";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken OR ServiceFailure"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken OR ServiceFailure"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken OR ServiceFailure"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(activeClient.getLatestRequestUrl(),tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                        handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                                e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }

         }
     }

     @WebTestName("isAuthorized - test object has a null AccessPolicy and testGroup as rights holder")
     @WebTestDescription("this test uses an object created with testGroup as the rights holder "
             + "and a null AccessPolicy. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_NullPolicy_testGroup_is_RightsHolder() {

         String procuringSubjectString = "testPerson";
         String objectSubjectString = null;
         Permission objectPermission = null;
//		 String objectIdentifier = "TierTesting:testObject:RightsHolder_testGroup" + testObjectSeriesSuffix;
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);

                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":RightsHolder_testGroup" + getTestObjectSeriesSuffix();

                 // get or create the test object
                 Identifier testObject = procureSpecialTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier),
//  				 		 "testGroup");
                 "CN=testGroup,DC=dataone,DC=org");


                // run tests
// testPerson is owner in this case, so don't need testRightsHolder tests
//				 activeClient = instantiateD1Node(clientSubject, node);  // should always have access
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 ArrayList<String> results = new ArrayList<String>();

                 String clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true OR NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true OR NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true OR NotAuthorized"));
                 } else {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }

                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has a public readable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has public readability enabled. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_Public_can_Read() {

         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = Constants.SUBJECT_PUBLIC;
         Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Public_READ" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":Public_READ" + getTestObjectSeriesSuffix();

                 // get or create the test object
                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an authenticated user readable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has readability enabled for an authenticated user. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_AuthenticatedUser_can_Read() {

         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = Constants.SUBJECT_AUTHENTICATED_USER;
         Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Authenticated_READ" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":Authenticated_READ" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);  // should always have access
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());


             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an verified user readable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has readability enabled for a verified user. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_VerifiedUser_can_Read() {

         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = Constants.SUBJECT_VERIFIED_USER;
         Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:Verified_READ" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":Verified_READ" + getTestObjectSeriesSuffix();

                 // get or create the test object
                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
                 } else {
                     // MNodes need subjectInfo to get verified status
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));



                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has testPerson-readable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has readability enabled for the testPerson subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testPerson_can_Read() {

         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
         Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_READ" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 // get or create the test object
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testPerson_READ" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));


                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);  // should always have access
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);

                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

//  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }

     }

     @WebTestName("isAuthorized - test object has an testPerson-writable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has write permission enabled for the testPerson subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testPerson_can_Write() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
         Permission objectPermission = Permission.WRITE;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_WRITE" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testPerson_WRITE" + getTestObjectSeriesSuffix();

                 // get or create the test object
                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node); // should always have access
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());


             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an testPerson-changeable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has change permission enabled for the testPerson subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testPerson_can_ChangePerm() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testPerson,DC=dataone,DC=org";
         Permission objectPermission = Permission.CHANGE_PERMISSION;
//		 String objectIdentifier = "TierTesting:testObject:testPerson_CHANGE" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();


             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testPerson_CHANGE" + getTestObjectSeriesSuffix();

                 // get or create the test object
                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));


                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an testGroup-readable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has read permission enabled for the testGroup subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testGroup_can_Read() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
         Permission objectPermission = Permission.READ;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_READ" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 // get or create the test object
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testGroup_READ" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
                 } else {
                     // MNodes need subjectInfo to get verified status
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());

             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an testGroup-writable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has write permission enabled for the testGroup subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testGroup_can_Write() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
         Permission objectPermission = Permission.WRITE;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_WRITE" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 // get or create the test object
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testGroup_WRITE" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
                 } else {
                     // MNodes need subjectInfo to get verified status
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }


                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));



                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());


             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an testGroup-changeable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has change permission enabled for the testGroup subject. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_testGroup_can_ChangePerm() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=testGroup,DC=dataone,DC=org";
         Permission objectPermission = Permission.CHANGE_PERMISSION;
//		 String objectIdentifier = "TierTesting:testObject:testGroup_CHANGE" + getTestObjectSeriesSuffix();
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 // get or create the test object
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":testGroup_CHANGE" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized OR true"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
                 } else {
                     // MNodes need subjectInfo to get verified status
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }

                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());


             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }

     @WebTestName("isAuthorized - test object has an legacy account writable AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy that has write permission enabled for some legacy account. It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
     @Test
     public void testIsAuthorized_AccessPolicy_is_legacyAccount_can_Write() {
         String procuringSubjectString = "testRightsHolder";
         String objectSubjectString = "CN=someLegacyAcct,DC=somewhere,DC=org";
         Permission objectPermission = Permission.WRITE;
         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 // get or create the test object
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":legacyAcct_WRITE" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureTestObject(activeClient,
                         buildAccessRule(objectSubjectString,objectPermission),
                         buildIdentifier(objectIdentifier));

                 ArrayList<String> results = new ArrayList<String>();

                 // run tests
                 String clientSubject = "testRightsHolder";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testPerson_NoSubjectInfo";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // CNodes can lookup subject info so results same as above
                 if (node.getType().equals(NodeType.CN)) {
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized OR true"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized OR true"));
                 } else {
                     // MNodes need subjectInfo to get verified status
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                     results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));
                 }

                 clientSubject = "testEQPerson1";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));

                 clientSubject = "testEQPerson3";
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "true"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "true"));


                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // to test access as a group member (of testPerson)
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = "testSubmitter";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // the designated no-rights subject
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                 clientSubject = Constants.SUBJECT_PUBLIC;
                 activeClient = instantiateD1Node(clientSubject, node);
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));


//				 clientSubject = "testPerson_Expired";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));

                 clientSubject = "testPerson_SelfSigned";
                 activeClient = instantiateD1Node(clientSubject, node);
                 // bad credentials should always fail
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "NotAuthorized"));
                 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "NotAuthorized"));

                //  TODO:  enable when ready to test
//				 clientSubject = "testPerson_InvalidVsSchema";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_Missing_EQ_IDs";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
//				 clientSubject = "testPerson_MissingSelf";
//				 activeClient = instantiateD1Node(clientSubject, node);
//				 // bad credentials should always fail
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.CHANGE_PERMISSION, "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.WRITE,             "InvalidToken"));
//				 results.add(checkExpectedIsAuthorizedOutcome(activeClient, testObject, clientSubject, Permission.READ,              "InvalidToken"));
//
                 for (String result : results) {
                     if (result != null && result.contains("FAILED!!")) {
                         handleFail(null, currentUrl + " " + tablifyResults(testObject, results) );
                         break;
                     }
                 }

             } catch (BaseException e) {
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
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

     @WebTestName("isAuthorized - test object has a more complicated AccessPolicy and testRightsHolder as rights holder")
     @WebTestDescription("this test uses an object created with testRightsHolder as the rights holder "
             + "and an AccessPolicy with read and write permission for testGroupie, "
             + "read permission for testPerson, and write permission for testPerson and 'cc'. "
             + "It runs a series of isAuthorized tests each "
             + "with a different certificate / subject trying to access the object")
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


         CommonCallAdapter activeClient = null;

         Iterator<Node> it = getNodeIterator();
         while (it.hasNext()) {
             Node node = it.next();
             currentUrl = node.getBaseURL();

             try {
                 activeClient = instantiateProcuringD1Node(procuringSubjectString, node);
                 String objectIdentifier = "TierTesting:" +
                     createNodeAbbreviation(activeClient.getNodeBaseServiceUrl()) +
                     ":ComplicatedPolicy" + getTestObjectSeriesSuffix();

                 Identifier testObject = procureSpecialTestObject(activeClient,
                         complicatedPolicy,
                         buildIdentifier(objectIdentifier), false);

                 String clientSubject = "testPerson";
                 activeClient = instantiateD1Node(clientSubject, node);

                 ArrayList<String> results = new ArrayList<String>();

                 // TODO:  set up the proper tests

                 String outcome;
                 if (runTest(Permission.READ)) {
                     outcome = runAuthTest(activeClient, testObject, Permission.READ);
                     if (! outcome.equals("true"))
                         results.add(String.format("FAILED!! %s should be allowed %s access" +
                                 " to %s. isAuthorized method did not process the 2nd or " +
                                 "3rd AccessRule.  Got %s",
                                 clientSubject,
                                 Permission.READ.xmlValue(),
                                 testObject.getValue(),
                                 outcome));
                 }
                 if (runTest(Permission.WRITE)) {
                     outcome = runAuthTest(activeClient, testObject, Permission.WRITE);
                     if (! outcome.equals("true"))
                         results.add(String.format("FAILED!! %s should be allowed %s access " +
                                 "to %s. isAuthorized method did not apply WRITE permission " +
                                 "to 2nd Subject in the AccessRule. Got %s",
                                 clientSubject,
                                 Permission.WRITE.xmlValue(),
                                 testObject.getValue(),
                                 outcome));
                 }
                 if (runTest(Permission.CHANGE_PERMISSION)) {
                     outcome = runAuthTest(activeClient, testObject, Permission.CHANGE_PERMISSION);
                     if (! outcome.equals("NotAuthorized"))
                         results.add(String.format("FAILED!! %s should NOT be allowed %s access " +
                                 "to %s. Got %s",
                                 clientSubject,
                                 Permission.CHANGE_PERMISSION.xmlValue(),
                                 testObject.getValue(),
                                 outcome));
                 }

                 clientSubject = "testGroupie";
                 activeClient = instantiateD1Node(clientSubject, node);

                 if (runTest(Permission.WRITE)) {
                     outcome = runAuthTest(activeClient, testObject, Permission.WRITE);
                     if (! outcome.equals("true"))
                         results.add(String.format("FAILED!! %s should be allowed %s access " +
                                 "to %s. isAuthorized method did not apply WRITE permission " +
                                 "to 2nd Permission in the AccessRule. Got %s",
                                 clientSubject,
                                 Permission.WRITE.xmlValue(),
                                 testObject.getValue(),
                                 outcome));
                 }
                 if (runTest(Permission.CHANGE_PERMISSION)) {
                     outcome = runAuthTest(activeClient, testObject, Permission.CHANGE_PERMISSION);
                     if (! outcome.equals("NotAuthorized"))
                         results.add(String.format("FAILED!! %s should NOT be allowed %s access " +
                                 "to %s. Got %s",
                                 clientSubject,
                                 Permission.CHANGE_PERMISSION.xmlValue(),
                                 testObject.getValue(),
                                 outcome));
                 }
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
                    handleFail(activeClient.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                            e.getDetail_code() + ": " + e.getDescription());


             } catch (Exception e) {
                 e.printStackTrace();
                 handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
             }
         }
     }


     private Identifier procureSpecialTestObject(CommonCallAdapter cca, AccessPolicy accessPolicy, Identifier pid, boolean exactPolicy)
     throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
     InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
     UnsupportedEncodingException, NotFound, TestIterationEndingException, ClientSideException
     {
         Identifier identifier = null;
         try {
             log.debug("procureTestObject: checking system metadata of requested object");
             SystemMetadata smd = cca.getSystemMetadata(null, pid);

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
             if (cca.getNodeType().equals(NodeType.MN)) {
                 Node node = cca.getCapabilities();
                 if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
                     log.debug("procureTestObject: calling createTestObject");
                     identifier = createTestObject(cca, pid, accessPolicy, "testSubmitter","CN=testRightsHolder,DC=dataone,DC=org");
                 }
             } else {
                 identifier = createTestObject(cca, pid, accessPolicy, cnSubmitter, "CN=testRightsHolder,DC=dataone,DC=org");
                // throw e;
             }
         }
         return identifier;
     }



     private  Identifier procureSpecialTestObject(CommonCallAdapter cca, AccessRule accessRule, Identifier pid, String rightsHolderSubjectString)
     throws InvalidToken, ServiceFailure, NotAuthorized, IdentifierNotUnique, UnsupportedType,
     InsufficientResources, InvalidSystemMetadata, NotImplemented, InvalidRequest,
     UnsupportedEncodingException, NotFound, TestIterationEndingException, ClientSideException
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
                     } else {
                         throw new TestIterationEndingException("the AccessRule of the returned object has either multiple subjects or multiple permissions");
                     }
                 } else {
                     throw new TestIterationEndingException("the AccessPolicy of the returned object is either null or has multiple AccessRules");
                 }
             }
         }
         catch (NotFound e) {
             if (cca.getNodeType().equals(NodeType.MN)) {
                 Node node = cca.getCapabilities();
                 if (APITestUtils.isServiceAvailable(node, "MNStorage")) {
                     log.debug("procureTestObject: calling createTestObject");
                     identifier = createTestObject(cca, pid, accessRule, "testSubmitter",rightsHolderSubjectString);
                 }
             } else {
                 identifier = createTestObject(cca,pid,accessRule,cnSubmitter,rightsHolderSubjectString);
                // throw e;
             }
         }
         log.info(" ====>>>>> pid of procured test Object: " + identifier.getValue());
         return identifier;

     }



}
