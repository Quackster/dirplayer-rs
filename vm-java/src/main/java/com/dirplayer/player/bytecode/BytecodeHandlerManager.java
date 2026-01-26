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
    private static ExecutionHistoryEntry[] executionHistory = new ExecutionHistoryEntry[EXECUTION_HISTORY_SIZE];
    private static int historyWriteIndex = 0;
    private static int historyCount = 0;

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
        ExecutionHistoryEntry entry = new ExecutionHistoryEntry();
        entry.opcode = opcodeValue;
        entry.bytecodePos = bytecodePos;
        entry.operand = operand;
        entry.handlerNameId = handlerNameId;
        entry.scriptCastLib = scriptCastLib;
        entry.scriptCastMember = scriptCastMember;

        executionHistory[historyWriteIndex] = entry;
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
                OpCode opcode = OpCode.fromValue(entry.opcode);
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
            case NEW_OBJ:
            case EXT_CALL:
            case OBJ_CALL:
            case LOCAL_CALL:
            case SET_OBJ_PROP:
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
            case ADD:
                return ArithmeticsBytecodeHandler.add(player, ctx);
            case SUB:
                return ArithmeticsBytecodeHandler.sub(player, ctx);
            case MUL:
                return ArithmeticsBytecodeHandler.mul(player, ctx);
            case DIV:
                return ArithmeticsBytecodeHandler.div(player, ctx);
            case MOD:
                return ArithmeticsBytecodeHandler.mod(player, ctx);
            case INV:
                return ArithmeticsBytecodeHandler.inv(player, ctx);

            // Stack operations
            case PUSH_INT8:
            case PUSH_INT16:
            case PUSH_INT32:
                return StackBytecodeHandler.pushInt(player, ctx);
            case PUSH_FLOAT32:
                return StackBytecodeHandler.pushF32(player, ctx);
            case PUSH_ARG_LIST:
                return StackBytecodeHandler.pushArglist(player, ctx);
            case PUSH_ARG_LIST_NO_RET:
                return StackBytecodeHandler.pushArglistNoRet(player, ctx);
            case PUSH_SYMB:
                return StackBytecodeHandler.pushSymb(player, ctx);
            case PUSH_CONS:
                return StackBytecodeHandler.pushCons(player, ctx);
            case PUSH_ZERO:
                return StackBytecodeHandler.pushZero(player, ctx);
            case PUSH_PROP_LIST:
                return StackBytecodeHandler.pushPropList(player, ctx);
            case PUSH_LIST:
                return StackBytecodeHandler.pushList(player, ctx);
            case PUSH_CHUNK_VAR_REF:
                return StackBytecodeHandler.pushChunkVarRef(player, ctx);
            case SWAP:
                return StackBytecodeHandler.swap(player, ctx);
            case PEEK:
                return StackBytecodeHandler.peek(player, ctx);
            case POP:
                return StackBytecodeHandler.pop(player, ctx);

            // Comparison
            case GT:
                return CompareBytecodeHandler.gt(player, ctx);
            case LT:
                return CompareBytecodeHandler.lt(player, ctx);
            case GT_EQ:
                return CompareBytecodeHandler.gtEq(player, ctx);
            case LT_EQ:
                return CompareBytecodeHandler.ltEq(player, ctx);
            case NOT:
                return CompareBytecodeHandler.not(player, ctx);
            case NT_EQ:
                return CompareBytecodeHandler.ntEq(player, ctx);
            case AND:
                return CompareBytecodeHandler.and(player, ctx);
            case OR:
                return CompareBytecodeHandler.or(player, ctx);
            case EQ:
                return CompareBytecodeHandler.eq(player, ctx);

            // Flow control
            case RET:
                return FlowControlBytecodeHandler.ret(player, ctx);
            case JMP_IF_Z:
                return FlowControlBytecodeHandler.jmpIfZero(player, ctx);
            case JMP:
                return FlowControlBytecodeHandler.jmp(player, ctx);
            case END_REPEAT:
                return FlowControlBytecodeHandler.endRepeat(player, ctx);

            // Get/Set
            case GET_PROP:
                return GetSetBytecodeHandler.getProp(player, ctx);
            case SET_PROP:
                return GetSetBytecodeHandler.setProp(player, ctx);
            case GET_OBJ_PROP:
                return GetSetBytecodeHandler.getObjProp(player, ctx);
            case GET_MOVIE_PROP:
                return GetSetBytecodeHandler.getMovieProp(player, ctx);
            case SET_MOVIE_PROP:
                return GetSetBytecodeHandler.setMovieProp(player, ctx);
            case SET:
                return GetSetBytecodeHandler.set(player, ctx);
            case GET:
                return GetSetBytecodeHandler.get(player, ctx);
            case GET_GLOBAL:
                return GetSetBytecodeHandler.getGlobal(player, ctx);
            case SET_GLOBAL:
                return GetSetBytecodeHandler.setGlobal(player, ctx);
            case GET_FIELD:
                return GetSetBytecodeHandler.getField(player, ctx);
            case GET_LOCAL:
                return GetSetBytecodeHandler.getLocal(player, ctx);
            case SET_LOCAL:
                return GetSetBytecodeHandler.setLocal(player, ctx);
            case GET_PARAM:
                return GetSetBytecodeHandler.getParam(player, ctx);
            case SET_PARAM:
                return GetSetBytecodeHandler.setParam(player, ctx);
            case THE_BUILTIN:
                return GetSetBytecodeHandler.theBuiltIn(player, ctx);
            case GET_CHAINED_PROP:
                return GetSetBytecodeHandler.getChainedProp(player, ctx);
            case GET_TOP_LEVEL_PROP:
                return GetSetBytecodeHandler.getTopLevelProp(player, ctx);

            // String operations
            case CONTAINS_STR:
                return StringBytecodeHandler.containsStr(player, ctx);
            case CONTAINS_0_STR:
                return StringBytecodeHandler.contains0Str(player, ctx);
            case JOIN_PAD_STR:
                return StringBytecodeHandler.joinPadStr(player, ctx);
            case JOIN_STR:
                return StringBytecodeHandler.joinStr(player, ctx);
            case PUT:
                return StringBytecodeHandler.put(player, ctx);
            case GET_CHUNK:
                return StringBytecodeHandler.getChunk(player, ctx);
            case DELETE_CHUNK:
                return StringBytecodeHandler.deleteChunk(player, ctx);
            case PUT_CHUNK:
                return StringBytecodeHandler.putChunk(player, ctx);

            // Sprite operations
            case ONTO_SPR:
                return SpriteCompareBytecodeHandler.ontoSprite(player, ctx);
            case INTO_SPR:
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
            case NEW_OBJ:
                return StackBytecodeHandler.newObj(player, ctx);
            case EXT_CALL:
                return FlowControlBytecodeHandler.extCall(player, ctx);
            case OBJ_CALL:
                return FlowControlBytecodeHandler.objCall(player, ctx);
            case LOCAL_CALL:
                return FlowControlBytecodeHandler.localCall(player, ctx);
            case SET_OBJ_PROP:
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
            // Dump execution history on error
            dumpExecutionHistoryOnError(e.getMessage());
            throw e;
        }
    }
}
