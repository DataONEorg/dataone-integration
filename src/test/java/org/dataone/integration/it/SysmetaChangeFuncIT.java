package org.dataone.integration.it;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
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
							cn.describe(null, createdObjectMap.get(mn));
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
	
	@Ignore("in progress")
	@Test
	public void testSetRightsHolderUseCase() throws ServiceFailure
	{
		Subject inheritorSubject = setupClientSubject("testPerson");
		
		CNode cn = D1Client.getCN();
		for (MNode mn : createdObjectMap.keySet()) { 
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testSetRightsHolderUseCase() vs. node: " + currentUrl);

			Identifier pid = createdObjectMap.get(mn);
				
			// do the test
			try {
				SystemMetadata smd = cn.getSystemMetadata(null, pid);
				cn.setRightsHolder(null, pid, inheritorSubject, smd.getSerialVersion().longValue());
			
			
			
				// check for change on original MN
		
				// check for change on all MNs
			}
			catch (BaseException e) {
				e.printStackTrace();
			}
//			catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			} 
//			catch (TestIterationEndingException e) {
//				e.printStackTrace();
//			}
		}
	}
	
	
	@Test
	public void testSetAccessPolicyUseCase()
	{
		
	}
	
	
	@Test
	public void testSetReplicationStatusUseCase()
	{
		
	}
	
	
	@Test
	public void testSetReplicationPolicyUseCase()
	{
		
	}
	
	
	@Test
	public void testUpdateReplicaMetadataUseCase()
	{
		
	}
	
	
	@Test
	public void testDeleteReplicaMetadataUseCase()
	{
		
	}


	@Override
	protected String getTestDescription() {
		return "Tests for the appropriate update in systemMetadata of the Authoritative membernode";
	}
}
