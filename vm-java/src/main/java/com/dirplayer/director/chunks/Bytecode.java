package com.dirplayer.director.chunks;

import com.dirplayer.director.lingo.LingoConstants;
import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.director.lingo.ScriptContext;

/**
 * A single bytecode instruction.
 * Port of Rust Bytecode struct.
 */
public class Bytecode {
    public OpCode opcode;
    public long obj;
    public int pos;
    public int ownerLoop;
    public String translation;
    public Integer lineNumber;

    public Bytecode(OpCode opcode, long obj, int pos) {
        this.opcode = opcode;
        this.obj = obj;
        this.pos = pos;
        this.ownerLoop = Integer.MAX_VALUE;
        this.translation = null;
        this.lineNumber = null;
    }

    public static String posToStr(int pos) {
        return "[" + pos + "]";
    }

    public String toBytecodeText(ScriptContext lctx, HandlerDef handler, int multiplier) {
        int opId = opcode.getValue();
        String opcodeName = getOpcodeName(opId);

        StringBuilder writer = new StringBuilder();
        writer.append(posToStr(pos));
        writer.append(" ");
        writer.append(opcodeName);

        switch (opcode) {
            case Jmp:
            case JmpIfZ:
                writer.append(" ");
                writer.append(posToStr(pos + (int) obj));
                break;

            case EndRepeat:
                writer.append(" ");
                writer.append(posToStr(pos - (int) obj));
                break;

            case ObjCall:
            case ExtCall:
            case GetObjProp:
            case SetObjProp:
            case PushSymb:
            case GetProp:
            case GetChainedProp:
            case GetGlobal:
            case SetGlobal:
                if (lctx != null && obj < lctx.names.size()) {
                    String name = lctx.names.get((int) obj);
                    writer.append(" ");
                    writer.append(name);
                }
                break;

            case SetLocal:
            case GetLocal:
                int localIndex = (int) (obj / multiplier);
                if (handler != null && localIndex < handler.localNameIds.size()) {
                    int nameId = handler.localNameIds.get(localIndex);
                    if (lctx != null && nameId < lctx.names.size()) {
                        String name = lctx.names.get(nameId);
                        writer.append(" ");
                        writer.append(name);
                    } else {
                        writer.append(" UNKNOWN_LOCAL");
                    }
                }
                break;

            case PushFloat32:
                writer.append(" ");
                float f = Float.intBitsToFloat((int) obj);
                writer.append(f);
                break;

            default:
                if (opId > 0x40) {
                    writer.append(" ");
                    writer.append(obj);
                }
                break;
        }

        return writer.toString();
    }

    public String toBytecodeTextWithAnnotation(
            ScriptContext lctx,
            HandlerDef handler,
            int multiplier,
            String annotation) {

        int opId = opcode.getValue();
        String opcodeName = getOpcodeName(opId);

        StringBuilder writer = new StringBuilder();

        // Position
        writer.append(String.format("[%3d] ", pos));

        // Opcode and operand
        writer.append(opcodeName);

        switch (opcode) {
            case SetLocal:
            case GetLocal:
                int localIndex = (int) (obj / multiplier);
                if (handler != null && localIndex < handler.localNameIds.size()) {
                    int nameId = handler.localNameIds.get(localIndex);
                    if (lctx != null && nameId < lctx.names.size()) {
                        writer.append(" ");
                        writer.append(lctx.names.get(nameId));
                    } else {
                        writer.append(" UNKNOWN");
                    }
                }
                break;

            default:
                if (opId > 0x40) {
                    writer.append(" ");
                    writer.append(obj);
                }
                break;
        }

        // Padding dots
        int currentLen = writer.length();
        int targetLen = 40;
        if (currentLen < targetLen) {
            for (int i = 0; i < targetLen - currentLen; i++) {
                writer.append(".");
            }
        }

        // Annotation
        if (annotation != null && !annotation.isEmpty()) {
            writer.append(" ");
            writer.append(annotation);
        }

        return writer.toString();
    }

    public static String getOpcodeName(int id) {
        if (id >= 0x40) {
            id = 0x40 + (id % 0x40);
        }

        OpCode opcode = OpCode.from(id);
        String name = LingoConstants.getOpcodeName(opcode);
        return name != null ? name : "UNKNOWN_BYTECODE";
    }
}
