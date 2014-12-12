package org.dataone.integration.webTest;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLauncher {

	@Test
	public void testGetBooleanProperty() {
		System.setProperty("test.prop", "true");
		assertTrue("should return set value of true", Launcher.getBooleanProperty("test.prop", false));
		System.setProperty("test.prop2", "false");
		assertFalse("should return set value of false", Launcher.getBooleanProperty("test.prop2", true));
		
	}
	
	@Test
	public void testGetBooleanPropertyDefaults() {
		assertFalse("should return defaultValue false", Launcher.getBooleanProperty("fake-o", false));
		assertTrue("should return defaultValue of true", Launcher.getBooleanProperty("fake-o", true));
		
	}


}
