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

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int argListDatumRef = scope.stack.pop();
        Datum argListDatum = player.getDatum(argListDatumRef);

        if (!argListDatum.isList()) {
            throw new ScriptError("ext_call was not passed a list");
        }

        List<Integer> argRefList = argListDatum.toList();
        boolean isNoRet = argListDatum.getListType() == DatumType.ArgListNoRet;

        // Call external handler
        int returnValue = HandlerManager.callHandler(player, name, argRefList);

        if (!isNoRet) {
            scope.stack.push(returnValue);
        }

        return HandlerExecutionResult.ADVANCE;
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
