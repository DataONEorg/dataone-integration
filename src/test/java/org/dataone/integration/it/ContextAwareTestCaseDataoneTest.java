package org.dataone.integration.it;


import static org.junit.Assert.assertEquals;

import org.dataone.configuration.Settings;
import org.dataone.service.types.v1.Subject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContextAwareTestCaseDataoneTest extends ContextAwareTestCaseDataone {


	@Before
	public void setUp() throws Exception
	{
		Settings.getResetConfiguration();
		// do nothing except override the setUp method in the base class
		// which is not needed here (cannot be called)
	}
	
	@Test
	public void testSetupClientSubject_Writer() throws Exception
	{
		Subject s = setupClientSubject_Writer();
		System.out.println("subject is: " + s.getValue());
		assertEquals("CN=testUserWriter,DC=dataone,DC=org",s.getValue());

	}
	
	@Test
	public void testSetupClientSubject_Reader() throws Exception
	{
		Subject s = setupClientSubject_Reader();
		System.out.println("subject is: " + s.getValue());
		assertEquals("CN=testUserReader,DC=dataone,DC=org",s.getValue());

	}
	
	@Test
	public void testSetupClientSubject_NoRights() throws Exception
	{
		Subject s = setupClientSubject_NoRights();
		System.out.println("subject is: " + s.getValue());
		assertEquals("CN=testUserNoRights,DC=dataone,DC=org",s.getValue());

	}

	@Override
	protected String getTestDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
