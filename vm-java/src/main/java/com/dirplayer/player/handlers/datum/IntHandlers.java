package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

/**
 * Handlers for integer datum operations.
 * Port of Rust IntDatumHandlers.
 */
public class IntHandlers {

    /**
     * Get a property from an integer datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        int intValue = player.getDatum(datumRef).intValue();

        switch (prop.toLowerCase()) {
            case "abs":
                return player.allocDatum(Datum.ofInt(Math.abs(intValue)));
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("integer"));
            case "integer":
                return datumRef;
            case "float":
                return player.allocDatum(Datum.ofFloat(intValue));
            case "number":
                return datumRef;
            case "char":
                if (intValue >= 0 && intValue <= 255) {
                    char ch = (char) intValue;
                    return player.allocDatum(Datum.ofString(String.valueOf(ch)));
                } else {
                    throw new ScriptError("Integer " + intValue + " out of range for char (must be 0-255)");
                }
            case "string":
                return player.allocDatum(Datum.ofString(String.valueOf(intValue)));
            default:
                throw new ScriptError("Cannot get int property " + prop);
        }
    }
}
