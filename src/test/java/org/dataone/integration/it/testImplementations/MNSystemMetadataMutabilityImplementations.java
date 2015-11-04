package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.RetryHandler;
import org.dataone.client.RetryHandler.TryAgainException;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.NotFound;
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
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;

public class MNSystemMetadataMutabilityImplementations extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private CNCallAdapter cnV1;
    private CNCallAdapter cnV2;
    /** MNs supporting ONLY the V1 API */
    private List<Node> v1mns;
    /** MNs supporting the V2 API (might also support V1 API) */
    private List<Node> v2mns;
    /** MNs supporting BOTH the V1 & V2 APIs */
    private List<Node> v1v2mns;
    private int availableMNs = 0;
    
    private static final long SYNC_WAIT = 15 * 60000;
    private static final long REPLICATION_WAIT = 45 * 60000;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs tests against mutability of fields in sysmeta.";
    }
    
    public void setup(Iterator<Node> cnIter) {
        List<Node> cnList = new ArrayList<Node>();
        List<Node> mnList = new ArrayList<Node>();
        v1mns = new ArrayList<Node>();
        v2mns = new ArrayList<Node>();
        v1v2mns = new ArrayList<Node>();
        availableMNs = 0;
        
        cnList = IteratorUtils.toList(cnIter);
        
        if(cnList.size() > 0) {
            cnV1 = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v1");
            cnV2 = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        }
        if(cnV2 != null) {
            try {
                for(Node n : cnV2.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (Exception e) {
                throw new AssertionError("Unable to fetch node list from CN: " + cnV2.getNodeBaseServiceUrl(), e);
            }
        }
        
        for (Node mNode : mnList) {
            MNCallAdapter v1mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
            MNCallAdapter v2mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
            
            boolean v1support = false;
            boolean v2support = false;
            
            // TODO is there a more reliable way to check this?
            try {
                v1mn.ping();        // ping v1 endpoint
                List<Service> serviceList = v1mn.getCapabilities().getServices().getServiceList();
                for (Service service : serviceList) {
                    if ("MNReplication".equals(service.getName()) 
                            && "v1".equals(service.getVersion())
                            && service.getAvailable()) {
                        v1support = true;
                        break;
                    }
                }
            } catch (Exception e1) {
                log.info("Unable to assess v1 capabilities for MN : " + v1mn.getNodeBaseServiceUrl() 
                        + " : " + e1.getClass().getSimpleName() + " : " + e1.getMessage());
            }
            
            try {
                v2mn.ping();        // ping v2 endpoint
                List<Service> serviceList = v2mn.getCapabilities().getServices().getServiceList();
                for (Service service : serviceList) {
                    if ("MNReplication".equals(service.getName()) 
                            && "v2".equals(service.getVersion())
                            && service.getAvailable()) {
                        v2support = true;
                        break;
                    }
                }
                v2support = true;
            } catch (Exception e1) {
                log.info("Unable to assess v2 capabilities for MN: " + v2mn.getNodeBaseServiceUrl() 
                        + " : " + e1.getClass().getSimpleName() + " : " + e1.getMessage());
            }

            if (v1support && !v2support)
                v1mns.add(mNode);
            if (v2support)
                v2mns.add(mNode);
            if (v1support && v2support)
                v1v2mns.add(mNode);
            if (v1support || v2support)
                availableMNs++;
        }

        log.info("v1-ONLY MNs available:     " + v1mns.size());
        log.info("v2 MNs available:          " + v2mns.size());
        log.info("v1 & v2 MNs available:     " + v1v2mns.size());
        log.info("available MNs:             " + availableMNs);
        
        for (Node n : v1mns)
            log.info("v1-ONLY MN:   " + n.getBaseURL());
        for (Node n : v2mns)
            log.info("v2 MN     :   " + n.getBaseURL());
        for (Node n : v1v2mns)
            log.info("v1 & v2 MN:   " + n.getBaseURL());
    }
    
    @WebTestName("create / registerSystemMetadata date modified")
    @WebTestDescription("Tests whether the call to registerSystemMetadata modifies "
            + "the dateSysMetadataModified. Since registerSystemMetadata is a call "
            + "internal to the CN, that happens as part of the create process, this "
            + "test runs create to test registerSystemMetadata.")
    public void testRegisterSystemMetadata_dateModified() {
        
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), v2mns.get(0), "v2");
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testRegisterSystemMetadata_dateModified_obj5");
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }
        
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        try {
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cnV2.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            sysmeta = cnGetSysmetaHandler.execute(30*1000, SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to fetch sysmeta from CN " 
                    + cnV2.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date cnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testRegisterSystemMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on creation.", dateUnchanged);
    }
    
    @WebTestName("setReplicationStatus date modified")
    @WebTestDescription("Tests whether the call to setReplicationStatus modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testSetReplicationStatus_dateModified() {
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testRegisterSystemMetadata_dateModified_obj5");
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        // need sysmeta to contain replica info first, so wait for replication
        final Node authNode = mNode;
        SystemMetadata cnSysmeta = null;
        try {
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cnV2.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0)
                            throw new TryAgainException();
                        
                        Node v2ReplicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : v2mns)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue())
                                        && !rep.getReplicaMemberNode().getValue().equals(authNode.getIdentifier().getValue()))
                                    v2ReplicaNode = v2Node;
                        }
                        if (v2ReplicaNode == null)
                            throw new TryAgainException();
                        
                        return sysmeta;
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = handler.execute(30* 1000, REPLICATION_WAIT);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
                    + "to fetch sysmeta with valid replicas from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
//      ReplicationStatus.COMPLETED
//      ReplicationStatus.FAILED
//      ReplicationStatus.INVALIDATED
//      ReplicationStatus.QUEUED
//      ReplicationStatus.REQUESTED
        
        // grab replica for current MN 
        Replica authMnReplica = null;
        for (Replica replica : cnSysmeta.getReplicaList()) {
            if (replica.getReplicaMemberNode().equals(mNode.getIdentifier())) {
                authMnReplica = replica;
                break;
            }
        }
        
        assertTrue("testSetReplicationStatus_dateModified unable to grab replica for current MN", authMnReplica != null);
        
        NodeReference authMnReplicaRef = authMnReplica.getReplicaMemberNode();
        ReplicationStatus repStatus = ReplicationStatus.INVALIDATED;
        if (authMnReplica.getReplicationStatus() == ReplicationStatus.INVALIDATED)
            repStatus = ReplicationStatus.FAILED;
           
        // setReplicationStatus call
        try {
            // expecting lots of state checks... changing to INVALIDATED seems like it should have the least (?) 
            cnV2.setReplicationStatus(null, pid, authMnReplicaRef, repStatus , null);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
                    + "to setReplicationStatus for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            sysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
                    + "to fetch sysmeta for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        Date cnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testSetReplicationStatus_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setReplicationStatus.", dateUnchanged);
    }
    
    @WebTestName("updateReplicationMetadata date modified")
    @WebTestDescription("Tests whether the call to updateReplicationMetadata modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testUpdateReplicationMetadata_dateModified() {
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testUpdateReplicationMetadata_dateModified_obj5");
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testUpdateReplicationMetadata_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testUpdateReplicationMetadata_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        // need sysmeta to contain replica info first, so wait for replication
        SystemMetadata cnSysmeta = null;
        try {
            final Node authNode = mNode;
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cnV2.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0)
                            throw new TryAgainException();
                        
                        Node v2ReplicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : v2mns)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue())
                                        && !rep.getReplicaMemberNode().getValue().equals(authNode.getIdentifier().getValue()))
                                    v2ReplicaNode = v2Node;
                        }
                        if (v2ReplicaNode == null)
                            throw new TryAgainException();
                        
                        return sysmeta;
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = handler.execute(30* 1000, REPLICATION_WAIT);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta with valid replicas from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        // make sure replication didn't update the sysmeta
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testUpdateReplicationMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "during replication.", dateUnchanged);

        // get a replica
        Replica replica = null;
        outerloop:
        for (Replica r : cnSysmeta.getReplicaList()) {
            NodeReference ref = r.getReplicaMemberNode();
            String refValue = ref.getValue();
            for (Node n : v2mns) {
                if (refValue.equals(n.getIdentifier().getValue())
                        && !r.getReplicaMemberNode().getValue().equals(mNode.getIdentifier().getValue())) {
                    replica = r;
                    break outerloop;
                }
            }
        }
        
        assertTrue("Should have found a replica for pid " + pid.getValue() + " in the sysmeta fetched from the CN ("
                + cnV2.getNodeBaseServiceUrl() + ") that is on one of the v2 MNs in the environment.", replica != null);
        
        // change something on the replica before updateReplicationMetadata
        if (replica.getReplicationStatus() != ReplicationStatus.INVALIDATED)
            replica.setReplicationStatus(ReplicationStatus.INVALIDATED);
        else 
            replica.setReplicationStatus(ReplicationStatus.FAILED);
        
        // updateReplicationMetadata call
        try {
            cnV2.updateReplicationMetadata(null, pid, replica, cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to updateReplicationMetadata for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testUpdateReplicationMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setReplicationStatus.", dateUnchanged);
    }
    
    @WebTestName("deleteReplicationMetadata date modified")
    @WebTestDescription("Tests whether the call to deleteReplicationMetadata modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testDeleteReplicationMetadata_dateModified() {

        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testDeleteReplicationMetadata_dateModified_" + ExampleUtilities.generateIdentifier());
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testDeleteReplicationMetadata_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testDeleteReplicationMetadata_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        // need sysmeta to contain replica info first, so wait for replication
        SystemMetadata cnSysmeta = null;
        try {
            final Node authNode = mNode;
            RetryHandler<SystemMetadata> handler =  new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        SystemMetadata sysmeta = cnV2.getSystemMetadata(null, pid);
                        
                        log.info("attempting to get replicas from CN sysmeta...");
                        List<Replica> replicaList = sysmeta.getReplicaList();
                        if (replicaList.size() == 0)
                            throw new TryAgainException();
                        
                        Node v2ReplicaNode = null;
                        for (Replica rep : replicaList) {
                            for (Node v2Node : v2mns)
                                if (v2Node.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue())
                                        && !rep.getReplicaMemberNode().getValue().equals(authNode.getIdentifier().getValue())
                                        && rep.getReplicationStatus() == ReplicationStatus.COMPLETED)
                                    v2ReplicaNode = v2Node;
                        }
                        if (v2ReplicaNode == null)
                            throw new TryAgainException();
                        
                        return sysmeta;
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = handler.execute(30* 1000, REPLICATION_WAIT);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testDeleteReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta with valid replicas from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }        
        // get a replica
        Replica replica = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testDeleteReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        // make sure replication didn't update the sysmeta
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testDeleteReplicationMetacat_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "during replication.", dateUnchanged);
        
        outerloop:
        for (Replica r : cnSysmeta.getReplicaList()) {
            NodeReference ref = r.getReplicaMemberNode();
            String refValue = ref.getValue();
            for (Node n : v1v2mns) {
                log.info("comparing sysmeta replica id (" + refValue + ") with available MN id (" + n.getIdentifier().getValue() + ")");
                if (refValue.equals(n.getIdentifier().getValue())
                        && !r.getReplicaMemberNode().getValue().equals(mNode.getIdentifier().getValue())
                        && r.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                    replica = r;
                    break outerloop;
                }
            }
        }
        
        assertTrue("Should have found a replica for pid " + pid.getValue() + " in the sysmeta fetched from the CN ("
                + cnV2.getNodeBaseServiceUrl() + ") that is on one of the v2 MNs in the environment.", replica != null);
        
        // deleteReplicationMetadata call
        try {
            cnV2.deleteReplicationMetadata(null, pid, replica.getReplicaMemberNode(), cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testDeleteReplicationMetadata_dateModified: unable "
                    + "to deleteReplicationMetadata for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testDeleteReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testDeleteReplicationMetacat_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on deleteReplicationMetadata.", dateUnchanged);
    }

    @WebTestName("setReplicationPolicy date modified")
    @WebTestDescription("Tests whether the call to setReplicationPolicy modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testSetReplicationPolicy_dateModified() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(availableMNs-1);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // create v1 object on MN
        final Identifier pid = D1TypeBuilder.buildIdentifier("testSetReplicationPolicy_dateModified_obj5");
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testSetReplicationPolicy_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testSetReplicationPolicy_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        
        SystemMetadata cnSysmeta = null;
        try {
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cnV1.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = cnGetSysmetaHandler.execute(30*1000, SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testSetReplicationPolicy_dateModified: Unable to fetch sysmeta from CN " 
                    + cnV1.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        // setReplicationPolicy call
        try {
            replPolicy.setReplicationAllowed(false);
            cnV1.setReplicationPolicy(null, pid, replPolicy, cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetReplicationPolicy_dateModified: unable "
                    + "to setReplicationPolicy for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged
        try {
            cnSysmeta = cnV1.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetReplicationPolicy_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testSetReplicationPolicy_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setReplicationPolicy.", dateUnchanged);
    }
    
    @WebTestName("setAccessPolicy date modified")
    @WebTestDescription("Tests whether the call to setAccessPolicy modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testSetAccessPolicy_dateModified() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        
        getSession("testRightsHolder");
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(availableMNs-1);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // create v1 object on MN
        final Identifier pid = D1TypeBuilder.buildIdentifier("testSetAccessPolicy_dateModified_obj5");
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testSetAccessPolicy_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testSetAccessPolicy_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        SystemMetadata cnSysmeta = null;
        try {
            
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cnV1.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = cnGetSysmetaHandler.execute(30*1000, SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testSetAccessPolicy_dateModified: Unable to fetch sysmeta from CN " 
                    + cnV1.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testSetAccessPolicy_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on sync.", dateUnchanged);
        
        // setAccessPolicy call
        
        // we'll switch back and forth between public / testRightsHolder
        boolean rightsHolderAccess = false;
        AccessPolicy cnAccessPolicy = cnSysmeta.getAccessPolicy();
        for(AccessRule rule : cnAccessPolicy.getAllowList()) {
            if (rule.getSubject(0).equals(getSubject("testRightsHolder")))
               rightsHolderAccess = true;
        }
        AccessPolicy newAccessPolicy = new AccessPolicy();
        if (rightsHolderAccess)
            newAccessPolicy.addAllow(D1TypeBuilder.buildAccessRule(Constants.SUBJECT_PUBLIC, Permission.CHANGE_PERMISSION));
        else
            newAccessPolicy.addAllow(D1TypeBuilder.buildAccessRule(getSubject("testRightsHolder").getValue(), Permission.CHANGE_PERMISSION));
        
        try {
            cnV1.setAccessPolicy(null, pid, newAccessPolicy, cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetAccessPolicy_dateModified: unable "
                    + "to setAccessPolicy for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged
        try {
            cnSysmeta = cnV1.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetAccessPolicy_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testSetAccessPolicy_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setAccessPolicy.", dateUnchanged);
    }
    
    @WebTestName("setRightsHolder date modified")
    @WebTestDescription("Tests whether the call to setRightsHolder modifies "
            + "the sysmeta.dateSysMetadataModified field. It shouldn't.")
    public void testSetRightsHolder_dateModified() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1mns.size() >= 1);
        
        getSession("testPerson");
        getSession("testRightsHolder");
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(availableMNs-1);
        
        Node v1MNode = v1mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");

        final Identifier pid = D1TypeBuilder.buildIdentifier("testSetRightsHolder_dateModified_obj5");
        // create v1 object on MN
        try {
            getSession(cnSubmitter);
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testSetRightsHolder_dateModified: Unable to get or create a "
                    + "test object with pid: " + pid.getValue(), e);
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("wait for metacat indexing was interrupted");
        }

        // get sysmeta date from MN
        SystemMetadata sysmeta = null;
        try {
            sysmeta = mn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testSetRightsHolder_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        SystemMetadata cnSysmeta = null;
        try {
            RetryHandler<SystemMetadata> cnGetSysmetaHandler = new RetryHandler<SystemMetadata>() {
                @Override
                protected SystemMetadata attempt() throws TryAgainException, Exception {
                    try {
                        log.info("attempting CN getSystemMEtadata...");
                        return cnV1.getSystemMetadata(null, pid);
                    } catch (NotFound | ServiceFailure e) {
                        TryAgainException f = new TryAgainException();
                        f.initCause(e);
                        throw f;
                    }
                }
            };
            cnSysmeta = cnGetSysmetaHandler.execute(30*1000, SYNC_WAIT);
        } catch (Exception e) {
            throw new AssertionError("testSetRightsHolder_dateModified: Unable to fetch sysmeta from CN " 
                    + cnV1.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        // setRightsHolder call
        
        // we'll switch back and forth between testPerson / testRightsHolder as rightsHolder
        boolean testPersonRH = false;
        if (cnSysmeta.getRightsHolder().equals(getSubject("testPerson")))
           testPersonRH = true;
        
        Subject newRightsHolder = null;
        if (testPersonRH)
            newRightsHolder = getSubject("testRightsHolder");
        else
            newRightsHolder = getSubject("testPerson");
        
        try {
            cnV1.setRightsHolder(null, pid, newRightsHolder, cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetRightsHolder_dateModified: unable "
                    + "to setRightsHolder for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged
        try {
            cnSysmeta = cnV1.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV1.getLatestRequestUrl() + " testSetRightsHolder_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testSetRightsHolder_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setRightsHolder.", dateUnchanged);
    }
}
