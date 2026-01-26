package com.dirplayer.player;

import com.dirplayer.player.script.ScriptInstanceRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Script execution scope - holds stack and local variables.
 * Port of Rust Scope struct.
 */
public class ScriptScope {
    public int scopeRef;
    public CastMemberRef scriptMemberRef;
    public ScriptInstanceRef receiver;
    public int handlerNameId;
    public Stack<Integer> stack;
    public Map<String, Integer> locals;
    public List<Integer> args;
    public int bytecodeIndex;
    public List<Integer> loopReturnIndices;
    public int returnValue;
    public boolean passed;

    public ScriptScope() {
        this.scopeRef = 0;
        this.scriptMemberRef = CastMemberRef.INVALID;
        this.receiver = null;
        this.handlerNameId = 0;
        this.stack = new Stack<>();
        this.locals = new HashMap<>();
        this.args = new ArrayList<>();
        this.bytecodeIndex = 0;
        this.loopReturnIndices = new ArrayList<>();
        this.returnValue = 0;  // DatumRef.Void
        this.passed = false;
    }

    public ScriptScope(int scopeRef) {
        this();
        this.scopeRef = scopeRef;
    }

    public void reset() {
        this.scriptMemberRef = CastMemberRef.INVALID;
        this.receiver = null;
        this.handlerNameId = 0;
        this.args.clear();
        this.bytecodeIndex = 0;
        this.locals.clear();
        this.loopReturnIndices.clear();
        this.returnValue = 0;
        this.stack.clear();
        this.passed = false;
    }

    /**
     * Pop n items from the stack.
     */
    public List<Integer> popN(int n) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n && !stack.isEmpty(); i++) {
            result.add(0, stack.pop()); // Insert at beginning to maintain order
        }
        return result;
    }

    /**
     * Peek at the top of the stack without removing.
     */
    public int peek() {
        return stack.peek();
    }

    /**
     * Get the local variable by name.
     */
    public Integer getLocal(String name) {
        return locals.get(name);
    }

    /**
     * Set the local variable by name.
     */
    public void setLocal(String name, int datumRef) {
        locals.put(name, datumRef);
    }
}
