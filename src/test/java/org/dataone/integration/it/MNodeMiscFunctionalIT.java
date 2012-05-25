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
 * Functional tests for MN api calls that involve interactions with CNs
 * @author rnahf
 *
 */
public class MNodeMiscFunctionalIT extends ContextAwareTestCaseDataone {

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
	
	/**
	 * reserve an ID, then try to create the object.  Should succeed.
	 * This test checks that an MN is check with the CN before creating object.
	 */
	@Test
	public void testCreateUsingReserveId() {
		
	}
	
	/**
	 * reserve an ID using one subject, then try to create with another. Should fail.
	 */
	@Test
	public void testCreateUsingReserveId_NotReserver() {
		
	}
	
	/**
	 * Nodes of all Tiers should do this, but stand-alone tests won't work
	 * for Tier2+ nodes, which also need to check with CN via isNodeAuthorized().
	 * Can it be tested without initiating replication? via systemMetadata change?
	 */
	@Test
	public void testGetReplica_CallLoggedAsReplicate() {
		
	}

	/**
	 * if content is restricted, mn.getReplica() needs to check that the caller 
	 * (another MN) is supposed to be making this request, using cn.isNodeAuthorized().
	 * This test does not initiate a replication cycle, so it will test that
	 * the appropriate exception is returned.  cn should throw NotAuthorized back 
	 * to the mn, and the mn should throw NotAuthorized back to the client.
	 * 
	 *   Tier 1 nodes, (all public content) will not test the same.  They will
	 *   not fail.
	 */
	@Test
	public void testGetReplica_isNodeAuthorized() {
		
	}
	
	
	
	
	@Override
	protected String getTestDescription() {
		return "Tests for the appropriate update in systemMetadata of the Authoritative membernode";
	}
}
