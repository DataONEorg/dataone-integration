package org.dataone.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.D1Node.ResponseData;
import org.dataone.service.Constants;
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
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.SystemMetadata;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
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
	private static final String cn_id = "cn-dev";
	private static final String cn_Url = "http://cn-dev.dataone.org/cn/";
	// mn1 needs to be a published node that supports login, create, get and meta
//	private static final String mn1_id = "http://knb-mn.ecoinformatics.org";
//	private static final String mn1_Url = "http://knb-mn.ecoinformatics.org/knb/";
	private static final String mn1_id = "unregistered";
	private static final String mn1_Url = "http://cn-dev.dataone.org/knb/d1/";
	
	//private static final String mn1_id = "unregistered";
	//private static final String mn1_Url = "http://amasa.local:8080/knb/d1/";
	
	private static final String mn2_id = "http://mn-dev.dataone.org";
	//private static final String mn2_Url = "http://amasa.local:8080/knb/d1/";
	private static final String mn2_id = "http://home.offhegoes.net:8080/knb/d1";
	
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
	@Test
	public void testDataReplicateWithGet() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// issue a replicate command to MN2 to get the object
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		InputStream is = doReplicateCall(mn2_Url, token, smd, mn1_Url);

		// poll get until found or tired of waiting

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
	@Test
	public void testDataReplicateWithMeta() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);

		// issue a replicate command to MN2 to get the object
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		InputStream is = doReplicateCall(mn2_Url, token, smd, mn1_Url);

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
//	@Test
	public void testDataReplicateWithResolve() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = ExampleUtilities.doCreateNewObject(mn1, prefix);
		

		// do resolve and count locations
		int preReplCount = ExampleUtilities.countLocationsWithResolve(cn,pid);
		System.out.println("locations before replication = " + preReplCount);

		// issue a replicate command to MN2 to get the object
		SystemMetadata smd = mn1.getSystemMetadata(token, pid);
		InputStream is = doReplicateCall(mn2_Url, token, smd, mn1_Url);

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
	
	private InputStream doReplicateCall(String mnBaseURL, AuthToken token, 
			SystemMetadata sysmeta, String sourceNode) throws ServiceFailure, IOException
	{
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
			createMimeMultipart(dataSink, sourceNode, sysmeta);
			multipartStream = new FileInputStream(outputFile);
		}
		catch(Exception e)
		{
			outputFile.delete();
			throw new ServiceFailure("1000", 
					"Error creating MMP stream in MNode.handleCreateOrUpdate: " + 
					e.getMessage() + " " + e.getStackTrace());
		}

		HttpURLConnection connection = null;
		
		System.out.println("restURL: " + restURL);
		System.out.println("method: " + method);
		
		URL u = new URL(restURL);
		connection = (HttpURLConnection) u.openConnection();
		
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method);
		connection.connect();	
		
		if(multipartStream != null)
		{
		    System.out.println("sending multipartStream to the connection.");
		    OutputStream connStream = connection.getOutputStream();
		    IOUtils.copy(multipartStream, connStream);
		    connStream.flush();
		}
		
		// this phrase if we are expecting something in the response body
//		if (dataStream != null) {
//			OutputStream out = connection.getOutputStream();
//			IOUtils.copy(dataStream, out);
//		}
		
		InputStream content = null;
		try {
			content = connection.getInputStream();
//			rd.setContentStream(content);
		} catch (IOException ioe) {
			System.out.println("tried to get content and failed.  Receiving an error stream instead.");
				// will set errorStream outside of catch
		}
	        

		int code = connection.getResponseCode();
//		resData.setCode(code);
//		resData.setHeaderFields(connection.getHeaderFields());
		if (code != HttpURLConnection.HTTP_OK) 
			// error
			return connection.getErrorStream();

		return content;
	}

	/**
	 * create a mime multipart message from object and sysmeta and write it to out
	 */
	protected void createMimeMultipart(OutputStream out, String sourceNode,
			SystemMetadata sysmeta) throws IOException, BaseException
	{
		if (sysmeta == null) {
			throw new InvalidSystemMetadata("1000",
					"System metadata was null.  Can't create multipart form.");
		}
		Date d = new Date();
		String boundary = d.getTime() + "";

		String mime = "MIME-Version:1.0\n";
		mime += "Content-type:multipart/mixed; boundary=\"" + boundary + "\"\n";
		boundary = "--" + boundary + "\n";
		mime += boundary;
		mime += "Content-Disposition: attachment; filename=sysmeta\n\n";
		out.write(mime.getBytes());
		out.flush();
		
		//write the sys meta
		try
		{
		    ByteArrayInputStream bais = serializeSystemMetadata(sysmeta);
			IOUtils.copy(bais, out);
		}
		catch(JiBXException e)
		{
		    throw new ServiceFailure("1000",
                    "Could not serialize the system metadata to multipart: "
                            + e.getMessage());
		}
		
		out.write("\n".getBytes());	

		if (sourceNode != null) 
		{    
			out.write(boundary.getBytes());
			String sn = "Content-Disposition: form-data; name=sourceNode\n\n" + 
			    sourceNode + "\n";
			out.write(sn.getBytes());
		}

		out.write((boundary + "--").getBytes());	
		out.flush();
	}

	
	/**
	 * Serialize the system metadata object to a ByteArrayInputStream
	 * @param sysmeta
	 * @return
	 * @throws JiBXException
	 */
	protected ByteArrayInputStream serializeSystemMetadata(SystemMetadata sysmeta)
			throws JiBXException {

		ByteArrayOutputStream sysmetaOut = new ByteArrayOutputStream();
		
		IBindingFactory bfact = BindingDirectory.getFactory(SystemMetadata.class);
		IMarshallingContext mctx = bfact.createMarshallingContext();
		mctx.marshalDocument(sysmeta, "UTF-8", null, sysmetaOut);		
		ByteArrayInputStream sysmetaStream = new ByteArrayInputStream(
				sysmetaOut.toByteArray());
		return sysmetaStream;
	}
}						