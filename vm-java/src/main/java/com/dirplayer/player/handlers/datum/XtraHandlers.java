package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.SimpleLogger;

import java.util.List;

/**
 * Handlers for Xtra and XtraInstance datum operations.
 * Port of Rust XtraHandlers.
 */
public class XtraHandlers {
    private static final SimpleLogger logger = SimpleLogger.getLogger(XtraHandlers.class);

    /**
     * Call a method on an Xtra instance.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        // Convert args to Datum array
        Datum[] argDatums = new Datum[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argDatums[i] = player.getDatum(args.get(i));
        }

        // Call through XtraManager
        Datum result = player.xtraManager.callXtraMethod(datum, handlerName, argDatums);
        return player.allocDatum(result);
    }

    /**
     * Get a property from an Xtra instance.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        Datum result = player.xtraManager.getXtraProperty(datum, prop);
        return player.allocDatum(result);
    }

    /**
     * Set a property on an Xtra instance.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        Datum value = player.getDatum(valueRef);
        player.xtraManager.setXtraProperty(datum, prop, value);
    }
}
