package org.dataone.integration.it;

public interface CNReadTestDefinitions {

    // see CNodeTier1_*.java classes 

    public void testResolve();

    public void testSearch();

    public void testSearch_Solr_unicodeTests();

    public void testQuery();

    public void testQuery_Authentication();

    public void testListQueryEngines();

    public void testGetQueryEngineDescription();

}
