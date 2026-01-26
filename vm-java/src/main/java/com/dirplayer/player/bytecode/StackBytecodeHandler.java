package com.dirplayer.player.bytecode;

import com.dirplayer.director.DirectorFile;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastLib;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.ContextVars;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.script.Script;
import com.dirplayer.player.script.ScriptInstance;
import com.dirplayer.player.handlers.datum.ScriptHandlers;
import java.util.ArrayList;
import java.util.List;

/**
 * Stack bytecode handlers - push/pop/peek operations.
 * Port of Rust StackBytecodeHandler struct.
 */
public class StackBytecodeHandler {

    public static HandlerExecutionResult pushInt(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int value = (int) player.getCtxCurrentBytecode(ctx).obj;
        int datumRef = player.allocDatum(Datum.ofInt(value));

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushF32(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int objValue = (int) player.getCtxCurrentBytecode(ctx).obj;

        // Interpret the 32 bits as f32, then convert to f64
        float floatF32 = Float.intBitsToFloat(objValue);
        double floatF64 = floatF32;

        int datumRef = player.allocDatum(Datum.ofFloat(floatF64));

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushArglist(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        if (scope.stack.size() < (int) bytecodeObj) {
            throw new ScriptError("Not enough items in stack to create arglist");
        }

        List<Integer> items = scope.popN((int) bytecodeObj);
        int datumRef = player.allocDatum(Datum.ofList(DatumType.ArgList, items, false));

        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushArglistNoRet(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        if (scope.stack.size() < (int) bytecodeObj) {
            throw new ScriptError("Not enough items in stack to create arglist");
        }

        List<Integer> items = scope.popN((int) bytecodeObj);
        int datumRef = player.allocDatum(Datum.ofList(DatumType.ArgListNoRet, items, false));

        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushSymb(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long nameId = player.getCtxCurrentBytecode(ctx).obj;
        String symbolName = player.getName(ctx, (int) nameId);

        int datumRef = player.allocDatum(Datum.ofSymbol(symbolName));

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushCons(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        // Get current script to access literals
        Script script = player.getCurrentScript(ctx);
        if (script == null || script.chunk == null) {
            throw new ScriptError("No script context for pushCons");
        }

        // Calculate the literal ID using variable multiplier
        int variableMultiplier = getVariableMultiplier(player, ctx);
        int literalId = (int) (player.getCtxCurrentBytecode(ctx).obj / variableMultiplier);

        // Get the literal from script chunk
        if (literalId < 0 || literalId >= script.chunk.literals.size()) {
            throw new ScriptError("Literal ID out of range: " + literalId);
        }
        Datum literal = script.chunk.literals.get(literalId);

        // Allocate and push a copy of the literal
        int datumRef = player.allocDatum(literal.clone());

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    private static int getVariableMultiplier(DirPlayer player, BytecodeHandlerContext ctx) {
        Script script = player.getCurrentScript(ctx);
        if (script != null) {
            CastLib cast = player.movie.castManager.getCastOrNull(script.memberRef.getCastLib());
            if (cast != null) {
                return DirectorFile.getVariableMultiplier(cast.capitalX, cast.dirVersion);
            }
        }
        // Default to 6 for older versions
        return player.movie.dirVersion >= 500 ? 8 : 6;
    }

    public static HandlerExecutionResult pushZero(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int datumRef = player.allocDatum(Datum.ofInt(0));

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushPropList(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int argListRef = scope.stack.pop();

        Datum argListDatum = player.getDatum(argListRef);
        List<Integer> argList = argListDatum.toList();

        if (argList.size() % 2 != 0) {
            throw new ScriptError("argList length must be even");
        }

        int entryCount = argList.size() / 2;
        List<int[]> entries = new ArrayList<>();

        for (int i = 0; i < entryCount; i++) {
            int baseIndex = i * 2;
            int key = argList.get(baseIndex);
            int value = argList.get(baseIndex + 1);
            entries.add(new int[] { key, value });
        }

        int datumRef = player.allocDatum(Datum.ofPropList(entries, false, true));
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushList(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int listId = scope.stack.pop();

        Datum listDatum = player.getDatum(listId);
        List<Integer> list = new ArrayList<>(listDatum.toList());

        int resultId = player.allocDatum(Datum.ofList(DatumType.List, list, false));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult peek(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int offset = (int) player.getCtxCurrentBytecode(ctx).obj;

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int stackIndex = scope.stack.size() - 1 - offset;
        int datumRef = scope.stack.get(stackIndex);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pop(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int count = (int) player.getCtxCurrentBytecode(ctx).obj;

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.popN(count);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult swap(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int a = scope.stack.pop();
        int b = scope.stack.pop();
        scope.stack.push(a);
        scope.stack.push(b);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult pushChunkVarRef(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;
        int[] contextVarArgs = ContextVars.readContextVarArgs(player, (int) bytecodeObj, ctx.scopeRef);
        int idRef = contextVarArgs[0];
        Integer castIdRef = contextVarArgs.length > 1 ? contextVarArgs[1] : null;

        int valueRef = ContextVars.getContextVar(
            player,
            idRef,
            castIdRef,
            (int) bytecodeObj,
            ctx
        );

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult newObj(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        // Get the object type name
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;
        String objType = player.getName(ctx, (int) bytecodeObj);

        if (!"script".equalsIgnoreCase(objType)) {
            throw new ScriptError("Cannot create new instance of non-script: " + objType);
        }

        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        // Pop the argument list
        int argListRef = scope.stack.pop();
        Datum argListDatum = player.getDatum(argListRef);
        List<Integer> argList = argListDatum.toList();

        if (argList.isEmpty()) {
            throw new ScriptError("newObj requires at least a script name argument");
        }

        // First arg is the script name
        String scriptName = player.getDatum(argList.get(0)).stringValue();

        // Find the script by name
        CastMemberRef scriptMemberRef = player.movie.castManager.findMemberRefByName(scriptName);
        if (scriptMemberRef == null) {
            throw new ScriptError("Script not found: " + scriptName);
        }

        // Create the script instance
        ScriptHandlers.ScriptInstanceResult instanceResult = ScriptHandlers.createScriptInstance(player, scriptMemberRef);

        // Get extra args for the "new" handler
        List<Integer> extraArgs = new ArrayList<>();
        for (int i = 1; i < argList.size(); i++) {
            extraArgs.add(argList.get(i));
        }

        // Check if the script has a "new" handler and call it
        Script script = player.movie.castManager.getScriptByRef(scriptMemberRef);
        if (script != null && script.hasHandler("new")) {
            // Call the "new" handler on the script instance
            try {
                com.dirplayer.player.handlers.datum.ScriptInstanceHandlers.call(
                    player, instanceResult.datumRef, "new", extraArgs);
            } catch (ScriptError e) {
                // Log but don't fail - some scripts may not have new handlers
                SimpleLogger.getLogger(StackBytecodeHandler.class).debug(
                    "Error calling 'new' handler: {}", e.getMessage());
            }
        }

        // Push the script instance datum onto the stack
        scope.stack.push(instanceResult.datumRef);

        return HandlerExecutionResult.ADVANCE;
    }
}
