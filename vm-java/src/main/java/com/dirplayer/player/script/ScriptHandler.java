package com.dirplayer.player.script;

import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.player.CastMemberRef;

/**
 * Handler reference combining script and handler info.
 * Port of Rust ScriptHandler struct.
 */
public class ScriptHandler {
    public CastMemberRef scriptRef;
    public String handlerName;
    public HandlerDef handlerDef;
    public Script script;

    public ScriptHandler() {
        this.scriptRef = new CastMemberRef(0, 0);
        this.handlerName = "";
        this.handlerDef = null;
        this.script = null;
    }

    public ScriptHandler(CastMemberRef scriptRef, String handlerName, HandlerDef handlerDef, Script script) {
        this.scriptRef = scriptRef;
        this.handlerName = handlerName;
        this.handlerDef = handlerDef;
        this.script = script;
    }

    public boolean isValid() {
        return handlerDef != null;
    }

    public String getName() {
        return handlerName;
    }

    public HandlerDef getHandlerDef() {
        return handlerDef;
    }

    public Script getScript() {
        return script;
    }

    public CastMemberRef getScriptRef() {
        return scriptRef;
    }
}
