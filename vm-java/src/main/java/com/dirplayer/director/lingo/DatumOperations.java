package com.dirplayer.director.lingo;

import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Datum arithmetic and comparison operations.
 * Port of Rust datum_operations.rs module.
 */
public class DatumOperations {

    /**
     * Add two datums together.
     * Handles type coercion rules for int/float/string/rect/point/vector/list/color operations.
     */
    public static Datum addDatums(Datum left, Datum right, DirPlayer player) throws ScriptError {
        DatumType leftType = left.getType();
        DatumType rightType = right.getType();

        // Void + some = some
        if (leftType == DatumType.Void) {
            return right.clone();
        }
        if (rightType == DatumType.Void) {
            return left.clone();
        }

        // Int + Int = Int
        if (leftType == DatumType.Int && rightType == DatumType.Int) {
            return Datum.ofInt(left.intValue() + right.intValue());
        }

        // Float + Float = Float
        if (leftType == DatumType.Float && rightType == DatumType.Float) {
            return Datum.ofFloat(left.floatValue() + right.floatValue());
        }

        // Float + Int = Float
        if (leftType == DatumType.Float && rightType == DatumType.Int) {
            return Datum.ofFloat(left.floatValue() + right.intValue());
        }

        // Int + Float = Float
        if (leftType == DatumType.Int && rightType == DatumType.Float) {
            return Datum.ofFloat(left.intValue() + right.floatValue());
        }

        // Rect + Rect
        if (leftType == DatumType.Rect && rightType == DatumType.Rect) {
            int[] a = left.toRect();
            int[] b = right.toRect();
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(b[i]);
                Datum sum = addDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(sum);
            }
            return Datum.ofRect(result);
        }

        // Rect + List (4 elements)
        if (leftType == DatumType.Rect && rightType == DatumType.List) {
            int[] a = left.toRect();
            List<Integer> refList = right.toList();
            if (refList.size() == 4) {
                int[] result = new int[4];
                for (int i = 0; i < 4; i++) {
                    Datum aVal = player.allocator.get(a[i]);
                    Datum bVal = player.allocator.get(refList.get(i));
                    Datum sum = addDatums(aVal, bVal, player);
                    result[i] = player.allocator.alloc(sum);
                }
                return Datum.ofRect(result);
            } else {
                throw new ScriptError("Invalid list length for add_datums: " + refList.size());
            }
        }

        // Vector + Vector
        if (leftType == DatumType.Vector && rightType == DatumType.Vector) {
            double[] a = left.toVector();
            double[] b = right.toVector();
            return Datum.ofVector(a[0] + b[0], a[1] + b[1], a[2] + b[2]);
        }

        // Vector + Int
        if (leftType == DatumType.Vector && rightType == DatumType.Int) {
            double[] a = left.toVector();
            double b = right.intValue();
            return Datum.ofVector(a[0] + b, a[1] + b, a[2] + b);
        }

        // Vector + Float
        if (leftType == DatumType.Vector && rightType == DatumType.Float) {
            double[] a = left.toVector();
            double b = right.floatValue();
            return Datum.ofVector(a[0] + b, a[1] + b, a[2] + b);
        }

        // Int + Vector
        if (leftType == DatumType.Int && rightType == DatumType.Vector) {
            double a = left.intValue();
            double[] b = right.toVector();
            return Datum.ofVector(a + b[0], a + b[1], a + b[2]);
        }

        // Float + Vector
        if (leftType == DatumType.Float && rightType == DatumType.Vector) {
            double a = left.floatValue();
            double[] b = right.toVector();
            return Datum.ofVector(a + b[0], a + b[1], a + b[2]);
        }

        // Vector + List (3 elements)
        if (leftType == DatumType.Vector && rightType == DatumType.List) {
            List<Integer> list = right.toList();
            if (list.size() == 3) {
                double[] a = left.toVector();
                double[] result = new double[3];
                for (int i = 0; i < 3; i++) {
                    Datum val = player.allocator.get(list.get(i));
                    if (val.isInt()) {
                        result[i] = a[i] + val.intValue();
                    } else if (val.isFloat()) {
                        result[i] = a[i] + val.floatValue();
                    } else {
                        throw new ScriptError("Cannot add Vector to non-numeric list element");
                    }
                }
                return Datum.ofVector(result);
            }
        }

        // List + Vector (3 elements)
        if (leftType == DatumType.List && rightType == DatumType.Vector) {
            List<Integer> list = left.toList();
            if (list.size() == 3) {
                double[] b = right.toVector();
                List<Integer> resultRefs = new ArrayList<>(3);
                for (int i = 0; i < 3; i++) {
                    Datum val = player.allocator.get(list.get(i));
                    Datum resultDatum;
                    if (val.isInt()) {
                        resultDatum = Datum.ofFloat(val.intValue() + b[i]);
                    } else if (val.isFloat()) {
                        resultDatum = Datum.ofFloat(val.floatValue() + b[i]);
                    } else {
                        throw new ScriptError("Cannot add list element to Vector");
                    }
                    resultRefs.add(player.allocator.alloc(resultDatum));
                }
                return Datum.ofList(DatumType.List, resultRefs, false);
            }
        }

        // List + List
        if (leftType == DatumType.List && rightType == DatumType.List) {
            List<Integer> listA = left.toList();
            List<Integer> listB = right.toList();
            int intersectionCount = Math.min(listA.size(), listB.size());
            List<Integer> result = new ArrayList<>(intersectionCount);
            for (int i = 0; i < intersectionCount; i++) {
                Datum a = player.allocator.get(listA.get(i));
                Datum b = player.allocator.get(listB.get(i));
                Datum resultDatum = addDatums(a, b, player);
                result.add(player.allocator.alloc(resultDatum));
            }
            return Datum.ofList(DatumType.List, result, false);
        }

        // List + Int
        if (leftType == DatumType.List && rightType == DatumType.Int) {
            List<Integer> list = left.toList();
            int intVal = right.intValue();
            List<Integer> resultRefs = new ArrayList<>();
            for (int ref : list) {
                Datum datum = player.allocator.get(ref);
                Datum resultDatum;
                if (datum.isInt()) {
                    resultDatum = Datum.ofInt(datum.intValue() + intVal);
                } else if (datum.isFloat()) {
                    resultDatum = Datum.ofFloat(datum.floatValue() + intVal);
                } else {
                    throw new ScriptError("Invalid list element for add_datums: " + ref);
                }
                resultRefs.add(player.allocator.alloc(resultDatum));
            }
            return Datum.ofList(DatumType.List, resultRefs, false);
        }

        // String + List
        if (leftType == DatumType.String && rightType == DatumType.List) {
            String s = left.stringValue();
            List<Integer> list = right.toList();
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    formatted.append(", ");
                }
                formatted.append(datumToStringForConcat(player.allocator.get(list.get(i)), player));
            }
            return Datum.ofString(s + formatted.toString());
        }

        // List + String
        if (leftType == DatumType.List && rightType == DatumType.String) {
            List<Integer> list = left.toList();
            String s = right.stringValue();
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    formatted.append(", ");
                }
                formatted.append(datumToStringForConcat(player.allocator.get(list.get(i)), player));
            }
            return Datum.ofString(formatted.toString() + s);
        }

        // Point + Point
        if (leftType == DatumType.Point && rightType == DatumType.Point) {
            int[] a = left.toPoint();
            int[] b = right.toPoint();
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(b[i]);
                Datum sum = addDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(sum);
            }
            return Datum.ofPoint(result);
        }

        // Point + List (2 elements)
        if (leftType == DatumType.Point && rightType == DatumType.List) {
            int[] a = left.toPoint();
            List<Integer> refList = right.toList();
            if (refList.size() == 2) {
                int[] result = new int[2];
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(a[i]);
                    Datum bVal = player.allocator.get(refList.get(i));
                    Datum sum = addDatums(aVal, bVal, player);
                    result[i] = player.allocator.alloc(sum);
                }
                return Datum.ofPoint(result);
            } else {
                throw new ScriptError("Invalid list length for add_datums: " + refList.size());
            }
        }

        // Point + Int
        if (leftType == DatumType.Point && rightType == DatumType.Int) {
            int[] a = left.toPoint();
            int bRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(bRef);
                Datum sum = addDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(sum);
            }
            return Datum.ofPoint(result);
        }

        // ColorRef + ColorRef
        if (leftType == DatumType.ColorRef && rightType == DatumType.ColorRef) {
            ColorRef a = left.toColorRef();
            ColorRef b = right.toColorRef();
            if (a.isPaletteIndex() && b.isPaletteIndex()) {
                return Datum.ofPaletteIndexColor(a.getPaletteIndex() + b.getPaletteIndex());
            } else if (a.isRgb() && b.isRgb()) {
                return Datum.ofColorRef(a.getR() + b.getR(), a.getG() + b.getG(), a.getB() + b.getB());
            } else {
                throw new ScriptError("Invalid operands for add_datums: " + a + ", " + b);
            }
        }

        // String + Int
        if (leftType == DatumType.String && rightType == DatumType.Int) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat + right.intValue());
        }

        // String + Float
        if (leftType == DatumType.String && rightType == DatumType.Float) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat + right.floatValue());
        }

        // Float + String
        if (leftType == DatumType.Float && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.floatValue() + rightFloat);
        }

        // Int + String
        if (leftType == DatumType.Int && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.intValue() + rightFloat);
        }

        throw new ScriptError("Invalid operands for add_datums: " + left.typeStr() + ", " + right.typeStr());
    }

    /**
     * Subtract two datums.
     * Handles type coercion rules for int/float/string/rect/point/vector/list/color operations.
     */
    public static Datum subtractDatums(Datum left, Datum right, DirPlayer player) throws ScriptError {
        DatumType leftType = left.getType();
        DatumType rightType = right.getType();

        // Int - Int = Int (wrapping)
        if (leftType == DatumType.Int && rightType == DatumType.Int) {
            return Datum.ofInt(left.intValue() - right.intValue());
        }

        // Float - Float = Float
        if (leftType == DatumType.Float && rightType == DatumType.Float) {
            return Datum.ofFloat(left.floatValue() - right.floatValue());
        }

        // Float - Int = Float
        if (leftType == DatumType.Float && rightType == DatumType.Int) {
            return Datum.ofFloat(left.floatValue() - right.intValue());
        }

        // Int - Float = Float
        if (leftType == DatumType.Int && rightType == DatumType.Float) {
            return Datum.ofFloat(left.intValue() - right.floatValue());
        }

        // Rect - Rect
        if (leftType == DatumType.Rect && rightType == DatumType.Rect) {
            int[] a = left.toRect();
            int[] b = right.toRect();
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(b[i]);
                Datum diff = subtractDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(diff);
            }
            return Datum.ofRect(result);
        }

        // Rect - List (4 elements)
        if (leftType == DatumType.Rect && rightType == DatumType.List) {
            int[] a = left.toRect();
            List<Integer> refList = right.toList();
            if (refList.size() == 4) {
                int[] result = new int[4];
                for (int i = 0; i < 4; i++) {
                    Datum aVal = player.allocator.get(a[i]);
                    Datum bVal = player.allocator.get(refList.get(i));
                    Datum diff = subtractDatums(aVal, bVal, player);
                    result[i] = player.allocator.alloc(diff);
                }
                return Datum.ofRect(result);
            } else {
                throw new ScriptError("Invalid list length for subtract_datums: " + refList.size());
            }
        }

        // Vector - Vector
        if (leftType == DatumType.Vector && rightType == DatumType.Vector) {
            double[] a = left.toVector();
            double[] b = right.toVector();
            return Datum.ofVector(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
        }

        // Vector - Int
        if (leftType == DatumType.Vector && rightType == DatumType.Int) {
            double[] a = left.toVector();
            double b = right.intValue();
            return Datum.ofVector(a[0] - b, a[1] - b, a[2] - b);
        }

        // Vector - Float
        if (leftType == DatumType.Vector && rightType == DatumType.Float) {
            double[] a = left.toVector();
            double b = right.floatValue();
            return Datum.ofVector(a[0] - b, a[1] - b, a[2] - b);
        }

        // Int - Vector
        if (leftType == DatumType.Int && rightType == DatumType.Vector) {
            double a = left.intValue();
            double[] b = right.toVector();
            return Datum.ofVector(a - b[0], a - b[1], a - b[2]);
        }

        // Float - Vector
        if (leftType == DatumType.Float && rightType == DatumType.Vector) {
            double a = left.floatValue();
            double[] b = right.toVector();
            return Datum.ofVector(a - b[0], a - b[1], a - b[2]);
        }

        // Vector - List (3 elements)
        if (leftType == DatumType.Vector && rightType == DatumType.List) {
            List<Integer> list = right.toList();
            if (list.size() == 3) {
                double[] a = left.toVector();
                double[] result = new double[3];
                for (int i = 0; i < 3; i++) {
                    Datum val = player.allocator.get(list.get(i));
                    if (val.isInt()) {
                        result[i] = a[i] - val.intValue();
                    } else if (val.isFloat()) {
                        result[i] = a[i] - val.floatValue();
                    } else {
                        throw new ScriptError("Cannot subtract non-numeric list element from Vector");
                    }
                }
                return Datum.ofVector(result);
            }
        }

        // List - Vector (3 elements)
        if (leftType == DatumType.List && rightType == DatumType.Vector) {
            List<Integer> list = left.toList();
            if (list.size() == 3) {
                double[] b = right.toVector();
                List<Integer> resultRefs = new ArrayList<>(3);
                for (int i = 0; i < 3; i++) {
                    Datum val = player.allocator.get(list.get(i));
                    Datum resultDatum;
                    if (val.isInt()) {
                        resultDatum = Datum.ofFloat(val.intValue() - b[i]);
                    } else if (val.isFloat()) {
                        resultDatum = Datum.ofFloat(val.floatValue() - b[i]);
                    } else {
                        throw new ScriptError("Cannot subtract Vector from list element");
                    }
                    resultRefs.add(player.allocator.alloc(resultDatum));
                }
                return Datum.ofList(DatumType.List, resultRefs, false);
            }
        }

        // List - List
        if (leftType == DatumType.List && rightType == DatumType.List) {
            List<Integer> listA = left.toList();
            List<Integer> listB = right.toList();
            int intersectionCount = Math.min(listA.size(), listB.size());
            List<Integer> result = new ArrayList<>(intersectionCount);
            for (int i = 0; i < intersectionCount; i++) {
                Datum a = player.allocator.get(listA.get(i));
                Datum b = player.allocator.get(listB.get(i));
                Datum resultDatum = subtractDatums(a, b, player);
                result.add(player.allocator.alloc(resultDatum));
            }
            return Datum.ofList(DatumType.List, result, false);
        }

        // Point - Point
        if (leftType == DatumType.Point && rightType == DatumType.Point) {
            int[] a = left.toPoint();
            int[] b = right.toPoint();
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(b[i]);
                Datum diff = subtractDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(diff);
            }
            return Datum.ofPoint(result);
        }

        // Point - List (2 elements)
        if (leftType == DatumType.Point && rightType == DatumType.List) {
            int[] a = left.toPoint();
            List<Integer> refList = right.toList();
            if (refList.size() == 2) {
                int[] result = new int[2];
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(a[i]);
                    Datum bVal = player.allocator.get(refList.get(i));
                    Datum diff = subtractDatums(aVal, bVal, player);
                    result[i] = player.allocator.alloc(diff);
                }
                return Datum.ofPoint(result);
            } else {
                throw new ScriptError("Invalid list length for subtract_datums: " + refList.size());
            }
        }

        // Int - Point
        if (leftType == DatumType.Int && rightType == DatumType.Point) {
            int aRef = player.allocator.alloc(Datum.ofInt(left.intValue()));
            int[] b = right.toPoint();
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(aRef);
                Datum bVal = player.allocator.get(b[i]);
                Datum diff = subtractDatums(aVal, bVal, player);
                result[i] = player.allocator.alloc(diff);
            }
            return Datum.ofPoint(result);
        }

        // ColorRef - ColorRef
        if (leftType == DatumType.ColorRef && rightType == DatumType.ColorRef) {
            ColorRef a = left.toColorRef();
            ColorRef b = right.toColorRef();
            if (a.isPaletteIndex() && b.isPaletteIndex()) {
                // Wrapping subtraction
                return Datum.ofPaletteIndexColor(a.getPaletteIndex() - b.getPaletteIndex());
            } else if (a.isRgb() && b.isRgb()) {
                // Wrapping subtraction for RGB components
                return Datum.ofColorRef(
                    (a.getR() - b.getR()) & 0xFF,
                    (a.getG() - b.getG()) & 0xFF,
                    (a.getB() - b.getB()) & 0xFF
                );
            } else {
                throw new ScriptError("Invalid operands for subtract_datums: " + a + ", " + b);
            }
        }

        // String - Int
        if (leftType == DatumType.String && rightType == DatumType.Int) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat - right.intValue());
        }

        // String - Float
        if (leftType == DatumType.String && rightType == DatumType.Float) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat - right.floatValue());
        }

        // Float - String
        if (leftType == DatumType.Float && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.floatValue() - rightFloat);
        }

        // Int - String
        if (leftType == DatumType.Int && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.intValue() - rightFloat);
        }

        // Void - Int
        if (leftType == DatumType.Void && rightType == DatumType.Int) {
            return Datum.ofFloat(0.0 - right.intValue());
        }

        // Void - Float
        if (leftType == DatumType.Void && rightType == DatumType.Float) {
            return Datum.ofFloat(0.0 - right.floatValue());
        }

        // Void - Void
        if (leftType == DatumType.Void && rightType == DatumType.Void) {
            return Datum.ofInt(0);
        }

        // Int - Void
        if (leftType == DatumType.Int && rightType == DatumType.Void) {
            return Datum.ofFloat(left.intValue() - 0.0);
        }

        // Float - Void
        if (leftType == DatumType.Float && rightType == DatumType.Void) {
            return Datum.ofFloat(left.floatValue() - 0.0);
        }

        // Void - some = some
        if (leftType == DatumType.Void) {
            return right.clone();
        }

        // some - Void = some
        if (rightType == DatumType.Void) {
            return left.clone();
        }

        throw new ScriptError("Invalid operands for subtract_datums: " + left.typeStr() + ", " + right.typeStr());
    }

    /**
     * Multiply two datums.
     * Takes DatumRef IDs and handles type coercion rules.
     */
    public static Datum multiplyDatums(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.allocator.get(leftRef);
        Datum right = player.allocator.get(rightRef);

        DatumType leftType = left.getType();
        DatumType rightType = right.getType();

        // Void * Int or Int * Void = Int(0)
        if ((leftType == DatumType.Void && rightType == DatumType.Int) ||
            (leftType == DatumType.Int && rightType == DatumType.Void)) {
            return Datum.ofInt(0);
        }

        // Void * Float or Float * Void = Float(0.0)
        if ((leftType == DatumType.Void && rightType == DatumType.Float) ||
            (leftType == DatumType.Float && rightType == DatumType.Void)) {
            return Datum.ofFloat(0.0);
        }

        // Int * Int = Int
        if (leftType == DatumType.Int && rightType == DatumType.Int) {
            return Datum.ofInt(left.intValue() * right.intValue());
        }

        // Int * Float = Float
        if (leftType == DatumType.Int && rightType == DatumType.Float) {
            return Datum.ofFloat(left.intValue() * right.floatValue());
        }

        // Float * Int = Float
        if (leftType == DatumType.Float && rightType == DatumType.Int) {
            return Datum.ofFloat(left.floatValue() * right.intValue());
        }

        // Float * Float = Float
        if (leftType == DatumType.Float && rightType == DatumType.Float) {
            return Datum.ofFloat(left.floatValue() * right.floatValue());
        }

        // Rect * Int
        if (leftType == DatumType.Rect && rightType == DatumType.Int) {
            int[] a = left.toRect();
            int rightValRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum prod = multiplyDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(prod);
            }
            return Datum.ofRect(result);
        }

        // Rect * Float
        if (leftType == DatumType.Rect && rightType == DatumType.Float) {
            int[] a = left.toRect();
            int rightValRef = player.allocator.alloc(Datum.ofFloat(right.floatValue()));
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum prod = multiplyDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(prod);
            }
            return Datum.ofRect(result);
        }

        // Float * Rect
        if (leftType == DatumType.Float && rightType == DatumType.Rect) {
            int[] b = right.toRect();
            int leftValRef = player.allocator.alloc(Datum.ofFloat(left.floatValue()));
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(leftValRef);
                Datum bVal = player.allocator.get(b[i]);
                Datum prod = multiplyDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(prod);
            }
            return Datum.ofRect(result);
        }

        // Point * Int
        if (leftType == DatumType.Point && rightType == DatumType.Int) {
            int[] arr = left.toPoint();
            int scalarRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum val = player.allocator.get(arr[i]);
                Datum scalarVal = player.allocator.get(scalarRef);
                Datum product = multiplyDatums(
                    player.allocator.alloc(val),
                    player.allocator.alloc(scalarVal),
                    player
                );
                result[i] = player.allocator.alloc(product);
            }
            return Datum.ofPoint(result);
        }

        // Point * Float
        if (leftType == DatumType.Point && rightType == DatumType.Float) {
            int[] arr = left.toPoint();
            int scalarRef = player.allocator.alloc(Datum.ofFloat(right.floatValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum val = player.allocator.get(arr[i]);
                Datum scalarVal = player.allocator.get(scalarRef);
                Datum product = multiplyDatums(
                    player.allocator.alloc(val),
                    player.allocator.alloc(scalarVal),
                    player
                );
                result[i] = player.allocator.alloc(product);
            }
            return Datum.ofPoint(result);
        }

        // Float * Point
        if (leftType == DatumType.Float && rightType == DatumType.Point) {
            int[] b = right.toPoint();
            int leftValRef = player.allocator.alloc(Datum.ofFloat(left.floatValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(leftValRef);
                Datum bVal = player.allocator.get(b[i]);
                Datum prod = multiplyDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(prod);
            }
            return Datum.ofPoint(result);
        }

        // Int * Point
        if (leftType == DatumType.Int && rightType == DatumType.Point) {
            int[] b = right.toPoint();
            int leftValRef = player.allocator.alloc(Datum.ofInt(left.intValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(leftValRef);
                Datum bVal = player.allocator.get(b[i]);
                Datum prod = multiplyDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(prod);
            }
            return Datum.ofPoint(result);
        }

        // Point * Point
        if (leftType == DatumType.Point && rightType == DatumType.Point) {
            int[] leftArr = left.toPoint();
            int[] rightArr = right.toPoint();
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum leftVal = player.allocator.get(leftArr[i]);
                Datum rightVal = player.allocator.get(rightArr[i]);
                Datum product = multiplyDatums(
                    player.allocator.alloc(leftVal),
                    player.allocator.alloc(rightVal),
                    player
                );
                result[i] = player.allocator.alloc(product);
            }
            return Datum.ofPoint(result);
        }

        // List * Float
        if (leftType == DatumType.List && rightType == DatumType.Float) {
            List<Integer> list = left.toList();
            double rightVal = right.floatValue();
            List<Integer> refList = new ArrayList<>();
            for (int item : list) {
                Datum itemDatum = player.allocator.get(item);
                Datum resultDatum;
                if (itemDatum.isInt()) {
                    resultDatum = Datum.ofFloat(itemDatum.intValue() * rightVal);
                } else if (itemDatum.isFloat()) {
                    resultDatum = Datum.ofFloat(itemDatum.floatValue() * rightVal);
                } else {
                    throw new ScriptError("Mul operator in list only works with ints and floats. Given: " + formatDatum(item, player));
                }
                refList.add(player.allocator.alloc(resultDatum));
            }
            return Datum.ofList(DatumType.List, refList, false);
        }

        // String * Int
        if (leftType == DatumType.String && rightType == DatumType.Int) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat * right.intValue());
        }

        // String * Float
        if (leftType == DatumType.String && rightType == DatumType.Float) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat * right.floatValue());
        }

        // Float * String
        if (leftType == DatumType.Float && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.floatValue() * rightFloat);
        }

        // Int * String
        if (leftType == DatumType.Int && rightType == DatumType.String) {
            double rightFloat = parseDoubleOrZero(right.stringValue());
            return Datum.ofFloat(left.intValue() * rightFloat);
        }

        // Point * List (2 elements)
        if (leftType == DatumType.Point && rightType == DatumType.List) {
            List<Integer> list = right.toList();
            if (list.size() == 2) {
                int[] p = left.toPoint();
                int[] result = new int[2];
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(p[i]);
                    Datum bVal = player.allocator.get(list.get(i));
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result[i] = player.allocator.alloc(prod);
                }
                return Datum.ofPoint(result);
            }
        }

        // List * Point (2 elements)
        if (leftType == DatumType.List && rightType == DatumType.Point) {
            List<Integer> list = left.toList();
            if (list.size() == 2) {
                int[] p = right.toPoint();
                int[] result = new int[2];
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(list.get(i));
                    Datum bVal = player.allocator.get(p[i]);
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result[i] = player.allocator.alloc(prod);
                }
                return Datum.ofPoint(result);
            }
        }

        // List (2 elements) * Int
        if (leftType == DatumType.List && rightType == DatumType.Int) {
            List<Integer> list = left.toList();
            if (list.size() == 2) {
                int rightValRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
                List<Integer> result = new ArrayList<>(2);
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(list.get(i));
                    Datum bVal = player.allocator.get(rightValRef);
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result.add(player.allocator.alloc(prod));
                }
                return Datum.ofList(DatumType.List, result, false);
            }
        }

        // List (2 elements) * Float
        if (leftType == DatumType.List && rightType == DatumType.Float) {
            List<Integer> list = left.toList();
            if (list.size() == 2) {
                int rightValRef = player.allocator.alloc(Datum.ofFloat(right.floatValue()));
                List<Integer> result = new ArrayList<>(2);
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(list.get(i));
                    Datum bVal = player.allocator.get(rightValRef);
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result.add(player.allocator.alloc(prod));
                }
                return Datum.ofList(DatumType.List, result, false);
            }
        }

        // Int * List (2 elements)
        if (leftType == DatumType.Int && rightType == DatumType.List) {
            List<Integer> list = right.toList();
            if (list.size() == 2) {
                int leftValRef = player.allocator.alloc(Datum.ofInt(left.intValue()));
                List<Integer> result = new ArrayList<>(2);
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(leftValRef);
                    Datum bVal = player.allocator.get(list.get(i));
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result.add(player.allocator.alloc(prod));
                }
                return Datum.ofList(DatumType.List, result, false);
            }
        }

        // Float * List (2 elements)
        if (leftType == DatumType.Float && rightType == DatumType.List) {
            List<Integer> list = right.toList();
            if (list.size() == 2) {
                int leftValRef = player.allocator.alloc(Datum.ofFloat(left.floatValue()));
                List<Integer> result = new ArrayList<>(2);
                for (int i = 0; i < 2; i++) {
                    Datum aVal = player.allocator.get(leftValRef);
                    Datum bVal = player.allocator.get(list.get(i));
                    Datum prod = multiplyDatums(
                        player.allocator.alloc(aVal),
                        player.allocator.alloc(bVal),
                        player
                    );
                    result.add(player.allocator.alloc(prod));
                }
                return Datum.ofList(DatumType.List, result, false);
            }
        }

        throw new ScriptError("Mul operator only works with ints and floats. Given: " +
            formatDatum(leftRef, player) + ", " + formatDatum(rightRef, player));
    }

    /**
     * Divide two datums.
     * Takes DatumRef IDs and handles type coercion rules.
     */
    public static Datum divideDatums(int leftRef, int rightRef, DirPlayer player) throws ScriptError {
        Datum left = player.allocator.get(leftRef);
        Datum right = player.allocator.get(rightRef);

        DatumType leftType = left.getType();
        DatumType rightType = right.getType();

        // Int / Int = Int
        if (leftType == DatumType.Int && rightType == DatumType.Int) {
            return Datum.ofInt(left.intValue() / right.intValue());
        }

        // Int / Float = Float
        if (leftType == DatumType.Int && rightType == DatumType.Float) {
            return Datum.ofFloat(left.intValue() / right.floatValue());
        }

        // Float / Int = Float
        if (leftType == DatumType.Float && rightType == DatumType.Int) {
            return Datum.ofFloat(left.floatValue() / right.intValue());
        }

        // Float / Float = Float
        if (leftType == DatumType.Float && rightType == DatumType.Float) {
            return Datum.ofFloat(left.floatValue() / right.floatValue());
        }

        // Point / Int
        if (leftType == DatumType.Point && rightType == DatumType.Int) {
            int[] a = left.toPoint();
            int rightValRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum quot = divideDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(quot);
            }
            return Datum.ofPoint(result);
        }

        // Point / Float
        if (leftType == DatumType.Point && rightType == DatumType.Float) {
            int[] a = left.toPoint();
            int rightValRef = player.allocator.alloc(Datum.ofFloat(right.floatValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum quot = divideDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(quot);
            }
            return Datum.ofPoint(result);
        }

        // Float / Point
        if (leftType == DatumType.Float && rightType == DatumType.Point) {
            int[] b = right.toPoint();
            int leftValRef = player.allocator.alloc(Datum.ofFloat(left.floatValue()));
            int[] result = new int[2];
            for (int i = 0; i < 2; i++) {
                Datum aVal = player.allocator.get(leftValRef);
                Datum bVal = player.allocator.get(b[i]);
                Datum quot = divideDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(quot);
            }
            return Datum.ofPoint(result);
        }

        // Rect / Int
        if (leftType == DatumType.Rect && rightType == DatumType.Int) {
            int[] a = left.toRect();
            int rightValRef = player.allocator.alloc(Datum.ofInt(right.intValue()));
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum quot = divideDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(quot);
            }
            return Datum.ofRect(result);
        }

        // Rect / Float
        if (leftType == DatumType.Rect && rightType == DatumType.Float) {
            int[] a = left.toRect();
            int rightValRef = player.allocator.alloc(Datum.ofFloat(right.floatValue()));
            int[] result = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum aVal = player.allocator.get(a[i]);
                Datum bVal = player.allocator.get(rightValRef);
                Datum quot = divideDatums(
                    player.allocator.alloc(aVal),
                    player.allocator.alloc(bVal),
                    player
                );
                result[i] = player.allocator.alloc(quot);
            }
            return Datum.ofRect(result);
        }

        // Int / String
        if (leftType == DatumType.Int && rightType == DatumType.String) {
            double rightVal;
            try {
                rightVal = Double.parseDouble(right.stringValue());
            } catch (NumberFormatException e) {
                throw new ScriptError("Cannot divide int by string: " + right.stringValue());
            }
            return Datum.ofFloat(left.intValue() / rightVal);
        }

        // Float / String
        if (leftType == DatumType.Float && rightType == DatumType.String) {
            double rightVal;
            try {
                rightVal = Double.parseDouble(right.stringValue());
            } catch (NumberFormatException e) {
                throw new ScriptError("Cannot divide float by string: " + right.stringValue());
            }
            return Datum.ofFloat(left.floatValue() / rightVal);
        }

        // String / Int
        if (leftType == DatumType.String && rightType == DatumType.Int) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat / right.intValue());
        }

        // String / Float
        if (leftType == DatumType.String && rightType == DatumType.Float) {
            double leftFloat = parseDoubleOrZero(left.stringValue());
            return Datum.ofFloat(leftFloat / right.floatValue());
        }

        // Void / anything = 0
        if (leftType == DatumType.Void) {
            return Datum.ofInt(0);
        }

        throw new ScriptError("Div operator only works with ints and floats (Provided: " +
            left.typeStr() + " and " + right.typeStr() + ")");
    }

    /**
     * Concatenate two datums as strings.
     */
    public static Datum concatDatums(Datum left, Datum right, DirPlayer player) throws ScriptError {
        String leftStr = datumToStringForConcat(left, player);
        String rightStr = datumToStringForConcat(right, player);
        return Datum.ofString(leftStr + rightStr);
    }

    /**
     * Convert a datum to string for concatenation purposes.
     * This follows specific rules for different datum types.
     */
    public static String datumToStringForConcat(Datum datum, DirPlayer player) throws ScriptError {
        DatumType type = datum.getType();

        if (type == DatumType.String || type == DatumType.StringChunk) {
            return datum.stringValue();
        }

        if (type == DatumType.Symbol) {
            return datum.symbolValue();
        }

        if (type == DatumType.Void || type == DatumType.Null) {
            return "";
        }

        if (type == DatumType.ColorRef) {
            ColorRef cr = datum.toColorRef();
            if (cr.isPaletteIndex()) {
                return "color(" + cr.getPaletteIndex() + ")";
            } else {
                return "rgb(" + cr.getR() + ", " + cr.getG() + ", " + cr.getB() + ")";
            }
        }

        if (type == DatumType.List) {
            List<Integer> list = datum.toList();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(datumToStringForConcat(player.allocator.get(list.get(i)), player));
            }
            sb.append("]");
            return sb.toString();
        }

        if (type == DatumType.PropList) {
            List<Datum.PropListPair> entries = datum.toMap();
            if (entries.isEmpty()) {
                return "[:]";
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                Datum.PropListPair entry = entries.get(i);
                sb.append(datumToStringForConcat(player.allocator.get(entry.key), player));
                sb.append(":");
                sb.append(datumToStringForConcat(player.allocator.get(entry.value), player));
            }
            sb.append("]");
            return sb.toString();
        }

        // For other types, use the standard formatter
        return formatConcreteDatum(datum, player);
    }

    /**
     * Format a datum for display.
     */
    public static String formatDatum(int datumRef, DirPlayer player) {
        Datum datum = player.allocator.get(datumRef);
        return formatConcreteDatum(datum, player);
    }

    /**
     * Format a concrete datum for display.
     */
    public static String formatConcreteDatum(Datum datum, DirPlayer player) {
        DatumType type = datum.getType();

        try {
            switch (type) {
                case String:
                    return "\"" + datum.stringValue() + "\"";
                case Int:
                    return String.valueOf(datum.intValue());
                case Float:
                    return formatFloatWithPrecision(datum.floatValue(), player);
                case Void:
                    return "Void";
                case Symbol:
                    return "#" + datum.symbolValue();
                case Null:
                    return "<Null>";
                case List:
                case ArgList:
                case ArgListNoRet:
                    List<Integer> items = datum.toList();
                    StringBuilder listSb = new StringBuilder("[");
                    for (int i = 0; i < items.size(); i++) {
                        if (i > 0) {
                            listSb.append(", ");
                        }
                        listSb.append(formatDatum(items.get(i), player));
                    }
                    listSb.append("]");
                    return listSb.toString();
                case PropList:
                    List<Datum.PropListPair> entries = datum.toMap();
                    if (entries.isEmpty()) {
                        return "[:]";
                    }
                    StringBuilder propSb = new StringBuilder("[");
                    for (int i = 0; i < entries.size(); i++) {
                        if (i > 0) {
                            propSb.append(", ");
                        }
                        Datum.PropListPair entry = entries.get(i);
                        propSb.append(formatDatum(entry.key, player));
                        propSb.append(": ");
                        propSb.append(formatDatum(entry.value, player));
                    }
                    propSb.append("]");
                    return propSb.toString();
                case Rect:
                    int[] rect = datum.toRect();
                    return String.format("rect(%s, %s, %s, %s)",
                        formatNumericValue(player.allocator.get(rect[0]), player),
                        formatNumericValue(player.allocator.get(rect[1]), player),
                        formatNumericValue(player.allocator.get(rect[2]), player),
                        formatNumericValue(player.allocator.get(rect[3]), player));
                case Point:
                    int[] point = datum.toPoint();
                    return String.format("point(%s, %s)",
                        formatNumericValue(player.allocator.get(point[0]), player),
                        formatNumericValue(player.allocator.get(point[1]), player));
                case Vector:
                    double[] v = datum.toVector();
                    return String.format("vector(%s, %s, %s)",
                        formatFloatWithPrecision(v[0], player),
                        formatFloatWithPrecision(v[1], player),
                        formatFloatWithPrecision(v[2], player));
                case ColorRef:
                    ColorRef cr = datum.toColorRef();
                    if (cr.isPaletteIndex()) {
                        return "color(" + cr.getPaletteIndex() + ")";
                    } else {
                        return "rgb(" + cr.getR() + ", " + cr.getG() + ", " + cr.getB() + ")";
                    }
                case SpriteRef:
                    return "(sprite " + datum.toSpriteRef() + ")";
                case CastMemberRef:
                    return "(member " + datum.toMemberRef().castMember + " of castLib " + datum.toMemberRef().castLib + ")";
                case SoundChannel:
                    return "<soundChannel>";
                case CursorRef:
                    return "<cursor>";
                case BitmapRef:
                    return "<bitmap>";
                case DateRef:
                    return "<date>";
                case MathRef:
                    return "<math>";
                case XmlRef:
                    return "<xml:" + datum.getDateRef() + ">";
                default:
                    return "<" + type.getTypeName() + ">";
            }
        } catch (ScriptError e) {
            return "<error: " + e.getMessage() + ">";
        }
    }

    /**
     * Format a numeric value (int or float) for display.
     */
    public static String formatNumericValue(Datum datum, DirPlayer player) {
        try {
            if (datum.isInt()) {
                return String.valueOf(datum.intValue());
            } else if (datum.isFloat()) {
                return formatFloatWithPrecision(datum.floatValue(), player);
            } else {
                return formatConcreteDatum(datum, player);
            }
        } catch (ScriptError e) {
            return "<error>";
        }
    }

    /**
     * Format a float value according to the player's floatPrecision setting.
     */
    public static String formatFloatWithPrecision(double val, DirPlayer player) {
        // Normalize negative zero to positive zero
        if (val == 0.0) {
            val = 0.0;
        }

        int fp = player.floatPrecision;

        // Calculate how many characters the decimal notation would take
        int integerDigits;
        if (Math.abs(val) < 1.0) {
            integerDigits = 1; // Just "0"
        } else {
            integerDigits = Math.max(1, (int) (Math.floor(Math.log10(Math.abs(val))) + 1));
        }

        int decimalPlaces = fp > 0 ? fp : 0;
        int totalChars = integerDigits + 1 + decimalPlaces; // digits + '.' + decimals

        // Director switches to scientific notation when formatted string >= 18 chars
        if (totalChars >= 18) {
            return String.format("%.14e", val);
        }

        // Normal formatting based on floatPrecision
        if (fp > 0) {
            int p = Math.min(fp, 15);
            return String.format("%." + p + "f", val);
        } else if (fp == 0) {
            return String.valueOf((int) Math.round(val));
        } else {
            int p = Math.min(-fp, 15);
            double pow = Math.pow(10, p);
            double rounded = Math.round(val * pow) / pow;
            String s = String.format("%." + p + "f", rounded);
            // Trim trailing zeros and decimal point
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            return s;
        }
    }

    /**
     * Parse a string to double, returning 0.0 if parsing fails.
     */
    private static double parseDoubleOrZero(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
