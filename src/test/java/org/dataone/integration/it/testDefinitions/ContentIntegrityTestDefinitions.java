package org.dataone.integration.it.testDefinitions;

import org.junit.Test;


public interface ContentIntegrityTestDefinitions {

    /**
     * Tests that a resource map can be parsed by the ResourceMapFactory.
     */
    public void testResourceMap_Parsing();

    /**
     * Iterates known resource map formats to find an exemplar for testing that
     * the checksum and size in the ObjectInfo match what is in
     * systemMetadata, and matches what is recalculated when retrieving the object.
     *
     */
    @Test
    public void testResourceMap_Checksum_Size_Consistency();

    /**
     * Iterates known metadata formats to find an exemplar for testing that
     * the checksum and size in the ObjectInfo match what is in
     * systemMetadata, and matches what is recalculated when retrieving the object.
     *
     */
    @Test
    public void testMetadata_Checksum_Size_Consistency();

    /**
     * Tests that a resource map can be parsed by the ResourceMapFactory.
     * Method: for every formatID of type resource, pull an objectList of just that
     * type (maximum of 20), and try to parse each one.
     */
    @Test
    public void testResourceMapParsing();

    /**
     * Tests that the resource map's URLs point to cn/v1/resolve.
     */
    @Test
    public void testResourceMap_ResolveURL();
}
