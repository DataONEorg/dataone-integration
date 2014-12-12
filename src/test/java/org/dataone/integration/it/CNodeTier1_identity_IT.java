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

import java.util.Iterator;

import org.dataone.client.v1.CNode;
import org.dataone.client.auth.ClientIdentityManager;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.IdentifierNotUnique;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Subject;
import org.junit.Test;
import org.springframework.util.StringUtils;

/**
 * Test the DataONE Java client methods that focus on CN services.
 * @author Rob Nahf
 */
public class CNodeTier1_identity_IT extends ContextAwareTestCaseDataone {
	
	private static String currentUrl;

    
	/**
     * test generating an Identifier containing the given fragment.
     * Specifies the UUID syntax
     */
    @Test
    public void testGenerateIdentifier() {
    	setupClientSubject("testSubmitter");
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

    		try {
    			String fragment = "CNodeTier1Test";
    			Identifier response = cn.generateIdentifier(null,"UUID",fragment);
    			//TODO: UUID isn't supporting the fragment concept, so can't check 
    			// this yet.
//    			checkTrue(cn.getLatestRequestUrl(),"generateIdentifier(...) should return an Identifier object" +
//    					" containing the given fragment: '" + fragment + "' Got: '" + response.getValue() + "'",
//    					response.getValue().contains(fragment));
    			checkTrue(cn.getLatestRequestUrl(),"generateIdentifier(...) should return a UUID-style" +
    					"identifier with 5 hexidecimal segments separated by '-'s.  Got: " +
    					response.getValue(),
    					StringUtils.countOccurrencesOf(response.getValue(),"-") >=4);
    					
	
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
     * tests validity of scheme names by trying an invalid scheme.
     * It should throw an error.
     */
    @Test
    public void testGenerateIdentifier_badScheme() {
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testGenerateIdentifier(...) vs. node: " + currentUrl);

    		try {
    			String fragment = "CNodeTier1Test";
    			cn.generateIdentifier(null,"bloip",fragment);
    			handleFail(cn.getLatestRequestUrl(),"generateIdentifier(...) with a bogus scheme should" +
    					"throw an exception (should not have reached here)");
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (InvalidRequest e) {
    			// the expected outcome indicating good behavior :-)
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }

	
	
	
	/**
     * Creates an identifier and reserves it.  Then checks that the client indeed
     * has the reservation (hasReservation());
     */
    @Test
    public void testHasReservation() {
    	
    	setupClientSubject("testSubmitter");
    	Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);
    		
    		try {
    			boolean response = false;
//    			if (reservedIdentifier != null) {
//    				response = cn.hasReservation(null,clientSubject,reservedIdentifier);
//    			} else {
    				Identifier pid = new Identifier();
    				pid.setValue(ExampleUtilities.generateIdentifier());
    				cn.reserveIdentifier(null, pid );
    				response = cn.hasReservation(null,clientSubject,pid);
//    			}
    			checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", response);
    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (BaseException e) {
    			handleFail(cn.getLatestRequestUrl(),e.getDescription());
    		}
    		catch(Exception e) {
    			e.printStackTrace();
    			handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
    		}
    	}
    }
    
    
    /**
     * Generates a new identifier, and tries hasReservation() without first reserving it.
     * Expect a NotFound exception.  
     */
    @Test
    public void testHasReservation_noReservation() {
    	
    	Subject clientSubject = ClientIdentityManager.getCurrentIdentity();
    	
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testHasReservation(...) vs. node: " + currentUrl);

    		try {
    			boolean response = false;
    			Identifier pid = new Identifier();
    			pid.setValue(ExampleUtilities.generateIdentifier());
    			response = cn.hasReservation(null,clientSubject, pid);

    			checkTrue(cn.getLatestRequestUrl(),"response cannot be false. [Only true or exception].", 
    					response);
    		}
    		catch (NotFound e) {
    			; // this is desired behavior
    		}
//    		catch (NotAuthorized e) {
//    			; // this is also acceptable
//    		}
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
     * tests that reserveIdentifier works, then tries again to make sure
     * that the identifier cannot be reserved again.
     */
    @Test
    public void testReserveIdentifier() {
    	setupClientSubject("testSubmitter");
    	Iterator<Node> it = getCoordinatingNodeIterator();
    	while (it.hasNext()) {
    		currentUrl = it.next().getBaseURL();
    		CNode cn = new CNode(currentUrl);
    		printTestHeader("testReserveIdentifier(...) vs. node: " + currentUrl);

    		boolean isReserved = false;
    		try {
    			Identifier pid = new Identifier();
    			pid.setValue(ExampleUtilities.generateIdentifier());

    			Identifier response = cn.reserveIdentifier(null,pid);
    			checkTrue(cn.getLatestRequestUrl(),"reserveIdentifier(...) should return the given identifier",
    					response.equals(pid));
    			isReserved = true;
    			// try again - should fail
    			response = cn.reserveIdentifier(null,pid);

    		} 
    		catch (IndexOutOfBoundsException e) {
    			handleFail(cn.getLatestRequestUrl(),"No Objects available to test against");
    		}
    		catch (IdentifierNotUnique e) {
    			if (isReserved) {
    				// then got the desired outcome
    			} else {
    				handleFail(cn.getLatestRequestUrl(),e.getDescription());
    			}
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


	@Override
	protected String getTestDescription() {
		return "Test the Tier1 CN methods implemented by the identity_manager package";
	}
	
}
