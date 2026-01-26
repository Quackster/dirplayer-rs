package com.dirplayer.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Script execution scope - holds stack and local variables.
 * Port of Rust ScriptScope struct.
 */
public class ScriptScope {
    public Stack<Integer> stack;
    public Map<String, Integer> locals;
    public List<Integer> args;
    public int receiverRef;
    public int scriptRef;
    public int handlerIndex;
    public int bytecodeIndex;
    public boolean isLoopScope;
    public int loopStartIndex;

    public ScriptScope() {
        this.stack = new Stack<>();
        this.locals = new HashMap<>();
        this.args = new ArrayList<>();
        this.receiverRef = 0;
        this.scriptRef = 0;
        this.handlerIndex = 0;
        this.bytecodeIndex = 0;
        this.isLoopScope = false;
        this.loopStartIndex = 0;
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
