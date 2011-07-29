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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dataone.client.CNode;
import org.dataone.client.MNode;
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
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v1.util.ObjectFormatServiceImpl;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.ChecksumAlgorithm;
import org.dataone.service.types.v1.Group;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatIdentifier;
import org.dataone.service.types.v1.ObjectLocation;
import org.dataone.service.types.v1.ObjectLocationList;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Session;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectList;
import org.dataone.service.types.v1.SystemMetadata;


/**
 * Utilities that are useful for generating test data.
 */
public class ExampleUtilities {
	protected final static String preferredMNId = "c3p0";
	protected final static String EML2_0_0 = "EML2_0_0";
	protected final static String EML2_0_1 = "EML2_0_1";
	protected final static String EML2_1_0 = "EML2_1_0";
	
	protected static final String ALLOWFIRST = "allowFirst";
	protected static final String DENYFIRST = "denyFirst";
			
	// header blocks
	protected final static String testEml_200_Header = "<?xml version=\"1.0\"?><eml:eml"
		+ " xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.0\""
		+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
		+ " packageId=\"eml.1.1\" system=\"knb\""
		+ " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.0 eml.xsd\""
		+ " scope=\"system\">";
	
	protected final static String testEml_201_Header = "<?xml version=\"1.0\"?><eml:eml"
		+ " xmlns:eml=\"eml://ecoinformatics.org/eml-2.0.1\""
		+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
		+ " packageId=\"eml.1.1\" system=\"knb\""
		+ " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.0.1 eml.xsd\""
		+ " scope=\"system\">";
	
	protected final static String testEml_210_Header = "<?xml version=\"1.0\"?><eml:eml"
			+ " xmlns:eml=\"eml://ecoinformatics.org/eml-2.1.0\""
			+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
			+ " packageId=\"eml.1.1\" system=\"knb\""
			+ " xsi:schemaLocation=\"eml://ecoinformatics.org/eml-2.1.0 eml.xsd\""
			+ " scope=\"system\">";

	protected final static String testEmlCreatorBlock = "<creator scope=\"document\">                                       "
			+ " <individualName>                                                  "
			+ "    <surName>Smith</surName>                                       "
			+ " </individualName>                                                 "
			+ "</creator>                                                         ";

	protected final static String testEmlContactBlock = "<contact scope=\"document\">                                       "
			+ " <individualName>                                                  "
			+ "    <surName>Jackson</surName>                                     "
			+ " </individualName>                                                 "
			+ "</contact>                                                         ";

	protected final static String testEmlInlineBlock1 = "<inline>                                                           "
			+ "  <admin>                                                          "
			+ "    <contact>                                                      "
			+ "      <name>Operator</name>                                        "
			+ "      <institution>PSI</institution>                               "
			+ "    </contact>                                                     "
			+ "  </admin>                                                         "
			+ "</inline>                                                          ";

	protected final static String testEmlInlineBlock2 = "<inline>                                                           "
			+ "  <instrument>                                                     "
			+ "    <instName>LCQ</instName>                                       "
			+ "    <source type=\"ESI\"></source>                                 "
			+ "    <detector type=\"EM\"></detector>                              "
			+ "  </instrument>                                                    "
			+ "</inline>                                                          ";

	/*
	 * Returns an access block base on params passed and the default perm order -
	 * allow first
	 */
	protected static String getAccessBlock(String principal, boolean grantAccess, boolean read,
			boolean write, boolean changePermission, boolean all) {
		return getAccessBlock(principal, grantAccess, read, write, changePermission, all,
				ALLOWFIRST);
	}
	
	/**
	 * This function returns an access block based on the params passed
	 */
	protected static String getAccessBlock(String principal, boolean grantAccess, boolean read,
			boolean write, boolean changePermission, boolean all, String permOrder) {
		String accessBlock = "<access "
				+ "authSystem=\"ldap://ldap.ecoinformatics.org:389/dc=ecoinformatics,dc=org\""
				+ " order=\"" + permOrder + "\"" + " scope=\"document\"" + ">";

		accessBlock += generateOneAccessRule(principal, grantAccess, read, write,
				changePermission, all);
		accessBlock += "</access>";

		return accessBlock;

	}

	/*
	 * Gets eml access block base on given acccess rules and perm order
	 */
	protected static String getAccessBlock(Vector<String> accessRules, String permOrder) {
		String accessBlock = "<access "
				+ "authSystem=\"ldap://ldap.ecoinformatics.org:389/dc=ecoinformatics,dc=org\""
				+ " order=\"" + permOrder + "\"" + " scope=\"document\"" + ">";
		// adding rules
		if (accessRules != null && !accessRules.isEmpty()) {
			for (int i = 0; i < accessRules.size(); i++) {
				String rule = (String) accessRules.elementAt(i);
				accessBlock += rule;

			}
		}
		accessBlock += "</access>";
		return accessBlock;
	}

	
	
	/*
	 * Generates a access rule for given parameter. Note this xml portion
	 * doesn't include <access></access>
	 */
	protected static String generateOneAccessRule(String principal, boolean grantAccess,
			boolean read, boolean write, boolean changePermission, boolean all) {
		String accessBlock = "";

		if (grantAccess) {
			accessBlock = "<allow>";
		} else {
			accessBlock = "<deny>";
		}

		accessBlock = accessBlock + "<principal>" + principal + "</principal>";

		if (all) {
			accessBlock += "<permission>all</permission>";
		} else {
			if (read) {
				accessBlock += "<permission>read</permission>";
			}
			if (write) {
				accessBlock += "<permission>write</permission>";
			}
			if (changePermission) {
				accessBlock += "<permission>changePermission</permission>";
			}
		}

		if (grantAccess) {
			accessBlock += "</allow>";
		} else {
			accessBlock += "</deny>";
		}
		return accessBlock;

	}

	/**
	 * This function returns a valid eml document with no access rules 
	 */
	protected static String generateEmlDocument(String title, String emlVersion, String inlineData1,
			String inlineData2, String onlineUrl1, String onlineUrl2,
			String docAccessBlock, String inlineAccessBlock1, String inlineAccessBlock2,
			String onlineAccessBlock1, String onlineAccessBlock2) {

//		debug("getTestEmlDoc(): title=" + title + " inlineData1=" + inlineData1
//				+ " inlineData2=" + inlineData2 + " onlineUrl1=" + onlineUrl1
//				+ " onlineUrl2=" + onlineUrl2 + " docAccessBlock=" + docAccessBlock
//				+ " inlineAccessBlock1=" + inlineAccessBlock1 + " inlineAccessBlock2="
//				+ inlineAccessBlock2 + " onlineAccessBlock1=" + onlineAccessBlock1
//				+ " onlineAccessBlock2=" + onlineAccessBlock2);
		String testDocument = "";
		String header;
		if (emlVersion == EML2_0_0) {
			header = testEml_200_Header;
		} else if (emlVersion == EML2_0_1) {
			header = testEml_201_Header;
		} else {
			header = testEml_210_Header;
		}
		testDocument += header;
		
		// if this is a 2.1.0+ document, the document level access block sits
		// at the same level and before the dataset element.
		if (docAccessBlock != null && emlVersion.equals(EML2_1_0)) {
			testDocument += docAccessBlock;
		}
		
		testDocument += "<dataset scope=\"document\"><title>"
				+ title + "</title>" + testEmlCreatorBlock;

		if (inlineData1 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"inlineEntity1\">"
					+ inlineData1 + "</distribution>";
		}
		if (inlineData2 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"inlineEntity2\">"
					+ inlineData2 + "</distribution>";
		}
		if (onlineUrl1 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"onlineEntity1\">"
					+ "<online><url function=\"download\">" + onlineUrl1
					+ "</url></online></distribution>";
		}
		if (onlineUrl2 != null) {
			testDocument = testDocument
					+ "<distribution scope=\"document\" id=\"onlineEntity2\">"
					+ "<online><url function=\"download\">" + onlineUrl2
					+ "</url></online></distribution>";
		}
		testDocument += testEmlContactBlock;

		// if this is a 2.0.X document, the document level access block sits
		// inside the dataset element.
		if (docAccessBlock != null && 
				(emlVersion.equals(EML2_0_0) || emlVersion.equals(EML2_0_1))) {
			testDocument += docAccessBlock;
		}

		testDocument += "</dataset>";

		if (inlineAccessBlock1 != null) {
			testDocument += "<additionalMetadata>";
			testDocument += "<describes>inlineEntity1</describes>";
			testDocument += inlineAccessBlock1;
			testDocument += "</additionalMetadata>";
		}

		if (inlineAccessBlock2 != null) {
			testDocument += "<additionalMetadata>";
			testDocument += "<describes>inlineEntity2</describes>";
			testDocument += inlineAccessBlock2;
			testDocument += "</additionalMetadata>";
		}

		if (onlineAccessBlock1 != null) {
			testDocument += "<additionalMetadata>";
			testDocument += "<describes>onlineEntity1</describes>";
			testDocument += onlineAccessBlock1;
			testDocument += "</additionalMetadata>";
		}

		if (onlineAccessBlock2 != null) {
			testDocument += "<additionalMetadata>";
			testDocument += "<describes>onlineEntity2</describes>";
			testDocument += onlineAccessBlock2;
			testDocument += "</additionalMetadata>";
		}

		testDocument += "</eml:eml>";

		// System.out.println("Returning following document" + testDocument);
		return testDocument;
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
        StringBuffer guid = new StringBuffer();

        // Create a calendar to get the date formatted properly
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        guid.append(calendar.get(Calendar.YEAR));
        guid.append(calendar.get(Calendar.DAY_OF_YEAR));
        guid.append(calendar.get(Calendar.HOUR_OF_DAY));
        guid.append(calendar.get(Calendar.MINUTE));
        guid.append(calendar.get(Calendar.SECOND));
        guid.append(calendar.get(Calendar.MILLISECOND));

        return guid.toString();
    }

    
    
    /** Generate a SystemMetadata object with bogus data. */
    protected static SystemMetadata generateSystemMetadata(
            Identifier guid, String objectFormatIdString, InputStream source) {
        
        return generateSystemMetadata(guid, objectFormatIdString, source, preferredMNId);
    }
    
    /** Generate a SystemMetadata object with bogus data. */
    protected static SystemMetadata generateSystemMetadata(
            Identifier guid, String objectFormatIdString, InputStream source, String mnIdentifier) 
    {
    	

    	ObjectFormatIdentifier ofid = new ObjectFormatIdentifier();
    	ofid.setValue(objectFormatIdString);
    	ObjectFormat objectFormat = null;
    	
    	// swallowing these exceptions for now, in v0.6.3 will be replacing 
    	// object format with objectformatIdentifier, and won't need exception handling
    	// (unless checking for registered format?)
    	try {
    		objectFormat = ObjectFormatServiceImpl.getInstance().getFormat(ofid);
    	} catch (InvalidRequest e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	} catch (ServiceFailure e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	} catch (NotFound e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	} catch (InsufficientResources e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	} catch (NotImplemented e1) {
    		// TODO Auto-generated catch block
    		e1.printStackTrace();
    	}

        SystemMetadata sysmeta = new SystemMetadata();
        sysmeta.setIdentifier(guid);
        sysmeta.setObjectFormat(objectFormat);
        sysmeta.setSize(12);
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
        Checksum checksum = null;
        try
        {
            checksum = ChecksumUtil.checksum(source, ChecksumAlgorithm.MD5);
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
 

    /**
     * produce a checksum for item using the given algorithm
     */
    protected static String checksum(byte[] object, String algorithm) throws NoSuchAlgorithmException 
    {
    	MessageDigest complete = MessageDigest.getInstance(algorithm);
    	complete.update(object);
    	return getHex(complete.digest());
    }
    
    
    /**
     * produce a checksum for item using the given algorithm
     */
    protected static String checksum(byte[][] baa, String algorithm) throws NoSuchAlgorithmException 
    {
    	MessageDigest complete = MessageDigest.getInstance(algorithm);
    	for(int i=0; i<baa.length;i++) {
    		complete.update(baa[i]);
    	}
    	return getHex(complete.digest());
    }
    
    
    /**
     * produce a checksum for item using the given algorithm
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    protected static String checksum(InputStream is, String algorithm) throws IOException, NoSuchAlgorithmException
    {        
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance(algorithm);
        int numRead;
        
        do 
        {
          numRead = is.read(buffer);
          if (numRead > 0) 
          {
            complete.update(buffer, 0, numRead);
          }
        } while (numRead != -1);
        
        
        return getHex(complete.digest());
    }
    
    /**
     * convert a byte array to a hex string
     */
    private static String getHex( byte [] raw ) 
    {
        final String HEXES = "0123456789ABCDEF";
        if ( raw == null ) {
          return null;
        }
        final StringBuilder hex = new StringBuilder( 2 * raw.length );
        for ( final byte b : raw ) {
          hex.append(HEXES.charAt((b & 0xF0) >> 4))
             .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
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
    		
    		sl.addPerson(p);
    	}
    	return sl;
    }
    
    protected static Subject buildSubject(String subjectValue) {
    	Subject s = new Subject();
    	s.setValue(subjectValue);
    	return s;
    }
    
	public static Identifier doCreateNewObject(MNode mn, String idPrefix) throws ServiceFailure,
	NotImplemented, InvalidToken, NotAuthorized, IdentifierNotUnique, UnsupportedType,
	InsufficientResources, InvalidSystemMetadata, NotFound, InvalidRequest
	{
//		String principal = "uid%3Dkepler,o%3Dunaffiliated,dc%3Decoinformatics,dc%3Dorg";
		Session token = null; // mn.login(principal, "kepler");
		String idString = idPrefix + ExampleUtilities.generateIdentifier();
		Identifier guid = new Identifier();
		guid.setValue(idString);
		InputStream objectStream = 
			new Throwable().getStackTrace()[2].getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");
//		InputStream objectStream = Caller.getClass().getResourceAsStream(
//				"/d1_testdocs/knb-lter-cdr.329066.1.data");
		SystemMetadata sysmeta = ExampleUtilities.generateSystemMetadata(guid, "text/csv", objectStream);
		Identifier rGuid = null;
		objectStream = 
            new Throwable().getStackTrace()[2].getClass().getResourceAsStream("/d1_testdocs/knb-lter-cdr.329066.1.data");

		rGuid = mn.create(token, guid, objectStream, sysmeta);
		assertThat("checking that returned guid matches given ", guid.getValue(), is(rGuid.getValue()));
		mn.setAccessPolicy(token, rGuid, ContextAwareTestCaseDataone.buildPublicReadAccessPolicy());
		System.out.println("new document created on " + mn.getNodeBaseServiceUrl() + 
		        " with guid " + rGuid.getValue());
		InputStream is = mn.get(null,guid);
		return rGuid;
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
  	SubjectList subjectList = new SubjectList();
  	subjectList.setGroupList(groupList);
  	
  	session.setSubject(subject);
  	session.setSubjectList(subjectList);
  	
  	return session;
  	
  }
	
	protected static int countLocationsWithResolve(CNode cn, Identifier pid) throws InvalidToken, ServiceFailure,
	NotAuthorized, NotFound, InvalidRequest, NotImplemented {

		ObjectLocationList oll = cn.resolve(null, pid);
		List<ObjectLocation> locs = oll.getObjectLocationList();
		return locs.toArray().length;
	}
    
}