package org.dataone.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectLocation;
import org.dataone.service.types.ObjectLocationList;
import org.dataone.service.types.SystemMetadata;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

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
	private static final String cnUrl = "http://cn-dev.dataone.org/cn/";
	private static final String mnUrl_1 = "http://cn-dev.dataone.org/knb/d1/";
	private static final String mnUrl_2 = "http://cn-dev.dataone.org/knb/d1/";
	 private static final String prefix = "repl:testID";

	 @Rule 
	 public ErrorCollector errorCollector = new ErrorCollector();
	 
	 @Test
	 public void testReplicate() throws ServiceFailure, NotImplemented, InterruptedException, 
	 InvalidToken, NotAuthorized, InvalidRequest 
	 {
		 // create the players
		 D1Client d1 = new D1Client(cnUrl);
		 CNode cn = d1.getCN();
		 MNode mn1 = d1.getMN(mnUrl_1);	
		 MNode mn2 = d1.getMN(mnUrl_2);

		 // create new object on MN_1

		 String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		 AuthToken token = mn1.login(principal, "kepler");
		 String idString = prefix + ExampleUtilities.generateIdentifier();
		 Identifier guid = new Identifier();
		 guid.setValue(idString);
		 InputStream objectStream = this.getClass().getResourceAsStream(
				 "/d1_testdocs/knb-lter-cdr.329066.1.data");
		 SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV);
		 Identifier rGuid = null;

		 try {
			 rGuid = mn1.create(token, guid, objectStream, sysmeta);
//			 assertTrue("checking that returned guid matches given ", guid.equals(rGuid.getValue()));
			 assertThat("checking that returned guid matches given ", guid.getValue(), is(rGuid.getValue()));
			 mn1.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
		 } catch (Exception e) {
			 errorCollector.addError(new Throwable(" error in creating data for replication test: " + e.getMessage()));
		 }

		 // do resolve and count locations, should be 1.
		 int preReplCount = -1;

		 ObjectLocationList oll;
		 try {
			 oll = cn.resolve(token, guid);
		 } catch (NotFound e) {
			 throw new ServiceFailure("1000","create not available to resolve");
		 }
		 List<ObjectLocation> locs = oll.getObjectLocationList();
		 preReplCount = locs.toArray().length;
		 System.out.println("locations before replication = " + preReplCount);


		 // issue a replicate command to MN2 to get the object
		 // this isn't a client command, so going to handcraft the call




		 // poll resolve until locations is 2 (or more);


		 int postReplCount = preReplCount;
		 int maxTries = 20;
		 int tries = 0;
		 while (postReplCount == preReplCount && tries < maxTries)
		 {
			 Thread.sleep(3000); // millisec's
			 try {
				 oll = cn.resolve(token, guid);
			 } catch (NotFound e) {
				 throw new ServiceFailure("1001","create not available to resolve");
			 }
			 locs = oll.getObjectLocationList();
			 postReplCount = locs.toArray().length;
			 System.out.println("try " + tries + ": postReplication resolve count = " + postReplCount);
			 tries++;
		 } 


		 // test the meta and get calls (to MN2)
		 try 
		 {
			 SystemMetadata smd = mn2.getSystemMetadata(null, rGuid);
		 } catch (NotFound e) {
			 fail("check meta test");
		 }

		 try {
			 InputStream response = mn2.get(token, rGuid);
		 } catch (NotFound e) {
			 fail("check get test");
		 }
	 }
	
    private void checkEquals(final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                assertThat("assertion failed", s1, is(s2));
                //assertThat("assertion failed for host " + currentUrl, s1, is(s2 + "x"));
                return null;
            }
        });
    }
    
//    private void checkTrue(final boolean b)
//    {
//        errorCollector.checkSucceeds(new Callable<Object>() 
//        {
//            public Object call() throws Exception 
//            {
//                assertThat("assertion failed for host " + currentUrl, true, is(b));
//                return null;
//            }
//        });
//    }
}
