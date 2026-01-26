package com.dirplayer.player.script;

import com.dirplayer.director.lingo.ScriptContext;
import com.dirplayer.player.CastMemberRef;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an instance of a script.
 * Port of Rust ScriptInstance struct.
 */
public class ScriptInstance {
    public int instanceId;
    public CastMemberRef script;
    public ScriptInstanceRef ancestor;
    public Map<String, Integer> properties;  // Property name -> DatumRef ID
    public boolean beginSpriteCalled;

    public ScriptInstance() {
        this.instanceId = 0;
        this.script = new CastMemberRef(0, 0);
        this.ancestor = null;
        this.properties = new HashMap<>();
        this.beginSpriteCalled = false;
    }

    public ScriptInstance(int instanceId, CastMemberRef scriptRef, Script scriptDef, ScriptContext lctx) {
        this();
        this.instanceId = instanceId;
        this.script = scriptRef;

        // Initialize properties from script definition
        if (scriptDef != null && scriptDef.chunk != null && lctx != null) {
            for (int propNameId : scriptDef.chunk.propertyNameIds) {
                String propName = lctx.names.size() > propNameId
                    ? lctx.names.get(propNameId)
                    : "prop_" + propNameId;
                properties.put(propName, 0);  // 0 represents DatumRef.Void
            }
        }
    }

    /**
     * Get a property value.
     */
    public Integer getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Set a property value.
     */
    public void setProperty(String name, Integer datumRef) {
        properties.put(name, datumRef);
    }

    /**
     * Check if this instance has a property.
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }
}
