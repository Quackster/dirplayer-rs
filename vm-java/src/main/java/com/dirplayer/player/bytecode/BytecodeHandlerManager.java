package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.director.chunks.Bytecode;
import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import java.util.ArrayList;
import java.util.List;

/**
 * Main bytecode handler manager - dispatches bytecode execution.
 * Port of Rust StaticBytecodeHandlerManager struct.
 */
public class BytecodeHandlerManager {

    // Execution history for debugging
    private static final int EXECUTION_HISTORY_SIZE = 100;
    private static ExecutionHistoryEntry[] executionHistory;
    private static int historyWriteIndex = 0;
    private static int historyCount = 0;

    // Pre-allocate history entries to avoid GC pressure
    static {
        executionHistory = new ExecutionHistoryEntry[EXECUTION_HISTORY_SIZE];
        for (int i = 0; i < EXECUTION_HISTORY_SIZE; i++) {
            executionHistory[i] = new ExecutionHistoryEntry();
        }
    }

    // Expression tracker for tracing
    private static StackExpressionTracker expressionTracker = new StackExpressionTracker();

    /**
     * Lightweight execution history entry.
     */
    private static class ExecutionHistoryEntry {
        int opcode;
        int bytecodePos;
        int operand;
        int handlerNameId;
        int scriptCastLib;
        int scriptCastMember;
    }

    /**
     * Record a bytecode execution to the history.
     */
    private static void recordExecution(
        int opcodeValue,
        int bytecodePos,
        int operand,
        int handlerNameId,
        int scriptCastLib,
        int scriptCastMember
    ) {
        // Reuse pre-allocated entry instead of creating new one
        ExecutionHistoryEntry entry = executionHistory[historyWriteIndex];
        entry.opcode = opcodeValue;
        entry.bytecodePos = bytecodePos;
        entry.operand = operand;
        entry.handlerNameId = handlerNameId;
        entry.scriptCastLib = scriptCastLib;
        entry.scriptCastMember = scriptCastMember;

        historyWriteIndex = (historyWriteIndex + 1) % EXECUTION_HISTORY_SIZE;
        if (historyCount < EXECUTION_HISTORY_SIZE) {
            historyCount++;
        }
    }

    /**
     * Dump execution history on error.
     */
    public static void dumpExecutionHistoryOnError(String errorMessage) {
        System.err.println("Bytecode execution history (last " + EXECUTION_HISTORY_SIZE + " ops before error):");
        System.err.println("Error: " + errorMessage);

        // Calculate start position for iteration (ring buffer)
        int start = historyCount < EXECUTION_HISTORY_SIZE ? 0 : historyWriteIndex;

        for (int i = 0; i < historyCount; i++) {
            int idx = (start + i) % EXECUTION_HISTORY_SIZE;
            ExecutionHistoryEntry entry = executionHistory[idx];
            if (entry != null) {
                OpCode opcode = OpCode.from(entry.opcode);
                String opName = opcode != null ? opcode.name() : "UNKNOWN";
                System.err.printf("%3d. [%4d] %-20s %6d (@%d:%d)%n",
                    i + 1,
                    entry.bytecodePos,
                    opName,
                    entry.operand,
                    entry.scriptCastLib,
                    entry.scriptCastMember
                );
            }
        }
    }

    /**
     * Clear the execution history.
     */
    public static void clearExecutionHistory() {
        for (int i = 0; i < EXECUTION_HISTORY_SIZE; i++) {
            executionHistory[i] = null;
        }
        historyWriteIndex = 0;
        historyCount = 0;
    }

    /**
     * Get the expression tracker for debugging.
     */
    public static StackExpressionTracker getExpressionTracker() {
        return expressionTracker;
    }

    /**
     * Check if an opcode has an async handler.
     */
    public static boolean hasAsyncHandler(OpCode opcode) {
        switch (opcode) {
            case NewObj:
            case ExtCall:
            case ObjCall:
            case LocalCall:
            case SetObjProp:
                return true;
            default:
                return false;
        }
    }

    /**
     * Call a synchronous bytecode handler.
     */
    public static HandlerExecutionResult callSyncHandler(
        DirPlayer player,
        OpCode opcode,
        BytecodeHandlerContext ctx
    ) throws ScriptError {
        switch (opcode) {
            // Arithmetic
            case Add:
                return ArithmeticsBytecodeHandler.add(player, ctx);
            case Sub:
                return ArithmeticsBytecodeHandler.sub(player, ctx);
            case Mul:
                return ArithmeticsBytecodeHandler.mul(player, ctx);
            case Div:
                return ArithmeticsBytecodeHandler.div(player, ctx);
            case Mod:
                return ArithmeticsBytecodeHandler.mod(player, ctx);
            case Inv:
                return ArithmeticsBytecodeHandler.inv(player, ctx);

            // Stack operations
            case PushInt8:
            case PushInt16:
            case PushInt32:
                return StackBytecodeHandler.pushInt(player, ctx);
            case PushFloat32:
                return StackBytecodeHandler.pushF32(player, ctx);
            case PushArgList:
                return StackBytecodeHandler.pushArglist(player, ctx);
            case PushArgListNoRet:
                return StackBytecodeHandler.pushArglistNoRet(player, ctx);
            case PushSymb:
                return StackBytecodeHandler.pushSymb(player, ctx);
            case PushCons:
                return StackBytecodeHandler.pushCons(player, ctx);
            case PushZero:
                return StackBytecodeHandler.pushZero(player, ctx);
            case PushPropList:
                return StackBytecodeHandler.pushPropList(player, ctx);
            case PushList:
                return StackBytecodeHandler.pushList(player, ctx);
            case PushChunkVarRef:
                return StackBytecodeHandler.pushChunkVarRef(player, ctx);
            case Swap:
                return StackBytecodeHandler.swap(player, ctx);
            case Peek:
                return StackBytecodeHandler.peek(player, ctx);
            case Pop:
                return StackBytecodeHandler.pop(player, ctx);

            // Comparison
            case Gt:
                return CompareBytecodeHandler.gt(player, ctx);
            case Lt:
                return CompareBytecodeHandler.lt(player, ctx);
            case GtEq:
                return CompareBytecodeHandler.gtEq(player, ctx);
            case LtEq:
                return CompareBytecodeHandler.ltEq(player, ctx);
            case Not:
                return CompareBytecodeHandler.not(player, ctx);
            case NtEq:
                return CompareBytecodeHandler.ntEq(player, ctx);
            case And:
                return CompareBytecodeHandler.and(player, ctx);
            case Or:
                return CompareBytecodeHandler.or(player, ctx);
            case Eq:
                return CompareBytecodeHandler.eq(player, ctx);

            // Flow control
            case Ret:
                return FlowControlBytecodeHandler.ret(player, ctx);
            case JmpIfZ:
                return FlowControlBytecodeHandler.jmpIfZero(player, ctx);
            case Jmp:
                return FlowControlBytecodeHandler.jmp(player, ctx);
            case EndRepeat:
                return FlowControlBytecodeHandler.endRepeat(player, ctx);

            // Get/Set
            case GetProp:
                return GetSetBytecodeHandler.getProp(player, ctx);
            case SetProp:
                return GetSetBytecodeHandler.setProp(player, ctx);
            case GetObjProp:
                return GetSetBytecodeHandler.getObjProp(player, ctx);
            case GetMovieProp:
                return GetSetBytecodeHandler.getMovieProp(player, ctx);
            case SetMovieProp:
                return GetSetBytecodeHandler.setMovieProp(player, ctx);
            case Set:
                return GetSetBytecodeHandler.set(player, ctx);
            case Get:
                return GetSetBytecodeHandler.get(player, ctx);
            case GetGlobal:
                return GetSetBytecodeHandler.getGlobal(player, ctx);
            case SetGlobal:
                return GetSetBytecodeHandler.setGlobal(player, ctx);
            case GetField:
                return GetSetBytecodeHandler.getField(player, ctx);
            case GetLocal:
                return GetSetBytecodeHandler.getLocal(player, ctx);
            case SetLocal:
                return GetSetBytecodeHandler.setLocal(player, ctx);
            case GetParam:
                return GetSetBytecodeHandler.getParam(player, ctx);
            case SetParam:
                return GetSetBytecodeHandler.setParam(player, ctx);
            case TheBuiltin:
                return GetSetBytecodeHandler.theBuiltIn(player, ctx);
            case GetChainedProp:
                return GetSetBytecodeHandler.getChainedProp(player, ctx);
            case GetTopLevelProp:
                return GetSetBytecodeHandler.getTopLevelProp(player, ctx);

            // String operations
            case ContainsStr:
                return StringBytecodeHandler.containsStr(player, ctx);
            case Contains0Str:
                return StringBytecodeHandler.contains0Str(player, ctx);
            case JoinPadStr:
                return StringBytecodeHandler.joinPadStr(player, ctx);
            case JoinStr:
                return StringBytecodeHandler.joinStr(player, ctx);
            case Put:
                return StringBytecodeHandler.put(player, ctx);
            case GetChunk:
                return StringBytecodeHandler.getChunk(player, ctx);
            case DeleteChunk:
                return StringBytecodeHandler.deleteChunk(player, ctx);
            case PutChunk:
                return StringBytecodeHandler.putChunk(player, ctx);

            // Sprite operations
            case OntoSpr:
                return SpriteCompareBytecodeHandler.ontoSprite(player, ctx);
            case IntoSpr:
                return SpriteCompareBytecodeHandler.intoSprite(player, ctx);

            default:
                String name = opcode.name();
                int value = opcode.getValue();
                throw new ScriptError(String.format("No handler for opcode %s (0x%04x)", name, value));
        }
    }

    /**
     * Call an async bytecode handler.
     * In Java, we handle async operations synchronously for simplicity.
     */
    public static HandlerExecutionResult callAsyncHandler(
        DirPlayer player,
        OpCode opcode,
        BytecodeHandlerContext ctx
    ) throws ScriptError {
        switch (opcode) {
            case NewObj:
                return StackBytecodeHandler.newObj(player, ctx);
            case ExtCall:
                return FlowControlBytecodeHandler.extCall(player, ctx);
            case ObjCall:
                return FlowControlBytecodeHandler.objCall(player, ctx);
            case LocalCall:
                return FlowControlBytecodeHandler.localCall(player, ctx);
            case SetObjProp:
                return GetSetBytecodeHandler.setObjProp(player, ctx);

            default:
                String name = opcode.name();
                int value = opcode.getValue();
                throw new ScriptError(String.format("No async handler for opcode %s (0x%04x)", name, value));
        }
    }

    /**
     * Execute a single bytecode instruction.
     */
    public static HandlerExecutionResult executeBytecode(
        DirPlayer player,
        BytecodeHandlerContext ctx
    ) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        HandlerDef handler = player.getCurrentHandlerDef(ctx);
        Bytecode bytecode = handler.bytecodeArray.get(scope.bytecodeIndex);
        OpCode opcode = bytecode.opcode;

        // Record to execution history
        recordExecution(
            opcode.getValue(),
            bytecode.pos,
            (int) bytecode.obj,
            handler.nameId,
            ctx.scriptCastLib,
            ctx.scriptCastMember
        );

        // Execute the bytecode
        try {
            if (hasAsyncHandler(opcode)) {
                return callAsyncHandler(player, opcode, ctx);
            } else {
                return callSyncHandler(player, opcode, ctx);
            }
        } catch (ScriptError e) {
            // Dump execution history on error (disabled for cleaner output)
            // dumpExecutionHistoryOnError(e.getMessage());
            throw e;
        }
    }
}
