package org.dataone.integration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.NodeReference;
import org.dataone.service.types.v1.ObjectList;

public class TestObjectCache {

    /** a key-value map organized by NodeReference */ 
    protected static Map<NodeReference, Map<String, Object>> cachedItems;
   
    
    private TestObjectCache() {
        super();
        cachedItems = new HashMap<>();
    }
   
    private static class TestObjectCacheSingletonHolder {
        public static final TestObjectCache INSTANCE = new TestObjectCache();
    }
    
    
    public static TestObjectCache getInstance() {
        return TestObjectCacheSingletonHolder.INSTANCE;
    }
    
    
    public void logCacheUtilization(Log log) {
        
        System.out.printf("%32s  %-25s %s\n", "nodeId", "key", "value");
        System.out.println("=============================================================================================================");
        for(Entry<NodeReference, Map<String,Object>> n : cachedItems.entrySet()) {
            for(Entry<String,Object> pair : n.getValue().entrySet()) {
                if (pair.getValue() instanceof Identifier) {
                    System.out.printf("%32s  %-25s %s\n", n.getKey().getValue(),pair.getKey(),((Identifier)pair.getValue()).getValue());
                } else {
                    System.out.printf("%32s  %-25s %s\n", n.getKey().getValue(),pair.getKey(),pair.getValue());
                }
            }
            System.out.println("=============================================================================================================");
        }
    }

    public void cacheObjectList(NodeReference nodeId, ObjectList ol) {
        Map<String,Object> nodeMap = null;
        if (!cachedItems.containsKey(nodeId)) {
            nodeMap = new HashMap<>();
            cachedItems.put(nodeId, nodeMap);
        }
        cachedItems.get(nodeId).put("objectList", ol);
        cachedItems.get(nodeId).put("objectList_cacheDate", new Date());
        cachedItems.get(nodeId).put("objectList_hits", new Integer(0));
    }
    
    public ObjectList getCachedObjectList(NodeReference nodeId) {
        if (cachedItems.containsKey(nodeId)) {
            cachedItems.get(nodeId).put("objectList_hits", 1 + (Integer)cachedItems.get(nodeId).get("objectList_hits"));
            return (ObjectList)cachedItems.get(nodeId).get("objectList");
        } else {
            return null;
        }
    }
    
    public boolean hasCachedObjectList(NodeReference nodeId) {
        if (cachedItems.containsKey(nodeId) && cachedItems.get(nodeId).containsKey("objectList")) {
            return true;
        }
        return false;
    }
    
    public void clearObjectList(NodeReference nodeId) {
        try {
            cachedItems.get(nodeId).remove("objectList");
        } 
        catch (NullPointerException e) { }
    }

    
    public void cachePublicIdentifier(NodeReference nodeId, Identifier id) {
        Map<String,Object> nodeMap = null;
        if (!cachedItems.containsKey(nodeId)) {
            nodeMap = new HashMap<>();
            cachedItems.put(nodeId, nodeMap);
        }
        cachedItems.get(nodeId).put("publicObject", id);
        cachedItems.get(nodeId).put("publicObject_cacheDate", new Date());
        cachedItems.get(nodeId).put("publicObject_hits", new Integer(0));
    }
    
    public Identifier getCachedPublicIdentifier(NodeReference nodeId) {
        if (cachedItems.containsKey(nodeId)) {
            cachedItems.get(nodeId).put("publicObject_hits", 1 + (Integer)cachedItems.get(nodeId).get("publicObject_hits"));
            return (Identifier)cachedItems.get(nodeId).get("publicObject");
        } else {
            return null;
        }
    }
    
    public boolean hasCachedPublicIdentifier(NodeReference nodeId) {
        if (cachedItems.containsKey(nodeId) && cachedItems.get(nodeId).containsKey("publicObject")) {
            return true;
        }
        return false;
    }
    
}
