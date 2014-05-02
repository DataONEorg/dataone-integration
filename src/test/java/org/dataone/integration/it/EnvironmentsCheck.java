package org.dataone.integration.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Callable;

import org.dataone.client.CNode;
import org.dataone.client.D1Node;
import org.dataone.client.MNode;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotImplemented;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.NodeList;
import org.dataone.service.types.v1.NodeType;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;


public class EnvironmentsCheck {

	private static final int MAX_CLOCK_DRIFT_SEC = 5;
	
	
	String[] rrCns = new String[] { 
			"https://cn-dev-rr.dataone.org/cn",
			"https://cn-sandbox.dataone.org/cn",
			"https://cn-stage.dataone.org/cn",
			"https://cn.dataone.org/cn"
	};
	
	@Test
	public void checkEnvironment_exclusivity() 
	{
		HashMap<String, Integer> nodeRefMap = new HashMap<String,Integer>();
		HashMap<String, Integer> baseUrlMap = new HashMap<String, Integer>();
		
		
		System.out.println(String.format("%-16s  %-32s %2s    %-64s %2s  %s",
				"environment",
				"NodeReference",
				"#",
				"baseUrl",
				"#",
				"duplicate?"
				));
		
		int duplicates = 0;
		
		// pull nodelists for each environment
		for (String baseUrl: rrCns) {
			CNode cn = new CNode(baseUrl);
			NodeList nl;
			try {
				nl = cn.listNodes();
				for (Node n: nl.getNodeList()) {

					int idCount = 0;
					if (nodeRefMap.containsKey(n.getIdentifier().getValue())) {
						idCount = nodeRefMap.get(n.getIdentifier().getValue());
					}
					nodeRefMap.put(n.getIdentifier().getValue(), new Integer(++idCount));
					
					int urlCount = 0;
					if (baseUrlMap.containsKey(n.getBaseURL())) {
						urlCount = baseUrlMap.get(n.getBaseURL());
					}
					baseUrlMap.put(n.getBaseURL(), new Integer(++urlCount));

					// increment the failure flag if we found a duplicate
					duplicates += idCount - 1 + urlCount - 1;
					
					
					System.out.println(String.format("%-16s  %-32s %2d    %-64s %2d  %s",
							baseUrl.replaceFirst("https://", "").replace(".dataone.org/cn",""),
							n.getIdentifier().getValue(),
							idCount,
							n.getBaseURL(),
							urlCount,
							idCount > 1 || urlCount > 1 ? "*" : ""
							));
				
				}
			} catch (NotImplemented e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got NotImplemented::" + e.getDescription());
			} catch (ServiceFailure e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got ServiceFailure::" + e.getDescription());
			}
		}
		System.out.println();
		System.out.println("Identifier distribution");
		System.out.println("=======================");
		for (String key : nodeRefMap.keySet()) {
			System.out.println(String.format("%-32s  %3d",key, nodeRefMap.get(key)));
		}
		System.out.println();
		System.out.println("BaseUrl distribution");
		System.out.println("====================");
		for (String baseUrl : baseUrlMap.keySet()) {
			System.out.println(String.format("%-64s  %3d",baseUrl, baseUrlMap.get(baseUrl)));
		}

		org.junit.Assert.assertEquals("Expect no duplicate node identifiers or baseUrls across the separate environments", duplicates, 0);
		
		
	}
	
	
	@Test
	public void testUTC() {
		for (String baseUrl: rrCns) {
			CNode cn = new CNode(baseUrl);
			NodeList nl;
		
			long startTime = (new Date()).getTime();
			
			try {
				nl = cn.listNodes();
				System.out.println("");
				System.out.println("CONTEXT STARTING POINT: " + baseUrl);
				
				System.out.println(String.format("%-64s\t%s\t%s\t%s\t%s\t%s",
						"node",
						"startTime",
						"callTime",
						"pingDate",
						"callDuration",
						"adjustedTime"));
				
				Long minTime = null;
				Long maxTime = null;
				for (Node n: nl.getNodeList()) {
					D1Node d1Node = null;
					if (n.getType().equals(NodeType.CN)) {
						d1Node = new CNode(n.getBaseURL());
					} else if (n.getType().equals(NodeType.MN)) {
						d1Node = new MNode(n.getBaseURL());
					}
					long callTime = (new Date()).getTime();
					try {
						Date pingDate = d1Node.ping();
						long callReturnTime = (new Date()).getTime();

//						long adjustedTime = pingDate.getTime() - (startTime - callTime);
						long adjustedTime = pingDate.getTime() - callTime;
						
						System.out.println(String.format("%-64s\t%d\t%d\t%d\t%d\t%4d",
								n.getBaseURL(),
								startTime,
								callTime,
								pingDate.getTime(),
								callReturnTime - callTime,
								adjustedTime));
						
						
						
						if ( minTime == null || adjustedTime < minTime) {
							minTime = new Long(adjustedTime);
						}
						if ( maxTime == null || adjustedTime > maxTime) {
							maxTime = new Long(adjustedTime);
						}
						if (minTime != null && maxTime != null) {
							checkTrue(baseUrl, "normalized ping times within the environment should be" +
								" within " + MAX_CLOCK_DRIFT_SEC + " seconds of each other (see test output for table)", maxTime - minTime < MAX_CLOCK_DRIFT_SEC * 1000);
						}
					}
					catch (BaseException be) {
						System.err.println(String.format("ERROR: ping() from %s " + 
								" threw exception %s",
								n.getBaseURL(),
								be.getClass().getSimpleName()
								));
					}
				}
				
				System.out.println("minTime = " + minTime);
				System.out.println("maxTime = " + maxTime);
				System.out.println("delta T = " + (maxTime - minTime));
				System.out.println();
				
			} catch (NotImplemented e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got NotImplemented::" + e.getDescription());
			} catch (ServiceFailure e) {
				System.err.println("ERROR: could not get NodeList from " + baseUrl
						+ ". Got ServiceFailure::" + e.getDescription());
			}
		}
	}
	
	@Ignore("not written")
	@Test
	public void testNodeBaseUrlConcordance() {
		// the baseurl in the nodelist should get you to the node, and also be 
		// able to retrieve the Node object and confirm the id and baseurl
	}
	
	@Ignore("not written")
	@Test
	public void testSpotTestResolve() {
		// does resolve take you there?
	}
	
	/**
	 * Tests should use the error collector to handle JUnit assertions
	 * and keep going.  The check methods in this class use this errorCollector
	 * the check methods 
	 */
	@Rule 
    public ErrorCollector errorCollector = new ErrorCollector();
	
	
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
            		assertThat(message + "  [for host " + host + "]", b, is(true));
            	} else {
            		assertThat(message, b, is(true));
            	}
                return null;
            }
        });
    }
	
	
	
}
