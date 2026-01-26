package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for symbol datum operations.
 * Port of Rust SymbolDatumHandlers.
 */
public class SymbolHandlers {

    /**
     * Get a property from a symbol.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("symbol"));
            case "string":
                String symbolValue = player.getDatum(datumRef).symbolValue();
                return player.allocDatum(Datum.ofString(symbolValue));
            default:
                throw new ScriptError("Cannot get symbol property " + prop);
        }
    }

    /**
     * Call handler on symbol datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        throw new ScriptError("No handler " + handlerName + " for symbol");
    }
}
