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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
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
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.SystemMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MNodeTier3IT extends ContextAwareTestCaseDataone {

  private static final String format_text_plain = "text/plain";
  
  private  String currentUrl;

//  @Before
//  public void setUp() throws Exception {
//  }

  @After
  public void tearDown() throws Exception {
  }

  /**
   *  Test MNStorage.create() functionality
   */
  @Test
  public void testCreate() {
	  
	  setupClientSubject_Writer();
  	
    Iterator<Node> it = getMemberNodeIterator();  	
  	
  	while (it.hasNext()) {
  		
      currentUrl = it.next().getBaseURL();
      MNode mn = D1Client.getMN(currentUrl);
      
      // get a Session object
  		Session session = ExampleUtilities.getTestSession();
			String identifierStr = ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue("mNodeTier3TestCreate." + identifierStr);
			SystemMetadata sysMeta = new SystemMetadata();
			InputStream textPlainSource;
			
			try {
				// get some data bytes as an input stream
				textPlainSource =
					new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));
				
				// build the system metadata object
				sysMeta = 
					ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource,null);
				
				// match the submitter as the cert DN 
				try {
					X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
					String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
					sysMeta.getRightsHolder().setValue(ownerX500);
				} catch (Exception e) {
					// warn about this
					e.printStackTrace();
				}
			      
				// try the create
				Identifier pid = mn.create(session, guid, textPlainSource, sysMeta);
				
				assertEquals(guid, pid);
				
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				
			} catch (InvalidToken e) {
		    fail("Unexpected error: " + e.getMessage());
		    
      } catch (ServiceFailure e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotAuthorized e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (IdentifierNotUnique e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (UnsupportedType e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InsufficientResources e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidSystemMetadata e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotImplemented e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidRequest e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      }
		}
    
    
  }
  
  /**
   *  Test MNStorage.update() functionality
   */
  @Test
  public void testUpdate() {
	  
	  setupClientSubject_Writer();
    
  	Iterator<Node> it = getMemberNodeIterator();
  	
  	while ( it.hasNext() ) {
  		
  		currentUrl = it.next().getBaseURL();
  		MNode mn = D1Client.getMN(currentUrl);
  		
  		Session session = ExampleUtilities.getTestSession();
  		String identifierStr = ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue("mNodeTier3TestUpdate." + identifierStr);
			SystemMetadata sysMeta = new SystemMetadata();
			InputStream textPlainSource;
			Identifier newPid;
			
			try {
	      // get some data bytes as an input stream
	      textPlainSource = 
	      	new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));
	      
	      // build the system metadata object
	      sysMeta = 
	      	ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource,null);
	      
	      // match the submitter as the cert DN 
	      try {
	    	  X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
	    	  String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
	    	  sysMeta.getRightsHolder().setValue(ownerX500);
	      } catch (Exception e) {
	    	  // warn about this
	    	  e.printStackTrace();
	      }
	      
	      // get a pid to update
	      Identifier pid = mn.create(session, guid, textPlainSource, sysMeta);
	  		String newIdentifierStr = ExampleUtilities.generateIdentifier();
	      newPid = new Identifier();
	      newPid.setValue("mNodeTier3TestUpdate." + newIdentifierStr);
//	      SystemMetadata newSysMeta = mn.getSystemMetadata(session, pid);

	      // TODO: reinstated the checks when obsolete behavior refactored.
	      // update the obsoletesList
//	      newSysMeta.addObsolete(pid);
	      
	      // update the derivedFrom list
//	      newSysMeta.addDerivedFrom(pid);
	      
	      // set the new pid on the sysmeta object
	      // TODO: should the MN do this?
	      sysMeta.setIdentifier(newPid);
	      // do the update
	      Identifier updatedPid = 
	      	mn.update(session, pid, textPlainSource, newPid, sysMeta);
	      
	      // get the updated system metadata
	      SystemMetadata updatedSysMeta = mn.getSystemMetadata(session, updatedPid);
	      
	      assertEquals(newPid, updatedPid);
//	      assertTrue(updatedSysMeta.getObsolete(0).getValue().equals(pid));
//	      assertTrue(updatedSysMeta.getDerivedFrom(0).getValue().equals(pid));	      
      
			} catch (UnsupportedEncodingException e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidToken e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (ServiceFailure e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotAuthorized e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (IdentifierNotUnique e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (UnsupportedType e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InsufficientResources e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidSystemMetadata e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotImplemented e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidRequest e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotFound e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      }

  	}
  	
  }
  
  /**
   *  Test MNStorage.delete() functionality
   */
  @Test
  public void testDelete() {
  	
	  setupClientSubject_Writer();
	  
  	Iterator<Node> it = getMemberNodeIterator();
  	
  	while ( it.hasNext() ) {
  		
  	  currentUrl = it.next().getBaseURL();
  		MNode mn = D1Client.getMN(currentUrl);
  		
  		Session session = ExampleUtilities.getTestSession();
  		String identifierStr = ExampleUtilities.generateIdentifier();
			Identifier guid = new Identifier();
			guid.setValue("mNodeTier3TestDelete." + identifierStr);
			SystemMetadata sysMeta = new SystemMetadata();
			InputStream textPlainSource;
			Identifier deletedPid;
  		
      try {
	      // get some data bytes as an input stream
	      textPlainSource = 
	      	new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));
	      
	      // build the system metadata object
	      sysMeta = 
	      	ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource,null);
	      
	      // match the submitter as the cert DN 
	      try {
	    	  X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
	    	  String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
	    	  sysMeta.getRightsHolder().setValue(ownerX500);
	      } catch (Exception e) {
	    	  // warn about this
	    	  e.printStackTrace();
	      }
	      
	      // get a pid to delete
	      Identifier pid = mn.create(session, guid, textPlainSource, sysMeta);
	      
	      // try the delete
	      deletedPid = mn.delete(session, pid);
	      assertEquals(deletedPid, pid);
	      
      } catch (UnsupportedEncodingException e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidToken e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (ServiceFailure e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotAuthorized e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (IdentifierNotUnique e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (UnsupportedType e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InsufficientResources e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidSystemMetadata e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotImplemented e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (InvalidRequest e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      } catch (NotFound e) {
  	    fail("Unexpected error: " + e.getMessage());
  	    
      }

  	}

  }

  @Override
  protected String getTestDescription() {
    return "Test Case that runs through the Member Node Tier 3 (Storage) API methods";
    
  }

}
