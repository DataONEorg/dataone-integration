package org.dataone.integration.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Iterator;

import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dataone.client.CNode;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.junit.Test;


public class CNsolrIT { // extends ContextAwareTestCaseDataone {

	private String currentUrl = "http://cn-dev.test.dataone.org/cn";
	
//	@Override
//	protected String getTestDescription() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Test 
	public void testSearch_SolrQuery_nullParams() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
		
			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSearch_SolrQuery() vs. node: " + currentUrl);

			try {
				
//				SolrDocumentList docList = cn.search(null,new Long(12),new Long(34),null);
				SolrDocumentList docList = cn.search(null,null,null,null);
				System.out.println("docs Returned: " + docList.size());
				assertTrue("documents should be returned", docList.size() > 0);
			} 
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
//			}
			catch(Exception e) {
				e.printStackTrace();
				fail("failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
		
	}
	
	@Test 
	public void testSearch_SolrQuery_parameterized() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
		
			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSearch_SolrQuery() vs. node: " + currentUrl);

			try {
				SolrDocumentList docList = cn.search(null,new Integer(1),new Integer(1),new String[] {"id"});
				
				assertTrue("1 SolrDocument should be returned", docList.size() == 1);
				assertTrue("SolrDocument should contain 'id' field", docList.get(0).getFieldValue("id") != null);
				assertTrue("the SolrDocument returned should have only 1 field", docList.get(0).getFieldNames().size() == 1);
			} 
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
//			}
			catch(Exception e) {
				e.printStackTrace();
				fail("failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
		
	}
	
	@Test 
	public void testGetSolrDocument() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
		
			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSearch_SolrQuery() vs. node: " + currentUrl);

			try {
				SolrDocumentList docList = cn.search(null,null,null,null);
				String documentID = (String) docList.get(0).getFieldValue("id");
				Identifier pid = new Identifier();
				pid.setValue(documentID);
				docList = cn.getSolrDocument(pid);
				
				assertTrue("should get at least one document", docList.size() >= 1);
				assertEquals("returned SolrDocument id should equal the pid given",documentID, docList.get(0).getFieldValue("id"));				
			} 
//			catch (BaseException e) {
//				handleFail(currentUrl,e.getClass().getSimpleName() + ":: " + e.getDescription());
//			}
			catch(Exception e) {
				e.printStackTrace();
				fail("failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
		
	}
	
	@Test
	public void testGetSearchFields() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
		
			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSearch_SolrQuery() vs. node: " + currentUrl);

			try {
				LukeResponse response = cn.getSearchFields();
				String idType = response.getFieldInfo("id").getType();
				assertTrue("solr should at least contain the 'id' field of type 'string'",idType.equalsIgnoreCase("string"));
			} 
			catch(Exception e) {
				e.printStackTrace();
				fail("failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
	
	}
	
	@Test
	public void testSearchSolrByDate() {
//		Iterator<Node> it = getCoordinatingNodeIterator();
//		while (it.hasNext()) {
//			currentUrl = it.next().getBaseURL();
		
			CNode cn = new CNode(currentUrl);
//			printTestHeader("testSearch_SolrQuery() vs. node: " + currentUrl);

			try {
				SolrDocumentList docs = cn.searchSolrByDate("dateuploaded","[* TO NOW]",new String[] {"id","dateuploaded"});
				
				assertTrue("solr should return at least one document",docs.getNumFound() > 0);
				Date uploadDate =  (Date) docs.get(0).getFieldValue("dateuploaded");
				System.out.println("uploadDate = " + uploadDate);
			} 
			catch(Exception e) {
				e.printStackTrace();
				fail("failed to create Node object from input stream" 
						+ e.getClass().getName() + ": " + e.getMessage());
			}	
	
	}
	
	
	
	
}
