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

package org.dataone.integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.dataone.client.exception.ClientSideException;
import org.dataone.client.v1.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.v1.MNode;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.AccessRule;
import org.dataone.service.types.v1.Event;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Subject;


/**
 * Utilities that are useful for generating test data.
 */
public class APITestUtils {



	/**
	 * Generate a list of potential replica target nodes using the capabilities
	 * of the authoritative node and those of the MNs in the NodeList from the CN
	 * @param cn the CN providing the NodeList
	 * @param authNode the authoritative Node for the object
	 * @return the potential replica list of MNs
	 */
	protected static List<NodeReference> generatePotentialReplicaNodeList(CNode cn, Node authNode) {

		// get the full node list from the cn
		NodeList nodeList = null;
		List<Node> nodes = null;

		// get the node list from the CN
		try {
			nodeList = cn.listNodes();
			nodes = nodeList.getNodeList();

		} catch (NotImplemented e) {
			e.printStackTrace();

		} catch (ServiceFailure e) {
			e.printStackTrace();

		}

		//create the list of potential target nodes 
		List<NodeReference> potentialNodeList = new ArrayList<NodeReference>();

		// verify the versions of replication the authNode supports
		List<String> implementedVersions = new ArrayList<String>();
		List<Service> origServices = authNode.getServices().getServiceList();
		for (Service service : origServices) {
			if(service.getName().equals("MNReplication") &&
					service.getAvailable()) {
				implementedVersions.add(service.getVersion());

			}
		}

		// build the potential list of target nodes
		for(Node node : nodes) {

			// only add MNs as targets, excluding the authoritative MN and MNs that are not tagged to replicate
			if ( (node.getType() == NodeType.MN) && node.isReplicate() &&
					!node.getIdentifier().getValue().equals(authNode.getIdentifier().getValue())) {

				for (Service service : node.getServices().getServiceList()) {
					if(service.getName().equals("MNReplication") &&
							implementedVersions.contains(service.getVersion()) &&
							service.getAvailable()) {
						potentialNodeList.add(node.getIdentifier());
					}
				}             
			}
		}
		return potentialNodeList;
	}

	
	
	public static Subject buildSubject(String value) 
	{
		Subject s = new Subject();
		s.setValue(value);
		return s;
	}


	
	public static AccessRule buildAccessRule(String subjectString, Permission permission)
	{
		if (subjectString == null || permission == null) {
			return null;
		}
		AccessRule ar = new AccessRule();
		ar.addSubject(buildSubject(subjectString));
		ar.addPermission(permission);
		return ar;
	}


	
	public static Identifier buildIdentifier(String value) {
		Identifier id = new Identifier();
		id.setValue(value);
		return id;
	}

	
	
	public static ObjectFormatIdentifier buildFormatIdentifier(String value) {
		ObjectFormatIdentifier fid = new ObjectFormatIdentifier();
		fid.setValue(value);
		return fid;
	}



	public static Person buildPerson(Subject subject, String familyName, 
			String givenName, String emailString) 
	{
		String[] badParam = new String[]{};
		Person person = new Person();
		//	  try {
			//		InternetAddress ia = new InternetAddress(emailString, true);
		if (emailString == null || emailString.trim().equals(""))
			badParam[badParam.length] = "emailString";
		if (familyName == null || familyName.trim().equals(""))
			badParam[badParam.length] = "familyName";
		if (givenName == null || givenName.trim().equals(""))
			badParam[badParam.length] = "givenName";
		if (subject == null || subject.getValue().equals(""))
			badParam[badParam.length] = "subject";

		if (badParam.length > 0)
			throw new IllegalArgumentException("null or empty string values for parameters: " + badParam);

		//	} catch (AddressException e) {
		//		// thrown by IndernetAddress constructor
		//	}

		person.addEmail(emailString);
		person.addGivenName(givenName);
		person.setFamilyName(familyName);
		person.setSubject(subject);
		return person;
	}



	protected static int countLocationsWithResolve(CNode cn, Identifier pid) throws InvalidToken, ServiceFailure,
	NotAuthorized, NotFound, InvalidRequest, NotImplemented {

		ObjectLocationList oll = cn.resolve(null, pid);
		List<ObjectLocation> locs = oll.getObjectLocationList();
		return locs.toArray().length;
	}


	/**
	 * checks a member node to see if it implements a tier.  Interrogates the 
	 * services returned by mn.getCapabilities().  If any services of a given
	 * tier are implemented, it returns true.
	 * @param mn - an MNode object for the Member Node to check.
	 * @param tierName
	 * @return
	 * @throws NotImplemented
	 * @throws ServiceFailure
	 */
	public static boolean isTierImplemented(MNode mn, String tierName) 
	throws NotImplemented, ServiceFailure 
	{
		Node node = mn.getCapabilities();

		if (tierName.equalsIgnoreCase("Tier1")) {
			if (isServiceAvailable(node, "MNCore") || isServiceAvailable(node,"MNRead"))
				return true;
		} 
		else if (tierName.equalsIgnoreCase("Tier2")) {
			if (isServiceAvailable(node, "MNAuthorization"))
				return true;
		}
		else if (tierName.equalsIgnoreCase("Tier3")) {
			if (isServiceAvailable(node, "MNStorage"))
				return true;
		}
		else if (tierName.equalsIgnoreCase("Tier4")) {
			if (isServiceAvailable(node, "MNReplication"))
				return true;
		}
		return false;
	}


	/**
	 * Given a node object will determine if the provided serviceName
	 * is available
	 * @param node
	 * @param serviceName
	 * @return
	 */
	public static boolean isServiceAvailable(Node node, String serviceName)
	{
		// create a single-node nodelist
		NodeList nl = new NodeList();
		nl.addNode(node);

		//		Set<Node> n = NodelistUtil.selectNodesByService(nl, serviceName, null, true);
		Set<Node> n = selectNodesByService(nl, serviceName, null, true);
		if (n.isEmpty()) {
			return false;
		}
		return true;
	}


	/**
	 * duplicate of NodelistUtil copied to allow testing of release candidates
	 * @param nodeList
	 * @param serviceName
	 * @param version
	 * @param isAvailable
	 * @return
	 */
	public static Set<Node> selectNodesByService(NodeList nodeList, String serviceName, String version, boolean isAvailable)
	{
		Set<Node> nodeSet = new TreeSet<Node>();
		for(int i=0; i < nodeList.sizeNodeList(); i++) 
		{
			Node node = nodeList.getNode(i);
			for (Service service: node.getServices().getServiceList())
			{	
				if (service.getName().equalsIgnoreCase(serviceName)) {
					boolean availability = true;
					if (service.getAvailable() != null) {
						availability = service.getAvailable().booleanValue();
					}
					if (availability == isAvailable) {
						if (version != null) {
							if (service.getVersion().equalsIgnoreCase(version)) {
								nodeSet.add(node);
							}
						} else {
							nodeSet.add(node);
							break;
						}
					}
				}
			}
		}
		return nodeSet;
	}
	
	/**
	 * Calls list objects iteratively using the paging mechanism in order to
	 * respect any server-imposed paging limits
	 * @param cca
	 * @param fromDate
	 * @param toDate
	 * @param formatid
	 * @param replicaStatus
	 * @param start
	 * @param count
	 * @return
	 * @throws InvalidRequest
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws NotImplemented
	 * @throws ServiceFailure
	 */
	public static ObjectList pagedListObjects(CommonCallAdapter cca, Date fromDate, Date toDate, 
      ObjectFormatIdentifier formatid, Boolean replicaStatus, Integer start, Integer count) 
    throws InvalidRequest, InvalidToken, NotAuthorized, NotImplemented, ServiceFailure
    {

		if (count != null && count <= 0) {
			return cca.listObjects(null, fromDate, toDate, formatid, replicaStatus, start, 0);
		} 
		ObjectList ol = cca.listObjects(null, fromDate, toDate, formatid, replicaStatus, start, null);
		if (ol.getTotal() == ol.sizeObjectInfoList()) {
			// don't need to ask for more
			if (count != null && ol.sizeObjectInfoList() > count) {
				// need to trim the object list to match the requested amount
				ol.setObjectInfoList(ol.getObjectInfoList().subList(0, count));
				ol.setCount(count);
			}
		}
		
		count = -1;
		int retrieved = ol.sizeObjectInfoList();
		int serverPageSize = ol.sizeObjectInfoList();  // server is happy to return this amount at a time.
		int totalNeeded = count > ol.getTotal() ? count : ol.getTotal();
		int remaining = totalNeeded - retrieved;
		while (remaining > 0) {
			int pageSize = remaining < serverPageSize ? remaining : serverPageSize;
			start = retrieved;
			ObjectList nextList = cca.listObjects(null, fromDate, toDate, formatid, replicaStatus, start, pageSize);
			retrieved += nextList.sizeObjectInfoList();
			remaining = totalNeeded - retrieved;
			ol.getObjectInfoList().addAll(nextList.getObjectInfoList());
		}
		ol.setCount(ol.sizeObjectInfoList());
		return ol;
    }

	
	/**
	 * Calls list objects iteratively using the paging mechanism in order to
	 * respect any server-imposed paging limits
	 * @param cca
	 * @param fromDate
	 * @param toDate
	 * @param formatid
	 * @param replicaStatus
	 * @param start
	 * @param count
	 * @return
	 * @throws InvalidRequest
	 * @throws InvalidToken
	 * @throws NotAuthorized
	 * @throws NotImplemented
	 * @throws ServiceFailure
	 * @throws InsufficientResources 
	 * @throws ClientSideException 
	 */
	public static Log pagedGetLogRecords(CommonCallAdapter cca, Date fromDate, Date toDate, 
      String event, String pidFilter, Integer start, Integer count) 
      throws InvalidToken, InvalidRequest, ServiceFailure, NotAuthorized, NotImplemented, 
      InsufficientResources, ClientSideException 
    {

		if (count != null && count <= 0) {
			return cca.getLogRecords(null, fromDate, toDate, event, pidFilter, start, 0);
		} 
		
		Log entries = cca.getLogRecords(null, fromDate, toDate, event, pidFilter, start, null);
		if (entries.getTotal() == entries.sizeLogEntryList()) {
			// don't need to ask for more
			if (count != null && entries.sizeLogEntryList() > count) {
				// need to trim the object list to match the requested amount
				entries.setLogEntryList(entries.getLogEntryList().subList(0, count));
				entries.setCount(count);
			}
		}
		
		count = -1;
		int retrieved = entries.sizeLogEntryList();
		int serverPageSize = entries.sizeLogEntryList();  // server is happy to return this amount at a time.
		int totalNeeded = (count > entries.getTotal()) ? count : entries.getTotal();
		int remaining = totalNeeded - retrieved;
		while (remaining > 0) {
			int pageSize = remaining < serverPageSize ? remaining : serverPageSize;
			start = retrieved;
			Log nextList = cca.getLogRecords(null, fromDate, toDate, event, pidFilter, start, pageSize);
			retrieved += nextList.sizeLogEntryList();
			remaining = totalNeeded - retrieved;
			entries.getLogEntryList().addAll(nextList.getLogEntryList());
		}
		entries.setCount(entries.sizeLogEntryList());
		return entries;
    }

	
}
