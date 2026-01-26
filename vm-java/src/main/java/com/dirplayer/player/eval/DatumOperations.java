package com.dirplayer.player.eval;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Arithmetic, comparison, and string operations for Datum values.
 * Port of Rust datum_operations module.
 *
 * This class provides static methods for performing operations on Datum values,
 * used by both the bytecode VM and the expression evaluator.
 */
public class DatumOperations {

    // ============ Arithmetic Operations ============

    /**
     * Add two datums.
     */
    public static int add(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            return player.allocDatum(Datum.ofInt(left.intValue() + right.intValue()));
        } else if (left.isNumber() && right.isNumber()) {
            return player.allocDatum(Datum.ofFloat(left.floatValue() + right.floatValue()));
        } else if (left.isString() && right.isString()) {
            return player.allocDatum(Datum.ofString(left.stringValue() + right.stringValue()));
        } else if (left.isString() || right.isString()) {
            // String + anything or anything + String
            String leftStr = player.datumToStringForConcat(left);
            String rightStr = player.datumToStringForConcat(right);
            return player.allocDatum(Datum.ofString(leftStr + rightStr));
        } else if (left.isPoint() && right.isPoint()) {
            return addPoints(leftRef, rightRef, player);
        } else if (left.isRect() && right.isRect()) {
            return addRects(leftRef, rightRef, player);
        } else if (left.isList() && right.isNumber()) {
            return addListScalar(leftRef, right, player, true);
        } else if (left.isNumber() && right.isList()) {
            return addListScalar(rightRef, left, player, true);
        }

        throw new ScriptError("Cannot add " + left.getTypeName() + " and " + right.getTypeName());
    }

    /**
     * Subtract two datums.
     */
    public static int subtract(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            return player.allocDatum(Datum.ofInt(left.intValue() - right.intValue()));
        } else if (left.isNumber() && right.isNumber()) {
            return player.allocDatum(Datum.ofFloat(left.floatValue() - right.floatValue()));
        } else if (left.isPoint() && right.isPoint()) {
            return subtractPoints(leftRef, rightRef, player);
        } else if (left.isRect() && right.isRect()) {
            return subtractRects(leftRef, rightRef, player);
        } else if (left.isList() && right.isNumber()) {
            return addListScalar(leftRef, right, player, false);  // false = subtract
        }

        throw new ScriptError("Cannot subtract " + right.getTypeName() + " from " + left.getTypeName());
    }

    /**
     * Multiply two datums.
     */
    public static int multiply(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            return player.allocDatum(Datum.ofInt(left.intValue() * right.intValue()));
        } else if (left.isNumber() && right.isNumber()) {
            return player.allocDatum(Datum.ofFloat(left.floatValue() * right.floatValue()));
        } else if (left.isPoint() && right.isNumber()) {
            return multiplyPointScalar(leftRef, right.floatValue(), player);
        } else if (left.isNumber() && right.isPoint()) {
            return multiplyPointScalar(rightRef, left.floatValue(), player);
        } else if (left.isList() && right.isNumber()) {
            return multiplyListScalar(leftRef, right.floatValue(), player);
        } else if (left.isNumber() && right.isList()) {
            return multiplyListScalar(rightRef, left.floatValue(), player);
        }

        throw new ScriptError("Cannot multiply " + left.getTypeName() + " and " + right.getTypeName());
    }

    /**
     * Divide two datums.
     */
    public static int divide(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            if (right.intValue() == 0) {
                throw new ScriptError("Division by zero");
            }
            return player.allocDatum(Datum.ofInt(left.intValue() / right.intValue()));
        } else if (left.isNumber() && right.isNumber()) {
            if (right.floatValue() == 0.0) {
                throw new ScriptError("Division by zero");
            }
            return player.allocDatum(Datum.ofFloat(left.floatValue() / right.floatValue()));
        } else if (left.isPoint() && right.isNumber()) {
            if (right.floatValue() == 0.0) {
                throw new ScriptError("Division by zero");
            }
            return multiplyPointScalar(leftRef, 1.0 / right.floatValue(), player);
        } else if (left.isList() && right.isNumber()) {
            if (right.floatValue() == 0.0) {
                throw new ScriptError("Division by zero");
            }
            return multiplyListScalar(leftRef, 1.0 / right.floatValue(), player);
        }

        throw new ScriptError("Cannot divide " + left.getTypeName() + " by " + right.getTypeName());
    }

    /**
     * Modulo operation on two datums.
     */
    public static int mod(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            if (right.intValue() == 0) return player.allocDatum(Datum.ofInt(0));
            return player.allocDatum(Datum.ofInt(left.intValue() % right.intValue()));
        } else if (left.isNumber() && right.isNumber()) {
            if (right.floatValue() == 0.0) return player.allocDatum(Datum.ofFloat(0.0));
            return player.allocDatum(Datum.ofFloat(left.floatValue() % right.floatValue()));
        } else if (left.isList() && right.isNumber()) {
            List<Integer> list = left.toList();
            List<Integer> newList = new ArrayList<>();
            for (int itemRef : list) {
                newList.add(mod(itemRef, rightRef, player));
            }
            return player.allocDatum(Datum.ofList(DatumType.List, newList, false));
        }

        throw new ScriptError("Modulus operator only works with ints and floats (given " +
            left.getTypeName() + " and " + right.getTypeName() + ")");
    }

    /**
     * Negate a datum.
     */
    public static int negate(int operandRef, DirPlayer player) throws ScriptError {
        Datum operand = player.getDatum(operandRef);

        if (operand.isInt()) {
            return player.allocDatum(Datum.ofInt(-operand.intValue()));
        } else if (operand.isFloat()) {
            return player.allocDatum(Datum.ofFloat(-operand.floatValue()));
        } else if (operand.isPoint()) {
            int[] point = operand.toPoint();
            Datum xVal = player.getDatum(point[0]);
            Datum yVal = player.getDatum(point[1]);

            int xRef = xVal.isInt() ?
                player.allocDatum(Datum.ofInt(-xVal.intValue())) :
                player.allocDatum(Datum.ofFloat(-xVal.floatValue()));
            int yRef = yVal.isInt() ?
                player.allocDatum(Datum.ofInt(-yVal.intValue())) :
                player.allocDatum(Datum.ofFloat(-yVal.floatValue()));

            return player.allocDatum(Datum.ofPoint(xRef, yRef));
        }

        throw new ScriptError("Cannot negate non-numeric value: " + operand.getTypeName());
    }

    // ============ Comparison Operations ============

    /**
     * Check if two datums are equal.
     */
    public static boolean equals(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isInt() && right.isInt()) {
            return left.intValue() == right.intValue();
        } else if (left.isNumber() && right.isNumber()) {
            return left.floatValue() == right.floatValue();
        } else if (left.isString() && right.isString()) {
            // Lingo string comparison is case-insensitive
            return left.stringValue().equalsIgnoreCase(right.stringValue());
        } else if (left.isVoid() && right.isVoid()) {
            return true;
        } else if (left.isSymbol() && right.isSymbol()) {
            return left.stringValue().equalsIgnoreCase(right.stringValue());
        } else if (left.isNull() && right.isNull()) {
            return true;
        }

        return false;
    }

    /**
     * Check if left is less than right.
     */
    public static boolean lessThan(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() < right.floatValue();
        } else if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) < 0;
        }

        throw new ScriptError("Cannot compare " + left.getTypeName() + " and " + right.getTypeName());
    }

    /**
     * Check if left is greater than right.
     */
    public static boolean greaterThan(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() > right.floatValue();
        } else if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) > 0;
        }

        throw new ScriptError("Cannot compare " + left.getTypeName() + " and " + right.getTypeName());
    }

    /**
     * Check if a datum is zero/falsy.
     */
    public static boolean isZero(int ref, DirPlayer player) throws ScriptError {
        Datum datum = player.getDatum(ref);
        if (datum.isVoid()) return true;
        if (datum.isNull()) return true;
        if (datum.isInt()) return datum.intValue() == 0;
        if (datum.isFloat()) return datum.floatValue() == 0.0;
        return false;
    }

    // ============ String Operations ============

    /**
     * Concatenate two datums as strings.
     */
    public static int concat(int leftRef, int rightRef, DirPlayer player, boolean pad) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        String leftStr = player.datumToStringForConcat(left);
        String rightStr = player.datumToStringForConcat(right);

        String result = pad ? (leftStr + " " + rightStr) : (leftStr + rightStr);
        return player.allocDatum(Datum.ofString(result));
    }

    /**
     * Check if a string contains another string.
     */
    public static boolean contains(int haystackRef, int needleRef, DirPlayer player) throws ScriptError {
        Datum haystack = player.getDatum(haystackRef);
        Datum needle = player.getDatum(needleRef);

        if (haystack.isString() && needle.isString()) {
            return haystack.stringValue().toLowerCase().contains(needle.stringValue().toLowerCase());
        } else if (haystack.isList()) {
            String needleStr = needle.stringValue();
            List<Integer> list = haystack.toList();
            for (int itemRef : list) {
                Datum item = player.getDatum(itemRef);
                if (item.isString() && item.stringValue().contains(needleStr)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Check if a string starts with another string.
     */
    public static boolean startsWith(int haystackRef, int needleRef, DirPlayer player) throws ScriptError {
        Datum haystack = player.getDatum(haystackRef);
        Datum needle = player.getDatum(needleRef);

        if (haystack.isVoid()) return false;

        String haystackStr = haystack.stringValue();
        String needleStr = needle.stringValue();

        return haystackStr.toLowerCase().startsWith(needleStr.toLowerCase());
    }

    // ============ Helper Methods for Point/Rect Operations ============

    private static int addPoints(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        int[] leftPoint = left.toPoint();
        int[] rightPoint = right.toPoint();

        int xRef = add(leftPoint[0], rightPoint[0], player);
        int yRef = add(leftPoint[1], rightPoint[1], player);

        return player.allocDatum(Datum.ofPoint(xRef, yRef));
    }

    private static int subtractPoints(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        int[] leftPoint = left.toPoint();
        int[] rightPoint = right.toPoint();

        int xRef = subtract(leftPoint[0], rightPoint[0], player);
        int yRef = subtract(leftPoint[1], rightPoint[1], player);

        return player.allocDatum(Datum.ofPoint(xRef, yRef));
    }

    private static int multiplyPointScalar(int pointRef, double scalar, DirPlayer player) throws ScriptError {
        Datum point = player.getDatum(pointRef);
        int[] pointVals = point.toPoint();

        Datum xVal = player.getDatum(pointVals[0]);
        Datum yVal = player.getDatum(pointVals[1]);

        int xRef, yRef;
        if (xVal.isInt() && yVal.isInt() && scalar == Math.floor(scalar)) {
            xRef = player.allocDatum(Datum.ofInt((int)(xVal.intValue() * scalar)));
            yRef = player.allocDatum(Datum.ofInt((int)(yVal.intValue() * scalar)));
        } else {
            xRef = player.allocDatum(Datum.ofFloat(xVal.floatValue() * scalar));
            yRef = player.allocDatum(Datum.ofFloat(yVal.floatValue() * scalar));
        }

        return player.allocDatum(Datum.ofPoint(xRef, yRef));
    }

    private static int addRects(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        int[] leftRect = left.toRect();
        int[] rightRect = right.toRect();

        int l = add(leftRect[0], rightRect[0], player);
        int t = add(leftRect[1], rightRect[1], player);
        int r = add(leftRect[2], rightRect[2], player);
        int b = add(leftRect[3], rightRect[3], player);

        return player.allocDatum(Datum.ofRect(l, t, r, b));
    }

    private static int subtractRects(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        int[] leftRect = left.toRect();
        int[] rightRect = right.toRect();

        int l = subtract(leftRect[0], rightRect[0], player);
        int t = subtract(leftRect[1], rightRect[1], player);
        int r = subtract(leftRect[2], rightRect[2], player);
        int b = subtract(leftRect[3], rightRect[3], player);

        return player.allocDatum(Datum.ofRect(l, t, r, b));
    }

    private static int addListScalar(int listRef, Datum scalar, DirPlayer player, boolean isAdd) throws ScriptError {
        Datum list = player.getDatum(listRef);
        List<Integer> items = list.toList();
        List<Integer> newItems = new ArrayList<>();

        int scalarRef = player.allocDatum(scalar);
        for (int itemRef : items) {
            int newRef = isAdd ? add(itemRef, scalarRef, player) : subtract(itemRef, scalarRef, player);
            newItems.add(newRef);
        }

        return player.allocDatum(Datum.ofList(DatumType.List, newItems, false));
    }

    private static int multiplyListScalar(int listRef, double scalar, DirPlayer player) throws ScriptError {
        Datum list = player.getDatum(listRef);
        List<Integer> items = list.toList();
        List<Integer> newItems = new ArrayList<>();

        int scalarRef = player.allocDatum(Datum.ofFloat(scalar));
        for (int itemRef : items) {
            int newRef = multiply(itemRef, scalarRef, player);
            newItems.add(newRef);
        }

        return player.allocDatum(Datum.ofList(DatumType.List, newItems, false));
    }
}
