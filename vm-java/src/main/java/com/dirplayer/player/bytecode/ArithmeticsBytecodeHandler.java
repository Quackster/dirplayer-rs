package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import java.util.ArrayList;
import java.util.List;

/**
 * Arithmetic bytecode handlers - add, sub, mul, div, mod, inv.
 * Port of Rust ArithmeticsBytecodeHandler struct.
 */
public class ArithmeticsBytecodeHandler {

    public static HandlerExecutionResult add(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        Datum result = addDatums(left, right, player);
        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult sub(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        Datum result = subtractDatums(left, right, player);
        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult mul(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum result = multiplyDatums(leftRef, rightRef, player);
        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult div(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum result = divideDatums(leftRef, rightRef, player);
        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult mod(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        Datum result;
        if (left.isInt() && right.isInt()) {
            result = Datum.ofInt(safeModInt(left.intValue(), right.intValue()));
        } else if (left.isInt() && right.isFloat()) {
            result = Datum.ofFloat(safeModFloat(left.intValue(), right.floatValue()));
        } else if (left.isFloat() && right.isInt()) {
            result = Datum.ofFloat(safeModFloat(left.floatValue(), right.intValue()));
        } else if (left.isFloat() && right.isFloat()) {
            result = Datum.ofFloat(safeModFloat(left.floatValue(), right.floatValue()));
        } else if (left.isList() && right.isNumber()) {
            List<Integer> list = left.toList();
            List<Integer> newList = new ArrayList<>();
            for (int itemRef : list) {
                Datum item = player.getDatum(itemRef);
                Datum itemResult;
                if (item.isInt()) {
                    if (right.isInt()) {
                        itemResult = Datum.ofInt(safeModInt(item.intValue(), right.intValue()));
                    } else {
                        itemResult = Datum.ofInt((int) safeModFloat(item.intValue(), right.floatValue()));
                    }
                } else if (item.isFloat()) {
                    if (right.isInt()) {
                        itemResult = Datum.ofInt((int) safeModFloat(item.floatValue(), right.intValue()));
                    } else {
                        itemResult = Datum.ofInt((int) safeModFloat(item.floatValue(), right.floatValue()));
                    }
                } else {
                    throw new ScriptError("Modulus operator in list only works with ints and floats");
                }
                newList.add(player.allocDatum(itemResult));
            }
            result = Datum.ofList(DatumType.List, newList, false);
        } else {
            throw new ScriptError("Modulus operator only works with ints and floats (given " +
                left.typeStr() + " and " + right.typeStr() + ")");
        }

        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult inv(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueId = scope.stack.pop();

        Datum value = player.getDatum(valueId);
        Datum result;

        if (value.isInt()) {
            result = Datum.ofInt(-value.intValue());
        } else if (value.isFloat()) {
            result = Datum.ofFloat(-value.floatValue());
        } else if (value.isPoint()) {
            int[] point = value.toPoint();
            Datum xVal = player.getDatum(point[0]);
            Datum yVal = player.getDatum(point[1]);

            int xRef, yRef;
            if (xVal.isInt()) {
                xRef = player.allocDatum(Datum.ofInt(-xVal.intValue()));
            } else if (xVal.isFloat()) {
                xRef = player.allocDatum(Datum.ofFloat(-xVal.floatValue()));
            } else {
                throw new ScriptError("Point component must be Int or Float");
            }

            if (yVal.isInt()) {
                yRef = player.allocDatum(Datum.ofInt(-yVal.intValue()));
            } else if (yVal.isFloat()) {
                yRef = player.allocDatum(Datum.ofFloat(-yVal.floatValue()));
            } else {
                throw new ScriptError("Point component must be Int or Float");
            }

            result = Datum.ofPoint(xRef, yRef);
        } else {
            throw new ScriptError("Cannot inv non-numeric value: " + value.typeStr());
        }

        int resultId = player.allocDatum(result);
        scope.stack.push(resultId);

        return HandlerExecutionResult.ADVANCE;
    }

    // Helper methods

    private static int safeModInt(int left, int right) {
        return right == 0 ? 0 : left % right;
    }

    private static double safeModFloat(double left, double right) {
        return right == 0.0 ? 0.0 : left % right;
    }

    public static Datum addDatums(Datum left, Datum right, DirPlayer player) throws ScriptError {
        if (left.isInt() && right.isInt()) {
            return Datum.ofInt(left.intValue() + right.intValue());
        } else if (left.isNumber() && right.isNumber()) {
            return Datum.ofFloat(left.floatValue() + right.floatValue());
        } else if (left.isString() && right.isString()) {
            return Datum.ofString(left.stringValue() + right.stringValue());
        } else if (left.isString() || right.isString()) {
            return Datum.ofString(left.stringValue() + right.stringValue());
        } else {
            throw new ScriptError("Cannot add " + left.typeStr() + " and " + right.typeStr());
        }
    }

    public static Datum subtractDatums(Datum left, Datum right, DirPlayer player) throws ScriptError {
        if (left.isInt() && right.isInt()) {
            return Datum.ofInt(left.intValue() - right.intValue());
        } else if (left.isNumber() && right.isNumber()) {
            return Datum.ofFloat(left.floatValue() - right.floatValue());
        } else {
            throw new ScriptError("Cannot subtract " + right.typeStr() + " from " + left.typeStr());
        }
    }

    public static Datum multiplyDatums(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            return Datum.ofInt(left.intValue() * right.intValue());
        } else if (left.isNumber() && right.isNumber()) {
            return Datum.ofFloat(left.floatValue() * right.floatValue());
        } else {
            throw new ScriptError("Cannot multiply " + left.typeStr() + " and " + right.typeStr());
        }
    }

    public static Datum divideDatums(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            if (right.intValue() == 0) {
                throw new ScriptError("Division by zero");
            }
            return Datum.ofInt(left.intValue() / right.intValue());
        } else if (left.isNumber() && right.isNumber()) {
            if (right.floatValue() == 0.0) {
                throw new ScriptError("Division by zero");
            }
            return Datum.ofFloat(left.floatValue() / right.floatValue());
        } else {
            throw new ScriptError("Cannot divide " + left.typeStr() + " by " + right.typeStr());
        }
    }
}
