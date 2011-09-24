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

package org.dataone.integration.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Test;

/**
 * Test the DataONE Java client methods.
 * @author Rob Nahf
 */
public class MNodeTier2IT extends ContextAwareTestCaseDataone  {

    private static final String TEST_MN_ID = "c3p0";
    private static String format_text_csv = "text/csv";
    private static String format_eml_200 = "eml://ecoinformatics.org/eml-2.0.0";
    private static String format_eml_201 = "eml://ecoinformatics.org/eml-2.0.1";
    private static String format_eml_210 = "eml://ecoinformatics.org/eml-2.1.0";
    private static String format_eml_211 = "eml://ecoinformatics.org/eml-2.1.1";

    private static final String idPrefix = "mnTier1:";
    private static final String bogusId = "foobarbaz214";

    private static String currentUrl;

    

	@Override
	protected String getTestDescription() {
		return "Test Case that runs through the Member Node Tier 2 API (Authorization) methods";
	}

	
 
	@Test
	public void testIsAuthorized() {
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testIsAuthorized() vs. node: " + currentUrl);
		
			try {
				// should be a valid Identifier
				Identifier pid = mn.listObjects().getObjectInfo(0).getIdentifier();
				boolean success = mn.isAuthorized(null, pid, Permission.READ);
				checkTrue(currentUrl,"isAuthorized response cannot be false. [Only true or exception].", success);
			} 
			catch (BaseException e) {
				handleFail(currentUrl,e.getDescription());
			}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}	
		}
	}
	
	
    @Test
	public void testSetAccessPolicy() {
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = D1Client.getMN(currentUrl);
			printTestHeader("testSetAccessPolicy() vs. node: " + currentUrl);

			try {
				// create the identifier
				Identifier guid = new Identifier();
				guid.setValue("mNodeTier2TestSetAccessPolicy." + ExampleUtilities.generateIdentifier());

				// get some data bytes as an input stream
				ByteArrayInputStream textPlainSource = 
					new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));

				// build the system metadata object
				SystemMetadata sysMeta = 
					ExampleUtilities.generateSystemMetadata(guid, "text/plain", textPlainSource, null);

				// make the submitter the same as the cert DN 
				try {
					String ownerDN = CertificateManager.getInstance().loadCertificate().getSubjectDN().toString();
					String ownerX500 = CertificateManager.getInstance().loadCertificate().getSubjectX500Principal().toString();
					sysMeta.getRightsHolder().setValue(ownerX500);
				} catch (Exception e) {
					// warn about this?
					e.printStackTrace();
				}
			      
				// create a test object
				Identifier pid = mn.create(null, guid, textPlainSource, sysMeta);
				assertEquals(guid, pid);

				// set access on the object
				boolean success = mn.setAccessPolicy(null, pid, buildPublicReadAccessPolicy());
				assertTrue(success);

				// TODO: check the access by switching users or trashing the cert
				// mn.isAuthorized(session, pid, Permission.READ);

			} catch (BaseException e) {
				handleFail(currentUrl, e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}

}
