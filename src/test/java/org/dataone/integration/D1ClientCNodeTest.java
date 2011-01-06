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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.service.EncodingUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.AuthToken;
import org.dataone.service.types.Identifier;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectLocation;
import org.dataone.service.types.ObjectLocationList;
import org.dataone.service.types.SystemMetadata;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Matthew Jones
 */
public class D1ClientCNodeTest  {

	private static final String baseURL = "http://cn-dev.dataone.org";
	private static final String cnUrl = "http://cn-dev.dataone.org/cn/";
	private static final String mnUrl = "http://cn-dev.dataone.org/knb/d1/";
	private static final String badIdentifier = "ThisIdentifierShouldNotExist";
//  TODO: test against testUnicodeStrings file instead when metacat supports unicode.
//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";
//	private static HashMap<String,String> StandardTests = new HashMap<String,String>();
	private static Vector<String> testPIDEncodingStrings = new Vector<String>();
	@Rule 
	public ErrorCollector errorCollector = new ErrorCollector();

    
	@Before
	public  void generateStandardTests() {
		
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
//						StandardTests.put(temp[0], temp[1]);
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
	 * test the resolve() operation on Coordinating Nodes
	 */
	@Test
	public void testResolve() {
		String idString = "test" + ExampleUtilities.generateIdentifier();
		Vector<String> testIDs = new Vector<String>();
		testIDs.add(idString);
		resolveRunner(testIDs);
	}


	/**
	 * test the resolve() operation on Coordinating Nodes
	 */
	@Test
	public void testInvalidResolve() {
		D1Client d1 = new D1Client(cnUrl);
		CNode cn = d1.getCN();

		printHeader("testResolve - node " + cnUrl);
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
		printHeader("test_IdEncoding");
			
		for (int j=0; j<testPIDEncodingStrings.size(); j++) 
		{
			String idString = "cn:testid:" + ExampleUtilities.generateIdentifier() + ":" + testPIDEncodingStrings.get(j);
			testIDs.add(idString);
		}
		resolveRunner(testIDs);
	}

	/*
	 * a general procedure for creating and testing the return from resolve for different
	 * identifier strings
	 */
	private void resolveRunner(Vector<String> ids) {

		Vector<String> summaryReport = new Vector<String>();
		
		D1Client d1 = new D1Client(cnUrl);
		CNode cn = d1.getCN();

		d1 = new D1Client(mnUrl);
		MNode mn = d1.getMN(mnUrl);
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
					guid.setValue(idString);
					System.out.println();
					System.out.println("*** ID string:  " + idString);

					//insert a data file
					InputStream objectStream = this.getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
					SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, ObjectFormat.TEXT_CSV);

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
						System.out.println("Location: " + ol.getNodeIdentifier().getValue()
								+ " (" + ol.getUrl() + ")");
						checkTrue(ol.getUrl().contains(idString));
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

	private void checkEquals(final String s1, final String s2)
	{
		errorCollector.checkSucceeds(new Callable<Object>() 
				{
			public Object call() throws Exception 
			{
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
}
