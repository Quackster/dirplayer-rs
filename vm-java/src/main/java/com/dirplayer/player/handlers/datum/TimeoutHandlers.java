package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.TimeoutManager;

import java.util.List;

/**
 * Handlers for timeout datum operations.
 * Port of Rust TimeoutDatumHandlers.
 */
public class TimeoutHandlers {

    /**
     * Check if handler is async.
     */
    public static boolean hasAsyncHandler(String name) {
        return name.equalsIgnoreCase("new");
    }

    /**
     * Call handler on timeout datum (synchronous handlers only).
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "forget":
                return forget(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for timeout");
        }
    }

    /**
     * setAt handler - supports #ancestor for Object Manager compatibility.
     */
    private static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String key = player.getDatum(args.get(0)).stringValue();

        if (key.equalsIgnoreCase("ancestor")) {
            // Silently accept but ignore - timeouts don't use ancestor chains
            return 0; // Void
        }

        throw new ScriptError("Cannot setAt property " + key + " on timeout");
    }

    /**
     * Forget (cancel) the timeout.
     */
    private static int forget(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String timeoutName = getTimeoutName(player, datumRef);
        if (timeoutName != null) {
            player.timeoutManager.remove(timeoutName);
        }
        return 0; // Void
    }

    /**
     * Create a new timeout.
     * Note: In Java, this is typically handled by the VM's async system.
     * This method provides the synchronous implementation.
     */
    public static int createTimeout(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        String timeoutName;
        int periodArgIdx, handlerArgIdx, targetArgIdx;

        if (datum.getType() == DatumType.TimeoutFactory) {
            // Factory call: timeout().new("name", ...)
            if (args.isEmpty()) {
                throw new ScriptError("timeout.new() requires at least a name argument");
            }
            timeoutName = player.getDatum(args.get(0)).stringValue();

            if (args.size() < 4) {
                throw new ScriptError("timeout.new() requires 4 arguments: name, period, handler, target");
            }
            periodArgIdx = 1;
            handlerArgIdx = 2;
            targetArgIdx = 3;
        } else if (datum.getType() == DatumType.TimeoutRef) {
            // Named call: timeout("name").new(...)
            timeoutName = datum.getTimeoutName();

            if (args.size() < 3) {
                throw new ScriptError("timeout(name).new() requires 3 arguments: period, handler, target");
            }
            periodArgIdx = 0;
            handlerArgIdx = 1;
            targetArgIdx = 2;
        } else {
            throw new ScriptError("Cannot create timeout from non-timeout");
        }

        // Check if this timeout name corresponds to a script in the cast (Director 10+)
        int dirVersion = player.movie.getDirVersion();

        if (dirVersion >= 1000) {
            // Check for script-based timeout
            var scriptRef = player.movie.castManager.findMemberRefByName(timeoutName);
            if (scriptRef != null) {
                var script = player.movie.castManager.getScriptByRef(scriptRef);
                if (script != null) {
                    // Script-based timeout - delegate to ScriptHandlers
                    ScriptHandlers.ScriptInstanceResult result = ScriptHandlers.createScriptInstance(player, scriptRef);

                    // Return a TimeoutInstance wrapping the script instance
                    return player.allocDatum(Datum.ofTimeoutInstance(
                        timeoutName,
                        0, // Script-based timeouts manage their own duration
                        0, // Void callback
                        0, // Void target
                        result.datumRef
                    ));
                }
            }
        }

        // Create traditional timeout
        int timeoutPeriod = player.getDatum(args.get(periodArgIdx)).intValue();

        Datum handlerDatum = player.getDatum(args.get(handlerArgIdx));
        String timeoutHandler;
        if (handlerDatum.isString()) {
            timeoutHandler = handlerDatum.stringValue();
        } else if (handlerDatum.isSymbol()) {
            timeoutHandler = handlerDatum.symbolValue();
        } else {
            throw new ScriptError("Timeout handler must be a string or symbol");
        }

        int targetRef = args.get(targetArgIdx);

        TimeoutManager.Timeout timeout = new TimeoutManager.Timeout(timeoutName, timeoutPeriod);
        timeout.callback = args.get(handlerArgIdx);
        timeout.target = targetRef;
        player.timeoutManager.add(timeoutName, timeout);

        // Return a TimeoutInstance
        return player.allocDatum(Datum.ofTimeoutInstance(
            timeoutName,
            timeoutPeriod,
            args.get(handlerArgIdx),
            targetRef,
            null
        ));
    }

    /**
     * Get a property from a timeout datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        String timeoutName = getTimeoutName(player, datumRef);
        if (timeoutName == null) {
            throw new ScriptError("Cannot get prop of non-timeout");
        }

        switch (prop.toLowerCase()) {
            case "name":
                return player.allocDatum(Datum.ofString(timeoutName));

            case "target":
                if (datum.getType() == DatumType.TimeoutRef) {
                    TimeoutManager.Timeout timeout = player.timeoutManager.get(timeoutName);
                    if (timeout != null) {
                        return timeout.target;
                    }
                    return 0; // Void
                } else if (datum.getType() == DatumType.TimeoutInstance) {
                    return datum.getTimeoutTarget();
                }
                return 0; // Void

            default:
                throw new ScriptError("Cannot get timeout property " + prop);
        }
    }

    /**
     * Set a property on a timeout datum.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        String timeoutName = getTimeoutName(player, datumRef);
        if (timeoutName == null) {
            throw new ScriptError("Cannot set prop of non-timeout");
        }

        switch (prop.toLowerCase()) {
            case "target":
                TimeoutManager.Timeout timeout = player.timeoutManager.get(timeoutName);
                if (timeout != null) {
                    timeout.target = valueRef;
                } else {
                    throw new ScriptError("Cannot set target of unscheduled timeout");
                }
                break;

            default:
                throw new ScriptError("Cannot set timeout property " + prop);
        }
    }

    // Helper methods

    private static String getTimeoutName(DirPlayer player, int datumRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        switch (datum.getType()) {
            case TimeoutRef:
                return datum.getTimeoutName();
            case TimeoutInstance:
                return datum.getTimeoutName();
            default:
                return null;
        }
    }
}
