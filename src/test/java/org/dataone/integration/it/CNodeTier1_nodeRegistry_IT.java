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

import java.util.Iterator;

import org.dataone.client.CNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_nodeRegistry_IT extends ContextAwareTestCaseDataone {
	
	private static String currentUrl;


	   
    /**
     * tests that a valid nodelist is returned
     */
    @Test
    public void testListNodes() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testListNodes(...) vs. node: " + currentUrl);

    		try {
    			NodeList response = cn.listNodes();
    			checkTrue(cn.getLatestRequestUrl(),"listNodes(...) returns a valid NodeList object", 
    					response instanceof NodeList);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods imlemented by the nodeRegistry component";
	}
	
}
