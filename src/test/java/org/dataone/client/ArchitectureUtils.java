package org.dataone.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ArchitectureUtils {
	
	protected static Log log = LogFactory.getLog(ArchitectureUtils.class);
	
	private static String methodRefDoc = 
			"https://repository.dataone.org/documents/Projects/cicore/" +
			"architecture/api-documentation/MethodCrossReference.xls";

	public static boolean checkDocTypeEqualsJavaType(String docParamType, String implParamType)
	{
		docParamType = docParamType.toLowerCase();
		implParamType = implParamType.toLowerCase();
		if (docParamType.equals(implParamType))
			return true;
		
		if (docParamType.equals("bytes") && implParamType.equals("inputstream"))
			return true;
		
		if (docParamType.equals("octetstream") && implParamType.equals("inputstream"))
			return true;
		
		if (docParamType.equals("relationshipenum") && implParamType.equals("string"))
			return true;
		
		if (docParamType.equals("datetime")  && implParamType.equals("date"))
			return true;
		
		if (docParamType.equals("unsigned long") && implParamType.equals("long"))
			return true;
		
		if (docParamType.equals("exception") && implParamType.equals("synchronizationfailed"))
			return true;
		
		return false;
	}
	
	
	public static HashMap<String,HashMap<String,List<String>>>setUpMethodDocumentationMap() throws IOException  
	{
		// get and parse architecture document
		URL url = new URL(methodRefDoc);
		InputStream is = url.openStream();
		HSSFWorkbook wb = new HSSFWorkbook(is);
//		System.out.println("Data dump:\n");
		
		// map<method,map<aspect,set<stringValue>>>
		// eg: methodMap.get("ping").get("params")
		HashMap<String,HashMap<String,List<String>>> methodMap = 
				new HashMap<String,HashMap<String,List<String>>>();
		
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			HSSFSheet sheet = wb.getSheetAt(k);
			if (!wb.getSheetName(k).equals("Functions")) {
				continue;
			}
			int rows = sheet.getPhysicalNumberOfRows();
			log.info("Sheet " + k + " \"" + wb.getSheetName(k) + "\" has " 
				+ rows + " physically defined row(s).");
			rows = sheet.getLastRowNum();
			log.info("Sheet " + k + " \"" + wb.getSheetName(k) + "\" has " 
					+ rows + " logical row(s).");
			
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
					log.debug("mapKey = " + methodMapKey);
					
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
						methodDetailsMap.put("returnType", Arrays.asList(new String[] {value}));
					}
					
					value = getCellValue(row.getCell(restCol));
					log.debug("rest column (verb & path) " + value); 
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
		return methodMap;
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
	
	
}
