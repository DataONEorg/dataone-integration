package org.dataone.integration.it;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.apache.solr.common.SolrDocumentList;
import org.dataone.client.CNode;
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
	public void testSearch_SolrQuery() {
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
	
}
