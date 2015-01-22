package org.dataone.integration.it.apiTests;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.ContentIntegrityTestDefinitions;
import org.dataone.integration.it.testDefinitions.SSLTestDefinitions;
import org.dataone.integration.it.testImplementations.ContentIntegrityTestImplementations;
import org.dataone.integration.it.testImplementations.SSLTestImplementations;
import org.junit.Before;
import org.junit.Test;

public class MNContentIntegrityV1V2IT extends ContextAwareTestCaseDataone 
implements  ContentIntegrityTestDefinitions {

    private ContentIntegrityTestImplementations contentTestImpl;

    
    @Before
    public void setup() {
        contentTestImpl = new ContentIntegrityTestImplementations(this);
    }

    @Override
    protected String getTestDescription() {
        return "performs various content parsing and consistency checks";
    }



    @Override
    @Test
    public void testResourceMap_Parsing()
    {
        contentTestImpl.testResourceMap_Parsing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testResourceMap_Checksum_Size_Consistency()
    {
        contentTestImpl.testResourceMap_Checksum_Size_Consistency(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testMetadata_Checksum_Size_Consistency()
    {
        contentTestImpl.testMetadata_Checksum_Size_Consistency(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testResourceMapParsing()
    {
        contentTestImpl.testResourceMapParsing(getMemberNodeIterator(), "v1");
    }

    @Override
    @Test
    public void testResourceMap_ResolveURL()
    {
        contentTestImpl.testResourceMap_ResolveURL(getMemberNodeIterator(), "v1");
    }

}
