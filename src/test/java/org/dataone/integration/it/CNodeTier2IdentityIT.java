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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class CNodeTier2IdentityIT extends ContextAwareTestCaseDataone {

	

	private static final String badIdentifier = "ThisIdentifierShouldNotExist";
//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";
//	private static HashMap<String,String> StandardTests = new HashMap<String,String>();
	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	private static Vector<String> encodedPIDStrings = new Vector<String>();
	
	 private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;
  
	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 * @throws ServiceFailure 
	 */
	@Before
	public void setup() throws ServiceFailure {
//		prefetchObjects();
//		generateStandardTests();
	}


	public void prefetchObjects() throws ServiceFailure {
		if (listedObjects == null) {
			listedObjects = new Hashtable<String,ObjectList>();
			Iterator<Node> it = getCoordinatingNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				CNode cn = new CNode(currentUrl);
				currentUrl = cn.getNodeBaseServiceUrl();
				try {
					ObjectList ol = cn.listObjects(null);
					listedObjects.put(currentUrl, ol);
				} 
				catch (BaseException e) {
					handleFail(currentUrl,e.getDescription());
				}
				catch(Exception e) {
					log.warn(e.getClass().getName() + ": " + e.getMessage());
				}	
			}
		}
	}
	
	
	private ObjectInfo getPrefetchedObject(String currentUrl, Integer index)
	{
		if (index == null) 
			index = new Integer(0);
		if (index < 0) {
			// return off the right end of the list
			index = listedObjects.get(currentUrl).getCount() + index;
		}
		return listedObjects.get(currentUrl).getObjectInfo(index);
	}
	
	
	public void generateStandardTests() {

		if (testPIDEncodingStrings.size() == 0) {
			System.out.println(" * * * * * * * Unicode Test Strings * * * * * * ");
			
			InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
			Scanner s = new Scanner(is,"UTF-8");
			String[] temp;
			int c = 0;
			try
			{
				while (s.hasNextLine()) 
				{
					String line = s.nextLine();
					if (line.startsWith("common-") || line.startsWith("path-"))
					{
						System.out.println(c++ + "   " + line);
						temp = line.split("\t");
						if (temp.length > 1)
							testPIDEncodingStrings.add(temp[0]);
							encodedPIDStrings.add(temp[1]);
					}
				}	
				System.out.println("");
			} finally {
				s.close();
			}
			System.out.println("");
		}
	}
	

	@Test
	public void testRegisterAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

					// TODO:  fill out person object
				Subject response = cn.registerAccount(null, new Person());
				checkTrue(currentUrl,"registerAccount(...) returns a Subject object", response != null);
				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


	@Test
	public void testUpdateAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateAccount(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				Subject response = cn.updateAccount(null,new Person());
				checkTrue(currentUrl,"updateAccount(...) returns a Subject object", response != null);
				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


	@Test
	public void testVerifyAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testVerifyAccount(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.verifyAccount(null,new Subject());
				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			}
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


//	@Test
//	public void testGetSubjectInfo() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				SubjectInfo response = cn.getSubjectInfo();
//				checkTrue(currentUrl,"getSubjectInfo(...) returns a SubjectInfo object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testListSubjects() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testListSubjects(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				SubjectInfo response = cn.listSubjects();
//				checkTrue(currentUrl,"listSubjects(...) returns a SubjectInfo object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.mapIdentity();
//				checkTrue(currentUrl,"mapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testRequestMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRequestMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.requestMapIdentity();
//				checkTrue(currentUrl,"requestMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testGetPendingMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testGetPendingMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				SubjectInfo response = cn.getPendingMapIdentity();
//				checkTrue(currentUrl,"getPendingMapIdentity(...) returns a SubjectInfo object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testConfirmMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testConfirmMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.confirmMapIdentity();
//				checkTrue(currentUrl,"confirmMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testDenyMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testDenyMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.denyMapIdentity();
//				checkTrue(currentUrl,"denyMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testRemoveMapIdentity() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRemoveMapIdentity(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.removeMapIdentity();
//				checkTrue(currentUrl,"removeMapIdentity(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testCreateGroup() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testCreateGroup(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				Subject response = cn.createGroup();
//				checkTrue(currentUrl,"createGroup(...) returns a Subject object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//
//	@Test
//	public void testUpdateGroup() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testUpdateGroup(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.updateGroup();
//				checkTrue(currentUrl,"updateGroup(...) returns a boolean object", response != null);
//				// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
//			} 
//			catch (IndexOutOfBoundsException e) {
//				handleFail(currentUrl,"No Objects available to test against");
//			}
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
//			catch(Exception e) {
//				e.printStackTrace();
//				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
//			}
//		}
//	}
//
//	
	

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
