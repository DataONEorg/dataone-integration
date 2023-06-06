package org.dataone.integration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.dataone.service.types.v1.Identifier;
import org.dataone.service.types.v1.ObjectList;

public class TestObjectCache {

    /** a key-value map organized by Node (string representation) */ 
    protected static Map<String, Map<String, Object>> cachedItems;
   
    
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
        for(Entry<String, Map<String,Object>> n : cachedItems.entrySet()) {
            for(Entry<String,Object> pair : n.getValue().entrySet()) {
                if (pair.getValue() instanceof Identifier) {
                    System.out.printf("%32s  %-25s %s\n", n.getKey(),pair.getKey(),((Identifier)pair.getValue()).getValue());
                } else {
                    System.out.printf("%32s  %-25s %s\n", n.getKey(),pair.getKey(),pair.getValue());
                }
            }
            System.out.println("=============================================================================================================");
        }
    }

    public void cacheObjectList(String nodeId, ObjectList ol) {
        Map<String,Object> nodeMap = null;
        if (!cachedItems.containsKey(nodeId)) {
            nodeMap = new HashMap<>();
            cachedItems.put(nodeId, nodeMap);
        }
        cachedItems.get(nodeId).put("objectList", ol);
        cachedItems.get(nodeId).put("objectList_cacheDate", new Date());
        cachedItems.get(nodeId).put("objectList_hits", new Integer(0));
    }
    
    public ObjectList getCachedObjectList(String nodeId) {
        if (cachedItems.containsKey(nodeId)) {
            cachedItems.get(nodeId).put("objectList_hits", 1 + (Integer)cachedItems.get(nodeId).get("objectList_hits"));
            return (ObjectList)cachedItems.get(nodeId).get("objectList");
        } else {
            return null;
        }
    }
    
    public boolean hasCachedObjectList(String nodeId) {
        if (cachedItems.containsKey(nodeId) && cachedItems.get(nodeId).containsKey("objectList")) {
            return true;
        }
        return false;
    }
    
    /**
     * returns the cache date if one was set or new Date(0) - beginning of epoch
     * @param nodeId
     * @return
     */
    public Date getCachedObjectListCacheDate(String nodeId) {

        if (hasCachedObjectList(nodeId)) {
            return (Date) cachedItems.get(nodeId).get("objectList_cacheDate");
        }
        return new Date(0);
    }
    
    public void clearObjectList(String nodeId) {
        try {
            cachedItems.get(nodeId).remove("objectList");
        } 
        catch (NullPointerException e) { }
    }

    
    public void cachePublicIdentifier(String nodeId, Identifier id) {
        Map<String,Object> nodeMap = null;
        if (!cachedItems.containsKey(nodeId)) {
            nodeMap = new HashMap<>();
            cachedItems.put(nodeId, nodeMap);
        }
        cachedItems.get(nodeId).put("publicObject", id);
        cachedItems.get(nodeId).put("publicObject_cacheDate", new Date());
        cachedItems.get(nodeId).put("publicObject_hits", new Integer(0));
    }
    
    public Identifier getCachedPublicIdentifier(String nodeId) {
        if (cachedItems.containsKey(nodeId)) {
            cachedItems.get(nodeId).put("publicObject_hits", 1 + (Integer)cachedItems.get(nodeId).get("publicObject_hits"));
            return (Identifier)cachedItems.get(nodeId).get("publicObject");
        } else {
            return null;
        }
    }
    
    public boolean hasCachedPublicIdentifier(String nodeId) {
        if (cachedItems.containsKey(nodeId)  && cachedItems.get(nodeId).containsKey("publicObject") ) {
            return true;
        }   
        return false;
    }
      
    /**
     * returns the cache date if one was set or new Date(0) - beginning of epoch
     * @param nodeId
     * @return
     */
    public Date getCachedPublicIdentifierCacheDate(String nodeId) {

        if (hasCachedPublicIdentifier(nodeId)) {
            return (Date) cachedItems.get(nodeId).get("publicObject_cacheDate");
        }
        return new Date(0);
    }
    
    public void clearPublicIdentifier(String nodeId) {
        try {
            cachedItems.get(nodeId).remove("publicObject");
            cachedItems.get(nodeId).remove("publicObject_hits");
            cachedItems.get(nodeId).remove("publicObject_cacheDate");
        }
        catch (NullPointerException e) { }
    }
        
    
}
