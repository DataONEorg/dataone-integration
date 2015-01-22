package org.dataone.integration.it.testImplementations;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.dataone.client.v1.types.D1TypeBuilder;
import org.dataone.client.v2.formats.ObjectFormatCache;
import org.dataone.integration.ContextAwareTestCaseDataone;
import org.dataone.integration.adapters.CommonCallAdapter;
import org.dataone.integration.it.ContextAwareAdapter;
import org.dataone.ore.ResourceMapFactory;
import org.dataone.service.exceptions.BaseException;
import org.dataone.service.exceptions.NotAuthorized;
import org.dataone.service.types.v1.Checksum;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.Node;
import org.dataone.service.types.v1.ObjectFormat;
import org.dataone.service.types.v1.ObjectInfo;
import org.dataone.service.types.v1.ObjectList;
import org.dataone.service.types.v1.util.ChecksumUtil;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.types.v2.SystemMetadata;
import org.dataone.service.util.Constants;
import org.dspace.foresite.OREException;
import org.dspace.foresite.OREParserException;

public class ContentIntegrityTestImplementations extends ContextAwareAdapter
{

    public ContentIntegrityTestImplementations(ContextAwareTestCaseDataone catc)
    {
        super(catc);
    }


    public void testResourceMap_Parsing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testResourceMap_Parsing(nodeIterator.next(), version);
    }

    public void testResourceMap_Parsing(Node node, String version)
    {
        CommonCallAdapter cca = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = cca.getNodeBaseServiceUrl();
        printTestHeader("testGetChecksum() vs. node: " + currentUrl);

        try {
            ObjectList ol = cca.listObjects(null, null, null,
                    D1TypeBuilder.buildFormatIdentifier("http://www.openarchives.org/ore/terms"),
                    null, null, null);
            if (ol.sizeObjectInfoList() > 0) {
                ObjectInfo oiResMap = ol.getObjectInfoList().get(0);

                InputStream is = cca.get(null, oiResMap.getIdentifier());
                String resMapContent = IOUtils.toString(is);
                try {
                    ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
                } catch (Exception e) {
                    handleFail(cca.getLatestRequestUrl(), "should be able to parse the serialized resourceMap");
                }
            } else {
                handleFail(cca.getLatestRequestUrl(),"No resource maps " +
                        "(formatId = 'http://www.openarchives.org/ore/terms' " +
                        "returned from listObjects.  Cannot test.");
            }
        }
        catch (BaseException e) {
            handleFail(cca.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }


    public void testResourceMap_Checksum_Size_Consistency(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testContent_Checksum_Size_Consistency("RESOURCE", nodeIterator.next(), version);
    }



    public void testMetadata_Checksum_Size_Consistency(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testContent_Checksum_Size_Consistency("METADATA", nodeIterator.next(), version);
    }

    /**
     * Test to compare the checksum and size in the ObjectInfo match what is in
     * systemMetadata, and matches what is recalculated when retrieving the object.
     */
    protected void testContent_Checksum_Size_Consistency(String formatType, Node node, String version)
    {
        StringBuffer formatsChecked = new StringBuffer("Formats Checked:");

        CommonCallAdapter cca = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = cca.getNodeBaseServiceUrl();
        printTestHeader("testContent_Checksum_Size_Consistency(" + formatType +
                ") vs. node: " + currentUrl);
        boolean foundOne = false;
        try {
            ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
            for(ObjectFormat of : ofl.getObjectFormatList()) {
                if (of.getFormatType().equals(formatType)) {
                    formatsChecked.append("\n" + of.getFormatId().getValue());
                    log.info("   looking for objects with format: " + of.getFormatId().getValue());

                    ObjectList ol = cca.listObjects(null, null, null,
                            of.getFormatId(), null, null, null);

                    //TODO: listObjects returns ids for things that are not readable...
                    if (ol.sizeObjectInfoList() > 0) {
                        foundOne = true;


                        log.info(ol.sizeObjectInfoList() + " items found of type " +
                                of.getFormatId().getValue());

                        ObjectInfo oi = ol.getObjectInfoList().get(0);
                        SystemMetadata smd = cca.getSystemMetadata(null, oi.getIdentifier());
                        checkEquals(cca.getLatestRequestUrl(),"objectInfo checksum should equal that of sysMeta",
                                oi.getChecksum().getAlgorithm() + " : " + oi.getChecksum().getValue(),
                                smd.getChecksum().getAlgorithm() + " : " + smd.getChecksum().getValue());
                        checkEquals(cca.getLatestRequestUrl(),"objectInfo size should equal that of sysMeta",
                                oi.getSize().toString(),
                                smd.getSize().toString());


                        InputStream is = cca.get(null, oi.getIdentifier());
                        //calculate the checksum and length
                        CountingInputStream cis = new CountingInputStream(is);
                        Checksum calcCS = ChecksumUtil.checksum(cis,oi.getChecksum().getAlgorithm());
                        long calcSize = cis.getByteCount();

                        checkEquals(cca.getLatestRequestUrl(),"calculated checksum should equal that of sysMeta",
                                calcCS.getValue(),
                                smd.getChecksum().getValue());
                        checkEquals(cca.getLatestRequestUrl(),"calculated size should equal that of sysMeta",
                                String.valueOf(calcSize),
                                smd.getSize().toString());

                        //                          break;
                    }  // found at least one of that type
                }  // formatType matches
            } // for each type
        }
        catch (BaseException e) {
            handleFail(cca.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }

        if (!foundOne)
            handleFail(cca.getLatestRequestUrl(),"No objects of formatType " +
                    formatType + "returned from listObjects.  Cannot test.\n" +
                    formatsChecked.toString());

    }


    public void testResourceMapParsing(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testResourceMapParsing(nodeIterator.next(), version);
    }

    public void testResourceMapParsing(Node node, String version)
    {
        CommonCallAdapter mn = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testResourceMapParsing() vs. node: " + currentUrl);

        StringBuffer formatsChecked = new StringBuffer("Formats Checked:");

        try {
            boolean foundOne = false;
            ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
            for(ObjectFormat of : ofl.getObjectFormatList())
            {
                if (of.getFormatType().equals("RESOURCE")) {
                    formatsChecked.append("\n" + of.getFormatId().getValue());

                    ObjectList ol = mn.listObjects(null, null, null, of.getFormatId(),null, null, 20);
                    if (ol.sizeObjectInfoList() > 0) {
                        log.info(ol.sizeObjectInfoList() + " items found of type " +
                                of.getFormatId().getValue());
                        for (ObjectInfo oi : ol.getObjectInfoList()) {
                            String resMapContent;
                            try {
                                InputStream is = mn.get(null, oi.getIdentifier());
                                foundOne = true;
                                log.info("Found public resource map: " + oi.getIdentifier().getValue());
                                resMapContent = IOUtils.toString(is);
                                if (resMapContent != null) {
                                    ResourceMapFactory.getInstance().parseResourceMap(resMapContent);
                                } else {
                                    handleFail(mn.getLatestRequestUrl(),"got null content from the get request");
                                }
                            } catch (NotAuthorized e) {
                                ; // comes from the mn.get(), will keep trying...
                            } catch (NullPointerException npe) {
                                handleFail(mn.getLatestRequestUrl(),
                                        "Got NPE exception from the parsing library, which means that the " +
                                                "content could not be parsed into a ResourceMap.  One known cause " +
                                                "is relative resource URIs used for the resource map object, the aggregated resources," +
                                        " or the aggregation itself." );
                            } catch (Exception e) {
                                handleFail(mn.getLatestRequestUrl(),
                                        "Should be able to parse the serialized resourceMap.  Got exception: " +
                                                e.getClass().getSimpleName() + ": " +
                                                e.getMessage() +
                                                "at line number " + e.getStackTrace()[0].getLineNumber());
                            }
                        }
                    }
                }
            }
            if (!foundOne) {
                handleFail(mn.getLatestRequestUrl(),"No public resource maps " +
                        "returned from listObjects.  Cannot test.\n" +
                        formatsChecked.toString());
            }
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage() +
                    "at line number " + e.getStackTrace()[0].getLineNumber());
        }

    }


    public void testResourceMap_ResolveURL(Iterator<Node> nodeIterator, String version) {
        while (nodeIterator.hasNext())
            testResourceMap_ResolveURL(nodeIterator.next(), version);
    }

    public void testResourceMap_ResolveURL(Node node, String version)
    {
        CommonCallAdapter mn = new CommonCallAdapter(getSession(Constants.SUBJECT_PUBLIC), node, version);
        String currentUrl = mn.getNodeBaseServiceUrl();
        printTestHeader("testResourceMapParsing_ResolveURL() vs. node: " + currentUrl);

        StringBuffer formatsChecked = new StringBuffer("Formats Checked:");

        try {
            boolean foundOne = false;
            ObjectFormatList ofl = ObjectFormatCache.getInstance().listFormats();
            for(ObjectFormat of : ofl.getObjectFormatList())
            {
                if (of.getFormatType().equals("RESOURCE")) {
                    formatsChecked.append("\n" + of.getFormatId().getValue());

                    ObjectList ol = mn.listObjects(null, null, null, of.getFormatId(),null, null, 20);
                    if (ol.sizeObjectInfoList() > 0) {
                        log.info(ol.sizeObjectInfoList() + " items found of type " +
                                of.getFormatId().getValue());
                        for (ObjectInfo oi : ol.getObjectInfoList()) {
                            String resMapContent;
                            try {
                                InputStream is = mn.get(null, oi.getIdentifier());
                                foundOne = true;
                                log.info("Found public resource map: " + oi.getIdentifier().getValue());
                                resMapContent = IOUtils.toString(is);
                                resourceMapChecker(mn, oi.getIdentifier(), resMapContent);


                            } catch (NotAuthorized e) {
                                ; // comes from the mn.get(), will keep trying...
                            } catch (NullPointerException npe) {
                                handleFail(mn.getLatestRequestUrl(),
                                        "Got NPE exception from the parsing library, which means that the " +
                                                "content could not be parsed into a ResourceMap.  One known cause " +
                                                "is relative resource URIs used for the resource map object, the aggregated resources," +
                                        " or the aggregation itself." );} catch (Exception e) {
                                            handleFail(mn.getLatestRequestUrl(),
                                                    "Should be able to parse the serialized resourceMap.  Got exception: " +
                                                            e.getClass().getSimpleName() + ": " +
                                                            e.getMessage());
                                        }
                        }
                    }
                }
            }
            if (!foundOne) {
                handleFail(mn.getLatestRequestUrl(),"No public resource maps " +
                        "returned from listObjects.  Cannot test.\n" +
                        formatsChecked.toString());
            }
        }
        catch (BaseException e) {
            handleFail(mn.getLatestRequestUrl(), e.getClass().getSimpleName() + ": " +
                    e.getDetail_code() + ":: " + e.getDescription());
        }
        catch(Exception e) {
            e.printStackTrace();
            handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
        }
    }


    private void resourceMapChecker(CommonCallAdapter cca, Identifier packageId, String resMapContent)
    throws UnsupportedEncodingException, OREException, URISyntaxException, OREParserException
    {
        Map<Identifier, Map<Identifier, List<Identifier>>> rm =
                ResourceMapFactory.getInstance().parseResourceMap(resMapContent);

        //     checkTrue(mn.getLatestRequestUrl(),
        //         "packageId matches packageId used to call", rm.containsKey(packageId));

        if (rm != null) {
            Iterator<Identifier> it = rm.keySet().iterator();

            while (it.hasNext()) {
                Identifier pp = it.next();
                System.out.println("package: " + pp.getValue());
            }

            Map<Identifier, List<Identifier>> agg = rm.get(rm.keySet().iterator().next());
            Iterator<Identifier> itt  = agg.keySet().iterator();
            while (itt.hasNext()) {
                Identifier docs = itt.next();
                System.out.println("md: " + docs.getValue());
                //checkTrue("the identifier should start with https://cn.dataone.org/cn/v1/resolve","",true);
                List<Identifier> docd = agg.get(docs);
                for (Identifier id: docd) {
                    System.out.println("data: " + id.getValue());
                }
            }
        } else {
            handleFail("","parseResourceMap returned null");
        }
    }

}

