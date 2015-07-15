package org.dataone.integration.it.testImplementations;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.exception.ClientSideException;
import org.dataone.configuration.Settings;
import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CNCallAdapter;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.SystemMetadata;
import org.jibx.runtime.JiBXException;

public class SynchronizeObjectTestImplementations extends ContextAwareAdapter {

    private static final String cnSubmitter = Settings.getConfiguration().getString("dataone.it.cnode.submitter.cn", "cnDevUNM1");
    private List<Node> mnList;
    private List<Node> cnList;
    
    private static Log log = LogFactory.getLog(SystemMetadataFunctionalTestImplementation.class);
    
    
    public SynchronizeObjectTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }
    
    public void setup(Iterator<Node> cnIter) {
        if(cnList != null && mnList != null)
            return;
        
        cnList = IteratorUtils.toList(cnIter);
        mnList = new ArrayList<Node>();
        
        CNCallAdapter cn = null;
        if(cnList.size() > 0)
            cn = new CNCallAdapter(getSession(cnSubmitter), cnList.get(0), "v2");
        if(cn != null) {
            try {
                for(Node n : cn.listNodes().getNodeList())
                    if(n.getType() == NodeType.MN)
                        mnList.add(n);
            } catch (NotImplemented | ServiceFailure | InstantiationException
                    | IllegalAccessException | InvocationTargetException | ClientSideException
                    | JiBXException | IOException e) {
                log.warn("Unable to fetch node list from CN: " + cn.getNodeBaseServiceUrl(), e);
            }
        }

        log.info("CNs available: " + cnList.size());
        log.info("MNs available: " + mnList.size());
    }
    
    @WebTestName("testSynchronizeObject - tests if the call fails with an unauthorized certificate subject")
    @WebTestDescription("this test calls testSynchronizeObject() with the \"testPerson\" certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testSynchronizeObject_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        setup(nodeIterator);
        while (nodeIterator.hasNext())
            testSynchronizeObject_NotAuthorized(nodeIterator.next(), version);
    }
    
    public void testSynchronizeObject_NotAuthorized(Node node, String version) {
        
        CNCallAdapter cnCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronizeObject_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            // MN should have the object
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronizeObject_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(mnCallAdapter, accessRule, pid);
            
            // try CN.synchronizeObject() with mnCallAdapter cert is "testPerson"
            cnCallAdapter.synchronize(null, testObjPid);
            
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception.");
        }
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("testSynchronizeObject - tests if the call fails with a call from non-authoritative MN")
    @WebTestDescription("this test calls testSynchronizeObject() with a certificate from an non-authoritative MN "
            + "and expects a NotAuthorized exception to be thrown")
    public void testSynchronizeObject_NotAuthorizedMN(Iterator<Node> nodeIterator, String version) {
        setup(nodeIterator);
        while (nodeIterator.hasNext())
            testSynchronizeObject_NotAuthorizedMN(nodeIterator.next(), version);
    }
    
    public void testSynchronizeObject_NotAuthorizedMN(Node node, String version) {
        
        CNCallAdapter cnCallAdapter = new CNCallAdapter(getSession("mnCertSubject"), node, version);
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronizeObject_NotAuthorized(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            // MN should have the object
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronizeObject_NotAuthorized_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(mnCallAdapter, accessRule, pid);
            
            // try CN.synchronizeObject() with mnCallAdapter cert that isn't the authoritative MN  
            cnCallAdapter.synchronize(null, testObjPid);
            
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception.");
        }
        catch (NotAuthorized e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("testSynchronizeObject - tests if the call fails with a bogus pid")
    @WebTestDescription("this test calls testSynchronizeObject() with a bogus pid "
            + "and expects a NotFound exception to be thrown")
    public void testSynchronizeObject_NotFound(Iterator<Node> nodeIterator, String version) {
        setup(nodeIterator);
        while (nodeIterator.hasNext())
            testSynchronizeObject_NotFound(nodeIterator.next(), version);
    }
    
    public void testSynchronizeObject_NotFound(Node node, String version) {
        
        CNCallAdapter cnCallAdapter = new CNCallAdapter(getSession("testPerson"), node, version);
        MNCallAdapter mnCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testSynchronizeObject_NotFound(...) vs. node: " + currentUrl);
        currentUrl = cnCallAdapter.getNodeBaseServiceUrl();
        
        try {
            // MN should have the object
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testSynchronizeObject_NotFound_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(mnCallAdapter, accessRule, pid);
            
            // try CN.synchronizeObject() with a bogus pid
            testObjPid.setValue("supercalifragilisticexpialidocious");
            cnCallAdapter.synchronize(null, testObjPid);
            
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotFound exception.");
        }
        catch (NotFound e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(cnCallAdapter.getLatestRequestUrl(), "Expected a NotFound exception. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected a NotFound exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails if the pid and system metadata don't match")
    @WebTestDescription("this test calls updateSystemMetadata() with a pid and system matadata whose identifier "
            + "doesn't match, expecting an InvalidRequest exception to be thrown")
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch(Iterator<Node> nodeIterator, String version) {
        setup(nodeIterator);
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_InvalidRequest_PidMismatch(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_InvalidRequest_PidMismatch(Node node, String version) {
        
        CommonCallAdapter rightsHolderCallAdapter = new CommonCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_InvalidRequestPidMismatch(...) vs. node: " + currentUrl);
        currentUrl = rightsHolderCallAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_InvalidRequestPidMismatch" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(rightsHolderCallAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = rightsHolderCallAdapter.getSystemMetadata(null, testObjPid);
            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
            sysmeta.setDateSysMetadataModified(new Date());
            Identifier diffPid = new Identifier();
            diffPid.setValue("bogus pid");
            sysmeta.setIdentifier(diffPid);
            rightsHolderCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for invalid metadata");
        } 
        catch (InvalidRequest e) {
            // expected
        }
        catch (BaseException e) {
            handleFail(rightsHolderCallAdapter.getLatestRequestUrl(), "Expected an InvalidRequest. Got: " + 
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, "Expected an InvalidRequest exception. Got: " + e.getClass().getName() + 
                    ": " + e.getMessage());
        }
    }
    
}