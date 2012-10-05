package org.dataone.integration.it;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.dataone.client.CNode;
import org.dataone.client.MNode;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.ChecksumAlgorithmList;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.junit.Before;
import org.junit.Test;


public class PreRegNodeContentCheck extends ContextAwareTestCaseDataone {

	ObjectList objectList = null;
	
	@Before
	public void fetchMnObjectList() {
		
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

	/**
	 * This looks for identifiers in the potential membernode that might already be 
	 * taken in DataONE.  If it's already there, it checks the size and checksum
	 * to see if the associated object is the same or not.  This could happen when 
	 * multiple nodes that may already hold copies of each other's data come online.
	 * 
	 * Implementation note: This test doesn't hold the entire CN objectList in memory and/or a hashmap,
	 * so should be resistant to the size of the cn objectlist.
	 */
	@Test
	public void checkIdentifierCollisions() {

		String cnUrl = getReferenceContextCnUrl();
		CNode cn = new CNode(cnUrl);
		
		try {
			HashMap<Identifier,ObjectInfo> mnOiMap = new HashMap<Identifier,ObjectInfo>(objectList.getObjectInfoList().size());
			for (ObjectInfo oi: objectList.getObjectInfoList()) {
				mnOiMap.put(oi.getIdentifier(), oi);
			}
			
			int runningTotal = 0;
			int olTotal = 0;
			while (runningTotal < olTotal || olTotal == 0) {
				ObjectList ol = cn.listObjects(null, null, null, null, runningTotal, 1000);
				olTotal = ol.getTotal();
				runningTotal += ol.getObjectInfoList().size();
				for (ObjectInfo oi : ol.getObjectInfoList()) {
					if (mnOiMap.containsKey(oi.getIdentifier())) {
						if ( oi.getSize().equals( mnOiMap.get(oi.getIdentifier()).getSize() ) 
							&& oi.getChecksum().getValue().equals( mnOiMap.get(oi.getIdentifier()).getChecksum().getValue()))
						{
							System.out.println("Identifier " + oi.getIdentifier().getValue() + " is already registered, yet appears to be the same object");
						}
						else {
							System.out.println("Identifier " + oi.getIdentifier().getValue() + " is already registered:");
							System.out.println(String.format("  cn: %s %d %s %s", 
									oi.getIdentifier().getValue(),
									oi.getSize(),
									oi.getChecksum().getAlgorithm(),
									oi.getChecksum().getValue()));
							System.out.println(String.format("  mn: %s %d %s %s", 
									oi.getIdentifier().getValue(),
									mnOiMap.get(oi.getIdentifier()).getSize(),
									mnOiMap.get(oi.getIdentifier()).getChecksum().getAlgorithm(),
									mnOiMap.get(oi.getIdentifier()).getChecksum().getValue()));
						}
					}
				}
			
			}
		}
		catch (BaseException be) {
			be.printStackTrace();
		}
	}
	
	
	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
