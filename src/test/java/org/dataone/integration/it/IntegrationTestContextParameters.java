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

/**
 * Defines the parameter names used by dataone integration to configure tests 
 * implemented by ContextAwareTestCaseDataone and subclasses.
 * Also used by the TestRunners contained in org.dataone.integration.webTest package
 * 
 * Definitions:
 * TEST_CONTEXT - the context name (LOCAL,DEV,STAGING,PROD) plus any ad-hoc contexts
 * The context name will point to a similarly named settings file to get test properties 
 * 
 * NODELIST_URI  - a URI, (filepath or url) that designates a set of nodes for the given tests
 * 
 * CN_URL - the base url of a coordinating node to be tested. used for testing a single node
 * MN_URL - the base url of a member node to be tested. used for testing a single node.
 * 
 * Pre-registration tests need to refer to a context prior to the node being added
 * to the context's nodelist.  The CN is the Reference environment against which these
 * tests run 
 * REFERENCE_CN_URL - the baseURL of the CN that will determine the reference for the tests
 * REFERENCE_CONTEXT - the context name (LOCAL,DEV,STAGING,PROD) for the reference environment
 */
public interface IntegrationTestContextParameters {
	
	/**
	 * the names of system properties that ContextAwareTestCaseDataone will fetch and take action on
	 * during the test setup phase.
	 * 
	 * Also used by the TestRunners contained in org.dataone.integration.webTest package
	 */
	
	public final static String PARAM_TEST_CONTEXT = "context.label";  // the string that signals the context to run against
	public final static String DEFAULT_CONTEXT = "LOCAL";  // the default value for PARAM_TEST_CONTEXT
	
	public final static String PARAM_CN_URL = "context.cn.baseurl";    // the base url for the cn within the environment 
	public final static String PARAM_MN_URL = "context.mn.baseurl";    // the base url for the MN being tested
	public final static String PARAM_NODELIST_URI = "context.nodelist.uri";  // a uri (file or url) of the nodelist to use to set the environment

	public final static String PARAM_REFERENCE_CONTEXT = "reference.context.label"; 
	public final static String PARAM_REFERENCE_CN_URL = "reference.cn.baseurl"; 
	
	public final static String PARAM_TEST_SETTINGS_URI = "opt.overriding.properties.filename";
	
}
