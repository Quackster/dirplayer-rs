package com.dirplayer.director.lingo.decompiler;

import com.dirplayer.director.chunks.Bytecode;
import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.director.chunks.ScriptChunk;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.director.lingo.ScriptContext;
import com.dirplayer.director.lingo.decompiler.DecompilerEnums.*;
import com.dirplayer.director.lingo.decompiler.Tokenizer.Span;

import java.util.*;

/**
 * Lingo bytecode decompiler - core handler logic.
 * Port of Rust handler.rs.
 */
public class DecompilerHandler {

    /**
     * Represents a decompiled line of Lingo code.
     */
    public static class DecompiledLine {
        public String text;
        public List<Integer> bytecodeIndices;
        public int indent;
        public List<Span> spans;

        public DecompiledLine(String text, List<Integer> bytecodeIndices, int indent, List<Span> spans) {
            this.text = text;
            this.bytecodeIndices = bytecodeIndices;
            this.indent = indent;
            this.spans = spans;
        }
    }

    /**
     * Result of decompiling a handler.
     */
    public static class DecompiledHandler {
        public String name;
        public List<String> arguments;
        public List<DecompiledLine> lines;
        public Map<Integer, Integer> bytecodeToLine;

        public DecompiledHandler() {
            this.arguments = new ArrayList<>();
            this.lines = new ArrayList<>();
            this.bytecodeToLine = new HashMap<>();
        }
    }

    /**
     * Tags for bytecode instructions (used for loop identification).
     */
    private static class BytecodeInfo {
        public BytecodeTag tag = BytecodeTag.None;
        public int ownerLoop = 0;
    }

    /**
     * Stack entry with bytecode tracking.
     */
    private static class StackEntry {
        public AstNode node;
        public List<Integer> bytecodeIndices;

        public StackEntry(AstNode node, List<Integer> bytecodeIndices) {
            this.node = node;
            this.bytecodeIndices = bytecodeIndices;
        }
    }

    /**
     * Decompiler state.
     */
    private static class DecompilerState {
        HandlerDef handler;
        ScriptChunk chunk;
        ScriptContext lctx;
        int version;
        int multiplier;

        // Stack for expressions with bytecode tracking
        List<StackEntry> stack;

        // AST building
        BlockNode rootBlock;
        BlockNode currentBlock;
        List<BlockNode> blockStack;

        // Bytecode tagging
        BytecodeInfo[] bytecodeTags;

        // Position mapping
        Map<Integer, Integer> bytecodePosMap;

        // Current bytecode index being processed
        int currentBytecodeIndex;

        // Statement to bytecode indices mapping
        List<List<Integer>> statementBytecodeIndices;

        DecompilerState(HandlerDef handler, ScriptChunk chunk, ScriptContext lctx, int version, int multiplier) {
            this.handler = handler;
            this.chunk = chunk;
            this.lctx = lctx;
            this.version = version;
            this.multiplier = multiplier;
            this.stack = new ArrayList<>();
            this.rootBlock = new BlockNode();
            this.currentBlock = rootBlock;
            this.blockStack = new ArrayList<>();
            this.statementBytecodeIndices = new ArrayList<>();

            // Build position map
            this.bytecodePosMap = new HashMap<>();
            for (int i = 0; i < handler.bytecodeArray.size(); i++) {
                Bytecode bc = handler.bytecodeArray.get(i);
                bytecodePosMap.put(bc.pos, i);
            }

            // Initialize tags
            this.bytecodeTags = new BytecodeInfo[handler.bytecodeArray.size()];
            for (int i = 0; i < bytecodeTags.length; i++) {
                bytecodeTags[i] = new BytecodeInfo();
            }
        }

        String getName(long id) {
            if (lctx != null && id >= 0 && id < lctx.names.size()) {
                return lctx.names.get((int) id);
            }
            return "UNKNOWN_" + id;
        }

        String getLocalName(long id) {
            int localIndex = (int) (id / multiplier);
            if (handler.localNameIds != null && localIndex < handler.localNameIds.size()) {
                int nameId = handler.localNameIds.get(localIndex);
                if (lctx != null && nameId < lctx.names.size()) {
                    return lctx.names.get(nameId);
                }
            }
            return "local_" + localIndex;
        }

        String getArgumentName(long id) {
            int argIndex = (int) (id / multiplier);
            if (handler.argumentNameIds != null && argIndex < handler.argumentNameIds.size()) {
                int nameId = handler.argumentNameIds.get(argIndex);
                if (lctx != null && nameId < lctx.names.size()) {
                    return lctx.names.get(nameId);
                }
            }
            return "arg_" + argIndex;
        }

        /**
         * Pop from stack, returning node.
         */
        AstNode pop() {
            if (!stack.isEmpty()) {
                return stack.remove(stack.size() - 1).node;
            }
            return AstNode.error();
        }

        /**
         * Pop from stack and collect bytecode indices into the provided list.
         */
        AstNode popWithIndices(List<Integer> indices) {
            if (!stack.isEmpty()) {
                StackEntry entry = stack.remove(stack.size() - 1);
                indices.addAll(entry.bytecodeIndices);
                return entry.node;
            }
            return AstNode.error();
        }

        void push(AstNode node) {
            List<Integer> indices = new ArrayList<>();
            indices.add(currentBytecodeIndex);
            stack.add(new StackEntry(node, indices));
        }

        void pushWithIndices(AstNode node, List<Integer> indices) {
            List<Integer> allIndices = new ArrayList<>(indices);
            allIndices.add(currentBytecodeIndex);
            stack.add(new StackEntry(node, allIndices));
        }

        void enterBlock(BlockNode block) {
            blockStack.add(currentBlock);
            currentBlock = block;
        }

        void exitBlock() {
            if (!blockStack.isEmpty()) {
                currentBlock = blockStack.remove(blockStack.size() - 1);
            }
        }

        void addStatement(AstNode node, List<Integer> bytecodeIndices) {
            currentBlock.addChild(node);
            statementBytecodeIndices.add(bytecodeIndices);
        }

        /**
         * Tag loops in the bytecode.
         */
        void tagLoops() {
            List<Bytecode> bytecodeArray = handler.bytecodeArray;

            for (int startIndex = 0; startIndex < bytecodeArray.size(); startIndex++) {
                Bytecode jmpifz = bytecodeArray.get(startIndex);
                if (jmpifz.opcode != OpCode.JmpIfZ) {
                    continue;
                }

                // Calculate jump position
                int jmpPos = jmpifz.pos + (int) jmpifz.obj;
                Integer endIndexObj = bytecodePosMap.get(jmpPos);
                if (endIndexObj == null) {
                    continue;
                }
                int endIndex = endIndexObj;

                if (endIndex == 0) {
                    continue;
                }

                Bytecode endRepeat = bytecodeArray.get(endIndex - 1);
                if (endRepeat.opcode != OpCode.EndRepeat) {
                    continue;
                }

                // Check if endrepeat jumps back before jmpifz
                if (endRepeat.pos < endRepeat.obj) {
                    continue;
                }
                if ((endRepeat.pos - (int) endRepeat.obj) > jmpifz.pos) {
                    continue;
                }

                BytecodeTag loopType = identifyLoop(startIndex, endIndex);
                bytecodeTags[startIndex].tag = loopType;

                switch (loopType) {
                    case RepeatWithIn:
                        // Tag pre-loop setup (7 instructions before jmpifz)
                        if (startIndex >= 7) {
                            for (int i = startIndex - 7; i < startIndex; i++) {
                                bytecodeTags[i].tag = BytecodeTag.Skip;
                            }
                        }
                        // Tag post-condition setup (5 instructions after jmpifz)
                        for (int i = startIndex + 1; i <= Math.min(startIndex + 5, bytecodeArray.size() - 1); i++) {
                            bytecodeTags[i].tag = BytecodeTag.Skip;
                        }
                        // Tag loop increment and end
                        if (endIndex >= 3) {
                            bytecodeTags[endIndex - 3].tag = BytecodeTag.NextRepeatTarget;
                            bytecodeTags[endIndex - 3].ownerLoop = startIndex;
                            bytecodeTags[endIndex - 2].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 1].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 1].ownerLoop = startIndex;
                        }
                        if (endIndex < bytecodeArray.size()) {
                            bytecodeTags[endIndex].tag = BytecodeTag.Skip;
                        }
                        break;

                    case RepeatWithTo:
                    case RepeatWithDownTo:
                        Bytecode endRep = bytecodeArray.get(endIndex - 1);
                        Integer condStartIndexObj = bytecodePosMap.get(endRep.pos - (int) endRep.obj);
                        if (condStartIndexObj != null) {
                            int condStartIndex = condStartIndexObj;
                            if (condStartIndex > 0) {
                                bytecodeTags[condStartIndex - 1].tag = BytecodeTag.Skip;
                            }
                            bytecodeTags[condStartIndex].tag = BytecodeTag.Skip;
                        }
                        if (startIndex > 0) {
                            bytecodeTags[startIndex - 1].tag = BytecodeTag.Skip;
                        }
                        if (endIndex >= 5) {
                            bytecodeTags[endIndex - 5].tag = BytecodeTag.NextRepeatTarget;
                            bytecodeTags[endIndex - 5].ownerLoop = startIndex;
                            bytecodeTags[endIndex - 4].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 3].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 2].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 1].tag = BytecodeTag.Skip;
                            bytecodeTags[endIndex - 1].ownerLoop = startIndex;
                        }
                        break;

                    case RepeatWhile:
                        bytecodeTags[endIndex - 1].tag = BytecodeTag.NextRepeatTarget;
                        bytecodeTags[endIndex - 1].ownerLoop = startIndex;
                        break;

                    default:
                        break;
                }
            }
        }

        BytecodeTag identifyLoop(int startIndex, int endIndex) {
            // Check for repeat with in
            if (isRepeatWithIn(startIndex, endIndex)) {
                return BytecodeTag.RepeatWithIn;
            }

            if (startIndex < 1) {
                return BytecodeTag.RepeatWhile;
            }

            List<Bytecode> bytecodeArray = handler.bytecodeArray;

            // Check for repeat with to/downto
            boolean up;
            OpCode prevOp = bytecodeArray.get(startIndex - 1).opcode;
            if (prevOp == OpCode.LtEq) {
                up = true;
            } else if (prevOp == OpCode.GtEq) {
                up = false;
            } else {
                return BytecodeTag.RepeatWhile;
            }

            Bytecode endRepeat = bytecodeArray.get(endIndex - 1);
            int conditionStartPos = endRepeat.pos - (int) endRepeat.obj;
            Integer condStartIndexObj = bytecodePosMap.get(conditionStartPos);
            if (condStartIndexObj == null) {
                return BytecodeTag.RepeatWhile;
            }
            int conditionStartIndex = condStartIndexObj;

            if (conditionStartIndex < 1) {
                return BytecodeTag.RepeatWhile;
            }

            // Verify the set/get pattern
            OpCode setOp = bytecodeArray.get(conditionStartIndex - 1).opcode;
            OpCode getOp;
            switch (setOp) {
                case SetGlobal:
                    getOp = OpCode.GetGlobal;
                    break;
                case SetGlobal2:
                    getOp = OpCode.GetGlobal2;
                    break;
                case SetProp:
                    getOp = OpCode.GetProp;
                    break;
                case SetParam:
                    getOp = OpCode.GetParam;
                    break;
                case SetLocal:
                    getOp = OpCode.GetLocal;
                    break;
                default:
                    return BytecodeTag.RepeatWhile;
            }

            long varId = bytecodeArray.get(conditionStartIndex - 1).obj;

            if (bytecodeArray.get(conditionStartIndex).opcode != getOp ||
                bytecodeArray.get(conditionStartIndex).obj != varId) {
                return BytecodeTag.RepeatWhile;
            }

            if (endIndex < 5) {
                return BytecodeTag.RepeatWhile;
            }

            // Check increment pattern
            int expectedInc = up ? 1 : -1;
            if (bytecodeArray.get(endIndex - 5).opcode != OpCode.PushInt8 ||
                bytecodeArray.get(endIndex - 5).obj != expectedInc) {
                return BytecodeTag.RepeatWhile;
            }

            if (bytecodeArray.get(endIndex - 4).opcode != getOp ||
                bytecodeArray.get(endIndex - 4).obj != varId) {
                return BytecodeTag.RepeatWhile;
            }

            if (bytecodeArray.get(endIndex - 3).opcode != OpCode.Add) {
                return BytecodeTag.RepeatWhile;
            }

            if (bytecodeArray.get(endIndex - 2).opcode != setOp ||
                bytecodeArray.get(endIndex - 2).obj != varId) {
                return BytecodeTag.RepeatWhile;
            }

            return up ? BytecodeTag.RepeatWithTo : BytecodeTag.RepeatWithDownTo;
        }

        boolean isRepeatWithIn(int startIndex, int endIndex) {
            List<Bytecode> bytecodeArray = handler.bytecodeArray;

            if (startIndex < 7 || startIndex + 5 >= bytecodeArray.size()) {
                return false;
            }

            // Check pre-jmpifz pattern
            if (bytecodeArray.get(startIndex - 7).opcode != OpCode.Peek ||
                bytecodeArray.get(startIndex - 7).obj != 0) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 6).opcode != OpCode.PushArgList ||
                bytecodeArray.get(startIndex - 6).obj != 1) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 5).opcode != OpCode.ExtCall ||
                !getName(bytecodeArray.get(startIndex - 5).obj).equals("count")) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 4).opcode != OpCode.PushInt8 ||
                bytecodeArray.get(startIndex - 4).obj != 1) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 3).opcode != OpCode.Peek ||
                bytecodeArray.get(startIndex - 3).obj != 0) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 2).opcode != OpCode.Peek ||
                bytecodeArray.get(startIndex - 2).obj != 2) {
                return false;
            }

            if (bytecodeArray.get(startIndex - 1).opcode != OpCode.LtEq) {
                return false;
            }

            // Check post-jmpifz pattern
            if (bytecodeArray.get(startIndex + 1).opcode != OpCode.Peek ||
                bytecodeArray.get(startIndex + 1).obj != 2) {
                return false;
            }

            if (bytecodeArray.get(startIndex + 2).opcode != OpCode.Peek ||
                bytecodeArray.get(startIndex + 2).obj != 1) {
                return false;
            }

            if (bytecodeArray.get(startIndex + 3).opcode != OpCode.PushArgList ||
                bytecodeArray.get(startIndex + 3).obj != 2) {
                return false;
            }

            if (bytecodeArray.get(startIndex + 4).opcode != OpCode.ExtCall ||
                !getName(bytecodeArray.get(startIndex + 4).obj).equals("getAt")) {
                return false;
            }

            OpCode setOp = bytecodeArray.get(startIndex + 5).opcode;
            if (setOp != OpCode.SetGlobal && setOp != OpCode.SetProp &&
                setOp != OpCode.SetParam && setOp != OpCode.SetLocal) {
                return false;
            }

            // Check end pattern
            if (endIndex < 3) {
                return false;
            }

            if (bytecodeArray.get(endIndex - 3).opcode != OpCode.PushInt8 ||
                bytecodeArray.get(endIndex - 3).obj != 1) {
                return false;
            }

            if (bytecodeArray.get(endIndex - 2).opcode != OpCode.Add) {
                return false;
            }

            if (bytecodeArray.get(endIndex).opcode != OpCode.Pop ||
                bytecodeArray.get(endIndex).obj != 3) {
                return false;
            }

            return true;
        }

        String getVarNameFromSet(int index) {
            Bytecode bytecode = handler.bytecodeArray.get(index);
            switch (bytecode.opcode) {
                case SetGlobal:
                case SetGlobal2:
                case SetProp:
                    return getName(bytecode.obj);
                case SetParam:
                    return getArgumentName(bytecode.obj);
                case SetLocal:
                    return getLocalName(bytecode.obj);
                default:
                    return "unknown";
            }
        }

        /**
         * Parse and translate all bytecode.
         */
        void parse() {
            tagLoops();
            stack.clear();

            int i = 0;
            while (i < handler.bytecodeArray.size()) {
                Bytecode bytecode = handler.bytecodeArray.get(i);
                int pos = bytecode.pos;

                // Exit blocks at their end position
                while (pos == currentBlock.endPos) {
                    exitBlock();
                }

                currentBytecodeIndex = i;
                int translateSize = translateBytecode(i);
                i += translateSize;
            }
        }

        int translateBytecode(int index) {
            BytecodeTag tag = bytecodeTags[index].tag;

            // Skip tagged internal loop bytecode
            if (tag == BytecodeTag.Skip || tag == BytecodeTag.NextRepeatTarget) {
                return 1;
            }

            Bytecode bytecode = handler.bytecodeArray.get(index);
            OpCode opcode = bytecode.opcode;
            long obj = bytecode.obj;

            BlockNode nextBlock = null;
            List<Integer> collectedIndices = new ArrayList<>();
            collectedIndices.add(index);

            AstNode translation = null;

            switch (opcode) {
                case Ret:
                case RetFactory:
                    if (index == handler.bytecodeArray.size() - 1) {
                        translation = null; // end of handler
                    } else {
                        translation = AstNode.exit();
                    }
                    break;

                case PushZero:
                    translation = AstNode.literal(DecompilerDatum.ofInt(0));
                    break;

                case Mul:
                case Add:
                case Sub:
                case Div:
                case Mod:
                case JoinStr:
                case JoinPadStr:
                case Lt:
                case LtEq:
                case NtEq:
                case Eq:
                case Gt:
                case GtEq:
                case And:
                case Or:
                case ContainsStr:
                case Contains0Str: {
                    AstNode b = popWithIndices(collectedIndices);
                    AstNode a = popWithIndices(collectedIndices);
                    translation = AstNode.binaryOp(opcode, a, b);
                    break;
                }

                case Inv: {
                    AstNode x = popWithIndices(collectedIndices);
                    translation = AstNode.inverseOp(x);
                    break;
                }

                case Not: {
                    AstNode x = popWithIndices(collectedIndices);
                    translation = AstNode.notOp(x);
                    break;
                }

                case GetChunk: {
                    AstNode str = popWithIndices(collectedIndices);
                    translation = readChunkRefWithIndices(str, collectedIndices);
                    break;
                }

                case HiliteChunk: {
                    AstNode castIdNode = null;
                    if (version >= 500) {
                        castIdNode = popWithIndices(collectedIndices);
                    }
                    AstNode fieldId = popWithIndices(collectedIndices);
                    AstNode field = AstNode.member("field", fieldId, castIdNode);
                    AstNode chunk = readChunkRefWithIndices(field, collectedIndices);
                    translation = AstNode.chunkHilite(chunk);
                    break;
                }

                case OntoSpr: {
                    AstNode second = popWithIndices(collectedIndices);
                    AstNode first = popWithIndices(collectedIndices);
                    translation = AstNode.spriteIntersects(first, second);
                    break;
                }

                case IntoSpr: {
                    AstNode second = popWithIndices(collectedIndices);
                    AstNode first = popWithIndices(collectedIndices);
                    translation = AstNode.spriteWithin(first, second);
                    break;
                }

                case GetField: {
                    AstNode castIdNode = null;
                    if (version >= 500) {
                        castIdNode = popWithIndices(collectedIndices);
                    }
                    AstNode fieldId = popWithIndices(collectedIndices);
                    translation = AstNode.member("field", fieldId, castIdNode);
                    break;
                }

                case StartTell: {
                    AstNode window = popWithIndices(collectedIndices);
                    BlockNode tellBlock = new BlockNode();
                    translation = AstNode.tell(window, tellBlock);
                    nextBlock = tellBlock;
                    break;
                }

                case EndTell:
                    exitBlock();
                    translation = null;
                    break;

                case PushList: {
                    AstNode listNode = popWithIndices(collectedIndices);
                    if (listNode.nodeType == AstNode.NodeType.Literal && listNode.datum != null) {
                        DecompilerDatum d = listNode.datum.copy();
                        d.datumType = DecompilerDatumType.List;
                        translation = AstNode.literal(d);
                    } else {
                        translation = listNode;
                    }
                    break;
                }

                case PushPropList: {
                    AstNode listNode = popWithIndices(collectedIndices);
                    if (listNode.nodeType == AstNode.NodeType.Literal && listNode.datum != null) {
                        DecompilerDatum d = listNode.datum.copy();
                        d.datumType = DecompilerDatumType.PropList;
                        translation = AstNode.literal(d);
                    } else {
                        translation = listNode;
                    }
                    break;
                }

                case Swap:
                    if (stack.size() >= 2) {
                        int len = stack.size();
                        StackEntry temp = stack.get(len - 1);
                        stack.set(len - 1, stack.get(len - 2));
                        stack.set(len - 2, temp);
                    }
                    translation = null;
                    break;

                case PushInt8:
                case PushInt16:
                case PushInt32:
                    translation = AstNode.literal(DecompilerDatum.ofInt((int) obj));
                    break;

                case PushFloat32: {
                    float f = Float.intBitsToFloat((int) obj);
                    translation = AstNode.literal(DecompilerDatum.ofFloat(f));
                    break;
                }

                case PushArgListNoRet: {
                    int argCount = (int) obj;
                    List<AstNode> args = new ArrayList<>();
                    for (int i = 0; i < argCount; i++) {
                        args.add(0, popWithIndices(collectedIndices));
                    }
                    translation = AstNode.literal(DecompilerDatum.ofArgListNoRet(args));
                    break;
                }

                case PushArgList: {
                    int argCount = (int) obj;
                    List<AstNode> args = new ArrayList<>();
                    for (int i = 0; i < argCount; i++) {
                        args.add(0, popWithIndices(collectedIndices));
                    }
                    translation = AstNode.literal(DecompilerDatum.ofArgList(args));
                    break;
                }

                case PushCons: {
                    int literalId = (int) (obj / multiplier);
                    if (chunk != null && chunk.literals != null && literalId < chunk.literals.size()) {
                        Datum literal = chunk.literals.get(literalId);
                        DecompilerDatum d;
                        switch (literal.getType()) {
                            case String:
                                try {
                                    d = DecompilerDatum.ofString(literal.stringValue());
                                } catch (Exception e) {
                                    d = DecompilerDatum.ofVoid();
                                }
                                break;
                            case Int:
                                try {
                                    d = DecompilerDatum.ofInt(literal.intValue());
                                } catch (Exception e) {
                                    d = DecompilerDatum.ofVoid();
                                }
                                break;
                            case Float:
                                try {
                                    d = DecompilerDatum.ofFloat(literal.floatValue());
                                } catch (Exception e) {
                                    d = DecompilerDatum.ofVoid();
                                }
                                break;
                            case Symbol:
                                try {
                                    d = DecompilerDatum.ofSymbol(literal.symbolValue());
                                } catch (Exception e) {
                                    d = DecompilerDatum.ofVoid();
                                }
                                break;
                            default:
                                d = DecompilerDatum.ofVoid();
                                break;
                        }
                        translation = AstNode.literal(d);
                    } else {
                        translation = AstNode.error();
                    }
                    break;
                }

                case PushSymb:
                    translation = AstNode.literal(DecompilerDatum.ofSymbol(getName(obj)));
                    break;

                case PushVarRef:
                    translation = AstNode.literal(DecompilerDatum.ofVarRef(getName(obj)));
                    break;

                case GetGlobal:
                case GetGlobal2:
                    translation = AstNode.var(getName(obj));
                    break;

                case GetProp:
                    translation = AstNode.var(getName(obj));
                    break;

                case GetParam:
                    translation = AstNode.var(getArgumentName(obj));
                    break;

                case GetLocal:
                    translation = AstNode.var(getLocalName(obj));
                    break;

                case SetGlobal:
                case SetGlobal2: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode var = AstNode.var(getName(obj));
                    translation = AstNode.assignment(var, value, false);
                    break;
                }

                case SetProp: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode var = AstNode.var(getName(obj));
                    translation = AstNode.assignment(var, value, false);
                    break;
                }

                case SetParam: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode var = AstNode.var(getArgumentName(obj));
                    translation = AstNode.assignment(var, value, false);
                    break;
                }

                case SetLocal: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode var = AstNode.var(getLocalName(obj));
                    translation = AstNode.assignment(var, value, false);
                    break;
                }

                case Jmp:
                    translation = translateJmp(index, obj);
                    break;

                case EndRepeat:
                    translation = AstNode.comment("ERROR: Stray endrepeat");
                    break;

                case JmpIfZ: {
                    BlockNode[] nextBlockHolder = new BlockNode[1];
                    translation = translateJmpIfZWithIndices(index, obj, nextBlockHolder, collectedIndices);
                    nextBlock = nextBlockHolder[0];
                    break;
                }

                case LocalCall: {
                    AstNode argList = popWithIndices(collectedIndices);
                    String handlerName;
                    if (chunk != null && chunk.handlers != null && obj < chunk.handlers.size()) {
                        HandlerDef handlerDef = chunk.handlers.get((int) obj);
                        if (lctx != null && handlerDef.nameId < lctx.names.size()) {
                            handlerName = lctx.names.get(handlerDef.nameId);
                        } else {
                            handlerName = "handler_" + obj;
                        }
                    } else {
                        handlerName = "handler_" + obj;
                    }
                    translation = AstNode.call(handlerName, argList);
                    break;
                }

                case ExtCall:
                case TellCall: {
                    String name = getName(obj);
                    AstNode argList = popWithIndices(collectedIndices);
                    translation = AstNode.call(name, argList);
                    break;
                }

                case ObjCallV4: {
                    AstNode argList = popWithIndices(collectedIndices);
                    AstNode object = readVarWithIndices(obj, collectedIndices);
                    translation = AstNode.objCallV4(object, argList);
                    break;
                }

                case Put: {
                    PutType putType = PutType.fromValue((int) ((obj >> 4) & 0xF));
                    long varType = obj & 0xF;
                    AstNode var = readVarWithIndices(varType, collectedIndices);
                    AstNode val = popWithIndices(collectedIndices);
                    translation = AstNode.put(putType, var, val);
                    break;
                }

                case PutChunk: {
                    PutType putType = PutType.fromValue((int) ((obj >> 4) & 0xF));
                    long varType = obj & 0xF;
                    AstNode var = readVarWithIndices(varType, collectedIndices);
                    AstNode chunk = readChunkRefWithIndices(var, collectedIndices);
                    AstNode val = popWithIndices(collectedIndices);
                    translation = AstNode.put(putType, chunk, val);
                    break;
                }

                case DeleteChunk: {
                    AstNode var = readVarWithIndices(obj, collectedIndices);
                    AstNode chunk = readChunkRefWithIndices(var, collectedIndices);
                    translation = AstNode.chunkDelete(chunk);
                    break;
                }

                case Get: {
                    AstNode propId = popWithIndices(collectedIndices);
                    int propIdVal = 0;
                    if (propId.getValue() != null) {
                        propIdVal = propId.getValue().toInt();
                    }
                    translation = readV4Property(obj, propIdVal);
                    break;
                }

                case Set: {
                    AstNode propId = popWithIndices(collectedIndices);
                    AstNode value = popWithIndices(collectedIndices);
                    int propIdVal = 0;
                    if (propId.getValue() != null) {
                        propIdVal = propId.getValue().toInt();
                    }
                    AstNode prop = readV4Property(obj, propIdVal);
                    if (prop != null) {
                        translation = AstNode.assignment(prop, value, true);
                    } else {
                        translation = AstNode.error();
                    }
                    break;
                }

                case GetMovieProp:
                    translation = AstNode.the(getName(obj));
                    break;

                case SetMovieProp: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode prop = AstNode.the(getName(obj));
                    translation = AstNode.assignment(prop, value, false);
                    break;
                }

                case GetObjProp:
                case GetChainedProp: {
                    AstNode object = popWithIndices(collectedIndices);
                    translation = AstNode.objProp(object, getName(obj));
                    break;
                }

                case SetObjProp: {
                    AstNode value = popWithIndices(collectedIndices);
                    AstNode object = popWithIndices(collectedIndices);
                    AstNode prop = AstNode.objProp(object, getName(obj));
                    translation = AstNode.assignment(prop, value, false);
                    break;
                }

                case Peek:
                case Pop:
                    // Handled specially
                    translation = null;
                    break;

                case TheBuiltin:
                    popWithIndices(collectedIndices); // empty arglist
                    translation = AstNode.the(getName(obj));
                    break;

                case ObjCall: {
                    String method = getName(obj);
                    AstNode argList = popWithIndices(collectedIndices);
                    translation = translateObjCall(method, argList);
                    break;
                }

                case PushChunkVarRef:
                    translation = readVarWithIndices(obj, collectedIndices);
                    break;

                case GetTopLevelProp:
                    translation = AstNode.var(getName(obj));
                    break;

                case NewObj: {
                    AstNode objArgs = popWithIndices(collectedIndices);
                    translation = AstNode.newObj(getName(obj), objArgs);
                    break;
                }

                default: {
                    // Unknown opcode
                    int opId = opcode.getValue();
                    String comment;
                    if (opId > 0x40) {
                        comment = String.format("Unknown opcode %02x %d", opId, obj);
                    } else {
                        comment = String.format("Unknown opcode %02x", opId);
                    }
                    stack.clear();
                    translation = AstNode.comment(comment);
                    break;
                }
            }

            if (translation != null) {
                if (translation.isExpression()) {
                    pushWithIndices(translation, collectedIndices);
                } else {
                    addStatement(translation, collectedIndices);
                }
            }

            if (nextBlock != null) {
                enterBlock(nextBlock);
            }

            return 1;
        }

        AstNode translateJmp(int index, long obj) {
            List<Bytecode> bytecodeArray = handler.bytecodeArray;
            Bytecode bytecode = bytecodeArray.get(index);
            int targetPos = bytecode.pos + (int) obj;

            Integer targetIndexObj = bytecodePosMap.get(targetPos);
            if (targetIndexObj == null) {
                return AstNode.comment("ERROR: Invalid jump target");
            }
            int targetIndex = targetIndexObj;

            // Check for exit repeat / next repeat
            if (targetIndex > 0) {
                Bytecode prevBytecode = bytecodeArray.get(targetIndex - 1);
                if (prevBytecode.opcode == OpCode.EndRepeat) {
                    int ownerLoop = bytecodeTags[targetIndex - 1].ownerLoop;
                    if (ownerLoop > 0) {
                        return AstNode.exitRepeat();
                    }
                }
            }

            if (bytecodeTags[targetIndex].tag == BytecodeTag.NextRepeatTarget) {
                return AstNode.nextRepeat();
            }

            // Check for else branch
            if (index + 1 < bytecodeArray.size()) {
                Bytecode nextBytecode = bytecodeArray.get(index + 1);
                if (nextBytecode.pos == currentBlock.endPos) {
                    return null;
                }
            }

            return AstNode.comment("jmp");
        }

        AstNode translateJmpIfZWithIndices(int index, long obj, BlockNode[] nextBlockHolder, List<Integer> indices) {
            Bytecode bytecode = handler.bytecodeArray.get(index);
            int endPos = bytecode.pos + (int) obj;
            BytecodeTag tag = bytecodeTags[index].tag;

            switch (tag) {
                case RepeatWhile: {
                    AstNode condition = popWithIndices(indices);
                    BlockNode block = new BlockNode();
                    block.endPos = endPos;
                    nextBlockHolder[0] = block;
                    return AstNode.repeatWhile(condition, block, index);
                }

                case RepeatWithIn: {
                    AstNode listNode = popWithIndices(indices);
                    String varName = getVarNameFromSet(index + 5);
                    BlockNode block = new BlockNode();
                    block.endPos = endPos;
                    nextBlockHolder[0] = block;
                    return AstNode.repeatWithIn(varName, listNode, block, index);
                }

                case RepeatWithTo:
                case RepeatWithDownTo: {
                    boolean up = (tag == BytecodeTag.RepeatWithTo);
                    AstNode endNode = popWithIndices(indices);
                    AstNode startNode = popWithIndices(indices);

                    List<Bytecode> bytecodeArray = handler.bytecodeArray;
                    Integer endIndexObj = bytecodePosMap.get(endPos);
                    int endIndex = endIndexObj != null ? endIndexObj : index;
                    Bytecode endRepeat = bytecodeArray.get(Math.max(0, endIndex - 1));
                    int conditionStartPos = endRepeat.pos - (int) endRepeat.obj;
                    Integer condStartIndexObj = bytecodePosMap.get(conditionStartPos);
                    int conditionStartIndex = condStartIndexObj != null ? condStartIndexObj : 0;
                    String varName = conditionStartIndex > 0 ? getVarNameFromSet(conditionStartIndex - 1) : "i";

                    BlockNode block = new BlockNode();
                    block.endPos = endPos;
                    nextBlockHolder[0] = block;
                    return AstNode.repeatWithTo(varName, startNode, endNode, up, block, index);
                }

                default: {
                    // Regular if statement
                    AstNode condition = popWithIndices(indices);
                    BlockNode block1 = new BlockNode();
                    block1.endPos = endPos;
                    BlockNode block2 = new BlockNode();
                    nextBlockHolder[0] = block1;
                    return AstNode.ifNode(condition, block1, block2, false);
                }
            }
        }

        AstNode translateObjCall(String method, AstNode argList) {
            if (argList.nodeType == AstNode.NodeType.Literal && argList.datum != null) {
                List<AstNode> args = argList.datum.listValue;
                int nargs = args.size();

                // Handle special method translations
                if (method.equals("getAt") && nargs == 2) {
                    return AstNode.objBracket(args.get(0), args.get(1));
                }
                if (method.equals("setAt") && nargs == 3) {
                    AstNode propExpr = AstNode.objBracket(args.get(0), args.get(1));
                    return AstNode.assignment(propExpr, args.get(2), false);
                }
                if (method.equals("hilite") && nargs == 1) {
                    return AstNode.chunkHilite(args.get(0));
                }
                if (method.equals("delete") && nargs == 1) {
                    return AstNode.chunkDelete(args.get(0));
                }
            }

            return AstNode.objCall(method, argList);
        }

        AstNode readVarWithIndices(long varType, List<Integer> indices) {
            AstNode castIdNode = null;
            if (varType == 0x6 && version >= 500) {
                castIdNode = popWithIndices(indices);
            }
            AstNode id = popWithIndices(indices);

            switch ((int) varType) {
                case 0x1:
                case 0x2:
                case 0x3:
                    return id;
                case 0x4: {
                    if (id.getValue() != null) {
                        String name = getArgumentName(id.getValue().toInt());
                        return AstNode.literal(DecompilerDatum.ofVarRef(name));
                    }
                    return id;
                }
                case 0x5: {
                    if (id.getValue() != null) {
                        String name = getLocalName(id.getValue().toInt());
                        return AstNode.literal(DecompilerDatum.ofVarRef(name));
                    }
                    return id;
                }
                case 0x6:
                    return AstNode.member("field", id, castIdNode);
                default:
                    return AstNode.error();
            }
        }

        AstNode readChunkRefWithIndices(AstNode str, List<Integer> indices) {
            AstNode lastLine = popWithIndices(indices);
            AstNode firstLine = popWithIndices(indices);
            AstNode lastItem = popWithIndices(indices);
            AstNode firstItem = popWithIndices(indices);
            AstNode lastWord = popWithIndices(indices);
            AstNode firstWord = popWithIndices(indices);
            AstNode lastChar = popWithIndices(indices);
            AstNode firstChar = popWithIndices(indices);

            AstNode result = str;

            // Build chunk expression from innermost to outermost
            if (!isZero(firstLine)) {
                result = AstNode.chunkExpr(ChunkExprType.Line, firstLine, lastLine, result);
            }
            if (!isZero(firstItem)) {
                result = AstNode.chunkExpr(ChunkExprType.Item, firstItem, lastItem, result);
            }
            if (!isZero(firstWord)) {
                result = AstNode.chunkExpr(ChunkExprType.Word, firstWord, lastWord, result);
            }
            if (!isZero(firstChar)) {
                result = AstNode.chunkExpr(ChunkExprType.Char, firstChar, lastChar, result);
            }

            return result;
        }

        AstNode readV4Property(long propertyType, int propertyId) {
            switch ((int) propertyType) {
                case 0x00:
                    return AstNode.the(getMoviePropertyName(propertyId));
                case 0x01: {
                    AstNode soundIdNode = AstNode.literal(DecompilerDatum.ofInt(propertyId));
                    return AstNode.soundProp(soundIdNode, 1);
                }
                case 0x02: {
                    AstNode spriteIdNode = AstNode.literal(DecompilerDatum.ofInt(propertyId));
                    return AstNode.spriteProp(spriteIdNode, 0);
                }
                default:
                    return AstNode.comment(String.format("Unknown property type %d id %d", propertyType, propertyId));
            }
        }

        /**
         * Generate output lines from the parsed AST.
         */
        DecompiledHandler generateOutput() {
            CodeWriter code = new CodeWriter();

            // Write handler header
            String name;
            if (lctx != null && handler.nameId < lctx.names.size()) {
                name = lctx.names.get(handler.nameId);
            } else {
                name = "handler_" + handler.nameId;
            }

            List<String> args = new ArrayList<>();
            if (handler.argumentNameIds != null) {
                for (int id : handler.argumentNameIds) {
                    if (lctx != null && id < lctx.names.size()) {
                        args.add(lctx.names.get(id));
                    }
                }
            }

            // Write block contents
            rootBlock.writeScript(code, true, false);

            String output = code.toStringResult();

            // Parse output into lines and create mappings
            DecompiledHandler result = new DecompiledHandler();
            result.name = name;
            result.arguments = args;

            // Split output into lines, filtering empty lines
            String[] outputLines = output.split("\n");
            List<String> nonEmptyLines = new ArrayList<>();
            for (String line : outputLines) {
                if (!line.trim().isEmpty()) {
                    nonEmptyLines.add(line);
                }
            }

            // Match statements to output lines
            if (!statementBytecodeIndices.isEmpty() && !nonEmptyLines.isEmpty()) {
                int statementsCount = statementBytecodeIndices.size();
                int statementIndex = 0;

                for (int lineIdx = 0; lineIdx < nonEmptyLines.size(); lineIdx++) {
                    String line = nonEmptyLines.get(lineIdx);
                    int indent = 0;
                    for (char c : line.toCharArray()) {
                        if (c == ' ') {
                            indent++;
                        } else {
                            break;
                        }
                    }
                    indent /= 2;
                    String text = line.trim();

                    // Check if this is a closing keyword line
                    boolean isClosingLine = text.equals("end if") || text.equals("end repeat") ||
                                          text.equals("end tell") || text.equals("end case") ||
                                          text.equals("else");

                    List<Integer> stmtIndices;
                    if (isClosingLine) {
                        stmtIndices = new ArrayList<>();
                    } else if (statementIndex < statementsCount) {
                        stmtIndices = new ArrayList<>(statementBytecodeIndices.get(statementIndex));
                        statementIndex++;
                    } else {
                        stmtIndices = new ArrayList<>();
                    }

                    // Sort indices
                    Collections.sort(stmtIndices);

                    // Tokenize the line for syntax highlighting
                    List<Span> spans = Tokenizer.tokenizeLine(text);

                    result.lines.add(new DecompiledLine(text, stmtIndices, indent, spans));

                    // Map each bytecode index to this line
                    for (int bcIdx : stmtIndices) {
                        if (!result.bytecodeToLine.containsKey(bcIdx)) {
                            result.bytecodeToLine.put(bcIdx, lineIdx);
                        }
                    }
                }
            } else {
                // Fallback: no statement tracking
                for (int lineIdx = 0; lineIdx < nonEmptyLines.size(); lineIdx++) {
                    String line = nonEmptyLines.get(lineIdx);
                    int indent = 0;
                    for (char c : line.toCharArray()) {
                        if (c == ' ') {
                            indent++;
                        } else {
                            break;
                        }
                    }
                    indent /= 2;
                    String text = line.trim();

                    List<Span> spans = Tokenizer.tokenizeLine(text);

                    result.lines.add(new DecompiledLine(text, new ArrayList<>(), indent, spans));
                }
            }

            return result;
        }
    }

    private static boolean isZero(AstNode node) {
        if (node.nodeType == AstNode.NodeType.Literal && node.datum != null) {
            return node.datum.datumType == DecompilerDatumType.Int && node.datum.intValue == 0;
        }
        return false;
    }

    private static String getMoviePropertyName(int id) {
        switch (id) {
            case 0x01: return "floatPrecision";
            case 0x02: return "mouseDownScript";
            case 0x03: return "mouseUpScript";
            case 0x04: return "keyDownScript";
            case 0x05: return "keyUpScript";
            case 0x06: return "timeoutScript";
            default: return "movieProp_" + id;
        }
    }

    /**
     * Main entry point for decompiling a handler.
     */
    public static DecompiledHandler decompileHandler(
            HandlerDef handler,
            ScriptChunk chunk,
            ScriptContext lctx,
            int version,
            int multiplier) {

        DecompilerState state = new DecompilerState(handler, chunk, lctx, version, multiplier);
        state.parse();
        return state.generateOutput();
    }
}
