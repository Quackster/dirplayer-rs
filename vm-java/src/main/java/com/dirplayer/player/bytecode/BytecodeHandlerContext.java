package com.dirplayer.player.bytecode;

/**
 * Context for bytecode handler execution.
 * Port of Rust BytecodeHandlerContext struct.
 */
public class BytecodeHandlerContext {
    public int scopeRef;
    public int bytecodeIndex;
    public int handlerIndex;
    public int scriptCastLib;
    public int scriptCastMember;

    public BytecodeHandlerContext() {
    }

    public BytecodeHandlerContext(int scopeRef, int bytecodeIndex, int handlerIndex) {
        this.scopeRef = scopeRef;
        this.bytecodeIndex = bytecodeIndex;
        this.handlerIndex = handlerIndex;
        this.scriptCastLib = 0;
        this.scriptCastMember = 0;
    }

    public BytecodeHandlerContext(int scopeRef, int bytecodeIndex, int handlerIndex, int scriptCastLib, int scriptCastMember) {
        this.scopeRef = scopeRef;
        this.bytecodeIndex = bytecodeIndex;
        this.handlerIndex = handlerIndex;
        this.scriptCastLib = scriptCastLib;
        this.scriptCastMember = scriptCastMember;
    }

    public BytecodeHandlerContext copy() {
        return new BytecodeHandlerContext(scopeRef, bytecodeIndex, handlerIndex, scriptCastLib, scriptCastMember);
    }
}
