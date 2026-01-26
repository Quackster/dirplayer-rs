package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;

import java.util.HashMap;
import java.util.Map;

/**
 * Allocator for Datum values with reference counting.
 * Port of Rust DatumAllocator struct.
 */
public class DatumAllocator {
    private Map<Integer, Datum> datums;
    private Map<Integer, ScriptInstance> scriptInstances;
    private int nextDatumId;
    private int nextScriptInstanceId;

    public DatumAllocator() {
        this.datums = new HashMap<>();
        this.scriptInstances = new HashMap<>();
        this.nextDatumId = 1;
        this.nextScriptInstanceId = 1;

        // Pre-allocate special values
        datums.put(0, Datum.VOID);  // ID 0 = Void
    }

    public int alloc(Datum datum) {
        int id = nextDatumId++;
        datums.put(id, datum);
        return id;
    }

    public Datum get(int id) {
        return datums.get(id);
    }

    public void set(int id, Datum datum) {
        datums.put(id, datum);
    }

    public void free(int id) {
        datums.remove(id);
    }

    public int allocScriptInstance(ScriptInstance instance) {
        int id = nextScriptInstanceId++;
        scriptInstances.put(id, instance);
        return id;
    }

    public ScriptInstance getScriptInstance(int id) {
        return scriptInstances.get(id);
    }

    public void freeScriptInstance(int id) {
        scriptInstances.remove(id);
    }

    public void reset() {
        datums.clear();
        scriptInstances.clear();
        nextDatumId = 1;
        nextScriptInstanceId = 1;
        datums.put(0, Datum.VOID);
    }

    /**
     * Script instance for behavior/parent scripts.
     */
    public static class ScriptInstance {
        public int id;
        public CastMemberRef scriptRef;
        public Map<String, Integer> properties;  // Property name -> DatumRef
        public int ancestor;  // DatumRef to ancestor instance

        public ScriptInstance() {
            this.properties = new HashMap<>();
            this.ancestor = 0;
        }

        public void setProperty(String name, int datumRef) {
            properties.put(name.toLowerCase(), datumRef);
        }

        public Integer getProperty(String name) {
            return properties.get(name.toLowerCase());
        }

        public boolean hasProperty(String name) {
            return properties.containsKey(name.toLowerCase());
        }
    }
}
