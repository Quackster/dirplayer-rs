package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for point datum operations.
 * Port of Rust PointDatumHandlers.
 */
public class PointHandlers {

    /**
     * Get item at index (1 for x/locH, 2 for y/locV).
     */
    public static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int[] pointArr = player.getDatum(datumRef).toPoint();
        int index = player.getDatum(args.get(0)).intValue();

        if (index < 1 || index > 2) {
            throw new ScriptError("Invalid index for point");
        }

        int valueRef = pointArr[index - 1];
        Datum value = player.getDatum(valueRef);

        if (!value.isInt() && !value.isFloat()) {
            throw new ScriptError("Point component is not numeric: " + value.typeStr());
        }

        return valueRef;
    }

    /**
     * Set item at index (1 for x/locH, 2 for y/locV).
     */
    public static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int index = player.getDatum(args.get(0)).intValue();

        if (index < 1 || index > 2) {
            throw new ScriptError("Invalid index for point");
        }

        Datum newVal = player.getDatum(args.get(1));
        int newRef;
        if (newVal.isInt()) {
            newRef = player.allocDatum(Datum.ofInt(newVal.intValue()));
        } else if (newVal.isFloat()) {
            newRef = player.allocDatum(Datum.ofFloat(newVal.floatValue()));
        } else {
            throw new ScriptError("Point component must be numeric, got " + newVal.typeStr());
        }

        player.getDatum(datumRef).setPointAt(index - 1, newRef);
        return 0; // Void
    }

    /**
     * Check if point is inside a rect.
     */
    public static int inside(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int[] point = player.getDatum(datumRef).toPoint();
        int[] rect = player.getDatum(args.get(0)).toRect();

        int px = player.getDatum(point[0]).intValue();
        int py = player.getDatum(point[1]).intValue();

        int x1 = player.getDatum(rect[0]).intValue();
        int y1 = player.getDatum(rect[1]).intValue();
        int x2 = player.getDatum(rect[2]).intValue();
        int y2 = player.getDatum(rect[3]).intValue();

        boolean isInside = x1 <= px && px < x2 && y1 <= py && py < y2;
        return player.allocDatum(Datum.ofInt(isInside ? 1 : 0));
    }

    /**
     * Get a property from a point.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        int[] pointArr = player.getDatum(datumRef).toPoint();

        double x = player.getDatum(pointArr[0]).floatValue();
        double y = player.getDatum(pointArr[1]).floatValue();

        switch (prop.toLowerCase()) {
            case "loch":
                return player.allocDatum(Datum.fromF64(x));
            case "locv":
                return player.allocDatum(Datum.fromF64(y));
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("point"));
            default:
                throw new ScriptError("Cannot get point property " + prop);
        }
    }

    /**
     * Set a property on a point.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        Datum newVal = player.getDatum(valueRef);

        int idx;
        switch (prop.toLowerCase()) {
            case "loch":
                idx = 0;
                break;
            case "locv":
                idx = 1;
                break;
            default:
                throw new ScriptError("Cannot set point property " + prop);
        }

        int newRef;
        if (newVal.isInt()) {
            newRef = player.allocDatum(Datum.ofInt(newVal.intValue()));
        } else if (newVal.isFloat()) {
            newRef = player.allocDatum(Datum.ofFloat(newVal.floatValue()));
        } else {
            throw new ScriptError("Point property must be numeric, got " + newVal.typeStr());
        }

        player.getDatum(datumRef).setPointAt(idx, newRef);
    }

    /**
     * Call handler on point datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "getat":
                return getAt(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            case "inside":
                return inside(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for point");
        }
    }
}
