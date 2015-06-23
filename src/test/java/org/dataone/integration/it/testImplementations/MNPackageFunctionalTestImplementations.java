package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;

public class MNPackageFunctionalTestImplementations extends ContextAwareTestCaseDataone {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs MN package API methods and checks results for correctness.";
    }

    @WebTestName("getPackage - zip")
    @WebTestDescription(
     "The test calls getPackage() with application/zip as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a zip file")
    public void testGetPackage_Zip(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Zip(nodeIterator.next(), version);        
    }
    
    private void testGetPackage_Zip(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/zip");
            
            Identifier resourceMapPid = procureResourceMap(callAdapter);
            is = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", is != null);
            
            // check if it did in fact return application/zip
            
            try {
                boolean zipContainsEntry = false;
                
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry zipEntry;

                while ((zipEntry = zis.getNextEntry()) != null) {
                    log.info("ore pid: " + resourceMapPid + "     " 
                            + "zip entry: " + zipEntry.getName() + "     "
                            + "size: " + zipEntry.getSize());
                    zipContainsEntry = true;
                    break;
                }

                assertTrue("The \"application/zip\" returned from getPackage() "
                        + "should contain at least one entry in the zip.", zipContainsEntry);
                
            } catch (Exception e1) {
                e1.printStackTrace();
                handleFail(currentUrl, "getPackage() did not return an InputStream containing "
                        + "a package of type \"application/zip\"" 
                        + e1.getClass().getName() + ": " + e1.getMessage());
            }
            
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
    
    @WebTestName("getPackage - gzip")
    @WebTestDescription(
     "The test calls getPackage() with application/x-gzip as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a gzip file")
    public void testGetPackage_Gzip(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Gzip(nodeIterator.next(), version);        
    }
    
    private void testGetPackage_Gzip(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream is = null;
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/x-gzip");
            
            Identifier resourceMapPid = procureResourceMap(callAdapter);
            is = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", is != null);
            
            // check if it did in fact return application/x-gzip
            
            try {
                GZIPInputStream gzis = new GZIPInputStream(is);

                assertTrue("The InputStream returned from getPackage() "
                      + "should be in gzip format (application/x-gzip).", gzis != null);
                
                // gzip won't contain multiple entries
                // (unless it contains a tar file, 
                // which we can't assume given the "application/x-gzip" request)
                
            } catch (Exception e1) {
                e1.printStackTrace();
                handleFail(currentUrl, "getPackage() did not return an InputStream containing "
                        + "a package of type \"application/x-gzip\"" 
                        + e1.getClass().getName() + ": " + e1.getMessage());
            }
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
