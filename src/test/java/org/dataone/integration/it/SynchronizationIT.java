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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.UnsupportedType;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.EncodingUtilities;
import org.junit.Ignore;
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
public class SynchronizationIT extends ContextAwareTestCaseDataone {
	private static final String cn_id = "cn-dev";
    private static final String TEST_CN_URL = "http://cn-dev.dataone.org/cn";
	//private static final String cn_Url = "http://cn-dev.dataone.org/cn/";
	// mn1 needs to be a node that supports login, create, get and meta
	// TODO: unobfuscate urls when allowed to put test data into knb-mn
    private static final String mn1_id = "http://___knb-mn.ecoinformatics.org";
	private static final String mn1_Url = "http://___knb-mn.ecoinformatics.org/knb/";
//	private static final String mn1_id = "unregistered";
//	private static final String mn1_Url = "http://cn-dev.dataone.org/knb/d1/";
	
	private static final int pollingFrequencySec = 5;	
	// as of jan20, 2011, dev nodes on a 5 minute synchronize cycle
	private static final int synchronizeWaitLimitSec = 7 * 60;
	
	private String currentUrl;
/* other mn info	
	http://dev-dryad-mn.dataone.org
	http://dev-dryad-mn.dataone.org/mn/

	http://daacmn.dataone.utk.edu
	http://daacmn.dataone.utk.edu/mn/

*/
	
	private static final String prefix = "synch:testID";
	
//	@Rule 
//	public ErrorCollector errorCollector = new ErrorCollector();
	
    
    /**
     * test the unit test harness
     */
    @Test
    public void testTrue()
    {
 
    }

	/**
	 * Naive test of metadata Replication to the CNs
	 * Because of the synch schedule, want to test all methods in the same test, instead of using multiple
	 * synch runs.
	 * @throws NotFound 
	 */
	@Ignore("takes too long")
	@Test
	public void testMDSynchronizeNewData() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata, NotFound 
	{
		CNode cn = D1Client.getCN();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = new MNode(currentUrl);  
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testing synchronization for node: " + currentUrl);

			log.info("current time is: " + new Date());
			Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
			log.info("fromDate is: " + fromDate);
		
		
		// create new object on MN
			Identifier pid = ExampleUtilities.doCreateNewObject(mn, prefix);

			// poll resolve until the new object is found;
			int callsPassing = 0;
			int elapsedTimeSec = 0;
			boolean testsRemain = true;
			boolean resolveTodo = true;
			boolean metaTodo = true;
			boolean searchTodo = true;
			while (testsRemain && (elapsedTimeSec <= synchronizeWaitLimitSec))
			{

				if (resolveTodo) {
					try {
						if (ExampleUtilities.countLocationsWithResolve(cn,pid) > 0) {
							resolveTodo = false;
							System.out.println("...resolve call succeeded");
							callsPassing++;
						}
					} catch (NotFound e) {
						resolveTodo = true;
					}
				}
				if (metaTodo) {
					try {
						SystemMetadata s = cn.getSystemMetadata(null,pid);
						metaTodo = false;
						System.out.println("...meta call succeeded");
						callsPassing++;
					} catch (NotFound e) {
						metaTodo = true;
					} 
				}
				if (searchTodo) {
					ObjectList ol = cn.search(null,QUERYTYPE_SOLR,
							"query="+EncodingUtilities.encodeUrlQuerySegment(pid.getValue()));
					if (ol.getCount() > 0) {
						searchTodo = false;
						System.out.println("...search call succeeded");
						callsPassing++;
					}
				}
				Thread.sleep(pollingFrequencySec * 1000); // millisec's
				elapsedTimeSec += pollingFrequencySec;
				System.out.println(" = = time elapsed: " + elapsedTimeSec);

				testsRemain = resolveTodo && metaTodo && searchTodo; 
			} 
			assertTrue("synchronize succeeded at least partially on " + cn_id, callsPassing > 0);
			assertTrue("synchronize succeeded fully on" + cn_id, callsPassing == 4);
		}
	}

	
	
	/**
	 * For each mn, resolve each Identifier in the ObjectList returned from the mn.
	 * (The objectList filters out any objects within 5 minutes of test running time.)
	 * Fails if NotFound is returned from any of the resolve calls. 
	 * @throws ServiceFailure
	 */
	@Ignore("thorough, but expensive - calls resolve against all objects")
	@Test
	public void testAllObjectsSynchronized_viaResolve() throws ServiceFailure
	{
		// just to be somebody other than public
		setupClientSubject("testRightsHolder");
		
		CNode cn = D1Client.getCN();
		
		Iterator<Node> it = getMemberNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			MNode mn = new MNode(currentUrl);  
			currentUrl = mn.getNodeBaseServiceUrl();
			printTestHeader("testing synchronization for node: " + currentUrl);

			try {
				Date now = new Date();
				Date toDate = new Date(now.getTime() - 5 * 60 * 1000);
				ObjectList ol = APITestUtils.pagedListObjects(mn, null, toDate, null, null, null, null);
				log.info("  total objects at T-5min = " + ol.getCount() );
				if (ol.getCount() > 0) {		
					boolean hasExceptions = false;
					HashMap<String,Integer> resTable = new HashMap<String,Integer>();
					for (ObjectInfo oi : ol.getObjectInfoList()) {

						String result = null;
						try {
							ObjectLocationList oll = cn.resolve(null, oi.getIdentifier());
							result = "resolved";
						} catch (BaseException be) {
							if (!(be instanceof NotAuthorized))
								hasExceptions = true;
							result = be.getClass().getSimpleName();
							log.info("exception: " + result + ": " + be.getDescription());
						}
						int y = resTable.containsKey(result) ? resTable.get(result).intValue() : 0;
						resTable.put(result,new Integer(y+1));
					}
					
					StringBuffer results = new StringBuffer();
					for (String result : resTable.keySet()) {
						results.append(result + " = " + resTable.get(result) + "; ");
					}
					log.info("results: " + results.toString());
					if (hasExceptions) {
						handleFail(currentUrl,"not all objects on the mn could be cn.resolved: " + results.toString());
					} 
				}
			} catch (BaseException be) {
				handleFail(currentUrl,"problem getting ObjectList");
			} 
			
		}
//			log.info("current time is: " + new Date());
//			Date fromDate = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
//			log.info("fromDate is: " + fromDate);
//		
//		
//		// create new object on MN
//			Identifier pid = ExampleUtilities.doCreateNewObject(mn, prefix);
//
//			// poll resolve until the new object is found;
//			int callsPassing = 0;
//			int elapsedTimeSec = 0;
//			boolean testsRemain = true;
//			boolean resolveTodo = true;
//			boolean metaTodo = true;
//			boolean searchTodo = true;
//			while (testsRemain && (elapsedTimeSec <= synchronizeWaitLimitSec))
//			{
//
//				if (resolveTodo) {
//					try {
//						if (ExampleUtilities.countLocationsWithResolve(cn,pid) > 0) {
//							resolveTodo = false;
//							System.out.println("...resolve call succeeded");
//							callsPassing++;
//						}
//					} catch (NotFound e) {
//						resolveTodo = true;
//					}
//				}
//				if (metaTodo) {
//					try {
//						SystemMetadata s = cn.getSystemMetadata(null,pid);
//						metaTodo = false;
//						System.out.println("...meta call succeeded");
//						callsPassing++;
//					} catch (NotFound e) {
//						metaTodo = true;
//					} 
//				}
//				if (searchTodo) {
//					ObjectList ol = cn.search(null,QUERYTYPE_SOLR,
//							"query="+EncodingUtilities.encodeUrlQuerySegment(pid.getValue()));
//					if (ol.getCount() > 0) {
//						searchTodo = false;
//						System.out.println("...search call succeeded");
//						callsPassing++;
//					}
//				}
//				Thread.sleep(pollingFrequencySec * 1000); // millisec's
//				elapsedTimeSec += pollingFrequencySec;
//				System.out.println(" = = time elapsed: " + elapsedTimeSec);
//
//				testsRemain = resolveTodo && metaTodo && searchTodo; 
//			} 
//			assertTrue("synchronize succeeded at least partially on " + cn_id, callsPassing > 0);
//			assertTrue("synchronize succeeded fully on" + cn_id, callsPassing == 4);
//		}
	}
	
	
	private Set<Identifier> buildIdentifierSet(ObjectList ol) {
		Set<Identifier> idSet = new HashSet<Identifier>();
		for (ObjectInfo oi: ol.getObjectInfoList()) {
			idSet.add(oi.getIdentifier());
		}
		return idSet;
	}
	
	@Test
	public void testAllObjectsSynchronized_via_ListObjects()
	{

		setupClientSubject("testRightsHolder");
		
		CNode cn = null;
		try {
			cn = D1Client.getCN();
			Date now = new Date();
			Date toDate = new Date(now.getTime() - 10 * 60 * 1000);
			ObjectList cnList = APITestUtils.pagedListObjects(cn, null, null, null, null, null, null);
			Set<Identifier> cnIdentifierSet = buildIdentifierSet(cnList);
			log.info("Size CN Objectlist = " + cnList.getCount());
			Iterator<Node> it = getMemberNodeIterator();
			while (it.hasNext()) {
				currentUrl = it.next().getBaseURL();
				MNode mn = new MNode(currentUrl);  
				currentUrl = mn.getNodeBaseServiceUrl();
				printTestHeader("testing synchronization for node: " + currentUrl);

				try {
					ObjectList ol = APITestUtils.pagedListObjects(mn, null, toDate, null, null, null, null);
					log.info("  object count for mn at T-10min = " + ol.getCount() );
					Set<Identifier> mnIds = buildIdentifierSet(ol);
					
					checkTrue(mn.getNodeBaseServiceUrl(),"The objects returned from mn.listObjects(where toDate='T-10min') should all be" +
							" contained in the cn's objectList", cnIdentifierSet.containsAll(mnIds));
					
				} catch (BaseException be) {
					handleFail(mn.getLatestRequestUrl(),"problem getting an ObjectList from the mn");
				} catch (NullPointerException npe) {
					handleFail(mn.getLatestRequestUrl(),"NPE thrown comparing cn identifiers to mn identifiers");
				}
			}
		}
		catch (BaseException be) {
			String baseurl = cn != null ? cn.getNodeBaseServiceUrl() : "default CN";
			handleFail(baseurl,"problem getting an ObjectList from the cn");
		}
	}
	
	
	
	
	/**
	 * Naive test of metadata Replication to the CNs
	 * Using getSystemMetadata call to detect successful synchronization  
	 */
//	@Test
	public void testMDSynchronizeWithMeta() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		Session token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// synchronization is a regularly schedule event self-determined by the CN
		// so there is nothing to trigger, just wait

		// poll get until found or tired of waiting
		

		int elapsedTimeSec = 0;
		boolean notFound = true;
		while (notFound && (elapsedTimeSec <= synchronizeWaitLimitSec ))
		{			
			notFound = false;
			try {
				SystemMetadata smd = cn.getSystemMetadata(token, pid);
			} catch (NotFound e) {
				notFound = true;
				// expect a notfound until replication completes
			}
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("Metadata synchronized to the CN " + cn_id, !notFound);
	}

	/**
	 * A naive test of synchronization, using resolve to poll for the existence of an object 
	 * on a Coordinating Node. 
	 * 
	 */
//	@Test
	public void testMDSynchronizeWithResolve() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// poll resolve until the new object is found;
		int count = 0;
		int elapsedTimeSec = 0;
		boolean notFound = true;
		while (count == 0 && (elapsedTimeSec <= synchronizeWaitLimitSec))
		{
			count = ExampleUtilities.countLocationsWithResolve(cn,pid);
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object resolved on " + cn_id, count > 0);
	}
	
	
	/**
	 * Naive test of metadata Replication (synchronization) to the CNs
	 * Using the search function to test success  
	 */
//	@Test
	public void testMDSynchronizeWithSearch() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		Session token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// poll get until found or tired of waiting

		int elapsedTimeSec = 0;
		int count = 0;
		while (count == 0 && (elapsedTimeSec <= synchronizeWaitLimitSec ))
		{			
			ObjectList ol = cn.search(token, QUERYTYPE_SOLR,
					"query=" + EncodingUtilities.encodeUrlQuerySegment(pid.getValue()));
			count = ol.getCount();
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("Metadata synchronized to the CN " + cn_id, count > 0);
	}

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}

}						
