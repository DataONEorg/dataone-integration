package org.dataone.service.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.dataone.service.types.v2.Log;
import org.dataone.service.types.v2.LogEntry;
import org.dataone.service.types.v2.Node;
import org.dataone.service.types.v2.NodeList;
import org.dataone.service.types.v2.ObjectFormat;
import org.dataone.service.types.v2.ObjectFormatList;
import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;

/**
 * Handles conversion of types that cannot be directly converted
 * by {@link TypeMarshaller} (most likely due to being composite
 * objects, containing further d1 types which also need conversion).
 * Makes use of {@link TypeMarshaller}.
 * 
 * @author Andrei
 */
public class TypeConverter {


    public static Log convertLog(org.dataone.service.types.v1.Log v1Log) throws InstantiationException,
            IllegalAccessException, InvocationTargetException, JiBXException, IOException {
        Log v2Log = new Log();
        if (v1Log.getLogEntryList() != null && v1Log.getLogEntryList().size() > 0)
            for (org.dataone.service.types.v1.LogEntry v1entry : v1Log.getLogEntryList()) {
                LogEntry v2LogEntry = TypeMarshaller.convertTypeFromType(v1entry, LogEntry.class);
                v2Log.addLogEntry(v2LogEntry);
            }
        
        v2Log.setStart(v1Log.getStart());
        v2Log.setCount(v1Log.getCount());
        v2Log.setTotal(v1Log.getTotal());
        return v2Log;
    }

    public static org.dataone.service.types.v1.Log convertLog(Log v2Log) throws InstantiationException,
            IllegalAccessException, InvocationTargetException, JiBXException, IOException {
        org.dataone.service.types.v1.Log v1Log = new org.dataone.service.types.v1.Log();
        if (v2Log.getLogEntryList() != null && v2Log.getLogEntryList().size() > 0)
            for (LogEntry v2entry : v2Log.getLogEntryList()) {
                org.dataone.service.types.v1.LogEntry v1LogEntry = TypeMarshaller.convertTypeFromType(v2entry, org.dataone.service.types.v1.LogEntry.class);
                v1Log.addLogEntry(v1LogEntry);
            }

        v1Log.setStart(v2Log.getStart());
        v1Log.setCount(v2Log.getCount());
        v1Log.setTotal(v2Log.getTotal());
        return v1Log;
    }

    public static ObjectFormatList convertObjectFormatList(org.dataone.service.types.v1.ObjectFormatList v1FormatList)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            JiBXException, IOException {
        ObjectFormatList v2FormatList = new ObjectFormatList();
        List<org.dataone.service.types.v1.ObjectFormat> innerList = v1FormatList
                .getObjectFormatList();
        if (innerList != null && innerList.size() > 0)
            for (org.dataone.service.types.v1.ObjectFormat v1Format : innerList) {
                ObjectFormat v2ObjectFormat = TypeMarshaller.convertTypeFromType(v1Format,
                        ObjectFormat.class);
                v2FormatList.addObjectFormat(v2ObjectFormat);
            }
        
        v2FormatList.setStart(v1FormatList.getStart());
        v2FormatList.setCount(v1FormatList.getCount());
        v2FormatList.setTotal(v1FormatList.getTotal());
        return v2FormatList;
    }
    
    public static org.dataone.service.types.v1.ObjectFormatList convertObjectFormatList(
            ObjectFormatList v2FormatList) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, JiBXException, IOException {
        org.dataone.service.types.v1.ObjectFormatList v1FormatList = new org.dataone.service.types.v1.ObjectFormatList();
        List<ObjectFormat> innerList = v2FormatList.getObjectFormatList();
        if (innerList != null && innerList.size() > 0)
            for (ObjectFormat v2Format : innerList) {
                org.dataone.service.types.v1.ObjectFormat v1ObjectFormat = TypeMarshaller
                        .convertTypeFromType(v2Format, org.dataone.service.types.v1.ObjectFormat.class);
                v1FormatList.addObjectFormat(v1ObjectFormat);
            }

        v1FormatList.setStart(v2FormatList.getStart());
        v1FormatList.setCount(v2FormatList.getCount());
        v1FormatList.setTotal(v2FormatList.getTotal());
        return v1FormatList;
    }

    public static NodeList convertNodeList(org.dataone.service.types.v1.NodeList v1NodeList)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            JiBXException, IOException {
        NodeList v2NodeList = TypeMarshaller.convertTypeFromType(v1NodeList, NodeList.class);
        List<org.dataone.service.types.v1.Node> innerList = v1NodeList.getNodeList();
        if (innerList != null && innerList.size() > 0) {
            v2NodeList.clearNodeList();
            for (org.dataone.service.types.v1.Node v1Node : innerList) {
                org.dataone.service.types.v2.Node v2Node = TypeMarshaller.convertTypeFromType(
                        v1Node, org.dataone.service.types.v2.Node.class);
                v2NodeList.addNode(v2Node);
            }
        }
        return v2NodeList;
    }
    
    public static org.dataone.service.types.v1.NodeList convertNodeList(NodeList v2NodeList)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            JiBXException, IOException {
        org.dataone.service.types.v1.NodeList v1NodeList = TypeMarshaller.convertTypeFromType(v2NodeList, org.dataone.service.types.v1.NodeList.class);
        List<Node> innerList = v2NodeList.getNodeList();
        if (innerList != null && innerList.size() > 0) {
            v1NodeList.clearNodeList();
            for (Node v2Node : innerList) {
                org.dataone.service.types.v1.Node v1Node = TypeMarshaller.convertTypeFromType(
                        v2Node, org.dataone.service.types.v1.Node.class);
                v1NodeList.addNode(v1Node);
            }
        }
        return v1NodeList;
    }
}
