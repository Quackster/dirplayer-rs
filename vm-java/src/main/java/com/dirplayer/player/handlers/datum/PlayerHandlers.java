package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for player datum operations.
 * Port of Rust PlayerDatumHandlers.
 */
public class PlayerHandlers {

    /**
     * Call handler on player datum.
     */
    public static int call(DirPlayer player, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "count":
                return count(player, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for player datum");
        }
    }

    /**
     * Count elements in a player collection.
     */
    private static int count(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("count requires at least one argument");
        }

        String subject = player.getDatum(args.get(0)).stringValue();

        switch (subject.toLowerCase()) {
            case "windowlist":
                return player.allocDatum(Datum.ofInt(0));
            default:
                throw new ScriptError("Invalid call _player.count(" + subject + ")");
        }
    }
}
