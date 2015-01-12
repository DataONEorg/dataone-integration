package org.dataone.integration.it.testImplementations;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.ServiceFailure;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.Permission;

public class SSLTestImplementations extends ContextAwareAdapter {

    public SSLTestImplementations(ContextAwareTestCaseDataone catc) {
        super(catc);
        // TODO Auto-generated constructor stub
    }

    public void testConnectionLayer_SelfSignedCert(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testConnectionLayer_SelfSignedCert(nodeIterator.next(), version);
    }

    public void testConnectionLayer_SelfSignedCert(Node node, String version)
    {
        String currentUrl = node.getBaseURL();
        CommonCallAdapter d1Node = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);

        try {
            this.catc.setupClientSubject("testPerson_SelfSigned");
            d1Node.getLogRecords(null, null, null, null, null, 0, 0);
            handleFail(d1Node.getLatestRequestUrl(), "ssl connection should not succeed with " +
            		"a self-signed certificate (untrusted CA): getLogRecords(...)");
        } catch (BaseException be) {
            if (!(be instanceof ServiceFailure)) {
                handleFail(d1Node.getLatestRequestUrl(),"a self-signed certificate should not be" +
                		" trusted and should throw a ServiceFailure. Got: "
                        + be.getClass().getSimpleName() + ": " +
                        be.getDetail_code() + ":: " + be.getDescription());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    
    public void testConnectionLayer_ExpiredCertificate(Iterator<Node> nodeIterator, String version) 
    {   
        this.catc.setupClientSubject("testPerson_Expired");
        
        while (nodeIterator.hasNext())
            testConnectionLayer_ExpiredCertificate(nodeIterator.next(), version);
        
        this.catc.setupClientSubject_NoCert();
    }

    
    public void testConnectionLayer_ExpiredCertificate(Node node, String version) 
    {    
        String currentUrl = node.getBaseURL();
        CommonCallAdapter d1Node = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);

        try {
            d1Node.ping();
        } catch (BaseException be) {
            if (!(be instanceof ServiceFailure)) {
            handleFail(d1Node.getLatestRequestUrl(),"an Expired Certificate should throw a ServiceFailure. Got: "
                    + be.getClass().getSimpleName() + ": " +
                    be.getDetail_code() + ":: " + be.getDescription());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }

    
    
    public void testConnectionLayer_dataoneCAtrusted(Iterator<Node> nodeIterator, String version) {
        this.catc.setupClientSubject("testSubmitter");
        while (nodeIterator.hasNext())
            testConnectionLayer_dataoneCAtrusted(nodeIterator.next(), version);
    }

    
    public void testConnectionLayer_dataoneCAtrusted(Node node, String version) 
    {    
        String currentUrl = node.getBaseURL();
        CommonCallAdapter d1Node = new CommonCallAdapter(MULTIPART_REST_CLIENT, node, version);

        try {
            d1Node.ping();
        } catch (BaseException e) {
            handleFail(d1Node.getLatestRequestUrl(),e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    
    
    }
}
