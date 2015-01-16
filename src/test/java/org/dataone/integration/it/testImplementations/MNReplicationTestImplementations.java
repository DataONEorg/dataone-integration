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
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;

public class MNReplicationTestImplementations extends ContextAwareAdapter {

    private static Log log = LogFactory.getLog(MNReplicationTestImplementations.class);
    
    
    public MNReplicationTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    public void testReplicate(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testReplicate(nodeIterator.next(), version);
    }
    
//  @Ignore("test not implemented")
    public void testReplicate(Node node, String version) {
        
    }
    
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
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
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
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testReplicate_ValidCertificate_NotCN vs. node: " + currentUrl);

        NodeReference sourceNode = new NodeReference();
        sourceNode.setValue("bad");
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier4", true);             
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
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
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(dataPackage[2], SystemMetadata.class);
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
    
    public void testSystemMetadataChanged_EarlierDate(Iterator<Node> nodeIterator, String version){
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_EarlierDate(nodeIterator.next(), version);
    }

    public void testSystemMetadataChanged_EarlierDate(Node node, String version){
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("urn:node:cnDevUNM1"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);
    
        try {
            String objectIdentifier = "TierTesting:" + 
                    catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl()) +
                    ":Public_READ" + catc.getTestObjectSeries();
            Identifier pid = catc.procurePublicReadableTestObject(callAdapter,D1TypeBuilder.buildIdentifier(objectIdentifier));


            Date modDate = callAdapter.getSystemMetadata(null, pid).getDateSysMetadataModified();
            callAdapter.systemMetadataChanged(null, pid, 10, new Date(modDate.getTime()-10000));
        }   
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " 
                    + e.getDetail_code() + ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }
    
    public void testSystemMetadataChanged_authenticatedITKuser(Iterator<Node> nodeIterator, String version){
        while (nodeIterator.hasNext())
            testSystemMetadataChanged_authenticatedITKuser(nodeIterator.next(), version);
    }
    
    public void testSystemMetadataChanged_authenticatedITKuser(Node node, String version){
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged_authITKuser() vs. node: " + currentUrl);
    
        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage("mNodeTier3",true);
    
            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            Identifier pid = callAdapter.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            Date afterCreate = new Date();
            callAdapter.systemMetadataChanged(null, pid, 10, afterCreate);
        }
        catch (NotAuthorized e) {
            // expected response
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"Expect an ITK client to receive NotAuthorized, got: " +
                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + 
                    ": " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }   
    }

    public void testSystemMetadataChanged_withCreate(Iterator<Node> nodeIterator, String version){
        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
            testSystemMetadataChanged_withCreate(nodeIterator.next(), version);
        }
    }

    public void testSystemMetadataChanged_withCreate(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession("urn:node:cnDevUNM1"), node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSystemMetadataChanged() vs. node: " + currentUrl);

        try {
            Object[] dataPackage = ExampleUtilities.generateTestSciDataPackage(
                    "mNodeTier3TestDelete", true);

            org.dataone.service.types.v1.SystemMetadata sysMetaV1 = (org.dataone.service.types.v1.SystemMetadata) dataPackage[2];
            SystemMetadata sysMetaV2 = TypeMarshaller.convertTypeFromType(sysMetaV1, SystemMetadata.class);
            Identifier pid = callAdapter.create(null, (Identifier) dataPackage[0],
                    (InputStream) dataPackage[1], sysMetaV2);

            Date afterCreate = new Date();
            callAdapter.systemMetadataChanged(null, pid, 10, afterCreate);
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "Expected InvalidToken, got: " + e.getClass().getSimpleName() + ": "
                            + e.getDetail_code() + ": " + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
}