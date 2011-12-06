package org.dataone.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.Replica;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ClientArchitectureConformityIT {

	/* configuration information for the echo service */
	protected static final String TEST_SERVICE_BASE = "http://dev-testing.dataone.org/testsvc";
	protected static final String ECHO_MMP = "/echomm";
	protected static final String EXCEPTION_SERVICE = "/exception";
	protected static String pathInfoBase;
	
	protected static Log log = LogFactory.getLog(ClientArchitectureConformityIT.class);

	private static String methodMatchPattern = System.getenv("test.method.match");
	
	private static HashMap<String,HashMap<String,List<String>>> methodMap;
	
	/* lists of methods to hold the client interface methods */
	private static HashMap<String,Method> clientMethodMapMN;
	private static HashMap<String,Method> clientMethodMapCN;
	
	private String currentMethodKey;
	private NodeType nodeType;
	
	
	public ClientArchitectureConformityIT(String methodKey) {
		
		if (methodKey.startsWith("CN.")) {
			this.nodeType = NodeType.CN;
		} else if (methodKey.startsWith("MN.")) {
			this.nodeType = NodeType.MN;
		} else {
			this.nodeType = null;
		}
		this.currentMethodKey = methodKey;
	}
	
	
	/**
	 * Tests should use the error collector to handle JUnit assertions
	 * and keep going.  The check methods in this class use this errorCollector
	 * the check methods 
	 */
	@Rule 
    public ErrorCollector errorCollector = new ErrorCollector();
	
	
	@Parameters
	public static Collection<Object[]> setUpTestParameters() throws IOException
	{
		methodMap = ArchitectureUtils.setUpMethodDocumentationMap();
		
		// creating a superset of methodKeys - if it exists in documentation or
		// implementation we're gonna test it
		
		TreeSet<String> methodKeys = new TreeSet<String>(getCNInterfaceMethods().keySet());
		methodKeys.addAll(new TreeSet<String>(getMNInterfaceMethods().keySet()));
		methodKeys.addAll(new TreeSet<String>(methodMap.keySet()));
	
		// create the return data structure
		ArrayList<Object[]> paramList = new ArrayList<Object[]>();
		

		if (methodMatchPattern != null) {
			for (String key : methodKeys) {
				if (key.matches(methodMatchPattern)) {
					paramList.add(new Object[] {key});
				}
			}
		} else {
			for (String key : methodKeys) {
				paramList.add(new Object[] {key});
			}
		}
		return paramList;
	}
	

	//	@Test
	public void testHarness() {
		assertTrue("test", true);	
	}
	
	/**
	 * want to test a particularly confusing bit of logic for the path matching test
	 * Note, (String).matches() method will only return true if the pattern includes
	 * the entire String under test. 
	 */
//	@Test
	public void testEscapedPattern() {
		
		String x = "/werth/rtyudfgh/xcvbg/{pid}";
		String pathPattern = x.replaceAll("\\{\\w+\\}", "\\\\w+\\$");
		
		String y = "/werth/rtyudfgh/xcvbg/testIdentifier";
		assertTrue("backslash pattern in replace should find target", y.matches(pathPattern));
	}


	/**
	 * method to generate unique meaningful key for each method (includes entire signature)
	 * in order to weed out non-interface methods from list of implemented methods
	 */
	private static String buildMethodListKey(Method method) {
		String methodString = method.toString();
		// remove the class and package from beginning
		String qualifiedName = methodString.substring(methodString.indexOf(method.getName()+"("));
		// remove the "throws" clause
		return qualifiedName.replaceAll("\\sthrows\\s.*", "");
	}
	
	private static HashMap<String,Method> getCNInterfaceMethods() {

		if (clientMethodMapCN == null) {
			// build a list of interface methods for the class under test
			ArrayList<String> interfaceMethodList = new ArrayList<String>();
			Class<?>[] interfaces = CNode.class.getInterfaces();
			for (Class<?> c : interfaces) {
				log.info("Interface under test: " + c.getName());
				Method[] methods = c.getMethods();
				for (Method m : methods) {
					log.debug("    Method from interface: " + m.getName());
					interfaceMethodList.add(buildMethodListKey(m));
				}
			}
			Method[] methods = CNode.class.getMethods();  // gets only public methods
			HashMap<String,Method> clientInterfaceMethods = new HashMap<String,Method>();
			for (Method method : methods) {
				String methodKey = buildMethodListKey(method);
				if (interfaceMethodList.contains(methodKey)) {
					String methodMapKey = "CN." + method.getName();
					clientInterfaceMethods.put(methodMapKey,method);
				}
			}
			clientMethodMapCN = clientInterfaceMethods;
		}
		return clientMethodMapCN;
	}
	
	
	private static HashMap<String,Method> getMNInterfaceMethods() {

		if (clientMethodMapMN == null) {
			// build a list of interface methods for the class under test
			ArrayList<String> interfaceMethodList = new ArrayList<String>();
			Class<?>[] interfaces = MNode.class.getInterfaces();
			for (Class<?> c : interfaces) {
				log.info("Interface under test: " + c.getName());
				Method[] methods = c.getMethods();
				for (Method m : methods) {
					log.debug("    Method from interface: " + m.getName());
					interfaceMethodList.add(buildMethodListKey(m));
				}
			}
			
			Method[] methods = MNode.class.getMethods();  // gets only public methods
			HashMap<String,Method> clientInterfaceMethods = new HashMap<String,Method>();
			for (Method method : methods) {
				String methodKey = buildMethodListKey(method);
				if (interfaceMethodList.contains(methodKey)) {
					String methodMapKey = "MN." + method.getName();
					clientInterfaceMethods.put(methodMapKey,method);
				}
			}
			clientMethodMapMN = clientInterfaceMethods;
		}
		return clientMethodMapMN;
	}
	

	
	@Test
	public void testMethodIsDocumented()
	{
		checkTrue(currentMethodKey,"method should be documented.",
				methodMap.containsKey(currentMethodKey));
	}
	
	@Test
	public void testMethodIsImplemented()
	{
		checkTrue(currentMethodKey,"method should be implemented.",
				getCNInterfaceMethods().containsKey(currentMethodKey) ||
				getMNInterfaceMethods().containsKey(currentMethodKey)
				);
	}
	
	
	@Test
	public void testMethodParameterAgreement()
	{
		List<String> docParamTypes = methodMap.get(currentMethodKey).get("paramTypes");
		Class<?>[] implParamTypes = null;
		if (nodeType == NodeType.CN) {
			implParamTypes = getCNInterfaceMethods().get(currentMethodKey).getParameterTypes();
		} else  if (nodeType == NodeType.MN) {
			implParamTypes = getMNInterfaceMethods().get(currentMethodKey).getParameterTypes();
		} else {
			handleFail(currentMethodKey,"bad nodeType value: " + nodeType.xmlValue());
		}
		
		// make null equal to zero size list, to avoid null pointer exceptions
		if (implParamTypes == null) {
			implParamTypes = new Class<?>[]{};
		}
		if (docParamTypes == null) {
			docParamTypes = new ArrayList<String>();
		}
		
		// check that there are equal number of parameters
		checkEquals(currentMethodKey,"number of parameters in implemented method should match " +
				"number of parameters in documentation", implParamTypes.length, docParamTypes.size());
		
		// check that types agree
		for (int i=0; i<docParamTypes.size(); i++) {
			if (i < implParamTypes.length) {
				String paramTypeSimpleName = implParamTypes[i].getSimpleName(); // don't have to worry about arrays
				checkTrue(currentMethodKey,"Implemented parameter type ("+ paramTypeSimpleName +
						") at position " + i + " should match documented type ("+ docParamTypes.get(i) +")", 
					ArchitectureUtils.checkDocTypeEqualsJavaType(docParamTypes.get(i), 
							paramTypeSimpleName));
			} else {
				handleFail(currentMethodKey,"Implementation does not have parameter at position " + i);
			}
		}
	}
	
	@Test
	public void testHttpVerb()
	{
		String exceptionLocation = null;
		try {	
			exceptionLocation = "getEchoResponse";
			String echoResponse = getEchoResponse(ECHO_MMP);
			exceptionLocation = "get verb from documentMap";
			String docVerb = methodMap.get(currentMethodKey).get("verb").get(0);
			checkEquals(currentMethodKey, "method verb should agree",
					docVerb,
					getVerb(echoResponse));
		} catch (Exception e) {
			e.printStackTrace();
			handleFail(currentMethodKey,"unexpected error at '" + exceptionLocation +
					"' " + e.getClass().getName() + ": " + e.getMessage());
		}
	}
	
	
	@Test
	public void testPath()
	{
		String exceptionLocation = null;
		
		try {
			// create pattern from docMap path entry to match against echo response
			exceptionLocation = "creating pathMatch";
			String pathMatch = pathInfoBase + 
			methodMap.get(currentMethodKey).get("path").get(0).replaceAll("\\{\\w+\\}", "\\\\w+\\$");
			log.debug("testPath() pathMatch: " + pathMatch);

			exceptionLocation = "getEchoResponse";
			String echoResponse = getEchoResponse(ECHO_MMP);

			checkTrue(currentMethodKey, "path_info should be matched by " + pathMatch 
					+ ". got: " + getUrlPath(echoResponse),
					verifyUrlPath(pathMatch, echoResponse));

		} catch (Exception e) {
			e.printStackTrace();
			handleFail(currentMethodKey,"unexpected error at " + exceptionLocation +
					" " + e.getClass().getName() + ": " + e.getMessage());
		}
	}

	
	@Test
	public void checkMethodParameters()
	{
		String exceptionLocation = null;
		// catch all exceptions into errorHandler
		try {	
			exceptionLocation = "getEchoResponse";
			String echoResponse = getEchoResponse(ECHO_MMP);
			if (methodMap.containsKey(currentMethodKey)) {
				ArrayList<String> parameters = (ArrayList<String>) methodMap.get(currentMethodKey).get("params");
				ArrayList<String> paramTypes = (ArrayList<String>) methodMap.get(currentMethodKey).get("paramTypes");
			
				
				if (parameters != null && paramTypes != null) {
					
					Pattern pat = Pattern.compile("(\\w+).*");
					if (parameters.size() != paramTypes.size()) {
						handleFail(currentMethodKey,"documentation error: the number of params" +
								" and paramTypes in the documentation are not equal.");
					} else {
						int i = 0;			
						for (String param : parameters) {
							if (param == null || param.trim().isEmpty())
								continue;
							
							// get the corresponding type from paramType
							// pick out the param name from the text
							String paramType = null;
							if (paramTypes != null) {
								paramType = paramTypes.get(i++);
							}
							Matcher matcher = pat.matcher(param);
							String paramKey = null;
							if (matcher.find()) {
								paramKey = matcher.group(1);
							}
	
							String actualLocation = findParameterLocation(echoResponse,paramKey,"test" + paramType);
							
							String expectedLocation = calcExpectedParamLocation(paramKey,paramType,currentMethodKey);
							if (expectedLocation != null && 
								(expectedLocation.equals("filePart")  || 
									 expectedLocation.equals("paramPart")) && 
								getVerb(echoResponse).equals("PUT") && 
								actualLocation == null) {
								   handleFail(currentMethodKey, "PUT operation: parameter '" + paramKey + "' " +
								   		"is expected to be in a file or param part, but cannot be properly tested " +
								   		"in a PUT operation against the echo service. (actual location WAS null)");
							} else {
								checkEquals(currentMethodKey, "parameter '" + paramKey + "' is not in expected location",
									actualLocation, expectedLocation);				
							}
						}
					}
				}
			} else {
				handleFail(currentMethodKey, "did not find method in documentation map");
			}
		} catch (Exception e) {
			e.printStackTrace();
			handleFail(currentMethodKey,"unexpected error at " + exceptionLocation +
					" " + e.getClass().getName() + ": " + e.getMessage());
		}
			
	}
	
	
	@Test
	public void checkExceptionHandling()
	{
		log.info("::::::: checkExceptionHandling vs. " + currentMethodKey);
		String exceptionName = null;
		try {	
			ArrayList<String> exceptions = (ArrayList<String>) methodMap.get(currentMethodKey).get("exceptions");

			for (int i=0; i< exceptions.size(); i++) {
				exceptionName = exceptions.get(i);
				log.info(" : : : : : checking: " + exceptionName);
				String actualException =  getExceptionResponse(exceptionName);
			
				checkEquals(currentMethodKey, "exception '" + exceptionName + "' is not returned by the method",
						actualException, exceptionName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			handleFail(currentMethodKey,"unexpected error at " + exceptionName +
					" " + e.getClass().getName() + ": " + e.getMessage());
		}
	}



	private String getEchoResponse(String testResource) {
		String echoResponse = null;
		D1Node d1node = null;
		if (nodeType.equals(NodeType.CN)) {
			d1node = new CNode(TEST_SERVICE_BASE + testResource);
			echoResponse = getEchoResponse(d1node,getCNInterfaceMethods().get(currentMethodKey));
		} else {
			d1node = new MNode(TEST_SERVICE_BASE + testResource);
			echoResponse = getEchoResponse(d1node,getMNInterfaceMethods().get(currentMethodKey));
		}
		
		pathInfoBase = d1node.getNodeBaseServiceUrl().replaceAll(TEST_SERVICE_BASE, "");
		log.info("set PATH_INFO base (prefix): " + pathInfoBase);

		return echoResponse;
	}
	
	
	/**
	 * expect the echo service to return an http status in the 400s
	 * whereupon the client exception handler will not be able to 
	 * parse the response into a d1 exception, and will instead wrap
	 * the response text into a ServiceFailure.  
	 * 
	 * @param d1node - either a CN or MN instance intended to be called by
	 *                 the passed in method.
	 * @param method - the method object that will be invoked
	 * @return - the response from the echo service
	 */
	private String getEchoResponse(D1Node d1node, Method method) {
		String echoResponse = null;
		try {
			Object[] paramObjects = buildParameterObjectInstances(method);
			method.invoke(d1node,paramObjects);
		} catch (InvocationTargetException e) {	
			if (!(e.getCause() instanceof ServiceFailure)) {
				e.printStackTrace();
				handleFail("","Error invoking method " + method.getName());
			} else {
				ServiceFailure sf = (ServiceFailure) e.getCause();
				echoResponse = "\ndetail code = " + sf.getDetail_code() + "\n" +
				sf.getDescription();
			}
		} catch (Exception e) {
			log.debug("invocation class: " + d1node.getClass());
			log.debug("class of method being invoked: " + method.getDeclaringClass());
			e.printStackTrace();
			handleFail(buildMethodListKey(method),"Error building parameters for or invoking method " + method.getName());
		}
		
		log.debug("echo response: " + echoResponse);	
		return echoResponse;
	}
	
	
	private String getExceptionResponse(String exception) throws Exception
	{
		String exceptionReturned = null;
		D1Node d1node = null;
		Method method = null;
		log.debug("  - - - -  calling " + TEST_SERVICE_BASE + EXCEPTION_SERVICE +
				"/" + exception);
		if (nodeType.equals(NodeType.CN)) {
			d1node = new CNode(TEST_SERVICE_BASE + EXCEPTION_SERVICE +
					"/" + exception); // + "/v1/object");
			method = getCNInterfaceMethods().get(currentMethodKey);
		} else {
			d1node = new MNode(TEST_SERVICE_BASE + EXCEPTION_SERVICE +
					"/" + exception); // + "/v1/object");
			method = getMNInterfaceMethods().get(currentMethodKey);
		}
		try {
			Object[] paramObjects = buildParameterObjectInstances(method);
			method.invoke(d1node,paramObjects);
		} catch (InvocationTargetException e) {	
			log.debug("exceptionReturned: " + ((BaseException)e.getCause()).getDescription());
			exceptionReturned = e.getCause().getClass().getSimpleName();
		}
		return exceptionReturned;
	}

	
	private Object[] buildParameterObjectInstances(Method method) 
	throws Exception {
		Class<?>[] params = method.getParameterTypes();
		Object[] paramObjects = new Object[params.length];
		for (int i = 0; i<params.length; i++) {
			log.debug("building instance for: " + params[i].getCanonicalName());					
			paramObjects[i] = buildParamObject(params[i]);
			if (paramObjects[i] instanceof Date) {
				Thread.sleep(1000); // to ensure different times for dates
			}
		}
		return paramObjects;
	}

	
	private Object buildParamObject(Class<?> c) 
	throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, 
	InstantiationException, SecurityException, NoSuchMethodException, UnsupportedEncodingException 
	{
		Object o = null;
		if (c.getCanonicalName().equals("java.io.InputStream")) {
			byte[] contentBytes = "some input for the stream".getBytes("UTF-8");
			o = new ByteArrayInputStream(contentBytes);
		} else if (c.getCanonicalName().equals("long")) {
			o = 0;
		} else if (c.getCanonicalName().equals("java.lang.Integer")) {
			o = new Integer(1);
		} else if (c.getCanonicalName().equals("java.lang.Boolean")) {
			o = new Boolean(true);
		} else if (c.getCanonicalName().equals("java.lang.String")) {
			o = new String("testString");
			
		// dataone types needing special instruction (required fields in xsd)	
		} else if (c.getCanonicalName().endsWith("Event")) {
			o = org.dataone.service.types.v1.Event.READ;
		} else if (c.getCanonicalName().endsWith("Permission")) {
			o = Permission.READ;
		} else if (c.getCanonicalName().endsWith("ReplicationStatus")) {
			o = ReplicationStatus.COMPLETED;
		} else if (c.getCanonicalName().endsWith("SystemMetadata")) {
			Identifier id = new Identifier();
			id.setValue("testIdentifier");
			D1Object d1o = null;
			try {
				d1o = new 	D1Object(id, "testData".getBytes(), "text/plain", 
						"submitterString", "nodeIdString");
			} catch (Exception e) {
				e.printStackTrace();
			}
			o = d1o.getSystemMetadata();
			
		} else if (c.getCanonicalName().endsWith("SynchronizationFailed")) {
			o = new SynchronizationFailed("detailCodeString","descriptionString");

		// 
		} else {
			Constructor co = c.getConstructor(new Class[]{});		
			o = co.newInstance((Object[]) null);
			log.debug("created :" + o.getClass());
			try {
				Method mm = c.getMethod("setValue", new Class[]{String.class});
				mm.invoke(o, "test" + c.getSimpleName());
				log.debug("    setValue : test" + c.getSimpleName());
			} catch (NoSuchMethodException e) {
				// ok, object doesn't have that method...
			}
		}
		
		// post-instantiation modifications
		if (c.getCanonicalName().endsWith("Node")) {
			NodeReference nr = new NodeReference();
			nr.setValue("testNodeReference");
			((Node)o).setIdentifier(nr);
			((Node)o).setName("fako");
			((Node)o).setDescription("blah blah blah");
			((Node)o).setBaseURL("somewhere over the rainbow");
			((Node)o).setType(NodeType.MN);
			((Node)o).setState(NodeState.UP);
		} else if (c.getCanonicalName().endsWith("Person")) {
			Subject s = new Subject();
			s.setValue("testSubject");
			((Person)o).setSubject(s);
			((Person)o).setGivenNameList(Arrays.asList(new String[]{"Sleepy"}));
			((Person)o).setFamilyName("Dwarf");
		} else if (c.getCanonicalName().endsWith("Replica")) {
			NodeReference nf = new NodeReference();
			nf.setValue("testNodeReference");
			((Replica)o).setReplicaMemberNode(nf);
			((Replica)o).setReplicationStatus(ReplicationStatus.REQUESTED);
			((Replica)o).setReplicaVerified(new Date());
		}
		return o;
	}

	
	private String findParameterLocation(String echoText,String parameterName, String paramTestObject)
	{
		String[] textLines = echoText.split("\n");
		String location = null;
		for (String line : textLines) {
			if (line.startsWith("request.META[ PATH_INFO ]")) {
				if (line.endsWith(paramTestObject)) {
					location = "path";
				}
			} else if (line.contains(parameterName)) {
				if (line.startsWith("request.META[ QUERY_STRING ]")) {
					location = "queryString";
				} else if (line.startsWith("request.POST=<QueryDict:")) {
					location = "paramPart";
				} else if (line.startsWith("request.FILES=<MultiValueDict:")) {
					location = "filePart";
				} else if (line.startsWith("request.PUT=<QueryDict:")) {
					location = "paramPart";
				} else if (line.startsWith("request.DELETE=<QueryDict:")) {
					location = "paramPart";
				}
				if (location != null)
					break;
			} 
		}
		return location;
	}
	

	private boolean verifyUrlPath(String key, String echoText)
	{
		String pathInfoLine = getUrlPath(echoText);
		if ( pathInfoLine.matches(key) ) {
			return true;
		}
		return false;
	}
	
	
	private String getUrlPath(String echoText)
	{
		String[] textLines = echoText.split("\n");
		String pathInfoLine = null;
		for (String s : textLines) {
			if (s.startsWith("request.META[ PATH_INFO ]")) {
				pathInfoLine = s.replace("request.META[ PATH_INFO ] = ", "");
				break;
			}
		}
		return pathInfoLine;
	}

	
	
	private String getVerb(String echoText) 
	{
		String[] textLines = echoText.split("\n");
		String lineOfInterest = null;
		for (String s : textLines) {
			if (s.startsWith("request.META[ REQUEST_METHOD ]")) {
				lineOfInterest = s;
				break;
			}
		}
		if (lineOfInterest == null)
			return null;
		
		String[] words = lineOfInterest.trim().split("\\s+");
		return words[words.length-1];
	}




	
	public String calcExpectedParamLocation(String key, String keyType, String methodMapKey) 
	{
		String location = "bad methodMapKey";
		if (key.toLowerCase().equals("session")) {
			location = null;
		}
		else if (methodMap.containsKey(methodMapKey) ) {
			if (methodMapKey.endsWith("listObjects")) {
				log.debug(methodMapKey);
			}
			if (methodMap.get(methodMapKey).get("path").get(0).contains("{" + key + "}")) {
				location =  "path";
			} else {
				// 
				if (methodMap.get(methodMapKey).get("verb").get(0).equals("GET") ||
					methodMap.get(methodMapKey).get("verb").get(0).equals("HEAD")) 
				{
					// has to be on queryString by default, since these are no-body requests
					location = "queryString";
				} 
				else {
					// will check all remaining options
					// starting with queryString
					if (methodMap.get(methodMapKey).containsKey("query")) {
						String queryString = methodMap.get(methodMapKey).get("query").get(0);
						if (methodMap.get(methodMapKey).get("query").get(0).toLowerCase().contains(key.toLowerCase()+"=")) {
							location = "queryString";
						}
					} else {
						try {
							if (keyType.equals("Subject") || keyType.endsWith("Identifier") ||
								keyType.equals("Permission") || keyType.equals("NodeReference") ||
								keyType.equals("boolean") || keyType.equals("DateTime") ||
								key.equals("scheme") || key.equals("fragment") ||
								keyType.equals("long") || keyType.equals("unsigned long")
								) 
							{
								location = "paramPart";
							} else {
								location = "filePart";
							}
						} catch (NullPointerException npe) {
							location = "null key or keyType";
						}
					}
				}
			}
		}
		return location;
	}
	
	
	public void checkEquals(final String host, final String message, final String s1, final String s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                if (host != null) {
                	assertThat("for method: " + host + ":: " + message, s1, is(s2));
                } else {
                	assertThat(message, s1, is(s2));
                }
                return null;
            }
        });
    }
	
	
	public void checkEquals(final String host, final String message, final int s1, final int s2)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
                if (host != null) {
                	assertThat("for method: " + host + ":: " + message, s1, is(s2));
                } else {
                	assertThat(message, s1, is(s2));
                }
                return null;
            }
        });
    }
	
	public void checkTrue(final String host, final String message, final boolean b)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		assertThat("for method: " + host + ":: " + message, b, is(true));
            	} else {
            		assertThat(message, b, is(true));
            	}
                return null;
            }
        });
    }
	
	public void handleFail(final String host, final String message)
    {
        errorCollector.checkSucceeds(new Callable<Object>() 
        {
            public Object call() throws Exception 
            {
            	if (host != null) {	
            		fail("for method: " + host + ":: " + message);
            	} else {
            		fail(message);
            	}
                return null;
            }
        });
    }
}
