package org.dataone.integration;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.configuration.Settings;
import org.dataone.service.exceptions.InsufficientResources;
import org.dataone.service.exceptions.InvalidRequest;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.impl.ObjectFormatServiceImpl;
import org.dataone.service.types.AccessPolicy;
import org.dataone.service.types.AccessRule;
import org.dataone.service.types.Node;
import org.dataone.service.types.NodeList;
import org.dataone.service.types.ObjectFormat;
import org.dataone.service.types.ObjectFormatIdentifier;
import org.dataone.service.types.Permission;
import org.dataone.service.types.Subject;
import org.dataone.service.types.util.ServiceTypeUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

/**
 * DO NOT ADD tests to this class directly!  Subclass, subclass, subclass!!! :-) 
 * 
 * This class is intended as a base class that implements the standard IntegrationTestContextParameters
 * 
 * There is a setUp routine that parses configurations into objects useful for running tests in a given
 * context.  It also sets up the logger, and errorCollector logic (see @Rule, and  protected "check" methods)
 * 
 * @author rnahf
 *
 */
public abstract class ContextAwareTestCaseDataone implements IntegrationTestContextParameters {

	protected static Log log = LogFactory.getLog(ContextAwareTestCaseDataone.class);
	
	// variables to the context interface parameters
	protected static String testContext;
	protected static String cnBaseUrl;
	protected static String mnBaseUrl;
	protected static String nodelistUri;

	public List<Node> listOfNodes = null;
	
	protected abstract String getTestDescription();
	
	
	/**
	 * sets static variables based on properties returned from org.dataone.configuration.Settings object
	 * assigns only once (the first test)
	 *  
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		// skip setUp steps if already run
		if (testContext == null) {
			testContext = Settings.getConfiguration().getString(Settings.OPTIONAL_PROPERTY_CONTEXT_LABEL);			
			cnBaseUrl = Settings.getConfiguration().getString(PARAM_CN_URL);
			mnBaseUrl = Settings.getConfiguration().getString(PARAM_MN_URL);
			nodelistUri = Settings.getConfiguration().getString(PARAM_NODELIST_URI);

			log.debug("context: " + testContext);
			log.debug("overrides from: " + Settings.getConfiguration().getString(Settings.OPTIONAL_PROPERTY_PROPERTIES_FILENAME));
		

			if (mnBaseUrl != null) {
				// the context is standalone member node
				Node n = new Node();
				n.setBaseURL(mnBaseUrl);
				listOfNodes = new Vector<Node>();
				listOfNodes.add(n);
			} else {
				// we will be testing multiple member nodes
				if (nodelistUri != null) {
					// the list of member nodes is in this NodeList.xml file
					URL url = new URL(nodelistUri);
					InputStream is = url.openStream();
					NodeList nl = (NodeList) ServiceTypeUtil.deserializeServiceType(NodeList.class, is);
					listOfNodes = nl.getNodeList();
				} else {
					// use the context specified by D1Client
					CNode cn = D1Client.getCN();
					listOfNodes = cn.listNodes().getNodeList();
				} 
			} // nodelist set up
		}  // settings already set up
	}
	
	
   
	
	/**
	 * create an accessPolicy that assigns read permission to public subject.
	 * This is a common test scenario...
	 */
	protected static AccessPolicy buildPublicReadAccessPolicy() {

		AccessRule ar = new AccessRule();
		ar.addPermission(Permission.READ);

		Subject sub = new Subject();
    	sub.setValue("public");
		ar.addSubject(sub);

		AccessPolicy ap = new AccessPolicy();
		ap.addAllow(ar);
    	return ap;
	}

	
	
	/**
	 * creates a header line with the text: "*** running test for <string> ***" 
	 * into the log (INFO level)
	 * @param methodName
	 */
	protected void printTestHeader(String methodName)
    {
        log.info("\n***************** running test for " + methodName + " *****************");
    }

	
	/**
	 * Tests should use the error collector to handle JUnit assertions
	 * and keep going.  The check methods in this class use this errorCollector
	 * the check methods 
	 */
	@Rule 
    public ErrorCollector errorCollector = new ErrorCollector();
	
	/**
	 * performs the junit assertThat method using the errorCollector
	 * to record the error and keep going
	 * 
	 * @param message
	 * @param s1
	 * @param s2
	 */
    protected void checkEquals(final String host, final String message, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                if (host != null) {
                	assertThat("for host: " + host + ":: " + message, s1, is(s2));
                } else {
                	assertThat(message, s1, is(s2));
                }
                return null;
            }
        });
    }
    
    
    /**
	 * performs the equivalent of the junit assertTrue method
	 * using the errorCollector to record the error and keep going
	 * 
	 * @param message
	 * @param s1
	 * @param s2
	 */
    protected void checkTrue(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for host: " + host + ":: " + message, true, is(b));
            	} else {
            		assertThat(message, true, is(b));
            	}
                return null;
            }
        });
    }
	
    /**
	 * performs the equivalent of the junit assertFalse method
	 * using the errorCollector to record the error and keep going
	 * 
	 * @param message
	 * @param s1
	 * @param s2
	 */
    protected void checkFalse(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for host: " + host + ":: " + message, false, is(b));
            	} else {
            		assertThat(message, false, is(b));
            	}
                return null;
            }
        });
    }
	
}
