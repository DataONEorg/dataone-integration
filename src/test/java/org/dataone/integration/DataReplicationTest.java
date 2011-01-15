package org.dataone.integration;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * The goal here is to test CN initiated data replication between member nodes
 * We should be able to trigger a replicate command on the CN, so the naive 
 * approach taken is to:
 *    1. create a new data on a MN
 *    2. trigger a replicate command on the CN
 *    3. sleep for a bit...
 *    4. check replicaStatus in the systemMetadata
 *    5. when complete, use resolve to find new MN
 *    6. OK if retreivable from new MN (the resolve is followable)
 * @author rnahf
 *
 */
public class DataReplicationTest {

//	@Test
	public void testReplicate() 
	{
		// create new object
		
		// do resolve and count locations, should be 1.
		
		// trigger replicate command on CN
		
		// poll resolve until locations is 2 (or more);
		
		// follow the resolve link using a get
		
		// test for success
	}
	
	@Test
	public void checkTrue()
	{
		assertTrue("just checking", true);
	}
}
