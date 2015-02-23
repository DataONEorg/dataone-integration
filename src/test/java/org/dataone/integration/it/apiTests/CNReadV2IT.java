package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.CNReadSidTestDefinitions;
import org.dataone.integration.it.testDefinitions.CNReadTestDefinitions;
import org.dataone.integration.it.testDefinitions.ReadTestDefinitions;
import org.dataone.integration.it.testImplementations.CNReadTestImplementations;
import org.dataone.integration.it.testImplementations.ReadTestImplementations;
import org.dataone.integration.it.testImplementations.SidCNTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests CNRead functionality for v2 of the API 
 */
public class CNReadV2IT extends ContextAwareTestCaseDataone 
        implements ReadTestDefinitions, CNReadTestDefinitions, CNReadSidTestDefinitions {

    @WebTestImplementation
    private ReadTestImplementations readTestImpl;
    
    @WebTestImplementation
    private CNReadTestImplementations cnReadTestImpl;
    
    @WebTestImplementation
    private SidCNTestImplementations sidImpl;
    
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the CN version 2 of read API methods";
    }
    
    @Before
    public void setup() {
        readTestImpl = new ReadTestImplementations(this);
        cnReadTestImpl = new CNReadTestImplementations(this);
        sidImpl = new SidCNTestImplementations();
    }

    @After
    public void cleanUp() {
        sidImpl.cleanUp();
    }
    
    @Override
    @Test
    public void testGet() {
        readTestImpl.testGet(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGet_NotFound() {
        readTestImpl.testGet_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGet_IdentifierEncoding() {
        readTestImpl.testGet_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata() {
        readTestImpl.testGetSystemMetadata(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata_NotFound() {
        readTestImpl.testGetSystemMetadata_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetSystemMetadata_IdentifierEncoding() {
        readTestImpl.testGetSystemMetadata_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe() {
        readTestImpl.testDescribe(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe_NotFound() {
        readTestImpl.testDescribe_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testDescribe_IdentifierEncoding() {
        readTestImpl.testDescribe_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum() {
        readTestImpl.testGetChecksum(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum_NotFound() {
        readTestImpl.testGetChecksum_NotFound(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetChecksum_IdentifierEncoding() {
        readTestImpl.testGetChecksum_IdentifierEncoding(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_Slicing() {
        readTestImpl.testListObjects_Slicing(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FromDateTest() {
        readTestImpl.testListObjects_FromDateTest(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTest() {
        readTestImpl.testListObjects(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListObjects_FormatIdFilteringTestFakeFormat() {
        readTestImpl.testListObjects_FormatIdFilteringTestFakeFormat(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testResolve() {
        cnReadTestImpl.testResolve(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSearch() {
        cnReadTestImpl.testSearch(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSearch_Solr_unicodeTests() {
        cnReadTestImpl.testSearch_Solr_unicodeTests(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testQuery() {
        cnReadTestImpl.testQuery(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testQuery_Authentication() {
        cnReadTestImpl.testQuery_Authentication(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testListQueryEngines() {
        cnReadTestImpl.testListQueryEngines(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetQueryEngineDescription() {
        cnReadTestImpl.testGetQueryEngineDescription(getCoordinatingNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testSidGet() {
        sidImpl.testGet();
    }

    @Override
    @Test
    public void testSidGetSystemMetadata() {
        sidImpl.testGetSystemMetadata();
    }

    @Override
    @Test
    public void testSidDescribe() {
        sidImpl.testDescribe();
    }

    @Override
    @Test
    public void testSidListObjects() {
        sidImpl.testListObjects();
    }

    @Override
    @Test
    public void testSidResolve() {
        sidImpl.testResolve();
    }
}
