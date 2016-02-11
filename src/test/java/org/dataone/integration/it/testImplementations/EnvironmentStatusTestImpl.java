package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Services;
import org.dataone.service.types.v2.NodeList;
import org.junit.Before;
import org.junit.Test;


public class EnvironmentStatusTestImpl extends ContextAwareTestCaseDataone {

    private List<Node> cns;
    private NodeList cnNodeList;
    
    /** v1-ONLY MNs */
    private List<Node> v1mns; 
    /** v2-ONLY MNs */
    private List<Node> v2mns;
    /** v1 AND v2 MNs */
    private List<Node> v1v2mns;
    /** all MNs */
    private List<Node> mns;     
    
    private boolean setup = false;
    
    
    @Before
    public void setup() throws Exception {
        if(!setup) {    // only run once, but still test assertions at end @Before each test case
        
            cns = new ArrayList<Node>();
            mns = new ArrayList<Node>();
            v1mns = new ArrayList<Node>();
            v2mns = new ArrayList<Node>();
            v1v2mns = new ArrayList<Node>();
            
            Iterator<Node> cnIter = getCoordinatingNodeIterator();
            if(cnIter != null)
                cns = IteratorUtils.toList(cnIter);
            
            assertTrue("Setup failed! Test requires at least one CN to function.", cns.size() > 0);
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cns.get(0), "v2");
            
            try {
                log.info("Fetching NodeList from CN: " + cn.getNodeBaseServiceUrl());
                cnNodeList = cn.listNodes();
            } catch (Exception e) {
                log.error("FAILED setup, unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
                throw e;
            }
            
            for(Node n : cnNodeList.getNodeList()) {
                if(n.getType() != NodeType.MN)
                    continue;
                
                log.info("Checking capabilities for MN: " + n.getBaseURL());
                MNCallAdapter v1MN = new MNCallAdapter(getSession(cnSubmitter), n, "v1");
                MNCallAdapter v2MN = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                
                Node v1Capabilities = null;
                Node v2Capabilities = null;
                try {
                    v1Capabilities = v1MN.getCapabilities();
                    log.info("success for v1 MN.getCapabilities() on: " + v1MN.getLatestRequestUrl());
                } catch (Exception v1e) {
                    log.error("FAILED v1 MN.getCapabilities() on: " + v1MN.getLatestRequestUrl());
                }
                
                try {
                    v2Capabilities = v2MN.getCapabilities();
                    log.info("success for v2 MN.getCapabilities() on: " + v2MN.getLatestRequestUrl());
                } catch (Exception v2e) {
                    log.error("FAILED v2 MN.getCapabilities() on: " + v2MN.getLatestRequestUrl());
                }
                
                if (v1Capabilities != null && v2Capabilities == null) {
                    log.info("adding v1 MN: " + v1Capabilities.getBaseURL());
                    v1mns.add(v1Capabilities);
                    mns.add(v1Capabilities);
                }
                if (v2Capabilities != null && v1Capabilities == null) {
                    log.info("adding v2 MN: " + v2Capabilities.getBaseURL());
                    v2mns.add(v2Capabilities);
                    mns.add(v2Capabilities);
                }
                if (v1Capabilities != null && v2Capabilities != null) {
                    log.info("adding v1/v2 MN: " + v2Capabilities.getBaseURL());
                    v1v2mns.add(v2Capabilities);
                    mns.add(v2Capabilities);
                }
            }
            setup = true;
        }
        assertTrue("FAILED setup, found zero CNs.", cns.size() != 0);
    }
    
    
    @Override
    protected String getTestDescription() {
        return "Various checks for the current environment - stuff like whether all nodes can be pinged, or CN / MN node doc consistency.";
    }

    
    @Test
    @WebTestName("ping environment nodes")
    @WebTestDescription("Tests whether the nodes listed for the environment can be pinged and are up. "
            + "For MNs, disregards whether they are registered as v1 or v2 nodes with the CN "
            + "and pings both endpoints.")
    public void checkNodes() {

        ArrayList<String> errors = new ArrayList<String>();
        
        log.info("======================================================================");
        log.info("pinging CNs...");
        
        for (Node node : cnNodeList.getNodeList()) {
            if (node.getType() != NodeType.CN)
                continue;
            
            log.info("pinging CN: " + node.getBaseURL());
            CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), node, "v2");
            
            try {
                cn.ping();
                log.info("success pinging " + node.getBaseURL());
            } catch (Exception e) {
                log.error("ERROR: failed pinging " + node.getBaseURL());
                errors.add("ERROR: failed pinging " + node.getBaseURL());
            }
        }
        
        log.info("pinging MNs...");
        
        for (Node node : cnNodeList.getNodeList()) {
            if (node.getType() != NodeType.MN)
                continue;
            
            if (node.getState() == NodeState.DOWN) {
                log.warn("WARNING: CN has Node state as DOWN for: " + node.getBaseURL());
                errors.add("WARNING: CN has Node state as DOWN for: " + node.getBaseURL());
                continue;
            } else if (node.getState() == NodeState.UNKNOWN) {
                log.warn("WARNING: CN has Node state as UNKNOWN for: " + node.getBaseURL());
                errors.add("WARNING: CN has Node state as UNKNOWN for: " + node.getBaseURL());
            } else if (node.getState() == NodeState.UP) {
                log.info("CN has Node state as UP for: " + node.getBaseURL());
            }
            
            log.info("pinging MN: " + node.getBaseURL());
            
            boolean pingV1 = true;
            boolean pingV2 = true;
            
            Services services = node.getServices();
            boolean foundV1Service = false;
            boolean foundV2Service = false;
            if (services == null) {
                log.error("ERROR: null services for " + node.getBaseURL());
                errors.add("ERROR: null services for " + node.getBaseURL());
            } else {
                for (Service s : services.getServiceList()) {
                    String ver = s.getVersion();
                    if (ver.equalsIgnoreCase("v1"))
                        foundV1Service = true;
                    else if (ver.equalsIgnoreCase("v2"))
                        foundV2Service = true;
                }
                if (!foundV1Service)
                    pingV1 = false;
                if (!foundV2Service)
                    pingV2 = false;
            }
            
            if (pingV1)
                try {
                    MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, "v1");
                    mn.ping();
                    log.info("success pinging v1 endpoint " + node.getBaseURL());
                } catch (Exception e) {
                    log.error("WARNING: failed pinging v1 endpoint " + node.getBaseURL());
                    errors.add("WARNING: failed pinging v1 endpoint " + node.getBaseURL());
                }
            
            if (pingV2)
                try {
                    MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), node, "v2");
                    mn.ping();
                    log.error("success pinging v2 endpoint " + node.getBaseURL());
                } catch (Exception e) {
                    log.error("WARNING: failed pinging v2 endpoint " + node.getBaseURL());
                    errors.add("WARNING: failed pinging v2 endpoint " + node.getBaseURL());
                }
        }
        
        outputResults(errors);
    }

    @Test
    @WebTestName("replication enabled on MNs")
    @WebTestDescription("Tests whether replicate is enabled on the MNs in the environment. "
            + "Compares replicate status in CN NodeList to MN's Node capabilities for consistency.")
    public void replicationEnabled() {
        
        ArrayList<String> errors = new ArrayList<String>();
        
        log.info("======================================================================");
        for (Node mNode : cnNodeList.getNodeList()) {
            if (mNode.getType() != NodeType.MN)
                continue;
            
            log.info("checking replicate capabilities of MN: " + mNode.getBaseURL());
            
            boolean replicateCnCap = mNode.isReplicate();
            if (!replicateCnCap)
                errors.add("WARNING: replication is off for MN " + mNode.getBaseURL() 
                        + " in CN NodeList (" + cns.get(0).getBaseURL() + ")");

            Node capabilities = null;
            for (Node n : mns)
                if (n.getIdentifier().equals(mNode.getIdentifier())) {
                    capabilities = n;
                    break;
                }
            
            if (capabilities == null) {
                log.error("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                errors.add("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                continue;
            }
            
            boolean replicateMnCap = capabilities.isReplicate();
            if (!replicateMnCap)
                errors.add("WARNING: replication is off for MN " + mNode.getBaseURL() 
                        + " in MN's Node capabilities.");
            
            if (replicateCnCap != replicateMnCap)
                errors.add("ERROR: replication status is inconsistent between MN (" + mNode.getBaseURL() 
                        + ") and CN (" + cns.get(0).getBaseURL() + ")");
        }
        
        outputResults(errors);
    }
    
    @Test
    @WebTestName("synchronize enabled on MNs")
    @WebTestDescription("Tests whether synchronize is enabled on the MNs in the environment. "
            + "Compares synchronize status in CN NodeList to MN's Node capabilities for consistency.")
    public void checkSynchronize() {
        
        ArrayList<String> errors = new ArrayList<String>();
        
        log.info("======================================================================");
        for (Node mNode : cnNodeList.getNodeList()) {
            if (mNode.getType() != NodeType.MN)
                continue;
            
            log.info("checking synchronize capabilities of MN: " + mNode.getBaseURL());
            
            boolean synchronizeCnCap = mNode.isSynchronize();
            if (!synchronizeCnCap)
                errors.add("WARNING: syncronize is off for MN " + mNode.getBaseURL() 
                        + " in CN NodeList (" + cns.get(0).getBaseURL() + ")");
            
            Node capabilities = null;
            for (Node n : mns)
                if (n.getIdentifier().equals(mNode.getIdentifier())) {
                    capabilities = n;
                    break;
                }
            
            if (capabilities == null) {
                log.error("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                errors.add("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                continue;
            }
            
            boolean synchronizeMnCap = capabilities.isSynchronize();
            if (!synchronizeMnCap)
                errors.add("WARNING: synchronization is off for MN " + mNode.getBaseURL() 
                        + " in MN's Node capabilities.");
            
            if (synchronizeCnCap != synchronizeMnCap)
                errors.add("ERROR: synchronize status is inconsistent between MN (" + mNode.getBaseURL() 
                        + ") and CN (" + cns.get(0).getBaseURL() + ")");
        }
        
        outputResults(errors);
    }
    
    @Test
    @WebTestName("CN / MN service consistency")
    @WebTestDescription("Tests whether the copies of the Node documents on the CN match those on the MNs "
            + "in terms of the Services.")
    public void cnMnServiceConsistency() {

        ArrayList<String> errors = new ArrayList<String>();
        
        log.info("======================================================================");
        for (Node mNode : cnNodeList.getNodeList()) {
            if (mNode.getType() != NodeType.MN)
                continue;
            
            log.info("checking Services of MN: " + mNode.getBaseURL());
            
            Services servicesCN = mNode.getServices();
            if (servicesCN == null) {
                log.error("ERROR: Services are NULL for MN " + mNode.getBaseURL());
                errors.add("ERROR: Services are NULL for MN " + mNode.getBaseURL());
            }
            
            Node capabilities = null;
            for (Node n : mns)
                if (n.getIdentifier().equals(mNode.getIdentifier())) {
                    capabilities = n;
                    break;
                }
            
            if (capabilities == null) {
                log.error("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                errors.add("ERROR: Unable to get Node capabilities from MN: " + mNode.getBaseURL());
                continue;
            }
                
            Services servicesMN = capabilities.getServices();
            if (servicesMN == null) {
                log.error("ERROR: Services are NULL for MN " + mNode.getBaseURL());
                errors.add("ERROR: Services are NULL for MN " + mNode.getBaseURL());
            }
            
            if (servicesCN == null || servicesMN == null)
                continue;
            
            HashMap<String,Boolean> servicesMapCN = new HashMap<String,Boolean>();
            HashMap<String,Boolean> servicesMapMN = new HashMap<String,Boolean>();
            
            for (Service srvMN : servicesMN.getServiceList())
                servicesMapMN.put(srvMN.getVersion() + "." + srvMN.getName(), srvMN.getAvailable());
            
            for (Service srvCN : servicesCN.getServiceList())
                servicesMapCN.put(srvCN.getVersion() + "." + srvCN.getName(), srvCN.getAvailable());
            
            for (String s : servicesMapMN.keySet())
                log.debug("MN service: " + s);
            for (String s : servicesMapCN.keySet())
                log.debug("CN service: " + s);
            
            if (!servicesMapCN.keySet().containsAll(servicesMapMN.keySet())) {
                log.warn("WARNING: the CN (" + cns.get(0).getBaseURL() + ") does not contain all Services for MN ("
                        + mNode.getBaseURL() + ") that the MN has listed in its Node document." );
                errors.add("WARNING: the CN (" + cns.get(0).getBaseURL() + ") does not contain all Services for MN ("
                        + mNode.getBaseURL() + ") that the MN has listed in its Node document." );
                
                for (String s : servicesMapMN.keySet()) {
                    if (!servicesMapCN.containsKey(s)) {
                        log.warn("The CN (" + cns.get(0).getBaseURL() + ") does not contain "
                                + "the " + s + " service from the MN (" + mNode.getBaseURL() + ")");
                        errors.add("The CN (" + cns.get(0).getBaseURL() + ") does not contain "
                                + "the " + s + " service from the MN (" + mNode.getBaseURL() + ")");
                    }
                }
            }
            
            if (!servicesMapMN.keySet().containsAll(servicesMapCN.keySet())) {
                log.warn("WARNING: the MN (" + cns.get(0).getBaseURL() + ") does not contain all Services for MN ("
                        + mNode.getBaseURL() + ") that the CN has listed.");
                errors.add("WARNING: the MN (" + cns.get(0).getBaseURL() + ") does not contain all Services for MN ("
                        + mNode.getBaseURL() + ") that the CN has listed.");
                
                for (String s : servicesMapCN.keySet()) {
                    if (!servicesMapMN.containsKey(s)) {
                        log.warn("The MN (" + mNode.getBaseURL() + ") does not contain "
                                + "the " + s + " service from the CN (" + cns.get(0).getBaseURL() + ")");
                        errors.add("The MN (" + mNode.getBaseURL() + ") does not contain "
                                + "the " + s + " service from the CN (" + cns.get(0).getBaseURL() + ")");
                    }
                }
            }
        }
        
        outputResults(errors);
    }
    
    
    private void outputResults(ArrayList<String> errors) {
        String results = "";
        if (errors.size() > 0) {
            results += "----------------------------------------------------------------------\n";
            results += "Results: \n";
            for (String string : errors) {
                results += "\t" + string + "\n";
            }
            log.error(results);
        }
        
        assertTrue("Test contained errors or warnings:\n" + results, errors.size() == 0);
    }
    
}
