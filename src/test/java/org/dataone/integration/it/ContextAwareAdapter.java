package org.dataone.integration.it;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dataone.client.rest.MultipartRestClient;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.service.types.v1.Subject;

/**
 * ContextAwareAdapter is used as a base class for test implementation classes
 * that are subclasses of ContextAwareTestCaseDataone, providing wrapper methods
 * for some of the latter's methods used by test implementations.
 * 
 * @author rnahf
 *
 */
public class ContextAwareAdapter {

    protected static Log log = LogFactory.getLog(ContextAwareAdapter.class);

    
    protected ContextAwareTestCaseDataone catc;
//    public static MultipartRestClient MULTIPART_REST_CLIENT;
    public String cnSubmitter;
    
    public ContextAwareAdapter(ContextAwareTestCaseDataone catc) {
        this.catc = catc;
        this.cnSubmitter = catc.cnSubmitter;
//        ContextAwareAdapter.MULTIPART_REST_CLIENT = this.catc.MULTIPART_REST_CLIENT;
    }

//    public Subject setupClientSubject(String clientSubjectName) {
//        return this.catc.setupClientSubject(clientSubjectName);
//    }
//    
//    public void setupClientSubject_NoCert() {
//        this.catc.setupClientSubject_NoCert();
//    }
    
    public MultipartRestClient getSession(String subjectString) {
        return this.catc.getSession(subjectString);
    }
    
    
    public String createNodeAbbreviation(String baseUrl) {
        return this.catc.createNodeAbbreviation(baseUrl);
      
    }
   
    public String getTestObjectSeries() {
        return this.catc.getTestObjectSeries();
    }
    
    public String getTestObjectSeriesSuffix() {
        return this.catc.getTestObjectSeriesSuffix();
    }
    
    
    public void checkEquals(final String host, final String message, final String s1, final String s2)
    {
        this.catc.checkEquals(host, message, s1, s2);
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
        this.catc.checkTrue(host, message, b);

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
        this.catc.checkFalse(host, message, b);
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
        this.catc.handleFail(host, message);
    }
    
    public void printTestHeader(String message) {
        this.catc.printTestHeader(message);
    }

}
