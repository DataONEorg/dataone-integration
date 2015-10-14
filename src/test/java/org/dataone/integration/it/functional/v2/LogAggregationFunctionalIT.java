package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.LogAggregationFunctionalTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LogAggregationFunctionalIT extends ContextAwareTestCaseDataone {

    @WebTestImplementation
    LogAggregationFunctionalTestImplementations logAggImpl;

    @Override
    protected String getTestDescription() {
        return "Test Case that runs metadata management scenarios including v1 and v2 nodes.";
    }

    @Before
    public void setup() {
        logAggImpl = new LogAggregationFunctionalTestImplementations();
        logAggImpl.setup(getCoordinatingNodeIterator());
    }

    @Test
    public void testMnGetLogRecords_Access() {
        logAggImpl.testMnGetLogRecords_Access();
    }
    
    @Test
    public void testMnQuery_Access() {
        logAggImpl.testMnQuery_Access();
    }
    
    @Test
    public void testMnQuery_Params() {
        logAggImpl.testQuery_Params();
    }
    
    @Test
    public void testCnGetLogRecords_Aggregating() {
        logAggImpl.testCnGetLogRecords_Aggregating();
    }
    
    @Test
    public void testCnGetLogRecords_Access() {
        logAggImpl.testCnGetLogRecords_Access();
    }
}
