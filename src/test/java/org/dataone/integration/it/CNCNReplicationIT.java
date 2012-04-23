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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.dataone.client.CNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.junit.Test;

/**
 * Want to test the CNs' ability to replicate data around themselves.
 * 
 *  
 * @author rnahf
 *
 */
public class CNCNReplicationIT extends ContextAwareTestCaseDataone {

	
	@Override
	protected String getTestDescription() {
		return "Test the CNs' ability to replicate data around themselves";
	}

	@Test 
	public void testCNObjectListConsistency() {

		HashMap<String,Set<String>> objectListMap = 
			new HashMap<String,Set<String>>();
		
		// get all CN instances and get the objectList
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			Node node = (Node) it.next();
			String currentUrl = node.getBaseURL();
			
//			if (currentUrl.contains("cn-dev-2"))
//				continue;

			CNode cn = new CNode(currentUrl);
					
			try {
				ObjectList ol = cn.listObjects(null);
				Set<String> objectSet = new TreeSet<String>();
				for (ObjectInfo oi: ol.getObjectInfoList()) {
					String serializedInfo = String.format("%s:%s:%s;%s",
							oi.getIdentifier().getValue(),
							oi.getFormatId().getValue(),
							oi.getChecksum().getValue(),
							oi.getDateSysMetadataModified().toString());

					objectSet.add(serializedInfo);
				}
				objectListMap.put(currentUrl, objectSet);
			} catch (BaseException e) {
				handleFail(currentUrl,"problem performing listObjects:" + 
						e.getClass().getSimpleName() + ": " +
						e.getDescription());
			}
		}
	
		// create the superset of objects
		Set<String> allObjects = null;
		for (String nodeStr: objectListMap.keySet()) {
			if (allObjects == null) {
				allObjects = objectListMap.get(nodeStr);
			} else {
				allObjects.addAll(objectListMap.get(nodeStr));
			}
		}
		
		// check individual lists against the superset
		int c = 0;

		for (String nodeStr: objectListMap.keySet()) {
			log.info("Checking all objects against node: " + nodeStr );
			Vector<String> missingFromNode = new Vector<String>();
;
			for (String serializedInfo: allObjects) {
//				log.debug(c++);
				if (!objectListMap.get(nodeStr).contains(serializedInfo)) {
					missingFromNode.add(serializedInfo);
				}
			}
			String example = missingFromNode.size() > 0 ? missingFromNode.firstElement() : null;
			checkEquals(nodeStr, "node should not be missing any objects " +
					"(missing " + missingFromNode.size() + ", example: " +  example + ")",
					String.valueOf(objectListMap.get(nodeStr).size()),
					String.valueOf(allObjects.size()));
		}
	}
	
	
}
