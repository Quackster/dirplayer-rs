package com.dirplayer.player;

/**
 * Context for a breakpoint hit during execution.
 * Port of Rust BreakpointContext struct.
 */
public class BreakpointContext {
    public String scriptName;
    public String handlerName;
    public int bytecodeIndex;
    public int scopeIndex;

    public BreakpointContext(String scriptName, String handlerName, int bytecodeIndex, int scopeIndex) {
        this.scriptName = scriptName;
        this.handlerName = handlerName;
        this.bytecodeIndex = bytecodeIndex;
        this.scopeIndex = scopeIndex;
    }
}
