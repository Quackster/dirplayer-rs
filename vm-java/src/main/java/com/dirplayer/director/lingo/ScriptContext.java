package com.dirplayer.director.lingo;

import com.dirplayer.director.chunks.ScriptChunk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context for Lingo scripts, holding names and script chunks.
 * Port of Rust ScriptContext struct.
 */
public class ScriptContext {
    public List<String> names;
    public Map<Integer, ScriptChunk> scripts;

    public ScriptContext() {
        this.names = new ArrayList<>();
        this.scripts = new HashMap<>();
    }

    public String getName(int index) {
        if (index >= 0 && index < names.size()) {
            return names.get(index);
        }
        return null;
    }

    public void addName(String name) {
        names.add(name);
    }

    public void addScript(int id, ScriptChunk chunk) {
        scripts.put(id, chunk);
    }

    public ScriptChunk getScript(int id) {
        return scripts.get(id);
    }
}
