package org.dataone.integration.it.testImplementations;

import java.util.Iterator;

import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;

public class CNIdentityTestImplementations extends ContextAwareAdapter {

    public CNIdentityTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    public void testRegisterAccount(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testRegisterAccount(nodeIterator.next(), version);
    }
    
    /**
     * This is difficult to run over and over, because it require creating
     * new certificates for each new subject provided. 
     */
    public void testRegisterAccount(Node node, String version) {
        
        ContextAwareTestCaseDataone.setupClientSubject("testPerson");
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

        try {
            Subject response = callAdapter.registerAccount(null, APITestUtils.buildPerson(
                    APITestUtils.buildSubject("testAccountA"),
                    "aFamily", "aGivenName", "me@foo.bar"));
            checkTrue(callAdapter.getLatestRequestUrl(),"registerAccount(...) returns a Subject object",
                    response != null);
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
    
    public void testUpdateAccount(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateAccount(nodeIterator.next(), version);
    }
    
    public void testUpdateAccount(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateAccount(...) vs. node: " + currentUrl);

        try {
            Subject response = callAdapter.updateAccount(null,new Person());
            checkTrue(callAdapter.getLatestRequestUrl(),"updateAccount(...) returns a Subject object", response != null);
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

    public void testVerifyAccount(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testVerifyAccount(nodeIterator.next(), version);
    }
    
    public void testVerifyAccount(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testVerifyAccount(...) vs. node: " + currentUrl);

        try {
            boolean response = callAdapter.verifyAccount(null,new Subject());
            checkTrue(callAdapter.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
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

    public void testGetSubjectInfo(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetSubjectInfo(nodeIterator.next(), version);
    }
    
    public void testGetSubjectInfo(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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
        
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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

    public void testListSubjects(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testListSubjects(nodeIterator.next(), version);
    }
    
    public void testListSubjects(Node node, String version) {
        
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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
        CNCallAdapter callAdapter = new CNCallAdapter(MULTIPART_REST_CLIENT, node, version);
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
