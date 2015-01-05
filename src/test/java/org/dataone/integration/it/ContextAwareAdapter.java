package org.dataone.integration.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.dataone.client.rest.MultipartRestClient;
import org.dataone.integration.ContextAwareTestCaseDataone;

public class ContextAwareAdapter {

    protected ContextAwareTestCaseDataone catc;
    public static MultipartRestClient MULTIPART_REST_CLIENT;
    
    public ContextAwareAdapter(ContextAwareTestCaseDataone catc) {
        this.catc = catc;
        ContextAwareAdapter.MULTIPART_REST_CLIENT = this.catc.MULTIPART_REST_CLIENT;
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
