/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataone.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.D1Client;
import org.dataone.client.D1Object;
import org.dataone.client.DataPackage;
import org.dataone.client.MNode;
import org.dataone.eml.DataoneEMLParser;
import org.dataone.eml.EMLDocument;
import org.dataone.eml.EMLDocument.DistributionMetadata;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Checksum;
import org.dataone.service.types.ChecksumAlgorithm;
import org.dataone.service.types.DescribeResponse;
import org.dataone.service.types.Event;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.Log;
import org.dataone.service.types.LogEntry;
import org.dataone.service.types.Node;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectInfo;
import org.dataone.service.types.ObjectList;
import org.dataone.service.types.SystemMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test the DataONE Java client methods.
 * @author Matthew Jones
 */
public class PackageTest  {

    private static final String TEST_MN_URL = "http://cn-dev.dataone.org/knb/d1/";
    private static final String TEST_MN_ID = "http://cn-dev.dataone.org/knb/d1";
    private static final String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
    private static final String password = "kepler";
    private static final String prefix = "knb:testid:";

    private List<Node> nodeList = null;
    private static String currentUrl;
    //set this to false if you don't want to use the node list to get the urls for 
    //the test.  
        
    @Rule 
    public ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setUp() throws Exception 
    {
        if (nodeList == null || nodeList.size() == 0) {
            nodeList = new Vector<Node>();
            Node n = new Node();
            n.setBaseURL(TEST_MN_URL);
            nodeList.add(n);
        }
    }
    
    /**
     * test creation of a D1Object and its download
     */
    @Test
    public void testD1Object() {
        for (int i = 0; i < nodeList.size(); i++) {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testD1Object - node " + nodeList.get(i).getBaseURL());
            checkTrue(true);
            AuthToken token;
            try {
                token = mn.login(principal, password);

                Identifier id = createDataObject(mn, token);

                D1Object d1o = new D1Object(id);
                checkTrue(d1o != null);
                checkEquals(id.getValue(), d1o.getIdentifier().getValue());
            } catch (BaseException e) {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testD1Object: " + e.getClass().getName() + ": "
                        + e.getMessage()));
            }
        }
    }
    
    @Test
    public void testDataPackage() {
        for (int i = 0; i < nodeList.size(); i++) {
            currentUrl = nodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testDataPackage - node " + nodeList.get(i).getBaseURL());
            checkTrue(true);
            AuthToken token;
            try {
                token = mn.login(principal, password);

                Identifier id = createDataObject(mn, token);

                DataPackage dp = new DataPackage(id);
                checkTrue(dp != null);
                checkEquals(id.getValue(), dp.get(id).getIdentifier().getValue());
                Set<Identifier> identifiers = dp.identifiers();
                for (Identifier current_id : identifiers) {
                    D1Object o = dp.get(current_id);
                    ObjectFormat fmt = o.getType();
                    byte[] data = o.getData();
                    System.out.println(current_id.getValue() + ": " + fmt + " (" + data.length + ")");
                }
            } catch (BaseException e) {
                errorCollector.addError(new Throwable(createAssertMessage() + "" +
                		" unexpected error in testD1Object: " + e.getClass().getName() + ": "
                        + e.getMessage()));
            }
        }
    }
    
    /**
     * Create a test data object on the given member node, and return the Identifier that was created for that object.
     * @param mn Member node on which to create the object
     * @param token a valid authentication token
     * @return the Identifer of the created object
     */
    private Identifier createDataObject(MNode mn, AuthToken token) {
        String idString = prefix + ExampleUtilities.generateIdentifier();
        Identifier guid = new Identifier();
        guid.setValue(idString);
        InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-cdr.329066.1.data");
        SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream, TEST_MN_ID);
        objectStream = this.getClass().getResourceAsStream(
            "/d1_testdocs/knb-lter-cdr.329066.1.data");
        Identifier rGuid = null;
        try {
            rGuid = mn.create(token, guid, objectStream, sysmeta);
            mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
            checkEquals(guid.getValue(), rGuid.getValue());
        } catch (Exception e) {
            errorCollector.addError(new Throwable(createAssertMessage() + 
                    " error in testCreateData: " + e.getClass() + ": " + e.getMessage()));
        }
        return rGuid;
    }
    
    private static String createAssertMessage()
    {
        return "test failed at url " + currentUrl;
    }
    
    private void printHeader(String methodName)
    {
        System.out.println("\n***************** running test for " + methodName + " *****************");
    }
    
    private void checkEquals(final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat("assertion failed for host " + currentUrl, s1, is(s2));
                //assertThat("assertion failed for host " + currentUrl, s1, is(s2 + "x"));
                return null;
            }
        });
    }
    
    private void checkTrue(final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat("assertion failed for host " + currentUrl, true, is(b));
                return null;
            }
        });
    }
}
