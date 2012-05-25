package org.dataone.integration.it;

import java.util.Set;
import java.util.TreeSet;

import org.dataone.client.CNode;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.junit.Test;


public class PreRegNodeContentCheck { //extends ContextAwareTestCaseDataone {

	
	@Test
	public void checkNodeNotRegisteredElsewhere() {
		// loop through the various environments looking for the nodeId

	}
	
	@Test
	public void checkFormatsRegistered() {

		
		String cnBase = "https://cn-dev.dataone.org/cn";
		String mnBase = "http://mercury.ornl.gov/clearinghouse/mn";
		
		MNode mn = new MNode(mnBase);
		CNode cn = new CNode(cnBase);
		
		Set<ObjectFormatIdentifier> formats = new TreeSet<ObjectFormatIdentifier>();
		try {
			ObjectList olCount = mn.listObjects(null, null, null, null, null, 0, 0);
			if (olCount.getTotal() > 0) {
				ObjectList ol = mn.listObjects(null, null, null, null, null, 0, olCount.getTotal());
				for (ObjectInfo oi : ol.getObjectInfoList()) {
					formats.add(oi.getFormatId());
				}
			}
			formats.add(D1TypeBuilder.buildFormatIdentifier("goo-goo"));
			String[] unregistered = new String[formats.size()];
			int i = 0;
			for (ObjectFormatIdentifier formatId : formats) {
				System.out.println("checking " + formatId.getValue());
				try {
					cn.getFormat(formatId);
				} 
				catch (NotFound nfe) {
					unregistered[i++] = formatId.getValue();
				}
			}
			
			System.out.println("Unregistered Formats:");
			for (String f : unregistered) {
				System.out.println("  " + f);
			}
		
		} catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
		}
	}
	
	
	@Test
	public void checkRequiredChecksumsUsed() throws ServiceFailure, NotImplemented {

		
		String cnBase = "https://cn-dev.dataone.org/cn";
		String mnBase = "http://mercury.ornl.gov/clearinghouse/mn";
		
		MNode mn = new MNode(mnBase);
		CNode cn = new CNode(cnBase);
		ChecksumAlgorithmList cnAlgs = cn.listChecksumAlgorithms();
		Set<String> chAlgSet = new TreeSet<String>();
		try {
			ObjectList olCount = mn.listObjects(null, null, null, null, null, 0, 0);
			if (olCount.getTotal() > 0) {
				ObjectList ol = mn.listObjects(null, null, null, null, null, 0, olCount.getTotal());
				for (ObjectInfo oi : ol.getObjectInfoList()) {
					chAlgSet.add(oi.getChecksum().getAlgorithm());
				}
			}
//			chAlgSet.add("goo-goo");
			String[] unregistered = new String[chAlgSet.size()];
			int i = 0;
			for (String alg : chAlgSet) {
				System.out.println("checking " + alg);
				if (!cnAlgs.getAlgorithmList().contains(alg))
					unregistered[i++] = alg;
			}
			
			System.out.println("Unregistered Formats:");
			for (String f : unregistered) {
				System.out.println("  " + f);
			}
		
		} catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
		}
	}
	
	
	
//	@Test
	public void registerMercuryNode() {
		
		String cnBase = "https://cn-dev.dataone.org/cn";
		String mnBase = "http://mercury.ornl.gov/clearinghouse/mn";
		
		MNode mn = new MNode(mnBase);
		CNode cn = new CNode(cnBase);
		
		try 
		{
			Node mnNode = mn.getCapabilities();
//			String id = mnNode.getIdentifier().getValue().replace(":daac:", ":node:");
//			System.out.println(id);
//			mnNode.setIdentifier(D1TypeBuilder.buildNodeReference(id));
			NodeReference nodeRef = cn.register(null, mnNode);
			
		} catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
		}
	}

//	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
