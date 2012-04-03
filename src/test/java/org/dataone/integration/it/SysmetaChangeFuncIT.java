package org.dataone.integration.it;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1Node;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
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
		
		Map<MNode,Subject> expectedChange = new HashMap<MNode,Subject>();
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
					expectedChange.put(mn, setupClientSubject("testRightsHolder")); 
				} else {
					setupClientSubject("testRightsHolder");  
					cn.setRightsHolder(null, pid, testPerson, smdCN.getSerialVersion().longValue());					
					expectedChange.put(mn, setupClientSubject("testPerson"));
				}
				cnSmd.put(pid,smdCN);
				totalReplicaCount += smdCN.getReplicaList().size();
			}
			catch (BaseException e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
			}
		}
		
		
		// check for changes to be reflected in the sysmeta on the replica MNs
	
		Map<Identifier,List<Replica>> testedReplicas = new HashMap<Identifier, List<Replica>>();
		// but don't wait forever...
		int maxMinutes = 5;
		long cutoffTime = new Date().getTime() + maxMinutes * 60 * 1000;
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
								checkEquals(currentUrl, String.format("the updated sysmeta for '%s' should have '%s' as rightsHolder.",
										pid.getValue(), 
										expectedChange.get(mn).getValue()),
										smdMN.getRightsHolder().getValue(),
										expectedChange.get(mn).getValue()
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
		
		
		// finally report on timed-out tests
		
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
							maxMinutes));
				}
			}
			
		}
		
//		Iterator<Identifier> it = testedReplicas.keySet().iterator();
//		while (it.hasNext()) {
//			Identifier pid = it.next();
//			for (SystemMetadata sysmeta : cnSmd.get(pid)) {
//			
//		
//		}
		
	}
	
	
	@Test
	public void testSetAccessPolicyUseCase() throws ServiceFailure
	{

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

	
	private void testReplicas(Map<Identifier,SystemMetadata> cnSmd, Map<MNode,String> expectedChange, String testType) throws InterruptedException
	{
		int totalReplicaCount = 0;
		Iterator<Identifier> idit = cnSmd.keySet().iterator();
		while (idit.hasNext()) {
			totalReplicaCount += cnSmd.get(idit.next()).sizeReplicaList();
		}
		log.info("testReplicas().totalReplicaCount = " + totalReplicaCount);
		
		Map<Identifier,List<Replica>> testedReplicas = new HashMap<Identifier, List<Replica>>();
		// but don't wait forever...
		int maxMinutes = 5;
		long cutoffTime = new Date().getTime() + maxMinutes * 60 * 1000;
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
								checkEquals(currentUrl, String.format("the updated sysmeta for '%s' should have '%s' as rightsHolder.",
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
		
	}
	
	private String pullSMDValue(SystemMetadata smd, String type) {
		if (type.equals("rightsHolder"))
			return smd.getRightsHolder().getValue();
		if (type.equals("setRepPolicyNumReps"))
			return smd.getReplicationPolicy().getNumberReplicas().toString();
			
		return null;
		
	}

	@Override
	protected String getTestDescription() {
		return "Tests for the appropriate update in systemMetadata of the Authoritative membernode";
	}
}
