package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.time.DateUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
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
import org.dataone.service.util.Constants;

public class MNSystemMetadataMutabilityImplementations extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private CNCallAdapter cn;
    private List<Node> mns;
    private List<Node> v2mns;
    
    private static final long REPLICATION_WAIT = 5 * 60000;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs tests against mutability of fields in sysmeta.";
    }
    
    public void setup(Iterator<Node> cnIter) {
        List<Node> cnList = new ArrayList<Node>();
        List<Node> mnList = new ArrayList<Node>();
        mns = new ArrayList<Node>();
        v2mns = new ArrayList<Node>();
        
        cnList = IteratorUtils.toList(cnIter);
        
        if(cnList.size() > 0) {
            cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        }
        if(cn != null) {
            try {
                for(Node n : cn.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (Exception e) {
                throw new AssertionError("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
            }
        }
        
        for (Node mNode : mnList) {
            MNCallAdapter v1mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
            MNCallAdapter v2mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
            
            boolean v1support = false;
            boolean v2support = false;
            
            try {
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

            if (v1support || v2support)
                mns.add(mNode);
            if (v2support)
                v2mns.add(mNode);
        }

        log.info("MNs available:     " + mns.size());
        log.info("v2 MNs available:  " + v2mns.size());
        
        
        for (Node n : mns)
            log.info("MN:   " + n.getBaseURL());
        for (Node n : v2mns)
            log.info("v2 MN     :   " + n.getBaseURL());
        
        assertTrue("Tests require at least two total available MNs.", mns.size() > 0);
        assertTrue("Tests require at least one available v2 MN.", v2mns.size() > 0);
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
        
        Identifier pid = null;
        try {
            getSession(cnSubmitter);
            pid = D1TypeBuilder.buildIdentifier("testRegisterSystemMetadata_dateModified_obj1");
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            pid = procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
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
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e) {
            log.warn("wait for CN sync was interrupted");
        }
        
        try {
            sysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError("testRegisterSystemMetadata_dateModified: Unable to fetch sysmeta from CN " 
                    + cn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date cnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testRegisterSystemMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on creation.", dateUnchanged);
    }
    
    @WebTestName("setReplicationStatus date modified")
    @WebTestDescription("Tests whether the call to setReplicationStatus modifies "
            + "the dateSysMetadataModified.")
    public void testSetReplicationStatus_dateModified() {
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        Identifier pid = null;
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            pid = D1TypeBuilder.buildIdentifier("testRegisterSystemMetadata_dateModified_obj1");
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            pid = procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
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
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e) {
            log.error("testRegisterSystemMetadata_dateModified: wait for replication interrupted");
        }
        
//      ReplicationStatus.COMPLETED
//      ReplicationStatus.FAILED
//      ReplicationStatus.INVALIDATED
//      ReplicationStatus.QUEUED
//      ReplicationStatus.REQUESTED
        
        // verify that object has made it to CN
        try {
            cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        // setReplicationStatus call
        try {
            // expecting lots of state checks... changing to INVALIDATED seems like it should have the least (?) 
            cn.setReplicationStatus(null, pid, mNode.getIdentifier(), ReplicationStatus.INVALIDATED , null);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
                    + "to setReplicationStatus for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            sysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testSetReplicationStatus_dateModified: unable "
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
            + "the dateSysMetadataModified.")
    public void testUpdateReplicationMetadata_dateModified() {
        
        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        Identifier pid = null;
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            pid = D1TypeBuilder.buildIdentifier("testUpdateReplicationMetadata_dateModified_obj1");
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            pid = procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
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
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e) {
            log.error("testUpdateReplicationMetadata_dateModified: wait for replication interrupted");
        }
        
        // get a replica
        SystemMetadata cnSysmeta = null;
        Replica replica = null;
        try {
            cnSysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        Date cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        // make sure replication didn't update the sysmeta
        boolean dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testUpdateReplicationMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "during replication.", dateUnchanged);
        
        outerloop:
        for (Replica r : cnSysmeta.getReplicaList()) {
            NodeReference ref = r.getReplicaMemberNode();
            String refValue = ref.getValue();
            for (Node n : v2mns) {
                if (refValue.equals(n.getIdentifier().getValue())) {
                    replica = r;
                    break outerloop;
                }
            }
        }
        
        assertTrue("Should have found a replica for pid " + pid.getValue() + " in the sysmeta fetched from the CN ("
                + cn.getNodeBaseServiceUrl() + ") that is on one of the v2 MNs in the environment.", replica != null);
        
        // change something on the replica before updateReplicationMetadata
        if (replica.getReplicationStatus() != ReplicationStatus.INVALIDATED)
            replica.setReplicationStatus(ReplicationStatus.INVALIDATED);
        else 
            replica.setReplicationStatus(ReplicationStatus.FAILED);
        
        // updateReplicationMetadata call
        try {
            cn.updateReplicationMetadata(null, pid, replica, sysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to updateReplicationMetadata for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            cnSysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testUpdateReplicationMetadata_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testUpdateReplicationMetadata_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on setReplicationStatus.", dateUnchanged);
    }
    
    public void testDeleteReplicationMetacat_dateModified() {

        AccessRule publicAccessRule = new AccessRule();
        publicAccessRule.addSubject(D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC));
        publicAccessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(v2mns.size() > 1 ? v2mns.size() -1 : 2);
        
        Identifier pid = null;
        Node mNode = v2mns.get(0);
        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2"); 
        
        // create object on MN
        try {
            getSession(cnSubmitter);
            pid = D1TypeBuilder.buildIdentifier("testDeleteReplicationMetacat_dateModified_obj1");
            log.info("attempting to create test object on " + mn.getNodeBaseServiceUrl() + " with pid " + pid.getValue());
            pid = procureTestObject(mn,  publicAccessRule, pid, cnSubmitter, "public", replPolicy);
        } catch (Exception e) {
            throw new AssertionError("testDeleteReplicationMetacat_dateModified: Unable to get or create a "
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
            throw new AssertionError("testDeleteReplicationMetacat_dateModified: Unable to fetch sysmeta from MN "
                    + mn.getLatestRequestUrl() + " for pid " + pid.getValue(), e);
        }
        Date mnSysmetaDateModified = sysmeta.getDateSysMetadataModified();
        
        // need sysmeta to contain replica info first, so wait for replication
        try {
            Thread.sleep(REPLICATION_WAIT);
        } catch (InterruptedException e) {
            log.error("testDeleteReplicationMetacat_dateModified: wait for replication interrupted");
        }
        
        // get a replica
        SystemMetadata cnSysmeta = null;
        Replica replica = null;
        try {
            cnSysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testDeleteReplicationMetacat_dateModified: unable "
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
            for (Node n : v2mns) {
                if (refValue.equals(n.getIdentifier().getValue())) {
                    replica = r;
                    break outerloop;
                }
            }
        }
        
        assertTrue("Should have found a replica for pid " + pid.getValue() + " in the sysmeta fetched from the CN ("
                + cn.getNodeBaseServiceUrl() + ") that is on one of the v2 MNs in the environment.", replica != null);
        
        // deleteReplicationMetadata call
        try {
            cn.deleteReplicationMetadata(null, pid, replica.getReplicaMemberNode(), cnSysmeta.getSerialVersion().longValue());
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testDeleteReplicationMetacat_dateModified: unable "
                    + "to deleteReplicationMetadata for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }

        // getSystemMetadata and verify date unchanged?
        try {
            cnSysmeta = cn.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cn.getLatestRequestUrl() + " testDeleteReplicationMetacat_dateModified: unable "
                    + "to fetch sysmeta from CN for pid " + pid.getValue() + " Got exception: " 
                    + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        
        cnSysmetaDateModified = cnSysmeta.getDateSysMetadataModified();
        
        dateUnchanged = DateUtils.isSameInstant(mnSysmetaDateModified, cnSysmetaDateModified);
        assertTrue("testDeleteReplicationMetacat_dateModified: The CN should not be changing the dateSysMetadataModified "
                + "on deleteReplicationMetadata.", dateUnchanged);
    }

    /*
        entire list:
           k    CN.create       
           k    CN.updateSystemMetadata
           k    CN.setReplicationStatus
           k    CN.testDeleteReplicationMetacat
                CN.setReplicationMetadata
                CN.setReplicationPolicy
                CN.setAccessPolicy
                CN.setObsoletedBy
                CN.setArchive
                CN.setRightsHolder
           k    CN.registerSystemMetadata
     
     */
}
