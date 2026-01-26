package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.script.ScriptInstance;
import com.dirplayer.player.script.ScriptInstanceRef;

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
        instance.instanceId = id;
        scriptInstances.put(id, instance);
        return id;
    }

    public ScriptInstance getScriptInstance(int id) {
        return scriptInstances.get(id);
    }

    /**
     * Get script instance by reference.
     */
    public ScriptInstance getScriptInstance(ScriptInstanceRef ref) {
        if (ref == null) return null;
        return scriptInstances.get(ref.instanceId);
    }

    /**
     * Get mutable script instance by reference.
     */
    public ScriptInstance getScriptInstanceMut(ScriptInstanceRef ref) {
        if (ref == null) return null;
        return scriptInstances.get(ref.instanceId);
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
}
