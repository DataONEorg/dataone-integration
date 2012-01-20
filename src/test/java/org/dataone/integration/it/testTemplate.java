package org.dataone.integration.it;
public class testTemplate {
//	@Test
//	public void testGetNodeBaseServiceUrl() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testGetNodeBaseServiceUrl(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				String response = cn.getNodeBaseServiceUrl();
//				checkTrue(currentUrl,"getNodeBaseServiceUrl(...) returns a String object", response != null);
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
//	public void testLookupNodeBaseUrl() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testLookupNodeBaseUrl(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				String response = cn.lookupNodeBaseUrl();
//				checkTrue(currentUrl,"lookupNodeBaseUrl(...) returns a String object", response != null);
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
//	public void testLookupNodeId() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testLookupNodeId(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				String response = cn.lookupNodeId();
//				checkTrue(currentUrl,"lookupNodeId(...) returns a String object", response != null);
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
//
//
//

//
//
//	@Test
//	public void testSetRightsHolder() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSetRightsHolder(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				Identifier response = cn.setRightsHolder();
//				checkTrue(currentUrl,"setRightsHolder(...) returns a Identifier object", response != null);
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
//	public void testIsAuthorized() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testIsAuthorized(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.isAuthorized();
//				checkTrue(currentUrl,"isAuthorized(...) returns a boolean object", response != null);
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
//	public void testSetAccessPolicy() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSetAccessPolicy(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.setAccessPolicy();
//				checkTrue(currentUrl,"setAccessPolicy(...) returns a boolean object", response != null);
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
//	public void testRegisterAccount() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRegisterAccount(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				Subject response = cn.registerAccount();
//				checkTrue(currentUrl,"registerAccount(...) returns a Subject object", response != null);
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
//	public void testUpdateAccount() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testUpdateAccount(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				Subject response = cn.updateAccount();
//				checkTrue(currentUrl,"updateAccount(...) returns a Subject object", response != null);
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
//	public void testVerifyAccount() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testVerifyAccount(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.verifyAccount();
//				checkTrue(currentUrl,"verifyAccount(...) returns a boolean object", response != null);
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
//	@Test
//	public void testUpdateNodeCapabilities() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testUpdateNodeCapabilities(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.updateNodeCapabilities();
//				checkTrue(currentUrl,"updateNodeCapabilities(...) returns a boolean object", response != null);
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
//	public void testRegister() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testRegister(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				NodeReference response = cn.register();
//				checkTrue(currentUrl,"register(...) returns a NodeReference object", response != null);
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
//	public void testSetReplicationStatus() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSetReplicationStatus(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.setReplicationStatus();
//				checkTrue(currentUrl,"setReplicationStatus(...) returns a boolean object", response != null);
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
//	public void testSetReplicationPolicy() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSetReplicationPolicy(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.setReplicationPolicy();
//				checkTrue(currentUrl,"setReplicationPolicy(...) returns a boolean object", response != null);
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
//	public void testIsNodeAuthorized() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testIsNodeAuthorized(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.isNodeAuthorized();
//				checkTrue(currentUrl,"isNodeAuthorized(...) returns a boolean object", response != null);
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
//	public void testUpdateReplicationMetadata() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
//			CNode cn = new CNode(currentUrl);
//			printTestHeader("testUpdateReplicationMetadata(...) vs. node: " + currentUrl);
//
//			try {
//				ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
//				log.debug("   pid = " + oi.getIdentifier());
//
//				boolean response = cn.updateReplicationMetadata();
//				checkTrue(currentUrl,"updateReplicationMetadata(...) returns a boolean object", response != null);
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
}