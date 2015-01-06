package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.junit.Before;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNReadV1IT extends ContextAwareTestCaseDataone implements ReadTestDefinitions, CNReadTestDefinitions {

    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of read API methods";
    }
    
    protected CoreTestImplementations coreTestImpl;
    
    @Before 
    public void setup() {
        this.coreTestImpl = new CoreTestImplementations(this);
    }

    @Override
    public void testResolve() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testSearch() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testSearch_Solr_unicodeTests() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testQuery() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testQuery_Authentication() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListQueryEngines() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetQueryEngineDescription() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGet() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGet_NotFound() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGet_IdentifierEncoding() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetSystemMetadata() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetSystemMetadata_NotFound() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetSystemMetadata_IdentifierEncoding() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testDescribe() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testDescribe_NotFound() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testDescribe_IdentifierEncoding() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetChecksum() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetChecksum_NotFound() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testGetChecksum_IdentifierEncoding() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListObjects() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListObjects_Slicing() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListObjects_FromDateTest() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListObjects_FormatIdFilteringTest() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        // TODO Auto-generated method stub
        
    }

}
