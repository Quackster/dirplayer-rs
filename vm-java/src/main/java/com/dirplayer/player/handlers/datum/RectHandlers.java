package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for rect datum operations.
 * Port of Rust RectDatumHandlers and RectUtils.
 */
public class RectHandlers {

    /**
     * Union of two rects.
     */
    public static int[] union(int[] rect1, int[] rect2) {
        int left = Math.min(rect1[0], rect2[0]);
        int top = Math.min(rect1[1], rect2[1]);
        int right = Math.max(rect1[2], rect2[2]);
        int bottom = Math.max(rect1[3], rect2[3]);
        return new int[] { left, top, right, bottom };
    }

    /**
     * Intersection of two rects.
     */
    public static int[] intersectRects(int[] rect1, int[] rect2) {
        int left = Math.max(rect1[0], rect2[0]);
        int top = Math.max(rect1[1], rect2[1]);
        int right = Math.min(rect1[2], rect2[2]);
        int bottom = Math.min(rect1[3], rect2[3]);

        // If rectangles don't overlap, return empty rect
        if (left >= right || top >= bottom) {
            return new int[] { 0, 0, 0, 0 };
        }
        return new int[] { left, top, right, bottom };
    }

    /**
     * Get item at index (1-4 for left, top, right, bottom).
     */
    public static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int[] arr = player.getDatum(datumRef).toRect();
        int index = player.getDatum(args.get(0)).intValue();

        if (index < 1 || index > 4) {
            throw new ScriptError("Invalid index for rect");
        }

        int valueRef = arr[index - 1];
        Datum value = player.getDatum(valueRef);

        if (!value.isInt() && !value.isFloat()) {
            throw new ScriptError("Rect component is not numeric: " + value.typeStr());
        }

        return valueRef;
    }

    /**
     * Set item at index (1-4 for left, top, right, bottom).
     */
    public static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int index = player.getDatum(args.get(0)).intValue();
        Datum newVal = player.getDatum(args.get(1));

        if (index < 1 || index > 4) {
            throw new ScriptError("Invalid index for rect");
        }

        int newRef;
        if (newVal.isInt()) {
            newRef = player.allocDatum(Datum.ofInt(newVal.intValue()));
        } else if (newVal.isFloat()) {
            newRef = player.allocDatum(Datum.ofFloat(newVal.floatValue()));
        } else {
            throw new ScriptError("Rect component must be numeric, got " + newVal.typeStr());
        }

        player.getDatum(datumRef).setRectAt(index - 1, newRef);
        return 0; // Void
    }

    /**
     * Intersect with another rect.
     */
    public static int intersect(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int[] r1 = player.getDatum(datumRef).toRect();
        int[] r2 = player.getDatum(args.get(0)).toRect();

        // Get numeric values (handles both int and float refs)
        double l = Math.min(player.getDatum(r1[0]).floatValue(), player.getDatum(r2[0]).floatValue());
        double t = Math.min(player.getDatum(r1[1]).floatValue(), player.getDatum(r2[1]).floatValue());
        double r = Math.max(player.getDatum(r1[2]).floatValue(), player.getDatum(r2[2]).floatValue());
        double b = Math.max(player.getDatum(r1[3]).floatValue(), player.getDatum(r2[3]).floatValue());

        int leftRef = player.allocDatum(Datum.fromF64(l));
        int topRef = player.allocDatum(Datum.fromF64(t));
        int rightRef = player.allocDatum(Datum.fromF64(r));
        int bottomRef = player.allocDatum(Datum.fromF64(b));

        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
    }

    /**
     * Get a property from a rect.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        int[] rectArr = player.getDatum(datumRef).toRect();

        double left = player.getDatum(rectArr[0]).floatValue();
        double top = player.getDatum(rectArr[1]).floatValue();
        double right = player.getDatum(rectArr[2]).floatValue();
        double bottom = player.getDatum(rectArr[3]).floatValue();

        switch (prop.toLowerCase()) {
            case "width":
                return player.allocDatum(Datum.fromF64(right - left));
            case "height":
                return player.allocDatum(Datum.fromF64(bottom - top));
            case "left":
                return player.allocDatum(Datum.fromF64(left));
            case "top":
                return player.allocDatum(Datum.fromF64(top));
            case "right":
                return player.allocDatum(Datum.fromF64(right));
            case "bottom":
                return player.allocDatum(Datum.fromF64(bottom));
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("rect"));
            default:
                throw new ScriptError("Cannot get rect property " + prop);
        }
    }

    /**
     * Set a property on a rect.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        int idx;
        switch (prop.toLowerCase()) {
            case "left":
                idx = 0;
                break;
            case "top":
                idx = 1;
                break;
            case "right":
                idx = 2;
                break;
            case "bottom":
                idx = 3;
                break;
            default:
                throw new ScriptError("Cannot set rect property " + prop);
        }

        Datum newVal = player.getDatum(valueRef);
        int newRef;
        if (newVal.isInt()) {
            newRef = player.allocDatum(Datum.ofInt(newVal.intValue()));
        } else if (newVal.isFloat()) {
            newRef = player.allocDatum(Datum.ofFloat(newVal.floatValue()));
        } else {
            throw new ScriptError("Rect property must be numeric, got " + newVal.typeStr());
        }

        player.getDatum(datumRef).setRectAt(idx, newRef);
    }

    /**
     * Call handler on rect datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "getat":
                return getAt(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            case "intersect":
                return intersect(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for rect");
        }
    }
}
