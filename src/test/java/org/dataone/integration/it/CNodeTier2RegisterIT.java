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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.dataone.client.CNode;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Ping;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier2RegisterIT extends ContextAwareTestCaseDataone {

	private static String currentUrl;
	
	protected String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", /* default */ "urn:node:cnDevUNM1");

	

	@Override
	protected String getTestDescription() {
		return "tests the CN.Register methods";
	}
	
	@Ignore("confirm the change to the Node record")
	@Test
	public void testUpdateNodeCapabilities() {
		// TODO: set the appropriate subject - will need a subject that can
		// update it's node record on the CN.  either an MN or CN subject.
		setupClientSubject(cnSubmitter);
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

			try {
				Set<Node> cNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.CN);
				if (cNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test updateNodeCapabilities unless there is a node in the NodeList");
				} else {
					Node node = cNodeSet.iterator().next();
					NodeReference nodeRef = node.getIdentifier();
					
					Ping ping = node.getPing();
					Date orginalLastSuccess = ping.getLastSuccess();
					ping.setLastSuccess(new Date());
					node.setPing(ping);
					
					boolean response = cn.updateNodeCapabilities(null,nodeRef,node);
					checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
			
					// TODO: confirm the changed node record.  currently cannot do this as the node update
					// process is not automatic.
									
				} 
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
	public void testUpdateNodeCapabilities_NotFound() {

		setupClientSubject(cnSubmitter);
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

			try {
				Set<Node> cNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.CN);
				if (cNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test updateNodeCapabilities unless there is a node in the NodeList");
				} else {
					Node node = cNodeSet.iterator().next();
					NodeReference nodeRef = node.getIdentifier();
					nodeRef.setValue(nodeRef.getValue() + "bizzBazzBuzz");
					node.setIdentifier(nodeRef);
										
					Ping ping = node.getPing();
					Date orginalLastSuccess = ping.getLastSuccess();
					ping.setLastSuccess(new Date());
					node.setPing(ping);
					
					boolean response = cn.updateNodeCapabilities(null,nodeRef,node);
					handleFail(cn.getLatestRequestUrl(),"updateNodeCapabilities on fictitious node should fail");
			
					// TODO: confirm the changed node record.  currently cannot do this as the node update
					// process is not automatic.
									
				} 
			}
			catch (NotFound e) {
				// this is the expected behavior
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
	
	
	
	@Test
	public void testUpdateNodeCapabilities_NotAuthorized() {
		//TODO ensure that the current subject is not able to update the node record
		// do this by looking at the node record?
		setupClientSubject("testSubmitter");
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

			try {
				Set<Node> cNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.CN);
				if (cNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test updateNodeCapabilities unless there is a node in the NodeList");
				} else {
					Node node = cNodeSet.iterator().next();
					NodeReference nodeRef = node.getIdentifier();
										
					Ping ping = node.getPing();
					Date orginalLastSuccess = ping.getLastSuccess();
					ping.setLastSuccess(new Date());
					node.setPing(ping);
					
					boolean response = cn.updateNodeCapabilities(null,nodeRef,node);
					handleFail(cn.getLatestRequestUrl(),"updateNodeCapabilities on fictitious node should fail");
			
					// TODO: confirm the changed node record.  currently cannot do this as the node update
					// process is not automatic.
									
				} 
			}
			catch (NotAuthorized e) {
				// this is the expected behavior
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

	@Ignore("not many values it will refuse change on")
	@Test
	public void testUpdateNodeCapabilities_updatingOtherField() {
		// TODO: set the appropriate subject - will need a subject that can
		// update it's node record on the CN.  either an MN or CN subject.

		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);

			try {
				Set<Node> cNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.CN);
				if (cNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test updateNodeCapabilities unless there is a node in the NodeList");
				} else {
					Node node = cNodeSet.iterator().next();
					node.setDescription(node.getDescription() + 
							" Tier2 updateNodeCapabilities_updatingOtherField test");
					NodeReference nodeRef = node.getIdentifier();
										
					Ping ping = node.getPing();
					Date orginalLastSuccess = ping.getLastSuccess();
					ping.setLastSuccess(new Date());
					node.setPing(ping);
					
					boolean response = cn.updateNodeCapabilities(null,nodeRef,node);
					handleFail(cn.getLatestRequestUrl(),"updateNodeCapabilities to update other fields should fail");
				} 
			}
			catch (InvalidRequest e) {
				// this is the expected behavior
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

	
	@Test
	public void testRegister() {
		// TODO: set the appropriate subject - will need a subject that can
		// create a node record.  

		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegister(...) vs. node: " + currentUrl);

			try {
				Set<Node> mNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.MN);
				if (mNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test cn.register() unless there is a Member Node in the NodeList");
				} else {
					Node node = mNodeSet.iterator().next();
					String nr = node.getIdentifier().getValue();
					NodeReference newRef = new NodeReference();
					newRef.setValue(nr + nr);
					node.setIdentifier(newRef);
					node.setBaseURL(node.getBaseURL() + "/fakeBaseUrlThatIsDifferent");
					NodeReference response = cn.register(null,node);
					checkTrue(cn.getLatestRequestUrl(),"register(...) returns a NodeReference object", response != null);
				}
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
	public void testRegister_IdentifierNotUnique() {
		// TODO: set the appropriate subject - will need a subject that can
		// create a node record.  

		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			printTestHeader("testRegister(...) vs. node: " + currentUrl);

			try {
				Set<Node> mNodeSet = NodelistUtil.selectNodes(cn.listNodes(), NodeType.MN);
				if (mNodeSet.isEmpty()) {
					handleFail(cn.getLatestRequestUrl(),"Cannot test cn.register() unless there is a Member Node in the NodeList");
				} else {
					Node node = mNodeSet.iterator().next();
					// attempt to re-register a node...
					NodeReference response = cn.register(null,node);
					handleFail(cn.getLatestRequestUrl(),"register(...) should throw IndentifierNotUnique exception when" +
							" trying to re-register a node (same Identifer)");
				}
			} 
			catch (IdentifierNotUnique e) {
				// this is expected outcome
			}
			catch (BaseException e) {
				handleFail(cn.getLatestRequestUrl(),"expected fail with InvalidRequest. Got: " + e.getClass() + 
					":: "+ e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}



}
