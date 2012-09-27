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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.io.IOUtils;
import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.D1Object;
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
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Service;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.types.v1.SubjectList;
import org.dataone.service.types.v1.SystemMetadata;
import org.dataone.service.types.v1.util.AccessUtil;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v1.util.NodelistUtil;
import org.dataone.service.util.Constants;


/**
 * Utilities that are useful for generating test data.
 */
public class ExampleUtilities {
	
	public static final String CHECKSUM_ALGORITHM = "MD5";
	
	// common object formats to test
	protected static final String FORMAT_TEXT_PLAIN  = "text/plain";
	protected static final String FORMAT_TEXT_CSV    = "text/csv";
	protected static final String FORMAT_EML_2_0_0   = "eml://ecoinformatics.org/eml-2.0.0";
	protected static final String FORMAT_EML_2_0_1   = "eml://ecoinformatics.org/eml-2.0.1";
	protected static final String FORMAT_EML_2_1_0   = "eml://ecoinformatics.org/eml-2.1.0";
	protected static final String FORMAT_EML_2_1_1   = "eml://ecoinformatics.org/eml-2.1.1";

	// paths to common science data and metadata examples for the above formats
	protected static final String SCIDATA_TEXT_PLAIN = "/d1_testdocs/eml200/IPCC.200802107062739.1"; 
	protected static final String SCIDATA_TEXT_CSV   = "/d1_testdocs/eml201/TPT001_018MHP2000R00_20110121.40.1.csv"; 
	protected static final String SCIMETA_EML_2_0_0  = "/d1_testdocs/eml200/dpennington.195.2"; 
	protected static final String SCIMETA_EML_2_0_1  = "/d1_testdocs/eml201/TPT001_018MHP2000R00_20110121.50.1.xml"; 
	protected static final String SCIMETA_EML_2_1_0  = "/d1_testdocs/eml210/peggym.130.4"; 
	// TODO: protected static final String SCIMETA_EML_2_1_1  = "need to get a 2.1.1 test doc"; 
	
	
	protected final static String preferredMNId = "c3p0";

	

	/**
	 * creates the identifier, data inputstream, and sysmetadata for testing purposes
	 * the rightsHolder is set to the subject of the current certificate (user)
	 * 
	 * uses a default text/plain data source
	 */
    public static Object[] generateTestSciDataPackage(String idString, boolean isPrefix)
        throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException {
        
        return generateTestDataPackage(idString, isPrefix, FORMAT_TEXT_PLAIN);
    }
    
	/**
	 * creates the identifier, data inputstream, and sysmetadata for testing purposes
	 * the rightsHolder is set to the subject of the current certificate (user)
	 * 
	 * uses a default text/plain data source
	 */
    public static Object[] generateTestSciMetaDataPackage(String idString, boolean isPrefix)
        throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException {
        
        return generateTestDataPackage(idString, isPrefix, FORMAT_EML_2_1_0);
    }
  
    
    /**
     * Provides a byte array representation of the object for the specified format
     * Accepts any of the types defined as constants in ExampleUtilities.  eg:
     * ExampleUtilities.FORMAT_EML_2_0_0
     * @param formatIDString
     * @return
     * @throws IOException
     */
    public static byte[] getExampleObjectOfType(String formatIDString) throws IOException 
    {
        byte[] contentBytes = null;
        InputStream fileStream = null;
        
        // choose a test object file based on the object format passed in
        if ( formatIDString == FORMAT_TEXT_PLAIN ) {
            fileStream = ExampleUtilities.class.getResourceAsStream(SCIDATA_TEXT_PLAIN);
            
        } else if ( formatIDString == FORMAT_EML_2_0_0 ) {
            fileStream = ExampleUtilities.class.getResourceAsStream(SCIMETA_EML_2_0_0);

        } else if ( formatIDString == FORMAT_EML_2_0_1 ) {
            fileStream = ExampleUtilities.class.getResourceAsStream(SCIMETA_EML_2_0_1);

        } else if ( formatIDString == FORMAT_EML_2_1_0 ) {
            fileStream = ExampleUtilities.class.getResourceAsStream(SCIMETA_EML_2_1_0);
        
        //TODO: get an EML 2.1.1 test doc in place
        //} else if ( formatIDString == FORMAT_EML_2_1_1 ) {
        //    fileStream = ExampleUtilities.class.getResourceAsStream(SCIMETA_EML_2_1_1);
        }   
        
        contentBytes = IOUtils.toByteArray(fileStream); 
        return contentBytes;
    }
    
	
  /**
   * creates the identifier, data inputstream, and sysmetadata for testing purposes
   * the rightsHolder is set to the subject of the current certificate (user)
   */
    public static Object[] generateTestDataPackage(String idString,
        boolean isPrefix, String formatString)
        throws NoSuchAlgorithmException, NotFound, InvalidRequest, IOException {
        
        if (isPrefix) {
            idString += generateIdentifier();
        }
        Identifier guid = new Identifier();
        guid.setValue(idString);

      
        byte[] contentBytes = getExampleObjectOfType(formatString);        
        
        // figure out who we are
        String ownerX500 = idString + "_unknownCert";
        try {
            X509Certificate certificate = CertificateManager.getInstance()
                    .loadCertificate();
            if (certificate != null) {
                ownerX500 = CertificateManager.getInstance().getSubjectDN(
                        certificate);
                // sysMeta.getRightsHolder().setValue(ownerX500);
                // sysMeta.getSubmitter().setValue(ownerX500);
            }
        } catch (Exception e) {
            // ignore
        }

        D1Object d1o = new D1Object(guid, contentBytes, formatString,
                ownerX500, "authNode");
        SystemMetadata sysMeta = d1o.getSystemMetadata();

        // match the submitter as the cert DN

        sysMeta.setAccessPolicy(AccessUtil.createSingleRuleAccessPolicy(
                new String[] { Constants.SUBJECT_PUBLIC },
                new Permission[] { Permission.READ }));

        ByteArrayInputStream bis = new ByteArrayInputStream(contentBytes);
        return new Object[] { guid, bis, sysMeta };
    }  
	
	
	
	

	/**
	 * Create a unique identifier for testing insert and update.
	 * 
	 * @return a String identifier based on the current date and time
	 */
	protected static String generateIdentifier() {
		return ExampleUtilities.generateTimeString();
	}
	
    /** Generate a timestamp for use in IDs. */
    private static String generateTimeString()
    {
        StringBuffer pid = new StringBuffer();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        pid.append(calendar.get(Calendar.YEAR));
        pid.append(calendar.get(Calendar.DAY_OF_YEAR));
        pid.append(calendar.get(Calendar.HOUR_OF_DAY));
        pid.append(calendar.get(Calendar.MINUTE));
        pid.append(calendar.get(Calendar.SECOND));
        pid.append(calendar.get(Calendar.MILLISECOND));

        return pid.toString();
    }

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
    
    /** Generate a SystemMetadata object with bogus data. */
    @Deprecated
    protected static SystemMetadata generateSystemMetadata(
            Identifier pid, String objectFormatIdString, InputStream source, String mnIdentifier) {

    	ObjectFormatIdentifier ofid = new ObjectFormatIdentifier();
    	ofid.setValue(objectFormatIdString);
    	if (mnIdentifier == null)
    		return generateSystemMetadata(pid, ofid, source, preferredMNId);
    	else
    		return generateSystemMetadata(pid, ofid, source, mnIdentifier);
    }
    
    /** Generate a SystemMetadata object with bogus data. */
    @Deprecated
    protected static SystemMetadata generateSystemMetadata(
            Identifier pid, ObjectFormatIdentifier fmtid, InputStream source) {

        return generateSystemMetadata(pid, fmtid, source, preferredMNId);
    }
    
    /** Generate a SystemMetadata object with bogus data. */
    @Deprecated
    protected static SystemMetadata generateSystemMetadata(
            Identifier pid, ObjectFormatIdentifier fmtid, InputStream source, String mnIdentifier) 
    {
    	
        SystemMetadata sysmeta = new SystemMetadata();
        sysmeta.setIdentifier(pid);
        sysmeta.setFormatId(fmtid);
        sysmeta.setSize(BigInteger.valueOf(12));
        Subject submitter = new Subject();
        String dn = "uid=jones,o=NCEAS,dc=ecoinformatics,dc=org";
        submitter.setValue(dn);
        sysmeta.setSubmitter(submitter);
        Subject rightsHolder = new Subject();
        rightsHolder.setValue(dn);
        sysmeta.setRightsHolder(rightsHolder);
        sysmeta.setDateSysMetadataModified(new Date());
        sysmeta.setDateUploaded(new Date());
        NodeReference originMemberNode = new NodeReference();
        originMemberNode.setValue(mnIdentifier);
        sysmeta.setOriginMemberNode(originMemberNode);
        NodeReference authoritativeMemberNode = new NodeReference();
        authoritativeMemberNode.setValue(mnIdentifier);
        sysmeta.setAuthoritativeMemberNode(authoritativeMemberNode);
        Replica firstReplica = new Replica();
        NodeReference replicaNodeReference = new NodeReference();
        replicaNodeReference.setValue(mnIdentifier);
        firstReplica.setReplicaMemberNode(replicaNodeReference);
        firstReplica.setReplicationStatus(ReplicationStatus.COMPLETED);
        firstReplica.setReplicaVerified(new Date());
        sysmeta.addReplica(firstReplica);
        sysmeta.setSerialVersion(BigInteger.valueOf(1));
        Checksum checksum = null;
        try
        {
            checksum = ChecksumUtil.checksum(source, CHECKSUM_ALGORITHM);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            System.out.println("could not create the checksum for the document: " + e.getMessage());
            e.printStackTrace();
        }
        sysmeta.setChecksum(checksum);
        return sysmeta;
    }
 
    
    protected static String extractObjectListTotalAttribute(String ol) {
    	Pattern pat = Pattern.compile("total=\"\\d+\"");

		Matcher mat = pat.matcher(ol);
		String totalPattern = null;
		if (mat.find())
			totalPattern = mat.group();
		return totalPattern;
    }    
    
    protected static SubjectList buildSubjectList(Object persons) {
    	SubjectList sl = new SubjectList();
    	for(String pString: (String[])persons) {
    		Person p = new Person();
    		Subject s = new Subject();
    		s.setValue(pString);
    		p.setSubject(s);
    		
    		sl.addSubject(p.getSubject());
    	}
    	return sl;
    }
    
    protected static Subject buildSubject(String subjectValue) {
    	Subject s = new Subject();
    	s.setValue(subjectValue);
    	return s;
    }
    
	public static Identifier doCreateNewObject(D1Node d1Node, String idPrefix) throws ServiceFailure,
	NotImplemented, InvalidToken, NotAuthorized, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata, NotFound, InvalidRequest
	{
//		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		Session token = null; // mn.login(principal, "kepler");
		String idString = idPrefix + ExampleUtilities.generateIdentifier();
		Identifier pid = new Identifier();
		pid.setValue(idString);
		InputStream objectStream = 
			new Throwable().getStackTrace()[2].getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
//		InputStream objectStream = Caller.getClass().getResourceAsStream(
//				"/d1_testdocs/knb-lter-cdr.329066.1.data");
		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(pid, "text/csv", objectStream,null);
		Identifier rpid = null;
		objectStream = 
            new Throwable().getStackTrace()[2].getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");

		if (d1Node instanceof MNode) {
			rpid = ((MNode) d1Node).create(token, pid, objectStream, sysmeta);
		} else {
			rpid = ((CNode) d1Node).create(token, pid, objectStream, sysmeta);
		}
		assertThat("checking that returned pid matches given ", pid.getValue(), is(rpid.getValue()));
//		mn.setAccessPolicy(token, rpid, ContextAwareTestCaseDataone.buildPublicReadAccessPolicy());
		System.out.println("new document created on " + d1Node.getNodeBaseServiceUrl() + 
		        " with pid " + rpid.getValue());
		InputStream is = d1Node.get(null,pid);
		return rpid;
	}
  
	/**
	 * Utility method for getting a mock session object
	 * 
	 * @return session - the session object with a Subject set and 
	 */
  protected static Session getTestSession() {
		
  	Session session = new Session();
  	String subjectStr  = "uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org";
  	List<Group> groupList= new ArrayList<Group>();
  	Group group1 = new Group();
  	group1.setGroupName("cn=test-group,dc=ecoinformatics,dc=org");
  	groupList.add(group1);
  	Group group2 = new Group();
  	group1.setGroupName("cn=test-group2,dc=ecoinformatics,dc=org");
  	groupList.add(group2);
  	
  	Subject subject = new Subject();
  	subject.setValue(subjectStr);
  	SubjectInfo subjectInfo = new SubjectInfo();
  	subjectInfo.setGroupList(groupList);
  	
  	session.setSubject(subject);
  	session.setSubjectInfo(subjectInfo);
  	
  	return session;
  	
  }
  
  public Person buildPerson(Subject subject, String familyName, 
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
}
