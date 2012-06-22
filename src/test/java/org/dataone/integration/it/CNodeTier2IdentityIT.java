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
 */

package org.dataone.integration.it;

import java.util.Iterator;

import org.dataone.client.CNode;
import org.dataone.client.D1TypeBuilder;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier2IdentityIT extends ContextAwareTestCaseDataone {

	
	private static String currentUrl;

	 
	/**
	 * This is difficult to run over and over, because it require creating
	 * new certificates for each new subject provided. 
	 */
//	@Test
	public void testRegisterAccount() {
		setupClientSubject("testPerson");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

			try {
				Subject response = cn.registerAccount(null, APITestUtils.buildPerson(
						APITestUtils.buildSubject("testAccountA"),
						"aFamily", "aGivenName", "me@foo.bar"));
				checkTrue(cn.getLatestRequestUrl(),"registerAccount(...) returns a Subject object",
						response != null);
				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


//	@Test
	public void testUpdateAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateAccount(...) vs. node: " + currentUrl);

			try {
				Subject response = cn.updateAccount(null,new Person());
				checkTrue(cn.getLatestRequestUrl(),"updateAccount(...) returns a Subject object", response != null);
				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


//	@Test
	public void testVerifyAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testVerifyAccount(...) vs. node: " + currentUrl);

			try {
				boolean response = cn.verifyAccount(null,new Subject());
				checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

//	@Ignore("test not written yet")
	@Test
	public void testGetSubjectInfo() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);

			try {
				SubjectInfo subjectList = cn.listSubjects(null,"",null,null,null);
				Subject personSubject = subjectList.getPersonList().get(0).getSubject();
				SubjectInfo response = cn.getSubjectInfo(null,personSubject);
				checkTrue(cn.getLatestRequestUrl(),"getSubjectInfo(...) returns a SubjectInfo object", response != null);
				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	
	/** 
	 * URL character escaping tests are mostly done via get(Identifier), but since
	 * Identifiers cannot contain spaces, that case is not tested there.  It is
	 * tested here, instead. 
	 */
	@Test
	public void testGetSubjectInfo_UrlEncodingSpaces() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);

			try {
				SubjectInfo response = cn.getSubjectInfo(null,
						D1TypeBuilder.buildSubject("CN=Duque de Alburquerque, DC=spain, DC=emp"));
				checkTrue(cn.getLatestRequestUrl(),"getSubjectInfo(<subject with spaces>) should return either a SubjectInfo, or a NotFound", response != null);
				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotFound e) {
				; // the preferred response
			}
			catch (NotAuthorized e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}



	@Test
	public void testListSubjects() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testListSubjects(...) vs. node: " + currentUrl);

			try {
				SubjectInfo response = cn.listSubjects(null,"",null,null,null);
				checkTrue(cn.getLatestRequestUrl(),"listSubjects(...) returns a SubjectInfo object", response != null);
				for (Person p: response.getPersonList()) {
					System.out.println("subject: " + p.getSubject().getValue());
				}
				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	
	
    /**
     * Tests that count and start parameters are functioning, and getCount() and getTotal()
     * are reasonable values.
     */
	@Ignore("slicing behavior not yet defined for SubjectInfo - there are two lists contained therein.")
	@Test
    public void testListSubjects_Slicing()
    {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListSubjects_Slicing(...) vs. node: " + currentUrl);  
    		currentUrl = cn.getNodeBaseServiceUrl();

    		try {
    			SubjectInfo si = cn.listSubjects(null, null, null, null);   			
    			
    			StringBuffer sb = new StringBuffer();
    			int i = 0;
    			
    			// test that one can limit the count
    			int halfCount = si.sizeGroupList() + si.sizePersonList() / 2; // rounds down
    			si = cn.listSubjects(null, null, 0, halfCount);

    			if (si.sizeGroupList() + si.sizePersonList()  != halfCount)
    				sb.append(++i + ". Should be able to limit the number of returned Subject objects using the 'count' parameter.");
    				    			
    			// TODO:  test that 'start' parameter does what it says

    			// TODO: paging test
    			
    			
    			if (i > 0) {
    				handleFail(cn.getLatestRequestUrl(),"Slicing errors:\n" + sb.toString());
    			}   			
    			
    		}
    		catch (NotAuthorized e) {
    			handleFail(cn.getLatestRequestUrl(),"Should not get a NotAuthorized when connecting" +
    					"with a cn admin subject . Check NodeList and MN configuration.  Msg details:" +
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ": " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}	           
    	}
    }
	
	
	
	@Ignore("test not written yet")
	@Test
	public void testMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testMapIdentity(...) vs. node: " + currentUrl);
//
//			try {

//				boolean response = cn.mapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"mapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}
	
	
	@Ignore("test not written yet")
	@Test
	public void testRequestMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRequestMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				boolean response = cn.requestMapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"requestMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}

	
	@Ignore("test not written yet")
	@Test
	public void testGetPendingMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testGetPendingMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				SubjectInfo response = cn.getPendingMapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"getPendingMapIdentity(...) returns a SubjectInfo object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}


	@Ignore("test not written yet")
	@Test
	public void testConfirmMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testConfirmMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				boolean response = cn.confirmMapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"confirmMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}


	@Ignore("test not written yet")
	@Test
	public void testDenyMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testDenyMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				boolean response = cn.denyMapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"denyMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}


	@Ignore("test not written yet")
	@Test
	public void testRemoveMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRemoveMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				boolean response = cn.removeMapIdentity();
//				checkTrue(cn.getLatestRequestUrl(),"removeMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}


	@Ignore("test not written yet")
	@Test
	public void testCreateGroup() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testCreateGroup(...) vs. node: " + currentUrl);
//
//			try {//
//				Subject response = cn.createGroup();
//				checkTrue(cn.getLatestRequestUrl(),"createGroup(...) returns a Subject object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}


	@Ignore("test not written yet")
	@Test
	public void testUpdateGroup() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testUpdateGroup(...) vs. node: " + currentUrl);
//
//			try {
//				boolean response = cn.updateGroup();
//				checkTrue(cn.getLatestRequestUrl(),"updateGroup(...) returns a boolean object", response != null);
//				// checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(cn.getLatestRequestUrl(),e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
	}
	

	@Override
	protected String getTestDescription() {
		return "Tests CN Identity methods";
	}

}
