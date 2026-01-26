package com.dirplayer.director.lingo.decompiler;

import com.dirplayer.director.lingo.decompiler.DecompilerEnums.DecompilerDatumType;
import java.util.ArrayList;
import java.util.List;

/**
 * Datum represents values in the decompiler.
 * This is separate from the runtime Datum class.
 * Port of Rust Datum struct from decompiler ast.rs.
 */
public class DecompilerDatum {
    private static final int MAX_WRITE_DEPTH = 100;

    public DecompilerDatumType datumType;
    public int intValue;
    public double floatValue;
    public String stringValue;
    public List<AstNode> listValue;

    private DecompilerDatum() {
        this.stringValue = "";
        this.listValue = new ArrayList<>();
    }

    /**
     * Create a void datum.
     */
    public static DecompilerDatum ofVoid() {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.Void;
        return d;
    }

    /**
     * Create an integer datum.
     */
    public static DecompilerDatum ofInt(int val) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.Int;
        d.intValue = val;
        return d;
    }

    /**
     * Create a float datum.
     */
    public static DecompilerDatum ofFloat(double val) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.Float;
        d.floatValue = val;
        return d;
    }

    /**
     * Create a string datum.
     */
    public static DecompilerDatum ofString(String val) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.String;
        d.stringValue = val;
        return d;
    }

    /**
     * Create a symbol datum.
     */
    public static DecompilerDatum ofSymbol(String val) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.Symbol;
        d.stringValue = val;
        return d;
    }

    /**
     * Create a variable reference datum.
     */
    public static DecompilerDatum ofVarRef(String val) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.VarRef;
        d.stringValue = val;
        return d;
    }

    /**
     * Create a list datum.
     */
    public static DecompilerDatum ofList(List<AstNode> items) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.List;
        d.listValue = new ArrayList<>(items);
        return d;
    }

    /**
     * Create an argument list datum.
     */
    public static DecompilerDatum ofArgList(List<AstNode> items) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.ArgList;
        d.listValue = new ArrayList<>(items);
        return d;
    }

    /**
     * Create an argument list (no return) datum.
     */
    public static DecompilerDatum ofArgListNoRet(List<AstNode> items) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.ArgListNoRet;
        d.listValue = new ArrayList<>(items);
        return d;
    }

    /**
     * Create a property list datum.
     */
    public static DecompilerDatum ofPropList(List<AstNode> items) {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = DecompilerDatumType.PropList;
        d.listValue = new ArrayList<>(items);
        return d;
    }

    /**
     * Convert to integer value.
     */
    public int toInt() {
        switch (datumType) {
            case Int:
                return intValue;
            case Float:
                return (int) floatValue;
            default:
                return 0;
        }
    }

    /**
     * Write the script representation of this datum.
     */
    public void writeScript(CodeWriter code, boolean dot, boolean sum) {
        writeScriptWithDepth(code, dot, sum, 0);
    }

    void writeScriptWithDepth(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (depth > MAX_WRITE_DEPTH) {
            code.write("/* MAX DEPTH */");
            return;
        }

        switch (datumType) {
            case Void:
                code.write("VOID");
                break;

            case Int:
                code.write(String.valueOf(intValue));
                break;

            case Float:
                String s = String.format("%.4f", floatValue);
                // Remove trailing zeros but keep at least one decimal place
                while (s.endsWith("0") && !s.endsWith(".0")) {
                    s = s.substring(0, s.length() - 1);
                }
                code.write(s);
                break;

            case String:
                code.write("\"");
                code.write(escapeString(stringValue));
                code.write("\"");
                break;

            case Symbol:
                code.write("#");
                code.write(stringValue);
                break;

            case VarRef:
                code.write(stringValue);
                break;

            case List:
                code.write("[");
                for (int i = 0; i < listValue.size(); i++) {
                    if (i > 0) {
                        code.write(", ");
                    }
                    listValue.get(i).writeScriptWithDepth(code, dot, false, depth + 1);
                }
                code.write("]");
                break;

            case ArgList:
            case ArgListNoRet:
                for (int i = 0; i < listValue.size(); i++) {
                    if (i > 0) {
                        code.write(", ");
                    }
                    listValue.get(i).writeScriptWithDepth(code, dot, false, depth + 1);
                }
                break;

            case PropList:
                code.write("[");
                int i = 0;
                while (i + 1 < listValue.size()) {
                    if (i > 0) {
                        code.write(", ");
                    }
                    listValue.get(i).writeScriptWithDepth(code, dot, false, depth + 1);
                    code.write(": ");
                    listValue.get(i + 1).writeScriptWithDepth(code, dot, false, depth + 1);
                    i += 2;
                }
                if (listValue.isEmpty()) {
                    code.write(":");
                }
                code.write("]");
                break;
        }
    }

    private static String escapeString(String s) {
        StringBuilder result = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }

    /**
     * Clone this datum.
     */
    public DecompilerDatum copy() {
        DecompilerDatum d = new DecompilerDatum();
        d.datumType = this.datumType;
        d.intValue = this.intValue;
        d.floatValue = this.floatValue;
        d.stringValue = this.stringValue;
        d.listValue = new ArrayList<>(this.listValue);
        return d;
    }
}
