package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for script datum operations.
 * Port of Rust ScriptDatumHandlers.
 */
public class ScriptHandlers {

    /**
     * Check if handler is async (requires special handling).
     */
    public static boolean hasAsyncHandler(DirPlayer player, int datumRef, String name) {
        switch (name.toLowerCase()) {
            case "new":
                return true;
            case "handler":
                return false;
            default:
                // Check if the script has a handler with this name
                Datum datum = player.getDatum(datumRef);
                if (datum.getType() == DatumType.ScriptRef) {
                    try {
                        CastMemberRef scriptRef = datum.getScriptRef();
                        Script script = player.movie.castManager.getScriptByRef(scriptRef);
                        if (script != null) {
                            return script.getOwnHandler(name) != null;
                        }
                    } catch (ScriptError e) {
                        return false;
                    }
                }
                return false;
        }
    }

    /**
     * Call handler on script datum (synchronous handlers only).
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "handler":
                return handler(player, datumRef, args);
            case "handlers":
                return handlers(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for script datum");
        }
    }

    /**
     * Get list of handler names in the script.
     */
    private static int handlers(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        CastMemberRef scriptRef = player.getDatum(datumRef).getScriptRef();
        Script script = player.movie.castManager.getScriptByRef(scriptRef);

        if (script == null) {
            throw new ScriptError("Script not found");
        }

        List<String> handlerNames = script.handlerNames;
        List<Integer> handlerNameDatums = new ArrayList<>();

        for (String name : handlerNames) {
            handlerNameDatums.add(player.allocDatum(Datum.ofSymbol(name)));
        }

        return player.allocDatum(Datum.ofList(DatumType.List, handlerNameDatums, false));
    }

    /**
     * Check if the script has a specific handler.
     */
    private static int handler(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String name = player.getDatum(args.get(0)).stringValue();
        CastMemberRef scriptRef = player.getDatum(datumRef).getScriptRef();
        Script script = player.movie.castManager.getScriptByRef(scriptRef);

        if (script == null) {
            throw new ScriptError("Cannot get handlers of non-script");
        }

        com.dirplayer.director.chunks.HandlerDef ownHandler = script.getOwnHandler(name);
        return player.allocDatum(Datum.ofInt(ownHandler != null ? 1 : 0));
    }

    /**
     * Create a new script instance.
     * This is the async "new" handler - in Java, it's called by the VM when needed.
     */
    public static ScriptInstanceResult createScriptInstance(DirPlayer player, CastMemberRef scriptRef) throws ScriptError {
        Script script = player.movie.castManager.getScriptByRef(scriptRef);

        if (script == null) {
            throw new ScriptError("Script not found: " + scriptRef);
        }

        com.dirplayer.player.DatumAllocator.ScriptInstance instance = new com.dirplayer.player.DatumAllocator.ScriptInstance();
        instance.scriptRef = scriptRef;
        instance.properties = new java.util.HashMap<>();

        int instanceRef = player.allocator.allocScriptInstance(instance);
        instance.id = instanceRef; // Set the ID after allocation
        int datumRef = player.allocDatum(Datum.ofScriptInstanceRef(instanceRef));

        return new ScriptInstanceResult(instanceRef, datumRef);
    }

    /**
     * Result holder for script instance creation.
     */
    public static class ScriptInstanceResult {
        public final int instanceRef;
        public final int datumRef;

        public ScriptInstanceResult(int instanceRef, int datumRef) {
            this.instanceRef = instanceRef;
            this.datumRef = datumRef;
        }
    }

    /**
     * Get property from a script datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String propName) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.getType() != DatumType.ScriptRef) {
            throw new ScriptError("Cannot get property of non-script");
        }

        switch (propName.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("script"));
            default:
                throw new ScriptError("Cannot get script property " + propName);
        }
    }
}
