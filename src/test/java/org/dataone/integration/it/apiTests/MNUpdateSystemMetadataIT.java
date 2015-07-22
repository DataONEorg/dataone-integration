package org.dataone.integration.it.apiTests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.testImplementations.MNUpdateSystemMetadataTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNUpdateSystemMetadataIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    private MNUpdateSystemMetadataTestImplementations metaImpl;
    
    private static Log log = LogFactory.getLog(MNUpdateSystemMetadataIT.class);
    
    protected String getTestDescription() {
        return "Test Case that runs MN update system metadata methods and checks results for correctness.";
    }

    @Before
    public void setup() {
        metaImpl = new MNUpdateSystemMetadataTestImplementations(this);
        metaImpl.setup(getCoordinatingNodeIterator().next());
    }
    
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        Node cNode = getCoordinatingNodeIterator().next();
        CNCallAdapter cn = new CNCallAdapter(getSession(cnSubmitter), cNode, "v2");

        List<Node> mnList = new ArrayList();
        try {
            for(Node n : cn.listNodes().getNodeList())
                if(n.getType() == NodeType.MN)
                    try {
                        MNCallAdapter mn = new MNCallAdapter(getSession(cnSubmitter), n, "v2");
                        mn.ping();  // make sure the MN is up
                        mnList.add(n);
                    } catch (Exception e) {
                        log.warn("Couldn't ping MN at " + n.getBaseURL() + ". Skipping it.", e);
                        e.printStackTrace();
                    }
                        
        } catch (Exception e) {
            log.fatal("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
        }
        
        return mnList.iterator();
    }
    
    @Test 
    @Ignore("Cannot currently test this without an MN certificate.")
    public void testUpdateSystemMetadata_NotAuthorizedMN() {
        metaImpl.testUpdateSystemMetadata_NotAuthorizedMN(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_RightsHolder() {
        metaImpl.testUpdateSystemMetadata_RightsHolder(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableRightsHolder() {
        metaImpl.testUpdateSystemMetadata_MutableRightsHolder(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableFormat() {
        metaImpl.testUpdateSystemMetadata_MutableFormat(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableAccessPolicy() {
        metaImpl.testUpdateSystemMetadata_MutableAccessPolicy(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableReplPolicy() {
        metaImpl.testUpdateSystemMetadata_MutableReplPolicy(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableAuthMN() {
        metaImpl.testUpdateSystemMetadata_MutableAuthMN(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableArchived() {
        metaImpl.testUpdateSystemMetadata_MutableArchived(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_RightsHolderNonAuthMN() {
        metaImpl.testUpdateSystemMetadata_RightsHolderNonAuthMN(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_ObsoletesFail() {
        metaImpl.testUpdateSystemMetadata_ObsoletesFail(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_ObsoletedByFail() {
        metaImpl.testUpdateSystemMetadata_ObsoletedByFail(getMemberNodeIterator(), "v2");
    }
     
    @Test 
    public void testUpdateSystemMetadata_MutableObsoletedBy() {
        metaImpl.testUpdateSystemMetadata_MutableObsoletedBy(getMemberNodeIterator(), "v2");
    }
    
    @Test 
    public void testUpdateSystemMetadata_MutableObsoletes() {
        metaImpl.testUpdateSystemMetadata_MutableObsoletes(getMemberNodeIterator(), "v2");
    }
}
