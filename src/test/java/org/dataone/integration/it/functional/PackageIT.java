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

package org.dataone.integration.it.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.v1.itk.D1Client;
import org.dataone.client.v1.itk.D1Object;
import org.dataone.client.v1.itk.DataPackage;
import org.dataone.client.v1.MNode;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Matthew Jones
 */
public class PackageIT extends ContextAwareTestCaseDataone {

    private static final String TEST_MN_ID = "c3p0";
    private static final String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
//    private static final String password = "kepler";
    private static final String prefix = "knb:testid:";

    private static String currentUrl;
        
    
    /**
     * test creation of a D1Object and its download
     */
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testD1Object() {
//        for (int i = 0; i < memberNodeList.size(); i++) {
//            currentUrl = memberNodeList.get(i).getBaseURL();
//            MNode mn = D1Client.getMN(currentUrl);
//
//            printHeader("testD1Object - node " + memberNodeList.get(i).getBaseURL());
//            checkTrue(true);
//            Session token;
////            try {
//                token = null;//mn.login(principal, password);
//
//                Identifier id = createDataObject(mn, token);
//                checkTrue(id != null);
//                
//
//                D1Object d1o = new D1Object(id);
//
//                checkTrue(d1o != null);
//                checkEquals(id.getValue(), d1o.getIdentifier().getValue());
////            } catch (BaseException e) {
////                errorCollector.addError(new Throwable(createAssertMessage() + 
////                        " unexpected error in testD1Object: " + e.getClass().getName() + ": "
////                        + e.getMessage()));
////            }
//        }
    }
    
	@Ignore("test not adapted for v0.6.x")
    @Test
    public void testDataPackage() throws IOException, ServiceFailure {
        for (int i = 0; i < memberNodeList.size(); i++) {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testDataPackage - node " + memberNodeList.get(i).getBaseURL());
            checkTrue(true);
            Session token;
//            try {
                token = null;//mn.login(principal, password);

                Identifier id = createDataObject(mn, token);
                checkTrue(id != null);
                
                DataPackage dp = new DataPackage(id);
                checkTrue(dp != null);
                checkEquals(id.getValue(), dp.get(id).getIdentifier().getValue());
                Set<Identifier> identifiers = dp.identifiers();
                for (Identifier current_id : identifiers) {
                    D1Object o = dp.get(current_id);
                    ObjectFormatIdentifier fmtid = o.getFmtId();
                    byte[] data = o.getData();
                    if (data != null) {
                        System.out.println(current_id.getValue() + ": " + fmtid + " (" + data.length + ")");
                    } else {
                         System.out.println(current_id.getValue() + ": " + fmtid + "(null data)");
                    }
                }
//            } catch (BaseException e) {
//                errorCollector.addError(new Throwable(createAssertMessage() + "" +
//                		" unexpected error in testD1Object: " + e.getClass().getName() + ": "
//                        + e.getMessage()));
//            }
        }
    }
    
    /**
     * XXX TODO  fix this at some  point!!! when we have separate dev mn's and cn's we can
     * count on
     * 
     * this test will not work because it is attempting to call update on the
     * coordinating node rather than on a membernode
     *
     * we have not identifier for cn-dev running as a member node
     * though that is how we are performing tests right now, subtly using
     * cn dev as a membernode.
     * However, this test is attempting to pull out the correct url from
     * from a real nodeList rather than from a configured node properties
     * So, the node's, c3p0, baseURL is pointing to the CN rather than metacat
     * and cn's do not create!
     *
     * test creation of a D1Object and its call to create()
     * @throws ServiceFailure 
     */
	@Ignore("test not adapted for v0.6.x - and see comment for method")
	@Test
    public void testD1ObjectManualCreate() throws ServiceFailure {
        for (int i = 0; i < memberNodeList.size(); i++) {
            currentUrl = memberNodeList.get(i).getBaseURL();
            MNode mn = D1Client.getMN(currentUrl);

            printHeader("testD1ObjectManualCreate - node " + memberNodeList.get(i).getBaseURL());
            checkTrue(true);
            Session token;
            try {
                token = null; //mn.login("uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg", password);

                Identifier id = new Identifier();
                id.setValue(prefix + ExampleUtilities.generateIdentifier());
                InputStream objectStream = this.getClass().getResourceAsStream(
                    "/d1_testdocs/knb-lter-cdr.329066.1.data");
                byte[] data = IOUtils.toByteArray(objectStream);
//                String[] describes = {"j.1.1", "j.2.1"};
//                String[] describedBy = {};
                D1Object d1o = new D1Object(id, data, "text/csv", principal, TEST_MN_ID);
                checkTrue(d1o != null);
                D1Client.create(token,d1o);
                d1o.setPublicAccess(token);
                
                // Now check if the object is on the MN
                SystemMetadata smCopy = mn.getSystemMetadata(token, id);
                checkEquals(id.getValue(), smCopy.getIdentifier().getValue());
                checkEquals(d1o.getFmtId().toString(), smCopy.getFormatId().toString());
                
                // Now check that create() fails if called a second time
                try {
                    System.out.println("Trying create a second time, expecting an exception due to ID conflicts...");
                    D1Client.create(token,d1o);
                    // Should not get here, because the object already exists on the MN
                } catch (IdentifierNotUnique e) {
                    checkTrue(e != null);
                }
            } catch (BaseException e) {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testD1ObjectManualCreate: " + e.getClass().getName() + ": "
                        + e.getMessage()));
            } catch (NoSuchAlgorithmException e) {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                        " unexpected error in testD1ObjectManualCreate: " + e.getClass().getName() + ": "
                        + e.getMessage()));
            } catch (IOException e) {
                errorCollector.addError(new Throwable(createAssertMessage() + 
                " unexpected error in testD1ObjectManualCreate: " + e.getClass().getName() + ": "
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
    private Identifier createDataObject(MNode mn, Session token) {
        printHeader("createDataObject - node " + mn.getNodeBaseServiceUrl());
        String idString = prefix + ExampleUtilities.generateIdentifier();
        Identifier guid = new Identifier();
        guid.setValue(idString);
        InputStream objectStream = this.getClass().getResourceAsStream(
                "/d1_testdocs/knb-lter-cdr.329066.1.data");
        SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, "text/csv", objectStream,TEST_MN_ID);
        objectStream = this.getClass().getResourceAsStream(
            "/d1_testdocs/knb-lter-cdr.329066.1.data");
        Identifier rGuid = null;
        try {
            rGuid = mn.create(token, guid, objectStream, sysmeta);
//            mn.setAccessPolicy(token, rGuid, buildPublicReadAccessPolicy());
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

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
