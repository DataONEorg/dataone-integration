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
 * 
 * $Id$
 */

package org.dataone.integration.it;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.icu.util.Calendar;

public class ClientCertificateInstallationIT { 


	@Before
	public void setUp() throws Exception
	{
		Settings.getResetConfiguration();
		// do nothing except override the setUp method in the base class
		// which is not needed here (cannot be called)
	}

	
	@Test
	public void testSetupClientSubject_Person() throws Exception
	{
		runTestSetupClient_Typical("testPerson");
	}
	
	@Test
	public void testSetupClientSubject_EQPerson1() throws Exception
	{
		runTestSetupClient_Typical("testEQPerson1");
	}
	
	@Test
	public void testSetupClientSubject_EQPerson3() throws Exception
	{
		runTestSetupClient_Typical("testEQPerson3");
	}
	
	@Test
	public void testSetupClientSubject_RightsHolder() throws Exception
	{
		runTestSetupClient_Typical("testRightsHolder");
	}	
	
	@Test
	public void testSetupClientSubject_Submitter() throws Exception
	{
		runTestSetupClient_Typical("testSubmitter");
	}
	
	@Test
	public void testSetupClientSubject_Groupie() throws Exception
	{
		runTestSetupClient_Typical("testGroupie");
	}

	@Test
	public void testSetupClientSubject_Person_SelfSigned() throws Exception
	{
		runTestSetupClient_Typical("testPerson_SelfSigned");
	}


	@Test
	public void testSetupClientSubject_Person_NoSubjectInfo() throws Exception
	{
		runTestSetupClient_Typical("testPerson_NoSubjectInfo", false);
	}
	
	@Test
	public void testSetupClientSubject_CN_sandbox() throws Exception
	{
		runTestSetupClient_Typical("urn:node:cnSandboxUNM1", false);
	}
	
	@Test
	public void testSetupClientSubject_CN_stage() throws Exception
	{
		runTestSetupClient_Typical("urn:node:cnStageUNM1", false);
	}
	
	@Test
	public void testSetupClientSubject_CN_dev() throws Exception
	{
		runTestSetupClient_Typical("urn:node:cnDevUNM1", false);
	}

	

	@Ignore
	@Test
	public void testSetupClientSubject_Person_InvalidVsSchema() throws Exception
	{
		try {
			runTestSetupClient_Typical("testPerson_InvalidVsSchema");
			fail("Expected an invalid schema subjectInfo to throw JiBXException");
		} catch (Exception e) {
			if (!(e instanceof JiBXException)) {
				fail("Expected an invalid schema subjectInfo to throw JiBXException");
		}
		}
	}
	

	
	@Ignore
	@Test
	public void testSetupClientSubject_Person_MissingMappedID() throws Exception
	{
		runTestSetupClient_Typical("testPerson_MissingMappedID");
	}
	
	@Ignore
	@Test
	public void testSetupClientSubject_Person_MissingSelf() throws Exception
	{
		runTestSetupClient_Typical("testPerson_MissingSelf");
	}
	
	
	@Test
	public void testSetupClientSubject_Person_Expired() throws Exception
	{
		String testSubject = "testPerson_Expired";
		
		
		System.out.println("certificate label is: " + testSubject);
		Subject s = ContextAwareTestCaseDataone.setupClientSubject(testSubject);
		String modifiedTestSubjectValue = testSubject.replaceAll("_\\w+", "");  // removes the _Expired or _SelfSigned
		System.out.println("subject is: " + s.getValue());
		assertEquals(s.getValue(), "CN=" + modifiedTestSubjectValue + ",DC=dataone,DC=org");

		java.security.cert.X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println(" Issuer: " + cert.getIssuerX500Principal().getName(X500Principal.RFC2253));

		Date notBefore = cert.getNotBefore(); 
		DateFormat fmt = SimpleDateFormat.getDateTimeInstance();
		System.out.println("   From: " + fmt.format(notBefore));
		Date notAfter = cert.getNotAfter();
		System.out.println("     To: " + fmt.format(notAfter));

		Calendar expirationHorizon = Calendar.getInstance();
		expirationHorizon.add(Calendar.MONTH,1);
		assertTrue("this certificate SHOULD be expired", expirationHorizon.after(notAfter));

		CertificateManager cm = CertificateManager.getInstance();
		SubjectInfo si = cm.getSubjectInfo(cm.loadCertificate());
		assertNotNull("subjectInfo should not be null",si);
		
	}
	
	
	@Test
	public void testSetupClientSubject_NoCert() throws Exception
	{
		ContextAwareTestCaseDataone.setupClientSubject_NoCert();
		X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println("subjectDN is: " + cert);
		assertNull(cert);
	}


	private void runTestSetupClient_Typical(String testSubject) throws Exception
	{
		 runTestSetupClient_Typical( testSubject, true );
	}
	
	
	
	private void runTestSetupClient_Typical(String testSubject, boolean expectSubjectInfo) throws Exception
	{
		System.out.println("certificate label is: " + testSubject);
		Subject s = ContextAwareTestCaseDataone.setupClientSubject(testSubject);
		String modifiedTestSubjectValue = testSubject.replaceAll("_\\w+", "");  // removes the _Expired or _SelfSigned
		System.out.println("subject is: " + s.getValue());
		assertEquals(s.getValue(), "CN=" + modifiedTestSubjectValue + ",DC=dataone,DC=org");

		java.security.cert.X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println(" Issuer: " + cert.getIssuerX500Principal().getName(X500Principal.RFC2253));

		// check the exiration dates
		Date notBefore = cert.getNotBefore(); 
		DateFormat fmt = SimpleDateFormat.getDateTimeInstance();
		System.out.println("   From: " + fmt.format(notBefore));
		Date notAfter = cert.getNotAfter();
		System.out.println("     To: " + fmt.format(notAfter));

		// the expiration date should be well out into the future.  
		Calendar expirationHorizon = Calendar.getInstance();
		expirationHorizon.add(Calendar.MONTH,1);
		assertTrue("certificate should not be expired", !expirationHorizon.after(notAfter));

		/////////////////////////
		// check the subjectInfo
		////////////////////////
		
		CertificateManager cm = CertificateManager.getInstance();
		SubjectInfo si = cm.getSubjectInfo(cm.loadCertificate());
		System.out.println("subjectInfo for: " + testSubject);
		
		if (si != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TypeMarshaller.marshalTypeToOutputStream(si, baos);
			System.out.println(baos.toString() + "\n");
		}
		
		if (expectSubjectInfo) {
			assertNotNull("subjectInfo should not be null",si);

			
			
			boolean foundSelf = false;
			for (Person p: si.getPersonList()) {
				System.out.println("si person: " + p.getSubject().getValue());
				if (p.getSubject().equals(s)) {
					foundSelf = true;
				}
			}
			assertTrue("certificate's subject info contains person matching self", foundSelf);
			
		} else {
			
			assertNull("subjectInfo should be null",si);
		}
	}
}
