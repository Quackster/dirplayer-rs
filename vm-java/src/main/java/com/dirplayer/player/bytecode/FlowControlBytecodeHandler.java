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

        boolean isZero = CompareBytecodeHandler.datumIsZero(datum, player);

        if (isZero) {
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
        com.dirplayer.director.chunks.Bytecode bytecode = player.getCtxCurrentBytecode(ctx);
        int nameId = (int) bytecode.obj;
        String name = player.getName(ctx, nameId);

        // Debug: trace handler calls (disabled for cleaner output)
        // System.err.println("[TRACE] ExtCall: " + name);

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

        // Debug: Track convertToPropList calls
        if (name.equalsIgnoreCase("converttoproplist")) {
            System.err.println("[DEBUG extCall convertToPropList] Args count: " + argRefList.size());
            for (int i = 0; i < argRefList.size(); i++) {
                Datum argDatum = player.getDatum(argRefList.get(i));
                System.err.println("[DEBUG extCall convertToPropList] Arg " + i + ": type=" + argDatum.getType() + ", value=\"" + player.formatDatum(argDatum) + "\"");
            }
            System.err.println("[DEBUG extCall convertToPropList] Script member: " + scope.scriptMemberRef);
            new Exception("convertToPropList call trace").printStackTrace(System.err);
        }

        // Try to find handler in movie scripts first (like Rust player_call_global_handler)
        int returnValue = callGlobalHandler(player, name, argRefList);

        // Debug: log if void is returned for a non-void call
        if (!isNoRet) {
            Datum resultDatum = player.getDatum(returnValue);
            if (resultDatum != null && resultDatum.getType() == DatumType.Void) {
                System.err.println("[DEBUG] extCall '" + name + "' returned VOID (unexpected for non-noret call)");
            }
        }

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

        // Debug: Track objCalls to trace empty string issues
        if (handlerName.equalsIgnoreCase("setat") || handlerName.equalsIgnoreCase("converttoproplist")) {
            Datum objDatum = player.getDatum(objRef);
            System.err.println("[DEBUG objCall " + handlerName + "] Object type: " + objDatum.getType());
            System.err.println("[DEBUG objCall " + handlerName + "] Object value: " + player.formatDatum(objDatum));
            System.err.println("[DEBUG objCall " + handlerName + "] Args count: " + args.size());
            for (int i = 0; i < args.size(); i++) {
                Datum argDatum = player.getDatum(args.get(i));
                System.err.println("[DEBUG objCall " + handlerName + "] Arg " + i + ": type=" + argDatum.getType() + ", value=" + player.formatDatum(argDatum));
            }
            // Print current script/handler context
            ScriptScope scope2 = player.scopes.get(ctx.scopeRef);
            System.err.println("[DEBUG objCall " + handlerName + "] Script member: " + scope2.scriptMemberRef);
            System.err.println("[DEBUG objCall " + handlerName + "] Handler nameId: " + scope2.handlerNameId);
            // Print stack trace to see call chain
            if (handlerName.equalsIgnoreCase("converttoproplist")) {
                new Exception("convertToPropList call trace").printStackTrace(System.err);
            }
        }

        // Call object handler
        int result = player.callDatumHandler(objRef, handlerName, args);

        player.lastHandlerResult = result;

        if (!isNoRet) {
            scope.stack.push(result);
        }

        return HandlerExecutionResult.ADVANCE;
    }

    /**
     * Check if a handler name is an event handler name that should not be called
     * as a function. These handlers should only be triggered by the event system.
     * Based on Rust's ScriptInstanceDatumHandlers which returns void for these.
     */
    private static boolean isEventHandlerName(String name) {
        switch (name) {
            case "preparemovie":
            case "startmovie":
            case "stopmovie":
            case "prepareframe":
            case "enterframe":
            case "exitframe":
            case "beginsprite":
            case "endsprite":
            case "keydown":
            case "keyup":
            case "mousedown":
            case "mouseup":
            case "mouseenter":
            case "mouseleave":
            case "mousewithin":
            case "activate":
            case "deactivate":
            case "idle":
            case "stepframe":
                return true;
            default:
                return false;
        }
    }
}
