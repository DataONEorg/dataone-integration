package org.dataone.integration.it.functional.v2;

import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.it.testImplementations.MNPackageFunctionalTestImplementations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MNPackageFunctionalIT extends ContextAwareTestCaseDataone {

    private MNPackageFunctionalTestImplementations pkgImpl;
    
    @Override
    protected String getTestDescription() {
        return "Test Case that runs MN package API methods and checks results for correctness.";
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
