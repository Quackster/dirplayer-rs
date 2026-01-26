package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.ContextVars;
import java.util.ArrayList;
import java.util.List;

import com.dirplayer.player.CastMemberRef;

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
        // TODO: Implement literal access from script context
        // For now, push void as placeholder
        int datumRef = player.allocDatum(Datum.ofVoid());

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(datumRef);

        return HandlerExecutionResult.ADVANCE;
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
        // TODO: Implement script instance creation
        throw new ScriptError("newObj bytecode handler not yet implemented");
    }
}
