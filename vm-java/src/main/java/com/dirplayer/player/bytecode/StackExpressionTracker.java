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

            case PushInt8:
            case PushInt16:
            case PushInt32: {
                String expr = String.valueOf(bytecode.obj);
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PushFloat32: {
                float f = Float.intBitsToFloat((int) bytecode.obj);
                String expr = String.valueOf(f);
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PushZero:
                stack.add("0");
                return "<0>";

            case PushSymb: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "#" + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PushCons: {
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

            case GetLocal: {
                int localIndex = (int) (bytecode.obj / multiplier);
                String name = getLocalName(handler, lctx, localIndex);
                stack.add(name);
                return "<" + name + ">";
            }

            case SetLocal: {
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

            case GetParam: {
                int paramIndex = (int) (bytecode.obj / multiplier);
                String name = getParamName(handler, lctx, paramIndex);
                stack.add(name);
                return "<" + name + ">";
            }

            case SetParam: {
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

            case GetGlobal: {
                String name = getName(lctx, (int) bytecode.obj);
                stack.add(name);
                return "<" + name + ">";
            }

            case SetGlobal: {
                String name = getName(lctx, (int) bytecode.obj);
                if (!stack.isEmpty()) {
                    return "<" + name + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "";
            }

            // ============================================================
            // PROPERTY ACCESS
            // ============================================================

            case GetProp: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "me." + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case SetProp: {
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

            case GetChainedProp: {
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

            case GetObjProp: {
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

            case SetObjProp: {
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

            case GetTopLevelProp: {
                String name = getName(lctx, (int) bytecode.obj);
                String expr = "_global." + name;
                stack.add(expr);
                return "<" + expr + ">";
            }

            // ============================================================
            // THE BUILTIN & MOVIE PROPERTIES
            // ============================================================

            case TheBuiltin: {
                String propName = getBuiltinName((int) bytecode.obj);
                String expr = "the " + propName;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case GetMovieProp: {
                String propName = getMoviePropName((int) bytecode.obj);
                String expr = "the " + propName;
                stack.add(expr);
                return "<" + expr + ">";
            }

            case SetMovieProp: {
                String propName = getMoviePropName((int) bytecode.obj);
                if (!stack.isEmpty()) {
                    return "<the " + propName + " = " + stack.get(stack.size() - 1) + ">";
                }
                return "";
            }

            // ============================================================
            // ARITHMETIC OPERATIONS
            // ============================================================

            case Add:
                return binaryOp("+");
            case Sub:
                return binaryOp("-");
            case Mul:
                return binaryOp("*");
            case Div:
                return binaryOp("/");
            case Mod:
                return binaryOp("mod");

            case Inv: {
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

            case JoinStr:
                return binaryOp("&");
            case JoinPadStr:
                return binaryOp("&&");
            case ContainsStr:
                return binaryOp("contains");
            case Contains0Str:
                return binaryOp("starts");

            case GetChunk: {
                // Pop chunk expression components
                if (stack.size() >= 3) {
                    String endIdx = stack.remove(stack.size() - 1);
                    String startIdx = stack.remove(stack.size() - 1);
                    String obj = stack.remove(stack.size() - 1);
                    String expr = "char " + startIdx + " to " + endIdx + " of " + obj;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            case Put: {
                // put <source> into/after/before <dest>
                if (stack.size() >= 2) {
                    String dest = stack.remove(stack.size() - 1);
                    String source = stack.remove(stack.size() - 1);
                    return "<put " + source + " into " + dest + ">";
                }
                return "";
            }

            case PutChunk: {
                // put <value> into char X to Y of <string>
                if (stack.size() >= 2) {
                    String chunk = stack.remove(stack.size() - 1);
                    String value = stack.remove(stack.size() - 1);
                    return "<put " + value + " into " + chunk + ">";
                }
                return "";
            }

            case DeleteChunk: {
                if (!stack.isEmpty()) {
                    String chunk = stack.remove(stack.size() - 1);
                    return "<delete " + chunk + ">";
                }
                return "";
            }

            // ============================================================
            // COMPARISON OPERATIONS
            // ============================================================

            case Eq:
                return binaryOp("=");
            case NtEq:
                return binaryOp("<>");
            case Lt:
                return binaryOp("<");
            case LtEq:
                return binaryOp("<=");
            case Gt:
                return binaryOp(">");
            case GtEq:
                return binaryOp(">=");

            // ============================================================
            // LOGICAL OPERATIONS
            // ============================================================

            case And:
                return binaryOp("and");
            case Or:
                return binaryOp("or");

            case Not: {
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

            case PushList: {
                int count = lastArgCount;
                List<String> items = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    items.add(0, stack.remove(stack.size() - 1));
                }
                String expr = "[" + String.join(", ", items) + "]";
                stack.add(expr);
                return "<" + expr + ">";
            }

            case PushPropList: {
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

            case PushArgList:
            case PushArgListNoRet:
                lastArgCount = (int) bytecode.obj;
                return "<" + bytecode.obj + ">";

            // ============================================================
            // FUNCTION CALLS
            // ============================================================

            case ExtCall: {
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

            case LocalCall: {
                String name = getName(lctx, (int) bytecode.obj);
                int count = lastArgCount;
                List<String> args = new ArrayList<>();
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    args.add(0, stack.remove(stack.size() - 1));
                }
                return name + "(" + String.join(", ", args) + ")";
            }

            case ObjCall: {
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

            case NewObj: {
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

            case Ret:
                return "exit";

            case JmpIfZ: {
                if (!stack.isEmpty()) {
                    return "if " + stack.get(stack.size() - 1) + " then";
                }
                return "if ? then";
            }

            case Jmp:
            case EndRepeat:
                return "";

            // ============================================================
            // STACK MANIPULATION
            // ============================================================

            case Pop: {
                int count = (int) bytecode.obj;
                for (int i = 0; i < count && !stack.isEmpty(); i++) {
                    stack.remove(stack.size() - 1);
                }
                return count == 1 ? "end case" : "";
            }

            case Swap: {
                if (stack.size() >= 2) {
                    int len = stack.size();
                    String temp = stack.get(len - 1);
                    stack.set(len - 1, stack.get(len - 2));
                    stack.set(len - 2, temp);
                }
                return "";
            }

            case Peek: {
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

            case OntoSpr: {
                if (stack.size() >= 2) {
                    String sprite = stack.remove(stack.size() - 1);
                    String point = stack.remove(stack.size() - 1);
                    String expr = point + " within " + sprite;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            case IntoSpr: {
                if (stack.size() >= 2) {
                    String sprite = stack.remove(stack.size() - 1);
                    String point = stack.remove(stack.size() - 1);
                    String expr = point + " intersects " + sprite;
                    stack.add(expr);
                    return "<" + expr + ">";
                }
                return "";
            }

            // ============================================================
            // FIELD OPERATIONS
            // ============================================================

            case GetField:
                // This is complex - field references
                return "";

            case Set: {
                // Generic set operation
                if (stack.size() >= 2) {
                    String value = stack.remove(stack.size() - 1);
                    String target = stack.remove(stack.size() - 1);
                    return "<" + target + " = " + value + ">";
                }
                return "";
            }

            case Get:
                // Generic get operation
                return "";

            // ============================================================
            // CHUNK VARIABLE REFERENCES
            // ============================================================

            case PushChunkVarRef: {
                // Push a reference to a chunk for later assignment
                if (stack.size() >= 3) {
                    String endIdx = stack.remove(stack.size() - 1);
                    String startIdx = stack.remove(stack.size() - 1);
                    String obj = stack.remove(stack.size() - 1);
                    String expr = "char " + startIdx + " to " + endIdx + " of " + obj;
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
        switch (id) {
            case 0x00: return "floatPrecision";
            case 0x01: return "mouseDownScript";
            case 0x02: return "mouseUpScript";
            case 0x03: return "keyDownScript";
            case 0x04: return "keyUpScript";
            case 0x05: return "timeoutScript";
            case 0x06: return "updateMovieEnabled";
            case 0x07: return "selStart";
            case 0x08: return "selEnd";
            case 0x09: return "soundLevel";
            case 0x0A: return "fixStageSize";
            case 0x0B: return "searchCurrentFolder";
            case 0x0C: return "searchPaths";
            case 0x0D: return "lastClick";
            case 0x0E: return "lastRoll";
            case 0x0F: return "lastEvent";
            case 0x10: return "lastKey";
            case 0x11: return "timeoutLapsed";
            case 0x12: return "multiSound";
            case 0x13: return "soundKeepDevice";
            case 0x14: return "soundMixMedia";
            case 0x15: return "freeBytes";
            case 0x16: return "freeBLock";
            case 0x17: return "maxInteger";
            case 0x18: return "pi";
            case 0x19: return "rightMouseDown";
            case 0x1A: return "optionDown";
            case 0x1B: return "commandDown";
            case 0x1C: return "controlDown";
            case 0x1D: return "shiftDown";
            case 0x1E: return "platform";
            case 0x1F: return "colorDepth";
            case 0x20: return "frame";
            case 0x21: return "movie";
            case 0x22: return "beepOn";
            case 0x23: return "movieName";
            case 0x24: return "moviePath";
            case 0x25: return "movieFileFreeSize";
            case 0x26: return "movieFileSize";
            case 0x27: return "pathName";
            case 0x28: return "systemDate";
            case 0x29: return "applicationPath";
            case 0x2A: return "machinetype";
            case 0x2B: return "productVersion";
            case 0x2C: return "romanLingo";
            case 0x2D: return "version";
            case 0x2E: return "environment";
            case 0x2F: return "deskTopRectList";
            case 0x30: return "colorQD";
            case 0x31: return "quickTimePresent";
            case 0x32: return "memorySize";
            case 0x33: return "checkBoxAccess";
            case 0x34: return "checkBoxType";
            case 0x35: return "lastFrame";
            case 0x36: return "lastClick";
            case 0x37: return "lastRoll";
            case 0x38: return "lastEvent";
            case 0x39: return "lastKey";
            case 0x3A: return "doubleClick";
            case 0x3B: return "keyCode";
            case 0x3C: return "key";
            case 0x3D: return "mouseH";
            case 0x3E: return "mouseV";
            case 0x3F: return "mouseDown";
            case 0x40: return "ticks";
            case 0x41: return "timer";
            case 0x42: return "clickLoc";
            case 0x43: return "rollover";
            case 0x44: return "centerStage";
            case 0x45: return "exitLock";
            case 0x46: return "runMode";
            case 0x47: return "windowPresent";
            case 0x48: return "currentSpriteNum";
            case 0x49: return "puppetSprite";
            case 0x4A: return "pauseState";
            case 0x4B: return "timeoutKeyDown";
            case 0x4C: return "timeoutLength";
            case 0x4D: return "timeoutMouse";
            case 0x4E: return "timeoutPlay";
            case 0x4F: return "perFrameHook";
            case 0x50: return "alertHook";
            case 0x51: return "updateLock";
            case 0x52: return "itemDelimiter";
            case 0x53: return "colorDepth";
            case 0x54: return "switchColorDepth";
            case 0x55: return "maxInteger";
            case 0x56: return "preLoadRAM";
            case 0x57: return "cursor";
            case 0x58: return "keyDownScript";
            case 0x59: return "keyUpScript";
            case 0x5A: return "mouseDownScript";
            case 0x5B: return "mouseUpScript";
            case 0x5C: return "timeoutScript";
            case 0x5D: return "buttonStyle";
            case 0x5E: return "selStart";
            case 0x5F: return "selEnd";
            case 0x60: return "videoForWindowsPresent";
            case 0x61: return "quickTimeVersion";
            case 0x62: return "soundDevice";
            case 0x63: return "soundEnabled";
            case 0x64: return "traceLoad";
            case 0x65: return "traceLogFile";
            case 0x66: return "stageColor";
            case 0x67: return "paramCount";
            case 0x68: return "mouseItem";
            case 0x69: return "mouseWord";
            case 0x6A: return "mouseLine";
            case 0x6B: return "mouseChar";
            case 0x6C: return "menu";
            case 0x6D: return "menuItems";
            case 0x6E: return "locToCharPos";
            case 0x6F: return "charToLoc";
            case 0x70: return "frameTempo";
            case 0x71: return "framePalette";
            case 0x72: return "frameLabel";
            case 0x73: return "frameScript";
            case 0x74: return "scriptExecutionStyle";
            case 0x75: return "selection";
            case 0x76: return "stillDown";
            case 0x77: return "result";
            case 0x78: return "number of castMembers";
            case 0x79: return "number of menus";
            case 0x7A: return "number of menuItems";
            case 0x7B: return "number of chars";
            case 0x7C: return "number of words";
            case 0x7D: return "number of items";
            case 0x7E: return "number of lines";
            case 0x7F: return "number of castLibs";
            default: return "builtin" + id;
        }
    }

    private String getMoviePropName(int id) {
        switch (id) {
            case 0x00: return "beepOn";
            case 0x01: return "buttonStyle";
            case 0x02: return "centerStage";
            case 0x03: return "checkBoxAccess";
            case 0x04: return "checkBoxType";
            case 0x06: return "colorDepth";
            case 0x07: return "colorQD";
            case 0x08: return "exitLock";
            case 0x09: return "floatPrecision";
            case 0x0A: return "frameLabel";
            case 0x0B: return "framePalette";
            case 0x0C: return "frameScript";
            case 0x0D: return "frameTempo";
            case 0x0E: return "itemDelimiter";
            case 0x0F: return "keyDownScript";
            case 0x10: return "keyUpScript";
            case 0x11: return "lastClick";
            case 0x12: return "lastEvent";
            case 0x13: return "lastFrame";
            case 0x14: return "lastKey";
            case 0x15: return "lastRoll";
            case 0x16: return "locToCharPos";
            case 0x17: return "menuItems";
            case 0x18: return "menu";
            case 0x19: return "mouseChar";
            case 0x1A: return "mouseDown";
            case 0x1B: return "mouseDownScript";
            case 0x1C: return "mouseH";
            case 0x1D: return "mouseItem";
            case 0x1E: return "mouseLine";
            case 0x1F: return "mouseMember";
            case 0x20: return "mouseUpScript";
            case 0x21: return "mouseV";
            case 0x22: return "mouseWord";
            case 0x23: return "movieFileFreeSize";
            case 0x24: return "movieFileFreeSize";
            case 0x25: return "movieFileSize";
            case 0x26: return "movieName";
            case 0x27: return "moviePath";
            case 0x28: return "paramCount";
            case 0x29: return "pauseState";
            case 0x2A: return "perFrameHook";
            case 0x2B: return "preloadRAM";
            case 0x2C: return "quickTimePresent";
            case 0x2D: return "rollover";
            case 0x2E: return "romanLingo";
            case 0x2F: return "runMode";
            case 0x30: return "scriptExecutionStyle";
            case 0x31: return "selEnd";
            case 0x32: return "selStart";
            case 0x33: return "soundDevice";
            case 0x34: return "soundEnabled";
            case 0x35: return "soundKeepDevice";
            case 0x36: return "soundLevel";
            case 0x37: return "soundMixMedia";
            case 0x38: return "stageColor";
            case 0x49: return "switchColorDepth";
            case 0x4A: return "timeoutKeyDown";
            case 0x4B: return "timeoutLapsed";
            case 0x4C: return "timeoutLength";
            case 0x4D: return "timeoutMouse";
            case 0x4E: return "timeoutPlay";
            case 0x4F: return "timeoutScript";
            case 0x50: return "timer";
            case 0x51: return "traceLoad";
            case 0x52: return "traceLogFile";
            case 0x53: return "updateMovieEnabled";
            case 0x54: return "videoForWindowsPresent";
            case 0x55: return "floatPrecision";
            case 0xB1: return "currentSpriteNum";
            default: return "movieProp" + id;
        }
    }

    public String getStackTop() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }
}
