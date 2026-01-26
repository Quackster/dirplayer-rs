package com.dirplayer.player.bytecode;

import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.director.chunks.ScriptChunk;
import com.dirplayer.director.lingo.ScriptContext;

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
    public HandlerDef handler;
    public ScriptChunk script;
    public ScriptContext scriptContext;

    public BytecodeHandlerContext() {
        this.scriptContext = null;
    }

    public BytecodeHandlerContext(int scopeRef, int bytecodeIndex, int handlerIndex) {
        this.scopeRef = scopeRef;
        this.bytecodeIndex = bytecodeIndex;
        this.handlerIndex = handlerIndex;
        this.scriptCastLib = 0;
        this.scriptCastMember = 0;
        this.handler = null;
        this.script = null;
        this.scriptContext = null;
    }

    public BytecodeHandlerContext(int scopeRef, int bytecodeIndex, int handlerIndex, int scriptCastLib, int scriptCastMember) {
        this.scopeRef = scopeRef;
        this.bytecodeIndex = bytecodeIndex;
        this.handlerIndex = handlerIndex;
        this.scriptCastLib = scriptCastLib;
        this.scriptCastMember = scriptCastMember;
        this.handler = null;
        this.script = null;
        this.scriptContext = null;
    }

    public BytecodeHandlerContext(int scopeRef, int bytecodeIndex, int handlerIndex, int scriptCastLib, int scriptCastMember, HandlerDef handler, ScriptChunk script) {
        this.scopeRef = scopeRef;
        this.bytecodeIndex = bytecodeIndex;
        this.handlerIndex = handlerIndex;
        this.scriptCastLib = scriptCastLib;
        this.scriptCastMember = scriptCastMember;
        this.handler = handler;
        this.script = script;
        this.scriptContext = null;
    }

    public BytecodeHandlerContext copy() {
        BytecodeHandlerContext copy = new BytecodeHandlerContext(scopeRef, bytecodeIndex, handlerIndex, scriptCastLib, scriptCastMember);
        copy.handler = this.handler;
        copy.script = this.script;
        copy.scriptContext = this.scriptContext;
        return copy;
    }
}
