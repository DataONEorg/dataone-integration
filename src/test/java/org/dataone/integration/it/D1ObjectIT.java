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

import org.dataone.client.D1Client;
import org.dataone.client.D1Object;
import org.dataone.client.D1TypeBuilder;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * The goal here is to test synchronization of metadata created on a MN
 * with a CN.  Synchronization is a CN scheduled cron job, so there is no
 * trigger for synchronization.  We have to wait for it to happen :-)
 * The testing approach is:
 *    1. create a new data on a MN
 *    2. periodically poll the CN for a period greater than the cron interval
 *       checking for presence of the new object there using:
 *       a). getSystemMetadata
 *       b). resolve
 *       c). search
 *       
 * @author rnahf
 *
 */
public class D1ObjectIT extends ContextAwareTestCaseDataone {

	private static Set<Node> memberNodes;
//	private static NodeList nl
	
	private String currentUrl;
	
//	@Rule 
//	public ErrorCollector errorCollector = new ErrorCollector();

	
	@Override
	protected String getTestDescription() {
		return "Tests behviors of D1Object that require interaction with CN/MNs";
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
	public void testRefreshSystemMetadata() 
	{
		System.out.println("****************** testRefreshSystemMetadata()");
		Iterator<Node> memberNodes = getMemberNodeIterator();
		try {
			D1Object d = new D1Object(D1TypeBuilder.buildIdentifier("foooooo"),
					"someData".getBytes(),
					D1TypeBuilder.buildFormatIdentifier("text/plain"),
					D1TypeBuilder.buildSubject("submitterMe"),
					memberNodes.next().getIdentifier());

			assertFalse("An uncreated object should not be able to refresh",d.refreshSystemMetadata(null));
			Date start = new Date();
			System.out.println(start.toString());
			boolean outcome = d.refreshSystemMetadata(5000);
			Date end = new Date();
			System.out.println(end.toString());
			assertTrue("refresh should wait about 5000ms before returning",
					Math.abs(end.getTime() - start.getTime() - 5000) < 5000);


			start = new Date();
			System.out.println(start.toString());
			outcome = d.refreshSystemMetadata(30000);
			end = new Date();
			System.out.println(end.toString());
			assertTrue("refresh should wait about 30000ms before returning",
					Math.abs(end.getTime() - start.getTime() - 30000) < 5000);
		} 
		
		catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			fail("Failed: Exception thrown");
		}

	}

    
    
}						
