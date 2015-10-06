package org.dataone.integration.it.testImplementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.util.Constants;
import org.springframework.test.AssertThrows;

public class CNIdentityTestImplementations extends ContextAwareAdapter {

    public CNIdentityTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("registerAccount - tests with an invalid person")
    @WebTestDescription("tests a negative case, calling registerAccount with "
            + "an invalid Person, expecting an exception")
    public void testRegisterAccount_InvalidPerson(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegisterAccount_InvalidPerson(nodeIterator.next(), version);
    }

    /**
     * This is difficult to run over and over, because it require creating
     * new certificates for each new subject provided.
     */
    public void testRegisterAccount_InvalidPerson(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

        try {
            callAdapter.registerAccount(null, APITestUtils.buildPerson(
                    APITestUtils.buildSubject("testAccountA"),
                    "aFamily", "aGivenName", "me@foo.bar"));
            
            handleFail(callAdapter.getLatestRequestUrl(),
                    "registerAccount() should fail if not given a valid Subject");
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            // expected result - registerAccount() needs to fail if not given a valid subject
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("registerAccount - tests with an existing person")
    @WebTestDescription("tests a negative case, calling registerAccount with "
            + "a Person that already exists, expecting an exception")
    public void testRegisterAccount_ExistingPerson(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegisterAccount_ExistingPerson(nodeIterator.next(), version);
    }
    
    public void testRegisterAccount_ExistingPerson(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

        try {
            // test registerAccount( existingPerson )
            SubjectInfo subjectInfo = callAdapter.listSubjects(null, null, null, null, null);
            if (subjectInfo.getPersonList().size() > 0) {
                Person person = subjectInfo.getPerson(0);
                callAdapter.registerAccount(null, person);
            }
        } catch (BaseException e) {
            // expected result
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateAccount - tests with an invalid person")
    @WebTestDescription("tests a negative case, calling updateAccount with "
            + "an invalid Person, expecting an exception")
    public void testUpdateAccount_InvalidPerson(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateAccount_InvalidPerson(nodeIterator.next(), version);
    }

    public void testUpdateAccount_InvalidPerson(Node node, String version) {
        //TODO: is this the right client subject?  maybe testPerson?
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateAccount(...) vs. node: " + currentUrl);

        try {
            callAdapter.updateAccount(null, new Person());
            handleFail(callAdapter.getLatestRequestUrl(), "Should not be able to update an invalid Person");
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            // expected value - should not be able to update an invalid Person
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("verifyAccount - tests with CN subject")
    @WebTestDescription("tests verification call as CN")
    public void testVerifyAccount_AlreadyVerified(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testVerifyAccount_AlreadyVerified(nodeIterator.next(), version);
    }
    
    public void testVerifyAccount_AlreadyVerified(Node node, String version) {
            
        String currentUrl = node.getBaseURL();
        printTestHeader("testVerifyAccount_AlreadyVerified(...) vs. node: " + currentUrl);
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(cnSubmitter), node, version);
        
        try {
            SubjectInfo subjectInfo = callAdapter.listSubjects(null, null, null, null, null);
            Person verifiedPerson = null;
            for (Person person : subjectInfo.getPersonList()) {
                if (person.getVerified()) {
                    verifiedPerson = person;
                    break;
                }
            }
            
            if(verifiedPerson != null)
                callAdapter.verifyAccount(null, verifiedPerson.getSubject());
        }
        catch (BaseException e) {
            e.printStackTrace();
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("verifyAccount - tests with an unauthorized subject")
    @WebTestDescription("tests a negative case, calling verifyAccount as "
            + "a subject who is unauthorized to verify accounts, "
            + "expecting a NotAuthorized exception")
    public void testVerifyAccount_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testVerifyAccount_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testVerifyAccount_NotAuthorized(Node node, String version) {
        
        String currentUrl = node.getBaseURL();
        printTestHeader("testVerifyAccount_NotAuthorized(...) vs. node: " + currentUrl);
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(null), node, version);
        //CNCallAdapter callAdapter = new CNCallAdapter(getSession("testPerson"), node, version);

        
        try {
            SubjectInfo subjectInfo = callAdapter.listSubjects(null, null, null, null, null);
            Person verifiedPerson = null;
            for (Person person : subjectInfo.getPersonList())
                if (person.getVerified()) {
                    verifiedPerson = person;
                    break;
                }
            
            if(verifiedPerson != null)
                callAdapter.verifyAccount(null, verifiedPerson.getSubject());
        }
        catch (NotAuthorized e) {
            // expected result - Subject "testPerson" shouldn't be able to verify accounts
        }
        catch (BaseException e) {
            e.printStackTrace();
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("getSubjectInfo - tests that getSubjectInfo works")
    @WebTestDescription("test calling getSubjectInfo with a valid / existing "
            + "Subject and checks that it returns a non-null SubjectInfo object")
    public void testGetSubjectInfo(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSubjectInfo(nodeIterator.next(), version);
    }

    public void testGetSubjectInfo(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);

        try {
            SubjectInfo subjectList = callAdapter.listSubjects(null,"",null,null,null);
            Subject personSubject = subjectList.getPersonList().get(0).getSubject();
            SubjectInfo response = callAdapter.getSubjectInfo(null,personSubject);
            checkTrue(callAdapter.getLatestRequestUrl(),"getSubjectInfo(...) returns a SubjectInfo object", response != null);
            // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("getSubjectInfo - tests identifier with spaces")
    @WebTestDescription("test calling getSubjectInfo with an identifier that "
            + "contains spaces, expecting to get either a non-null SubjectInfo "
            + "or a NotFound exception, but no other kind of exception")
    public void testGetSubjectInfo_UrlEncodingSpaces(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSubjectInfo_UrlEncodingSpaces(nodeIterator.next(), version);
    }

    /**
     * URL character escaping tests are mostly done via get(Identifier), but since
     * Identifiers cannot contain spaces, that case is not tested there.  It is
     * tested here, instead.
     */
    public void testGetSubjectInfo_UrlEncodingSpaces(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);

        try {
            SubjectInfo response = callAdapter.getSubjectInfo(null,
                    D1TypeBuilder.buildSubject("CN=Duque de Alburquerque, DC=spain, DC=emp"));
            checkTrue(callAdapter.getLatestRequestUrl(),"getSubjectInfo(<subject with spaces>) should return either a SubjectInfo, or a NotFound", response != null);
            // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotFound e) {
            ; // the preferred response
        }
        catch (NotAuthorized e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("listSubjects - tests that listSubjects works with null parameters")
    @WebTestDescription("test calling listSubjects with null or empty parameters "
            + "expecting it to return a non-null SubjectInfo response")
    public void testListSubjects(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListSubjects(nodeIterator.next(), version);
    }

    public void testListSubjects(Node node, String version) {

        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListSubjects(...) vs. node: " + currentUrl);

        try {
            SubjectInfo response = callAdapter.listSubjects(null,"",null,null,null);
            checkTrue(callAdapter.getLatestRequestUrl(),"listSubjects(...) returns a SubjectInfo object", response != null);
            for (Person p: response.getPersonList()) {
                System.out.println("subject: " + p.getSubject().getValue());
            }
            // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @WebTestName("listSubjects - tests slicing with the count parameter")
    @WebTestDescription("test calling listSubjects with the count parameter set to half "
            + "the actual number of records, expecting it to return a SubjectInfo with "
            + "the expected number of results")
    public void testListSubjects_Slicing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListSubjects_Slicing(nodeIterator.next(), version);
    }

    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
    public void testListSubjects_Slicing(Node node, String version)
    {
        CNCallAdapter callAdapter = new CNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testListSubjects_Slicing(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
            SubjectInfo si = callAdapter.listSubjects(null, null, null, null, null);
            StringBuffer sb = new StringBuffer();
            int i = 0;

            // test that one can limit the count
            int halfCount = si.sizeGroupList() + si.sizePersonList() / 2; // rounds down
            si = callAdapter.listSubjects(null, null, null, 0, halfCount);

            if (si.sizeGroupList() + si.sizePersonList()  != halfCount)
                sb.append(++i + ". Should be able to limit the number of returned Subject objects using the 'count' parameter.");

            // TODO:  test that 'start' parameter does what it says
            // TODO: paging test
            if (i > 0) {
                handleFail(callAdapter.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
            }
        }
        catch (NotAuthorized e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
                    "with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void testMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testMapIdentity(nodeIterator.next(), version);
    }

    public void testMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testMapIdentity(...) vs. node: " + currentUrl);
//
//          try {

//              boolean response = cn.mapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"mapIdentity(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testRequestMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRequestMapIdentity(nodeIterator.next(), version);
    }

    public void testRequestMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testRequestMapIdentity(...) vs. node: " + currentUrl);
//
//          try {
//              boolean response = cn.requestMapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"requestMapIdentity(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testGetPendingMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPendingMapIdentity(nodeIterator.next(), version);
    }

    public void testGetPendingMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testGetPendingMapIdentity(...) vs. node: " + currentUrl);
//
//          try {
//              SubjectInfo response = cn.getPendingMapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"getPendingMapIdentity(...) returns a SubjectInfo object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testConfirmMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testConfirmMapIdentity(nodeIterator.next(), version);
    }

    public void testConfirmMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testConfirmMapIdentity(...) vs. node: " + currentUrl);
//
//          try {
//              boolean response = cn.confirmMapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"confirmMapIdentity(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testDenyMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testDenyMapIdentity(nodeIterator.next(), version);
    }

    public void testDenyMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testDenyMapIdentity(...) vs. node: " + currentUrl);
//
//          try {
//              boolean response = cn.denyMapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"denyMapIdentity(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testRemoveMapIdentity(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRemoveMapIdentity(nodeIterator.next(), version);
    }

    public void testRemoveMapIdentity(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testRemoveMapIdentity(...) vs. node: " + currentUrl);
//
//          try {
//              boolean response = cn.removeMapIdentity();
//              checkTrue(cn.getLatestRequestUrl(),"removeMapIdentity(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testCreateGroup(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testCreateGroup(nodeIterator.next(), version);
    }

    public void testCreateGroup(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testCreateGroup(...) vs. node: " + currentUrl);
//
//          try {//
//              Subject response = cn.createGroup();
//              checkTrue(cn.getLatestRequestUrl(),"createGroup(...) returns a Subject object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

    public void testUpdateGroup(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateGroup(nodeIterator.next(), version);
    }

    public void testUpdateGroup(Node node, String version) {
//      Iterator<Node> it = getCoordinatingNodeIterator();
//      while (it.hasNext()) {
//          currentUrl = it.next().getBaseURL();
//          CNode cn = new CNode(currentUrl);
//          printTestHeader("testUpdateGroup(...) vs. node: " + currentUrl);
//
//          try {
//              boolean response = cn.updateGroup();
//              checkTrue(cn.getLatestRequestUrl(),"updateGroup(...) returns a boolean object", response != null);
//              // checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//          }
//          catch (IndexOutOfBoundsException e) {
//              handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//          }
//          catch (BaseException e) {
//              handleFail(cn.getLatestRequestUrl(),e.getDescription());
//          }
//          catch(Exception e) {
//              e.printStackTrace();
//              handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//          }
//      }
    }

}
