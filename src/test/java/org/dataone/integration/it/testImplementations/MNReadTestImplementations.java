package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.ExampleUtilities;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.InvalidToken;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.exceptions.NotFound;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.exceptions.SynchronizationFailed;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.TypeMarshaller;

public class MNReadTestImplementations extends ContextAwareAdapter {

    protected static Log log = LogFactory.getLog(MNReadTestImplementations.class);
    private static Vector<String> unicodeStringV;
    private static Vector<String> escapedStringV;
    
    
	public MNReadTestImplementations(ContextAwareTestCaseDataone catc) {
		super(catc);
	}

	public void testSynchronizationFailed_NoCert(Iterator<Node> nodeIterator, String version){
		while (nodeIterator.hasNext())
        	testSynchronizationFailed_NoCert(nodeIterator.next(), version);
	}

    public void testSynchronizationFailed_NoCert(Node node, String version) {

        ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testSynchronizationFailed() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:"
                    + catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + catc.getTestObjectSeries();
            Identifier id = catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));
            SynchronizationFailed sf = new SynchronizationFailed("0", "a message", id.getValue(),
                    null);
            System.out.println(sf.serialize(SynchronizationFailed.FMT_XML));
            callAdapter.synchronizationFailed(null,
                    new SynchronizationFailed("0", "a message", id.getValue(), null));
            checkTrue(callAdapter.getLatestRequestUrl(),
                    "synchronizationFailed() does not throw exception", true);
        } catch (NotAuthorized e) {
            ; // this is an acceptable (and preferrable) outcome for calling without a client cert.
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    e.getClass().getSimpleName() + ":: " + e.getDetail_code() + " "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

	public void testGetReplica_PublicObject(Iterator<Node> nodeIterator, String version){
		while (nodeIterator.hasNext())
        	testGetReplica_PublicObject(nodeIterator.next(), version);
	}

    public void testGetReplica_PublicObject(Node node, String version) {

        String clientSubject = "urn:node:cnStageUNM1";
        ContextAwareTestCaseDataone.setupClientSubject(clientSubject);
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetReplica() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:"
                    + catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl())
                    + ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier pid = catc.procurePublicReadableTestObject(callAdapter,
                    D1TypeBuilder.buildIdentifier(objectIdentifier));

            InputStream is = callAdapter.getReplica(null, pid);
            checkTrue(callAdapter.getLatestRequestUrl(), "Successful getReplica() call"
                    + "should yield a non-null inputStream.", is != null);
        } catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(), "No Objects available to test against");
        } catch (BaseException e) {
            handleFail(
                    callAdapter.getLatestRequestUrl(),
                    "Should be able to retrieve " + "a public object (as subject " + clientSubject
                            + ").  If the node is checking the client subject against the "
                            + "CN for all getReplica requests, and the node is not "
                            + "registered to an environment, this failure can be ignored.  Got:"
                            + e.getClass().getSimpleName() + ": " + e.getDetail_code() + ":: "
                            + e.getDescription());
        } catch (Exception e) {
            e.printStackTrace();
            handleFail(currentUrl, e.getClass().getName() + ": " + e.getMessage());
        }
    }

	public void testGetReplica_ValidCertificate_NotMN(Iterator<Node> nodeIterator, String version){
		ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        while (nodeIterator.hasNext()) {
        	testGetReplica_ValidCertificate_NotMN(nodeIterator.next(), version);
        }
	}

	public void testGetReplica_ValidCertificate_NotMN(Node node, String version){
		
	    ContextAwareTestCaseDataone.setupClientSubject("testRightsHolder");
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetReplica_AuthenticateITKUser() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + 
                    catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl()) +
                    ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier pid = catc.procurePublicReadableTestObject(callAdapter,D1TypeBuilder.buildIdentifier(objectIdentifier));
            callAdapter.getReplica(null, pid);
            handleFail(callAdapter.getLatestRequestUrl(),"with non-Node client certificate, getReplica() should throw exception");
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (NotAuthorized e) {
            // expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
	}


	public void testGetReplica_NoCertificate(Iterator<Node> nodeIterator, String version){
		while (nodeIterator.hasNext())
        	testGetReplica_NoCertificate(nodeIterator.next(), version);
	}

	public void testGetReplica_NoCertificate(Node node, String version){
		
	    ContextAwareTestCaseDataone.setupClientSubject_NoCert();
	    MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetReplica_NoCert() vs. node: " + currentUrl);

        try {
            String objectIdentifier = "TierTesting:" + 
                    catc.createNodeAbbreviation(callAdapter.getNodeBaseServiceUrl()) +
                    ":Public_READ" + catc.getTestObjectSeriesSuffix();
            Identifier pid = catc.procurePublicReadableTestObject(callAdapter,D1TypeBuilder.buildIdentifier(objectIdentifier));
            callAdapter.getReplica(null, pid);
            handleFail(callAdapter.getLatestRequestUrl(),"with no client certificate, getReplica() should throw exception");
        }
        catch (IndexOutOfBoundsException e) {
            handleFail(callAdapter.getLatestRequestUrl(),"No Objects available to test against");
        }
        catch (InvalidToken e) {
            // expected behavior
        }
        catch (NotAuthorized e) {
            // also expected behavior
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
	}


	public void testGetReplica_NotFound(Iterator<Node> nodeIterator, String version){
		while (nodeIterator.hasNext())
        	testGetReplica_NotFound(nodeIterator.next(), version);
	}

	public void testGetReplica_NotFound(Node node, String version){
		
	    ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetReplica() vs. node: " + currentUrl);

        try {
            String fakeID = "TestingNotFound:" + ExampleUtilities.generateIdentifier(); 
            InputStream is = callAdapter.get(null,D1TypeBuilder.buildIdentifier(fakeID));
            handleFail(callAdapter.getLatestRequestUrl(),"getReplica(fakeID) should not return an objectStream.");
            is.close();
        }
        catch (NotFound nf) {
            ;  // expected outcome
        }
        catch (BaseException e) {
            handleFail(callAdapter.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " + 
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
	}


	public void testGetReplica_IdentifierEncoding(Iterator<Node> nodeIterator, String version){
        while (nodeIterator.hasNext())
        	testGetReplica_IdentifierEncoding(nodeIterator.next(), version);
	}

	public void testGetReplica_IdentifierEncoding(Node node, String version){
	    
	    ContextAwareTestCaseDataone.setupClientSubject_NoCert();
        MNCallAdapter callAdapter = new MNCallAdapter(MULTIPART_REST_CLIENT, node, version);
        String currentUrl = callAdapter.getNodeBaseServiceUrl();
        printTestHeader("testGetReplica_IdentifierEncoding() vs. node: " + currentUrl);

        Vector<String> nodeSummary = new Vector<String>();
        nodeSummary.add("Node Test Summary for node: " + currentUrl );
        printTestHeader("  Node:: " + currentUrl);
        setupIdentifierVectors();
        
        for (int j=0; j<unicodeStringV.size(); j++) 
        {
            String status = "OK   ";

            log.info("");
            log.info(j + "    unicode String:: " + unicodeStringV.get(j));
            String idString = "Test" + ExampleUtilities.generateIdentifier() + "_" + unicodeStringV.get(j) ;
            String idStringEscaped = "Test"  + ExampleUtilities.generateIdentifier() + "_" + escapedStringV.get(j);


            try {
                callAdapter.getReplica(null, D1TypeBuilder.buildIdentifier(idString));
                handleFail(callAdapter.getLatestRequestUrl(), "getSystemMetadata() against the fake identifier (" +
                        idStringEscaped + ") should throw NotFound");

                status = "Error";
            }
            catch (NotFound nf) {
                ;
            }
            catch (ServiceFailure e) {
                if (e.getDescription().contains("Providing message body")) {
                    if (e.getDescription().contains("404: NotFound:")) {
                        // acceptable result
                        ;
                    }
                } 
                else {
                    status = String.format("Error:: %s: %s: %s",
                            e.getClass().getSimpleName(),
                            e.getDetail_code(),
                            first100Characters(e.getDescription()));
                }
            }
            catch (BaseException e) {
                status = String.format("Error:: %s: %s: %s",
                        e.getClass().getSimpleName(),
                        e.getDetail_code(),
                        first100Characters(e.getDescription()));
            }
            catch(Exception e) {
                status = "Error";
                e.printStackTrace();
                status = String.format("Error:: %s: %s",
                        e.getClass().getName(),
                        first100Characters(e.getMessage()));
            }

            nodeSummary.add("Test " + j + ": " + status + ": " + unicodeStringV.get(j));
        }
        
        for (String result : nodeSummary) {
            if (result.contains("Error")) {
                handleFail(null, currentUrl + " " + tablifyResults(nodeSummary) );
                break;
            }
        }
	}

    private void setupIdentifierVectors() {
        if(unicodeStringV != null && escapedStringV != null)
            return;

        // get identifiers to check with
        unicodeStringV = new Vector<String>();
        escapedStringV = new Vector<String>();
        //   TODO: test against Unicode characters when metacat supports unicode        
        InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testUnicodeStrings.utf8.txt");
        //InputStream is = this.getClass().getResourceAsStream("/d1_testdocs/encodingTestSet/testAsciiStrings.utf8.txt");
        Scanner s = new Scanner(is,"UTF-8");
        String[] temp;
        int c = 0;
        try{
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (line.startsWith("common-") || line.startsWith("path-"))
                {
                    if (line.contains("supplementary"))
                        continue;

                    temp = line.split("\t");

                    // identifiers can't contain spaces by default
                    if (temp[0].contains(" ")) 
                        continue;

                    log.info(c++ + "   " + line);
                    unicodeStringV.add(temp[0]);
                    escapedStringV.add(temp[1]);    
                }
            }
        } finally {
            s.close();
        }
    }
	
	private String first100Characters(String s) {
        if (s.length() <= 100) 
            return s;
        return s.substring(0, 100) + "...";
    }

    private String tablifyResults(Vector<String> results)
    {
        StringBuffer table = new StringBuffer("Failed 1 or more identifier encoding tests");
        for (String result: results) {
            table.append(result);   
            table.append("\n    ");
        }
        return table.toString();         
    }
}
