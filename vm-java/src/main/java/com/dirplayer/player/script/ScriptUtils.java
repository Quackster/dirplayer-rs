package com.dirplayer.player.script;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

/**
 * Utility methods for script property access.
 * Port of Rust script.rs helper functions.
 */
public class ScriptUtils {

    /**
     * Get a property from a script instance (optional - returns null if not found).
     */
    public static Integer scriptGetPropOpt(DirPlayer player, ScriptInstanceRef instanceRef, String propName) {
        ScriptInstance instance = player.allocator.getScriptInstance(instanceRef);
        if (instance == null) {
            return null;
        }

        // Handle special "ancestor" property
        if ("ancestor".equals(propName)) {
            if (instance.ancestor != 0) {
                return instance.ancestor;  // Already a datum ref
            } else {
                return 0; // Void
            }
        }

        // Try to find the property on the current instance first
        Integer propValue = instance.getProperty(propName);
        if (propValue != null) {
            return propValue;
        }

        // Check ancestor for the property
        if (instance.ancestor != 0) {
            Datum ancestorDatum = player.getDatum(instance.ancestor);
            if (ancestorDatum != null && ancestorDatum.isScriptInstanceRef()) {
                try {
                    ScriptInstanceRef ancestorRef = ancestorDatum.toScriptInstanceRef();
                    Integer ancestorProp = scriptGetPropOpt(player, ancestorRef, propName);
                    if (ancestorProp != null) {
                        return ancestorProp;
                    }
                } catch (ScriptError e) {
                    // Ignore
                }
            }
        }

        // Fall back to built-in "class" property if not found
        if ("class".equals(propName)) {
            return player.allocDatum(Datum.ofScriptRef(instance.script));
        }

        return null;
    }

    /**
     * Get a static property from a script (not an instance).
     */
    public static int scriptGetStaticProp(DirPlayer player, CastMemberRef scriptRef, String propName) throws ScriptError {
        Script script = player.movie.castManager.getScriptByRef(scriptRef);
        if (script == null) {
            throw new ScriptError("Script not found: " + scriptRef);
        }

        Integer prop = script.properties.get(propName);
        if (prop != null) {
            return prop;
        } else {
            throw new ScriptError("Cannot get static property " + propName + " on script " + script.name);
        }
    }

    /**
     * Set a static property on a script (not an instance).
     */
    public static void scriptSetStaticProp(DirPlayer player, CastMemberRef scriptRef, String propName,
                                           int valueRef, boolean required) throws ScriptError {
        Script script = player.movie.castManager.getScriptByRef(scriptRef);
        if (script == null) {
            throw new ScriptError("Script not found: " + scriptRef);
        }

        if (required && !script.properties.containsKey(propName)) {
            throw new ScriptError("Cannot set static property " + propName + " on script " + script.name);
        }

        script.properties.put(propName, valueRef);
    }

    /**
     * Get a property from a script instance (required - throws if not found).
     */
    public static int scriptGetProp(DirPlayer player, ScriptInstanceRef instanceRef, String propName) throws ScriptError {
        Integer prop = scriptGetPropOpt(player, instanceRef, propName);
        if (prop != null) {
            return prop;
        }

        ScriptInstance instance = player.allocator.getScriptInstance(instanceRef);
        String validProps = instance != null ? String.join(", ", instance.properties.keySet()) : "none";
        throw new ScriptError("Cannot get property " + propName + " on script instance. Valid properties: " + validProps);
    }

    /**
     * Set a property on a script instance.
     */
    public static void scriptSetProp(DirPlayer player, ScriptInstanceRef instanceRef, String propName,
                                     int valueRef, boolean required) throws ScriptError {
        // Handle special "ancestor" property
        if ("ancestor".equals(propName)) {
            ScriptInstance instance = player.allocator.getScriptInstanceMut(instanceRef);
            if (instance != null) {
                instance.ancestor = valueRef;  // Store as datum ref directly
            }
            return;
        }

        // Try to set the property on the current instance
        ScriptInstance instance = player.allocator.getScriptInstanceMut(instanceRef);
        if (instance != null && instance.hasProperty(propName)) {
            instance.setProperty(propName, valueRef);
            return;
        }

        // Try to set it on the ancestor
        if (instance != null && instance.ancestor != 0) {
            Datum ancestorDatum = player.getDatum(instance.ancestor);
            if (ancestorDatum != null && ancestorDatum.isScriptInstanceRef()) {
                try {
                    ScriptInstanceRef ancestorRef = ancestorDatum.toScriptInstanceRef();
                    scriptSetProp(player, ancestorRef, propName, valueRef, true);
                    return;
                } catch (ScriptError e) {
                    // Ancestor doesn't have it either
                }
            }
        }

        // If not required, add it to the instance
        if (!required && instance != null) {
            instance.setProperty(propName, valueRef);
            return;
        }

        throw new ScriptError("Cannot set property " + propName + " on script instance");
    }
}
