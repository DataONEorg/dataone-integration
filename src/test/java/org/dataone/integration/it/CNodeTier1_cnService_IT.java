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
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_cnService_IT extends ContextAwareTestCaseDataone {

	private static String currentUrl;


    /**
     * This test procures an objectlist (if no objects in it, it creates one),
     * and checks for successful return of an ObjectLocationList from cn.resolve()
     * for the first item in the objectlist.  
     */
    @Test
    public void testResolve() {
    	setupClientSubject_NoCert();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testResolve(...) vs. node: " + currentUrl);

    		try {
    			ObjectList ol = procureObjectList(cn);
    			Identifier pid = null;
    			for (int i = 0; i<ol.sizeObjectInfoList(); i++) {
    				try {
    					cn.getSystemMetadata(ol.getObjectInfo(i).getIdentifier());
    					pid = ol.getObjectInfo(i).getIdentifier();
    					break;
    				} catch (BaseException be) {
    					;
    				}
    			}
    			if (pid != null) {
    				log.debug("   pid = " + pid.getValue());
    				ObjectLocationList response = cn.resolve(null,pid);
    				checkTrue(cn.getLatestRequestUrl(),"resolve(...) returns an ObjectLocationList object",
    					response != null && response instanceof ObjectLocationList);
    			}
    			else {
    				handleFail(currentUrl, "No public object available to test resolve against");
    			}
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
		return "Test the Tier1 CN methods implemented in package cn_service";
	}
	
}
