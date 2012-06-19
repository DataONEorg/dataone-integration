package org.dataone.integration.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.dataone.client.CNode;
import org.dataone.client.D1TypeBuilder;
import org.dataone.client.MNode;
import org.dataone.client.ObjectFormatCache;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
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
import org.junit.Before;
import org.junit.Test;


public class PreRegNodeContentCheck extends ContextAwareTestCaseDataone {

	ObjectList objectList = null;
	
	@Before
	public void fetchObjectList() {
		
		if (objectList == null) {
			Iterator<Node> it = this.getMemberNodeIterator();
			MNode mn = new MNode(it.next().getBaseURL());

			try {
				objectList = APITestUtils.pagedListObjects(mn, null, null, null, null, null, null);
			} catch (BaseException e) {
				System.out.println(e.getClass() + ": " + e.getDescription());
			}
		}
	}
	
//	@Test
	public void checkNodeNotRegisteredElsewhere() {
		// loop through the various environments looking for the nodeId

	}
	
	@Test
	public void checkFormatsRegistered() {

		Set<ObjectFormatIdentifier> formats = new TreeSet<ObjectFormatIdentifier>();
		try {
			String cnUrl = getReferenceContextCnUrl();
			CNode cn = new CNode(cnUrl);
//			ObjectFormatCache.setCNode(cn);
			
			if (objectList != null && objectList.getCount() > 0) {
				for (ObjectInfo oi : objectList.getObjectInfoList()) {
					formats.add(oi.getFormatId());
				}
			}

			String[] unregistered = new String[formats.size()];
			int i = 0;
			boolean hasUnregistered = false;
			Settings.getConfiguration().setProperty("CNode.useObjectFormatCache", "false");
			for (ObjectFormatIdentifier formatId : formats) {
				System.out.println("checking " + formatId.getValue());
				try {
					cn.getFormat(formatId);
				} 
				catch (NotFound nfe) {
					hasUnregistered = true;
					unregistered[i++] = formatId.getValue();
				}
			}
			
			System.out.println("Unregistered Formats:");
			for (String f : unregistered) {
				if (f != null)
 					System.out.println("  " + f);
			}
			assertFalse("All formats should be registered in the reference environment", hasUnregistered);
		}
		catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
			fail(e.getClass() + ": " + e.getDescription());
		}
		catch (Exception e) {
			System.out.println(e.getClass() + ": " + e.getMessage());
			fail(e.getClass() + ": " + e.getMessage());
		}
	}
	
	
	@Test
	public void checkRequiredChecksumsUsed() throws ServiceFailure, NotImplemented {
		
		Set<String> chAlgSet = new TreeSet<String>();
		try {
			
			String cnUrl = getReferenceContextCnUrl();
			CNode cn = new CNode(cnUrl);
			ChecksumAlgorithmList cnAlgs = cn.listChecksumAlgorithms();
			
			if (objectList != null && objectList.getCount() > 0) {
				for (ObjectInfo oi : objectList.getObjectInfoList()) {
					chAlgSet.add(oi.getChecksum().getAlgorithm());
				}
			}

			String[] unregistered = new String[chAlgSet.size()];
			int i = 0;
			boolean hasUnregistered = false;
			for (String alg : chAlgSet) {
				System.out.println("checking " + alg);
				if (!cnAlgs.getAlgorithmList().contains(alg)) {
					hasUnregistered = true;
					unregistered[i++] = alg;
				}
			}
			
			System.out.println("Unregistered Algorithms:");
			for (String f : unregistered) {
				System.out.println("  " + f);
			}
			assertFalse("All checksums algorithms used by MN objects should be in the reference environment", hasUnregistered);
		
		} 
		catch (BaseException e) {
			System.out.println(e.getClass() + ": " + e.getDescription());
			fail(e.getClass() + ": " + e.getDescription());
		}
		catch (Exception e) {
			System.out.println(e.getClass() + ": " + e.getMessage());
			fail(e.getClass() + ": " + e.getMessage());
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
