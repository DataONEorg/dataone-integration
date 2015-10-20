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

package org.dataone.integration;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.dataone.configuration.Settings;
import org.dataone.configuration.TestSettings;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class ContextAwareTestCaseDataoneTest { 


	
	@After
	public void tearDown() throws Exception
	{
		System.clearProperty(TestSettings.REFERENCE_CONTEXT_LABEL);
		System.clearProperty(TestSettings.REFERENCE_CONTEXT_CN_URL);
		System.clearProperty(TestSettings.CONTEXT_MN_URL);
		System.clearProperty(TestSettings.CONTEXT_LABEL);
	}

	@Test
	public void testNodeAbbreviation() {
		
		String demo2 = "https://demo2.test.dataone.org/knb/d1/mn/v1".replaceFirst("https{0,1}://", "").replaceFirst("\\..+", "");
		assertEquals("demo2",demo2);
		String cndev = "https://cn-dev.dataone.org/cn/v1".replaceFirst("https{0,1}://", "").replaceFirst("\\..+", "");
		assertEquals("cn-dev",cndev);
	}

	@Ignore("skipping test because it's really an integration test - dependencies on client-side trust manager and registered MNode certificates")
	@Test
	public void testReferenceContextLabel() throws Exception {
		System.setProperty(TestSettings.REFERENCE_CONTEXT_LABEL, "DEV");
		System.setProperty(TestSettings.CONTEXT_LABEL, "DEV");

		Settings.getResetConfiguration();
		Settings.getConfiguration().setProperty("certificate.truststore.useDefault", false);
		
		ContextAwareTestCaseDataone tc = new ContextAwareTestCaseDataone() {

			@Override
			protected String getTestDescription() {
				// TODO Auto-generated method stub
				return null;
			}			
		};
		
		tc.setUpContext();
		String refCNbaseUrl = tc.getReferenceContextCnUrl();
		System.out.println("reference BaseUrl = " + refCNbaseUrl);
		
		String prodCNurl = Settings.getConfiguration().getString("D1Client.CN_URL");		
		assertEquals("reference CN should equal context CN in this test",prodCNurl,refCNbaseUrl);
		assertFalse("reference and context CNs should not be empty or null", StringUtils.isEmpty(refCNbaseUrl));
	}
	
	
	@Test
	public void testReferenceContextLabel_isolation() throws Exception {
		System.setProperty(TestSettings.REFERENCE_CONTEXT_LABEL, "DEV");
		System.setProperty(TestSettings.CONTEXT_MN_URL, "https://mn-x.dataone.org/mn");
		Settings.getResetConfiguration();
		ContextAwareTestCaseDataone tc = new ContextAwareTestCaseDataone() {

			@Override
			protected String getTestDescription() {
				// TODO Auto-generated method stub
				return null;
			}			
		};
		
		tc.setUpContext();
		String refCNbaseUrl = tc.getReferenceContextCnUrl();
		System.out.println("reference BaseUrl = " + refCNbaseUrl);
		
		String prodCNurl = Settings.getConfiguration().getString("D1Client.CN_URL");	
		System.out.println("context CN url = " + prodCNurl);
		assertTrue("reference CN should not bleed over into context CN", StringUtils.isEmpty(prodCNurl));
	}
	
}
