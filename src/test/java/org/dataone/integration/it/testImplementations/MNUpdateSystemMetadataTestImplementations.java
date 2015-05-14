package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;

import org.dataone.integration.APITestUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v2.SystemMetadata;

public class MNUpdateSystemMetadataTestImplementations extends UpdateSystemMetadataTestImplementations {

    public MNUpdateSystemMetadataTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    protected MNCallAdapter getCallAdapter(Node node, String version) {
        return new MNCallAdapter(getSession("testRightsHolder"), node, version);
    }
    
    @WebTestName("updateSystemMetadata - tests if the call fails with an unauthorized MN certificate subject")
    @WebTestDescription("this test calls updateSystemMetadata() with a non-authoritative MN certificate subject "
            + "and expects a NotAuthorized exception to be thrown")
    public void testUpdateSystemMetadata_NotAuthorizedMN(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_NotAuthorizedMN(nodeIterator.next(), version);
    }
    
    // @Ignore'd in test class: No MN test certificates to test with at the moment
    public void testUpdateSystemMetadata_NotAuthorizedMN(Node node, String version) {
        
//        String mnSubject = no MN test certs to use;
//        CommonCallAdapter cnCertCallAdapter = new CommonCallAdapter(getSession(cnSubmitter), node, version);
//        CommonCallAdapter mnCertCallAdapter = new CommonCallAdapter(getSession(mnSubject), node, version);
//        String currentUrl = node.getBaseURL();
//        printTestHeader("testUpdateSystemMetadata_NotAuthorizedMN(...) vs. node: " + currentUrl);
//        currentUrl = cnCertCallAdapter.getNodeBaseServiceUrl();
//        
//        try {
//            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
//            Identifier pid = new Identifier();
//            pid.setValue("testUpdateSystemMetadata_NotAuthorizedMN_" + ExampleUtilities.generateIdentifier());
//            Identifier testObjPid = catc.procureTestObject(cnCertCallAdapter, accessRule, pid);
//            
//            SystemMetadata sysmeta = cnCertCallAdapter.getSystemMetadata(null, testObjPid);
//            sysmeta.setSerialVersion(sysmeta.getSerialVersion().add(BigInteger.ONE));
//            sysmeta.setDateSysMetadataModified(new Date());
//            mnCertCallAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
//            handleFail(mnCertCallAdapter.getLatestRequestUrl(), "updateSystemMetadata call should fail for a connection with unauthorized certificate");
//        } 
//        catch (NotAuthorized e) {
//            // expected
//        }
//        catch (BaseException e) {
//            handleFail(cnCertCallAdapter.getLatestRequestUrl(), "Expected a NotAuthorized exception. Got: " + 
//                    e.getClass().getSimpleName() + ": " + e.getDetail_code() + ": " + e.getDescription());
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//            handleFail(currentUrl, "Expected a NotAuthorized exception. Got: " + e.getClass().getName() + 
//                    ": " + e.getMessage());
//        }
    }
    
    @WebTestName("updateSystemMetadata - tests that the updateSystemMetadata call works for a rights holder")
    @WebTestDescription("this test calls updateSystemMetadata() using an object's rights-holder "
            + "as the certificate subject to update the metadata, "
            + "checks that the call was successful, then also uses getSystemMetadata() "
            + "to fetch the new metadata and check that for updated serialVersion and "
            + "dateSysMetadataModified")
    public void testUpdateSystemMetadata_RightsHolder(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testUpdateSystemMetadata_RightsHolder(nodeIterator.next(), version);
    }
    
    public void testUpdateSystemMetadata_RightsHolder(Node node, String version) {
        
        CommonCallAdapter callAdapter = getCallAdapter(node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testUpdateSystemMetadata_RightsHolder(...) vs. node: " + currentUrl);
        currentUrl = callAdapter.getNodeBaseServiceUrl();
        
        try {
            AccessRule accessRule = APITestUtils.buildAccessRule("testRightsHolder", Permission.CHANGE_PERMISSION);
            Identifier pid = new Identifier();
            pid.setValue("testUpdateSystemMetadata_RightsHolder_" + ExampleUtilities.generateIdentifier());
            Identifier testObjPid = catc.procureTestObject(callAdapter, accessRule, pid);
            
            SystemMetadata sysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            BigInteger newSerialVersion = sysmeta.getSerialVersion().add(BigInteger.ONE);
            Date nowIsh = new Date();
            sysmeta.setSerialVersion(newSerialVersion);
            sysmeta.setDateSysMetadataModified(nowIsh);
            boolean success = callAdapter.updateSystemMetadata(null, testObjPid , sysmeta);
            assertTrue("Call to updateSystemMetadata() should be successful.", success);
            
            SystemMetadata fetchedSysmeta = callAdapter.getSystemMetadata(null, testObjPid);
            boolean serialVersionMatches = fetchedSysmeta.getSerialVersion().equals(newSerialVersion);
            boolean dateModifiedMatches = fetchedSysmeta.getDateSysMetadataModified().equals(nowIsh);
            assertTrue("System metadata should now have updated serialVersion", serialVersionMatches);
            assertTrue("System metadata should now have updated dateSysMetadataModified", dateModifiedMatches );
        
        } catch (ServiceFailure e) {
            // ServiceFailure is an allowed outcome for the MN.updateSystemMetadata()
            if(node.getType() != NodeType.MN)
                handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                    "MN may throw ServiceFailure on a valid call to updateSystemMetadata. CN should not. " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch (SynchronizationFailed e) {
            // SynchronizationFailed is an allowed outcome for the CN.updateSystemMetadata()
            if(node.getType() != NodeType.CN)
                handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                   "CN may throw SynchronizationFailed on a valid call to updateSystemMetadata. MN should not. " +
                    e.getDetail_code() + ": " + e.getDescription());
        } catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ": " + e.getDescription());
        } catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
}
