package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.SimpleLogger;


/**
 * Datum comparison utilities.
 * Port of Rust compare.rs
 */
public class DatumCompare {
    private static final SimpleLogger logger = SimpleLogger.getLogger(DatumCompare.class);

    /**
     * Check if two datums are equal.
     * Director equality is case-insensitive for strings.
     */
    public static boolean datumEquals(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        DatumType leftType = left.getType();
        DatumType rightType = right.getType();

        // Same type comparisons
        if (leftType == rightType) {
            switch (leftType) {
                case Int:
                    return left.intValue() == right.intValue();
                case Float:
                    return left.floatValue() == right.floatValue();
                case String:
                    // Director strings are case-insensitive
                    return left.stringValue().equalsIgnoreCase(right.stringValue());
                case Symbol:
                    return left.stringValue().equalsIgnoreCase(right.stringValue());
                case Void:
                    return true;
                case Null:
                    return true;
                case List:
                    return listEquals(left, right, allocator);
                case PropList:
                    return propListEquals(left, right, allocator);
                case Point:
                    return pointEquals(left, right, allocator);
                case Rect:
                    return rectEquals(left, right, allocator);
                case CastMemberRef:
                    return left.toMemberRef().equals(right.toMemberRef());
                case SpriteRef:
                    return left.intValue() == right.intValue();
                case ColorRef:
                    return left.toColorRef().equals(right.toColorRef());
                default:
                    logger.warn("Unhandled equals comparison for type: {}", leftType);
                    return false;
            }
        }

        // Cross-type comparisons
        if (leftType == DatumType.Int && rightType == DatumType.Float) {
            return left.intValue() == right.floatValue();
        }
        if (leftType == DatumType.Float && rightType == DatumType.Int) {
            return left.floatValue() == right.intValue();
        }
        if (leftType == DatumType.Void && rightType == DatumType.Int) {
            return right.intValue() == 0;
        }
        if (leftType == DatumType.Int && rightType == DatumType.Void) {
            return left.intValue() == 0;
        }
        if (leftType == DatumType.String && rightType == DatumType.Int) {
            try {
                return Integer.parseInt(left.stringValue()) == right.intValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (leftType == DatumType.Int && rightType == DatumType.String) {
            try {
                return left.intValue() == Integer.parseInt(right.stringValue());
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Default: not equal
        return false;
    }

    /**
     * Compare datums (less than).
     */
    public static boolean datumLessThan(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() < right.floatValue();
        }
        if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) < 0;
        }
        throw new ScriptError("Cannot compare " + left.getType() + " with " + right.getType());
    }

    /**
     * Compare datums (less than or equal).
     */
    public static boolean datumLessThanEqual(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        return datumLessThan(left, right, allocator) || datumEquals(left, right, allocator);
    }

    /**
     * Compare datums (greater than).
     */
    public static boolean datumGreaterThan(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() > right.floatValue();
        }
        if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) > 0;
        }
        throw new ScriptError("Cannot compare " + left.getType() + " with " + right.getType());
    }

    /**
     * Compare datums (greater than or equal).
     */
    public static boolean datumGreaterThanEqual(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        return datumGreaterThan(left, right, allocator) || datumEquals(left, right, allocator);
    }

    /**
     * Check if string contains substring.
     */
    public static boolean datumContains(Datum container, Datum item, DatumAllocator allocator) throws ScriptError {
        String containerStr = container.stringValue();
        String itemStr = item.stringValue();
        return containerStr.toLowerCase().contains(itemStr.toLowerCase());
    }

    private static boolean listEquals(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        java.util.List<Integer> leftList = left.toList();
        java.util.List<Integer> rightList = right.toList();
        if (leftList.size() != rightList.size()) {
            return false;
        }
        for (int i = 0; i < leftList.size(); i++) {
            Datum leftItem = allocator.get(leftList.get(i));
            Datum rightItem = allocator.get(rightList.get(i));
            if (!datumEquals(leftItem, rightItem, allocator)) {
                return false;
            }
        }
        return true;
    }

    private static boolean propListEquals(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        java.util.List<int[]> leftList = left.toPropList();
        java.util.List<int[]> rightList = right.toPropList();

        if (leftList.size() != rightList.size()) {
            return false;
        }

        // Compare each key-value pair
        for (int i = 0; i < leftList.size(); i++) {
            int[] leftPair = leftList.get(i);
            int[] rightPair = rightList.get(i);

            // Compare keys
            Datum leftKey = allocator.get(leftPair[0]);
            Datum rightKey = allocator.get(rightPair[0]);
            if (!datumEquals(leftKey, rightKey, allocator)) {
                return false;
            }

            // Compare values
            Datum leftValue = allocator.get(leftPair[1]);
            Datum rightValue = allocator.get(rightPair[1]);
            if (!datumEquals(leftValue, rightValue, allocator)) {
                return false;
            }
        }

        return true;
    }

    private static boolean pointEquals(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        int[] leftPoint = left.toPoint();
        int[] rightPoint = right.toPoint();
        return leftPoint[0] == rightPoint[0] && leftPoint[1] == rightPoint[1];
    }

    private static boolean rectEquals(Datum left, Datum right, DatumAllocator allocator) throws ScriptError {
        int[] leftRect = left.toRect();
        int[] rightRect = right.toRect();
        return leftRect[0] == rightRect[0] && leftRect[1] == rightRect[1] &&
               leftRect[2] == rightRect[2] && leftRect[3] == rightRect[3];
    }
}
