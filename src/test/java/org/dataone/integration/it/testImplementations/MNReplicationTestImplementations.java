package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.types.v2.TypeFactory;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;

public class MNReplicationTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(MNReplicationTestImplementations.class);
    
    
    public MNReplicationTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("replicate - tests that replicate works")
    @WebTestDescription(" ... test not yet implemented ... ")
    public void testReplicate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate(nodeIterator.next(), version);
    }
    
//  @Ignore("test not implemented")
    public void testReplicate(Node node, String version) {
        
    }
    
    @WebTestName("replicate - test without a certificate")
    @WebTestDescription("this test calls replicate without a certificate, "
            + "expecting a NotAuthorized exception")
    public void testReplicate_NoCertificate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_NoCertificate(nodeIterator.next(), version);
    }

    /**
     *  Test MN.Replicate() functionality
     */
    public void testReplicate_NoCertificate(Node node, String version) {

        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);             
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2 , sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"should not be able to initiate replication without a certificate");
        }
        catch (NotAuthorized na) {
            // expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    @WebTestName("replicate - test with a valid non-CN certificate")
    @WebTestDescription("this test calls replicate with a certificate that "
            + "is valid but is not a coordinating node certificate, "
            + "expecting a NotAuthorized exception")
    public void testReplicate_ValidCertificate_NotCN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_ValidCertificate_NotCN(nodeIterator.next(), version);
    }

    /**
     *  Test MN.Replicate() functionality
     */
    public void testReplicate_ValidCertificate_NotCN(Node node, String version) {

        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testPerson"), node, version);
        Subject subject = ContextAwareTestCaseDataone.getSubject("testPerson");
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_ValidCertificate_NotCN vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true, subject.getValue());             
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2, sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"should not be able to initiate replication a certificate representing a CN");
        }
        catch (NotAuthorized na) {
            // expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    @WebTestName("replicate - test with a bad node reference")
    @WebTestDescription("this test calls replicate with a NodeReference that "
            + "is invalid, expecting an InvalidRequest exception")
    public void testReplicate_FaultyNodeReference(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate_FaultyNodeReference(nodeIterator.next(), version);
    }
    
    /**
     *  Test MN.Replicate() functionality
     */
//    @Ignore("need to create testCN certificate to run this subtest")
    public void testReplicate_FaultyNodeReference(Node node, String version) {

        String currentUrl = node.getBaseURL();
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testCN"), node, version);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_NoCertificate vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);
            SystemMetadata sysMetaV2 = TypeFactory.convertTypeFromType(dataPackage[2], SystemMetadata.class);
            callAdapter.replicate(null, sysMetaV2, sourceNode);    
            handleFail(callAdapter.getLatestRequestUrl(),"replicate call should not succeed with faulty node reference");
        }
        catch (InvalidRequest na) {
            // expected behavior ??
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expected InvalidRequest, got: " +
                    e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }

}
