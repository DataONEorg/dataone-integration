package org.dataone.integration.it.functional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dataone.client.v1.CNode;
import org.dataone.client.v1.MNode;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.dataone.service.util.D1Url;
import org.junit.After;
import org.junit.Before;

public class SyncInventoryIT extends ContextAwareTestCaseDataone {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

//	@Test
	public void test() throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure {
		
		MNode edacNode = new MNode("https://gstore.unm.edu/dataone");
		
		ObjectList ol = edacNode.listObjects();
		
		List<SystemMetadata> sysMetaList = new ArrayList<SystemMetadata>();
		for(ObjectInfo oi : ol.getObjectInfoList()) {
			try {
				SystemMetadata sysmeta = edacNode.getSystemMetadata(oi.getIdentifier());
				sysMetaList.add(sysmeta);
				while (sysmeta.getObsoletes() != null) {
					try {
						sysmeta = edacNode.getSystemMetadata(sysmeta.getObsoletes());
						sysMetaList.add(sysmeta);
					} catch (NotFound nf) {
						log.warn(oi.getIdentifier().getValue() + " (obsoleted) is not NotFound on the MN");
					}
				}
				if (sysmeta.getObsoletes() != null) {
					
				}
			} catch (NotFound nf) {
				log.warn(oi.getIdentifier().getValue() + " is not NotFound on the MN");
			}
		}
		System.out.println("output format:  predecessor -> identifier -> successor");
		for(SystemMetadata sys : sysMetaList) {
			
			String message = String.format("( %40s -> %40s -> %40s )",
					sys.getObsoletes() != null ? sys.getObsoletes().getValue() : "",
			        sys.getIdentifier().getValue(),		
			        sys.getObsoletedBy() != null ? sys.getObsoletedBy().getValue() : ""
					);
			System.out.println(message);
		}
		
		CNode rrcn = new CNode("https://cn-stage.test.dataone.org/cn");
		NodeList nl = rrcn.listNodes();
		Set<Node> nodes = NodelistUtil.selectNodes(nl, NodeType.CN);
		
		System.out.println("CN instances...");
		CNode[] cns = new CNode[nodes.size()-1];
		int i=0;
		for (Node cnInst : nodes) {
			if (!cnInst.getBaseURL().equals("https://cn-stage.test.dataone.org/cn")) {
				System.out.println(cnInst.getIdentifier().getValue() + "    " + cnInst.getBaseURL());
				cns[i++] = new CNode(cnInst.getBaseURL());
			}
		}
		Iterator<SystemMetadata> it = sysMetaList.iterator();
		
		List<List<String>> table = new ArrayList<List<String>>();
		while(it.hasNext()) {
			SystemMetadata s = it.next();
			Identifier pid = s.getIdentifier();
			for (CNode cn : cns) {
				List<String> line = new ArrayList<String>();
				line.add(pid.getValue());
				line.add(StringUtils.substring(cn.getNodeBaseServiceUrl(), 8, 20));
				try {
					SystemMetadata sysmeta = cn.getSystemMetadata(pid);
					line.add("y");
					line.add(s.getAuthoritativeMemberNode().getValue());
					line.add(ObjectFormatCache.getInstance().getFormat(sysmeta.getFormatId()).getFormatType().toString());
					line.add(sysmeta.getFormatId().getValue());
					if (s.getObsoletedBy() != null) {
						if (s.getObsoletedBy().getValue() == null) {
							line.add("");
						} else {
							line.add("obsolete");
						}
					} else {
						line.add("");
					}
					D1Url q = new D1Url();
					q.addNonEmptyParamPair("q", "id:" + pid.getValue());
					String searchString = "?" + q.getAssembledQueryString();
					ObjectList is2 = cn.search("solr", searchString);
					line.add(String.valueOf(is2.getTotal()));
//					System.out.println(" ...solr results returned: " + is2.getTotal());
					InputStream is = cn.get(pid);
					line.add("y");
//					System.out.println(" ...object found.");
					
				
				} 
				catch (NotFound nf) {
//					System.out.println(cn.getLatestRequestUrl());
//					System.out.println(nf.getMessage());
				}
				table.add(line);
			}
		}
		

		System.out.println("PID\tNODE\tSysMeta\tAuthMN\tF-TYPE\tFormat\tObsolete\tSearch\tGet");
		Iterator<List<String>> tableIt = table.iterator();
		while (tableIt.hasNext()) {
			System.out.println(StringUtils.join(tableIt.next(),"\t"));
		}
	}

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return "Inventories the EDAC node with regards to syncrhonization issues in the STAGE environment";
	}

	
}
