package org.dataone.client;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v2.CNode;
import org.dataone.client.v2.MNode;
import org.dataone.client.v2.itk.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
    public void test_true() {
        ;
    }

//    @Test
    public void testMultipleGets_D1Client_singleThread() throws ServiceFailure, InvalidRequest, InvalidToken, NotAuthorized, NotImplemented
    {
        Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");
        CNode cn = D1Client.getCN();//"https://cn-sandbox-ucsb-2.test.dataone.org/cn");
//        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null, null);
//        System.out.println("Number of objects retrieved: " + ol.sizeObjectInfoList());
        
        String[] annIds = new String[]{"https://pasta.lternet.edu/package/metadata/eml/knb-lter-sbc/1001/7",
                "https://pasta.lternet.edu/package/metadata/eml/knb-lter-sbc/1002/6",
                "https://pasta.lternet.edu/package/metadata/eml/knb-lter-sbc/1003/6",
                "https://pasta.lternet.edu/package/metadata/eml/knb-lter-sbc/1004/6"};
        int c = 0;
//        for (ObjectInfo oi : ol.getObjectInfoList()) {
        for (String idString : annIds) {  
            ObjectInfo oi = new ObjectInfo();
            oi.setIdentifier(D1TypeBuilder.buildIdentifier(idString));
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
        ObjectList ol = mn.listObjects(null, null, null, null, null, null, null, null);
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
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null, null);
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
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null, null);
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
        ObjectList ol = cn.listObjects(null, null, null, null, null, null, null, null);
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
                    ObjectLocationList oll = cn.resolve(null, oi.getIdentifier());
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
                    stream = mn.get(null, oll.getIdentifier());
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

    @Ignore("this isn't needed as a regular test")
    @Test
    public void testFromExample() {
        Settings.getConfiguration().setProperty("D1Client.CN_URL", "https://cn-sandbox-2.test.dataone.org/cn");
        
        List<String> identifiers = new ArrayList<String>();

        System.out.println("Starting MultipleRead.main()");
        InputStream inputStream = null;
        try {
            // gather the pids to test reading
//            if (args.length > 0) {
//                // read the pids from this file URL
//                String pidFile = args[0];
                URL url = new URL("https://raw.githubusercontent.com/DataONEorg/semantic-query/master/lib/test_corpus_C_id_list.txt");
                inputStream = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    identifiers.add(line);
                }
   //         }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
           
        System.out.println("%%%%%%%%%%%%% getting all objects %%%%%%%%%%%%%%");
        // read em all
        int count = 0;
        try {
            CNode cn = D1Client.getCN();
            for (String id: identifiers) {
                InputStream emlStream = null;

                try {
                    Identifier pid = new Identifier();
                    pid.setValue(id);
                    emlStream = cn.get(null, pid);
                    //              DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    //              Document doc = builder.parse(emlStream);
                    System.out.println(IOUtils.toString(emlStream, "UTF-8").substring(0, 80) + "...");
                    count++;
                    System.out.println("Read count: " + count);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(emlStream);
                }
            }
        } catch (NotImplemented | ServiceFailure e) {
            e.printStackTrace();
        }
        
    }
}
