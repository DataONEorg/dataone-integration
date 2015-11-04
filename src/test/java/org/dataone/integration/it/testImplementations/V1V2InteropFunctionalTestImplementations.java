package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class V1V2InteropFunctionalTestImplementations extends ContextAwareTestCaseDataone {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private static CNCallAdapter cnV1;
    private static CNCallAdapter cnV2;
    /** MNs supporting ONLY the V1 API */
    private static List<Node> v1MNs;
    /** MNs supporting the V2 API (might also support V1 API) */
    private static List<Node> v2MNs;
    /** MNs supporting BOTH the V1 & V2 APIs */
    private static List<Node> v1v2MNs;
    /** MNs supporting EITHER the V1 or V2 API */
    private static List<Node> allMNs;
    
    private static String pidTimestamp = null;
    private static boolean setupDone = false;
    private static ArrayList<String> syncTestPids;
    private static ArrayList<String> replTestPids;
    private static final long MAX_SYNC_MINUTES = 10;
    private static final long MAX_REPL_MINUTES = 30;
    private static final long POLLING_SECONDS = 20;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs various V1 and V2 methods in conjunction with each other, "
                + "checking for invalid interoperation.";
    }
    
    public void setup(Iterator<Node> cnIter) {
        if (setupDone)
            return;
        
        log.info("SETUP: finding available nodes...");
        setupNodes(cnIter);
        
        log.info("SETUP: creating test objects...");
        setupTestObjects();
        
        log.info("SETUP: waiting for sync and replication...");
        waitForReplication();
        
        setupDone = true;
        log.info("SETUP: finished");
    }
    
    private void setupNodes(Iterator<Node> cnIter) {
        
        List<Node> cnList = new ArrayList<Node>();
        List<Node> mnList = new ArrayList<Node>();
        v1MNs = new ArrayList<Node>();
        v2MNs = new ArrayList<Node>();
        v1v2MNs = new ArrayList<Node>();
        allMNs = new ArrayList<Node>();
        
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
                v1MNs.add(mNode);
            if (v2support)
                v2MNs.add(mNode);
            if (v1support && v2support)
                v1v2MNs.add(mNode);
            if (v1support || v2support) {
                allMNs.add(mNode);
            }
        }

        log.info("v1-ONLY MNs available:     " + v1MNs.size());
        log.info("v2 MNs available:          " + v2MNs.size());
        log.info("v1 & v2 MNs available:     " + v1v2MNs.size());
        log.info("available MNs:             " + allMNs.size());
        
        for (Node n : v1MNs)
            log.info("v1-ONLY MN:   " + n.getBaseURL());
        for (Node n : v2MNs)
            log.info("v2 MN     :   " + n.getBaseURL());
        for (Node n : v1v2MNs)
            log.info("v1 & v2 MN:   " + n.getBaseURL());
    }
    
    private void setupTestObjects() {
        
        pidTimestamp = ExampleUtilities.generateIdentifier();
        syncTestPids = new ArrayList<String>();
        replTestPids = new ArrayList<String>();
        
        try {
            setupV2CreateV1UpdateSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2UpdateSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1GetSysMeta();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1GetSysmetaSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2Get();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1GetSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2GetSysmeta();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2GetSysmetaSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2Query();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1Query();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1DeleteSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2DeleteSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1Delete();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2Delete();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1ListObjectsSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2ListObjectsSameNode();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateV1ListObjects();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV1CreateV2ListObjects();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateCnArchive();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateCnSetReplicationPolicy();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateCnSetAccessPolicy();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateCnSetRightsHolder();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        try {
            setupV2CreateCnSetObsoletedBy();
        } catch (Exception e) {
            log.error("setup step / object creation failed! " + e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
    }
    
    private void waitForReplication() {
        
        log.info("waiting for CN sync task to run...");
        
        // wait a bit for sync task to run 
        // before we start trying to poll the sysmeta repeatedly
        final long INITIAL_WAIT_MINUTES = (MAX_SYNC_MINUTES < 2) ? 0 : 2; 
        try {
            Thread.sleep(INITIAL_WAIT_MINUTES * 60000);    
        } catch (InterruptedException e) {
            // ignore
        }
        
        log.info("waiting for CN sync to get test objects...");
        
        // all syncTestPids should get sync'ed
        
        HashSet<String> pidsNotSyncedCurrent = new HashSet<String>(syncTestPids);
        HashSet<String> pidsNotSyncedNext = new HashSet<String>(syncTestPids);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + ((MAX_SYNC_MINUTES-INITIAL_WAIT_MINUTES) * 60000);

        long interval = POLLING_SECONDS * 1000;
        log.info("will check for sync'ed pids till " + (new Date(endTime)));
        
        while (System.currentTimeMillis() < endTime) {
            if (pidsNotSyncedCurrent.size() == 0)
                break;
            
            for (String pidValue : pidsNotSyncedCurrent) {
                Identifier pid = D1TypeBuilder.buildIdentifier(pidValue);
                try {
                    log.info("checking " + cnV2.getNodeBaseServiceUrl() + " for pid : " + pidValue);
                    cnV2.getSystemMetadata(null, pid);
                    pidsNotSyncedNext.remove(pidValue);
                    log.info("sysmeta found for pid : " + pidValue + " on " + cnV2.getNodeBaseServiceUrl());
                } catch (Exception e) {
                    log.info("sysmeta NOT found for pid : " + pidValue + " on " + cnV2.getNodeBaseServiceUrl());
                }
            }
            
            pidsNotSyncedCurrent = pidsNotSyncedNext;  
            pidsNotSyncedNext = new HashSet<String>(pidsNotSyncedCurrent);
            log.info("(" + pidsNotSyncedCurrent.size() + " objects left to sync)");
            
            if (pidsNotSyncedCurrent.size() == 0)
                break;
            
            try {
                log.info("waiting...");
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        
        for (String pidValue : pidsNotSyncedCurrent)
            log.error("Pid not synced to CN yet: " + pidValue);
        
        log.info("waiting for replication of test objects...");
        
        HashSet<String> pidsNotReplicatedCurrent = new HashSet<String>(replTestPids);
        HashSet<String> pidsNotReplicatedNext = new HashSet<String>(replTestPids);
        
        startTime = System.currentTimeMillis();
        endTime = startTime + (MAX_REPL_MINUTES * 60000);
        
        log.info("will check for replicas till " + (new Date(endTime)));
        
        while (System.currentTimeMillis() < endTime) {
            if (pidsNotReplicatedCurrent.size() == 0)
                break;
            
            for (String pidValue : pidsNotReplicatedCurrent) {
                Identifier pid = D1TypeBuilder.buildIdentifier(pidValue);
                try {
                    log.info("checking " + cnV2.getNodeBaseServiceUrl() + " for pid : " + pidValue + " for replicas");
                    SystemMetadata cnSysmeta = cnV2.getSystemMetadata(null, pid);
                    List<Replica> replicaList = cnSysmeta.getReplicaList();
                    if (replicaList == null || replicaList.size() == 0) {
                        log.info("replicas NOT found for pid : " + pidValue + " on " + cnV2.getNodeBaseServiceUrl());
                        continue;
                    }
                    int numMnReplicas = 0;
                    for (Replica rep : replicaList)
                        for (Node n : allMNs)
                            if (n.getIdentifier().getValue().equals(rep.getReplicaMemberNode().getValue()))
                                numMnReplicas++;

                    if (numMnReplicas < allMNs.size()-1) { // -1 for origin MN
                        log.info("only " + numMnReplicas + "/" + (allMNs.size()-1) + " replicas found for pid : " + pidValue + " on " + cnV2.getNodeBaseServiceUrl());
                        continue;
                    } else {
                        log.info("found " + (numMnReplicas-1) + " replicas found for pid : " + pidValue + " on " + cnV2.getNodeBaseServiceUrl());
                        pidsNotReplicatedNext.remove(pidValue);
                    }
                    
                } catch (Exception e) {
                    // just don't remove pid from set to search
                }
            }
            
            pidsNotReplicatedCurrent = pidsNotReplicatedNext;  
            pidsNotReplicatedNext = new HashSet<String>(pidsNotReplicatedCurrent);
            log.info("(" + pidsNotReplicatedCurrent.size() + " objects left to fully replicate)");
            
            if (pidsNotReplicatedCurrent.size() == 0)
                break;
            try {
                log.info("waiting...");
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        
        for (String pidValue : pidsNotReplicatedCurrent)
            log.error("Not enough replicas found on CN for: " + pidValue);
    }
    
    private void setupV2CreateV1UpdateSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        AccessPolicy policy = new AccessPolicy();
        policy.addAllow(accessRule);
        
        Node mNode = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        
        // v2 create
        
        Identifier oldPid = D1TypeBuilder.buildIdentifier("testV2CreateV1UpdateSameNode_pid_" + pidTimestamp);
        Identifier oldSid = D1TypeBuilder.buildIdentifier("testV2CreateV1UpdateSameNode_sid_" + pidTimestamp);
        
        try {
            createTestObject(v2CallAdapter, oldPid, oldSid, null, null, policy, "testRightsHolder", "testRightsHolder");
            syncTestPids.add(oldPid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 update")
    @WebTestDescription(
     "Test operates on a single MN that supports both v1 & v2 APIs." +
     "It does a create on the v2 endpoint, then an update on the v1 endpoint." +
     "The update operation should be successful.")
    public void testV2CreateV1UpdateSameNode() {

        Node mNode = v1v2MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        Identifier oldPid = D1TypeBuilder.buildIdentifier("testV2CreateV1UpdateSameNode_pid_" + pidTimestamp);
        
        // grab the old sysmeta
        
        SystemMetadata oldSysmeta = null;
        try {
            oldSysmeta = v2CallAdapter.getSystemMetadata(null, oldPid);
        } catch (Exception e) {
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() couldn't fetch old sysmeta: " 
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // v1 update
        
        Identifier newPid = null;
        SystemMetadata newSysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV2CreateV1UpdateSameNode_pid2_" + pidTimestamp);
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            newSysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            v1CallAdapter.update(null, oldPid, objectInputStream, newPid, newSysmeta);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1UpdateSameNode() update call failed! : " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2UpdateSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node mNode = v1v2MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2UpdateSameNode_1_" + pidTimestamp);
        
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2UpdateSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2UpdateSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
    }
    
    @WebTestName("v1 create, v2 update")
    @WebTestDescription(
     "Test operates on a single MN that supports both v1 & v2 APIs." +
     "It does a create on the v1 endpoint, then an update on the v2 endpoint." +
     "The update operation should succeed.")
    public void testV1CreateV2UpdateSameNode() {

        Node mNode = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), mNode, "v2");
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2UpdateSameNode_1_" + pidTimestamp);

        // v2 update
        
        Identifier newPid = null;
        SystemMetadata sysmeta = null;
        InputStream objectInputStream = null;
        
        try {
            newPid = D1TypeBuilder.buildIdentifier("testV1CreateV2UpdateSameNode_2_" + pidTimestamp);
            byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
            D1Object d1o = new D1Object(newPid, contentBytes,
                    D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                    D1TypeBuilder.buildSubject(getSubject(cnSubmitter).getValue()),
                    D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
            sysmeta = TypeFactory.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
            
            objectInputStream = new ByteArrayInputStream(contentBytes);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError("testV1CreateV2UpdateSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError("testV1CreateV2UpdateSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
        try {
            Identifier updPid = v2CallAdapter.update(null, pid, objectInputStream, newPid, sysmeta);
            assertTrue("testV2CreateV1Update: update on v2 endpoint should succeed", updPid != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2UpdateSameNode: update on v2 endpoint should succeed; got: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2UpdateSameNode: update on v2 endpoint should succeed; got: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateV1GetSysMeta() {
        
        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSysMeta_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysMeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysMeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
        
    }
    
    @WebTestName("v2 create, v1 getSystemMetadata")
    @WebTestDescription(
     "Test operates on two MNs - one that supports the v2 APIs, one that supports ONLY v1." +
     "It does a create on the v2 endpoint, then sleeps for some time, " + 
     "then checks the v1 MN whether replication has moved anything to it." + 
     "This check should fail since v2-created data should NOT be replicated " +
     "down to v1 nodes.")
    public void testV2CreateV1GetSysMeta() {
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSysMeta_" + pidTimestamp);

        // v1 getSysmeta

        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        try {
            v1CallAdapter.getSystemMetadata(null, pid);
        } catch (NotFound e) {
            // expected, shouldn't have been replicated to a v1 node
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysMeta() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysMeta() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateV1GetSysmetaSameNode() {
        
        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2Node = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSysmetaSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysmetaSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysmetaSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 getSystemMetadata, same node")
    @WebTestDescription(
     "Test operates on a single MN - one that supports BOTH the v1 and v2 APIs. " +
     "It does a create on the v2 endpoint, then attempts a getSystemMetadata on the v1 endpoint. " +
     "The getSystemMetadata should succeed since we're on the same node; " +
     "the SystemMetadata will just be downcasted from the v2 version.")
    public void testV2CreateV1GetSysmetaSameNode() {
        
        Node v1v2Node = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSysmetaSameNode_" + pidTimestamp);
        
        // v1 getSysmeta

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v1");
        
        try {
            SystemMetadata sysmeta = v1CallAdapter.getSystemMetadata(null, pid);
            assertTrue("v1 getSystemMetadata() after a v2 create() on the same node should succeed, "
                    + "returning a non-null SystemMetadata", sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysmetaSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSysmetaSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2Get() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Get_" + pidTimestamp);
        try {
            createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            replTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Get() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Get() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } 
    }
    
    @WebTestName("v1 create, v2 get, different nodes")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a get() on the v2 MN. " +
     "The get() should succeed")
    public void testV1CreateV2Get() {

        Node v1MNode = v1MNs.get(0);
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Get_" + pidTimestamp);
        
        // v2 get

        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (NotFound e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Get() : unable to find CN sysmeta for pid " + pid.getValue() 
                    + ". Check status of CN sync. " + " Origin MN: " + v1MNode.getBaseURL());
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Get() : unable to get CN sysmeta for pid " + pid.getValue() 
                    + ". Check status of CN sync. " + " Origin MN: " + v1MNode.getBaseURL());
        }
        
        Node replicaMN = null;
        try {
            List<Replica> replicaList = cnSysmeta.getReplicaList();
            outerloop:
            for (Replica rep : replicaList) {
                for (Node availMN : v2MNs) {
                    if (availMN.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() )
                            && !rep.getReplicaMemberNode().getValue().equals(v1MNode.getIdentifier().getValue())
                            && rep.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                        replicaMN = availMN;
                        break outerloop;
                    }
                }
            }
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Get() : unable to get replica info from CN sysmeta! Check status of CN replication." 
                    + ", origin MN: " + v1MNode.getBaseURL());
        }
        
        if(replicaMN == null)
            throw new AssertionError("Unable to locate a replica MN for pid: " + pid.getValue());
        
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), replicaMN, "v2");
        
        InputStream is = null;
        try {
            is = v2CallAdapter.get(null, pid);
            assertTrue("a v2 get() after a v1 create() on a different node should "
                    + "(given enough time for replication) "
                    + "return a non-null InputStream", 
                    is != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Get() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Get() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    private void setupV2CreateV1GetSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2Node = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 get, same node")
    @WebTestDescription(
     "Test operates on a single MN - one that supports BOTH the v1 and v2 APIs. " +
     "It does a create on the v2 endpoint, then attempts a get() on the v1 endpoint. " +
     "The get() should succeed since we're on the same node.")
    public void testV2CreateV1GetSameNode() {

        Node v1v2Node = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1GetSameNode_" + pidTimestamp);
        
        // v1 get

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2Node, "v1");
        
        InputStream is = null;
        try {
            is = v1CallAdapter.get(null, pid);
            assertTrue("A get on the v1 endpoint after a v2 endpoint create on the same node "
                    + "should return a non-null InputStream.", is != null);
        } catch (BaseException e) {
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSameNode() couldn't create update object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription(), e);
        }
        catch(Exception e) {
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1GetSameNode() couldn't create update object: " 
            + e.getClass().getName() + ": " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    private void setupV1CreateV2GetSysmeta() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2GetSysmeta_" + pidTimestamp);
        
        log.info("Trying to create test object: " + pid.getValue() + " on MN: " + v1CallAdapter.getNodeBaseServiceUrl());
        try {
            createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            replTestPids.add(pid.getValue());
            log.info("Created test object: " + pid.getValue() + " on MN: " + v1CallAdapter.getNodeBaseServiceUrl());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2GetSysmeta() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2GetSysmeta() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 getSystemMetadata, different nodes")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a getSystemMetadata on the v2 MN. " +
     "The getSystemMetadata should succeed; the SystemMetadata will just be " + 
     "upcasted to the v2 version.")
    public void testV1CreateV2GetSysmeta() {

        // v1 create
        
        Node v1MNode = v1MNs.get(0);
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2GetSysmeta_" + pidTimestamp);
        
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (NotFound e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2GetSysmeta() : unable to find sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v1MNode.getBaseURL());
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2GetSysmeta() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v1MNode.getBaseURL());
        } 
        
        Node replicaMN = null;
        List<Replica> replicaList = cnSysmeta.getReplicaList();
        outerloop:
        for (Replica rep : replicaList) {
            for (Node availMN : v2MNs) {
                if (availMN.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() )
                        && !rep.getReplicaMemberNode().getValue().equals(v1MNode.getIdentifier().getValue())
                        && rep.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                    replicaMN = availMN;
                    break outerloop;
                }
            }
        }
        
        if(replicaMN == null)
            throw new AssertionError("Unable to locate a replica MN for pid: " + pid.getValue());
        
        // v2 getSysmeta

        Node v2MNode = replicaMN;
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            SystemMetadata sysmeta = v2CallAdapter.getSystemMetadata(null, pid);
            assertTrue("getSystemMetadata() should return an upcasted version of the created object's system metadata", 
                    sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " : testV1CreateV2GetSysmeta() couldn't fetch sysmeta from replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " : testV1CreateV2GetSysmeta() couldn't fetch sysmeta from replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2GetSysmetaSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1MNode = v1v2MNs.get(0);
        MNCallAdapter v1Endpoint = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2GetSysmetaSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v1Endpoint, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1Endpoint.getLatestRequestUrl() + " testV1CreateV2GetSysmetaSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1Endpoint.getLatestRequestUrl() + " testV1CreateV2GetSysmetaSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 getSystemMetadata")
    @WebTestDescription(
     "Test operates on one MN which supports BOTH the v1 API and the v2 API" +
     "It does a create on the v1 endpoint, then attempts a getSystemMetadata on the v2 endpoint." +
     "The getSystemMetadata should succeed; the SystemMetadata will just be" + 
     "upcasted to the v2 version.")
    public void testV1CreateV2GetSysmetaSameNode() {

        // v2 getSysmeta

        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2GetSysmetaSameNode_" + pidTimestamp);
        Node v2MNode = v1v2MNs.get(0);
        MNCallAdapter v2Endpoint = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            SystemMetadata sysmeta = v2Endpoint.getSystemMetadata(null, pid);
            assertTrue("getSystemMetadata() should return an upcasted version of the created object's system metadata", 
                    sysmeta != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2Endpoint.getLatestRequestUrl() + " testV1CreateV2GetSysmetaSameNode() couldn't fetch sysmeta from replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2Endpoint.getLatestRequestUrl() + " testV1CreateV2GetSysmetaSameNode() couldn't fetch sysmeta from replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2Query() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.READ);
        
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Query_" + pidTimestamp);
        try {
            createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            replTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 query")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " + 
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then waits for replication to happen, " +
     "then attempts a query for the created pid on the v2 MN. " +
     "The query should succeed, returning a non-empty result.")
    public void testV1CreateV2Query() {

        Node v1MNode = v1MNs.get(0);
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Query_" + pidTimestamp);
        
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Query() : unable to fetch sysmeta for " + pid.getValue() 
                    + " from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v1MNode.getBaseURL());
        }
        
        Node replicaMN = null;
        try {
            List<Replica> replicaList = cnSysmeta.getReplicaList();
            outerloop:
            for (Replica rep : replicaList) {
                for (Node availMN : v2MNs) {
                    if (availMN.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() )
                            && !rep.getReplicaMemberNode().getValue().equals(v1MNode.getIdentifier().getValue())
                            && rep.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                        replicaMN = availMN;
                        break outerloop;
                    }
                }
            }
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Query() : unable to get replica info from CN sysmeta! Check status of CN replication." 
                    + ", origin MN: " + v1MNode.getBaseURL());
        }
        
        if(replicaMN == null)
            throw new AssertionError("Unable to locate a replica MN for pid: " + pid.getValue());
        
        // v2 query

        Node v2MNode = replicaMN;
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        InputStream is = null;
        try {
            String encodedPid = URLEncoder.encode(pid.getValue(), "UTF-8");
            is = v2CallAdapter.query(null, "solr", "q=identifier:" + encodedPid);
            assertTrue("query() should return a non-null stream", is != null);
            
            Document doc = null;
            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.parse(new InputSource(is));
            } catch (Exception e) {
                throw new AssertionError(v2CallAdapter.getLatestRequestUrl() +
                        "testV1CreateV2Query: unable to convert response to document: " 
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            XPath xPath =  XPathFactory.newInstance().newXPath();
            String resultCountExp = "/response/result";
            org.w3c.dom.Node resultNode = (org.w3c.dom.Node) xPath.compile(resultCountExp).evaluate(doc, XPathConstants.NODE);
            org.w3c.dom.Node numFoundAttr = resultNode.getAttributes().getNamedItem("numFound");
            assertTrue("testV1CreateV2Query() query response doesn't have valid numFound attribute.", numFoundAttr != null);

            String numFoundVal = numFoundAttr.getNodeValue(); 
            Integer numFoundInt = Integer.parseInt(numFoundVal);
            assertTrue("testV1CreateV2Query() query response should not contain zero results.", 
                    numFoundInt > 0);

            String pidExp = "/response/result/doc/str[@name='identifier']";
            String pidVal = xPath.compile(pidExp).evaluate(doc);
            assertTrue("testV1CreateV2Query() query response should be for the pid created", 
                    pidVal.equals(pid.getValue()));
            
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Query() query failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Query() query failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private void setupV2CreateV1Query() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.READ);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Query_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1Query() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1Query() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 query")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then waits for replication to happen, " +
     "then attempts a query for the created pid on the v1 MN. " +
     "The query should fail with an exception, since replication shouldn't " + 
     "have happened upward for a v1 object to a v2 node.")
    public void testV2CreateV1Query() {

        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Query_" + pidTimestamp);
        
        // v1 query

        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        InputStream is = null;
        try {
            String encodedPid = URLEncoder.encode(pid.getValue(), "UTF-8");
            is = v1CallAdapter.query(null, "solr", "q=identifier:" + encodedPid);
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Query(): "
                    + "query() on the v1 MN should fail.");
        } catch (InvalidRequest e) {
            // expected - query() on v1 MN should fail
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Query() query failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Query() query failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    private void setupV2CreateV1DeleteSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1DeleteSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1DeleteSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1DeleteSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 delete, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v2 endpoint, then attempts to delete the object " +
     "for that pid on the v1 endpoint. The delete should succeed.")
    public void testV2CreateV1DeleteSameNode() {

        Node v1v2MNode = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1DeleteSameNode_" + pidTimestamp);
        
        // v1 delete

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        try {
            Identifier deleteId = v1CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2DeleteSameNode() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1DeleteSameNode() delete failed: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1DeleteSameNode() delete failed: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2DeleteSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2DeleteSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2DeleteSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2DeleteSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 delete, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v1 endpoint, then attempts to delete the object " +
     "for that pid on the v2 endpoint. The delete should succeed since we're on " +
     "the same node.")
    public void testV1CreateV2DeleteSameNode() {

        Node v1v2MNode = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2DeleteSameNode_" + pidTimestamp);

        // v2 delete

        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        try {
            Identifier deleteId = v2CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2DeleteSameNode() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2DeleteSameNode() delete failed: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2DeleteSameNode() delete failed: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateV1Delete() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Delete_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1Delete() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1Delete() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 delete")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then attempts to delete the object " +
     "for that pid on the v1 MN. The delete should fail because replication from " +
     "a v2 MN to a v1 MN shouldn't happen for v2 objects.")
    public void testV2CreateV1Delete() {

        // v1 delete

        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1Delete_" + pidTimestamp);
        
        try {
            v1CallAdapter.delete(null, pid);
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Delete(): "
                    + "delete() on the v1 MN should fail.");
        } catch (NotFound e) {
            // expected - not available on this node
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Delete() delete failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1Delete() delete failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2Delete() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Delete_" + pidTimestamp);
        try {
            createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            replTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Delete() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2Delete() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 delete")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then attempts to delete the object " +
     "for that pid on the v2 MN. The delete should succeed because the object should " +
     "have been replicated from the v1 MN to the v2 MN.")
    public void testV1CreateV2Delete() {

        Node v1MNode = v1MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2Delete_" + pidTimestamp);
        
        // wait for replication
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Query() : unable to fetch sysmeta "
                    + "for pid " + pid.getValue() + " on CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v1MNode.getBaseURL());
        } 
        
        Node replicaMN = null;
        try {
            List<Replica> replicaList = cnSysmeta.getReplicaList();
            outerloop:
            for (Replica rep : replicaList) {
                for (Node availMN : v2MNs) {
                    if (availMN.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() )
                            && !rep.getReplicaMemberNode().getValue().equals(v1MNode.getIdentifier().getValue())
                            && rep.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                        replicaMN = availMN;
                        break outerloop;
                    }
                }
            }
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2Query() : unable to get replica info from CN sysmeta! Check status of CN replication." 
                    + ", origin MN: " + v1MNode.getBaseURL());
        }
        
        if(replicaMN == null)
            throw new AssertionError("Unable to locate a replica MN for pid: " + pid.getValue());
        
        // v2 delete

        Node v2MNode = replicaMN;
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            Identifier deleteId = v2CallAdapter.delete(null, pid);
            assertTrue("testV1CreateV2Delete() - v2 delete should have succeeded and returned "
                    + "the pid of the deleted object.", deleteId != null);
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Delete() delete failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2Delete() delete failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateV1ListObjectsSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1ListObjectsSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjectsSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjectsSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 listObjects, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v2 endpoint, then attempts to locate the object " +
     "for that pid using listObjects() on the v1 endpoint. It should be found.")
    public void testV2CreateV1ListObjectsSameNode() {

        Node v1v2MNode = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1ListObjectsSameNode_" + pidTimestamp);
        
        // v1 delete

        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        try {
            ObjectList objList = APITestUtils.pagedListObjects(v1CallAdapter, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV2CreateV1ListObjectsSameNode() - v1 listObjects() results "
                    + "should include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjectsSameNode() listObjects failed: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjectsSameNode() listObjects failed: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2ListObjectsSameNode() {

        assertTrue("Tests require at least 1 MN that supports BOTH v1 & v2 APIs", v1v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        Node v1v2MNode = v1v2MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v1");
        
        // v1 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2ListObjectsSameNode_" + pidTimestamp);
        try {
            pid = createTestObject(v1CallAdapter, pid, accessRule);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjectsSameNode() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjectsSameNode() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 listObjects, same node")
    @WebTestDescription(
     "Test operates on one MN that supports BOTH the v1 and v2 APIs " +
     "It does a create on the v1 endpoint, then attempts to locate the object " +
     "for that pid using listObjects() on the v2 endpoint. The pid should be found.")
    public void testV1CreateV2ListObjectsSameNode() {

        Node v1v2MNode = v1v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2ListObjectsSameNode_" + pidTimestamp);
        
        // v2 listObjects

        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1v2MNode, "v2");
        
        try {
            ObjectList objList = APITestUtils.pagedListObjects(v2CallAdapter, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV1CreateV2ListObjectsSameNode() - v2 listObjects() results "
                    + "should include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjectsSameNode() listObjects failed: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjectsSameNode() listObjects failed: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateV1ListObjects() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1ListObjects_" + pidTimestamp);
        try {
            pid = createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjects() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjects() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, v1 listObjects")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v2 MN, then attempts to locate the object " +
     "for that pid using listObjects() on the v1 MN. The listObjects results " +
     "should not contain the pid created because replication from " +
     "a v2 MN to a v1 MN shouldn't happen for v2 objects.")
    public void testV2CreateV1ListObjects() {

        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1ListObjects_" + pidTimestamp);

        // v1 listObjects

        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        try {
            ObjectList objList = APITestUtils.pagedListObjects(v1CallAdapter, null, null, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertFalse("testV2CreateV1ListObjects() - v2 listObjects() results "
                    + "should NOT include the created object.", objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjects() listObjects failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV2CreateV1ListObjects() listObjects failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV1CreateV2ListObjects() {

        assertTrue("Tests require at least 1 MN that supports ONLY the v1 API", v1MNs.size() >= 1);
        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        getSession("testRightsHolder");
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v1MNode = v1MNs.get(0);
        MNCallAdapter v1CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v1MNode, "v1");
        
        // v1 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2ListObjects_" + pidTimestamp);
        try {
            createTestObject(v1CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            replTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjects() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v1CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjects() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v1 create, v2 listObjects")
    @WebTestDescription(
     "Test operates on two MNs - one that supports ONLY the v1 API and a node " +
     "that supports the v2 API. " +
     "It does a create on the v1 MN, then attempts to delete the object " +
     "for that pid on the v2 MN. The listObjects call should return results containing " +
     "the created pid because the object should have been replicated " +
     "from the v1 MN to the v2 MN.")
    public void testV1CreateV2ListObjects() {

        Node v1MNode = v1MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV1CreateV2ListObjects_" + pidTimestamp);
        
        // wait for replication
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2ListObjects() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v1MNode.getBaseURL());
        } 
        
        Node replicaMN = null;
        try {
            List<Replica> replicaList = cnSysmeta.getReplicaList();
            outerloop:
            for (Replica rep : replicaList) {
                for (Node availMN : v2MNs) {
                    if (availMN.getIdentifier().getValue().equals( rep.getReplicaMemberNode().getValue() )
                            && !rep.getReplicaMemberNode().getValue().equals(v1MNode.getIdentifier().getValue())
                            && rep.getReplicationStatus() == ReplicationStatus.COMPLETED) {
                        replicaMN = availMN;
                        break outerloop;
                    }
                }
            }
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV1CreateV2ListObjects() : unable to get replica info from CN sysmeta! Check status of CN replication." 
                    + ", origin MN: " + v1MNode.getBaseURL());
        }
        
        if(replicaMN == null)
            throw new AssertionError("Unable to locate a replica MN for pid: " + pid.getValue());
        
        // v2 listObjects

        Node v2MNode = replicaMN;
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        try {
            // ObjectList objList = APITestUtils.pagedListObjects(v2CallAdapter, null, null, null, null, null);
            ObjectList objList = v2CallAdapter.listObjects(null, null, null, null, null, pid, null, null, null);
            boolean objFound = false;
            for(ObjectInfo objInfo : objList.getObjectInfoList())
                if(pid.getValue().equals(objInfo.getIdentifier().getValue())){
                    objFound = true;
                    break;
                }
            assertTrue("testV1CreateV2ListObjects() - v2 listObjects() on " + v2MNode.getBaseURL() + "results "
                    + "should include the created object: " + pid.getValue(), objFound );
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjects() listObjects failed on replica target: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV1CreateV2ListObjects() listObjects failed on replica target: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    private void setupV2CreateCnArchive() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnArchive_" + pidTimestamp);
        try {
            createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnArchive() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnArchive() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, CN archive")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to archive the object on the CN using both the v1 and v2 call. " +
     "The archive calls should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, and this call should only work for an object with a v1 authoritative MN. Expects a NotAuthorized exception.")
    public void testV2CreateCnArchive() {

        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnArchive_" + pidTimestamp);
        
        // CN archive
        
        try {
            cnV1.archive(null, pid);
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnArchive() : v1 archive call to CN should fail because "
                    + "object being archived has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnArchive() : expected v1 archive call to CN should fail "
                    + "with a NotAuthorized exception because object being archived has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
        
        // v2 call
        
        try {
            cnV2.archive(null, pid);
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnArchive() : v2 archive call to CN should fail because "
                    + "object being archived has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnArchive() : expected v2 archive call to CN should fail "
                    + "with a NotAuthorized exception because object being archived has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    private void setupV2CreateCnSetReplicationPolicy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetReplicationPolicy_" + pidTimestamp);
        try {
            createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetReplicationPolicy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetReplicationPolicy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, CN setReplicationPolicy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setReplicationPolicy on the object on the CN using both the v1 and v2 call. " +
     "The setReplicationPolicy calls should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, and this call should only work for an object with a v1 authoritative MN. Expects a NotAuthorized exception.")
    public void testV2CreateCnSetReplicationPolicy() {

        Node v2MNode = v2MNs.get(0);
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetReplicationPolicy_" + pidTimestamp);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        // CN setReplicationPolicy
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV2CreateCnSetReplicationPolicy() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v2MNode.getBaseURL());
        } 
        
        try {
            replPolicy.setNumberReplicas(replPolicy.getNumberReplicas() + 1);
            cnV1.setReplicationPolicy(null, pid, replPolicy, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetReplicationPolicy() : setReplicationPolicy "
                    + "v1 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetReplicationPolicy() : expected setReplicationPolicy "
                    + "v1 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
        
        // v2 call
        
        try {
            replPolicy.setNumberReplicas(replPolicy.getNumberReplicas() + 1);
            cnV2.setReplicationPolicy(null, pid, replPolicy, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetReplicationPolicy() : setReplicationPolicy "
                    + "v2 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetReplicationPolicy() : expected setReplicationPolicy "
                    + "v2 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    private void setupV2CreateCnSetAccessPolicy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetAccessPolicy_" + pidTimestamp);
        try {
            createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetAccessPolicy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetAccessPolicy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, cn setAccessPolicy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setAccessPolicy on the object on the CN using both the v1 and v2 call. " +
     "The setAccessPolicy calls should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, and this call should only work for an object with a v1 authoritative MN. Expects a NotAuthorized exception.")
    public void testV2CreateCnSetAccessPolicy() {

        Node v2MNode = v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetAccessPolicy_" + pidTimestamp);
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV2CreateCnSetAccessPolicy() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v2MNode.getBaseURL());
        } 
        
        // CN setAccessPolicy
        
        try {
            AccessPolicy accessPolicy = new AccessPolicy();
            accessPolicy.addAllow(accessRule);
            cnV1.setAccessPolicy(null, pid, null, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetAccessPolicy() : setAccessPolicy "
                    + "v1 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetAccessPolicy() : expected setAccessPolicy "
                    + "v1 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
        
        // v2 call
        
        try {
            AccessPolicy accessPolicy = new AccessPolicy();
            accessPolicy.addAllow(accessRule);
            cnV2.setAccessPolicy(null, pid, null, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetAccessPolicy() : setAccessPolicy "
                    + "v2 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetAccessPolicy() : expected setAccessPolicy "
                    + "v2 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    private void setupV2CreateCnSetRightsHolder() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetRightsHolder_" + pidTimestamp);
        try {
            createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetRightsHolder() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateCnSetRightsHolder() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, cn setRightsHolder")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setRightsHolder the object on the CN using both the v1 and v2 call. " +
     "The setRightsHolder calls should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, and this call should only work for an object with a v1 authoritative MN. Expects a NotAuthorized exception.")
    public void testV2CreateCnSetRightsHolder() {

        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateCnSetRightsHolder_" + pidTimestamp);
        
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV2CreateCnSetRightsHolder() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v2MNode.getBaseURL());
        } 
        
        // CN setRightsHolder
        
        try {
            getSession("testRightsHolder");
            Subject newSubject = D1TypeBuilder.buildSubject("testRightsHolder");
            cnV1.setRightsHolder(null, pid, newSubject, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetRightsHolder() : setRightsHolder "
                    + "v1 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateCnSetRightsHolder() : expected setRightsHolder "
                    + "v1 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
        
        // v2 call
        
        try {
            getSession("testRightsHolder");
            Subject newSubject = D1TypeBuilder.buildSubject("testRightsHolder");
            cnV2.setRightsHolder(null, pid, newSubject, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetRightsHolder() : setRightsHolder "
                    + "v2 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateCnSetRightsHolder() : expected setRightsHolder "
                    + "v2 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
    
    private void setupV2CreateCnSetObsoletedBy() {

        assertTrue("Tests require at least 1 MN that supports the v2 API", v2MNs.size() >= 1);
        
        AccessRule accessRule = new AccessRule();
        Subject subject = D1TypeBuilder.buildSubject(Constants.SUBJECT_PUBLIC);
        accessRule.addSubject(subject);
        accessRule.addPermission(Permission.CHANGE_PERMISSION);
        ReplicationPolicy replPolicy = new ReplicationPolicy();
        replPolicy.setReplicationAllowed(true);
        replPolicy.setNumberReplicas(allMNs.size()-1);
        
        Node v2MNode = v2MNs.get(0);
        MNCallAdapter v2CallAdapter = new MNCallAdapter(getSession(cnSubmitter), v2MNode, "v2");
        
        // v2 create
        
        final Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_" + pidTimestamp);
        Identifier obsoletedByPid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_obs_" + pidTimestamp);
        try {
            createTestObject(v2CallAdapter, pid, accessRule, replPolicy);
            obsoletedByPid = createTestObject(v2CallAdapter, obsoletedByPid, accessRule, replPolicy);
            syncTestPids.add(pid.getValue());
            syncTestPids.add(obsoletedByPid.getValue());
        } catch (BaseException e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1CnSetObsoletedBy() couldn't create test object: " 
                    + e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new AssertionError(v2CallAdapter.getLatestRequestUrl() + " testV2CreateV1CnSetObsoletedBy() couldn't create test object: " 
            + e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    @WebTestName("v2 create, cn setObsoletedBy")
    @WebTestDescription(
     "Test operates on a v2 MN and a CN." +
     "It does a create on the v2 MN, then attempts to setObsoletedBy on the object on the CN using both the v1 and v2 call. " +
     "The setObsoletedBy calls should fail since the object is a v2 object - its authoritative " +
     "MN is a v2 MN, and this call should only work for an object with a v1 authoritative MN. Expects a NotAuthorized exception.")
    public void testV2CreateCnSetObsoletedBy() {

        Node v2MNode = v2MNs.get(0);
        Identifier pid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_" + pidTimestamp);
        Identifier obsoletedByPid = D1TypeBuilder.buildIdentifier("testV2CreateV1CnSetObsoletedBy_obs_" + pidTimestamp);
        
        SystemMetadata cnSysmeta = null;
        try {
            cnSysmeta = cnV2.getSystemMetadata(null, pid);
        } catch (Exception e) {
            throw new AssertionError(cnV2.getLatestRequestUrl() + " testV2CreateV1CnSetObsoletedBy() : unable to fetch sysmeta from CN! Check status of CN sync. "
                    + e.getClass().getSimpleName() + " : " + e.getMessage() 
                    + ", origin MN: " + v2MNode.getBaseURL());
        } 
        
        // CN setObsoletedBy
        
        try {
            cnV1.setObsoletedBy(null, pid, obsoletedByPid, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : setObsoletedBy "
                    + "v1 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV1.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : expected setObsoletedBy "
                    + "v1 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
        
        // v2 call
        
        try {
            cnV2.setObsoletedBy(null, pid, obsoletedByPid, cnSysmeta.getSerialVersion().longValue());
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : setObsoletedBy "
                    + "v2 call to CN should fail because object has a v2 node as its authoritative MN" );
        } catch (NotAuthorized e) {
            
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(cnV2.getLatestRequestUrl(), "testV2CreateV1CnSetObsoletedBy() : expected setObsoletedBy "
                    + "v2 call to CN should fail with a NotAuthorized exception because object has a v2 node as its "
                    + "authoritative MN. Got: " + e.getClass().getSimpleName() + " : " + e.getMessage() );
        }
    }
}
