package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;

/**
 * Compare bytecode handlers - comparison operations.
 * Port of Rust CompareBytecodeHandler struct.
 */
public class CompareBytecodeHandler {

    public static HandlerExecutionResult gt(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isGt = datumGreaterThan(left, right, player);
        int resultId = player.allocDatum(datumBool(isGt));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult lt(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isLt = datumLessThan(left, right, player);
        int resultId = player.allocDatum(datumBool(isLt));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult ltEq(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isLt = datumLessThan(left, right, player);
        boolean isEq = datumEquals(left, right, player);
        int resultId = player.allocDatum(datumBool(isLt || isEq));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult gtEq(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isGt = datumGreaterThan(left, right, player);
        boolean isEq = datumEquals(left, right, player);
        int resultId = player.allocDatum(datumBool(isGt || isEq));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult not(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int objId = scope.stack.pop();

        Datum obj = player.getDatum(objId);
        boolean isNot;

        if (obj.isVoid()) {
            isNot = true;
        } else if (obj.isInt()) {
            isNot = obj.intValue() == 0;
        } else if (obj.isFloat()) {
            isNot = (long) obj.floatValue() == 0;
        } else {
            isNot = false;
        }

        int resultId = player.allocDatum(datumBool(isNot));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult ntEq(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isEq = datumEquals(left, right, player);
        int resultId = player.allocDatum(datumBool(!isEq));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult and(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isAnd = left.boolValue() && right.boolValue();
        int resultId = player.allocDatum(datumBool(isAnd));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult or(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isOr = left.boolValue() || right.boolValue();
        int resultId = player.allocDatum(datumBool(isOr));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult eq(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        boolean isEq = datumEquals(left, right, player);
        int resultId = player.allocDatum(datumBool(isEq));
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    // Helper methods

    public static Datum datumBool(boolean value) {
        return Datum.ofInt(value ? 1 : 0);
    }

    public static boolean datumEquals(Datum left, Datum right, DirPlayer player) throws ScriptError {
        if (left.isInt() && right.isInt()) {
            return left.intValue() == right.intValue();
        } else if (left.isNumber() && right.isNumber()) {
            return left.floatValue() == right.floatValue();
        } else if (left.isString() && right.isString()) {
            return left.stringValue().equalsIgnoreCase(right.stringValue());
        } else if (left.isVoid() && right.isVoid()) {
            return true;
        } else if (left.isSymbol() && right.isSymbol()) {
            return left.stringValue().equalsIgnoreCase(right.stringValue());
        } else {
            return false;
        }
    }

    public static boolean datumGreaterThan(Datum left, Datum right, DirPlayer player) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() > right.floatValue();
        } else if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) > 0;
        } else {
            throw new ScriptError("Cannot compare " + left.typeStr() + " and " + right.typeStr());
        }
    }

    public static boolean datumLessThan(Datum left, Datum right, DirPlayer player) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() < right.floatValue();
        } else if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) < 0;
        } else {
            throw new ScriptError("Cannot compare " + left.typeStr() + " and " + right.typeStr());
        }
    }

    public static boolean datumIsZero(Datum datum, DirPlayer player) throws ScriptError {
        if (datum.isVoid()) {
            return true;
        } else if (datum.isInt()) {
            return datum.intValue() == 0;
        } else if (datum.isFloat()) {
            return datum.floatValue() == 0.0;
        } else {
            return false;
        }
    }
}
