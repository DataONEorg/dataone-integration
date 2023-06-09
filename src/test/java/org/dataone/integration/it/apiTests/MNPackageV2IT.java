package org.dataone.integration.it.apiTests;

import java.util.Iterator;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testDefinitions.MNPackageTestDefinitions;
import org.dataone.integration.it.testImplementations.MNPackageTestImplementations;
import org.dataone.integration.webTest.WebTestImplementation;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNPackageV2IT extends ContextAwareTestCaseDataone implements MNPackageTestDefinitions{

    @WebTestImplementation
    private MNPackageTestImplementations packageTestImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs through the MN package API methods";
    }
    
    /**
     * Overrides getMemberNodeIterator() to include only v2 Nodes.
     */
    @Override
    protected Iterator<Node> getMemberNodeIterator() {
        return getV2MemberNodeIterator();
    }
    
    @Before 
    public void setup() {
        packageTestImpl = new MNPackageTestImplementations(this);
    }

    @Override
    @Test
    public void testGetPackage_NotAuthorized() {
        packageTestImpl.testGetPackage_NotAuthorized(getMemberNodeIterator(), "v2");
    }

    @Override
    @Test
    public void testGetPackage_NotFound() {
        packageTestImpl.testGetPackage_NotFound(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testGetPackage_UnsupportedType() {
        packageTestImpl.testGetPackage_UnsupportedType(getMemberNodeIterator(), "v2");
    }
    
    @Override
    @Test
    public void testGetPackage() {
        packageTestImpl.testGetPackage(getMemberNodeIterator(), "v2");
    }
}
