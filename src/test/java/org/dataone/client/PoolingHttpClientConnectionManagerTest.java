package org.dataone.client;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.CNode;
import org.dataone.client.v1.MNode;
import org.dataone.client.v1.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PoolingHttpClientConnectionManagerTest {

    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void test() {
        ;
    }

 //   @Test
    public void testMultipleGets_D1Client_singleThread() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        CNode cn = D1Client.getCN("https://cn-dev.test.dataone.org/cn");
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null);
        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        int c = 0;
        for (ObjectInfo oi : ol.getObjectInfoList()) {
            if (c % 10 == 0) {;
                System.out.print(String.format("%2d. ", c));
            }
            c++;
            System.out.print(oi.getIdentifier().getValue());
            System.out.print(", ");
            InputStream is = null;
            try {
                try {
                    is = cn.get(null, oi.getIdentifier());
                }
                catch (InvalidToken | NotAuthorized | NotFound | NotImplemented e) {
                   System.out.println(e.toString());
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

    }


 //   @Test
    public void testMultipleGets_D1Client_MN() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        MNode mn = D1Client.getMN("https://mn-demo-5.test.dataone.org/knb/d1/mn");
        ObjectList ol = mn.listObjects(null, null, null, null, null, null, null);
        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        int c = 0;
        for (ObjectInfo oi : ol.getObjectInfoList()) {
            if (c % 10 == 0) {;
                System.out.print(String.format("%2d. ", c));
            }
            c++;
            System.out.print(oi.getIdentifier().getValue());
            System.out.print(", ");
            InputStream is = null;
            try {
                try {
                    is = mn.get(null, oi.getIdentifier());
                }
                catch (InvalidToken | NotAuthorized | NotFound | NotImplemented | InsufficientResources  e) {
                   System.out.println(e.toString());
                }

            } finally {
                IOUtils.closeQuietly(is);
            }
        }

    }

//   @Test
    public void testMultipleGets_D1Client_CN_shortClientTimeout() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        Settings.getConfiguration().setProperty("D1Client.D1Node.get.timeout",5000);
        CNode cn = D1Client.getCN("https://cn-dev.test.dataone.org/cn");
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null);
        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        int c = 0;
        for (ObjectInfo oi : ol.getObjectInfoList()) {
            if (c % 10 == 0) {;
                System.out.print(String.format("%2d. ", c));
            }
            c++;
            System.out.print(oi.getIdentifier().getValue());
            System.out.print(", ");
            InputStream is = null;
            try {
                try {
                    is = cn.get(null, oi.getIdentifier());
                }
                catch (InvalidToken | NotAuthorized | NotFound | NotImplemented   e) {
                   System.out.println(e.toString());
                }

            } finally {
                IOUtils.closeQuietly(is);
            }
        }

    }

//    @Test
    public void testMultipleGets_D1Client_MN_shortClientTimeout() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        Settings.getConfiguration().setProperty("D1Client.D1Node.get.timeout",500);
        MNode cn = D1Client.getMN("https://mn-demo-5.test.dataone.org/knb/d1/mn");
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null);
        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        int c = 0;
        for (ObjectInfo oi : ol.getObjectInfoList()) {
            if (c % 10 == 0) {;
                System.out.print(String.format("%2d. ", c));
            }
            c++;
            System.out.print(oi.getIdentifier().getValue());
            System.out.print(", ");
            InputStream is = null;
            try {
                try {
                    is = cn.get(null, oi.getIdentifier());
                }
                catch (ServiceFailure | InvalidToken | NotAuthorized | NotFound | NotImplemented | InsufficientResources   e) {
                   System.out.println(e.toString());
                }

            } finally {
                IOUtils.closeQuietly(is);
            }
        }

    }


//    @Test
    public void testMultipleGets_D1Client_MultiNodes_shortClientTimeout() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        Settings.getConfiguration().setProperty("D1Client.D1Node.get.timeout",500);
        CNode cn = D1Client.getCN("https://cn-dev.test.dataone.org/cn");
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null);
        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        int c = 0;
        for (ObjectInfo oi : ol.getObjectInfoList()) {
            if (c % 10 == 0) {;
                System.out.print(String.format("%2d. ", c));
            }
            c++;
            System.out.print(oi.getIdentifier().getValue());
            System.out.print(", ");
            InputStream is = null;
            try {
                try {
                    ObjectLocationList oll = cn.resolve(oi.getIdentifier());
                    is = getFromLocation(oll);
                }
                catch (ServiceFailure | InvalidToken | NotAuthorized | NotFound | NotImplemented e) {
                   System.out.println(e.toString());
                }

            } finally {
                IOUtils.closeQuietly(is);
            }
        }

    }

    private InputStream getFromLocation(ObjectLocationList oll) {

        InputStream stream = null;
        for (ObjectLocation ol : oll.getObjectLocationList()) {
            try {
                try {
                    MNode mn = D1Client.getMN(ol.getBaseURL());
                    stream = mn.get(oll.getIdentifier());
                }
                catch (InvalidToken | NotAuthorized | NotImplemented
                        | ServiceFailure | NotFound | InsufficientResources e) {
                    ;
                }
                break;
            } finally {}
        }
        return stream;
    }


}
