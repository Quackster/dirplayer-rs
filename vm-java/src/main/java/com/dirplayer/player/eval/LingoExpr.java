package com.dirplayer.player.eval;

import com.dirplayer.player.ColorRef;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstract syntax tree nodes for Lingo expressions.
 * Port of Rust LingoExpr enum.
 */
public abstract class LingoExpr {

    /**
     * Get the expression type name for debugging.
     */
    public abstract String getExprType();

    // ============ Literal Expressions ============

    public static class SymbolLiteral extends LingoExpr {
        public final String value;
        public SymbolLiteral(String value) { this.value = value; }
        @Override public String getExprType() { return "SymbolLiteral"; }
        @Override public String toString() { return "#" + value; }
    }

    public static class StringLiteral extends LingoExpr {
        public final String value;
        public StringLiteral(String value) { this.value = value; }
        @Override public String getExprType() { return "StringLiteral"; }
        @Override public String toString() { return "\"" + value + "\""; }
    }

    public static class IntLiteral extends LingoExpr {
        public final int value;
        public IntLiteral(int value) { this.value = value; }
        @Override public String getExprType() { return "IntLiteral"; }
        @Override public String toString() { return String.valueOf(value); }
    }

    public static class FloatLiteral extends LingoExpr {
        public final double value;
        public FloatLiteral(double value) { this.value = value; }
        @Override public String getExprType() { return "FloatLiteral"; }
        @Override public String toString() { return String.valueOf(value); }
    }

    public static class BoolLiteral extends LingoExpr {
        public final boolean value;
        public BoolLiteral(boolean value) { this.value = value; }
        @Override public String getExprType() { return "BoolLiteral"; }
        @Override public String toString() { return value ? "TRUE" : "FALSE"; }
    }

    public static class VoidLiteral extends LingoExpr {
        public static final VoidLiteral INSTANCE = new VoidLiteral();
        private VoidLiteral() {}
        @Override public String getExprType() { return "VoidLiteral"; }
        @Override public String toString() { return "VOID"; }
    }

    public static class ListLiteral extends LingoExpr {
        public final List<LingoExpr> items;
        public ListLiteral(List<LingoExpr> items) {
            this.items = items != null ? items : new ArrayList<>();
        }
        @Override public String getExprType() { return "ListLiteral"; }
        @Override public String toString() { return "[" + items.size() + " items]"; }
    }

    public static class PropListLiteral extends LingoExpr {
        public final List<PropListEntry> entries;
        public PropListLiteral(List<PropListEntry> entries) {
            this.entries = entries != null ? entries : new ArrayList<>();
        }
        @Override public String getExprType() { return "PropListLiteral"; }
        @Override public String toString() { return "[:" + entries.size() + " entries]"; }
    }

    public static class PropListEntry {
        public final LingoExpr key;
        public final LingoExpr value;
        public PropListEntry(LingoExpr key, LingoExpr value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class ColorLiteral extends LingoExpr {
        public final ColorRef color;
        public ColorLiteral(ColorRef color) { this.color = color; }
        @Override public String getExprType() { return "ColorLiteral"; }
        @Override public String toString() { return color.toString(); }
    }

    public static class RectLiteral extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr top;
        public final LingoExpr right;
        public final LingoExpr bottom;
        public RectLiteral(LingoExpr left, LingoExpr top, LingoExpr right, LingoExpr bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        @Override public String getExprType() { return "RectLiteral"; }
        @Override public String toString() { return "rect(...)"; }
    }

    public static class PointLiteral extends LingoExpr {
        public final LingoExpr x;
        public final LingoExpr y;
        public PointLiteral(LingoExpr x, LingoExpr y) {
            this.x = x;
            this.y = y;
        }
        @Override public String getExprType() { return "PointLiteral"; }
        @Override public String toString() { return "point(...)"; }
    }

    // ============ Reference Expressions ============

    public static class Identifier extends LingoExpr {
        public final String name;
        public Identifier(String name) { this.name = name; }
        @Override public String getExprType() { return "Identifier"; }
        @Override public String toString() { return name; }
    }

    public static class MemberRef extends LingoExpr {
        public final LingoExpr memberExpr;
        public final LingoExpr castLibExpr;  // nullable
        public MemberRef(LingoExpr memberExpr, LingoExpr castLibExpr) {
            this.memberExpr = memberExpr;
            this.castLibExpr = castLibExpr;
        }
        @Override public String getExprType() { return "MemberRef"; }
        @Override public String toString() { return "member(...)"; }
    }

    // ============ Binary Operations ============

    public static class Add extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Add(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Add"; }
        @Override public String toString() { return "(" + left + " + " + right + ")"; }
    }

    public static class Subtract extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Subtract(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Subtract"; }
        @Override public String toString() { return "(" + left + " - " + right + ")"; }
    }

    public static class Multiply extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Multiply(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Multiply"; }
        @Override public String toString() { return "(" + left + " * " + right + ")"; }
    }

    public static class Divide extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Divide(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Divide"; }
        @Override public String toString() { return "(" + left + " / " + right + ")"; }
    }

    public static class Mod extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Mod(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Mod"; }
        @Override public String toString() { return "(" + left + " mod " + right + ")"; }
    }

    // ============ String Operations ============

    public static class Join extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Join(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Join"; }
        @Override public String toString() { return "(" + left + " & " + right + ")"; }
    }

    public static class JoinPad extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public JoinPad(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "JoinPad"; }
        @Override public String toString() { return "(" + left + " && " + right + ")"; }
    }

    // ============ Comparison Operations ============

    public static class Eq extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Eq(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Eq"; }
        @Override public String toString() { return "(" + left + " = " + right + ")"; }
    }

    public static class Ne extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Ne(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Ne"; }
        @Override public String toString() { return "(" + left + " <> " + right + ")"; }
    }

    public static class Lt extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Lt(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Lt"; }
        @Override public String toString() { return "(" + left + " < " + right + ")"; }
    }

    public static class Gt extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Gt(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Gt"; }
        @Override public String toString() { return "(" + left + " > " + right + ")"; }
    }

    public static class Le extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Le(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Le"; }
        @Override public String toString() { return "(" + left + " <= " + right + ")"; }
    }

    public static class Ge extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Ge(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Ge"; }
        @Override public String toString() { return "(" + left + " >= " + right + ")"; }
    }

    // ============ Logical Operations ============

    public static class And extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public And(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "And"; }
        @Override public String toString() { return "(" + left + " and " + right + ")"; }
    }

    public static class Or extends LingoExpr {
        public final LingoExpr left;
        public final LingoExpr right;
        public Or(LingoExpr left, LingoExpr right) { this.left = left; this.right = right; }
        @Override public String getExprType() { return "Or"; }
        @Override public String toString() { return "(" + left + " or " + right + ")"; }
    }

    public static class Not extends LingoExpr {
        public final LingoExpr operand;
        public Not(LingoExpr operand) { this.operand = operand; }
        @Override public String getExprType() { return "Not"; }
        @Override public String toString() { return "(not " + operand + ")"; }
    }

    // ============ Unary Operations ============

    public static class Negate extends LingoExpr {
        public final LingoExpr operand;
        public Negate(LingoExpr operand) { this.operand = operand; }
        @Override public String getExprType() { return "Negate"; }
        @Override public String toString() { return "(-" + operand + ")"; }
    }

    // ============ Access Expressions ============

    public static class ObjProp extends LingoExpr {
        public final LingoExpr object;
        public final String propName;
        public ObjProp(LingoExpr object, String propName) {
            this.object = object;
            this.propName = propName;
        }
        @Override public String getExprType() { return "ObjProp"; }
        @Override public String toString() { return object + "." + propName; }
    }

    public static class ListAccess extends LingoExpr {
        public final LingoExpr list;
        public final LingoExpr index;
        public ListAccess(LingoExpr list, LingoExpr index) {
            this.list = list;
            this.index = index;
        }
        @Override public String getExprType() { return "ListAccess"; }
        @Override public String toString() { return list + "[" + index + "]"; }
    }

    public static class ThePropOf extends LingoExpr {
        public final LingoExpr object;
        public final String propName;
        public ThePropOf(LingoExpr object, String propName) {
            this.object = object;
            this.propName = propName;
        }
        @Override public String getExprType() { return "ThePropOf"; }
        @Override public String toString() { return "the " + propName + " of " + object; }
    }

    // ============ Call Expressions ============

    public static class HandlerCall extends LingoExpr {
        public final String handlerName;
        public final List<LingoExpr> args;
        public HandlerCall(String handlerName, List<LingoExpr> args) {
            this.handlerName = handlerName;
            this.args = args != null ? args : new ArrayList<>();
        }
        @Override public String getExprType() { return "HandlerCall"; }
        @Override public String toString() { return handlerName + "(" + args.size() + " args)"; }
    }

    public static class ObjHandlerCall extends LingoExpr {
        public final LingoExpr object;
        public final String handlerName;
        public final List<LingoExpr> args;
        public ObjHandlerCall(LingoExpr object, String handlerName, List<LingoExpr> args) {
            this.object = object;
            this.handlerName = handlerName;
            this.args = args != null ? args : new ArrayList<>();
        }
        @Override public String getExprType() { return "ObjHandlerCall"; }
        @Override public String toString() { return object + "." + handlerName + "(" + args.size() + " args)"; }
    }

    // ============ Assignment Expressions ============

    public static class Assignment extends LingoExpr {
        public final LingoExpr target;
        public final LingoExpr value;
        public Assignment(LingoExpr target, LingoExpr value) {
            this.target = target;
            this.value = value;
        }
        @Override public String getExprType() { return "Assignment"; }
        @Override public String toString() { return target + " = " + value; }
    }

    // ============ Put Expressions ============

    public static class PutInto extends LingoExpr {
        public final LingoExpr value;
        public final LingoExpr target;
        public PutInto(LingoExpr value, LingoExpr target) {
            this.value = value;
            this.target = target;
        }
        @Override public String getExprType() { return "PutInto"; }
        @Override public String toString() { return "put " + value + " into " + target; }
    }

    public static class PutBefore extends LingoExpr {
        public final LingoExpr value;
        public final LingoExpr target;
        public PutBefore(LingoExpr value, LingoExpr target) {
            this.value = value;
            this.target = target;
        }
        @Override public String getExprType() { return "PutBefore"; }
        @Override public String toString() { return "put " + value + " before " + target; }
    }

    public static class PutAfter extends LingoExpr {
        public final LingoExpr value;
        public final LingoExpr target;
        public PutAfter(LingoExpr value, LingoExpr target) {
            this.value = value;
            this.target = target;
        }
        @Override public String getExprType() { return "PutAfter"; }
        @Override public String toString() { return "put " + value + " after " + target; }
    }

    public static class PutDisplay extends LingoExpr {
        public final LingoExpr value;
        public PutDisplay(LingoExpr value) { this.value = value; }
        @Override public String getExprType() { return "PutDisplay"; }
        @Override public String toString() { return "put " + value; }
    }

    // ============ Chunk Expressions ============

    public static class ChunkExpr extends LingoExpr {
        public final String chunkType;  // "char", "word", "item", "line"
        public final LingoExpr startIndex;
        public final LingoExpr endIndex;  // nullable for single chunk
        public final LingoExpr source;
        public ChunkExpr(String chunkType, LingoExpr startIndex, LingoExpr endIndex, LingoExpr source) {
            this.chunkType = chunkType;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.source = source;
        }
        @Override public String getExprType() { return "ChunkExpr"; }
        @Override public String toString() { return chunkType + " " + startIndex + " of " + source; }
    }
}
