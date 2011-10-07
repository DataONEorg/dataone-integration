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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
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
import org.dataone.service.types.v1.AccessPolicy;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationPolicy;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MNReplicationTest extends ContextAwareTestCaseDataone {

  private static final String format_text_plain = "text/plain";
  
  private Subject subject;
  private String currentUrl;
  private String originMNId = "DEMO1";
  private String targetMNId = "DEMO2";
  private String blockedNode = "DEMO4";
  MNode originMN;
  MNode targetMN;
  Node originNode;
  Node targetNode;
  private InputStream textPlainSource;
  private SystemMetadata sysMeta;

  /**
   *  Test MNReplication.replicate() functionality when a new object is inserted
   */
  @Test
  public void testReplicateOnCreate() {
	  
    
	  subject = setupClientSubject_Writer();
	  System.out.println("Subject is: " + subject.getValue());
	  
    Iterator<Node> it = getMemberNodeIterator();  	
    
    // iterate over the node list and set the source and target nodes
    while (it.hasNext()) {
  		
  	  Node currentNode = it.next();
  	  String currentNodeId = currentNode.getIdentifier().getValue();
  	
  	  if ( currentNodeId.equals(originMNId) ) {
  	      originNode = currentNode;
  	      
  	  } else if ( currentNodeId.equals(targetMNId) ) {
  	      targetNode = currentNode;
  	      
  	  }
  	  
    }
    
  	// get callable MN objects    
    try {
        originMN = D1Client.getMN(originNode.getIdentifier());
        targetMN = D1Client.getMN(targetNode.getIdentifier());
        
    } catch (ServiceFailure e) {
        fail("Couldn't get origin or target node objects: " + e.getMessage());   
        
    }
  	
    // get a Session object
    Session session = ExampleUtilities.getTestSession();
    String identifierStr = ExampleUtilities.generateIdentifier();
    Identifier guid = new Identifier();
    guid.setValue("mNodeTier4TestReplicationOnCreate." + identifierStr);
    
    try {
        textPlainSource =
          new ByteArrayInputStream("Plain text source".getBytes("UTF-8"));
        System.out.println("Data string is: " + textPlainSource.toString());
    } catch (UnsupportedEncodingException e) {
        
      fail("Couldn't get an example input stream: " + e.getMessage());          
    }
    
    // build the system metadata object
    sysMeta = 
        ExampleUtilities.generateSystemMetadata(guid, format_text_plain, textPlainSource, null);
    
    // Ensure we have valid system metadata fields for replication

  	// build a valid ReplicationPolicy
  	ReplicationPolicy policy = new ReplicationPolicy();
  	policy.setReplicationAllowed(true);
  	policy.setNumberReplicas(1);
  	
  	List<NodeReference> preferredList = new ArrayList<NodeReference>();
  	preferredList.add(targetNode.getIdentifier());  	
    List<NodeReference> blockedList = new ArrayList<NodeReference>();
    

    policy.setBlockedMemberNodeList(blockedList);
    policy.setPreferredMemberNodeList(preferredList);
  	sysMeta.setReplicationPolicy(policy);
  	
  	// build a valid Replica list
    sysMeta.clearReplicaList();
    Replica validReplica = new Replica();
  	validReplica.setReplicaMemberNode(targetNode.getIdentifier());
  	validReplica.setReplicationStatus(ReplicationStatus.COMPLETED);

  	List<Replica> replicaList = new ArrayList<Replica>();
  	replicaList.add(validReplica);
    sysMeta.setReplicaList(replicaList);

  	// build a valid access policy
  	AccessPolicy accessPolicy = new AccessPolicy();
  	AccessRule allowRule = new AccessRule();
  	Subject publicSubject = new Subject();
  	publicSubject.setValue("public");
  	allowRule.addSubject(publicSubject);
  	allowRule.addPermission(Permission.READ);
  	accessPolicy.addAllow(allowRule);
  	sysMeta.setAccessPolicy(accessPolicy);
  	
  	// update other critical fields for replication
  	sysMeta.setAuthoritativeMemberNode(originNode.getIdentifier());
  	sysMeta.setOriginMemberNode(originNode.getIdentifier());
  	
  	// set the submitter as the cert DN
  	try {
  	    X509Certificate certificate = CertificateManager.getInstance().loadCertificate();
  	    String ownerX500 = CertificateManager.getInstance().getSubjectDN(certificate);
  	    sysMeta.getRightsHolder().setValue(ownerX500);
  	    sysMeta.getSubmitter().setValue(ownerX500);
  	    
  	} catch (Exception e) {
  	    // warn about this
  	    e.printStackTrace();
  	    
  	}
  	  	      
  	// try the create
    try {
      Identifier pid = originMN.create(session, guid, textPlainSource, sysMeta);
  	  Thread.sleep(240000L); // wait four minutes to check replica
  	  ByteArrayInputStream returnedObject = (ByteArrayInputStream) targetMN.get(session, pid);
      System.out.println("Returned data string is: " + returnedObject.toString());

  	  assertEquals(textPlainSource.toString(), returnedObject.toString());
  	    
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
  	  
  	} catch (InterruptedException e) {
        fail("Unexpected error: " + e.getMessage());
    } catch (NotFound e) {

        fail("Unexpected error: " + e.getMessage());
    }
    
  }
  
  @Override
  protected String getTestDescription() {
    return "Test Case that runs through the Member Node Tier 4 (Replication) API methods";
    
  }

}
