package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DatumAllocator.ScriptInstance;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.script.Script;
import com.dirplayer.player.script.ScriptHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for script instance datum operations.
 * Port of Rust ScriptInstanceDatumHandlers and ScriptInstanceUtils.
 */
public class ScriptInstanceHandlers {

    /**
     * Get script instance and its script definition.
     */
    public static ScriptInfo getScript(DirPlayer player, int datumRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (!datum.isScriptInstanceRef()) {
            throw new ScriptError("Cannot get script from non-script instance (" + datum.typeStr() + ")");
        }

        int instanceId = datum.getScriptInstanceRef();
        ScriptInstance instance = player.allocator.getScriptInstance(instanceId);
        if (instance == null) {
            throw new ScriptError("Script instance " + instanceId + " not found");
        }

        Script script = player.movie.castManager.getScriptByRef(instance.scriptRef);
        if (script == null) {
            throw new ScriptError("Script not found");
        }

        return new ScriptInfo(instanceId, instance, script);
    }

    /**
     * Get a handler from a script instance.
     */
    public static ScriptHandler getHandler(DirPlayer player, int datumRef, String name) throws ScriptError {
        ScriptInfo info = getScript(player, datumRef);
        return getHandlerFromInstance(player, info.instanceId, name);
    }

    /**
     * Get a handler from a script instance by ID.
     */
    public static ScriptHandler getHandlerFromInstance(DirPlayer player, int instanceId, String name) throws ScriptError {
        ScriptInstance instance = player.allocator.getScriptInstance(instanceId);
        if (instance == null) {
            return null;
        }

        Script script = player.movie.castManager.getScriptByRef(instance.scriptRef);
        if (script == null) {
            return null;
        }

        // Check own handler
        ScriptHandler handler = script.getHandler(name);
        if (handler != null) {
            return handler;
        }

        // Check ancestor
        if (instance.ancestor != 0) {
            Datum ancestorDatum = player.getDatum(instance.ancestor);
            if (ancestorDatum.isScriptInstanceRef()) {
                return getHandlerFromInstance(player, ancestorDatum.getScriptInstanceRef(), name);
            }
        }

        return null;
    }

    /**
     * Get a property from a script instance.
     */
    public static int getProp(DirPlayer player, int datumRef, String propName) throws ScriptError {
        int instanceId = player.getDatum(datumRef).getScriptInstanceRef();
        return getInstanceProp(player, instanceId, propName);
    }

    /**
     * Get a property from a script instance by ID.
     */
    public static int getInstanceProp(DirPlayer player, int instanceId, String propName) throws ScriptError {
        ScriptInstance instance = player.allocator.getScriptInstance(instanceId);
        if (instance == null) {
            throw new ScriptError("Script instance " + instanceId + " not found");
        }

        // Check built-in properties
        switch (propName.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("instance"));
            case "ancestor":
                if (instance.ancestor != 0) {
                    return instance.ancestor;
                }
                return player.allocDatum(Datum.ofInt(0));
            case "script": {
                Script script = player.movie.castManager.getScriptByRef(instance.scriptRef);
                if (script != null) {
                    return player.allocDatum(Datum.ofScriptRef(instance.scriptRef));
                }
                return 0; // Void
            }
        }

        // Check instance properties
        Integer propRef = instance.getProperty(propName);
        if (propRef != null) {
            return propRef;
        }

        // Check ancestor
        if (instance.ancestor != 0) {
            Datum ancestorDatum = player.getDatum(instance.ancestor);
            if (ancestorDatum.isScriptInstanceRef()) {
                return getInstanceProp(player, ancestorDatum.getScriptInstanceRef(), propName);
            }
        }

        return 0; // Void - property not found
    }

    /**
     * Set a property on a script instance.
     */
    public static void setProp(DirPlayer player, int datumRef, String propName, int valueRef) throws ScriptError {
        int instanceId = player.getDatum(datumRef).getScriptInstanceRef();
        setInstanceProp(player, instanceId, propName, valueRef);
    }

    /**
     * Set a property on a script instance by ID.
     */
    public static void setInstanceProp(DirPlayer player, int instanceId, String propName, int valueRef) throws ScriptError {
        ScriptInstance instance = player.allocator.getScriptInstance(instanceId);
        if (instance == null) {
            throw new ScriptError("Script instance " + instanceId + " not found");
        }

        // Handle special properties
        if (propName.equalsIgnoreCase("ancestor")) {
            Datum valueDatum = player.getDatum(valueRef);
            if (valueDatum.isVoid()) {
                // Setting ancestor to void is a no-op
                return;
            }
            if (valueDatum.isScriptInstanceRef()) {
                instance.ancestor = valueRef;
            } else {
                // Store non-ScriptInstanceRef ancestors in properties
                instance.setProperty("ancestor", valueRef);
            }
            return;
        }

        // Set instance property
        instance.setProperty(propName, valueRef);
    }

    /**
     * Call handler on script instance datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "setat":
                return setAt(player, datumRef, args);
            case "handler":
                return handler(player, datumRef, args);
            case "setaprop":
                return setAProp(player, datumRef, args);
            case "setprop":
                return setPropHandler(player, datumRef, args);
            case "getprop":
            case "getpropref":
                return getPropHandler(player, datumRef, args);
            case "getaprop":
                return getAProp(player, datumRef, args);
            case "getat":
                return getAt(player, datumRef, args);
            case "count":
                return count(player, datumRef, args);
            case "handlers":
                return handlers(player, datumRef, args);
            case "getpropertydescriptionlist":
                return player.allocDatum(Datum.ofPropList(new ArrayList<>(), false));
            // System events that should be silently ignored
            case "exitframe":
            case "enterframe":
            case "prepareframe":
            case "idle":
            case "stepframe":
            case "mousedown":
            case "mouseup":
            case "mouseenter":
            case "mouseleave":
            case "mousewithin":
            case "keydown":
            case "keyup":
            case "beginsprite":
            case "endsprite":
            case "preparemovie":
            case "startmovie":
            case "stopmovie":
            case "activate":
            case "deactivate":
            case "forget":
                return 0; // Void
            default:
                throw new ScriptError("No handler " + handlerName + " for script instance datum");
        }
    }

    // Handler implementations

    private static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String key = player.getDatum(args.get(0)).stringValue();
        int valueRef = args.get(1);

        if (key.equalsIgnoreCase("ancestor")) {
            setProp(player, datumRef, "ancestor", valueRef);
            return 0; // Void
        }

        throw new ScriptError("Cannot setAt property " + key + " on script instance datum");
    }

    private static int handler(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String name = player.getDatum(args.get(0)).stringValue();
        ScriptInfo info = getScript(player, datumRef);
        ScriptHandler h = info.script.getHandler(name);
        return player.allocDatum(Datum.ofInt(h != null ? 1 : 0));
    }

    private static int setAProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String propName = player.getDatum(args.get(0)).stringValue();
        int valueRef = args.get(1);
        setProp(player, datumRef, propName, valueRef);
        return 0; // Void
    }

    private static int setPropHandler(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.size() == 2) {
            String propName = player.getDatum(args.get(0)).stringValue();
            int valueRef = args.get(1);
            setProp(player, datumRef, propName, valueRef);
        } else if (args.size() == 3) {
            // setProp with nested access
            String localPropName = player.getDatum(args.get(0)).stringValue();
            int subKeyRef = args.get(1);
            int valueRef = args.get(2);

            int localPropRef = getProp(player, datumRef, localPropName);
            Datum localProp = player.getDatum(localPropRef);

            if (localProp.isList()) {
                ListHandlers.setAt(player, localPropRef, List.of(subKeyRef, valueRef));
            } else if (localProp.isPropList()) {
                PropListHandlers.setProp(player, localPropRef, subKeyRef, valueRef, false);
            } else {
                throw new ScriptError("Cannot set sub-property on " + localProp.typeStr());
            }
        }
        return 0; // Void
    }

    private static int getPropHandler(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String localPropName = player.getDatum(args.get(0)).stringValue();
        int localPropRef = getProp(player, datumRef, localPropName);

        if (args.size() > 1) {
            int subKeyRef = args.get(1);
            Datum localProp = player.getDatum(localPropRef);

            if (localProp.isList()) {
                return ListHandlers.getAt(player, localPropRef, List.of(subKeyRef));
            } else if (localProp.isPropList()) {
                return PropListHandlers.getAProp(player, localPropRef, List.of(subKeyRef));
            } else {
                throw new ScriptError("Cannot get sub-property from " + localProp.typeStr());
            }
        }

        return localPropRef;
    }

    private static int getAProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String propName = player.getDatum(args.get(0)).stringValue();
        return getProp(player, datumRef, propName);
    }

    private static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String key = player.getDatum(args.get(0)).stringValue();

        if (key.equalsIgnoreCase("ancestor")) {
            int instanceId = player.getDatum(datumRef).getScriptInstanceRef();
            ScriptInstance instance = player.allocator.getScriptInstance(instanceId);
            if (instance.ancestor != 0) {
                return instance.ancestor;
            }
            return player.allocDatum(Datum.ofInt(0));
        }

        return getAProp(player, datumRef, args);
    }

    private static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int instanceId = player.getDatum(datumRef).getScriptInstanceRef();
        ScriptInstance instance = player.allocator.getScriptInstance(instanceId);

        String propName = player.getDatum(args.get(0)).stringValue();
        Integer propRef = instance.getProperty(propName);

        if (propRef == null) {
            throw new ScriptError("Property " + propName + " not found");
        }

        Datum propValue = player.getDatum(propRef);
        int countVal;

        if (propValue.isList()) {
            countVal = propValue.toList().size();
        } else if (propValue.isPropList()) {
            countVal = propValue.toMap().size();
        } else {
            throw new ScriptError("Cannot count non-list property");
        }

        return player.allocDatum(Datum.ofInt(countVal));
    }

    private static int handlers(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        ScriptInfo info = getScript(player, datumRef);
        List<String> handlerNames = info.script.getHandlerNames();

        List<Integer> handlerRefs = new ArrayList<>();
        for (String name : handlerNames) {
            handlerRefs.add(player.allocDatum(Datum.ofSymbol(name)));
        }

        return player.allocDatum(Datum.ofList(DatumType.List, handlerRefs, false));
    }

    /**
     * Helper class to hold script instance info.
     */
    public static class ScriptInfo {
        public final int instanceId;
        public final ScriptInstance instance;
        public final Script script;

        public ScriptInfo(int instanceId, ScriptInstance instance, Script script) {
            this.instanceId = instanceId;
            this.instance = instance;
            this.script = script;
        }
    }
}
