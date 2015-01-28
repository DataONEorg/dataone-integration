package org.dataone.integration.it.testDefinitions;


public interface CNCoreTestDefinitions {

    public void testCreate();

    public void testCreateData_IdentifierEncoding();

    // TODO found no CN delete test anywhere... (only MN one in MNodeTier3IT)
    // public void testDelete();

    public void testListChecksumAlgorithms();

    public void testListFormats();

    public void testGetFormat();

    public void testGetFormat_bogusFormat();

    public void testListNodes();

    public void testGenerateIdentifier();

    public void testGenerateIdentifier_badScheme();

    public void testReserveIdentifier();

    public void testRegisterSystemMetadata();

    public void testHasReservation();

    public void testHasReservation_noReservation();
    
    public void testGetLogRecords();

    public void testGetLogRecords_Slicing();

    public void testGetLogRecords_dateFiltering();
    
    // TODO found no CN archive test anywhere... (only MN one in MNodeTier3IT)
    // public void testArchive();

    // TODO not found...
    // public void testSetObsoletedBy();

    // TODO not found... 
    // public void testObsoletedBy();
}
