package org.dataone.integration.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.dataone.client.rest.DefaultHttpMultipartRestClient;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.junit.Test;

public class MNCoreIT {

    protected static final MultipartRestClient MULTIPART_REST_CLIENT = new DefaultHttpMultipartRestClient();

    @Test
    public void testCNv1Ping() {
        //        setupClientSubject_NoCert();
        //        Iterator<Node> it = getCoordinatingNodeIterator();
        //        while (it.hasNext()) {
        //           pingImpl(it.next(), "v1");
        //        }
    }

    @Test
    public void testCNv2Ping() {
        //        setupClientSubject_NoCert();
        //        Iterator<Node> it = getCoordinatingNodeIterator();
        //        while (it.hasNext()) {
        //           pingImpl(it.next(), "v2");
        //        }
    }

    @Test
    public void testMNv1Ping() {
        //       setupClientSubject_NoCert();
        //      Iterator<Node> it = getMemberNodeIterator();
        //      while (it.hasNext()) {
        Node n = new Node();
        n.setBaseURL("https://mn-stage-ucsb-3.test.dataone.org/knb/d1/mn/");
        n.setType(NodeType.CN);
        pingImpl(n, "v1");
        //      }
    }

    @Test
    public void testMNv2Ping() {
        //       setupClientSubject_NoCert();
        //       Iterator<Node> it = getMemberNodeIterator();
        //       while (it.hasNext()) {
        //          pingImpl(it.next(), "v2");
        //       }
    }

    private void pingImpl(Node node, String version) {
        
        String currentUrl = "";
        CommonCallAdapter callAdapter = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);
        
        currentUrl = callAdapter.getNodeBaseServiceUrl();

        try {
//          Assume.assumeTrue(APITestUtils.isTierImplemented(mn, "Tier5"));
            Date localNow = new Date();
            Date pingDate = callAdapter.ping();
            
            assertTrue(callAdapter.getLatestRequestUrl() + " ping should return a valid date", pingDate != null);
            // other invalid dates will be thrown as IOExceptions cast to ServiceFailures

            assertTrue(callAdapter.getLatestRequestUrl()
                    + " returned date should be within 1 minute of time measured on test machine",
                    pingDate.getTime() - localNow.getTime() < 1000 * 60  &&
                    localNow.getTime() - pingDate.getTime() > -1000 * 60);
            
        } 
        catch (BaseException e) {
            fail(callAdapter.getLatestRequestUrl() + " " + e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            fail(currentUrl + " " + e.getClass().getName() + ": " + e.getMessage());
        }   
        
    }

    //   @Override
    protected String getTestDescription() {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
