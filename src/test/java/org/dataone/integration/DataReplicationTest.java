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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.D1Node.ResponseData;
import org.dataone.service.Constants;
import org.dataone.service.EncodingUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidSystemMetadata;
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
	private static final String mn1_id = "http://knb-mn.ecoinformatics.org";
	private static final String mn1_Url = "http://knb-mn.ecoinformatics.org/knb/";
//	private static final String mn1_id = "unregistered";
//	private static final String mn1_Url = "http://cn-dev.dataone.org/knb/d1/";
	
	private static final String mn2_id = "http://mn-dev.dataone.org";
	private static final String mn2_Url = "http://home.offhegoes.net:8080/knb/d1/";
	
	private static final int replicateWaitLimitSec = 20;
	private static final int pollingFrequencyMS = 5000;
	
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
	 */
	@Test
	public void testReplicateWithGet() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = doCreateNewObject(mn1);

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
			Thread.sleep(pollingFrequencyMS); // millisec's
			elapsedTimeSec += pollingFrequencyMS;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object replicated on " + mn2_id, !notFound);
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
	 */
	@Test
	public void testReplicateWithMeta() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = doCreateNewObject(mn1);

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
			Thread.sleep(pollingFrequencyMS); // millisec's
			elapsedTimeSec += pollingFrequencyMS;
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
	 */
//	@Test
	public void testReplicateWithResolve() throws ServiceFailure, NotImplemented, InterruptedException, 
	InvalidToken, NotAuthorized, InvalidRequest, NotFound, IOException 
	{
		// create the players
		D1Client d1 = new D1Client(cn_Url);
		CNode cn = d1.getCN();
		MNode mn1 = d1.getMN(mn1_Url);	
		MNode mn2 = d1.getMN(mn2_Url);
		AuthToken token = null;
		
		// create new object on MN_1
		Identifier pid = doCreateNewObject(mn1);
		

		// do resolve and count locations
		int preReplCount = countLocationsWithResolve(cn,pid);
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
			postReplCount = countLocationsWithResolve(cn,pid);
			Thread.sleep(pollingFrequencyMS); // millisec's
			elapsedTimeSec += pollingFrequencyMS;
			System.out.println("Time elapsed: " + elapsedTimeSec);
		} 
		assertTrue("New object replicated on " + mn2_id, postReplCount > preReplCount);
	}


	//  ===================  helper procedures  =======================================//
	
	
	private Identifier doCreateNewObject(MNode mn) throws ServiceFailure, NotImplemented
	{
		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		AuthToken token = mn.login(principal, "kepler");
		String idString = prefix + ExampleUtilities.generateIdentifier();
		Identifier guid = new Identifier();
		guid.setValue(idString);
		InputStream objectStream = this.getClass().getResourceAsStream(
				"/d1_testdocs/knb-lter-cdr.329066.1.data");
		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV);
		Identifier rGuid = null;

		try {
			rGuid = mn.create(token, guid, objectStream, sysmeta);
			//			 assertTrue("checking that returned guid matches given ", guid.equals(rGuid.getValue()));
			assertThat("checking that returned guid matches given ", guid.getValue(), is(rGuid.getValue()));
			mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
			InputStream is = mn.get(null,guid);
		} catch (Exception e) {
			errorCollector.addError(new Throwable(" error in creating data for replication test: " + e.getMessage()));
		}
		return rGuid;
	}
	
	/**
	 *  
	 * @param cn
	 * @param pid
	 * @return
	 * @throws InvalidToken
	 * @throws ServiceFailure
	 * @throws NotAuthorized
	 * @throws NotFound
	 * @throws InvalidRequest
	 * @throws NotImplemented
	 */
	private int countLocationsWithResolve(CNode cn, Identifier pid) throws InvalidToken, ServiceFailure,
	NotAuthorized, NotFound, InvalidRequest, NotImplemented {

		ObjectLocationList oll = cn.resolve(null, pid);
		List<ObjectLocation> locs = oll.getObjectLocationList();
		return locs.toArray().length;
	}
	
	
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
	        
//		ResponseData rd = null;

		HttpURLConnection connection = null;
		//////
		System.out.println("restURL: " + restURL);
		System.out.println("method: " + method);
		
		URL u = new URL(restURL);
		connection = (HttpURLConnection) u.openConnection();
		
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method);
		connection.connect();			
		
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
			String sn = "Content-Disposition: attachment; sourceNode=" + sourceNode + "\n\n";
			out.write(sn.getBytes());
//			try {
//				mime += IOUtils.copy(object, out);
//			} 
//			catch (IOException ioe) 
//			{
//				throw new ServiceFailure("1000",
//						"Error serializing object to multipart: "
//								+ ioe.getMessage());
//			}
//			out.write("\n".getBytes());
		}

		out.write((boundary + "--").getBytes());		
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
	
//	@SuppressWarnings("rawtypes")
//	protected void serializeServiceType(Class type, Object object,
//			OutputStream out) throws JiBXException 
//			{
//		IBindingFactory bfact = BindingDirectory.getFactory(type);
//		IMarshallingContext mctx = bfact.createMarshallingContext();
//		mctx.marshalDocument(object, "UTF-8", null, out);
//	}
}						
