package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
     + "and verifies that the returned InputStream does in fact contain a zip file, "
     + "then checks the zip file to make sure that it contains only one top-level "
     + "directory, and that it contains a bagit.txt and a manifest file.")
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
            
            ZipInputStream zis = null;
            boolean bagitFound = false;
            boolean manifestFound = false;
            List<String> topLevelDirs = new ArrayList<String>();
            
            try {
                zis = new ZipInputStream(is);
                ZipEntry zipEntry;

                while ((zipEntry = zis.getNextEntry()) != null) {
                    String name = zipEntry.getName();
                    System.out.println("zip entry: " + name + "     " + "size: " + zipEntry.getSize());

                    zipEntry.isDirectory();
                    
                    if (!zipEntry.isDirectory()) {
                        if (name.endsWith("bagit.txt")) {
                            bagitFound = true;
                            continue;
                        }
                        if (name.matches(".*manifest.*\\.txt")) {
                            manifestFound = true;
                            continue;
                        }
                    } else if (!zipEntry.getName().substring(0, zipEntry.getName().length() - 1).contains("/"))
                        topLevelDirs.add(name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(zis);
            }

            assertTrue("The \"application/zip\" returned from getPackage() "
                    + "should contain only ONE top level directory.", 
                    topLevelDirs.size() == 1);
            
            assertTrue("The \"application/zip\" returned from getPackage() "
                    + "should contain a bagit.txt file.", bagitFound);
            assertTrue("The \"application/zip\" returned from getPackage() "
                    + "should contain a manifest file.", manifestFound);
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
     + "and verifies that the returned InputStream does in fact contain a gzip file"
     + "then checks the gzip file to make sure that it contains a top-level "
     + "directory, and that it contains a bagit.txt and a manifest file.")
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
            
            String tempDirProperty = "java.io.tmpdir";
            String tempDir = System.getProperty(tempDirProperty);
            String outDir = tempDir + File.pathSeparator +  "tempBagItContent";
            
            GZIPInputStream gzis = null;
            FileOutputStream fout = null;
            boolean bagitFound = false;
            boolean manifestFound = false;
            
            try {
                gzis = new GZIPInputStream(is);
                fout = new FileOutputStream(outDir);
                
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    fout.write(buffer, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(gzis);
                IOUtils.closeQuietly(fout);
            }
            
            File tempBagItContent = new File(outDir);

            assertTrue("The \"application/x-gzip\" returned from getPackage() "
                    + "should contain one top level directory.", 
                    tempBagItContent.isDirectory());
            
            for (File file : tempBagItContent.listFiles()) {
                
                String name = file.getName();
                
                if (name.equals("bagit.txt")) {
                    bagitFound = true;
                    continue;
                }
                if (name.matches("manifest.*\\.txt")) {
                    manifestFound = true;
                    continue;
                }
            }
            
            assertTrue("The \"application/x-gzip\" returned from getPackage() "
                    + "should contain a bagit.txt file.", bagitFound);
            assertTrue("The \"application/x-gzip\" returned from getPackage() "
                    + "should contain a manifest file.", manifestFound);
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
