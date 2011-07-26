package org.dataone.integration.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.util.EncodingUtilities;
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
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.QueryType;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

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
public class SynchronizationIT {
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
	
	
/* other mn info	
	http://dev-dryad-mn.dataone.org
	http://dev-dryad-mn.dataone.org/mn/

	http://daacmn.dataone.utk.edu
	http://daacmn.dataone.utk.edu/mn/

*/
	
	private static final String prefix = "synch:testID";
	
	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();
	
    
    /**
     * test the unit test harness
     */
    @Test
    public void testHarness()
    {
        assertTrue(true);
        assertEquals("1", "1");
    }

	/**
	 * Naive test of metadata Replication to the CNs
	 * Because of the synch schedule, want to test all methods in the same test, instead of using multiple
	 * synch runs.
	 * @throws NotFound 
	 */
	@Ignore("test not adapted for v0.6.x")
	@Test
	public void testMDSynchronizeWithMulti() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata, NotFound 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		Session token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// poll resolve until the new object is found;
		int callsPassing = 0;
		int elapsedTimeSec = 0;
		boolean testsRemain = true;
		boolean resolveTodo = true;
		boolean getTodo = true;
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
			if (getTodo) {
				try {
					InputStream is = cn.get(token,pid);
					getTodo = false;
					System.out.println("...get call succeeded");
					callsPassing++;
				} catch (NotFound e) {
					getTodo = true;
				} 
			}
			if (metaTodo) {
				try {
					SystemMetadata s = cn.getSystemMetadata(token,pid);
					metaTodo = false;
					System.out.println("...meta call succeeded");
					callsPassing++;
				} catch (NotFound e) {
					metaTodo = true;
				} 
			}
			if (searchTodo) {
				ObjectList ol = cn.search(token,QueryType.SOLR,
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
		
			testsRemain = resolveTodo && getTodo && metaTodo && searchTodo; 
		} 
		assertTrue("synchronize succeeded at least partially on " + cn_id, callsPassing > 0);
		assertTrue("synchronize succeeded fully on" + cn_id, callsPassing == 4);
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
			ObjectList ol = cn.search(token, QueryType.SOLR,
					"query=" + EncodingUtilities.encodeUrlQuerySegment(pid.getValue()));
			count = ol.getCount();
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("Metadata synchronized to the CN " + cn_id, count > 0);
	}

}						
