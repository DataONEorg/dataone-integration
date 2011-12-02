package org.dataone.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.D1Object;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.NodeState;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.Permission;
import org.dataone.service.types.v1.Person;
import org.dataone.service.types.v1.ReplicationStatus;
import org.dataone.service.types.v1.Subject;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ClientArchitectureConformityAnalysis {

	/* configuration information for the echo service */
	protected static final String TEST_SERVICE_BASE = "http://dev-testing.dataone.org/testsvc";
	protected static final String ECHO_MMP = "/echomm";
	protected static String pathInfoBase;
	
	protected static Log log = LogFactory.getLog(ClientArchitectureConformityAnalysis.class);

	private static String methodRefDoc = 
			"https://repository.dataone.org/documents/Projects/cicore/" +
			"architecture/api-documentation/MethodCrossReference.xls";
	
	private static HashMap<String,HashMap<String,List<String>>> methodMap;
	
	/* lists of methods to hold the client interface methods */
	private static HashMap<String,Method> clientMethodMapMN;
	private static HashMap<String,Method> clientMethodMapCN;
	
	private String currentMethodKey;
//	private String currentMethodMapKey;
	private NodeType nodeType;
	
	
	public ClientArchitectureConformityAnalysis(String methodKey) {
		
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
		setUpMethodDocumentationMap();
		
		// creating a superset of methodKeys - if it exists in documentation or
		// implementation we're gonna test it
		
		TreeSet<String> methodKeys = new TreeSet<String>(getCNInterfaceMethods().keySet());
		methodKeys.addAll(new TreeSet<String>(getMNInterfaceMethods().keySet()));
		methodKeys.addAll(new TreeSet<String>(methodMap.keySet()));
	
		// create the return data structure
		ArrayList<Object[]> paramList = new ArrayList<Object[]>();
		for (String key : methodKeys) {
			paramList.add(new Object[] {key});
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
		String qualifiedName = methodString.substring(methodString.indexOf(method.getName()+"("));
		return qualifiedName;
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
	


	/**
	 * Is the list of methods in documentation the same as the ones under test?
	 * comparing number and signature
	 */
//	@Test
	public void testMethodListAgreement()
	{
		try {
			Set<String> docMapKeys= new TreeSet(methodMap.keySet());
			Set<String> cnMapKeys = new TreeSet(getCNInterfaceMethods().keySet());
			Set<String> mnMapKeys = new TreeSet(getMNInterfaceMethods().keySet());
			Set<String> bothInterfacesMapKeys = cnMapKeys;
			System.out.println(docMapKeys.getClass());
			System.out.println(cnMapKeys.getClass());
			System.out.println(mnMapKeys.getClass());
				bothInterfacesMapKeys.addAll(mnMapKeys);
				
			if (!docMapKeys.equals(bothInterfacesMapKeys)) {
				Set<String> tmpDoc = new TreeSet<String>(docMapKeys);
				Set<String> tmpImpl = new TreeSet<String>(bothInterfacesMapKeys);
				tmpDoc.removeAll(bothInterfacesMapKeys);
				tmpImpl.removeAll(docMapKeys);
				String onlyDocumented = tmpDoc.toString();
				String onlyImplemented = tmpImpl.toString();
				handleFail("","document map and implementation map should be identical.\n" +
						"onlyDocumented = " + onlyDocumented +"\n" +
								"onlyImplemented = " + onlyImplemented);
			}

		} catch (Exception e) {
			e.printStackTrace();
			handleFail(currentMethodKey,"unexpected error: " + e.getClass().getName() + 
					": " + e.getMessage());
		}
	}
	
	@Test
	public void testIsDocumented()
	{
		checkTrue(currentMethodKey,"method should be documented.",
				methodMap.containsKey(currentMethodKey));
	}
	
	@Test
	public void testIsImplemented()
	{
		checkTrue(currentMethodKey,"method shold be implemented.",
				getCNInterfaceMethods().containsKey(currentMethodKey) ||
				getMNInterfaceMethods().containsKey(currentMethodKey)
				);
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
			handleFail(currentMethodKey,"unexpected error at " + exceptionLocation +
					" " + e.getClass().getName() + ": " + e.getMessage());
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

			checkTrue(currentMethodKey, "path_info should be matched by " + pathMatch,
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
							checkEquals(currentMethodKey, "parameter '" + paramKey + "' is not in expected location",
									actualLocation, expectedLocation);
							
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
	
	
	
////	@Test
//	public void testMNInterface() throws InterruptedException {
//		MNode mn = new MNode(TEST_SERVICE_BASE + ECHO_MMP);
//		runInterfaceTest(mn, "MN");
//	}
//		
////	@Test
//	public void testCNInterface() throws InterruptedException {
//		CNode cn = new CNode(TEST_SERVICE_BASE + ECHO_MMP);		
//		runInterfaceTest(cn, "CN");
//	}

	private String getEchoResponse(String testResource) {
		String echoResponse = null;
		D1Node d1node = null;
		if (nodeType.equals(NodeType.CN)) {
			d1node = new CNode(TEST_SERVICE_BASE + testResource);
		} else {
			d1node = new MNode(TEST_SERVICE_BASE + testResource);
		}
		echoResponse = getEchoResponse(d1node,getCNInterfaceMethods().get(currentMethodKey));
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
	
	
//	private void runInterfaceTest(D1Node d1node, String nodeType) throws InterruptedException {
//		
//
//
//		HashMap<String,Method> clientMethodList = null;
//		if (nodeType.equals("CN")) {
//			clientMethodList = getCNInterfaceMethods();
//		} else if (nodeType.equals("MN")) {
//			clientMethodList = getMNInterfaceMethods();
//		}
//		
////		for (Method method : clientMethodList) {			
////			log.info("**** testing ::::: " + buildMethodListKey(method));
////			String echoResponse = getEchoResponse(d1node,method);
////			if (echoResponse != null) {
////				checkEchoResponseVsArchitecture(echoResponse,nodeType,method);
////			} else {
////				handleFail(nodeType + "." + method.getName(),"no echo response received");
////			}
////		}
//	}
	

	
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
		}
		return o;
	}

	
	
	
	
	private boolean isFilePart(String key, String echoText) 
	{
		String[] textLines = echoText.split("\n");
		String filesLine = null;
		for (String s : textLines) {
			if (s.startsWith("request.FILES=<MultiValueDict:")) {
				filesLine = s;
				break;
			}
		}
		if (filesLine == null)
			return false;
		
		if (filesLine.contains(key)) {
			return true;
		}
		return false;
	}

	private boolean isParamPart(String key, String echoText)
	{
		String[] textLines = echoText.split("\n");
		String paramLine = null;
		for (String s : textLines) {
			if (s.startsWith("request.POST=<QueryDict:")) {
				paramLine = s;
				break;
			}
		}
		if (paramLine == null)
			return false;
		
		if (paramLine.contains(key)) {
			return true;
		}
		return false;
	}
	
	private boolean isUrlQueryString(String key, String echoText)
	{
		String[] textLines = echoText.split("\n");
		String queryLine = null;
		for (String s : textLines) {
			if (s.startsWith("request.META[ QUERY_STRING ]")) {
				queryLine = s;
				break;
			}
		}
		if (queryLine == null)
			return false;
		
		if (queryLine.contains(key+"=")) {
			return true;
		}
		return false;
	}
	
	private boolean isUrlPath(String objectValue, String echoText)
	{
		String[] textLines = echoText.split("\n");
		String lineOfInterest = null;
		for (String s : textLines) {
			if (s.startsWith("request.META[ PATH_INFO ]")) {
				lineOfInterest = s;
				break;
			}
		}
		if (lineOfInterest == null)
			return false;
		
		if (lineOfInterest.endsWith(objectValue)) {
			return true;
		}
		return false;
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
		String[] textLines = echoText.split("\n");
		String pathInfoLine = null;
		for (String s : textLines) {
			if (s.startsWith("request.META[ PATH_INFO ]")) {
				pathInfoLine = s;
				break;
			}
		}
		if (pathInfoLine == null)
			return false;
		pathInfoLine = pathInfoLine.replace("request.META[ PATH_INFO ] = ", "");
		if ( pathInfoLine.matches(key) ) {
			return true;
		}
		return false;
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



	public static void setUpMethodDocumentationMap() throws IOException  {
		// get and parse architecture document
		URL url = new URL(methodRefDoc);
		InputStream is = url.openStream();
		HSSFWorkbook wb = new HSSFWorkbook(is);
//		System.out.println("Data dump:\n");
		
		// map<method,map<aspect,set<stringValue>>>
		// eg: methodMap.get("ping").get("params")
		methodMap = new HashMap<String,HashMap<String,List<String>>>();
		
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			HSSFSheet sheet = wb.getSheetAt(k);
			if (!wb.getSheetName(k).equals("Functions")) {
				continue;
			}
			int rows = sheet.getPhysicalNumberOfRows();
//			System.out.println("Sheet " + k + " \"" + wb.getSheetName(k) + "\" has " 
//					+ rows + " row(s).");
			
			int moduleCol = -1;
			int functionCol = -1;
			int restCol = -1;
			int paramsCol = -1;
			int paramTypeCol = -1;
			int exceptionsCol = -1;
			int returnsCol = -1;
			HSSFRow headerRow = sheet.getRow(0);
			for (int c = 0; c < headerRow.getPhysicalNumberOfCells(); c++) {
				String columnName = headerRow.getCell(c).getStringCellValue();
				if (columnName.equals("Module")) {
					moduleCol = c;
				}
				if (columnName.equals("Function")) {
					functionCol = c;
				}
				if (columnName.equals("REST")) {
					restCol = c;
				}
				if (columnName.equals("Params")) {
					paramsCol = c;
				}
				if (columnName.equals("ParamType")) {
					paramTypeCol = c;
				}
				if (columnName.equals("Return")) {
					returnsCol = c;
				}
				if (columnName.equals("Exceptions")) {
					exceptionsCol = c;
				}
			}
			
			
			// http://poi.apache.org/apidocs/org/apache/poi/hssf/usermodel/HSSFRow.html
			boolean parse = false;

			for (int r = 1; r < rows; r++) {
				HSSFRow row = sheet.getRow(r);
				if (row == null) {
					log.debug("ROW " + r + " is null");
				} else {
					int cells = row.getPhysicalNumberOfCells();
					log.debug("ROW " + r + " has " + cells + " cell(s).");
								
					if (parse == false) {
						if (row.getCell(0).getStringCellValue().equals("START")) {
							parse = true;
						} else {
							continue;
						}
					}

					// http://poi.apache.org/apidocs/org/apache/poi/hssf/usermodel/HSSFCell.html
					String moduleString = getCellValue(row.getCell(moduleCol));
					String functionString = getCellValue(row.getCell(functionCol));
					// TODO: pulling CN/ MN off the beginning of module
					// might make things brittle.  What else to use?
					if (moduleString == null) {
						continue;
					}
					String apiDesignator = moduleString.substring(0,2);
					String methodMapKey = apiDesignator + "." + functionString;				
					
					if (methodMapKey.contains("search")) {
						System.out.println("method map key: " + methodMapKey);
					}
					// get the details map for the appropriate method
					// each line of a record block in the method cross reference
					// contains the functionName and module so we can use this
					// to add information to the proper method.
					if (!methodMap.containsKey(methodMapKey)) {
						methodMap.put(methodMapKey, new HashMap<String,List<String>>());
					}
					HashMap<String,List<String>> methodDetailsMap = methodMap.get(methodMapKey);
					
					String value = getCellValue(row.getCell(returnsCol));
					if (value != null) {
						methodDetailsMap.put("returnValue", Arrays.asList(new String[] {value}));
					}
					
					value = getCellValue(row.getCell(restCol));
					if (value != null) {
						if (value.trim().equals("GET /  and  GET /node")) {
							methodDetailsMap.put("verb", Arrays.asList(new String[] {"GET"}));
							methodDetailsMap.put("path", Arrays.asList(new String[] {"/node"}));
						} else {
							
							String[] verbPath = value.split("\\s+", 2);
							String[] pathQuery = verbPath[1].split("\\?");
							if (pathQuery[0].endsWith("[")) {
								pathQuery[0] = pathQuery[0].substring(0, pathQuery[0].length()-1);
							}
							methodDetailsMap.put("verb", Arrays.asList(new String[] {verbPath[0]}));
							methodDetailsMap.put("path", Arrays.asList(new String[] {pathQuery[0]}));
							if (pathQuery.length > 1) {
								methodDetailsMap.put("query", Arrays.asList(new String[] {pathQuery[1]}));
							}
						}
					}
					
					value = getCellValue(row.getCell(exceptionsCol));
					if (value != null) {
						if (methodDetailsMap.get("exceptions") == null) {
							ArrayList<String> al = new ArrayList<String>();
							methodDetailsMap.put("exceptions", al);
						} 
						((ArrayList<String>) methodDetailsMap.get("exceptions")).add(value);

					}
					
					value = getCellValue(row.getCell(paramsCol));
					if (value != null) {
						if (methodDetailsMap.get("params") == null) {
							ArrayList<String> al = new ArrayList<String>();
							methodDetailsMap.put("params", al);
						} 
						((ArrayList<String>) methodDetailsMap.get("params")).add(value);
					}

					value = getCellValue(row.getCell(paramTypeCol));
					if (value != null) {
						if (methodDetailsMap.get("paramTypes") == null) {
							ArrayList<String> al = new ArrayList<String>();
							methodDetailsMap.put("paramTypes", al);
						} 
						((ArrayList<String>) methodDetailsMap.get("paramTypes")).add(value);
					}
				}
			}
		}
	}

	
	private static String getCellValue(HSSFCell cell)
	{
		String value = null;
		if (cell != null) {
			switch (cell.getCellType()) 
			{
			case HSSFCell.CELL_TYPE_FORMULA:
				value = cell.getStringCellValue();
				break;

			case HSSFCell.CELL_TYPE_NUMERIC:
				value = String.valueOf(cell.getNumericCellValue());
				break;

			case HSSFCell.CELL_TYPE_STRING:
				value =  cell.getStringCellValue();
				break;

			default:
			}
		} 
		if (value != null) {
			value = value.trim();
			if (value.isEmpty()) {
				value = null;	
			}
		}
		return value;
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
			} 
			else if (methodMap.get(methodMapKey).containsKey("query") ) {		
				String queryString = methodMap.get(methodMapKey).get("query").get(0);
				if (methodMap.get(methodMapKey).get("query").get(0).toLowerCase().contains(key.toLowerCase()+"=")) {
					location = "queryString";
				}
			} 
			// parameter is in the message body. 
			//  determine ParamPart vs FilePart
			else if (keyType != null) {
				if (keyType.equals("Subject") || keyType.endsWith("Identifier") ||
					keyType.equals("Permission") || keyType.equals("NodeReference") ||
					keyType.equals("boolean") || keyType.equals("DateTime")) 
				{
					location = "paramPart";
				} else {
					location = "filePart";
				}
			} else {
				location = "null keyType";
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
