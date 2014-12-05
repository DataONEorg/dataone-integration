package org.dataone.integration.tools;


import static org.junit.Assert.fail;

import org.dataone.client.v1.CNode;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v1.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.Subject;
import org.junit.Test;


public class RegisterNodeTools {

	ObjectList objectList = null;
	

	
	
	
	@Test
	public void registerNode() {
		
		String cnBase = "https://cn-stage-ucsb-1.dataone.org/cn";
		String mnBase = "http://mercury-ops2.ornl.gov/ornldaac/mn";
		
		MNode mn = new MNode(mnBase);
		CNode cn = new CNode(cnBase);
		
		try 
		{
			Node mnNode = mn.getCapabilities();

			// stuff to change
			mnNode.setDescription("The ORNL DAAC archives data produced by NASA's Terrestrial Ecology Program. " +
					"The DAAC provides data and information relevant to biogeochemical dynamics, ecological data, " +
					"and environmental processes, critical for understanding the dynamics relating to the biological, " +
					"geological, and chemical components of Earth's environment. ");
			mnNode.setBaseURL("http://mercury-ops2.ornl.gov/ornldaac/mn");
			//mnNode.setIdentifier(D1TypeBuilder.buildNodeReference("urn:node:USGSCSAS"));
			mnNode.addSubject(D1TypeBuilder.buildSubject("CN=Robert Nahf A579,O=Google,C=US,DC=cilogon,DC=org"));
			mnNode.addSubject(D1TypeBuilder.buildSubject("CN=ornldaac,DC=cilogon,DC=org"));
			
			
			// register
			NodeReference nodeRef = cn.register(null, mnNode);
			System.out.println("returned NodeReference: " + nodeRef.getValue());
			
		} catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
			fail("did not get successful return from /register");
		}
	}
	
	@Test
	public void registerNode_tmp() {
		
		String cnBase = "https://cn.dataone.org/cn";
		String cnStageurl = "https://cn-stage-ucsb-1.dataone.org/cn";
//		String mnBase = "http://mercury-ops2.ornl.gov/ornldaac/mn";
		
		CNode cnStage = new CNode(cnStageurl);
		CNode cn = new CNode(cnBase);
		
		try 
		{
			NodeList nl = cnStage.listNodes();
			Node targetNode = null;
			for(int i=0; i < nl.sizeNodeList(); i++) 
	    	{
	    		targetNode = nl.getNode(i);
	    		if (targetNode.getIdentifier().getValue().equals("urn:node:ORNLDAAC")) {
	    			break;
	    		}
	    		targetNode = null;
	    	}
			

			// stuff to change
			Subject rn = D1TypeBuilder.buildSubject("CN=Robert Nahf A579,O=Google,C=US,DC=cilogon,DC=org");
//			if (!targetNode.getSubjectList().contains(rn))
//				targetNode.addSubject(rn);
//			targetNode.addSubject(D1TypeBuilder.buildSubject("CN=ornldaac,DC=cilogon,DC=org"));
			targetNode.addSubject(targetNode.getContactSubject(0));
			targetNode.setContactSubjectList(null);
			targetNode.addContactSubject(rn);
			targetNode.setSynchronize(false);
			// register
			NodeReference nodeRef = cn.register(null, targetNode);
			System.out.println("returned NodeReference: " + nodeRef.getValue());
			
		} catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
			fail("did not get successful return from /register");
		}
	}

	
	
	@Test
	public void updateNodeCapabilities() {
		
		String cnBase = "https://cn-stage-ucsb-1.dataone.org/cn";
		String nodeRef = "urn:node:ORNLDAAC";
		
		CNode cn = new CNode(cnBase);
		
//		CertificateManager.getInstance().setCertificateLocation("/etc/dataone/client/testClientCerts");
		
		try 
		{
			NodeList nl = cn.listNodes();
			Node targetNode = null;
			for (Node n: nl.getNodeList()) {
				if (n.getIdentifier().getValue().equals(nodeRef)) {
					targetNode = n;
					break;
				}
			}
			
			if (targetNode != null) {
				// do the edits
				targetNode.setSynchronize(true);
//				targetNode.addSubject(D1TypeBuilder.buildSubject("CN=csas,DC=cilogon,DC=org"));
//				targetNode.setBaseURL("http://mercury-ops2.ornl.gov/clearinghouse/mn");
				
				// then submit them 
				try {
					cn.updateNodeCapabilities(targetNode.getIdentifier(), targetNode);
					System.out.println("Node updated");
				} catch (Exception e) {
					fail(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			;
		}
	}
	
}
