package org.dataone.integration.it;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNReadTestDefinitions;
import org.dataone.integration.it.testDefinitions.ReadTestDefinitions;
import org.dataone.integration.it.testImplementations.CNReadTestImplementations;
import org.dataone.integration.it.testImplementations.ReadTestImplementations;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNCore functionality for v1 of the API 
 */
public class CNReadV1IT extends ContextAwareTestCaseDataone 
        implements ReadTestDefinitions, CNReadTestDefinitions {

    private ReadTestImplementations readTestImpl;
    private CNReadTestImplementations cnReadTestImpl;
    
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 1 of read API methods";
    }
    
    @Before
    public void setup() {
        readTestImpl = new ReadTestImplementations(this);
        cnReadTestImpl = new CNReadTestImplementations(this);
    }

    @Override
    @Test
    public void testGet() {
        readTestImpl.testGet(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe() {
        readTestImpl.testDescribe(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testResolve() {
        cnReadTestImpl.testResolve(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSearch() {
        cnReadTestImpl.testSearch(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testSearch_Solr_unicodeTests() {
        cnReadTestImpl.testSearch_Solr_unicodeTests(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testQuery() {
        cnReadTestImpl.testQuery(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testQuery_Authentication() {
        cnReadTestImpl.testQuery_Authentication(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testListQueryEngines() {
        cnReadTestImpl.testListQueryEngines(getCoordinatingNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        cnReadTestImpl.testGetQueryEngineDescription(getCoordinatingNodeIterator(), "v1");
    }
}
