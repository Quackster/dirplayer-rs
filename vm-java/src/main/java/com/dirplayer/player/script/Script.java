package com.dirplayer.player.script;

import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.director.chunks.ScriptChunk;
import com.dirplayer.director.ScriptType;
import com.dirplayer.player.CastMemberRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a compiled Lingo script.
 * Port of Rust Script struct.
 */
public class Script {
    public CastMemberRef memberRef;
    public String name;
    public ScriptChunk chunk;
    public ScriptType scriptType;
    public Map<String, HandlerDef> handlers;
    public List<String> handlerNames;
    public Map<String, Integer> properties;  // Property name -> DatumRef ID

    public Script() {
        this.memberRef = new CastMemberRef(0, 0);
        this.name = "";
        this.chunk = null;
        this.scriptType = ScriptType.Unknown;
        this.handlers = new HashMap<>();
        this.handlerNames = new ArrayList<>();
        this.properties = new HashMap<>();
    }

    public Script(CastMemberRef memberRef, String name, ScriptChunk chunk, ScriptType scriptType) {
        this();
        this.memberRef = memberRef;
        this.name = name;
        this.chunk = chunk;
        this.scriptType = scriptType;
    }

    /**
     * Get handler by name (case-insensitive).
     */
    public HandlerDef getOwnHandler(String name) {
        return handlers.get(name.toLowerCase());
    }

    /**
     * Get handler by name ID.
     */
    public HandlerDef getOwnHandlerByNameId(int nameId) {
        for (HandlerDef handler : handlers.values()) {
            if (handler.nameId == nameId) {
                return handler;
            }
        }
        return null;
    }

    /**
     * Get handler reference at index.
     */
    public ScriptHandlerRef getOwnHandlerRefAt(int index) {
        if (index >= 0 && index < handlerNames.size()) {
            return new ScriptHandlerRef(memberRef, handlerNames.get(index));
        }
        return null;
    }

    /**
     * Get handler reference by name.
     */
    public ScriptHandlerRef getOwnHandlerRef(String name) {
        if (handlers.containsKey(name.toLowerCase())) {
            return new ScriptHandlerRef(memberRef, name);
        }
        return null;
    }

    /**
     * Check if script has a handler.
     */
    public boolean hasHandler(String name) {
        return handlers.containsKey(name.toLowerCase());
    }

    public ScriptType getScriptType() {
        return scriptType;
    }

    /**
     * Script handler reference (member ref + handler name).
     */
    public static class ScriptHandlerRef {
        public final CastMemberRef memberRef;
        public final String handlerName;

        public ScriptHandlerRef(CastMemberRef memberRef, String handlerName) {
            this.memberRef = memberRef;
            this.handlerName = handlerName;
        }
    }
}
