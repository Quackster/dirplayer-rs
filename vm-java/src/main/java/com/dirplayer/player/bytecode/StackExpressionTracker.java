package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.director.lingo.ScriptContext;
import com.dirplayer.director.chunks.Bytecode;
import com.dirplayer.director.chunks.HandlerDef;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks stack expressions for debugging/tracing bytecode execution.
 * Port of Rust StackExpressionTracker struct.
 */
public class StackExpressionTracker {
    private List<String> stack;
    private int lastArgCount;

    public StackExpressionTracker() {
        this.stack = new ArrayList<>();
        this.lastArgCount = 0;
    }

    public void clear() {
        stack.clear();
        lastArgCount = 0;
    }

    /**
     * Process a bytecode and return its annotation.
     */
    public String processBytecode(
        Bytecode bytecode,
        ScriptContext lctx,
        HandlerDef handler,
        int multiplier,
        List<Datum> literals
    ) {
        OpCode opcode = bytecode.opcode;

        switch (opcode) {
            // ============================================================
            // PUSH OPERATIONS
            // ============================================================

            case PUSH_INT8:
            case PUSH_INT16:
            case PUSH_INT32: {
                String expr = String.valueOf(bytecode.obj);
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PUSH_FLOAT32: {
                float f = Float.intBitsToFloat((int) bytecode.obj);
                String expr = String.valueOf(f);
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PUSH_ZERO:
                stack.add("0");
                return "<0>";

            case PUSH_SYMB: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "#" + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PUSH_CONS: {
                int literalId = (int) (bytecode.obj / multiplier);
                if (literalId < literals.size()) {
                    Datum literal = literals.get(literalId);
                    String expr = formatLiteral(literal);
                    stack.add(expr);
                    return "<" + expr + ">";
                } else {
                    String expr = "CONST[" + literalId + "]";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
            }

            // ============================================================
            // VARIABLE ACCESS - LOCAL
            // ============================================================

            case GET_LOCAL: {
                int localIndex = (int) (bytecode.obj / multiplier);
                String name = getLocalName(handler, lctx, localIndex);
                stack.add(name);
                return "<" + name + ">";
            }

            case SET_LOCAL: {
                int localIndex = (int) (bytecode.obj / multiplier);
                String name = getLocalName(handler, lctx, localIndex);
                if (!stack.isEmpty()) {
                    return "<" + name + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "<" + name + " = ?>";
            }

            // ============================================================
            // VARIABLE ACCESS - PARAMETER
            // ============================================================

            case GET_PARAM: {
                int paramIndex = (int) (bytecode.obj / multiplier);
                String name = getParamName(handler, lctx, paramIndex);
                stack.add(name);
                return "<" + name + ">";
            }

            case SET_PARAM: {
                int paramIndex = (int) (bytecode.obj / multiplier);
                String name = getParamName(handler, lctx, paramIndex);
                if (!stack.isEmpty()) {
                    return "<" + name + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "<" + name + " = ?>";
            }

            // ============================================================
            // VARIABLE ACCESS - GLOBAL
            // ============================================================

            case GET_GLOBAL: {
                String name = getName(lctx, (int) bytecode.obj);
                stack.add(name);
                return "<" + name + ">";
            }

            case SET_GLOBAL: {
                String name = getName(lctx, (int) bytecode.obj);
                if (!stack.isEmpty()) {
                    return "<" + name + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "";
            }

            // ============================================================
            // PROPERTY ACCESS
            // ============================================================

            case GET_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "me." + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case SET_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                if (stack.size() >= 2) {
                    String value = stack.remove(stack.size() - 1);
                    String obj = stack.remove(stack.size() - 1);
                    return "<" + obj + "." + name + " = " + value + ">";
                } else if (stack.size() == 1) {
                    String value = stack.remove(stack.size() - 1);
                    return "<me." + name + " = " + value + ">";
                }
                return "";
            }

            case GET_CHAINED_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                if (stack.isEmpty()) {
                    String expr = "me." + name;
                    stack.add(expr);
                    return "<" + expr + ">";
                } else {
                    String obj = stack.remove(stack.size() - 1);
                    String expr = obj + "." + name;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
            }

            case GET_OBJ_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                if (stack.isEmpty()) {
                    String expr = "me[#" + name + "]";
                    stack.add(expr);
                    return "<" + expr + ">";
                } else {
                    String obj = stack.remove(stack.size() - 1);
                    String expr = obj + "[#" + name + "]";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
            }

            case SET_OBJ_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                if (stack.size() >= 2) {
                    String value = stack.remove(stack.size() - 1);
                    String obj = stack.remove(stack.size() - 1);
                    return "<" + obj + "[#" + name + "] = " + value + ">";
                } else if (stack.size() == 1) {
                    String value = stack.remove(stack.size() - 1);
                    return "<me[#" + name + "] = " + value + ">";
                }
                return "";
            }

            case GET_TOP_LEVEL_PROP: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "_global." + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            // ============================================================
            // THE BUILTIN & MOVIE PROPERTIES
            // ============================================================

            case THE_BUILTIN: {
                String propName = getBuiltinName((int) bytecode.obj);
                String expr = "the " + propName;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case GET_MOVIE_PROP: {
                String propName = getMoviePropName((int) bytecode.obj);
                String expr = "the " + propName;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case SET_MOVIE_PROP: {
                String propName = getMoviePropName((int) bytecode.obj);
                if (!stack.isEmpty()) {
                    return "<the " + propName + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "";
            }

            // ============================================================
            // ARITHMETIC OPERATIONS
            // ============================================================

            case ADD:
                return binaryOp("+");
            case SUB:
                return binaryOp("-");
            case MUL:
                return binaryOp("*");
            case DIV:
                return binaryOp("/");
            case MOD:
                return binaryOp("mod");

            case INV: {
                if (!stack.isEmpty()) {
                    String a = stack.remove(stack.size() - 1);
                    String expr = "-(" + a + ")";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            // ============================================================
            // STRING OPERATIONS
            // ============================================================

            case JOIN_STR:
                return binaryOp("&");
            case JOIN_PAD_STR:
                return binaryOp("&&");
            case CONTAINS_STR:
                return binaryOp("contains");
            case CONTAINS_0_STR:
                return binaryOp("starts");

            // ============================================================
            // COMPARISON OPERATIONS
            // ============================================================

            case EQ:
                return binaryOp("=");
            case NT_EQ:
                return binaryOp("<>");
            case LT:
                return binaryOp("<");
            case LT_EQ:
                return binaryOp("<=");
            case GT:
                return binaryOp(">");
            case GT_EQ:
                return binaryOp(">=");

            // ============================================================
            // LOGICAL OPERATIONS
            // ============================================================

            case AND:
                return binaryOp("and");
            case OR:
                return binaryOp("or");

            case NOT: {
                if (!stack.isEmpty()) {
                    String a = stack.remove(stack.size() - 1);
                    String expr = "not (" + a + ")";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            // ============================================================
            // LIST OPERATIONS
            // ============================================================

            case PUSH_LIST: {
                int count = lastArgCount;
                List<String> items = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    items.add(0, stack.remove(stack.size() - 1));
                }
                String expr = "[" + String.join(", ", items) + "]";
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PUSH_PROP_LIST: {
                int count = lastArgCount / 2;
                List<String> items = new ArrayList<>();
                for (int i = 0; i < count && stack.size() >= 2; i++) {
                    String value = stack.remove(stack.size() - 1);
                    String key = stack.remove(stack.size() - 1);
                    items.add(0, key + ": " + value);
                }
                String expr = "[" + String.join(", ", items) + "]";
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PUSH_ARG_LIST:
            case PUSH_ARG_LIST_NO_RET:
                lastArgCount = (int) bytecode.obj;
                return "<" + bytecode.obj + ">";

            // ============================================================
            // FUNCTION CALLS
            // ============================================================

            case EXT_CALL: {
                String name = getName(lctx, (int) bytecode.obj);
                int count = lastArgCount;
                List<String> args = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    args.add(0, stack.remove(stack.size() - 1));
                }
                String expr = name + "(" + String.join(", ", args) + ")";
                stack.add(expr);
                return expr;
            }

            case LOCAL_CALL: {
                String name = getName(lctx, (int) bytecode.obj);
                int count = lastArgCount;
                List<String> args = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    args.add(0, stack.remove(stack.size() - 1));
                }
                return name + "(" + String.join(", ", args) + ")";
            }

            case OBJ_CALL: {
                String name = getName(lctx, (int) bytecode.obj);
                int count = lastArgCount;
                List<String> args = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    args.add(0, stack.remove(stack.size() - 1));
                }
                if (!stack.isEmpty()) {
                    String obj = stack.remove(stack.size() - 1);
                    String expr = obj + "." + name + "(" + String.join(", ", args) + ")";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "<?." + name + "(...)>";
            }

            // ============================================================
            // OBJECT CREATION
            // ============================================================

            case NEW_OBJ: {
                if (!stack.isEmpty()) {
                    String objType = stack.remove(stack.size() - 1);
                    int count = lastArgCount;
                    List<String> args = new ArrayList<>();
                    for (int i = 0; i < count && !stack.isEmpty(); i++) {
                        args.add(0, stack.remove(stack.size() - 1));
                    }
                    String expr = args.isEmpty() ?
                        "new(" + objType + ")" :
                        "new(" + objType + ", " + String.join(", ", args) + ")";
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            // ============================================================
            // CONTROL FLOW
            // ============================================================

            case RET:
                return "exit";

            case JMP_IF_Z: {
                if (!stack.isEmpty()) {
                    return "if " + stack.get(stack.size() - 1) + " then";
                }
                return "if ? then";
            }

            case JMP:
            case END_REPEAT:
                return "";

            // ============================================================
            // STACK MANIPULATION
            // ============================================================

            case POP: {
                int count = (int) bytecode.obj;
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    stack.remove(stack.size() - 1);
                }
                return count == 1 ? "end case" : "";
            }

            case SWAP: {
                if (stack.size() >= 2) {
                    int len = stack.size();
                    String temp = stack.get(len - 1);
                    stack.set(len - 1, stack.get(len - 2));
                    stack.set(len - 2, temp);
                }
                return "";
            }

            case PEEK: {
                int offset = (int) bytecode.obj;
                if (offset < stack.size()) {
                    int idx = stack.size() - 1 - offset;
                    stack.add(stack.get(idx));
                }
                return "<peek " + offset + ">";
            }

            // ============================================================
            // SPRITE OPERATIONS
            // ============================================================

            case ONTO_SPR: {
                if (stack.size() >= 2) {
                    String sprite = stack.remove(stack.size() - 1);
                    String point = stack.remove(stack.size() - 1);
                    String expr = point + " within " + sprite;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            case INTO_SPR: {
                if (stack.size() >= 2) {
                    String sprite = stack.remove(stack.size() - 1);
                    String point = stack.remove(stack.size() - 1);
                    String expr = point + " intersects " + sprite;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            default:
                return "";
        }
    }

    private String binaryOp(String op) {
        if (stack.size() >= 2) {
            String b = stack.remove(stack.size() - 1);
            String a = stack.remove(stack.size() - 1);
            String expr = a + " " + op + " " + b;
            stack.add(expr);
            return "<" + expr + ">";
        }
        return "";
    }

    private static String formatLiteral(Datum literal) {
        if (literal.isString()) {
            return "\"" + literal.stringValue().replace("\"", "\\\"") + "\"";
        } else if (literal.isSymbol()) {
            return "#" + literal.stringValue();
        } else if (literal.isInt()) {
            return String.valueOf(literal.intValue());
        } else if (literal.isFloat()) {
            return String.valueOf(literal.floatValue());
        } else if (literal.isList()) {
            return "[...]";
        } else if (literal.isPropList()) {
            return "[...]";
        } else if (literal.isVoid()) {
            return "VOID";
        } else {
            return "?";
        }
    }

    private String getName(ScriptContext lctx, int id) {
        if (lctx != null && lctx.names != null && id < lctx.names.size()) {
            return lctx.names.get(id);
        }
        return "UNKNOWN";
    }

    private String getLocalName(HandlerDef handler, ScriptContext lctx, int localIndex) {
        if (handler.localNameIds != null && localIndex < handler.localNameIds.size()) {
            int nameId = handler.localNameIds.get(localIndex);
            return getName(lctx, nameId);
        }
        return "UNKNOWN";
    }

    private String getParamName(HandlerDef handler, ScriptContext lctx, int paramIndex) {
        if (handler.argumentNameIds != null && paramIndex < handler.argumentNameIds.size()) {
            int nameId = handler.argumentNameIds.get(paramIndex);
            return getName(lctx, nameId);
        }
        return "UNKNOWN";
    }

    private String getBuiltinName(int id) {
        // Simplified version - full list in Rust source
        switch (id) {
            case 0x00: return "floatPrecision";
            case 0x0D: return "lastClick";
            case 0x20: return "frame";
            case 0x21: return "movie";
            case 0x3D: return "mouseH";
            case 0x3E: return "mouseV";
            case 0x3F: return "mouseDown";
            case 0x40: return "ticks";
            case 0x41: return "timer";
            case 0x52: return "itemDelimiter";
            case 0x67: return "paramCount";
            case 0x77: return "result";
            default: return "builtin" + id;
        }
    }

    private String getMoviePropName(int id) {
        // Simplified version - full list in Rust source
        switch (id) {
            case 0x00: return "beepOn";
            case 0x02: return "centerStage";
            case 0x06: return "colorDepth";
            case 0x08: return "exitLock";
            case 0x0E: return "itemDelimiter";
            case 0x26: return "movieName";
            case 0x27: return "moviePath";
            case 0xB1: return "currentSpriteNum";
            default: return "movieProp" + id;
        }
    }

    public String getStackTop() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }
}
