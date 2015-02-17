package org.dataone.integration.it.apiTests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.junit.Ignore;
import org.junit.Test;

public class SidMNIT extends SidCommonIT {

private Logger logger = Logger.getLogger(SidMNIT.class);
    
    @Override
    protected String getTestDescription() {
        return "Tests v2 API methods for MNs that accept SID parameters";
    }
    
    @Override
    protected Iterator<Node> getNodeIterator() {
        return getMemberNodeIterator();
    }
    
    @Test
    @Ignore("getPackage() is not yet implemented in client code")
    public void testGetPackage() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("Testing getPackage() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing getPackage(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                // TODO getPackage() not implemented in client code
                
            }
        }
    }
    
    /**
     * Sets up each pid chain scenario. First calls archive() on the PID. Doing this makes
     * it mandatory for an update() call on the same object to fail with an {@link InvalidRequest}.
     * Then it calls update() on the SID and expects to catch the {@link InvalidRequest}.
     */
    @Test
    public void testUpdate() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, NoSuchAlgorithmException, NotFound {
        logger.info("Testing update() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing update(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                // archive PID
                try {
                    callAdapter.archive(null, pid);
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", testUpdate() setup failed; couldn't archive().", false);
                }
                
                boolean invalidRequestCaught = false;
                try {
                    // update SID
                    Identifier newPid = createIdentifier("P9_", node);
                    byte[] contentBytes = ExampleUtilities.getExampleObjectOfType(DEFAULT_TEST_OBJECTFORMAT);
                    D1Object d1o = new D1Object(newPid, contentBytes,
                            D1TypeBuilder.buildFormatIdentifier(DEFAULT_TEST_OBJECTFORMAT),
                            D1TypeBuilder.buildSubject(cnSubmitter),
                            D1TypeBuilder.buildNodeReference("bogusAuthoritativeNode"));
                    SystemMetadata sysmeta = TypeMarshaller.convertTypeFromType(d1o.getSystemMetadata(), SystemMetadata.class);
                    sysmeta.setObsoletes(pid);
                    InputStream objectInputStream = new ByteArrayInputStream(contentBytes);
                    callAdapter.update(null, sid, objectInputStream, newPid, sysmeta);
                } catch (InvalidRequest ir) {
                    // expect InvalidRequest on archived object
                    invalidRequestCaught = true;
                } catch (Exception e) {
                    assertTrue("update() Case " + caseNum + ", an exception occurred while trying to update() : " +
                            e.getMessage(), false);
                    e.printStackTrace();
                }
                
                assertTrue("update() Case " + caseNum + ", InvalidRequest expected on update of archived object.", invalidRequestCaught);
            }
        }
    }
    
    @Test
    @Ignore("..... underway .....")
    public void testSystemMetadataChanged() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("Testing systemMetadataChanged() method ... ");
        
        for (int caseNum = 1; caseNum <= 18; caseNum++) {
            
            logger.info("Testing systemMetadataChanged(), Case" + caseNum);
            
            Method setupMethod = SidCommonIT.class.getDeclaredMethod("setupCase" + caseNum, CommonCallAdapter.class, Node.class);
    
            Iterator<Node> nodeIter = getNodeIterator();
            while (nodeIter.hasNext()) {
                Node node = nodeIter.next();
                MNCallAdapter callAdapter = new MNCallAdapter(getSession(cnSubmitter), node, "v2");
                IdPair idPair = (IdPair) setupMethod.invoke(this, callAdapter, node);
                Identifier sid = idPair.firstID;
                Identifier pid = idPair.secondID;
    
                // TODO test systemMetadataChanged() ...
                
                // systemMetadataChanged() implies authoritative sysmeta on CN was updated
                // so ... update sysmeta on CN
                // (make sure this my knows to look to that CN ???)
                // call systemMetadataChanged() - impl should be grabbing from CN using SID
                //                              so CN does resolving, so this tests CN =/
                // wait ... an unknown amount of time (no way to guarantee correctness here ...)
                // check MN sysmeta against sysmeta updated to CN
                
            }
        }
    }
        
        
}
