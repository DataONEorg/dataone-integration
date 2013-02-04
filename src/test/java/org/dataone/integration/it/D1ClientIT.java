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
 * 
 * $Id$
 */

package org.dataone.integration.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.mail.util.ByteArrayDataSource;

import org.dataone.client.D1Client;
import org.dataone.client.D1Object;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.client.types.ObsoletesChain;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 *       
 * @author rnahf
 *
 */
public class D1ClientIT extends ContextAwareTestCaseDataone {

	private static Set<Node> memberNodes;

	
	private String currentUrl;
	
//	@Rule 
//	public ErrorCollector errorCollector = new ErrorCollector();

	
	@Override
	protected String getTestDescription() {
		return "Tests behaviors of D1Object that require interaction with CN/MNs";
	}
    
	@BeforeClass
	public static void setup() throws NotImplemented, ServiceFailure {
		ContextAwareTestCaseDataone.failOnMissingNodes = false;
	}
    
    /**
     * test the unit test harness
     */
    @Test
    public void testTrue()
    {
 
    }

	@Test
	public void testListUpdateHistory() throws Exception 
	{
		setupClientSubject("testRightsHolder");
		Subject rightsHolder = ClientIdentityManager.getCurrentIdentity();
		
		System.out.println("****************** test getObsoletesChain()");
		Iterator<Node> storageNodes = getMemberNodeIterator();
		
		NodeList nl = new NodeList();
		while (storageNodes.hasNext()) {
			nl.addNode(storageNodes.next());
		}
		Set<Node> storageSubset = NodelistUtil.selectNodesByService(nl, "MNStorage", null, true);
		
		if ( storageSubset.isEmpty()) {
			handleFail("<no host found>","Could not find a node with MNStorage server to run test with.");
		}
		
		storageNodes = storageSubset.iterator();
		

		// try each eligible node for the create/ updates until one works
		boolean testHasRun = false;
		Exception lastException = null;
		while (storageNodes.hasNext() && !testHasRun) {
			Node mn = storageNodes.next();
			NodeReference mnRef = mn.getIdentifier();
			if (!mn.isSynchronize()) continue;
		
			System.out.println("Using node: " + mnRef.getValue());
			try {
				String basePidVal = "testD1ClientObsoletesChain." + new Date().getTime() + ".";
				System.out.println("    pid base: " + basePidVal);
				
				Date v4date = null;
				for (int v=1; v <= 5; v++) {
					D1Object d = new D1Object(D1TypeBuilder.buildIdentifier(basePidVal + v),
							new ByteArrayDataSource("someData".getBytes(),null),
							D1TypeBuilder.buildFormatIdentifier("text/plain"),
							rightsHolder,
							mnRef);
					if (v==1) {
						D1Client.create(null,d);
					} else {
						// need to set the obsoletes field in the systemMetadata
						d.getSystemMetadata().setObsoletes(D1TypeBuilder.buildIdentifier(basePidVal + (v-1)));
						D1Client.update(null,d);
					}
					if (v==4) {
						v4date = new Date();
					}
				}
				testHasRun = true;
				lastException = null;
				
				System.out.println("Pausing for synchronization...");
				Thread.sleep(3*60*1000);
				
				ObsoletesChain oc = D1Client.listUpdateHistory(D1TypeBuilder.buildIdentifier(basePidVal + 3));
				
				checkTrue(mnRef.getValue(), "should have 5 objects in the chain", oc.size() == 5);
				
				for (int i=0; i < oc.size(); i++) {
					System.out.println("  " + oc.getByPosition(i).getValue());
				}
				
				checkEquals(mnRef.getValue(), "should find the original version" , 
						oc.getOriginalVersion().getValue(),basePidVal + 1);
				checkEquals(mnRef.getValue(), "should find the latest version" , 
						oc.getLatestVersion().getValue(),basePidVal + 5);
				checkEquals(mnRef.getValue(), "should get version 4 based on asOfDate",
						oc.getVersionAsOf(v4date).getValue(),basePidVal + 4);
				
				
			} catch (Exception e) {
				e.printStackTrace();
				lastException = e;
			}
		}
		if (lastException != null)
			throw lastException;
		
		
	}

    
    
}						
