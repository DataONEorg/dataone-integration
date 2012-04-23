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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.mimemultipart.MultipartRequestHandler;
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
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Ignore;
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
public class DataReplicationIT {
	// mn1 needs to be a published node that supports login, create, get and meta
    private static final String TEST_CN_URL = "http://cn-dev.dataone.org/cn";
    // TODO: remove the obfuscating characters once allowed to put test data into knb-mn
	private static final String mn1_id = "http://___knb-mn.ecoinformatics.org";
	private static final String mn1_Url = "http://___knb-mn.ecoinformatics.org/knb/d1/";
	
	//private static final String mn1_id = "unregistered";
	//private static final String mn1_Url = "http://amasa.local:8080/knb/d1/";
	
	private static final String mn2_id = "http://mn-dev.dataone.org";
	private static final String mn2_Url = "http://amasa.local:8080/knb/d1/";
	
	private static final int replicateWaitLimitSec = 20;
	private static final int pollingFrequencySec = 5;
	
	private static final int replicateResolveWaitLimitSec = 120;

	
	
/* other mn info	
	http://dev-dryad-mn.dataone.org
	http://dev-dryad-mn.dataone.org/mn/

	http://daacmn.dataone.utk.edu
	http://daacmn.dataone.utk.edu/mn/

*/
	
	private static final String prefix = "repl:testID";
	

	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();

    @Test
    public void testPlaceholder() {
    	assertTrue(true);
    }  
    
    
	/**
	 *  This test does not require synchronization to be working
	 * @throws ServiceFailure
	 * @throws NotImplemented
	 * @throws InterruptedException
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws InvalidRequest
	 * @throws NotFound
	 * @throws IOException
	 * @throws InvalidSystemMetadata 
	 * @throws InsufficientResources 
	 * @throws UnsupportedType 
	 * @throws IdentifierNotUnique 
	 */
	@Ignore("test not adapted for v0.6.x")
	@Test
	public void testDataReplicateWithGet() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		MNode mn2 = D1Client.getMN(mn2_Url);
		Session token = null;
		
		// create new object on MN_1
		System.out.println("creating doc");
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);  

		// issue a replicate command to MN2 to get the object
		System.out.println("getting sys meta");
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		System.out.println("doing replicate");
		doReplicateCall(mn2_Url, token, smd, mn1_id);
		System.out.println("done with replicate");

		// poll get until found or tired of waiting
/*
		int elapsedTimeSec = 0;
		boolean notFound = true;
		while (notFound && (elapsedTimeSec <= replicateWaitLimitSec ))
		{			
			notFound = false;
			try {
				InputStream response = mn2.get(token, pid);
			} catch (NotFound e) {
				notFound = true;
				// expect a notfound until replication completes
			}
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object replication on " + mn2_id, !notFound);
		*/
	}

	/**
	 * This test does not require synchronization to be working
	 * 
	 * @throws ServiceFailure
	 * @throws NotImplemented
	 * @throws InterruptedException
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws InvalidRequest
	 * @throws NotFound
	 * @throws IOException
	 * @throws InvalidSystemMetadata 
	 * @throws InsufficientResources 
	 * @throws UnsupportedType 
	 * @throws IdentifierNotUnique 
	 */
	@Ignore("test not adapted for v0.6.x")
	@Test
	public void testDataReplicateWithMeta() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		MNode mn2 = D1Client.getMN(mn2_Url);
		Session token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// issue a replicate command to MN2 to get the object
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		doReplicateCall(mn2_Url, token, smd, mn1_Url);

		// poll get until found or tired of waiting

		int elapsedTimeSec = 0;
		boolean notFound = true;
		SystemMetadata smd2 = null;
		while (notFound && (elapsedTimeSec <= replicateWaitLimitSec ))
		{			
			notFound = false;
			try {
				smd2 = mn2.getSystemMetadata(token, pid);
			} catch (NotFound e) {
				notFound = true;
				// expect a notfound until replication completes
			}
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object replicated on " + mn2_id, !notFound);
	}

	
	/**
	 * This test requires synchronization to be functional, and probably requires a longer wait time
	 * because of it. It is the more thorough test of replication, because it needs the replicationStatus
	 * to be updated once replication is complete. 
	 * 
	 * @throws ServiceFailure
	 * @throws NotImplemented
	 * @throws InterruptedException
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws InvalidRequest
	 * @throws NotFound
	 * @throws IOException
	 * @throws InvalidSystemMetadata 
	 * @throws InsufficientResources 
	 * @throws UnsupportedType 
	 * @throws IdentifierNotUnique 
	 */
	@Ignore("test not adapted for v0.6.x")
	@Test
	public void testDataReplicateWithResolve() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		CNode cn = D1Client.getCN();
		MNode mn1 = D1Client.getMN(mn1_Url);	
		MNode mn2 = D1Client.getMN(mn2_Url);
		Session token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);
		

		// do resolve and count locations
		int preReplCount = ExampleUtilities.countLocationsWithResolve(cn,pid);
		System.out.println("locations before replication = " + preReplCount);

		// issue a replicate command to MN2 to get the object
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		doReplicateCall(mn2_Url, token, smd, mn1_Url);

		// poll resolve until locations is 2 (or more);
		int postReplCount = preReplCount;
		int elapsedTimeSec = 0;
		boolean notFound = true;
		while (postReplCount == preReplCount && (elapsedTimeSec <= replicateResolveWaitLimitSec))
		{
			postReplCount = ExampleUtilities.countLocationsWithResolve(cn,pid);
			Thread.sleep(pollingFrequencySec * 1000); // millisec's
			elapsedTimeSec += pollingFrequencySec;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object replicated on " + mn2_id, postReplCount > preReplCount);
	}

	
	
	//  ===================  helper procedures  =======================================//
	/**
	 * adapted from D1Node.sendRequest()
	 */
	private void doReplicateCall(String mnBaseURL, Session token, 
			SystemMetadata sysmeta, String sourceNode) throws ServiceFailure, IOException
	{
	    System.out.println("doing replicate call");
		String restURL = mnBaseURL + "replicate";
		String method = Constants.POST;

		File outputFile = null;
		InputStream multipartStream;
		try
		{
			Date d = new Date();
			File tmpDir = new File(Constants.TEMP_DIR);
			outputFile = new File(tmpDir, "mmp.output." + d.getTime());
			System.out.println("outputFile is " + outputFile.getAbsolutePath());
			FileOutputStream dataSink = new FileOutputStream(outputFile);
			//createMimeMultipart(dataSink, sourceNode, sysmeta);
			MultipartRequestHandler mmpHandler = new MultipartRequestHandler(restURL, Constants.POST);
			//mmpHandler.addParamPart("sysmeta", sysmeta.toString());
			FileOutputStream fos = new FileOutputStream(outputFile);
			TypeMarshaller.marshalTypeToOutputStream(sysmeta, fos);
			fos.flush();
			fos.close();
			
			mmpHandler.addFilePart("sysmeta", outputFile);
			mmpHandler.addParamPart("sourceNode", sourceNode);
			//dataSink.close();
			HttpResponse response = mmpHandler.executeRequest();
			//multipartStream = new FileInputStream(outputFile);
		}
		catch(Exception e)
		{
			//outputFile.delete();
			throw new ServiceFailure("1000", 
					"Error creating MMP stream in MNode.handleCreateOrUpdate: " + 
					e.getMessage() + " " + e.getStackTrace());
		}

		/*HttpURLConnection connection = null;
		
		System.out.println("restURL: " + restURL);
		System.out.println("method: " + method);
		
		URL u = new URL(restURL);
		System.out.println("creating rest connection to " + restURL);
		connection = (HttpURLConnection) u.openConnection();
		
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method);
		connection.connect(); 
		
		System.out.println("printing multipart to connection");
		if(multipartStream != null)
		{
		    OutputStream out = connection.getOutputStream();
            IOUtils.copy(multipartStream, out);
		    System.out.println("sending multipartStream to the connection.");
		    OutputStream connStream = connection.getOutputStream();
		    //IOUtils.copy(multipartStream, connStream);
		    connStream.write("ASDFASFASDF".getBytes());
		    connStream.flush();
		    connStream.close();
		}
	
		System.out.println("done sending multipart to connection");
		
        try 
        {
            InputStream content = connection.getInputStream();
            System.out.println("inputStream: " + IOUtils.toString(content));
            int code = connection.getResponseCode();
            System.out.println("returned code: " + code);
        } 
        catch (IOException ioe) 
        {
            System.out.println("tried to get content and failed.  Receiving an error stream instead: " + ioe.getMessage());
        }*/
	}

	

	
	/**
	 * Serialize the system metadata object to a ByteArrayInputStream
	 * @param sysmeta
	 * @return
	 * @throws JiBXException
	 * @throws IOException 
	 */
	protected ByteArrayInputStream serializeSystemMetadata(SystemMetadata sysmeta)
			throws JiBXException, IOException {

		ByteArrayOutputStream sysmetaOut = new ByteArrayOutputStream();
		TypeMarshaller.marshalTypeToOutputStream(sysmeta, sysmetaOut);
		ByteArrayInputStream sysmetaStream = new ByteArrayInputStream(
                sysmetaOut.toByteArray());
		return sysmetaStream;
	}
}						
