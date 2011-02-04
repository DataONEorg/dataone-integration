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

package org.dataone.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.EncodingUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectList;
import org.dataone.service.types.ObjectLocation;
import org.dataone.service.types.ObjectLocationList;
import org.dataone.service.types.SystemMetadata;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class D1ClientCNodeTest  {

//	private static String cnUrl = D1Client.getCN().getNodeBaseServiceUrl();
    private static String cnUrl = "http://cn-dev.dataone.org/cn";
	private static final String mnUrl = "http://cn-dev.dataone.org/knb/d1/";
	private static final String badIdentifier = "ThisIdentifierShouldNotExist";
//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";
//	private static HashMap<String,String> StandardTests = new HashMap<String,String>();
	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	private static Vector<String> encodedPIDStrings = new Vector<String>();
	
	
	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();

    
	@Before
	public  void generateStandardTests() {
		
	    D1Client d1 = new D1Client(cnUrl);

		if (testPIDEncodingStrings.size() == 0) {
			System.out.println(" * * * * * * * Unicode Test Strings * * * * * * ");
			
			InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
			Scanner s = new Scanner(is,"UTF-8");
			String[] temp;
			int c = 0;
			try
			{
				while (s.hasNextLine()) 
				{
					String line = s.nextLine();
					if (line.startsWith("common-") || line.startsWith("path-"))
					{
						System.out.println(c++ + "   " + line);
						temp = line.split("\t");
						if (temp.length > 1)
							testPIDEncodingStrings.add(temp[0]);
							encodedPIDStrings.add(temp[1]);
					}
				}	
				System.out.println("");
			} finally {
				s.close();
			}
			System.out.println("");
		}
	}
	
	/**
	 * test the listObject() operation on Coordinating Nodes
	 * @throws JiBXException 
	 */
	@Test
	public void testlistObject() throws JiBXException {

		printHeader("testlistObject vs. node " + cnUrl);
		System.out.println("Using CN: " + D1Client.getCN().getNodeBaseServiceUrl());
		checkTrue(D1Client.getCN().getNodeBaseServiceUrl().equals(cnUrl));
		
		MNode mn = D1Client.getMN(mnUrl);
//		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		
		try {
			// creating an object to ensure that there is at least one object to list
//			AuthToken token = mn.login(principal, "kepler");
//			String idString = "test:cn:listobject:" + ExampleUtilities.generateIdentifier();
//			Identifier guid = new Identifier();
//			guid.setValue(idString);
//
//			//insert a data file
//			InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
//			SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV);
//			
//			Identifier rGuid = null;
//			
//			rGuid = mn.create(token, guid, objectStream, sysmeta);
//			System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
//			mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
//			checkEquals(guid.getValue(), rGuid.getValue());

			// test the totals that come back from each call
			
			ObjectList mnOL = mn.listObjects();
			String mnOLString = serializeObjectList(mnOL);
			String mnTotalPattern = ExampleUtilities.extractObjectListTotalAttribute(mnOLString);
			
			System.out.println("   ===> total from mn call = " + mnTotalPattern);
			
			CNode cn = D1Client.getCN();
		
			ObjectList cnOL = cn.listObjects();
			String cnOLString = serializeObjectList(cnOL);
			String cnTotalPattern = ExampleUtilities.extractObjectListTotalAttribute(cnOLString);

			System.out.println("   ===> total from cn call = " + cnTotalPattern);
			
			// have to compare length of files because the order of elements is not consistent
			assertTrue("objectList total from mn equals that from cn",cnTotalPattern.equals(mnTotalPattern));
			
		} catch (BaseException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSearch() {
		printHeader("testSearch vs. node " + cnUrl);
        System.out.println("Using CN: " + D1Client.getCN().getNodeBaseServiceUrl());
        checkTrue(D1Client.getCN().getNodeBaseServiceUrl().equals(cnUrl));

		MNode mn = D1Client.getMN(mnUrl);
		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		
		try {
			AuthToken token = mn.login(principal, "kepler");
			String idString = "testCNget" + ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue(idString);

			//insert a data file
			InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream);
			objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			
			Identifier rGuid = null;
			
			rGuid = mn.create(token, guid, objectStream, sysmeta);
			System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
			mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
			checkEquals(guid.getValue(), rGuid.getValue());

			CNode cn = D1Client.getCN();
		
			ObjectList ol = cn.search(null, "query="+EncodingUtilities.encodeUrlQuerySegment(rGuid.getValue()));
			
			assertTrue("search found new object",ol.getCount() == 1);

		} catch (BaseException e) {
			errorCollector.addError(new Throwable(createAssertMessage() + " error in " +
					new Throwable().getStackTrace()[1].getMethodName() + ": " +
					e.getClass() + ": " + e.getMessage()));
		}
	}
	
	/**
	 * test the getSystemMetacata() operation on Coordinating Nodes
	 * @throws JiBXException 
	 */
	@Test
	public void testCNGetSysMeta() throws JiBXException {

		printHeader("testGetSysMeta vs. node " + cnUrl);
        System.out.println("Using CN: " + D1Client.getCN().getNodeBaseServiceUrl());
        checkTrue(D1Client.getCN().getNodeBaseServiceUrl().equals(cnUrl));

		// create a new object in order to retrieve its sysmeta
		MNode mn = D1Client.getMN(mnUrl);
		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		
		try {
			AuthToken token = mn.login(principal, "kepler");
			String idString = "test:cn:meta:" + ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue(idString);

			//insert a data file
			InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream);
			objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			
			Identifier rGuid = null;
			
			rGuid = mn.create(token, guid, objectStream, sysmeta);
			System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
			mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
			checkEquals(guid.getValue(), rGuid.getValue());

			SystemMetadata mnSysMeta = mn.getSystemMetadata(token, rGuid);
			String mnSMString = serializeSystemMetadata(mnSysMeta);
			
			CNode cn = D1Client.getCN();
		
			SystemMetadata cnSysMeta = cn.getSystemMetadata(token, guid);
			String cnSMString = serializeSystemMetadata(cnSysMeta);
			assertTrue("systemMetadata from mn equals sysmeta from cn",mnSMString.equals(cnSMString));

		} catch (BaseException e) {
			checkTrue(e instanceof NotFound);
		}
	}
	
	
	/**
	 * test the resolve() operation on Coordinating Nodes
	 * @throws JiBXException 
	 */
	@Test
	public void testCNGet() throws JiBXException {

		printHeader("testGet vs. node " + cnUrl);

        System.out.println("Using CN: " + D1Client.getCN().getNodeBaseServiceUrl());
        checkTrue(D1Client.getCN().getNodeBaseServiceUrl().equals(cnUrl));

		// create a new object in order to retrieve its sysmeta
		MNode mn = D1Client.getMN(mnUrl);
		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		
		try {
			AuthToken token = mn.login(principal, "kepler");
			String idString = "test:cn:get:" + ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue(idString);

			//insert a data file
			InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream);
			objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
			
			Identifier rGuid = null;
			
			rGuid = mn.create(token, guid, objectStream, sysmeta);
			System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
			mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
			checkEquals(guid.getValue(), rGuid.getValue());

			String mnIn = IOUtils.toString(mn.get(token, rGuid));
			
			CNode cn = D1Client.getCN();
		
			String cnIn = IOUtils.toString(cn.get(token, guid));
			assertTrue("data from mn equals data from cn",mnIn.equals(cnIn));

		} catch (BaseException e) {

		} catch (IOException e) {

		}
	}


	
	
	/**
	 * test the resolve() operation on Coordinating Nodes
	 */
	@Test
	public void testResolve() {
		String idString = "test" + ExampleUtilities.generateIdentifier();
		Vector<String> testIDs = new Vector<String>();
		testIDs.add(idString);
		Vector<String> encodedIDs = new Vector<String>();
		encodedIDs.add(idString);

		resolveRunner(testIDs,encodedIDs);
	}


	/**
	 * test the resolve() operation on Coordinating Nodes
	 */
	@Test
	public void testInvalidResolve() {
	    System.out.println("Using CN: " + D1Client.getCN().getNodeBaseServiceUrl());
	    checkTrue(D1Client.getCN().getNodeBaseServiceUrl().equals(cnUrl));

		CNode cn = D1Client.getCN();

		printHeader("testInvalidResolve vs. node " + cnUrl);
		//AuthToken token = new AuthToken();
		AuthToken token = null;
		Identifier guid = new Identifier();
		guid.setValue(badIdentifier);
		try {
			ObjectLocationList oll = cn.resolve(token, guid);
			checkEquals("Should not reach this check, exception should have been generated.", "");
		} catch (BaseException e) {
			checkTrue(e instanceof NotFound);
		}
	}


	/**
	 * Test ID encoding using the same resolve procedure
	 * (it tests create, get, getsystemMetadata, and resolve)
	 */
	@Test
	public void test_IdEncoding() 
	{
		Vector<String> testIDs  = new Vector<String>();	
		Vector<String> encodedIDs  = new Vector<String>();	
		printHeader("test_IdEncoding");
			
		for (int j=0; j<testPIDEncodingStrings.size(); j++) 
		{
			String idStringPrefix = "cn:testid:" + ExampleUtilities.generateIdentifier() + ":";
			testIDs.add(idStringPrefix + testPIDEncodingStrings.get(j));
			encodedIDs.add(idStringPrefix + encodedPIDStrings.get(j));
		}
		resolveRunner(testIDs,encodedIDs);
	}

	/*
	 * a general procedure for creating and testing the return from resolve for different
	 * identifier strings
	 */
	private void resolveRunner(Vector<String> ids, Vector<String> encodedIDs) {

		Vector<String> summaryReport = new Vector<String>();
		
		CNode cn = D1Client.getCN();

		MNode mn = D1Client.getMN(mnUrl);
		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";

		try {
			AuthToken token = mn.login(principal, "kepler");
			// run tests for each identifier
			Identifier guid = new Identifier();
			String status;
			String message;
			for (int j=0; j<ids.size(); j++) 
			{
				status = "OK   ";
				message = "-";
				try {
					String idString = ids.get(j);
					String encodedID = encodedIDs.get(j);
					guid.setValue(idString);
					System.out.println();
					System.out.println("*** ID string:  " + idString);

					//insert a data file
					InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
					SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV, objectStream);
					objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
					
					Identifier rGuid = null;

					rGuid = mn.create(token, guid, objectStream, sysmeta);
					System.out.println("    == returned Guid (rGuid): " + rGuid.getValue());
					mn.setAccess(token, rGuid, "public", "read", "allow", "allowFirst");
					checkEquals(guid.getValue(), rGuid.getValue());


					// to prevent a null pointer exception
					if (rGuid == null) 
						rGuid = guid;

					ObjectLocationList oll = cn.resolve(token, rGuid);
					for (ObjectLocation ol : oll.getObjectLocationList()) {
						System.out.println("   === Location: " + ol.getNodeIdentifier().getValue()
								+ " (" + ol.getUrl() + ")");
						if (ol.getUrl().contains(encodedID))
							checkTrue(true);
						else
						{
							status = "Fail";
							message = "encodedID not found: " + encodedID;
							errorCollector.addError(new Throwable(createAssertMessage() + " encodedID not found: " + encodedID));
						}
					}
				}
				catch (Exception e) {
					
					status  = "error";
					message = e.getMessage();
					errorCollector.addError(new Throwable(createAssertMessage() + " error in testCreateScienceMetadata: " + e.getMessage()));
				}
				summaryReport.add(j + " " + status + " " + ids.get(j) + "  " + message);
			}
		} catch (BaseException e) {
			errorCollector.addError(new Throwable(createAssertMessage()
					+ " error in resolveRunner at logon: " + e.getMessage()));
		}
		System.out.println("*********  Test Summary ************");

		for (int j =0; j<summaryReport.size(); j++)
		{
			System.out.println(summaryReport.get(j));
		}
		System.out.println();
	}



	private static String createAssertMessage()
	{
		return "test failed at url " + cnUrl;
	}


	private void printHeader(String test)
	{
		System.out.println("\n***************** running test for " + test + " *****************");
	}

    private void checkEquals(final String s1, final String s2) {
        errorCollector.checkSucceeds(new Callable<Object>() {
            public Object call() throws Exception {
                assertThat("assertion failed for host " + cnUrl, s1, is(s2));
                return null;
            }
        });
    }

	private void checkTrue(final boolean b)
	{
		errorCollector.checkSucceeds(new Callable<Object>() 
				{
			public Object call() throws Exception 
			{
				assertThat("assertion failed for host " + cnUrl, true, is(b));
				return null;
			}
				});
	}

	/**
	 * Serialize the objectList object to a ByteArrayInputStream
	 * @param sysmeta
	 * @return
	 * @throws JiBXException
	 */
	private String serializeObjectList(ObjectList objectlist)
			throws JiBXException {

		ByteArrayOutputStream responseOut = new ByteArrayOutputStream();
		serializeServiceType(SystemMetadata.class, objectlist, responseOut);
		return responseOut.toString();
	}

	
	/**
	 * Serialize the system metadata object to a ByteArrayInputStream
	 * @param sysmeta
	 * @return
	 * @throws JiBXException
	 */
	private String serializeSystemMetadata(SystemMetadata sysmeta)
			throws JiBXException {

		ByteArrayOutputStream sysmetaOut = new ByteArrayOutputStream();
		serializeServiceType(SystemMetadata.class, sysmeta, sysmetaOut);
		return sysmetaOut.toString();
	}
	
	/**
	 * serialize an object of type to out
	 * 
	 * @param type
	 *            the class of the object to serialize (i.e.
	 *            SystemMetadata.class)
	 * @param object
	 *            the object to serialize
	 * @param out
	 *            the stream to serialize it to
	 * @throws JiBXException
	 */
	@SuppressWarnings("rawtypes")
	private void serializeServiceType(Class type, Object object,
			OutputStream out) throws JiBXException {
		IBindingFactory bfact = BindingDirectory.getFactory(type);
		IMarshallingContext mctx = bfact.createMarshallingContext();
		mctx.marshalDocument(object, "UTF-8", null, out);
	}
	
	

}
