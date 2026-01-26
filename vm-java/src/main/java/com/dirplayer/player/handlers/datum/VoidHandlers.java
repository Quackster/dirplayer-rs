package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for void datum operations.
 * Port of Rust VoidDatumHandlers.
 */
public class VoidHandlers {

    /**
     * Call handler on void datum.
     * Most operations on void silently return void or fail gracefully.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            // Calling addAt/add/append on void should just return void
            // In Director, operations on void typically no-op and return void
            case "addat":
            case "add":
            case "append":
                return 0; // Void
            default:
                throw new ScriptError("No handler " + handlerName + " for void");
        }
    }

    /**
     * Get a property from void.
     * Some properties return sensible defaults, others throw errors.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("void"));

            case "length":
                return player.allocDatum(Datum.ofInt(0));

            case "string":
                return player.allocDatum(Datum.ofString(""));

            // XML-related properties on Void should return empty/void values
            case "childnodes":
                // Return empty list for childNodes on void
                return player.allocDatum(Datum.ofList(DatumType.List, new ArrayList<>(), false));

            case "firstchild":
            case "lastchild":
            case "parentnode":
            case "nextsibling":
            case "previoussibling":
                // Return void for node navigation on void
                return player.allocDatum(Datum.VOID);

            case "nodename":
            case "nodevalue":
                // Return empty string for node properties on void
                return player.allocDatum(Datum.ofString(""));

            case "attributes":
                // Return void for attributes on void
                return player.allocDatum(Datum.VOID);

            default:
                throw new ScriptError(
                    "Cannot get property '" + prop + "' on VOID - a variable or property " +
                    "that should contain an object is uninitialized"
                );
        }
    }

    /**
     * Set a property on void - typically an error.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        throw new ScriptError(
            "Cannot set property '" + prop + "' on VOID - a variable or property " +
            "that should contain an object is uninitialized"
        );
    }
}
