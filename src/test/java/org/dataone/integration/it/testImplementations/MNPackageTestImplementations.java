package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;

public class MNPackageTestImplementations extends ContextAwareAdapter {

    private static final String BAGIT_ID = "application/bagit-097";
    
    public MNPackageTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    @WebTestName("getPackage - tests if the getPackage call succeeds")
    @WebTestDescription("this test calls getPackage() and verifies that a valid non-null InputStream is returned")
    public void testGetPackage(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage(nodeIterator.next(), version);
    }

    private void testGetPackage(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue(BAGIT_ID);
            
            Identifier resMapPid = catc.procureResourceMap(callAdapter, D1TypeBuilder.buildIdentifier(
                    "testGetPackage_" + ExampleUtilities.generateIdentifier()));
            is = testRightsHolderCallAdapter.getPackage(null, formatID, resMapPid);
            assertTrue("getPackage() should return a non-null InputStream", is != null);
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }

    @WebTestName("getPackage - tests if the getPackage call fails with a non-authorized certificate")
    @WebTestDescription("this test calls getPackage() with a certificate whose subject is not authorized, "
            + "expecting a NotAuthorized exception")
    public void testGetPackage_NotAuthorized(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_NotAuthorized(nodeIterator.next(), version);
    }

    private void testGetPackage_NotAuthorized(Node node, String version) {
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter personCallAdapter = new MNCallAdapter(getSession("testPerson"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage_NotAuthorized(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue(BAGIT_ID);
            
            Identifier resMapPid = catc.procureResourceMap(callAdapter, D1TypeBuilder.buildIdentifier(
                    "testGetPackage_NotAuthorized_" + ExampleUtilities.generateIdentifier()));
            is = personCallAdapter.getPackage(null, formatID, resMapPid);
            handleFail(callAdapter.getLatestRequestUrl(),"getPackage() should fail with a NotAuthorized subject");
        } 
        catch (NotFound e) {
            // expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("getPackage - tests if the getPackage call fails with an invalid packageType")
    @WebTestDescription("this test calls getPackage() with a bogus / invalid packageType, "
            + "expecting an InvalidRequest exception")
    public void testGetPackage_InvalidRequest(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_InvalidRequest(nodeIterator.next(), version);
    }

    private void testGetPackage_InvalidRequest(Node node, String version) {
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter rightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage_InvalidRequest(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("bogus/format");
            
            Identifier resMapPid = catc.procureResourceMap(callAdapter, D1TypeBuilder.buildIdentifier(
                    "testGetPackage_InvalidRequest_" + ExampleUtilities.generateIdentifier()));
            is = rightsHolderCallAdapter.getPackage(null, formatID, resMapPid);
            handleFail(callAdapter.getLatestRequestUrl(),"getPackage() should fail with an InvalidRequest for a bogus ObjectFormatIdentifier"
                    + " (" + formatID + ")");
        } 
        catch (InvalidRequest e) {
            // expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("getPackage - tests if the getPackage call fails with a non-existent pid")
    @WebTestDescription("this test calls getPackage() with a pid that doesn't exist, "
            + "expecting a NotFound exception")
    public void testGetPackage_NotFound(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_NotAuthorized(nodeIterator.next(), version);
    }

    private void testGetPackage_NotFound(Node node, String version) {
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter rightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage_NotAuthorized(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            Identifier pid = new Identifier();
            pid.setValue("bogusPid");
            
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/zip");
            
            is = rightsHolderCallAdapter.getPackage(null, formatID, pid);
            handleFail(callAdapter.getLatestRequestUrl(),"getPackage() should fail with a NotFound for a bogus pid"
                    + " (" + pid + ")");
        } 
        catch (NotFound e) {
            // expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    @WebTestName("getPackage - tests if the getPackage call fails with an unsupported format type")
    @WebTestDescription("this test calls getPackage() with a valid packageType format, but one that "
            + "does not make sense for getPackage (video/mp4), expecting a NotImplemented exception")
    public void testGetPackage_UnsupportedType(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_UnsupportedType(nodeIterator.next(), version);
    }

    private void testGetPackage_UnsupportedType(Node node, String version) {
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter rightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage_UnsupportedType(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            Identifier pid = new Identifier();
            pid.setValue("bogusPid");
            
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("video/mp4");
            
            is = rightsHolderCallAdapter.getPackage(null, formatID, pid);
            handleFail(callAdapter.getLatestRequestUrl(),"getPackage() should fail with a NotImplemented exception "
                    + "when given \"video/mp4\"");
        } 
        catch (NotImplemented e) {
            // expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(is);
        }
    }
}
