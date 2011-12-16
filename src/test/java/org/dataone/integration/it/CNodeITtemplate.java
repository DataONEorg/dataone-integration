package org.dataone.integration.it;

import java.io.InputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SubjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;



public class CNodeITtemplate extends ContextAwareTestCaseDataone {

	
	private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;

	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 * @throws ServiceFailure 
	 */
	@Before
	public void setup() throws ServiceFailure {
		prefetchObjects();
		//			generateStandardTests();
	}


	public void prefetchObjects() throws ServiceFailure {
		if (listedObjects == null) {
			listedObjects = new Hashtable<String,ObjectList>();
			Iterator<Node> it = getCoordinatingNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				CNode cn = D1Client.getCN();
				try {
					ObjectList ol = cn.search(null, QUERYTYPE_SOLR, "");
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


//	@Test
//	public void testCreate() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testCreate(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				Identifier response = cn.create(null, oi.getIdentifier(), object, sysmeta)
//				checkTrue(currentUrl,"create(...) returns a Identifier object", response != null);
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
	
	
	
	
	
	
	
	
	@Test
	public void testSearch() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSearch(...) vs. node: " + currentUrl);

			try {
				ObjectList response = cn.search(null, QUERYTYPE_SOLR, "");
				checkTrue(currentUrl,"search(...) returns a ObjectList object", response != null);
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
	public void testGet() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGet(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				InputStream response = cn.get(null, oi.getIdentifier());
				checkTrue(currentUrl,"get(...) returns a InputStream object", response != null);
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
	public void testGetSystemMetadata() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetSystemMetadata(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				SystemMetadata response = cn.getSystemMetadata(null, oi.getIdentifier());
				checkTrue(currentUrl,"getSystemMetadata(...) returns a SystemMetadata object", response != null);
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
	public void testResolve() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testResolve(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				ObjectLocationList response = cn.resolve(null, oi.getIdentifier());
				checkTrue(currentUrl,"resolve(...) returns a ObjectLocationList object", response != null);
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
	public void testAssertRelation() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testAssertRelation(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				ObjectInfo oi2 = getPrefetchedObject(currentUrl,-1);    
				log.debug("   pid2 = " + oi2.getIdentifier());

				// TODO:  find Relationship type ?
				boolean response = cn.assertRelation(null, oi.getIdentifier(),
						null, oi2.getIdentifier());
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
	public void testSetOwner() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetOwner(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				Subject subject = new Subject();
				subject.setValue("Public");
				Identifier response = cn.setRightsHolder(null, oi.getIdentifier(), subject, 1);
				checkTrue(currentUrl,"setOwner(...) returns a Identifier object", response != null);
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
	public void testIsAuthorized() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testIsAuthorized(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.isAuthorized(null,oi.getIdentifier(),Permission.READ);
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


	@Test
	public void testSetAccessPolicy() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetAccessPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.setAccessPolicy(null,oi.getIdentifier(),buildPublicReadAccessPolicy(),1);
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
	public void testCreateGroup() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testCreateGroup(...) vs. node: " + currentUrl);

			try {
				Subject subject = new Subject();
				subject.setValue("Public");
				Group group = new Group();
				group.setSubject(subject);
				Subject response = cn.createGroup(null,group);
				checkTrue(currentUrl,"createGroup(...) returns a Subject object", response != null);
				checkTrue(currentUrl,"createGroup(...) returns a Subject object with the same value", 
						response.getValue() == subject.getValue());
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

	@Ignore("not part of the service api")
	@Test
	public void testLookupNodeBaseUrl() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testLookupNodeBaseUrl(...) vs. node: " + currentUrl);

			try {
				// TODO would need to get a MNode to pass in.
				String response = cn.lookupNodeBaseUrl(null);
				checkTrue(currentUrl,"lookupNodeBaseUrl(...) returns a String object", response != null);
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

	@Ignore("not part of the service api")
	@Test
	public void testLookupNodeId() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testLookupNodeId(...) vs. node: " + currentUrl);

			try {
				String response = cn.lookupNodeId("");
				checkTrue(currentUrl,"lookupNodeId(...) returns a String object", response != null);
			} 
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


	@Test
	public void testListFormats() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testListFormats(...) vs. node: " + currentUrl);

			try {
				ObjectFormatList response = cn.listFormats();
				checkTrue(currentUrl,"listFormats(...) returns a ObjectFormatList object", response != null);
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
	public void testGetFormat() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetFormat(...) vs. node: " + currentUrl);

			// TODO: get a format from formatList
			try {
				ObjectFormat response = cn.getFormat(null);
				checkTrue(currentUrl,"getFormat(...) returns a ObjectFormat object", response != null);
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
	public void testListNodes() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testListNodes(...) vs. node: " + currentUrl);

			try {
				NodeList response = cn.listNodes();
				checkTrue(currentUrl,"listNodes(...) returns a NodeList object", response != null);
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
	public void testUpdateNodeCapabilities() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

			try {
				// TODO : all of this test
				boolean response = true;//cn.updateNodeCapabilities(null, nodeid, node)
				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
			} 
			// TODO uncomment this exception catch when method is called
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getDescription());
//			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}


	@Test
	public void testRegister() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegister(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				NodeReference response = cn.register(null, null);
				checkTrue(currentUrl,"register(...) returns a NodeReference object", response != null);
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
	public void testGetLogRecords() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetLogRecords(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
				Log response = cn.getLogRecords(null, fromDate, null, null, null, null);
				checkTrue(currentUrl,"getLogRecords(...) returns a Log object", response != null);
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


	// TODO: internal test for CNs, what to do?
//	@Test
//	public void testRegisterSystemMetadata() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRegisterSystemMetadata(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, objectFormatIdString, source);
//				boolean response = cn.registerSystemMetadata(null, oi.getIdentifier(), sysmeta);
//				checkTrue(currentUrl,"registerSystemMetadata(...) returns a boolean object", response != null);
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


	@Test
	public void testGetChecksum() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetChecksum(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				Checksum response = cn.getChecksum(null,oi.getIdentifier());
				checkTrue(currentUrl,"getChecksum(...) returns a Checksum object", response != null);
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
//	public void testRemoveGroupMembers() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRemoveGroupMembers(...) vs. node: " + currentUrl);
//			// TODO make meaningful Subject and SubjectLists
//			try {
//				Subject groupSubject = new Subject();
//				boolean response = cn.removeGroupMembers(null, 
//						groupSubject, ExampleUtilities.buildSubjectList(new String[]{"timmy","billy","lassie"}));
//				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
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
//	public void testAddGroupMembers() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testAddGroupMembers(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//				Subject groupSubject = new Subject();
//
//				boolean response = cn.removeGroupMembers(null, 
//						groupSubject, ExampleUtilities.buildSubjectList(new String[]{"timmy","billy","lassie"}));
//				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
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


	@Test
	public void testConfirmMapIdentity() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testConfirmMapIdentity(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.confirmMapIdentity(null, null);
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


	@Test
	public void testMapIdentity() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testMapIdentity(...) vs. node: " + currentUrl);

			try {
				boolean response = cn.mapIdentity(null, new Subject());
				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
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
	public void testListSubjects() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testListSubjects(...) vs. node: " + currentUrl);

			try {
				SubjectInfo response = cn.listSubjects(null, "need query semantics",null,null,null);
				checkTrue(currentUrl,"listSubjects(...) returns a SubjectList object", response != null);
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
	public void testGetSubjectInfo() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testGetSubjectInfo(...) vs. node: " + currentUrl);

			try {
				SubjectInfo response = cn.getSubjectInfo(null, new Subject());
				checkTrue(currentUrl,"getSubjectInfo(...) returns a SubjectList object", response != null);
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
				boolean response = cn.verifyAccount(null, new Subject());
				checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
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
				Subject response = cn.updateAccount(null,new Person());
				checkTrue(currentUrl,"updateAccount(...) returns a Subject object", response != null);
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
	public void testRegisterAccount() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);

			try {
				Subject response = cn.registerAccount(null, new Person());
				checkTrue(currentUrl,"registerAccount(...) returns a Subject object", response != null);
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
	public void testSetReplicationPolicy() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.setReplicationPolicy(null,oi.getIdentifier(),new ReplicationPolicy(),1);
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


	@Test
	public void testSetReplicationStatus() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			NodeReference cnRef = new NodeReference();
			cnRef.setValue(cn.getNodeId());
			printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.setReplicationStatus(null, 
						oi.getIdentifier(),cnRef, ReplicationStatus.COMPLETED,
						new ServiceFailure("", ""));
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


	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}