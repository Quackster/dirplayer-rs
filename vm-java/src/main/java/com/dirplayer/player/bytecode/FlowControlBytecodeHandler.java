package com.dirplayer.player.bytecode;

import com.dirplayer.director.chunks.Bytecode;
import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.handlers.HandlerManager;
import java.util.List;

/**
 * Flow control bytecode handlers - return, jump, call.
 * Port of Rust FlowControlBytecodeHandler struct.
 */
public class FlowControlBytecodeHandler {

    public static HandlerExecutionResult ret(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.clear();
        return HandlerExecutionResult.STOP;
    }

    public static HandlerExecutionResult jmpIfZero(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueId = scope.stack.pop();

        Datum datum = player.getDatum(valueId);
        Bytecode bytecode = player.getCtxCurrentBytecode(ctx);
        int position = bytecode.pos;
        int offset = (int) bytecode.obj;

        if (CompareBytecodeHandler.datumIsZero(datum, player)) {
            HandlerDef handler = player.getCurrentHandlerDef(ctx);
            int destPos = position + offset;
            Integer newBytecodeIndex = handler.bytecodeIndexMap.get(destPos);
            if (newBytecodeIndex != null) {
                scope.bytecodeIndex = newBytecodeIndex;
            }
            return HandlerExecutionResult.JUMP;
        } else {
            return HandlerExecutionResult.ADVANCE;
        }
    }

    public static HandlerExecutionResult jmp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        Bytecode bytecode = player.getCtxCurrentBytecode(ctx);
        HandlerDef handler = player.getCurrentHandlerDef(ctx);

        int destPos = bytecode.pos + (int) bytecode.obj;
        Integer newBytecodeIndex = handler.bytecodeIndexMap.get(destPos);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        if (newBytecodeIndex != null) {
            scope.bytecodeIndex = newBytecodeIndex;
        }

        return HandlerExecutionResult.JUMP;
    }

    public static HandlerExecutionResult endRepeat(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        Bytecode bytecode = player.getCtxCurrentBytecode(ctx);
        HandlerDef handler = player.getCurrentHandlerDef(ctx);

        int returnPos = bytecode.pos - (int) bytecode.obj;
        Integer newIndex = handler.bytecodeIndexMap.get(returnPos);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        if (newIndex != null) {
            scope.bytecodeIndex = newIndex;
        }

        return HandlerExecutionResult.JUMP;
    }

    public static HandlerExecutionResult extCall(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int nameId = (int) player.getCtxCurrentBytecode(ctx).obj;
        String name = player.getName(ctx, nameId);

        // Debug: show what ext_call is calling
        if (player.scopes.size() == 4) {
            System.err.println("[DEBUG] ext_call (depth=4): nameId=" + nameId + " name='" + name + "'");
            System.err.println("[DEBUG] Scope stack:");
            for (int i = 0; i < player.scopes.size(); i++) {
                ScriptScope s = player.scopes.get(i);
                System.err.println("  " + i + ": " + s.scriptMemberRef + " handler=" + s.handlerNameId);
            }
        } else if (player.scopes.size() > 4 && player.scopes.size() <= 6) {
            System.err.println("[DEBUG] ext_call: nameId=" + nameId + " name='" + name + "' depth=" + player.scopes.size());
        }

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int argListDatumRef = scope.stack.pop();
        Datum argListDatum = player.getDatum(argListDatumRef);

        if (!argListDatum.isList()) {
            throw new ScriptError("ext_call was not passed a list");
        }

        List<Integer> argRefList = argListDatum.toList();
        boolean isNoRet = argListDatum.getListType() == DatumType.ArgListNoRet;

        // Handle "return" specially
        if (name.equals("return")) {
            if (!argRefList.isEmpty()) {
                scope.returnValue = argRefList.get(0);
            }
            return HandlerExecutionResult.STOP;
        }

        // Try to find handler in movie scripts first (like Rust player_call_global_handler)
        int returnValue = callGlobalHandler(player, name, argRefList);

        if (!isNoRet) {
            scope.stack.push(returnValue);
        }

        return HandlerExecutionResult.ADVANCE;
    }

    private static final int MAX_SCOPE_DEPTH = 100;

    /**
     * Call a global handler - first tries script instances, then movie scripts, then built-in handlers.
     * Port of Rust player_call_global_handler.
     */
    private static int callGlobalHandler(DirPlayer player, String handlerName, List<Integer> args) throws ScriptError {
        // Check for maximum recursion depth
        if (player.scopes.size() > MAX_SCOPE_DEPTH) {
            throw new ScriptError("Maximum scope depth exceeded (possible infinite recursion) while calling: " + handlerName);
        }

        // Debug: log what's being called at high recursion depths
        if (player.scopes.size() > 5) {
            System.err.println("[DEBUG] callGlobalHandler: " + handlerName + " depth=" + player.scopes.size());
        }

        // "new" invocations should always go through the built-in handler (like Rust)
        if (handlerName.equalsIgnoreCase("new")) {
            return HandlerManager.callHandler(player, handlerName, args);
        }

        // Check if first arg is a script or script instance that has this handler
        // This allows calls like: customFunc(scriptInstance, arg1, arg2)
        if (!args.isEmpty()) {
            com.dirplayer.director.lingo.Datum firstArg = player.getDatum(args.get(0));
            com.dirplayer.player.script.ScriptInstanceRef instanceRef = getScriptInstanceFromDatum(player, firstArg);
            if (instanceRef != null) {
                com.dirplayer.player.script.ScriptInstance instance = player.allocator.getScriptInstance(instanceRef);
                if (instance != null) {
                    com.dirplayer.player.script.Script script = player.movie.castManager.getScriptByRef(instance.script);
                    if (script != null && script.hasHandler(handlerName)) {
                        com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                            instanceRef, script.memberRef, handlerName, args);
                        return result.returnValue;
                    }
                }
            }
        }

        // Check active script instances on sprites (behaviors)
        for (com.dirplayer.player.score.SpriteChannel channel : player.movie.score.channels) {
            for (Integer instanceId : channel.sprite.scriptInstanceList) {
                com.dirplayer.player.script.ScriptInstanceRef instanceRef =
                    new com.dirplayer.player.script.ScriptInstanceRef(instanceId);
                com.dirplayer.player.script.ScriptInstance instance = player.allocator.getScriptInstance(instanceRef);
                if (instance != null) {
                    com.dirplayer.player.script.Script script = player.movie.castManager.getScriptByRef(instance.script);
                    if (script != null && script.hasHandler(handlerName)) {
                        com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                            instanceRef, script.memberRef, handlerName, args);
                        return result.returnValue;
                    }
                }
            }
        }

        // Check movie scripts
        for (com.dirplayer.player.script.Script script : player.movie.castManager.getMovieScripts()) {
            if (script.hasHandler(handlerName)) {
                if (player.scopes.size() > 5) {
                    System.err.println("[DEBUG] Found handler '" + handlerName + "' in movie script: " +
                        script.memberRef + " name=" + script.name + " (type: " + script.scriptType + ")");
                }
                com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                    null, script.memberRef, handlerName, args);
                return result.returnValue;
            }
        }

        // Check frame script
        com.dirplayer.player.score.ScoreBehaviorReference frameScript =
            player.movie.score.getScriptInFrame(player.movie.currentFrame);
        if (frameScript != null) {
            com.dirplayer.player.CastMemberRef scriptRef =
                new com.dirplayer.player.CastMemberRef(frameScript.castLib, frameScript.castMember);
            com.dirplayer.player.script.Script script = player.movie.castManager.getScriptByRef(scriptRef);
            if (script != null && script.hasHandler(handlerName)) {
                com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                    null, scriptRef, handlerName, args);
                return result.returnValue;
            }
        }

        // Fall back to built-in handlers
        return HandlerManager.callHandler(player, handlerName, args);
    }

    /**
     * Try to extract a ScriptInstanceRef from a Datum.
     */
    private static com.dirplayer.player.script.ScriptInstanceRef getScriptInstanceFromDatum(
            DirPlayer player, com.dirplayer.director.lingo.Datum datum) {
        if (datum == null) return null;

        try {
            if (datum.getType() == com.dirplayer.director.lingo.DatumType.ScriptInstanceRef) {
                return datum.toScriptInstanceRef();
            }
        } catch (com.dirplayer.player.ScriptError e) {
            // Not a script instance, that's fine
        }
        return null;
    }

    public static HandlerExecutionResult localCall(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int argListId = scope.stack.pop();

        Datum argListDatum = player.getDatum(argListId);
        boolean isNoRet = argListDatum.getListType() == DatumType.ArgListNoRet;
        List<Integer> args = argListDatum.toList();

        // Get handler reference
        int handlerIndex = (int) player.getCtxCurrentBytecode(ctx).obj;

        // Call local handler
        int result = player.localCall(ctx, handlerIndex, args);

        if (!isNoRet) {
            scope.stack.push(result);
        }

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult objCall(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int argListId = scope.stack.pop();

        Datum argListDatum = player.getDatum(argListId);
        boolean isNoRet = argListDatum.getListType() == DatumType.ArgListNoRet;
        List<Integer> argList = argListDatum.toList();

        if (argList.isEmpty()) {
            throw new ScriptError("obj_call requires at least one argument (the object)");
        }

        int objRef = argList.get(0);
        List<Integer> args = argList.subList(1, argList.size());

        String handlerName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        // Call object handler
        int result = player.callDatumHandler(objRef, handlerName, args);

        player.lastHandlerResult = result;

        if (!isNoRet) {
            scope.stack.push(result);
        }

        return HandlerExecutionResult.ADVANCE;
    }
}
