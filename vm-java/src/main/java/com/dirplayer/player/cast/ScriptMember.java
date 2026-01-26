package com.dirplayer.player.cast;

import com.dirplayer.director.ScriptType;

/**
 * Script cast member data.
 * Port of Rust ScriptMember struct.
 */
public class ScriptMember {
    public int scriptId;
    public ScriptType scriptType;
    public String name;

    public ScriptMember() {
        this.scriptId = 0;
        this.scriptType = ScriptType.Movie;
        this.name = "";
    }

    public ScriptMember(int scriptId, ScriptType scriptType, String name) {
        this.scriptId = scriptId;
        this.scriptType = scriptType;
        this.name = name;
    }

    public ScriptMember copy() {
        return new ScriptMember(scriptId, scriptType, name);
    }
}
