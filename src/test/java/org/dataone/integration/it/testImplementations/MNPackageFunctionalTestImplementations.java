package org.dataone.integration.it.testImplementations;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.integration.webTest.WebTestDescription;
import org.dataone.integration.webTest.WebTestName;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

public class MNPackageFunctionalTestImplementations extends ContextAwareAdapter {

    public MNPackageFunctionalTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
    }

    private static Log log = LogFactory.getLog(MNPackageFunctionalTestImplementations.class);
    
    @WebTestName("getPackage - zip")
    @WebTestDescription(
     "The test calls getPackage() with application/zip as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a zip file, "
     + "then checks the zip file to make sure that it contains only one top-level "
     + "directory, and that it contains a bagit.txt, a manifest file, and a data directory.")
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
            
            Identifier resourceMapPid = catc.procureResourceMap(callAdapter);
            is = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", is != null);
            
            // check if it did in fact return application/zip
            
            ZipInputStream zis = null;
            boolean bagitFound = false;
            boolean manifestFound = false;
            boolean dataDirFound = false;
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
                    } else { // directory
                        String substr = zipEntry.getName().substring(0, zipEntry.getName().length() - 1);
                        
                        // check if the zipEntry is a top-level directory
                        if (!substr.contains("/")) {
                            topLevelDirs.add(name);
                        
                        // check if it's the /data/ directory (in top-level dir)
                        } else if (StringUtils.countMatches(substr, "/") == 1 &&
                                zipEntry.getName().matches(".*/data/")) {
                            
                            dataDirFound = true;
                        }
                    }
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
            assertTrue("The \"application/zip\" returned from getPackage() "
                    + "should contain a /data directory right inside the "
                    + "top-level directory.", dataDirFound);
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
     + "directory, and that it contains a bagit.txt, a manifest file, and a data directory.")
    public void testGetPackage_Gzip(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Gzip(nodeIterator.next(), version);        
    }
    
    private void testGetPackage_Gzip(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream pkgInStream = null;
        OutputStream gzipOutFile = null;
        File tempBagItContent = null;
        
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/x-gzip");
            
            Identifier resourceMapPid = catc.procureResourceMap(callAdapter);
            pkgInStream = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", pkgInStream != null);
            
            String tempDirProperty = "java.io.tmpdir";
            String tempDir = System.getProperty(tempDirProperty);
            String rootDirPath = tempDir + File.pathSeparator +  "tempBagItContent";
            String gzipFilePath = rootDirPath + File.pathSeparator +  "TempGzip.gz";
            
            try {
                gzipOutFile = new FileOutputStream(gzipFilePath);
                IOUtils.copy(pkgInStream, gzipOutFile);
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Gzip : Unable to write gzip stream to file!"
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            File rootDir = new File(rootDirPath);
            File gzFile = new File(gzipFilePath);
            File tarFile = new File(rootDirPath + "TempTar.tar");
            
            try {
                unGzip(gzFile, rootDir);
            } catch (IOException e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Gzip : Unable to un-gzip the file!"
                + e.getClass().getName() + ": " + e.getMessage());
            }
            
            checkTar("application/x-gzip", currentUrl, rootDir, tarFile);
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
            IOUtils.closeQuietly(pkgInStream);
            IOUtils.closeQuietly(gzipOutFile);
            try {
                FileUtils.deleteDirectory(tempBagItContent);
            } catch (Exception e) {}
        }
    }
    
    @WebTestName("getPackage - bzip2")
    @WebTestDescription(
     "The test calls getPackage() with application/x-bzip2 as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a gzip file"
     + "then checks the bzip2 file to make sure that it contains a top-level "
     + "directory, and that it contains a bagit.txt, a manifest file, and a data directory.")
    public void testGetPackage_Bzip2(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Bzip2(nodeIterator.next(), version);        
    }
    
    private void testGetPackage_Bzip2(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream pkgInStream = null;
        OutputStream bzipOutFile = null;
        File tempBagItContent = null;
        
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/x-bzip2");
            
            Identifier resourceMapPid = catc.procureResourceMap(callAdapter);
            pkgInStream = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", pkgInStream != null);
            
            String tempDirProperty = "java.io.tmpdir";
            String tempDir = System.getProperty(tempDirProperty);
            String rootDirPath = tempDir + File.pathSeparator +  "tempBagItContent";
            String bzipFilePath = rootDirPath + File.pathSeparator +  "TempBzip.bz2";
            
            try {
                bzipOutFile = new FileOutputStream(bzipFilePath);
                IOUtils.copy(pkgInStream, bzipOutFile);
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Bzip2 : Unable to write bzip stream to file!"
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            File rootDir = new File(rootDirPath);
            File gzFile = new File(bzipFilePath);
            File tarFile = new File(rootDirPath + "TempTar.tar");
            
            try {
                unBzip(gzFile, rootDir);
            } catch (IOException e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Bzip2 : Unable to un-bzip the file!"
                + e.getClass().getName() + ": " + e.getMessage());
            }
            
            checkTar("application/x-bzip2", currentUrl, rootDir, tarFile);
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
            IOUtils.closeQuietly(pkgInStream);
            IOUtils.closeQuietly(bzipOutFile);
            try {
                FileUtils.deleteDirectory(tempBagItContent);
            } catch (Exception e) {}
        }
    }

    private void checkTar(String formatId, String currentUrl, File rootDir, File tarFile) {
        
        try {
            unTar(tarFile, rootDir);
        } catch (IOException | ArchiveException e) {
            e.printStackTrace();
            handleFail(currentUrl, "Unable to un-tar the file!"
                    + e.getClass().getName() + ": " + e.getMessage());
        }
        
        checkBagItContent(formatId, rootDir);
    }

    /**
     * Checks that the BagIt content we extracted does in fact match the 
     * BagIt requirements. Should be given the folder we extracted to,
     * where we're expecting one top-level directory containing the rest
     * of the content.
     */
    private void checkBagItContent(String formatId, File extractDir) {
        
        if (!extractDir.exists())
            throw new AssertionError("Directory does not exist: " + extractDir.getParent());
        
        File[] listFiles = extractDir.listFiles();
        
        assertTrue("The \"" + formatId + "\" returned from getPackage() "
                + "should contain only ONE top level directory.", 
                listFiles.length != 1);
        
        checkBagItRootDir(formatId, listFiles[0]);
    }
    
    /**
     * Checks that the top-level BagIt directory given contains the 
     * files required by the BagIt spec: a manifest, a bagit.txt file, 
     * and a /data directory.
     */
    private void checkBagItRootDir(String formatId, File rootDir) {
        
        boolean bagitFound = false;
        boolean manifestFound = false;
        boolean dataDirFound = false;
        
        for (File f : rootDir.listFiles()) {
            String path = f.getPath();
            path = path.replaceAll("\\\\", "/");
            
            if (path.endsWith("bagit.txt")) {
                bagitFound = true;
                continue;
            }
            if (path.matches(".*manifest.*\\.txt")) {
                manifestFound = true;
                continue;
            }
            
            String remove = rootDir.getPath().replaceAll("\\\\", "/");
            String pathFromRoot = path.replaceAll(remove, "");
            
            if (StringUtils.countMatches(pathFromRoot, "/") == 1 &&
                    pathFromRoot.endsWith("/data"))
                dataDirFound = true;
        }
        
        assertTrue("The \"" + formatId + "\" returned from getPackage() "
                + "should contain a bagit.txt file.", bagitFound);
        assertTrue("The \"" + formatId + "\" returned from getPackage() "
                + "should contain a manifest file.", manifestFound);
        assertTrue("The \"" + formatId + "\" returned from getPackage() "
                + "should contain a /data directory right inside the "
                + "top-level directory.", dataDirFound);
    }
    
    @WebTestName("getPackage - tar")
    @WebTestDescription(
     "The test calls getPackage() with application/x-tar as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a tar file"
     + "then checks the tar file to make sure that it contains a top-level "
     + "directory, and that it contains a bagit.txt, a manifest file, and a data directory.")
    public void testGetPackage_Tar(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Tar(nodeIterator.next(), version);        
    }
    
    private void testGetPackage_Tar(Node node, String version) {

        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream pkgInStream = null;
        OutputStream tarOutFile = null;
        File tempBagItContent = null;
        
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/x-tar");
            
            Identifier resourceMapPid = catc.procureResourceMap(callAdapter);
            pkgInStream = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", pkgInStream != null);
            
            String tempDirProperty = "java.io.tmpdir";
            String tempDir = System.getProperty(tempDirProperty);
            String rootDirPath = tempDir + File.pathSeparator + "tempBagItContent";
            String tarFilePath = rootDirPath + File.pathSeparator + "TempTar.tar";
            
            try {
                tarOutFile = new FileOutputStream(tarFilePath);
                IOUtils.copy(pkgInStream, tarOutFile);
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Tar : Unable to write gzip stream to file!"
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            File rootDir = new File(rootDirPath);
            File tarFile = new File(tarFilePath);
            
            checkTar("application/x-bzip2", currentUrl, rootDir, tarFile);
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
            IOUtils.closeQuietly(pkgInStream);
            IOUtils.closeQuietly(tarOutFile);
            try {
                FileUtils.deleteDirectory(tempBagItContent);
            } catch (Exception e) {}
        }
    }
    
    private static File unGzip(final File inFile, final File outDir) throws FileNotFoundException, IOException {

        log.info(String.format("Un-gzipping %s to dir %s.", inFile.getAbsolutePath(), outDir.getAbsolutePath()));

        File outFile = new File(outDir, inFile.getName().substring(0, inFile.getName().length() - 3));

        GZIPInputStream is = new GZIPInputStream(new FileInputStream(inFile));
        FileOutputStream os = new FileOutputStream(outFile);

        IOUtils.copy(is, os);

        is.close();
        os.close();

        return outFile;
    }
    
    private static File unBzip(final File inFile, final File outDir) throws IOException {
    
        log.info(String.format("Un-bzipping %s to dir %s.", inFile.getAbsolutePath(), outDir.getAbsolutePath()));

        File outFile = new File(outDir, inFile.getName().substring(0, inFile.getName().length() - 3));

        FileInputStream is = new FileInputStream(inFile);
        FileOutputStream os = new FileOutputStream(outFile);
        BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(is);
        
        IOUtils.copy(bzIn, os);
        
        os.close();
        bzIn.close();
        
        return outFile;
    }
    
    private static List<File> unTar(File inFile, File outDir) throws FileNotFoundException, IOException, ArchiveException {
        log.info(String.format("Un-taring %s to dir %s.", inFile.getAbsolutePath(), outDir.getAbsolutePath()));

        final List<File> resultFiles = new LinkedList<File>();
        final InputStream is = new FileInputStream(inFile); 
        final TarArchiveInputStream debIS = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null;
        while ((entry = (TarArchiveEntry)debIS.getNextEntry()) != null) {
            final File outFile = new File(outDir, entry.getName());
            if (entry.isDirectory()) {
                log.info(String.format("Writing output directory %s.", outFile.getAbsolutePath()));
                if (!outFile.exists()) {
                    log.info(String.format("Creating output directory %s.", outFile.getAbsolutePath()));
                    if (!outFile.mkdirs())
                        throw new IllegalStateException(String.format("Failed to create directory! %s.", outFile.getAbsolutePath()));
                }
            } else {
                log.info(String.format("Creating output file %s.", outFile.getAbsolutePath()));
                final OutputStream outFS = new FileOutputStream(outFile); 
                IOUtils.copy(debIS, outFS);
                outFS.close();
            }
            resultFiles.add(outFile);
        }
        debIS.close(); 

        return resultFiles;
    }

    @WebTestName("getPackage - rar")
    @WebTestDescription(
     "The test calls getPackage() with application/x-rar-compressed as the requested packageType "
     + "and verifies that the returned InputStream does in fact contain a rar file"
     + "then checks the rar file to make sure that it contains a top-level "
     + "directory, and that it contains a bagit.txt, a manifest file, and a data directory.")
    public void testGetPackage_Rar(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testGetPackage_Rar(nodeIterator.next(), version);   
    }
    
    private void testGetPackage_Rar(Node node, String version) {
        
        MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, version);
        MNCallAdapter testRightsHolderCallAdapter = new MNCallAdapter(getSession("testRightsHolder"), node, version);
        String currentUrl = node.getBaseURL();
        printTestHeader("testGetPackage(...) vs. node: " + currentUrl);
        
        InputStream pkgInStream = null;
        OutputStream rarOutFile = null;
        File tempBagItContent = null;
        
        try {
            ObjectFormatIdentifier formatID = new ObjectFormatIdentifier();
            formatID.setValue("application/x-rar-compressed");
            
            Identifier resourceMapPid = catc.procureResourceMap(callAdapter);
            pkgInStream = testRightsHolderCallAdapter.getPackage(null, formatID, resourceMapPid);
            
            // check for valid InputStream
            
            assertTrue("getPackage() should return a non-null InputStream", pkgInStream != null);
            
            String tempDirProperty = "java.io.tmpdir";
            String tempDir = System.getProperty(tempDirProperty);
            String rootDirPath = tempDir + File.pathSeparator + "tempBagItContent";
            String rarFilePath = rootDirPath + File.pathSeparator + "TempRar.rar";
            
            try {
                rarOutFile = new FileOutputStream(rarFilePath);
                IOUtils.copy(pkgInStream, rarOutFile);
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Rar : Unable to write rar stream to file!"
                        + e.getClass().getName() + ": " + e.getMessage());
            }
            
            File rootDir = new File(rootDirPath);
            File rarFile = new File(rarFilePath);

            try {
                unRar(rarFile, rootDir);
            } catch (Exception e) {
                e.printStackTrace();
                handleFail(currentUrl, "testGetPackage_Rar : Unable to un-rar the file!"
                + e.getClass().getName() + ": " + e.getMessage());
            }
            
            checkBagItContent("application/x-rar-compressed", rootDir);
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
            IOUtils.closeQuietly(pkgInStream);
            try {
                FileUtils.deleteDirectory(tempBagItContent);
            } catch (Exception e) {}
        }
    }

    private static void unRar(File inFile, final File outDir) {
        
        Archive archive = null;
        try {
            archive = new Archive(inFile);
        } catch (Exception e) {
            log.error("Unable to read in RAR archive file: " + inFile.getAbsolutePath(), e);
        }
        
        if (archive == null)
            throw new AssertionError("Unable to create Archive from file: " + inFile.getAbsolutePath());
        if (archive.isEncrypted())
            throw new AssertionError("RAR archive is encrypted! Can't be decompressed!");
        
        FileHeader fileHeader = null;
        
        while ( (fileHeader = archive.nextFileHeader()) != null )
            if (archive.isEncrypted()) {
                log.error("RAR file header (" + fileHeader.getFileNameString() 
                        + ") is encrypted! Can't be decompressed!");
                continue;
            }
        
        log.info("Extracting file: " + fileHeader.getFileNameString());
        try {
            if (fileHeader.isDirectory())
                createDirectory(fileHeader, outDir);
            else {
                File f = createFile(fileHeader, outDir);
                OutputStream stream = new FileOutputStream(f);
                archive.extractFile(fileHeader, stream);
                stream.close();
            }
        } catch (Exception e) {
            log.error("Error extracting file!", e);
        }      
    }

    private static File createFile(FileHeader fh, File destination) {
        File f = null;
        String name = null;
        if (fh.isFileHeader() && fh.isUnicode()) {
            name = fh.getFileNameW();
        } else {
            name = fh.getFileNameString();
        }
        f = new File(destination, name);
        if (!f.exists()) {
            try {
                f = makeFile(destination, name);
            } catch (IOException e) {
                log.error("Error creating new file: " + f.getName(), e);
            }
        }
        return f;
    }

    private static File makeFile(File destination, String name) throws IOException {
        String[] dirs = name.split("\\\\");
        if (dirs == null) {
            return null;
        }
        String path = "";
        int size = dirs.length;
        if (size == 1) {
            return new File(destination, name);
        } else if (size > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                path = path + File.separator + dirs[i];
                new File(destination, path).mkdir();
            }
            path = path + File.separator + dirs[dirs.length - 1];
            File f = new File(destination, path);
            f.createNewFile();
            return f;
        } else {
            return null;
        }
    }

    private static void createDirectory(FileHeader fh, File destination) {
        File f = null;
        if (fh.isDirectory() && fh.isUnicode()) {
            f = new File(destination, fh.getFileNameW());
            if (!f.exists()) {
                makeDirectory(destination, fh.getFileNameW());
            }
        } else if (fh.isDirectory() && !fh.isUnicode()) {
            f = new File(destination, fh.getFileNameString());
            if (!f.exists()) {
                makeDirectory(destination, fh.getFileNameString());
            }
        }
    }

    private static void makeDirectory(File destination, String fileName) {
        String[] dirs = fileName.split("\\\\");
        if (dirs == null) {
            return;
        }
        String path = "";
        for (String dir : dirs) {
            path = path + File.separator + dir;
            new File(destination, path).mkdir();
        }

    }

}
