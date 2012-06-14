package org.dataone.integration.it;

import java.util.HashMap;

import org.dataone.client.CNode;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.junit.Test;


public class EnvironmentsCheck {

	String[] rrCns = new String[] { 
			"https://cn-dev-rr.dataone.org/cn",
			"https://cn-sandbox.dataone.org/cn",
			"https://cn-stage.dataone.org/cn",
			"https://cn.dataone.org/cn"
	};
	
	@Test
	public void checkEnvironment_exclusivity() 
	{
		HashMap<String, Integer> nodeRefMap = new HashMap<String,Integer>();
		HashMap<String, Integer> baseUrlMap = new HashMap<String, Integer>();
		
		
		System.out.println(String.format("%-16s  %-32s %2s    %-64s %2s  %s",
				"environment",
				"NodeReference",
				"#",
				"baseUrl",
				"#",
				"duplicate?"
				));
		
		int duplicates = 0;
		
		// pull nodelists for each environment
		for (String baseUrl: rrCns) {
			CNode cn = new CNode(baseUrl);
			NodeList nl;
			try {
				nl = cn.listNodes();
				for (Node n: nl.getNodeList()) {

					int idCount = 0;
					if (nodeRefMap.containsKey(n.getIdentifier().getValue())) {
						idCount = nodeRefMap.get(n.getIdentifier().getValue());
					}
					nodeRefMap.put(n.getIdentifier().getValue(), new Integer(++idCount));
					
					int urlCount = 0;
					if (baseUrlMap.containsKey(n.getBaseURL())) {
						urlCount = baseUrlMap.get(n.getBaseURL());
					}
					baseUrlMap.put(n.getBaseURL(), new Integer(++urlCount));

					// increment the failure flag if we found a duplicate
					duplicates += idCount - 1 + urlCount - 1;
					
					
					System.out.println(String.format("%-16s  %-32s %2d    %-64s %2d  %s",
							baseUrl.replaceFirst("https://", "").replace(".dataone.org/cn",""),
							n.getIdentifier().getValue(),
							idCount,
							n.getBaseURL(),
							urlCount,
							idCount > 1 || urlCount > 1 ? "*" : ""
							));
				
				}
			} catch (NotImplemented e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got NotImplemented::" + e.getDescription());
			} catch (ServiceFailure e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got ServiceFailure::" + e.getDescription());
			}
		}
		System.out.println();
		System.out.println("Identifier distribution");
		System.out.println("=======================");
		for (String key : nodeRefMap.keySet()) {
			System.out.println(String.format("%-32s  %3d",key, nodeRefMap.get(key)));
		}
		System.out.println();
		System.out.println("BaseUrl distribution");
		System.out.println("====================");
		for (String baseUrl : baseUrlMap.keySet()) {
			System.out.println(String.format("%-64s  %3d",baseUrl, baseUrlMap.get(baseUrl)));
		}

		org.junit.Assert.assertEquals("Expect no duplicate node identifiers or baseUrls across the separate environments", duplicates, 0);
		
		
	}
	
	
}
