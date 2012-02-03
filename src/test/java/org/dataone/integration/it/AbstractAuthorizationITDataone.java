package org.dataone.integration.it;

import java.util.Iterator;

import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.D1Node;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractAuthorizationITDataone extends ContextAwareTestCaseDataone {

	 private static String currentUrl;
	 
	 /**
	  * implementing class needs to set this to either NodeType.MN or .CN
	  * for running the tests
	  * @return
	  */
	 protected abstract NodeType getNodeType();

	 
	 /**
	  * Finds or creates an object with PUBLIC READ access, then ensures
	  * that an anonymous client can READ it.
	  */
	 @Test
	 public void testIsAuthorized_PublicSymbolicPrincipal() 
	 {	
		 AccessRule accessRule = AccessUtil.createAccessRule(
				 new String[]{Constants.SUBJECT_PUBLIC}, new Permission[]{Permission.READ});
		 runIsAuthorizedVsSubject( accessRule, "NoCert", "NoCert", null);
	 }
	 
	   /**
	     * Finds or creates an object where AUTHENTICATED_USER has READ access, then
	     * ensures that the "testNoRights" client can READ it.  (By design, the testNoRights
	     * subject is not on accessPolicies, and is not part of any group, or mapped
	     * to any other identity.)
	     */
	    @Test
		public void testIsAuthorized_AuthenticatedUserSymbolicPrincipal()
	    {	
	    	AccessRule accessRule = AccessUtil.createAccessRule(
	    			new String[]{Constants.SUBJECT_AUTHENTICATED_USER}, new Permission[]{Permission.READ});
	    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testNoRights",null);
	    }
	    
	    
	    @Ignore("user verification not implemented yet")
	    @Test
		public void testIsAuthorized_VerifiedUserSymbolicPrincipal()
	    {	
	    	AccessRule accessRule = AccessUtil.createAccessRule(
	    			new String[]{Constants.SUBJECT_VERIFIED_USER}, new Permission[]{Permission.READ});
	    	runIsAuthorizedVsSubject(accessRule, "testOwner", "testReader", null);
	    }
	    
	    /**
	     * assuming that testMappedReader and testReaderGroup are not in the
	     * accessPolicy of the object procured.
	     */
	    @Test
		public void testIsAuthorized_MappedIdentity()
	    {	
	    	AccessRule accessRule = AccessUtil.createAccessRule(
	    			new String[]{"testReader"}, new Permission[]{Permission.READ});
	    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testMappedReader", null);
	    }
	    
	    /**
	     * assumes that testReader and testMappedReader are not in the accessPolicy
	     * of the object procured
	     */
	    @Test
		public void testIsAuthorized_ViaGroupMembership()
	    {	
	    	AccessRule accessRule = AccessUtil.createAccessRule(
	    			new String[]{"testReaderGroup"}, new Permission[]{Permission.READ});
	    	Identifier pid = new Identifier();
	    	pid.setValue("testAuthByGroup");
	    	runIsAuthorizedVsSubject( accessRule, "testOwner", "testReader",pid);
	    }

	    
	    
	    
	    
	    
	 
	 
	 /** 
     * Implementation for isAuthorized Tests for various types of users
     * @param accessRule - the accessRule containing the subject and permission
     *           required in the systemmMetadata.accessPolicy 
     * @param procuringSubjectName - the client subject used to search for the
     *           objects with (listObjects, getSystemMetadata)
     * @param testingSubjectName - the client subject to run the test under
	 * @param pid - pass in a valid pid to bypass the procurement of the identifier
	 *              and test isAuthorized against the provided pid.
     */
	protected void runIsAuthorizedVsSubject(AccessRule accessRule, 
			String procuringSubjectName, String testingSubjectName, Identifier pid)
	{
		String policySubjectName = accessRule.getSubject(0).getValue();
		
		
		Iterator<Node> it = null;
		if (getNodeType().equals(NodeType.MN)) {
			it = getMemberNodeIterator();
		} else if (getNodeType().equals(NodeType.CN)) {
			it = getCoordinatingNodeIterator();
		}
		while (it.hasNext()) {
			currentUrl = it.next().getBaseURL();

			D1Node d1Node = null;
			if (getNodeType().equals(NodeType.MN)) {
				d1Node = D1Client.getMN(currentUrl);
			} else {
				d1Node = new CNode(currentUrl);
			}
			currentUrl = d1Node.getNodeBaseServiceUrl();
			printTestHeader("testIsAuthorized_" + policySubjectName + "() vs. node: " + currentUrl);

			try {				
				// change to the subject used for procuring an object
				if (procuringSubjectName.equals("NoCert") || procuringSubjectName == null)
					setupClientSubject_NoCert();
				else 
					setupClientSubject(procuringSubjectName);

				// get an appropriate test object
				 pid = procureTestObject(d1Node, accessRule, pid);
					
				// change to the subject used for testing, if necessary
				if (!testingSubjectName.equals(procuringSubjectName)) {
					if (testingSubjectName.equals("NoCert") || testingSubjectName == null)
						setupClientSubject_NoCert();
					else 
						setupClientSubject(testingSubjectName);
				}
				
				// test for success
				log.info("1. trying isAuthorized({READ}) as '" + testingSubjectName + "' vs. policy subject '" + policySubjectName + "'");		
				try {
					d1Node.isAuthorized(null, pid, Permission.READ);
				} catch (NotAuthorized na) {
					handleFail(currentUrl,"1. " + policySubjectName + " should be authorized to read this pid '" 
							+ pid.getValue() + "'\n" + na.getClass().getSimpleName() + ": "
							+ na.getDetail_code() + ": " + na.getDescription());
				}

				log.info("2. trying get() as '" + testingSubjectName + "' vs. policy subject '" + policySubjectName + "'");
				try {
					d1Node.get(null, pid);
				} 
				catch (NotAuthorized na) {
					handleFail(currentUrl,"2. " + policySubjectName + " should now be able to get the object - got NotAuthorized instead");
				}
				catch (NotFound na) {
					handleFail(currentUrl,"2. " + policySubjectName + " should now be able to get the object - got NotFound instead");
				}
	
			} catch (IndexOutOfBoundsException e) {
				handleFail(currentUrl,"No Objects available to test against");
			} catch (BaseException e) {
				handleFail(currentUrl, e.getClass().getSimpleName() + ": " + 
						e.getDetail_code() + ": " + e.getDescription());
			} catch (Exception e) {
				e.printStackTrace();
				handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
			}
			
		}
	}   

	
}
