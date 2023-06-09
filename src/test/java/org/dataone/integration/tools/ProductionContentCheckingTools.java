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

package org.dataone.integration.tools;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.dataone.client.v1.CNode;
import org.dataone.integration.APITestUtils;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.dataone.service.util.DateTimeMarshaller;
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
public class ProductionContentCheckingTools  {

	private String currentUrl;	
//	@Rule 
//	public ErrorCollector errorCollector = new ErrorCollector();
	
    
    /**
     * test the unit test harness
     */
    @Test
    public void testTrue()
    {
 
    }


    
	private Set<String> buildSerializedObjectInfoSet(ObjectList ol) {
		
		Set<String> oiSet = new HashSet<String>();

		for (ObjectInfo oi: ol.getObjectInfoList()) {
			oiSet.add(
					String.format("%s\t%s\t%s\t%d\t%s\t%s",
			    			oi.getIdentifier().getValue(),
			    			oi.getFormatId().getValue(),
			    			DateTimeMarshaller.serializeDateToUTC(oi.getDateSysMetadataModified()),
			    			oi.getSize(),
			    			oi.getChecksum().getAlgorithm(),
			    			oi.getChecksum().getValue())
					);
		}
		return oiSet;
	}
	
	
	private Set<String> buildSerializedObjectInfoSet_cs(ObjectList ol) {
		
		Set<String> oiSet = new HashSet<String>();

		for (ObjectInfo oi: ol.getObjectInfoList()) {
			oiSet.add(
					String.format("%s\t%s\t%d\t%s\t%s\t%s",
							oi.getChecksum().getAlgorithm(),
			    			oi.getChecksum().getValue(),
			    			oi.getSize(),
			    			oi.getIdentifier().getValue(),
			    			oi.getFormatId().getValue(),
			    			DateTimeMarshaller.serializeDateToUTC(oi.getDateSysMetadataModified())
			    			)		
					);
		}
		return oiSet;
	}

	
	@Test
	public void reportObjectInfoDifferences() 
	{			
		try {
			ObjectList ol = null;
			HashMap<String,Set<String>> olMap = new HashMap<String,Set<String>>();
			Set<String> superSet = new TreeSet<String>();
			
			CNode rrCn = new CNode("https://cn.dataone.org/cn");
			Set<Node> cnSet = NodelistUtil.selectNodes(rrCn.listNodes(),NodeType.CN);
			for (Node n : cnSet) {
				String url = n.getBaseURL();
				// skip the round-robin cn
				if (url.equals("https://cn.dataone.org/cn"))
					continue;
				System.out.println(url + ": doing listObjects()");
				
				Set<String> oiSet = new HashSet<String>(89);
				try { 
					CNode cn = new CNode(url);
					
					int runningTotal = 0;
					int olTotal = 0;
					int i = 0;
					while (runningTotal < olTotal || olTotal == 0) {
						ol = cn.listObjects(null, null, null, null, runningTotal, 1000);
						olTotal = ol.getTotal();
						runningTotal += ol.getObjectInfoList().size();
						for (ObjectInfo oi : ol.getObjectInfoList()) {
							String entry = String.format("%s\t%s\t%s\t%d\t%s\t%s",
					    			oi.getIdentifier().getValue(),
					    			oi.getFormatId().getValue(),
					    			oi.getDateSysMetadataModified(),
					    			oi.getSize(),
					    			oi.getChecksum().getAlgorithm(),
					    			oi.getChecksum().getValue());
							i++;
							if (!oiSet.add(entry))
								System.out.println("   duplicate entry: " + i + " " + entry);
						}
						
						System.out.println("  running total: " + runningTotal);
					}
					olMap.put(url, oiSet);
					superSet.addAll(oiSet);
					System.out.println(String.format("cn: %s   ol size: %d",url, olMap.get(url).size()));
				} 
				catch (BaseException be) {
					System.out.println("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
				}
			}
			Object[] urls = olMap.keySet().toArray();
			
			System.out.println();
			for (int i=0; i<urls.length; i++) {
				System.out.println((char) (i+65) + " = " + urls[i]);
			}
			System.out.println();
			
			Iterator<String> it = superSet.iterator();
			while (it.hasNext()) {
				String coi = it.next();
				
				String membership = "";
				for (int i=0; i<urls.length; i++) {
					char c = olMap.get(urls[i]).contains(coi) ?  (char) (i+65) : '-';
					membership += c;
				}
				if (membership.contains("-")) {
					System.out.println(membership + "\t" + coi.toString());
				}
				membership = "";
			}

		}
		catch (BaseException be) {
			fail("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
		}
	}
	
	
	
	
	/**
	 * this test is sensitive to the size of the CN objectList, memory-wise
	 */
	@Test
	public void testAllObjectsSynchronized_via_ListObjects()
	{

		CNode rrCn = null;
		try {
			rrCn = new CNode("https://cn.dataone.org/cn");
			
			Set<Node> cnSet = NodelistUtil.selectNodes(rrCn.listNodes(),NodeType.CN);
			
			Date now = new Date();
			Date toDate = new Date(now.getTime() - 10 * 60 * 1000);

			System.out.println("date: " + now);
			HashMap<String,Set<String>> olMap = new HashMap<String,Set<String>>();
			Set<String> superSet = new TreeSet<String>();
			for (Node n : cnSet) {
				String url = n.getBaseURL();
				// skip the round-robin cn
				if (url.equals("https://cn.dataone.org/cn"))
					continue;
				System.out.println(url + ": doing listObjects()");
				try { 
					CNode cn = new CNode(url);
					Set<String> oiSet = buildSerializedObjectInfoSet(
						APITestUtils.pagedListObjects(cn, null, null, null, null, null, null));
					olMap.put(url, oiSet);
					superSet.addAll(oiSet);
					System.out.println(String.format("cn: %s   ol size: %d",url, olMap.get(url).size()));
				} 
				catch (BaseException be) {
					System.out.println("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
				}
			}
			
			Object[] urls = olMap.keySet().toArray();
			
			System.out.println();
			for (int i=0; i<urls.length; i++) {
				System.out.println((char) (i+65) + " = " + urls[i]);
			}
			System.out.println();
			
			Iterator<String> it = superSet.iterator();
			while (it.hasNext()) {
				String coi = it.next();
				
				String membership = "";
				for (int i=0; i<urls.length; i++) {
					char c = olMap.get(urls[i]).contains(coi) ?  (char) (i+65) : '-';
					membership += c;
				}
				if (membership.contains("-")) {
					System.out.println(membership + "\t" + coi.toString());
				}
				membership = "";
			}

		}
		catch (BaseException be) {
			String baseurl = rrCn != null ? rrCn.getNodeBaseServiceUrl() : "default CN";
			fail("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
		}
	}
	
	/**
	 * this test is sensitive to the size of the CN objectList, memory-wise
	 */
	@Test
	public void testAllObjectsSynchronized_via_ListObjects_cs() throws NotImplemented, ServiceFailure
	{

		CNode rrCn = null;
//		try {
			rrCn = new CNode("https://cn.dataone.org/cn");
			
			Set<Node> cnSet = NodelistUtil.selectNodes(rrCn.listNodes(),NodeType.CN);
			
			Date now = new Date();
			Date toDate = new Date(now.getTime() - 10 * 60 * 1000);

			System.out.println("date: " + now);
			HashMap<String,Set<String>> olMap = new HashMap<String,Set<String>>();
			Set<String> superSet = new TreeSet<String>();
			for (Node n : cnSet) {
				String url = n.getBaseURL();
				// skip the round-robin cn
				if (url.equals("https://cn.dataone.org/cn"))
					continue;
				System.out.println(url + "\n... doing listObjects()");
				try { 
					CNode cn = new CNode(url);
					ObjectList ol = APITestUtils.pagedListObjects(cn, null, null, null, null, null, null);
					System.out.println("... building serializedObjectInfo set");
					Set<String> oiSet = buildSerializedObjectInfoSet_cs(ol);
					olMap.put(url, oiSet);
					superSet.addAll(oiSet);
					System.out.println(String.format("cn: %s   ol size: %d",url, olMap.get(url).size()));
				} 
				catch (BaseException be) {
					be.printStackTrace();
					System.out.println("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
				}
			}
			
			Object[] urls = olMap.keySet().toArray();
			
			System.out.println();
			for (int i=0; i<urls.length; i++) {
				System.out.println((char) (i+65) + " = " + urls[i]);
			}
			System.out.println();
			
			Iterator<String> it = superSet.iterator();
			while (it.hasNext()) {
				String coi = it.next();
				
				String membership = "";
				for (int i=0; i<urls.length; i++) {
					char c = olMap.get(urls[i]).contains(coi) ?  (char) (i+65) : '-';
					membership += c;
				}
				if (membership.contains("-")) {
					System.out.println(membership + "\t" + coi.toString());
				}
				membership = "";
			}

//		}
//		catch (BaseException be) {
//			be.printStackTrace();
//			String baseurl = rrCn != null ? rrCn.getNodeBaseServiceUrl() : "default CN";
//			fail("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
//		}
	}
	
	
	/**
	 * this test is sensitive to the size of the CN objectList, memory-wise
	 */
	@Test
	public void testListObjectsUniqueness()
	{

		CNode rrCn = null;
		try {
			rrCn = new CNode("https://cn.dataone.org/cn");
			
			Set<Node> cnSet = NodelistUtil.selectNodes(rrCn.listNodes(),NodeType.CN);
			
			Date now = new Date();
			Date toDate = new Date(now.getTime() - 10 * 60 * 1000);

			System.out.println("date: " + now);
			HashMap<String,Set<String>> olMap = new HashMap<String,Set<String>>();
			Set<String> superSet = new TreeSet<String>();
			for (Node n : cnSet) {
				String url = n.getBaseURL();
				// skip the round-robin cn
				if (url.equals("https://cn.dataone.org/cn"))
					continue;
				System.out.println(url + ": doing listObjects()");
				try { 
					CNode cn = new CNode(url);
					List<ObjectInfo> oiList = 
						APITestUtils.pagedListObjects(cn, null, null, null, null, null, null).getObjectInfoList();
					
					System.out.println("  ObjectList size = " + oiList.size());
					
					List<String> idList = new ArrayList<String>();
					List<String> csList = new ArrayList<String>();
					for (ObjectInfo oi : oiList) {
						idList.add(oi.getIdentifier().getValue());	
						csList.add(oi.getChecksum().getAlgorithm() + ":" + oi.getChecksum().getValue());
					}
					Map<String,Integer> idCardMap = CollectionUtils.getCardinalityMap(idList);
					Map<String,Integer> csCardMap = CollectionUtils.getCardinalityMap(csList);
					
					System.out.println("  # identifiers = " + idCardMap.size());
					
					for (String key : idCardMap.keySet()) {
						if (idCardMap.get(key) > 1) {
							System.out.println(String.format("    %d of %s", idCardMap.get(key), key));
						}
					}
					
					System.out.println("  # checksums = " + csCardMap.size());
					for (String key : csCardMap.keySet()) {
						if (csCardMap.get(key) > 1) {
							System.out.println(String.format("    %d of %s", csCardMap.get(key), key));
						}
					}
				} 
				catch (BaseException be) {
					System.out.println("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
				}
			}
			
			Object[] urls = olMap.keySet().toArray();
			
			System.out.println();
			for (int i=0; i<urls.length; i++) {
				System.out.println((char) (i+65) + " = " + urls[i]);
			}
			System.out.println();
			
			Iterator<String> it = superSet.iterator();
			while (it.hasNext()) {
				String coi = it.next();
				
				String membership = "";
				for (int i=0; i<urls.length; i++) {
					char c = olMap.get(urls[i]).contains(coi) ?  (char) (i+65) : '-';
					membership += c;
				}
				if (membership.contains("-")) {
					System.out.println(membership + "\t" + coi.toString());
				}
				membership = "";
			}

		}
		catch (BaseException be) {
			String baseurl = rrCn != null ? rrCn.getNodeBaseServiceUrl() : "default CN";
			fail("problem getting an ObjectList from the cn. " + be.getClass() + ": " + be.getDescription());
		}
	}
	
	
	


}						
