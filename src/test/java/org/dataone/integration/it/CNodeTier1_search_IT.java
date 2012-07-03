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

package org.dataone.integration.it;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dataone.client.CNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.util.D1Url;
import org.junit.Test;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_search_IT extends ContextAwareTestCaseDataone {


	private static String unicodeIdPrefix = "testCNodeTier1";
	
	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt";
	//	private static String identifierEncodingTestFile = "/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt";

	
	private static String currentUrl;


	

    

	
	/**
	 * Test runs a basic solr query ("q=*:*") using cn.search().  Successful if
	 * an ObjectList is returned. (can be empty)
	 */
	@Test
	public void testSearch() {
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSearch(...) vs. node: " + currentUrl);

			try {
				ObjectList response = cn.search(null,"solr","?q=*:*");
				checkTrue(cn.getLatestRequestUrl(),"search(...) returns a ObjectList object", response != null);
			} 
			catch (IndexOutOfBoundsException e) {
				handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
			}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
    					e.getDetail_code() + ":: " + e.getDescription());
    		}
			catch(Exception e) {
				e.printStackTrace();
				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
			}
		}
	}
	
	
	/**
	 * This test searches for identifiers containing the representative unicode
	 * characters, and is successful if ObjectList is returned (can be empty).
	 * The goal is to rule out encoding issues with the request and processing.
	 * It does NOT test that an object with those characters are found.
	 */
	@Test
	public void testSearch_Solr_unicodeTests() {
		
		// get identifiers to check with
		Vector<String> unicodeString = new Vector<String>();
		Vector<String> escapedString = new Vector<String>();
		InputStream is = this.getClass().getResourceAsStream(identifierEncodingTestFile);
		Scanner s = new Scanner(is,"UTF-8");
		String[] temp;
		int c = 0;
		try{
			while (s.hasNextLine()) {
				String line = s.nextLine();
				if (line.startsWith("common-") || line.startsWith("query-"))
				{
					System.out.println(c++ + "   " + line);
					temp = line.split("\t");
					unicodeString.add(temp[0]);
					escapedString.add(temp[1]);		
				}
			}
		} finally {
			s.close();
		}
		
		
		Iterator<Node> it = getCoordinatingNodeIterator();
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();
			CNode cn = new CNode(currentUrl);
			currentUrl = cn.getNodeBaseServiceUrl();
			printTestHeader("testSearch_Solr_unicodeTests(...) vs. node: " + currentUrl);
			
			Vector<String> nodeSummary = new Vector<String>();
			nodeSummary.add("Node Test Summary for node: " + currentUrl );
			
			for (int i=0; i<unicodeString.size(); i++) 
			{
				String status = "OK   ";

				//   String unicode = unicodeString.get(j);
				System.out.println();
				System.out.println(i + "    unicode String:: " + unicodeString.get(i));
//				String idSubStringEscaped =  escapedString.get(i);

				try {
					D1Url query = new D1Url("a","b");
					String wildcardPattern = unicodeIdPrefix + "*" + unicodeString.get(i) + "*";
					String solrEscapedWildcardPattern = ClientUtils.escapeQueryChars(wildcardPattern);
					query.addNonEmptyParamPair("q", "id:" + solrEscapedWildcardPattern);

					ObjectList response = cn.search(null,"solr",query);
					checkTrue(cn.getLatestRequestUrl(),"search(...) should return an ObjectList", response != null);
					
//					checkTrue(cn.getLatestRequestUrl(),"search(...) should ")
				}
				catch (IndexOutOfBoundsException e) {
					handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
				}
	    		catch (BaseException e) {
	    			status = "Error";
	    			handleFail(cn.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
	    					e.getDetail_code() + ":: " + e.getDescription());
	    		}
				catch(Exception e) {
					status = "Error";
					e.printStackTrace();
					handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
				}

				nodeSummary.add("Test " + i + ": " + status + ": " + unicodeString.get(i));
			}
			System.out.println();
			for (int k=0; k<nodeSummary.size(); k++) 
			{
				System.out.println(nodeSummary.get(k));
			}
			System.out.println();
		}
	}


	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods related to the search component";
	}
	
}
