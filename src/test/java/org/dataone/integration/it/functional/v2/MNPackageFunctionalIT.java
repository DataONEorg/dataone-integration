package org.dataone.integration.it.functional.v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.MNCallAdapter;
import org.dataone.integration.it.testImplementations.MNPackageFunctionalTestImplementations;
import org.dataone.service.types.v1.Node;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNPackageFunctionalIT extends ContextAwareTestCaseDataone {

    private MNPackageFunctionalTestImplementations pkgImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs MN package API methods and checks results for correctness.";
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
        pkgImpl = new MNPackageFunctionalTestImplementations(this);
    }
    
    @Test
    public void testGetPackage_EscapeChars() { 
        pkgImpl.testGetPackage_EscapeChars(getMemberNodeIterator(), "v2");
    }
    
    @Test
    public void testGetPackage_Zip() { 
        pkgImpl.testGetPackage_Zip(getMemberNodeIterator(), "v2");
    }
    
    @Test
    @Ignore("We're not yet trying to support this format")
    public void testGetPackage_Gzip() {
        pkgImpl.testGetPackage_Gzip(getMemberNodeIterator(), "v2");
    }
    
    @Test
    @Ignore("We're not yet trying to support this format")
    public void testGetPackage_Bzip2() {
        pkgImpl.testGetPackage_Bzip2(getMemberNodeIterator(), "v2");
    }

    @Test
    @Ignore("We're not yet trying to support this format")
    public void testGetPackage_Tar() {
        pkgImpl.testGetPackage_Tar(getMemberNodeIterator(), "v2");
    }
    
    @Test
    @Ignore("We're not yet trying to support this format")
    public void testGetPackage_Rar() {
        pkgImpl.testGetPackage_Rar(getMemberNodeIterator(), "v2");
    }
}
