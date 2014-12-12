package org.dataone.integration.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.CNode;
import org.dataone.client.D1Client;
import org.dataone.client.MNode;
import org.dataone.client.ObjectFormatCache;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeType;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectFormatList;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class ProductionResourceMapParsingIT {

	protected static Log log = LogFactory.getLog(ProductionResourceMapParsingIT.class);

	@Test
	public void test() {
		
	}

	
    /**
     * Tests that a resource map can be parsed by the ResourceMapFactory.
     * Method: for every formatID of type resource, pull an objectList of just that
     * type (maximum of 20), and try to parse each one.
     * @throws ServiceFailure 
     * @throws NotImplemented 
     */
    @Test
    public void testResourceMapParsing() throws NotImplemented, ServiceFailure {
    	
    	System.out.println("testResourceMapParsing\n\n");
//    	setupClientSubject_NoCert();
    	CNode cn = new CNode("https://cn.dataone.org/cn");
    	String currentUrl = null;
    	Iterator<Node> it = cn.listNodes().getNodeList().iterator();
    	ObjectFormatList ofl = cn.listFormats();
       	while (it.hasNext()) {
       		Node node = it.next();
       		if (node.getType() == NodeType.MN) {
       			currentUrl = node.getBaseURL();
       			MNode mn = new MNode(currentUrl);	
       			currentUrl = mn.getNodeBaseServiceUrl();
       			System.out.println("\nMember Node: " + currentUrl);

       			try {
       				boolean foundOne = false;
//       				ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
       				for(ObjectFormat of : ofl.getObjectFormatList()) 
       				{
       					if (of.getFormatType().equals("RESOURCE")) {      				
       						try {
       							ObjectList ol = mn.listObjects(null, null, of.getFormatId(),null, null, 20);
       							System.out.println("...checking Format: " + of.getFormatId().getValue() +
       								" (" + ol.getTotal() + " items)");
       							if (ol.sizeObjectInfoList() > 0) {
       								for (ObjectInfo oi : ol.getObjectInfoList()) {
       									String result = "ok";
       									String resMapContent;
       									try {
       										InputStream is = mn.get(oi.getIdentifier());
       										foundOne = true;
       										log.info("Found public resource map: " + oi.getIdentifier().getValue());
       										resMapContent = IOUtils.toString(is);
       										if (resMapContent != null) {
       											ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
       										} else {
       											handleFail(mn.getLatestRequestUrl(),"got null content from the get request");
       										}
       									} catch (Exception e) {
       										result = e.getClass().getSimpleName() + ": " + 
       												e.getMessage() + 
       												"at line number " + e.getStackTrace()[0].getLineNumber();       													
       									}

       									System.out.println("   " + of.getFormatId().getValue() + ": " + 
       											oi.getIdentifier().getValue() + ": "+ 
       											result);

       								}
       							}
       						} catch (Exception e) {
       							System.out.println(e.getClass().getSimpleName() + ": " + 
       									e.getMessage() + 
       									"at line number " + e.getStackTrace()[0].getLineNumber());     
       						}
       					}
       				}
       				if (!foundOne) {
       					System.out.println("No public resource maps " +
       							"returned from listObjects.  Cannot test.\n");
       				}
       			}
//       			catch (BaseException e) {
//       				handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
//       						e.getDetail_code() + ":: " + e.getDescription());
//       			}
       			catch(Exception e) {
       				e.printStackTrace();
       				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage() +
       						"at line number " + e.getStackTrace()[0].getLineNumber());
       			}
       			catch(Throwable th) {
       				th.printStackTrace();
       				handleFail(currentUrl,th.getClass().getName() + ": " + th.getMessage() +
       						"at line number " + th.getStackTrace()[0].getLineNumber());
       			}
       		}
       	}
    }
    
    
    
    @Test
    public void testResourceMapParsingThorough() throws NotImplemented, ServiceFailure {
    	
    	System.out.println("testResourceMapParsing\n\n");
//    	setupClientSubject_NoCert();
    	CNode cn = new CNode("https://cn.dataone.org/cn");
    	String currentUrl = null;
    	Iterator<Node> it = cn.listNodes().getNodeList().iterator();
    	ObjectFormatList ofl = cn.listFormats();
       	while (it.hasNext()) {
       		Node node = it.next();
       		if (node.getType() == NodeType.MN && node.getBaseURL().equals("https://merritt.cdlib.org:8084/knb/d1/mn")) {
       			currentUrl = node.getBaseURL();
       			MNode mn = new MNode(currentUrl);	
       			currentUrl = mn.getNodeBaseServiceUrl();
       			System.out.println("\nMember Node: " + currentUrl);

       			try {
       				boolean foundOne = false;
//       				ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
       				for(ObjectFormat of : ofl.getObjectFormatList()) 
       				{
       					if (of.getFormatType().equals("RESOURCE")) {      				
       						try {
       							ObjectList ol = mn.listObjects(null, null, of.getFormatId(),null, null, 20000);
       							System.out.println("...checking Format: " + of.getFormatId().getValue() +
       								" (" + ol.getTotal() + " items)");
       							if (ol.sizeObjectInfoList() > 0) {
       								for (int i = 0; i< 20000; i +=5) {
       									ObjectInfo oi = ol.getObjectInfo(i);

       									String result = "ok";
       									String resMapContent;
       									try {
       										InputStream is = mn.get(oi.getIdentifier());
       										foundOne = true;
       										log.info("Found public resource map: " + oi.getIdentifier().getValue());
       										resMapContent = IOUtils.toString(is);
       										if (resMapContent != null) {
       											ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
       										} else {
       											handleFail(mn.getLatestRequestUrl(),"got null content from the get request");
       										}
       									} catch (Exception e) {
       										result = e.getClass().getSimpleName() + ": " + 
       												e.getMessage() + 
       												"at line number " + e.getStackTrace()[0].getLineNumber();       													
       									}

       									System.out.println("   " + i + ". " + of.getFormatId().getValue() + ": " + 
       											oi.getIdentifier().getValue() + ": "+ 
       											result);

       								}
       							}
       						} catch (Exception e) {
       							System.out.println(e.getClass().getSimpleName() + ": " + 
       									e.getMessage() + 
       									"at line number " + e.getStackTrace()[0].getLineNumber());     
       						}
       					}
       				}
       				if (!foundOne) {
       					System.out.println("No public resource maps " +
       							"returned from listObjects.  Cannot test.\n");
       				}
       			}
//       			catch (BaseException e) {
//       				handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
//       						e.getDetail_code() + ":: " + e.getDescription());
//       			}
       			catch(Exception e) {
       				e.printStackTrace();
       				handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage() +
       						"at line number " + e.getStackTrace()[0].getLineNumber());
       			}
       			catch(Throwable th) {
       				th.printStackTrace();
       				handleFail(currentUrl,th.getClass().getName() + ": " + th.getMessage() +
       						"at line number " + th.getStackTrace()[0].getLineNumber());
       			}
       		}
       	}
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
    public void checkEquals(final String host, final String message, final String s1, final String s2)
    {
    	errorCollector.checkSucceeds(new Callable<Object>() 
    			{
    		public Object call() throws Exception 
    		{
    			if (host != null) {
    				assertThat(message + "  [for host " + host + " ]", s1, is(s2));
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
    public void checkTrue(final String host, final String message, final boolean b)
    {
    	errorCollector.checkSucceeds(new Callable<Object>() 
    			{
    		public Object call() throws Exception 
    		{
    			if (host != null) {	
    				assertThat(message + "  [for host " + host + " ]", b, is(true));
    			} else {
    				assertThat(message, b, is(true));
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
    public  void checkFalse(final String host, final String message, final boolean b)
    {
    	errorCollector.checkSucceeds(new Callable<Object>() 
    			{
    		public Object call() throws Exception 
    		{
    			if (host != null) {	
    				assertThat(message + "  [for host " + host + " ]", b, is(false));
    			} else {
    				assertThat(message, b, is(false));
    			}
    			return null;
    		}
    			});
    }

    /**
     * performs the equivalent of the junit fail method
     * using the errorCollector to record the error and keep going
     * 
     * @param host
     * @param message
     */
    public void handleFail(final String host, final String message)
    {
    	errorCollector.checkSucceeds(new Callable<Object>() 
    			{
    		public Object call() throws Exception 
    		{
    			if (host != null) {	
    				fail(message + "  [for host " + host + " ]");
    			} else {
    				fail(message);
    			}
    			return null;
    		}
    			});
    }
       	
}
