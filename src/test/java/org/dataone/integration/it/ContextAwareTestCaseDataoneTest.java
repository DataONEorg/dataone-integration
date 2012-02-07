package org.dataone.integration.it;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.dataone.client.auth.CertificateManager;
import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Subject;
import org.dataone.service.types.v1.SubjectInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContextAwareTestCaseDataoneTest { 


	@Before
	public void setUp() throws Exception
	{
		Settings.getResetConfiguration();
		// do nothing except override the setUp method in the base class
		// which is not needed here (cannot be called)
	}
	
	@Test
	public void testTrue() throws Exception
	{
		assertTrue(true);
	}
	

	@Test
	public void testSetupClientSubject_Reader() throws Exception
	{
		runTestSetupClient_Typical("testReader");
	}
	
	@Test
	public void testSetupClientSubject_Writer() throws Exception
	{
		runTestSetupClient_Typical("testWriter");
	}	
	
	@Test
	public void testSetupClientSubject_Owner() throws Exception
	{
		runTestSetupClient_Typical("testOwner");
	}	

	
	@Test
	public void testSetupClientSubject_MappedReader() throws Exception
	{
		runTestSetupClient_Typical("testMappedReader");
	}
	
	@Test
	public void testSetupClientSubject_MappedWriter() throws Exception
	{
		runTestSetupClient_Typical("testMappedWriter");
	}
	
	@Test
	public void testSetupClientSubject_MappedOwner() throws Exception
	{
		runTestSetupClient_Typical("testMappedOwner");
	}
	
	@Test
	public void testSetupClientSubject_NoRights() throws Exception
	{
		Subject s = ContextAwareTestCaseDataone.setupClientSubject("testNoRights");
		System.out.println("subject is: " + s.getValue());
		assertEquals("DC=org,DC=dataone,CN=testNoRights",s.getValue());
		
		java.security.cert.X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println(" Issuer: " + cert.getIssuerX500Principal().getName(X500Principal.RFC2253));
    	Date notBefore = cert.getNotBefore(); 
    	DateFormat fmt = SimpleDateFormat.getDateTimeInstance();
    	System.out.println("   From: " + fmt.format(notBefore));
    	Date notAfter = cert.getNotAfter();
    	System.out.println("     To: " + fmt.format(notAfter));
    	
    	Date now = new Date();
    	assertTrue("certificate should not be expired", !now.after(notAfter));    	
	}

	
	@Test
	public void testSetupClientSubject_ReaderExpired() throws Exception
	{
		Subject s = ContextAwareTestCaseDataone.setupClientSubject("testReader_Expired");
		System.out.println("subject is: " + s.getValue());
		assertEquals("DC=org,DC=dataone,CN=testReader",s.getValue());
		
		java.security.cert.X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println(" Issuer: " + cert.getIssuerX500Principal().getName(X500Principal.RFC2253));
    	Date notBefore = cert.getNotBefore(); 
    	DateFormat fmt = SimpleDateFormat.getDateTimeInstance();
    	System.out.println("   From: " + fmt.format(notBefore));
    	Date notAfter = cert.getNotAfter();
    	System.out.println("     To: " + fmt.format(notAfter));
    	
    	Date now = new Date();
    	assertTrue("certificate SHOULD be expired", now.after(notAfter));
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
		Subject s = ContextAwareTestCaseDataone.setupClientSubject(testSubject);
		System.out.println("subject is: " + s.getValue());
		assertEquals("DC=org,DC=dataone,CN=" + testSubject, s.getValue());

		java.security.cert.X509Certificate cert = CertificateManager.getInstance().loadCertificate();
		System.out.println(" Issuer: " + cert.getIssuerX500Principal().getName(X500Principal.RFC2253));

		Date notBefore = cert.getNotBefore(); 
		DateFormat fmt = SimpleDateFormat.getDateTimeInstance();
		System.out.println("   From: " + fmt.format(notBefore));
		Date notAfter = cert.getNotAfter();
		System.out.println("     To: " + fmt.format(notAfter));

		Date now = new Date();
		assertTrue("certificate should not be expired", !now.after(notAfter));

		CertificateManager cm = CertificateManager.getInstance();
		SubjectInfo si = cm.getSubjectInfo(cm.loadCertificate());
		assertNotNull("subjectInfo should not be null",si);
	}
}
