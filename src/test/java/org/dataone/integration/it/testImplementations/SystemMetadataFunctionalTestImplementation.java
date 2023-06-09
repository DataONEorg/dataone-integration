package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.dataone.client.RetryHandler;
import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * This class should prove whether the following series of events is functioning correctly:
 * 
 * <ol>
 *      <li>An object is created on an MN.</li>
 *      <li>The system metadata is changed on the MN. - we call MN.updateSystemMetadata(). This is a synchronous call.</li>
 *      <li>Under the hood, the MN will call CN.synchronize() to push the change out. This is a synchronous call but only tells us the request for synchronize was successful</li>
 *      <li>Under the hood, the CN REST will call CNStorage.updateSystemMetadata() to update the CN's sysmeta</li>
 *      <li>CN REST will also call MN.systemMetadataChanged() on notify them of new metadata.</li>
 *      <li>The replica MNs will then call getSystemMetadata() on the authoritative MN to update their copy.</li>
 * </ol>
 * 
 * Assumptions made:
 * </p>
 * <ul>
 *      <li>We have one or more working CNs in the environment</li>
 *      <li>We have two or more working v2 MNs in the environment</li>
 *      <li>At least one of the MNs has synchronization disabled 
 *      (this is so we can test if CN.synchronize() works on its own correctly, as opposed to the harbest sync task).</li>
 *      <li>The MNs/CNs are properly registered with each other.</li>
 *      <li>We know how long it takes the CN to sync MN data and how long it takes MNs to replicate authoritative sysmeta. 
 * </ul>
 * 
 * @author Andrei
 */
public class SystemMetadataFunctionalTestImplementation extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    private Node mnV2NoSync;
    private static final long SYNC_WAIT_MINUTES = 15;
    private static final long REPLICATION_WAIT_MINUTES = 45;    // on top of the sync wait
    
    @Before
    public void setup() {
        cnList = new ArrayList<Node>();
        mnList = new ArrayList<Node>();

        Iterator<Node> cnIter = getCoordinatingNodeIterator();
        if(cnIter != null)
            cnList = IteratorUtils.toList(cnIter);
        
        assertTrue("Test requires at least one CN to function!", cnList.size() > 0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        
        // check node list, add an MN to mnList if:
        //      it supports v2 calls
        //      it supports MNReplication
        try {
            for(Node n : cn.listNodes().getNodeList()) {
                if(n.getType() == NodeType.MN)
                    try {
                        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                        Node capabilities = mnCallAdapter.getCapabilities();    // also doubles as a v2 ping
                        
                        List<Service> serviceList = capabilities.getServices().getServiceList();
                        for (Service service : serviceList)
                            if(service.getName().equalsIgnoreCase("MNReplication") 
                                    && service.getAvailable()) {
                                mnList.add(n);
                                break;
                            }
                    }
                    catch (Exception e1) {
                        log.warn("MN failed V2 getCapabilities(), skipping : " + n.getIdentifier().getValue());
                    }
            }
        } catch (Exception e) {
            log.warn("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }

        log.info("v2 CNs available:                         " + cnList.size());
        log.info("v2 MNs available supporting replication:  " + mnList.size());
        
        for (Node n : cnList)
            log.info("CN:           " + n.getBaseURL());
        for (Node n : mnList)
            log.info("v2 MN:        " + n.getBaseURL());
        
        assertTrue("This test requires at least two v2 MNs that are tier 4 (support replication) to work.", mnList.size() >= 2);
        assertTrue("This test requires at least one CN to work.", cnList.size() >= 1);
        
        // we need to test against an MN that has synchronize disabled
        // so we can be sure that the MN->CN synchronize() call is working
        // instead of the harvesting sync job
        
        // do we already have an MN with sync turned off?
        Node mnNoSync = null;
        for (Node n : mnList) {
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
            Node capabilities = null;
            try {
                capabilities = mnCallAdapter.getCapabilities();
            } catch (NotImplemented | ServiceFailure | ClientSideException e) {
                log.warn("Unable to get MN capabilities. "
                        + "Error: " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
            }
            
            if (capabilities == null) {
                log.warn("MN returned NULL capabilities from getCapabilities(). MN: " 
                        + n.getBaseURL());
                continue;
            }
            boolean synchronize = capabilities.isSynchronize();
            if (!synchronize) {
                mnNoSync = n;
                break;
            }
        }
        
        if (mnNoSync != null) {
            mnV2NoSync = mnNoSync;
            return;
        }
        
        // no MN with sync turned off?
        // disable sync on an MN
        
        for (Node n : mnList) {
            MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
            Node capabilities = null;
            try {
                capabilities = mnCallAdapter.getCapabilities();
                capabilities.setSynchronize(false);
                org.dataone.service.types.v2.Node v2Capabilities = null;
                if (capabilities instanceof org.dataone.service.types.v2.Node)
                    v2Capabilities = (org.dataone.service.types.v2.Node) capabilities;
                else
                    v2Capabilities = TypeFactory.convertTypeFromType(capabilities, org.dataone.service.types.v2.Node.class);
                    
                cn.updateNodeCapabilities(null, v2Capabilities.getIdentifier(), v2Capabilities);
                mnV2NoSync = capabilities;
                break;
            } catch (Exception e) {
                log.error("Unable to disable synchronization on MN! : " + n.getBaseURL() + "\n"
                        + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        }
        
        assertTrue("Environment for test must have at least one v2 MN with synchronize disabled "
                + "(so we can test if CN.synchronize() works on its own correctly).", mnV2NoSync != null);
    }
    
    @After
    public void tearDown() {
        if(mnV2NoSync == null) {
            log.error("Cannot reenable sync on MN, no reference to MN with sync disabled is saved! Reenable in LDAP.");
            return;
        }
        
        Node cNode = cnList.get(0);
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cNode, "v2");
        
        try {
            NodeReference nodeRef = mnV2NoSync.getIdentifier();
            org.dataone.service.types.v2.Node currentCapabilities = cn.getNodeCapabilities(nodeRef);
            currentCapabilities.setSynchronize(true);
            cn.updateNodeCapabilities(null, nodeRef, currentCapabilities);
        } catch (Exception e) {
            throw new AssertionError("Unable to update MN capabilities to re-enable synchronization on: " + mnV2NoSync.getIdentifier().getValue() +
                    " Reenable in LDAP.");
        }
    }
    
    @Override
    protected String getTestDescription() {
        return "Tests the MN-triggered system metadata updating.";
    }

    @WebTestName("systemMetadataChanged: tests that changes made to system metadata by an MN are propegated")
    @WebTestDescription("This test needs to be run in an environment with two or more MNs, one of which must "
            + "have synchronization disabled (so we can test if CN.synchronize() works on its own correctly)."
            + "The test checks whether the following events on the MN trigger the correct changes: "
            + "Some metadata is created and changed on an MN, so when the CN sees it, it'll be a new object from its point of view."
            + "The MN calls CN.synchronize() to notify the CN to update its version of the object. (Asynchronous call.)"
            + "We then need to wait for the CN to synchronize."
            + "When that happens, the CN should update its own copy, then propegate the change to "
            + "the replica MNs. We check that the CN's as well as the other MNs' copies are up to date.")
    @Test
    public void testSystemMetadataChanged() {
        
        Identifier createdPid = null;
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        try {
            
            // create a test object
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            final Identifier pid = D1TypeBuilder.buildIdentifier("testSystemMetadataChanged_" + ExampleUtilities.generateIdentifier());
            SystemMetadata sysmeta = null;
            Date originalDateModified = null;
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
                Thread.sleep(10000);
                sysmeta = mn.getSystemMetadata(null, pid);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Unable to create a test object: " + pid.getValue());
                throw be;
            }
            originalDateModified = sysmeta.getDateSysMetadataModified();
            
            log.info("testSystemMetadataChanged:   "
                    + "created test object: " + createdPid.getValue());
            
            // modify the sysmeta
            
            log.info("testSystemMetadataChanged:   "
                    + "fetching sysmeta for pid " + createdPid.getValue() + " from auth MN");
                    
            sysmeta = mn.getSystemMetadata(null, createdPid);
            
            log.info("testSystemMetadataChanged:   "
                    + "original sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            log.info("testSystemMetadataChanged:   "
                    + "original sysmeta.dateSystemMetadataChanged: " + sysmeta.getDateSysMetadataModified());
            
            log.info("testSystemMetadataChanged:   "
                    + "updating sysmeta on auth MN");
                    
			boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Call to MN updateSystemMetadata failed: " + be.getMessage());
                throw be;
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSystemMetadataChanged:   "
                    + "updated sysmeta on auth MN successfully");
                    
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            Node cnNode = cnList.get(0);
            final CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

            // CN needs time to synchronize
            log.info("testSystemMetadataChanged:   "
                    + "polling the CN for the systemMetadata every 30 seconds while CN.synchronize() to run. " +
                    "(up to " + SYNC_WAIT_MINUTES + " minutes)");
            final Identifier pollingPid = createdPid;
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {

                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                       return cn.getSystemMetadata(null, pollingPid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
                
            };
            SystemMetadata fetchedCNSysmeta = handler.execute(30* 1000, SYNC_WAIT_MINUTES * 60 * 1000);
 
            log.info("testSystemMetadataChanged:   "
                    + "done waiting for CN.synchronize(), verifying CN has correct sysmeta");
            
            // verify that sysmeta fetched from CN is updated
            
            boolean dateModified = fetchedCNSysmeta.getDateSysMetadataModified().after(originalDateModified);
            assertTrue("testSystemMetadataChanged: sysmeta.dateSysMetadataModified should have been modified by the time it's synced to CN", dateModified );
            
            log.info("testSystemMetadataChanged:   "
                    + "CN sysmeta matches updates made on MN");
                    
            // TODO could also inspect the system metadata on the CN 
            //      for a replica in 'requested' status 
            //      and wait for it to move to 'completed' or 'failed'
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            log.info("testSystemMetadataChanged:   "
                    + "waiting for CN to trigger replication. polling the CN for the systemMetadata every 30 seconds until "
                    + "we have replica info in sysmeta. (waiting up to " + REPLICATION_WAIT_MINUTES + " minutes)");
            
//            final Identifier pollingPid2 = createdPid;
            RetryHandler<SystemMetadata> handler2 =  new RetryHandler<SystemMetadata>() {

                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cn.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained an empty replica list! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        Node replicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : mnList)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() ))
                                    replicaNode = v2Node;
                        }
                        if (replicaNode == null) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained no replica for a v2 MN! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        return sysmeta;
                       
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
                
            };
            fetchedCNSysmeta = handler2.execute(30* 1000, REPLICATION_WAIT_MINUTES * 60 * 1000);
            
            log.info("testSystemMetadataChanged:   "
                    + "done waiting for replication, fetching sysmeta for pid " + createdPid.getValue() + " from CN");

            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            log.info("testSystemMetadataChanged:   "
                    + "looking at MNs that have a replica");
                    
            // notify replica-holding MNs of the sysmeta change
            int replicasFound = 0;
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        log.info("testSystemMetadataChanged:   "
                                + "found replica MN: " + replicaNodeRef.getValue());
                        break;
                    }

                if (replicaHolderNode == null)
                    continue;   // may be a CN NodeRef, skip 
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "checking if replica MN holds updated sysmeta");
                        
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                replicasFound++;
            }
            
            assertTrue("Should have found at least one replica.", replicasFound > 0);
            
        } catch (Exception e) {
            log.error("Testing failed with exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.warn("Unable to delete test pid after running the test: " + createdPid.getValue(), e2);
            }
        }
    }
    
    
    @WebTestName("systemMetadataChanged: tests that changes made to system metadata by an MN are propegated when the object is not a new one")
    @WebTestDescription("This test needs to be run in an environment with two or more MNs, one of which must "
            + "have synchronization disabled (so we can test if CN.synchronize() works on its own correctly)."
            + "The test checks whether the following events on the MN trigger the correct changes: "
            + "Some metadata is created on an MN then waits for the CN to sync this data. The data is then modified on the MN."
            + "The MN calls CN.synchronize() to notify the CN to update its version of the object. (Asynchronous call.)"
            + "We then need to wait for the CN to synchronize."
            + "When that happens, the CN should update its own copy, then propegate the change to "
            + "the replica MNs. We check that the CN's as well as the other MNs' copies are up to date.")
    @Test
    public void testSystemMetadataChanged_ExistingObj() {
        
        Identifier createdPid = null;
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        Node cnNode = cnList.get(0);
        final CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

        try {
            
            // create a test object
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            final Identifier pid = D1TypeBuilder.buildIdentifier("testSystemMetadataChanged_ExistingObj_" + ExampleUtilities.generateIdentifier());
            SystemMetadata sysmeta = null;
            Date originalDateModified = null;
            
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
                Thread.sleep(10000);
                sysmeta = mn.getSystemMetadata(null, pid);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Unable to create a test object: " + pid.getValue());
                throw be;
            }
            originalDateModified = sysmeta.getDateSysMetadataModified();
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "created test object: " + createdPid.getValue());
            
            // wait for CN to synchronize
            //  - we're testing the case where the object already exists on the CN
            //    at the time when CN.synchronize() gets called
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN sync (up to " + SYNC_WAIT_MINUTES  + " minutes)");
            
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMetadata...");
                       return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            SystemMetadata fetchedCNSysmeta = handler.execute(30* 1000, SYNC_WAIT_MINUTES * 60 * 1000);
            assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta for pid " + pid.getValue() + " from " + cn.getLatestRequestUrl(), fetchedCNSysmeta != null);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "fetched sysmeta for pid " + createdPid.getValue() + " from CN successfully");
            
            // modify the sysmeta
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "fetching sysmeta for pid " + createdPid.getValue() + " from auth MN");
            
            sysmeta = mn.getSystemMetadata(null, createdPid);
            accessRule = APITestUtils.buildAccessRule(getSubject("testRightsHolder").getValue(), Permission.CHANGE_PERMISSION);
            AccessPolicy accessPolicy = new AccessPolicy();
            accessPolicy.addAllow(accessRule);
            sysmeta.setAccessPolicy(accessPolicy);

            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "new sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updating sysmeta on auth MN");
            
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Call to MN updateSystemMetadata failed: " + be.getMessage());
                throw be;
            }
            SystemMetadata mnSysmetaModMaybe = mn.getSystemMetadata(null, pid);
            log.info("after updSysmeta, date is: " + mnSysmetaModMaybe.getDateSysMetadataModified());
            
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "updated sysmeta on auth MN successfully");
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            // CN needs time to synchronize
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN.synchronize() to run (up to " + SYNC_WAIT_MINUTES + " minutes)");
            
            handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                       return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            fetchedCNSysmeta = handler.execute(30* 1000, SYNC_WAIT_MINUTES * 60 * 1000);
            assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta for pid " + pid.getValue() + " from " + cn.getLatestRequestUrl(), fetchedCNSysmeta != null);
            
            // verify that sysmeta fetched from CN is updated
            
            boolean dateModified = fetchedCNSysmeta.getDateSysMetadataModified().after(originalDateModified);
            assertTrue("testSystemMetadataChanged_ExistingObj: sysmeta.dateSysMetadataModified should have been modified by the time it's synced to CN", dateModified );
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "waiting for CN to trigger replication "
                    + "so we have replica info in sysmeta (up to " + REPLICATION_WAIT_MINUTES + " minutes)");
            
            handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cn.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained an empty replica list! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        Node replicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : mnList)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() ))
                                    replicaNode = v2Node;
                        }
                        if (replicaNode == null) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained no replica for a v2 MN! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        return sysmeta;
                        
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            fetchedCNSysmeta = handler.execute(30* 1000, REPLICATION_WAIT_MINUTES * 60 * 1000);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "done waiting for replication, fetched sysmeta for pid " + createdPid.getValue() + " from CN");
            
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("Replica list fetched from CN (after having time to replicate should not be null!", replicaList != null);
            assertTrue("System metadata fetched from CN should now have a non-empty replica list", replicaList.size() > 0);
            
            log.info("testSystemMetadataChanged_ExistingObj:   "
                    + "looking at MNs that have a replica");

            // check if replicas have updated sysmeta
            int replicasFound = 0;
            for (Replica replica : replicaList) {
                NodeReference replicaNodeRef = replica.getReplicaMemberNode();

                Node replicaHolderNode = null;
                for (Node n : mnList)
                    if (n.getIdentifier().getValue().equals(replicaNodeRef.getValue())) {
                        replicaHolderNode = n;
                        log.info("testSystemMetadataChanged_ExistingObj:   "
                                + "found replica MN: " + replicaNodeRef.getValue());
                        break;
                    }
                
                if (replicaHolderNode == null)
                    continue;   // may be a CN NodeRef, skip 
                
                CommonCallAdapter replicaHolderMN = new CommonCallAdapter(getSession("testRightsHolder"), replicaHolderNode, "v2");
                // it's not the MN's responsibility to update replica-holders 
                // (it's the CN's, as part of synchronize() and ensuing replication)
                // so here we just check if the replicas have the updated version of sysmeta 
                
                log.info("testSystemMetadataChanged_ExistingObj:   "
                        + "checking if replica MN holds updated sysmeta");
                
                SystemMetadata replicaSysmeta = replicaHolderMN.getSystemMetadata(null, createdPid);
                replicasFound++;
            }
            
            assertTrue("Should have found at least one replica.", replicasFound > 0);
            
        } catch (Exception e) {
            log.error("Testing failed with exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.warn("Unable to delete test pid after running the test: " + createdPid.getValue(), e2);
            }
        }
    }
    
    @WebTestName("setReplicationStatus: calling CN.setReplicationStatus() doesn't succeed regardless of status")
    @WebTestDescription("Calling setReplicaStatus with an unchanging status should not succeed - the update should "
            + "only be valid if updating the replica status to a new one.")
    @Test
    public void testSetReplicationStatus_NoChange() {
        
        // documentation says setReplicationStatus can be called by another CN,
        // using CN cert since no MN certs available ... hope it works ...
        Identifier createdPid = null;
        MNCallAdapter mn = new MNCallAdapter(getSession("testRightsHolder"), mnV2NoSync, "v2");
        Node cnNode = cnList.get(0);
        final CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cnNode, "v2");

        try {
            // create a test object
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "creating test object");
            
            AccessRule accessRule = APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.READ);
            ReplicationPolicy replPolicy = new ReplicationPolicy();
            replPolicy.setNumberReplicas(null);
            final Identifier pid = D1TypeBuilder.buildIdentifier("testSetReplicationStatus_NoChange" + ExampleUtilities.generateIdentifier());
            SystemMetadata sysmeta = null;
            Date originalDateModified = null;
            
            try {
                createdPid = createTestObject(mn, pid, accessRule, replPolicy);
                Thread.sleep(10000);
                sysmeta = mn.getSystemMetadata(null, createdPid);
                originalDateModified = sysmeta.getDateSysMetadataModified();
                sysmeta.getAccessPolicy().addAllow(APITestUtils.buildAccessRule(Constants.SUBJECT_PUBLIC + "-2", Permission.READ));
                mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Unable to create a test object: " + pid.getValue());
                throw be;
            }
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "created test object: " + createdPid.getValue());
            
            // wait for CN to synchronize
            
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMetadata...");
                       return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            SystemMetadata fetchedCNSysmeta = handler.execute(30* 1000, SYNC_WAIT_MINUTES * 60 * 1000);
            assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta for pid " + pid.getValue() + " from " + cn.getLatestRequestUrl(), fetchedCNSysmeta != null);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "fetched sysmeta for pid " + createdPid.getValue() + " from CN successfully");
            
            // modify the sysmeta
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "fetching sysmeta for pid " + createdPid.getValue() + " from auth MN");
            
            sysmeta = mn.getSystemMetadata(null, createdPid);
            log.info("testSetReplicationStatus_NoChange:   "
                    + "original sysmeta.dateSystemMetadataChanged: " + sysmeta.getDateSysMetadataModified());
            log.info("testSetReplicationStatus_NoChange:   "
                    + "original sysmeta.serialVersion      : " + sysmeta.getSerialVersion());
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "updating sysmeta on auth MN");
            
            boolean success = false;
            try {
                success = mn.updateSystemMetadata(null, createdPid, sysmeta);
            } catch (BaseException be) {
                handleFail(mn.getLatestRequestUrl(), "Call to MN updateSystemMetadata failed: " + be.getMessage());
                throw be;
            }
            assertTrue("MN should have modified its own system metadata successfully.", success);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "updated sysmeta on auth MN successfully");
            
            // MN.updateSystemMetadata() call should trigger a 
            // CN.synchronize() call under the hood
            
            // CN needs time to synchronize
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "waiting for CN.synchronize() to run (up to " + SYNC_WAIT_MINUTES + " minutes)");
            
            handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt()
                        throws TryAgainException, Exception {
                    
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                       return cn.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            fetchedCNSysmeta = handler.execute(30* 1000, SYNC_WAIT_MINUTES * 60 * 1000);
            assertTrue("cn.getSystemMetadata() should successfully fetch the sysmeta for pid " + pid.getValue() + " from " + cn.getLatestRequestUrl(), fetchedCNSysmeta != null);
            
            // verify that sysmeta fetched from CN is updated
            
            boolean dateModified = fetchedCNSysmeta.getDateSysMetadataModified().after(originalDateModified);
            assertTrue("testSetReplicationStatus_NoChange: sysmeta.dateSysMetadataModified should have been modified by the time it's synced to CN", dateModified );
            
            // CN needs to run replication in order for sysmeta to contain replica info
            // we need replica info in the sysmeta
            // so we can then verify that the sysmeta on the replica-holding MNs is updated
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "waiting for CN to trigger replication "
                    + "so we have replica info in sysmeta (up to " + REPLICATION_WAIT_MINUTES + " minutes)");
            
            handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cn.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained an empty replica list! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        Node replicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : mnList)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() ))
                                    replicaNode = v2Node;
                        }
                        if (replicaNode == null) {
                            TryAgainException f = new TryAgainException();
                            f.initCause(new NotFound("404", "CN sysmeta contained no replica for a v2 MN! " + cn.getLatestRequestUrl()));
                            throw f;
                        }
                        return sysmeta;
                        
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            fetchedCNSysmeta = handler.execute(30* 1000, REPLICATION_WAIT_MINUTES * 60 * 1000);
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "done waiting for replication, fetched sysmeta for pid " + createdPid.getValue() + " from CN");
            
            List<Replica> replicaList = fetchedCNSysmeta.getReplicaList();
            assertTrue("Test can't continue if replica list fetched from CN (after having time to replicate) is null!", replicaList != null);
            assertTrue("Test can't continue if system metadata fetched from CN does not have a non-empty replica list", replicaList.size() > 0);

            // request replication a bunch
            int statusUpdateFailures = 0; 
            for (int i = 0; i < 10; i++) {
                try {
                    log.info("testSetReplicationStatus_NoChange:   "
                            + "calling setReplicationStatus (iteration " + i + ")");
                    cn.setReplicationStatus(null, createdPid, mn.getNodeId(), ReplicationStatus.REQUESTED, new ServiceFailure("0101", "Setting replication status to INVALIDATED failed"));
                } catch (BaseException e) {
                    statusUpdateFailures++;
                    log.info("testSetReplicationStatus_NoChange:   "
                            + "status update failure (iteration " + i + ") " 
                            + e.getClass().getSimpleName() + " : " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            log.info("testSetReplicationStatus_NoChange:   "
                    + "status update failures: " + statusUpdateFailures);
            
            fetchedCNSysmeta = cn.getSystemMetadata(null, createdPid);
            BigInteger serialVersion = fetchedCNSysmeta.getSerialVersion();
            
            assertTrue("sysmeta should not have been updated if the ReplicationStatus is unchanged, "
                    + "and serialVersion should not have been incremented for each attempt", serialVersion.intValue() < 10);
            log.info("testSetReplicationStatus_NoChange:   "
                    + "status update failures: " + statusUpdateFailures);
            
        } catch (Exception e) {
            log.error("Testing failed with exception: " + e.getClass().getSimpleName() + " : " + e.getMessage());
        } finally {
            // TODO ideally, purge(pid)
            try {
                if(createdPid != null)     
                    mn.delete(null, createdPid);
            } catch (Exception e2) {
                log.warn("Unable to delete test pid after running the test: " + createdPid.getValue(), e2);
            }
        }
    }
}
