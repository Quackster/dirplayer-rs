package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

/**
 * Handlers for float datum operations.
 * Port of Rust FloatDatumHandlers.
 */
public class FloatHandlers {

    /**
     * Get a property from a float datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        double floatValue = player.getDatum(datumRef).floatValue();

        switch (prop.toLowerCase()) {
            case "abs":
                return player.allocDatum(Datum.ofFloat(Math.abs(floatValue)));
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("float"));
            case "integer":
                return player.allocDatum(Datum.ofInt((int) Math.round(floatValue)));
            case "float":
                return datumRef;
            case "char":
                int intValue = (int) Math.round(floatValue);
                if (intValue >= 0 && intValue <= 255) {
                    char ch = (char) intValue;
                    return player.allocDatum(Datum.ofString(String.valueOf(ch)));
                } else {
                    throw new ScriptError("Float " + floatValue + " out of range for char (must be 0-255)");
                }
            case "string":
                return player.allocDatum(Datum.ofString(String.valueOf(floatValue)));
            default:
                throw new ScriptError("Cannot get float property " + prop);
        }
    }
}
