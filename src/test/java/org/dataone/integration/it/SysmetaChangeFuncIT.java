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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1Node;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that changes made to system metadata of an object are propagated back
 * to the authoritative and replica member nodes
 * @author rnahf
 *
 */
public class SysmetaChangeFuncIT extends ContextAwareTestCaseDataone {

	String currentUrl;
	
	Map<MNode,Identifier> createdObjectMap;
	List<MNode> syncedMNs;
	
	int maxWaitMinutes = 5;
	
	@Before
	public void createTestObjects() throws ServiceFailure, InvalidToken, NotAuthorized, NotImplemented
	{
		if (createdObjectMap == null) {
			setupClientSubject_NoCert();
			Iterator<Node> it = getMemberNodeIterator();

			createdObjectMap = new HashMap<MNode,Identifier>();

			int mnCount = 0;
			
			CNode cn = D1Client.getCN();
			
			// create or find the test objects on the MNs
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				MNode mn = D1Client.getMN(currentUrl);

				try {
					Identifier pid = procureTestObject(mn,
							APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ),
							APITestUtils.buildIdentifier(
									"FunctionalTest:smdChange:" + cn.lookupNodeId(currentUrl) )
							);
					createdObjectMap.put(mn,pid);
					
					log.info(String.format("creating test object '%s' on MN '%s' ",pid.getValue(),currentUrl));
					mnCount++;
				}
				catch (BaseException e) {
					e.printStackTrace();
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
				catch (TestIterationEndingException e) {
					e.printStackTrace();
				}
			}
			
			// make sure all test objects are known to the cn.
			// wait until all are accounted for
			Date startWait = new Date();
			int syncedCount = 0;
			syncedMNs = new ArrayList<MNode>();
			while (syncedCount < mnCount) {
				for (MNode mn : createdObjectMap.keySet()) {
					if (!syncedMNs.contains(mn)) {
						try {
							cn.get(null, createdObjectMap.get(mn));
//							cn.describe(null, createdObjectMap.get(mn));
							syncedMNs.add(mn);
							syncedCount++;
						} 
						catch (NotFound e) {
							; // not synced yet
						} 
					}
				}
				
				Date now = new Date();
				if (startWait.getTime() + 10 * 60 * 1000 < now.getTime()) {
					// exceeded reasonable time for sync
					// proceed with what we've got
					break;
				}
			}
			
		}
	}
	

	@Test
	public void testSetRightsHolderUseCase() throws ServiceFailure, InterruptedException
	{
		Subject testPerson = setupClientSubject("testPerson");
		Subject testRH = setupClientSubject("testRightsHolder");

		int totalReplicaCount = 0;
		
		
		// change the systemMetadata on the CN
		
		Map<MNode,String> expectedChange = new HashMap<MNode,String>();
		Map<Identifier,SystemMetadata> cnSmd = new HashMap<Identifier,SystemMetadata>();
		CNode cn = D1Client.getCN();
		for (MNode mn : createdObjectMap.keySet()) { 
			currentUrl = mn.getNodeBaseServiceUrl();
			Identifier pid = createdObjectMap.get(mn);
				
			try {
				// gather the 'before' info
				SystemMetadata smdCN = cn.getSystemMetadata(null, pid);
				// change the sysmeta on the cn		
				if (smdCN.getRightsHolder().equals(testPerson)) {
					setupClientSubject("testPerson");
					cn.setRightsHolder(null, pid, testRH, smdCN.getSerialVersion().longValue());					
					expectedChange.put(mn, setupClientSubject("testRightsHolder").getValue()); 
				} else {
					setupClientSubject("testRightsHolder");  
					cn.setRightsHolder(null, pid, testPerson, smdCN.getSerialVersion().longValue());					
					expectedChange.put(mn, setupClientSubject("testPerson").getValue());
				}
				cnSmd.put(pid,smdCN);
				totalReplicaCount += smdCN.getReplicaList().size();
			}
			catch (BaseException e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
			}
		}
		
		testReplicas(cnSmd,expectedChange,"rightsHolder", "the updated sysmeta for '%s' should have '%s' as rightsHolder.");
		
		// check for changes to be reflected in the sysmeta on the replica MNs
	
//		Map<Identifier,List<Replica>> testedReplicas = new HashMap<Identifier, List<Replica>>();
//		// but don't wait forever...
//		int maxMinutes = 5;
//		long cutoffTime = new Date().getTime() + maxMinutes * 60 * 1000;
//		int tested = 0;
//		while (cutoffTime > new Date().getTime() && tested < totalReplicaCount ) {
//			Thread.sleep(10 * 1000);
//			
//			// iterate through the pids of those successfully altered on the CN
//			Iterator<MNode> it = expectedChange.keySet().iterator();
//			while (it.hasNext()) {
//				MNode mn = it.next();
//				Identifier pid = createdObjectMap.get(mn);
//				log.info("checking replicas of " + pid.getValue());
//				// check each object's replicas
//				
//				for (Replica replica : cnSmd.get(pid).getReplicaList()) {
//					if (testedReplicas.get(pid) == null) 
//						testedReplicas.put(pid, new ArrayList<Replica>());
//			
//					if (!testedReplicas.get(pid).contains(replica)) {
//						try {
//							D1Node d1Node = D1Client.getMN(replica.getReplicaMemberNode());
//							SystemMetadata smdMN = d1Node.getSystemMetadata(null, pid);
//
//							if (smdMN.getSerialVersion().compareTo(cnSmd.get(pid).getSerialVersion()) > 0) {  // BigInts use this idiom for comparison
//								// there's been a change
//								log.info("pid: " + pid.getValue() + ". mn: " + replica.getReplicaMemberNode().getValue() + 
//										". mnRH: " + smdMN.getRightsHolder().getValue());
//								checkEquals(currentUrl, String.format("the updated sysmeta for '%s' should have '%s' as rightsHolder.",
//										pid.getValue(), 
//										expectedChange.get(mn).getValue()),
//										smdMN.getRightsHolder().getValue(),
//										expectedChange.get(mn).getValue()
//								);
//								testedReplicas.get(pid).add(replica);
//								tested++;
//							}
//						}
//						catch (BaseException e) {
//							log.warn("problem getting sysmeta from the mn::" +
//									e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
//						}
//					} // test block
//				} // replica loop
//			} // object loop
//		} // time loop
		
		
		// finally report on timed-out tests
		
		
		
//		Iterator<Identifier> it = testedReplicas.keySet().iterator();
//		while (it.hasNext()) {
//			Identifier pid = it.next();
//			for (SystemMetadata sysmeta : cnSmd.get(pid)) {
//			
//		
//		}
		
	}
	
	
	@Test
	public void testSetAccessPolicyUseCase() throws ServiceFailure, InterruptedException
	{
		Subject testPerson = setupClientSubject("testPerson");

		int totalReplicaCount = 0;
		
		
		// change the systemMetadata on the CN
		
		Map<MNode,String> expectedChange = new HashMap<MNode,String>();
		Map<Identifier,SystemMetadata> cnSmd = new HashMap<Identifier,SystemMetadata>();
		CNode cn = D1Client.getCN();
		for (MNode mn : createdObjectMap.keySet()) { 
			currentUrl = mn.getNodeBaseServiceUrl();
			Identifier pid = createdObjectMap.get(mn);
				
			try {
				// gather the 'before' info
				SystemMetadata smdCN = cn.getSystemMetadata(null, pid);

				// change the sysmeta on the cn		
				if (smdCN.getRightsHolder().equals(testPerson)) {
					setupClientSubject("testPerson");
				} else {
					setupClientSubject("testRightsHolder");  
				}

				String verifiedPermission = pullSMDValue(smdCN,"accessPolicy");
				AccessPolicy ap = smdCN.getAccessPolicy();
				if (verifiedPermission == null) {
					ap.addAllow(APITestUtils.buildAccessRule(Constants.SUBJECT_VERIFIED_USER, Permission.READ));
					cn.setAccessPolicy(null, pid, ap, smdCN.getSerialVersion().longValue());					
					expectedChange.put(mn, Permission.READ.toString()); 
				
				} else if (verifiedPermission.equals(Permission.READ.toString())) {
					AccessPolicy edittedAP = new AccessPolicy();
					for (AccessRule ar: ap.getAllowList()) {
						if (ar.getSubjectList().contains(D1TypeBuilder.buildSubject(Constants.SUBJECT_VERIFIED_USER))) {
							List<Permission> lp = new ArrayList<Permission>();
							lp.add(Permission.WRITE);
							ar.setPermissionList(lp);
						}
						edittedAP.addAllow(ar);
					}
					cn.setAccessPolicy(null, pid, edittedAP, smdCN.getSerialVersion().longValue());			
					expectedChange.put(mn, Permission.WRITE.toString());
					
				} else if (verifiedPermission.equals(Permission.WRITE.toString())) {
					AccessPolicy edittedAP = new AccessPolicy();
					for (AccessRule ar: ap.getAllowList()) {
						if (ar.getSubjectList().contains(D1TypeBuilder.buildSubject(Constants.SUBJECT_VERIFIED_USER))) {
							List<Permission> lp = new ArrayList<Permission>();
							lp.add(Permission.READ);
							ar.setPermissionList(lp);
						}
						edittedAP.addAllow(ar);
					}
					cn.setAccessPolicy(null, pid, edittedAP, smdCN.getSerialVersion().longValue());			
					expectedChange.put(mn, Permission.READ.toString());
					
					// this case shouldn't happen, but you never know....
				} else if (verifiedPermission.equals(Permission.CHANGE_PERMISSION.toString())) {
					AccessPolicy edittedAP = new AccessPolicy();
					for (AccessRule ar: ap.getAllowList()) {
						if (ar.getSubjectList().contains(Constants.SUBJECT_VERIFIED_USER)) {
							List<Permission> lp = new ArrayList<Permission>();
							lp.add(Permission.READ);
							ar.setPermissionList(lp);
						}
						edittedAP.addAllow(ar);
					}
					cn.setAccessPolicy(null, pid, edittedAP, smdCN.getSerialVersion().longValue());			
					expectedChange.put(mn, Permission.READ.toString());
				}
				
				cnSmd.put(pid,smdCN);
				totalReplicaCount += smdCN.getReplicaList().size();
			}
			catch (BaseException e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
			}
		}
		
		testReplicas(cnSmd,expectedChange,"accessPolicy","the updated sysmeta for '%s' should have Permission '%s' for the verifiedUser subject in the accessPolicy.");
	}
	
	
	
	@Test
	public void testSetReplicationStatusUseCase() throws ServiceFailure
	{

	}
	
	
	@Test
	public void testSetReplicationPolicyUseCase() throws ServiceFailure
	{

	}
	
	
	@Test
	public void testUpdateReplicaMetadataUseCase() throws ServiceFailure
	{

	}
	
	
	@Test
	public void testDeleteReplicaMetadataUseCase() throws ServiceFailure
	{

	}

	
	private void testReplicas(Map<Identifier,SystemMetadata> cnSmd, Map<MNode,String> expectedChange, String testType, String messageString) throws InterruptedException
	{
		int totalReplicaCount = 0;
		Iterator<Identifier> idit = cnSmd.keySet().iterator();
		while (idit.hasNext()) {
			totalReplicaCount += cnSmd.get(idit.next()).sizeReplicaList();
		}
		log.info("testReplicas().totalReplicaCount = " + totalReplicaCount);
		
		Map<Identifier,List<Replica>> testedReplicas = new HashMap<Identifier, List<Replica>>();
		// but don't wait forever...
		long cutoffTime = new Date().getTime() + maxWaitMinutes * 60 * 1000;
		int tested = 0;
		while (cutoffTime > new Date().getTime() && tested < totalReplicaCount ) {
			Thread.sleep(10 * 1000);
			
			// iterate through the pids of those successfully altered on the CN
			Iterator<MNode> it = expectedChange.keySet().iterator();
			while (it.hasNext()) {
				MNode mn = it.next();
				Identifier pid = createdObjectMap.get(mn);
				log.info("checking replicas of " + pid.getValue());
				// check each object's replicas
				
				for (Replica replica : cnSmd.get(pid).getReplicaList()) {
					if (testedReplicas.get(pid) == null) 
						testedReplicas.put(pid, new ArrayList<Replica>());
			
					if (!testedReplicas.get(pid).contains(replica)) {
						try {
							D1Node d1Node = D1Client.getMN(replica.getReplicaMemberNode());
							SystemMetadata smdMN = d1Node.getSystemMetadata(null, pid);

							if (smdMN.getSerialVersion().compareTo(cnSmd.get(pid).getSerialVersion()) > 0) {  // BigInts use this idiom for comparison
								// there's been a change
								log.info("pid: " + pid.getValue() + ". mn: " + replica.getReplicaMemberNode().getValue() + 
										". mnRH: " + smdMN.getRightsHolder().getValue());
								checkEquals(d1Node.getNodeBaseServiceUrl(), String.format(messageString,
										pid.getValue(), 
										expectedChange.get(mn)),
										pullSMDValue(smdMN, testType),
										expectedChange.get(mn)
								);
								testedReplicas.get(pid).add(replica);
								tested++;
							}
						}
						catch (BaseException e) {
							log.warn("problem getting sysmeta from the mn::" +
									e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
						}
					} // test block
				} // replica loop
			} // object loop
		} // time loop
		
		for (MNode mn : createdObjectMap.keySet()) { 
			currentUrl = mn.getNodeBaseServiceUrl();
			Identifier pid = createdObjectMap.get(mn);
			for (Replica r: cnSmd.get(pid).getReplicaList()) {
				if (!testedReplicas.get(pid).contains(r)) {
					// mn didn't have updated sysmetadata, so never tested
					handleFail(currentUrl,String.format("replica '%s' for pid '%s' did not " +
							"get updated within test timeframe (%d minutes)",
							r.getReplicaMemberNode().getValue(),
							pid.getValue(),
							maxWaitMinutes));
				}
			}
			
		}
//		return testedReplicas;
	}
	
	private String pullSMDValue(SystemMetadata smd, String type) {
		if (type.equals("rightsHolder"))
			return smd.getRightsHolder().getValue();
		if (type.equals("setRepPolicyNumReps"))
			return smd.getReplicationPolicy().getNumberReplicas().toString();
		if (type.equals("accessPolicy")) {
			HashMap<Subject,Set<Permission>> whosGotWhat = AccessUtil.getPermissionMap(smd.getAccessPolicy());
			Set<Permission> perms = whosGotWhat.get(D1TypeBuilder.buildSubject(Constants.SUBJECT_VERIFIED_USER));
			// should not have more than one permission, so not going to worry about the order if more than one
			return StringUtils.join(perms, ",");
		}
		return null;
		
	}

	@Override
	protected String getTestDescription() {
		return "Tests for the appropriate update in systemMetadata of the Authoritative membernode";
	}
}
