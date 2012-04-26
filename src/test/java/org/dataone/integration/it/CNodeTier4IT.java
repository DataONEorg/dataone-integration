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

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier4IT extends ContextAwareTestCaseDataone {

	

	private static final String badIdentifier = "ThisIdentifierShouldNotExist";

	private static String currentUrl;
	private static Map<String,ObjectList> listedObjects;
 
	
	@Override
	protected String getTestDescription() {
		return "Tests the basic functioning of the Tier4 CN Replication service api " +
				"in a stand-alone context.  (Mostly negative tests - positive ones " +
				"require a synchronizing environment";
	}
	
	
	/**
	 * pre-fetch an ObjectList from each member node on the list, to allow testing gets
	 * without creating new objects.
	 * @throws ServiceFailure 
	 */
	@Before
	public void setup() throws ServiceFailure {
		prefetchObjects();
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
					handleFail(cn.getLatestRequestUrl(),e.getDescription());
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
	
	
	/**
	 * Test the that the membernode can set the replication status for on an object
	 */
	@Ignore("test not implemented")
	@Test
	public void testSetReplicationStatus_NotAuthorized() {
		setupClientSubject("testSubmitter");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				boolean response = cn.setReplicationStatus(null,oi.getIdentifier(),new NodeReference(),
						ReplicationStatus.FAILED,new ServiceFailure("0000","a test exception"));

//				checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
				
				handleFail(cn.getLatestRequestUrl(),"setReplicationStatus should fail when using no-rights client subject");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotAuthorized e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	
	/**
	 * Test the that the membernode can set the replication status for on an object
	 */
	@Ignore("test not implemented")
	@Test
	public void testSetReplicationStatus_InvalidRequest() {
		//TODO: implement a memberNode test subject
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

			try {
				Identifier pid = new Identifier();
				pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());

				boolean response = cn.setReplicationStatus(null,pid, new NodeReference(),
						ReplicationStatus.COMPLETED, new ServiceFailure("0000","a test exception"));

				handleFail(cn.getLatestRequestUrl(),"setReplicationStatus should fail when bogus nodeReference passed in");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (InvalidRequest e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test the that the membernode can set the replication status for on an object
	 */
	@Ignore("test not implemented")
	@Test
	public void testSetReplicationStatus_NotFound() {
		//TODO: implement a memberNode test subject
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);

			try {
				Identifier pid = new Identifier();
				pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());

				boolean response = cn.setReplicationStatus(null,pid, new NodeReference(),
						ReplicationStatus.FAILED, new ServiceFailure("0000","a test exception"));

				handleFail(cn.getLatestRequestUrl(),"setReplicationStatus should fail when fictitious pid passed in");
			} 
			catch (NotFound e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testSetReplicationPolicy() {
		setupClientSubject("testAdmin");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				BigInteger serialVersion = cn.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();

				ReplicationPolicy policy = new ReplicationPolicy();
				policy.setNumberReplicas(4);
				
				boolean response = cn.setReplicationPolicy(null,oi.getIdentifier(),
						policy, serialVersion.longValue());
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
	
	@Ignore("test not implemented")	
	@Test
	public void testSetReplicationPolicy_NotAuthorized() {
		setupClientSubject("testSubmitter");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				BigInteger serialVersion = cn.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();

				ReplicationPolicy policy = new ReplicationPolicy();
				policy.setNumberReplicas(4);
				
				boolean response = cn.setReplicationPolicy(null,oi.getIdentifier(),
						policy, serialVersion.longValue());
				handleFail(cn.getLatestRequestUrl(),"setReplicationPolicy should fail when using no-right client subject");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotAuthorized e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testSetReplicationPolicy_NotFound() {
		setupClientSubject("testAdmin");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				Identifier pid = new Identifier();
				pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());
				
				ReplicationPolicy policy = new ReplicationPolicy();
				policy.setNumberReplicas(4);
				
				boolean response = cn.setReplicationPolicy(null,pid,
						policy, 100);
				handleFail(cn.getLatestRequestUrl(),"setReplicationPolicy should fail when passing in fictitious pid");
			} 
			
			catch (NotFound e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testSetReplicationPolicy_VersionMismatch() {
		setupClientSubject("testAdmin");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				BigInteger serialVersion = cn.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion();
				
				ReplicationPolicy policy = new ReplicationPolicy();
				policy.setNumberReplicas(4);

				boolean response = cn.setReplicationPolicy(null,oi.getIdentifier(),
						policy, serialVersion.longValue()+10);
				handleFail(cn.getLatestRequestUrl(),"setReplicationPolicy should fail when setting a bogus serial version of the sysmeta");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (VersionMismatch e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with VersionMismatch. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testSetReplicationPolicy_InvalidRequest() {
		setupClientSubject("testAdmin");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				long serialVersion = cn.getSystemMetadata(null, oi.getIdentifier()).getSerialVersion().longValue();

				ReplicationPolicy policy = new ReplicationPolicy();
				policy.setNumberReplicas(-1);
				
				boolean response = cn.setReplicationPolicy(null,oi.getIdentifier(),
						policy, serialVersion);
				handleFail(cn.getLatestRequestUrl(),"setReplicationPolicy should fail when setting number of replicas to -1");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (InvalidRequest e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	

	@Ignore("test not implemented")
	@Test
	public void testIsNodeAuthorized_InvalidToken() {
		setupClientSubject("testAdmin");

		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());

				// TODO: should not be current identity, but Subject of a listed member node
				Subject subject = ClientIdentityManager.getCurrentIdentity();
				
				boolean response = cn.isNodeAuthorized(null, subject, oi.getIdentifier());
				
//				checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
				handleFail(cn.getLatestRequestUrl(),"isNodeAuthorized should fail when using no-rights client subject");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (InvalidToken e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidToken. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	@Ignore("test not implemented")
	@Test
	public void testIsNodeAuthorized_NotAuthorized() {
		setupClientSubject("testSubmitter");
		Subject noRightsSubject = ClientIdentityManager.getCurrentIdentity();
		// TODO: 
//		setupClientSubject("testMemberNode");
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
			
				boolean response = cn.isNodeAuthorized(null, noRightsSubject, oi.getIdentifier());
				
//				checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
				handleFail(cn.getLatestRequestUrl(),"isNodeAuthorized should fail when using no-rights client subject");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotAuthorized e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")
	@Test
	public void testIsNodeAuthorized_InvalidRequest() {
		// TODO: 
		setupClientSubject("testMemberNode");
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

			try {
				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
				log.debug("   pid = " + oi.getIdentifier());
				
				// passing in a null value for subject
				boolean response = cn.isNodeAuthorized(null, null, oi.getIdentifier());
				
				handleFail(cn.getLatestRequestUrl(),"isNodeAuthorized should fail when passing in null subject (omitting subject)");
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (InvalidRequest e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testIsNodeAuthorized_NotFound() {
		// TODO: 
//		setupClientSubject("testMemberNode");
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);

			try {
				Identifier pid = new Identifier();
				pid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());
				
				Subject subject = ClientIdentityManager.getCurrentIdentity();
				
				// passing in a null value for subject
				boolean response = cn.isNodeAuthorized(null, subject, pid);
				
				handleFail(cn.getLatestRequestUrl(),"isNodeAuthorized should fail when passing in fictitious pid");
			}
			catch (NotFound e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

	@Ignore("test not implemented")
	@Test
	public void testUpdateReplicationMetadata() {
		//TODO:
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);

			try {
				// want to get an object already replicated
				// will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
				List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
				List<ObjectInfo> unreplicatedObjects = cn.listObjects(null, null, null, null, false, null, null).getObjectInfoList();

				allObjects.removeAll(unreplicatedObjects);
				Identifier replicatedObject = allObjects.get(0).getIdentifier();				
				log.debug("   pid = " + replicatedObject);
				
				
				SystemMetadata smd = cn.getSystemMetadata(null, replicatedObject);
				long serialVersion = smd.getSerialVersion().longValue();
				Replica replica = smd.getReplica(0);
				
				// try an update to the replica by replacing it with itself... (no changes)
				boolean response = cn.updateReplicationMetadata(null, replicatedObject, replica, serialVersion);

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
	
	@Ignore("test not implemented")	
	@Test
	public void testUpdateReplicationMetadata_NotAuthorized() {
		setupClientSubject("testNoRights");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);

			try {
				// want to get an object already replicated
				// will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
				List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
				List<ObjectInfo> unreplicatedObjects = cn.listObjects(null, null, null, null, false, null, null).getObjectInfoList();

				allObjects.removeAll(unreplicatedObjects);
				Identifier replicatedObject = allObjects.get(0).getIdentifier();				
				log.debug("   pid = " + replicatedObject);
				
				
				SystemMetadata smd = cn.getSystemMetadata(null, replicatedObject);
				long serialVersion = smd.getSerialVersion().longValue();
				Replica replica = smd.getReplica(0);
				
				// try an update to the replica by replacing it with itself... (no changes)
				boolean response = cn.updateReplicationMetadata(null, replicatedObject, replica, serialVersion);

				handleFail(cn.getLatestRequestUrl(),"updateReplicaMetadata should fail when using no-rights subject");
			}
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotAuthorized e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotAuthorized. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testUpdateReplicationMetadata_NotFound() {
		//TODO:
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);

			try {
				// want to get an object already replicated
				// will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
				List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
				List<ObjectInfo> unreplicatedObjects = cn.listObjects(null, null, null, null, false, null, null).getObjectInfoList();

				allObjects.removeAll(unreplicatedObjects);
				Identifier replicatedObject = allObjects.get(0).getIdentifier();				
				log.debug("   pid = " + replicatedObject);

				Identifier badPid = new Identifier();
				badPid.setValue("CNodeTier4test: " + ExampleUtilities.generateIdentifier());

				
				SystemMetadata smd = cn.getSystemMetadata(null, replicatedObject);
				long serialVersion = smd.getSerialVersion().longValue();
				Replica replica = smd.getReplica(0);
				
				// try an update to the replica by replacing it with itself... (no changes)
				boolean response = cn.updateReplicationMetadata(null, badPid, replica, serialVersion);

				handleFail(cn.getLatestRequestUrl(),"updateReplicaMetadata should fail when using no-rights subject");
			}
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (NotFound e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with NotFound. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")	
	@Test
	public void testUpdateReplicationMetadata_InvalidRequest() {
		//TODO:
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);

			try {
				// want to get an object already replicated
				// will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
				List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
				List<ObjectInfo> unreplicatedObjects = cn.listObjects(null, null, null, null, false, null, null).getObjectInfoList();

				allObjects.removeAll(unreplicatedObjects);
				Identifier replicatedObject = allObjects.get(0).getIdentifier();				
				log.debug("   pid = " + replicatedObject);

				
				SystemMetadata smd = cn.getSystemMetadata(null, replicatedObject);
				long serialVersion = smd.getSerialVersion().longValue();
				Replica replica = smd.getReplica(0);
				
				// try an update to the replica by replacing it with a null value
				boolean response = cn.updateReplicationMetadata(null, replicatedObject, null, serialVersion);

				handleFail(cn.getLatestRequestUrl(),"updateReplicaMetadata should fail when using no-rights subject");
			}
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (InvalidRequest e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	@Ignore("test not implemented")
	@Test
	public void testUpdateReplicationMetadata_VersionMismatch() {
		//TODO:
//		setupClientSubject("testMemberNode");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);

			try {
				// want to get an object already replicated
				// will apply set logic:  allObjects - unreplicatedObject => replicatedObjects
				List<ObjectInfo> allObjects = listedObjects.get(currentUrl).getObjectInfoList();
				List<ObjectInfo> unreplicatedObjects = cn.listObjects(null, null, null, null, false, null, null).getObjectInfoList();

				allObjects.removeAll(unreplicatedObjects);
				Identifier replicatedObject = allObjects.get(0).getIdentifier();				
				log.debug("   pid = " + replicatedObject);

				
				SystemMetadata smd = cn.getSystemMetadata(null, replicatedObject);
				long serialVersion = smd.getSerialVersion().longValue();
				Replica replica = smd.getReplica(0);
				
				// try an update to the replica by replacing it with itself... (no changes)
				boolean response = cn.updateReplicationMetadata(null, replicatedObject, replica, serialVersion + 10);

				handleFail(cn.getLatestRequestUrl(),"updateReplicaMetadata should fail when passing in a bad serialVersion");
			}
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
			catch (VersionMismatch e) {
				// the expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with VersionMismatch. Got: " + e.getClass() + 
						":: " + e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

}
