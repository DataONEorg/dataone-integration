package org.dataone.integration.it.apiTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dataone.client.exception.ClientSideException;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.VersionMismatch;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v2.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

public class SidCNIT extends SidCommonIT {

    private Logger logger = Logger.getLogger(SidCNIT.class);
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods for CNs that accept SID parameters";
    }
    
    @Override
    protected Iterator<Node> getNodeIterator() {
        return getCoordinatingNodeIterator();
    }
    
    /**
     * Sets up each pid chain scenario, then calls resolve() with the SID and the head PID.
     * The resulting {@link ObjectLocationList}s should both hold the same resolve URLs.
     */
    @Test
    public void testResolve() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException {

        logger.info("Testing resolve() method ... ");
        
        for (int caseNum = 1; caseNum <= numCases; caseNum++) {
            
            logger.info("Testing resolve(), case " + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                ObjectLocationList sidLocationList = callAdapter.resolve(null, sid);
                ObjectLocationList pidLocationList = callAdapter.resolve(null, pid);
                
                if(sidLocationList.getObjectLocationList().size() == 0)
                    assertTrue("resolve() Case " + caseNum + ", resolve() on SID should yield non-empty location list", 
                            false);
                if(pidLocationList.getObjectLocationList().size() == 0)
                    assertTrue("resolve() Case " + caseNum + ", resolve() on head PID should yield non-empty location list", 
                            false);
                
                ObjectLocation sidLoc = sidLocationList.getObjectLocation(0);
                ObjectLocation pidLoc = pidLocationList.getObjectLocation(0);
                
                String sidResolveURL = sidLoc.getUrl();
                String pidResolveURL = pidLoc.getUrl();
                assertEquals("resolve() Case " + caseNum + ", SID and head PID should resolve() to same URL",
                        sidResolveURL, pidResolveURL);
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setRightsHolder() with the SID.
     * This should update the rights holder of the head PID.
     * So we get the rights holder on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @Test
    public void testSetRightsHolder() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest, VersionMismatch {

        logger.info("Testing setRightsHolder() method ... ");
        
        for (int caseNum = 1; caseNum <= numCases; caseNum++) {
            
            logger.info("Testing setRightsHolder(), case " + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                Subject sidRightsHolder = getSubject("testRightsHolder");
                callAdapter.setRightsHolder(null, sid, sidRightsHolder, 1);
                
                SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                Subject pidRightsHolder = sysmeta.getRightsHolder();
                
                assertTrue("setRightsHolder() Case " + caseNum, sidRightsHolder.getValue().equals(pidRightsHolder.getValue()));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setAccessPolicy() with the SID.
     * This should update the access policy of the head PID.
     * So we get the access policy on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @Test
    public void testSetAccessPolicy() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest, VersionMismatch {

        logger.info("Testing setAccessPolicy() method ... ");
        
        for (int caseNum = 1; caseNum <= numCases; caseNum++) {
            
            logger.info("Testing setAccessPolicy(), case " + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                AccessPolicy sidAccessPolicy = new AccessPolicy();
                Subject cnSubject = getSubject(subjectLabel);
                AccessRule accessRuleCN = AccessUtil.createAccessRule(
                        new Subject[] { cnSubject }, new Permission[] { Permission.CHANGE_PERMISSION });
                String testSubject = "BLARG";
                AccessRule accessRuleBlarg = AccessUtil.createAccessRule(
                        new String[] { testSubject }, new Permission[] { Permission.READ });
                sidAccessPolicy.addAllow(accessRuleCN);
                sidAccessPolicy.addAllow(accessRuleBlarg);
                
                callAdapter.setAccessPolicy(null, sid, sidAccessPolicy, 1);
                
                SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                AccessPolicy pidAccessPolicy = sysmeta.getAccessPolicy();
                
                assertTrue("setAccessPolicy() Case " + caseNum + " : allow list size", 
                        pidAccessPolicy.getAllowList().size() == 2);
                AccessRule fetchedAccessRuleCN = pidAccessPolicy.getAllowList().get(0);
                AccessRule fetchedAccessRuleBlarg = pidAccessPolicy.getAllowList().get(1);
                assertTrue("setAccessPolicy() Case " + caseNum + " : CN access rule", 
                        fetchedAccessRuleCN.getSubject(0).getValue().equals(cnSubject.getValue()));
                assertTrue("setAccessPolicy() Case " + caseNum + " : test access rule",
                        fetchedAccessRuleBlarg.getSubject(1).getValue().equals(testSubject));
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario, then calls setAccessPolicy() with the SID.
     * This should update the access policy of the head PID.
     * So we get the access policy on the head PID and assert it's equal to the 
     * one we just set to the SID.
     */
    @Test
    @Ignore("According to \"Mutability of Content\" page, only supposed to work for PIDS. v2 API disagrees though...")
    public void testSetReplicationPolicy() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest, VersionMismatch {

        logger.info("Testing setReplicationPolicy() method ... ");
        
        for (int caseNum = 1; caseNum <= numCases; caseNum++) {
            
            logger.info("Testing setReplicationPolicy(), case " + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                ReplicationPolicy policy = new ReplicationPolicy();
                NodeReference nodeRef = new NodeReference();
                String testNodeRef = "BLARG";
                nodeRef.setValue(testNodeRef);
                policy.addBlockedMemberNode(nodeRef);
                callAdapter.setReplicationPolicy(null, sid, policy, 1);
                
                SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, pid);
                ReplicationPolicy fetchedPolicy = sysmeta.getReplicationPolicy();
                
                assertTrue("setReplicationPolicy() Case " + caseNum + " : blocked nodes size", 
                        fetchedPolicy.getBlockedMemberNodeList().size() == 1);
                
                NodeReference fetchedNodeRef = fetchedPolicy.getBlockedMemberNodeList().get(0);
                
                assertTrue("setReplicationPolicy() Case " + caseNum + " : test node ref",
                        fetchedNodeRef.getValue().equals(testNodeRef));
            }
        }
    }
    
    /**
     * A more thorough CN test for archive(); overrides {@link SidCommonIT#testArchive()}.
     * Sets up each pid chain scenario, then calls archive() with the SID.
     * After this, the head PID should still be resolvable, but not show up in searches.
     * So we do a solr query and assert that it returns no reults.
     */
    @Test
    @Override
    public void testArchive() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure,
            NotFound, ClientSideException, InsufficientResources, IOException, InvalidRequest {

        logger.info("Testing archive() method ... ");
        
        for (int caseNum = 1; caseNum <= numCases; caseNum++) {
            
            logger.info("Testing archive(), case " + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            while (cnIter.hasNext()) {
                Node node = cnIter.next();
                CNCallAdapter callAdapter = new CNCallAdapter(getSession(subjectLabel), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                callAdapter.archive(null, sid);
                
                // test it's actually archived
                SystemMetadata sysmeta = null;
                try {
                    sysmeta = callAdapter.getSystemMetadata(null, pid);
                } catch (NotFound e) {
                    assertTrue("archive() Case " + caseNum + ", should be able to getSystemMetadata() for an archived object", 
                            false);
                }
                assertTrue("archive() Case " + caseNum + ", object should be archived", 
                        sysmeta.getArchived());
                
                // test search
                ObjectList objectList = callAdapter.search(null, QUERYTYPE_SOLR, "?q=identifier:" + pid);
                assertTrue("archive() Case " + caseNum + " search() for archived object shouldn't return results",
                        objectList.getObjectInfoList().size() == 0);

                // test resolve()-able
                ObjectLocationList locationList = null;
                try {
                    locationList = callAdapter.resolve(null, sid);
                } catch (NotFound e) {
                    assertTrue("archive() Case " + caseNum + ", should be able to resolve() an archived object", 
                            false);
                }
                
                assertTrue("archive() Case " + caseNum + ", should be able to resolve() an archived object to a location list", 
                        locationList != null && locationList.getObjectLocationList().size() > 0);
            }
        }
    }
    
}
