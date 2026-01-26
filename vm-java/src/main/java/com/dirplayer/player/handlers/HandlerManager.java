package com.dirplayer.player.handlers;

import com.dirplayer.director.chunks.FrameLabelsChunk.FrameLabel;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.Datum.PropListPair;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.NetTask;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.CastLib;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.handlers.datum.ListHandlers;
import com.dirplayer.player.handlers.datum.PointHandlers;
import com.dirplayer.player.handlers.datum.PropListHandlers;
import com.dirplayer.player.handlers.datum.ScriptInstanceHandlers;
import com.dirplayer.player.script.ScriptInstanceRef;
import com.dirplayer.director.chunks.MemberType;

import com.dirplayer.SimpleLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Main handler manager that coordinates built-in function calls.
 * Port of Rust BuiltInHandlerManager from vm-rust/src/player/handlers/manager.rs.
 *
 * This class is the central dispatcher for all built-in Lingo functions,
 * coordinating between various specialized handler classes.
 */
public class HandlerManager {
    private static final SimpleLogger logger = SimpleLogger.getLogger(HandlerManager.class);

    private static final Random random = new Random();

    // Keyboard mapping tables for keyPressed function
    private static final Map<Character, Integer> CHAR_TO_KEYCODE = new HashMap<>();
    private static final Map<Character, Integer> DIRECTOR_SPECIAL_CHAR_TO_KEYCODE = new HashMap<>();

    static {
        // Initialize character to keycode mapping (lowercase chars)
        CHAR_TO_KEYCODE.put('a', 65);
        CHAR_TO_KEYCODE.put('b', 66);
        CHAR_TO_KEYCODE.put('c', 67);
        CHAR_TO_KEYCODE.put('d', 68);
        CHAR_TO_KEYCODE.put('e', 69);
        CHAR_TO_KEYCODE.put('f', 70);
        CHAR_TO_KEYCODE.put('g', 71);
        CHAR_TO_KEYCODE.put('h', 72);
        CHAR_TO_KEYCODE.put('i', 73);
        CHAR_TO_KEYCODE.put('j', 74);
        CHAR_TO_KEYCODE.put('k', 75);
        CHAR_TO_KEYCODE.put('l', 76);
        CHAR_TO_KEYCODE.put('m', 77);
        CHAR_TO_KEYCODE.put('n', 78);
        CHAR_TO_KEYCODE.put('o', 79);
        CHAR_TO_KEYCODE.put('p', 80);
        CHAR_TO_KEYCODE.put('q', 81);
        CHAR_TO_KEYCODE.put('r', 82);
        CHAR_TO_KEYCODE.put('s', 83);
        CHAR_TO_KEYCODE.put('t', 84);
        CHAR_TO_KEYCODE.put('u', 85);
        CHAR_TO_KEYCODE.put('v', 86);
        CHAR_TO_KEYCODE.put('w', 87);
        CHAR_TO_KEYCODE.put('x', 88);
        CHAR_TO_KEYCODE.put('y', 89);
        CHAR_TO_KEYCODE.put('z', 90);
        CHAR_TO_KEYCODE.put('0', 48);
        CHAR_TO_KEYCODE.put('1', 49);
        CHAR_TO_KEYCODE.put('2', 50);
        CHAR_TO_KEYCODE.put('3', 51);
        CHAR_TO_KEYCODE.put('4', 52);
        CHAR_TO_KEYCODE.put('5', 53);
        CHAR_TO_KEYCODE.put('6', 54);
        CHAR_TO_KEYCODE.put('7', 55);
        CHAR_TO_KEYCODE.put('8', 56);
        CHAR_TO_KEYCODE.put('9', 57);
        CHAR_TO_KEYCODE.put(' ', 32);

        // Director special characters (arrow keys use control characters)
        DIRECTOR_SPECIAL_CHAR_TO_KEYCODE.put((char) 28, 37);  // Left arrow
        DIRECTOR_SPECIAL_CHAR_TO_KEYCODE.put((char) 29, 39);  // Right arrow
        DIRECTOR_SPECIAL_CHAR_TO_KEYCODE.put((char) 30, 38);  // Up arrow
        DIRECTOR_SPECIAL_CHAR_TO_KEYCODE.put((char) 31, 40);  // Down arrow
    }

    /**
     * Private constructor - all methods are static.
     */
    private HandlerManager() {
    }

    // ========================================================================
    // Core Handler Methods
    // ========================================================================

    /**
     * Get parameter from current handler scope.
     */
    public static int param(DirPlayer player, List<Integer> args) throws ScriptError {
        int paramNumber = player.getDatum(args.get(0)).intValue();
        int scopeRef = player.currentScopeRef();
        ScriptScope scope = player.scopes.get(scopeRef);

        if (scope.args != null && paramNumber >= 1 && paramNumber <= scope.args.size()) {
            return scope.args.get(paramNumber - 1);
        }
        return 0; // Void
    }

    /**
     * Get count of items in a list, proplist, or void.
     */
    public static int count(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));

        if (obj.isList()) {
            return player.allocDatum(Datum.ofInt(obj.toList().size()));
        } else if (obj.isPropList()) {
            return player.allocDatum(Datum.ofInt(obj.toMap().size()));
        } else if (obj.isVoid()) {
            // Director treats count(VOID) as 0 - this allows "repeat with i in VOID" to not iterate
            return player.allocDatum(Datum.ofInt(0));
        } else {
            throw new ScriptError("Cannot get count of non-list (type: " + obj.typeStr() + ")");
        }
    }

    /**
     * Get item at position (1-based index).
     */
    public static int getAt(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        int position = player.getDatum(args.get(1)).intValue();
        int index = position - 1;

        logger.debug("getAt: list={}, index={}", player.formatDatum(obj), position);

        if (obj.isPoint()) {
            int[] arr = obj.toPoint();
            if (index < 0 || index >= 2) {
                throw new ScriptError("point index " + position + " out of bounds");
            }
            return arr[index];
        }

        if (obj.isRect()) {
            int[] arr = obj.toRect();
            if (index < 0 || index >= 4) {
                throw new ScriptError("rect index " + position + " out of bounds");
            }
            return arr[index];
        }

        if (obj.isList()) {
            List<Integer> list = obj.toList();
            if (index < 0 || index >= list.size()) {
                throw new ScriptError("Index " + position + " out of bounds for list of length " + list.size());
            }
            int result = list.get(index);
            logger.debug("getAt returned: {}", player.formatDatum(player.getDatum(result)));
            return result;
        }

        if (obj.isPropList()) {
            List<PropListPair> propList = obj.toMap();
            if (index < 0 || index >= propList.size()) {
                throw new ScriptError("Index " + position + " out of bounds for proplist of length " + propList.size());
            }
            int result = propList.get(index).value;
            logger.debug("getAt returned (from PropList): {}", player.formatDatum(player.getDatum(result)));
            return result;
        }

        throw new ScriptError("Cannot getAt of non-list (type: " + obj.typeStr() + ")");
    }

    /**
     * Set item at position (1-based index).
     */
    public static int setAt(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        int position = player.getDatum(args.get(1)).intValue();
        int newValue = args.get(2);
        int index = position - 1;

        Datum listDatum = player.getDatumMut(listRef);
        logger.debug("setAt: list={}, index={}, new_value={}",
                player.formatDatum(listDatum), position, player.formatDatum(player.getDatum(newValue)));

        // Validate the new_value type BEFORE modifying
        Datum newValueDatum = player.getDatum(newValue);

        if (listDatum.isPoint()) {
            if (index < 0 || index >= 2) {
                throw new ScriptError("point index " + position + " out of bounds");
            }
            // Validate that it's an Int
            if (!newValueDatum.isInt()) {
                throw new ScriptError("Point component must be Int, got " + newValueDatum.typeStr());
            }
            listDatum.setPointAt(index, newValue);
            return 0; // Void
        }

        if (listDatum.isRect()) {
            if (index < 0 || index >= 4) {
                throw new ScriptError("rect index " + position + " out of bounds");
            }
            if (!newValueDatum.isInt()) {
                throw new ScriptError("Rect component must be Int, got " + newValueDatum.typeStr());
            }
            listDatum.setRectAt(index, newValue);
            return 0; // Void
        }

        if (listDatum.isList()) {
            List<Integer> list = listDatum.toListMut();
            if (index < 0) {
                throw new ScriptError("Index " + position + " out of bounds");
            }
            if (index < list.size()) {
                list.set(index, newValue);
                logger.debug("setAt complete: list is now {}", player.formatDatum(listDatum));
            } else {
                throw new ScriptError("Index " + position + " out of bounds");
            }
            return 0; // Void
        }

        if (listDatum.isPropList()) {
            List<PropListPair> propList = listDatum.toMap();
            if (index < 0 || index >= propList.size()) {
                throw new ScriptError("Index " + position + " out of bounds");
            }
            propList.get(index).value = newValue;
            return 0; // Void
        }

        throw new ScriptError("Cannot setAt of type " + listDatum.typeStr() + " (must be list, proplist, point, or rect)");
    }

    /**
     * Print/debug output.
     */
    public static int put(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            dispatchDebugMessage("--");
            return 0;
        }

        // Format the first argument to determine output
        Datum firstArg = player.getDatum(args.get(0));
        String output;

        if (args.size() == 1) {
            // Single argument
            output = formatForPut(firstArg, player);
        } else {
            // Multiple arguments - join with spaces
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                Datum datum = player.getDatum(args.get(i));
                // For multi-arg put, use string representation
                if (datum.isString()) {
                    try {
                        sb.append(datum.stringValue());
                    } catch (ScriptError e) {
                        sb.append(player.formatDatum(datum));
                    }
                } else {
                    sb.append(player.formatDatum(datum));
                }
            }
            output = sb.toString();
        }

        dispatchDebugMessage("-- " + output);
        return 0; // Void
    }

    /**
     * Format a datum for the put command.
     */
    private static String formatForPut(Datum datum, DirPlayer player) {
        try {
            switch (datum.getType()) {
                case String:
                    // Strings are output with quotes
                    return "\"" + datum.stringValue() + "\"";
                case Int:
                    // Numbers are output without quotes
                    return String.valueOf(datum.intValue());
                case Symbol:
                    // Symbols are output with # prefix
                    return "#" + datum.symbolValue();
                case Void:
                case Null:
                    // Void outputs as <Void>
                    return "<Void>";
                case List:
                    // Lists
                    List<Integer> list = datum.toList();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(formatForPut(player.getDatum(list.get(i)), player));
                    }
                    sb.append("]");
                    return sb.toString();
                default:
                    // Everything else uses default formatting
                    return player.formatDatum(datum);
            }
        } catch (ScriptError e) {
            return player.formatDatum(datum);
        }
    }

    /**
     * Clear all global variables.
     */
    public static int clearGlobals(DirPlayer player, List<Integer> args) throws ScriptError {
        player.globals.clear();
        player.initializeGlobals();
        return 0; // Void
    }

    /**
     * Generate random number from 1 to max (inclusive).
     */
    public static int randomFunc(DirPlayer player, List<Integer> args) throws ScriptError {
        int max = player.getDatum(args.get(0)).intValue();
        if (max <= 0) {
            throw new ScriptError("random: argument must be greater than 0");
        }

        // Director's random(n) returns a value from 1 to n (inclusive)
        int randomInt = random.nextInt(max) + 1;
        return player.allocDatum(Datum.ofInt(randomInt));
    }

    /**
     * Bitwise AND operation.
     */
    public static int bitAnd(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        int b = player.getDatum(args.get(1)).intValue();
        return player.allocDatum(Datum.ofInt(a & b));
    }

    /**
     * Bitwise OR operation.
     */
    public static int bitOr(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        int b = player.getDatum(args.get(1)).intValue();
        return player.allocDatum(Datum.ofInt(a | b));
    }

    /**
     * Bitwise NOT operation.
     */
    public static int bitNot(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        return player.allocDatum(Datum.ofInt(~a));
    }

    // ========================================================================
    // Async Handler Detection and Routing
    // ========================================================================

    /**
     * Check if a handler name is an async handler.
     * In Java we treat these as potentially requiring special handling.
     */
    public static boolean hasAsyncHandler(String name) {
        switch (name.toLowerCase()) {
            case "call":
            case "new":
            case "newobject":
            case "callancestor":
            case "sendsprite":
            case "sendallsprites":
            case "value":
            case "do":
            case "updatestage":
            case "go":
                return true;
            default:
                return false;
        }
    }

    /**
     * Call an async handler by name.
     * Note: In Java, we handle async operations synchronously or via callbacks.
     */
    public static int callAsyncHandler(DirPlayer player, String name, List<Integer> args) throws ScriptError {
        switch (name.toLowerCase()) {
            case "call":
                return call(player, args);
            case "new":
                return newInstance(player, args);
            case "newobject":
                return TypeHandlers.newObject(player, args);
            case "callancestor":
                return callAncestor(player, args);
            case "sendsprite":
                return sendSprite(player, args);
            case "sendallsprites":
                return sendAllSprites(player, args);
            case "value":
                return TypeHandlers.value(player, args);
            case "do":
                return doCommand(player, args);
            case "updatestage":
                return updateStage(player, args);
            case "go":
                return go(player, args);
            default:
                throw new ScriptError("No built-in async handler: " + name);
        }
    }

    /**
     * Create a new instance of a script or object.
     */
    public static int newInstance(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("new requires at least one argument");
        }

        Datum firstArg = player.getDatum(args.get(0));

        // Check if it's a script reference or script name
        if (firstArg.isScriptRef() || firstArg.isSymbol() || firstArg.isString()) {
            // Get the script reference
            com.dirplayer.player.CastMemberRef scriptRef = null;

            if (firstArg.isScriptRef()) {
                scriptRef = firstArg.toScriptRef();
            } else if (firstArg.isSymbol() || firstArg.isString()) {
                String scriptName = firstArg.isSymbol() ? firstArg.symbolValue() : firstArg.stringValue();
                scriptRef = player.movie.castManager.findMemberRefByName(scriptName);
            }

            if (scriptRef != null) {
                com.dirplayer.player.script.Script script = player.movie.castManager.getScriptByRef(scriptRef);
                if (script != null) {
                    // Create a new script instance
                    List<Integer> constructorArgs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<>();
                    int instanceRef = player.createScriptInstance(scriptRef, constructorArgs);
                    return instanceRef;
                }
            }
        }

        // Fall back to newObject for other types
        return TypeHandlers.newObject(player, args);
    }

    /**
     * Call ancestor handler.
     */
    public static int callAncestor(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("callAncestor requires at least 2 arguments (handler name, object)");
        }

        String handlerName = player.getDatum(args.get(0)).symbolValue();
        int objectRef = args.get(1);
        Datum objectDatum = player.getDatum(objectRef);

        if (!objectDatum.isScriptInstanceRef()) {
            throw new ScriptError("callAncestor: second argument must be a script instance");
        }

        // Get the script instance
        int instanceId = objectDatum.getScriptInstanceRef();
        com.dirplayer.player.script.ScriptInstance instance = player.getScriptInstance(instanceId);
        if (instance == null) {
            throw new ScriptError("callAncestor: script instance not found");
        }

        // Get the ancestor property
        int ancestorRef = instance.getProperty("ancestor");
        if (ancestorRef == 0) {
            // No ancestor, return void
            return 0;
        }

        Datum ancestorDatum = player.getDatum(ancestorRef);
        if (ancestorDatum.isVoid() || ancestorDatum.isNull()) {
            return 0;
        }

        // Call the handler on the ancestor
        List<Integer> callArgs = args.size() > 2 ? args.subList(2, args.size()) : new ArrayList<>();
        return ScriptInstanceHandlers.call(player, ancestorRef, handlerName, callArgs);
    }

    // ========================================================================
    // Main Handler Dispatcher
    // ========================================================================

    /**
     * Call a built-in handler by name.
     */
    public static int callHandler(DirPlayer player, String name, List<Integer> args) throws ScriptError {
        String lowerName = name.toLowerCase();

        switch (lowerName) {
            // Cast handlers
            case "castlib":
                return castLib(player, args);

            // Net handlers
            case "preloadnetthing":
                return preloadNetThing(player, args);
            case "netdone":
                return netDone(player, args);
            case "getnettext":
                return getNetText(player, args);
            case "getstreamstatus":
                return getStreamStatus(player, args);
            case "neterror":
                return netError(player, args);
            case "nettextresult":
                return netTextResult(player, args);
            case "postnettext":
                return postNetText(player, args);

            // Movie handlers
            case "movetofront":
                return 0; // No-op
            case "puppettempo":
                return puppetTempo(player, args);
            case "script":
                return script(player, args);
            case "member":
                return member(player, args);
            case "puppetsprite":
                return puppetSprite(player, args);
            case "sprite":
                return sprite(player, args);
            case "externalparamcount":
                return externalParamCount(player, args);
            case "externalparamname":
                return externalParamName(player, args);
            case "externalparamvalue":
                return externalParamValue(player, args);
            case "stopevent":
                return stopEvent(player, args);
            case "getpref":
                return getPref(player, args);
            case "setpref":
                return setPref(player, args);
            case "gotonetpage":
                return goToNetPage(player, args);
            case "pass":
                return pass(player, args);
            case "rollover":
                return rollover(player, args);
            case "puppetsound":
                return puppetSound(player, args);
            case "halt":
                return halt(player, args);

            // Type handlers
            case "objectp":
                return TypeHandlers.objectp(player, args);
            case "voidp":
                return TypeHandlers.voidp(player, args);
            case "listp":
                return TypeHandlers.listp(player, args);
            case "symbolp":
                return TypeHandlers.symbolp(player, args);
            case "stringp":
                return TypeHandlers.stringp(player, args);
            case "integerp":
                return TypeHandlers.integerp(player, args);
            case "floatp":
                return TypeHandlers.floatp(player, args);
            case "void":
                return TypeHandlers.voidFunc(player, args);
            case "ilk":
                return TypeHandlers.ilk(player, args);
            case "integer":
                return TypeHandlers.integer(player, args);
            case "float":
                return TypeHandlers.floatFunc(player, args);
            case "symbol":
                return TypeHandlers.symbol(player, args);
            case "point":
                return TypeHandlers.point(player, args);
            case "cursor":
                return cursor(player, args);
            case "timeout":
                return timeout(player, args);
            case "rect":
                return TypeHandlers.rect(player, args);
            case "rgb":
                return TypeHandlers.rgb(player, args);
            case "list":
                return TypeHandlers.list(player, args);
            case "image":
                return image(player, args);
            case "paletteindex":
                return TypeHandlers.paletteIndex(player, args);
            case "abs":
                return TypeHandlers.abs(player, args);
            case "xtra":
                return xtra(player, args);
            case "union":
                return union(player, args);
            case "bitxor":
                return TypeHandlers.bitXor(player, args);
            case "power":
                return TypeHandlers.power(player, args);
            case "add":
                return add(player, args);
            case "nothing":
                return TypeHandlers.nothing(player, args);
            case "getaprop":
                return getAProp(player, args);
            case "min":
                return TypeHandlers.min(player, args);
            case "max":
                return TypeHandlers.max(player, args);
            case "sort":
                return sort(player, args);
            case "intersect":
                return intersect(player, args);
            case "getpropat":
                return getPropAt(player, args);
            case "pi":
                return TypeHandlers.pi(player, args);
            case "sin":
                return TypeHandlers.sin(player, args);
            case "cos":
                return TypeHandlers.cos(player, args);
            case "sqrt":
                return TypeHandlers.sqrt(player, args);
            case "atan":
                return TypeHandlers.atan(player, args);
            case "sound":
                return TypeHandlers.sound(player, args);
            case "vector":
                return TypeHandlers.vector(player, args);
            case "color":
                return TypeHandlers.color(player, args);
            case "soundbusy":
                return TypeHandlers.soundBusy(player, args);

            // String handlers
            case "offset":
                return StringHandlers.offset(player, args);
            case "length":
                return StringHandlers.length(player, args);
            case "space":
                return StringHandlers.space(player, args);
            case "string":
                return StringHandlers.string(player, args);
            case "chartonum":
                return StringHandlers.charToNum(player, args);
            case "numtochar":
                return StringHandlers.numToChar(player, args);
            case "chars":
                return StringHandlers.chars(player, args);

            // Core handlers
            case "param":
                return param(player, args);
            case "count":
                return count(player, args);
            case "getat":
                return getAt(player, args);
            case "setat":
                return setAt(player, args);
            case "put":
                return put(player, args);
            case "random":
                return randomFunc(player, args);
            case "bitand":
                return bitAnd(player, args);
            case "bitor":
                return bitOr(player, args);
            case "bitnot":
                return bitNot(player, args);
            case "clearglobals":
                return clearGlobals(player, args);

            // Collection handlers with special routing
            case "inside":
                return inside(player, args);
            case "addprop":
                return addProp(player, args);
            case "deleteprop":
                return deleteProp(player, args);
            case "append":
                return append(player, args);
            case "deleteat":
                return deleteAt(player, args);
            case "getone":
                return getOne(player, args);
            case "setaprop":
                return setAProp(player, args);
            case "addat":
                return addAt(player, args);
            case "getnodes":
                return getNodes(player, args);
            case "duplicate":
                return duplicate(player, args);
            case "getprop":
                return getProp(player, args);

            // Misc handlers
            case "keypressed":
                return keyPressed(player, args);
            case "showglobals":
                return showGlobals(player);
            case "tellstreamstatus":
                return tellStreamStatus(player, args);
            case "label":
                return label(player, args);
            case "alert":
                return alert(player, args);
            case "starttimer":
                return startTimer(player, args);
            case "externalevent":
                return externalEvent(player, args);
            case "dontpassevent":
                return dontPassEvent(player, args);
            case "frameready":
                return frameReady(player, args);
            case "marker":
                return marker(player, args);

            default:
                // Format args for error message
                StringBuilder formattedArgs = new StringBuilder();
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) formattedArgs.append(", ");
                    formattedArgs.append(player.formatDatum(player.getDatum(args.get(i))));
                }
                String msg = "No built-in handler: " + name + "(" + formattedArgs + ")";
                logger.warn(msg);
                throw new ScriptError(msg);
        }
    }

    // ========================================================================
    // Call Handler - for calling handlers on objects
    // ========================================================================

    /**
     * Call a handler on a datum (list of receivers).
     */
    public static int call(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("call requires at least 2 arguments");
        }

        int receiverRef = args.get(1);
        Datum handlerNameDatum = player.getDatum(args.get(0));
        Datum receiverDatum = player.getDatum(receiverRef);

        if (!handlerNameDatum.isSymbol()) {
            throw new ScriptError("Handler name must be a symbol");
        }
        String handlerName = handlerNameDatum.symbolValue();

        List<Integer> callArgs = args.size() > 2 ? args.subList(2, args.size()) : new ArrayList<>();

        // Handle list/proplist of receivers
        List<ScriptInstanceRef> instanceRefs = new ArrayList<>();
        int listCount = 0;

        if (receiverDatum.isPropList()) {
            List<PropListPair> propList = receiverDatum.toMap();
            listCount = propList.size();
            for (PropListPair pair : propList) {
                instanceRefs.addAll(getDatumScriptInstanceIds(player, pair.value));
            }
        } else if (receiverDatum.isList()) {
            List<Integer> list = receiverDatum.toList();
            listCount = list.size();
            for (int valueRef : list) {
                instanceRefs.addAll(getDatumScriptInstanceIds(player, valueRef));
            }
        } else {
            // Single receiver - delegate to datum handler
            return playerCallDatumHandler(player, receiverRef, handlerName, callArgs);
        }

        // Call handler on each instance
        int result = 0; // Void
        for (ScriptInstanceRef instanceRef : instanceRefs) {
            int handlerResult = ScriptInstanceHandlers.call(player, player.allocDatum(Datum.ofScriptInstanceRef(instanceRef.id())), handlerName, callArgs);
            result = handlerResult;
        }

        return result;
    }

    /**
     * Get script instance IDs from a datum.
     */
    private static List<ScriptInstanceRef> getDatumScriptInstanceIds(DirPlayer player, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);
        List<ScriptInstanceRef> instanceRefs = new ArrayList<>();

        if (value.isScriptInstanceRef()) {
            instanceRefs.add(new ScriptInstanceRef(value.getScriptInstanceRef()));
        } else if (value.isSpriteRef()) {
            // Get script instances from sprite
            int spriteNum = value.toSpriteRef();
            com.dirplayer.player.Sprite sprite = player.movie.score.getSprite(spriteNum);
            if (sprite != null && sprite.scriptInstanceList != null) {
                instanceRefs.addAll(sprite.scriptInstanceList);
            }
        } else if (value.isInt()) {
            // Integer is allowed but ignored (e.g., for empty slots)
        } else {
            throw new ScriptError("Cannot get script instance ids from datum of type: " + value.typeStr());
        }

        return instanceRefs;
    }

    /**
     * Call a handler on a datum (generic dispatch).
     */
    private static int playerCallDatumHandler(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        if (datum.isScriptInstanceRef()) {
            return ScriptInstanceHandlers.call(player, datumRef, handlerName, args);
        } else if (datum.isList()) {
            return ListHandlers.call(player, datumRef, handlerName, args);
        } else if (datum.isPropList()) {
            return PropListHandlers.call(player, datumRef, handlerName, args);
        } else if (datum.isPoint()) {
            return PointHandlers.call(player, datumRef, handlerName, args);
        } else {
            throw new ScriptError("Cannot call handler " + handlerName + " on datum of type " + datum.typeStr());
        }
    }

    // ========================================================================
    // Do Command
    // ========================================================================

    /**
     * Execute a string as Lingo code.
     */
    public static int doCommand(DirPlayer player, List<Integer> args) throws ScriptError {
        String code = player.getDatum(args.get(0)).stringValue();
        logger.debug("do: executing code: {}", code);

        code = code.trim();

        // Parse handler name and arguments from the code string
        String handlerName;
        List<Integer> argRefs;

        int parenPos = code.indexOf('(');
        if (parenPos >= 0) {
            handlerName = code.substring(0, parenPos).trim();
            String argsStr = code.substring(parenPos + 1);

            int closeParenPos = argsStr.lastIndexOf(')');
            if (closeParenPos >= 0) {
                argsStr = argsStr.substring(0, closeParenPos);
                if (argsStr.trim().isEmpty()) {
                    argRefs = new ArrayList<>();
                } else {
                    argRefs = parseDoArguments(player, argsStr);
                }
            } else {
                argRefs = new ArrayList<>();
            }
        } else {
            handlerName = code;
            argRefs = new ArrayList<>();
        }

        // Call the global handler
        try {
            int result = callHandler(player, handlerName, argRefs);
            logger.debug("do completed: {}", player.formatDatum(player.getDatum(result)));
            return result;
        } catch (ScriptError e) {
            logger.error("do failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Parse arguments from a do command string.
     */
    private static List<Integer> parseDoArguments(DirPlayer player, String argsStr) {
        List<Integer> argRefs = new ArrayList<>();
        String[] parts = argsStr.split(",");

        for (String part : parts) {
            String arg = part.trim();

            // Try to parse as integer
            try {
                int i = Integer.parseInt(arg);
                argRefs.add(player.allocDatum(Datum.ofInt(i)));
                continue;
            } catch (NumberFormatException ignored) {
            }

            // Try to parse as float
            try {
                double f = Double.parseDouble(arg);
                argRefs.add(player.allocDatum(Datum.ofFloat(f)));
                continue;
            } catch (NumberFormatException ignored) {
            }

            // Check for quoted string
            if (arg.startsWith("\"") && arg.endsWith("\"") && arg.length() >= 2) {
                argRefs.add(player.allocDatum(Datum.ofString(arg.substring(1, arg.length() - 1))));
                continue;
            }

            // Check for symbol
            if (arg.startsWith("#")) {
                argRefs.add(player.allocDatum(Datum.ofSymbol(arg.substring(1))));
                continue;
            }

            // Default to string
            argRefs.add(player.allocDatum(Datum.ofString(arg)));
        }

        return argRefs;
    }

    // ========================================================================
    // Collection/List Handlers with Type Routing
    // ========================================================================

    /**
     * Check if point is inside rect.
     */
    public static int inside(DirPlayer player, List<Integer> args) throws ScriptError {
        int pointRef = args.get(0);
        List<Integer> rectArgs = args.subList(1, args.size());
        return PointHandlers.inside(player, pointRef, rectArgs);
    }

    /**
     * Add property to proplist.
     */
    public static int addProp(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> propArgs = args.subList(1, args.size());
        return PropListHandlers.addProp(player, listRef, propArgs);
    }

    /**
     * Delete property from proplist.
     */
    public static int deleteProp(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> propArgs = args.subList(1, args.size());
        return PropListHandlers.deleteProp(player, listRef, propArgs);
    }

    /**
     * Append item to list.
     */
    public static int append(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> appendArgs = args.subList(1, args.size());
        return ListHandlers.append(player, listRef, appendArgs);
    }

    /**
     * Delete item at position from list or proplist.
     */
    public static int deleteAt(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> deleteArgs = args.subList(1, args.size());
        Datum datum = player.getDatum(listRef);

        if (datum.isList()) {
            return ListHandlers.deleteAt(player, listRef, deleteArgs);
        } else if (datum.isPropList()) {
            return PropListHandlers.deleteAt(player, listRef, deleteArgs);
        } else {
            throw new ScriptError("Cannot delete at non list");
        }
    }

    /**
     * Get position of item in list or proplist.
     */
    public static int getOne(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> getArgs = args.subList(1, args.size());
        Datum datum = player.getDatum(listRef);

        if (datum.isList()) {
            return ListHandlers.getOne(player, listRef, getArgs);
        } else if (datum.isPropList()) {
            return PropListHandlers.getOne(player, listRef, getArgs);
        } else {
            throw new ScriptError("Cannot get one at non list");
        }
    }

    /**
     * Set optional property (setaProp).
     */
    public static int setAProp(DirPlayer player, List<Integer> args) throws ScriptError {
        int datumRef = args.get(0);
        Datum datum = player.getDatum(datumRef);
        List<Integer> propArgs = args.subList(1, args.size());

        DatumType datumType = datum.getType();
        if (datumType == DatumType.PropList) {
            return PropListHandlers.setOptProp(player, datumRef, propArgs);
        } else if (datumType == DatumType.ScriptInstanceRef) {
            return ScriptInstanceHandlers.setAProp(player, datumRef, propArgs);
        } else {
            throw new ScriptError("Cannot setaProp on non-prop list or child object");
        }
    }

    /**
     * Add item at position in list.
     */
    public static int addAt(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> addArgs = args.subList(1, args.size());
        return ListHandlers.addAt(player, listRef, addArgs);
    }

    /**
     * Duplicate a list, proplist, point, rect, or other value.
     */
    public static int duplicate(DirPlayer player, List<Integer> args) throws ScriptError {
        int itemRef = args.get(0);
        Datum item = player.getDatum(itemRef);
        List<Integer> dupArgs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<>();

        if (item.isList()) {
            return ListHandlers.duplicate(player, itemRef, dupArgs);
        } else if (item.isPropList()) {
            return PropListHandlers.duplicate(player, itemRef, dupArgs);
        } else if (item.isPoint()) {
            // Duplicate point
            int[] arr = item.toPoint();
            Datum val0 = player.getDatum(arr[0]);
            Datum val1 = player.getDatum(arr[1]);

            int newRef0, newRef1;
            if (val0.isInt()) {
                newRef0 = player.allocDatum(Datum.ofInt(val0.intValue()));
            } else if (val0.isFloat()) {
                newRef0 = player.allocDatum(Datum.ofFloat(val0.floatValue()));
            } else {
                throw new ScriptError("Point component must be numeric, got " + val0.typeStr());
            }
            if (val1.isInt()) {
                newRef1 = player.allocDatum(Datum.ofInt(val1.intValue()));
            } else if (val1.isFloat()) {
                newRef1 = player.allocDatum(Datum.ofFloat(val1.floatValue()));
            } else {
                throw new ScriptError("Point component must be numeric, got " + val1.typeStr());
            }

            return player.allocDatum(Datum.ofPoint(newRef0, newRef1));
        } else if (item.isRect()) {
            // Duplicate rect
            int[] arr = item.toRect();
            int[] newArr = new int[4];
            for (int i = 0; i < 4; i++) {
                Datum val = player.getDatum(arr[i]);
                if (val.isInt()) {
                    newArr[i] = player.allocDatum(Datum.ofInt(val.intValue()));
                } else if (val.isFloat()) {
                    newArr[i] = player.allocDatum(Datum.ofFloat(val.floatValue()));
                } else {
                    throw new ScriptError("Rect component must be numeric, got " + val.typeStr());
                }
            }
            return player.allocDatum(Datum.ofRect(newArr[0], newArr[1], newArr[2], newArr[3]));
        } else if (item.isString()) {
            return player.allocDatum(Datum.ofString(item.stringValue()));
        } else if (item.isInt()) {
            return player.allocDatum(Datum.ofInt(item.intValue()));
        } else if (item.isFloat()) {
            return player.allocDatum(Datum.ofFloat(item.floatValue()));
        } else if (item.isSymbol()) {
            return player.allocDatum(Datum.ofSymbol(item.symbolValue()));
        } else {
            throw new ScriptError("duplicate() on non list not implemented");
        }
    }

    /**
     * Get property from proplist.
     */
    public static int getProp(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> propArgs = args.subList(1, args.size());
        return PropListHandlers.getProp(player, listRef, propArgs);
    }

    /**
     * Get optional property (getaProp).
     */
    public static int getAProp(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("getaProp requires at least 2 arguments");
        }
        int listRef = args.get(0);
        List<Integer> propArgs = args.subList(1, args.size());

        Datum datum = player.getDatum(listRef);
        if (datum.isPropList()) {
            return PropListHandlers.getAProp(player, listRef, propArgs);
        } else if (datum.isScriptInstanceRef()) {
            return ScriptInstanceHandlers.getAProp(player, listRef, propArgs);
        } else {
            throw new ScriptError("getaProp requires a proplist or script instance");
        }
    }

    /**
     * Sort a list or proplist.
     */
    public static int sort(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        Datum datum = player.getDatum(listRef);

        if (datum.isList()) {
            return ListHandlers.sort(player, listRef, args.subList(1, args.size()));
        } else if (datum.isPropList()) {
            return PropListHandlers.sort(player, listRef, args.subList(1, args.size()));
        } else {
            throw new ScriptError("sort requires a list or proplist");
        }
    }

    /**
     * Get rect intersection.
     */
    public static int intersect(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("intersect requires 2 arguments");
        }

        int[] rect1 = player.getDatum(args.get(0)).toRect();
        int[] rect2 = player.getDatum(args.get(1)).toRect();

        // Get actual values
        int left1 = player.getDatum(rect1[0]).intValue();
        int top1 = player.getDatum(rect1[1]).intValue();
        int right1 = player.getDatum(rect1[2]).intValue();
        int bottom1 = player.getDatum(rect1[3]).intValue();

        int left2 = player.getDatum(rect2[0]).intValue();
        int top2 = player.getDatum(rect2[1]).intValue();
        int right2 = player.getDatum(rect2[2]).intValue();
        int bottom2 = player.getDatum(rect2[3]).intValue();

        // Calculate intersection
        int left = Math.max(left1, left2);
        int top = Math.max(top1, top2);
        int right = Math.min(right1, right2);
        int bottom = Math.min(bottom1, bottom2);

        // If no intersection, return empty rect
        if (left >= right || top >= bottom) {
            left = top = right = bottom = 0;
        }

        int leftRef = player.allocDatum(Datum.ofInt(left));
        int topRef = player.allocDatum(Datum.ofInt(top));
        int rightRef = player.allocDatum(Datum.ofInt(right));
        int bottomRef = player.allocDatum(Datum.ofInt(bottom));

        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
    }

    /**
     * Get property key at index.
     */
    public static int getPropAt(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        List<Integer> propArgs = args.subList(1, args.size());
        return PropListHandlers.getPropAt(player, listRef, propArgs);
    }

    // ========================================================================
    // Keyboard Handler
    // ========================================================================

    /**
     * Check if a key is pressed.
     */
    public static int keyPressed(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum argDatum = player.getDatum(args.get(0));

        int keyCode;
        if (argDatum.isString()) {
            String keyStr = argDatum.stringValue();

            // STRING: First check if it's a single character
            if (keyStr.length() == 1) {
                char ch = keyStr.charAt(0);

                // First check for special Director characters (arrow keys, etc.)
                Integer specialCode = DIRECTOR_SPECIAL_CHAR_TO_KEYCODE.get(ch);
                if (specialCode != null) {
                    keyCode = specialCode;
                } else {
                    // Regular character - lowercase and look up
                    char chLower = Character.toLowerCase(ch);
                    keyCode = CHAR_TO_KEYCODE.getOrDefault(chLower, 0);
                }
            } else {
                // Try to parse as number string (like "123")
                try {
                    int code = Integer.parseInt(keyStr);
                    // Check if it's an ASCII letter code that needs mapping
                    if ((code >= 65 && code <= 90) || (code >= 97 && code <= 122)) {
                        char ch = Character.toLowerCase((char) code);
                        keyCode = CHAR_TO_KEYCODE.getOrDefault(ch, code);
                    } else {
                        keyCode = code;
                    }
                } catch (NumberFormatException e) {
                    throw new ScriptError("keyPressed: cannot parse string '" + keyStr + "'");
                }
            }
        } else if (argDatum.isInt()) {
            int code = argDatum.intValue();
            // Check if it's an ASCII code that needs mapping
            if ((code >= 65 && code <= 90) || (code >= 97 && code <= 122)) {
                char ch = Character.toLowerCase((char) code);
                keyCode = CHAR_TO_KEYCODE.getOrDefault(ch, code);
            } else {
                keyCode = code;
            }
        } else {
            throw new ScriptError("keyPressed expects a string or integer");
        }

        // Check if any currently pressed key matches this code
        boolean isPressed = player.keyboardManager.isKeyDown(keyCode);
        return player.allocDatum(Datum.ofBool(isPressed));
    }

    // ========================================================================
    // Miscellaneous Handlers
    // ========================================================================

    /**
     * Get XML nodes by name.
     */
    public static int getNodes(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("getNodes requires 2 arguments: xml_node, node_name");
        }

        Datum xmlNode = player.getDatum(args.get(0));
        String nodeName = player.getDatum(args.get(1)).stringValue();

        logger.debug("getNodes called for node type: {}", nodeName);

        // Get the XML node ID
        if (xmlNode.getType() != DatumType.XmlRef) {
            throw new ScriptError("First argument must be an XML node reference");
        }

        // Search for matching nodes using XmlParser helper
        int xmlId = xmlNode.getXmlRef();

        // Get the XML nodes from player's XML storage
        java.util.Map<Integer, com.dirplayer.player.xml.XmlNode> nodes = player.getXmlNodes();
        if (nodes == null || nodes.isEmpty()) {
            return player.allocDatum(Datum.ofList(DatumType.List, new ArrayList<>(), false));
        }

        // Find child nodes matching the name
        List<Integer> matchingIds = com.dirplayer.player.xml.XmlParser.findNodesByName(nodes, xmlId, nodeName);

        // Convert to datum refs
        List<Integer> resultRefs = new ArrayList<>();
        for (int nodeId : matchingIds) {
            resultRefs.add(player.allocDatum(Datum.ofXmlRef(nodeId)));
        }

        return player.allocDatum(Datum.ofList(DatumType.List, resultRefs, false));
    }

    /**
     * Enable/disable stream status handler.
     */
    public static int tellStreamStatus(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("tellStreamStatus requires 1 argument");
        }

        boolean enabled = false;
        try {
            enabled = player.getDatum(args.get(0)).boolValue();
        } catch (ScriptError ignored) {
        }

        player.enableStreamStatusHandler = enabled;
        return player.allocDatum(Datum.ofInt(enabled ? 1 : 0));
    }

    /**
     * Find frame number for a label.
     */
    public static int label(DirPlayer player, List<Integer> args) throws ScriptError {
        String labelName = player.getDatum(args.get(0)).stringValue();
        logger.debug("Searching for label: {}", labelName);

        String labelNameLower = labelName.toLowerCase();

        // Search frame labels
        int frameNum = 0;
        for (FrameLabel fl : player.movie.score.frameLabels) {
            if (fl.label.toLowerCase().equals(labelNameLower)) {
                frameNum = fl.frameNum;
                logger.debug("Found label '{}' at frame {}", fl.label, fl.frameNum);
                break;
            }
        }

        if (frameNum == 0) {
            logger.warn("Label not found: {}", labelName);
        }

        return player.allocDatum(Datum.ofInt(frameNum));
    }

    /**
     * Show alert dialog.
     */
    public static int alert(DirPlayer player, List<Integer> args) throws ScriptError {
        String message = player.getDatum(args.get(0)).stringValue();
        dispatchDebugMessage("Alert: " + message);
        return 0; // Void
    }

    /**
     * Show global variables in debug output.
     */
    public static int showGlobals(DirPlayer player) throws ScriptError {
        dispatchDebugMessage("--- Global Variables ---");
        for (Map.Entry<String, Integer> entry : player.globals.entrySet()) {
            Datum value = player.getDatum(entry.getValue());
            dispatchDebugMessage(entry.getKey() + " = " + player.formatDatum(value));
        }
        return 0; // Void
    }

    /**
     * Check if datum is void.
     */
    public static int voidP(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofInt(1));
        }

        Datum datum = player.getDatum(args.get(0));
        boolean isVoid = datum.isVoid() || datum.isNull();
        return player.allocDatum(Datum.ofInt(isVoid ? 1 : 0));
    }

    /**
     * Check if datum is an object.
     */
    public static int objectP(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        Datum datum = player.getDatum(args.get(0));

        // Director considers these as objects (not primitives)
        boolean isObject = false;
        switch (datum.getType()) {
            case ScriptInstanceRef:
            case SpriteRef:
            case CastMemberRef:
            case List:
            case PropList:
            case BitmapRef:
            case ScriptRef:
            case XmlRef:
            case Xtra:
            case XtraInstance:
            case Matte:
            case PlayerRef:
            case MovieRef:
            case StageRef:
            case CastLibRef:
            case DateRef:
            case MathRef:
            case SoundRef:
            case SoundChannel:
            case CursorRef:
            case TimeoutRef:
            case TimeoutInstance:
                isObject = true;
                break;
            default:
                isObject = false;
        }

        return player.allocDatum(Datum.ofInt(isObject ? 1 : 0));
    }

    /**
     * Reset start timer.
     */
    public static int startTimer(DirPlayer player, List<Integer> args) throws ScriptError {
        player.startTime = LocalDateTime.now();
        return 0; // Void
    }

    /**
     * Dispatch external event.
     */
    public static int externalEvent(DirPlayer player, List<Integer> args) throws ScriptError {
        String eventString = player.getDatum(args.get(0)).stringValue();
        logger.debug("externalEvent: {}", eventString);
        dispatchExternalEvent(eventString);
        return 0; // Void
    }

    /**
     * Don't pass event to parent scripts.
     */
    public static int dontPassEvent(DirPlayer player, List<Integer> args) throws ScriptError {
        int scopeRef = player.currentScopeRef();
        if (scopeRef >= 0 && scopeRef < player.scopes.size()) {
            ScriptScope scope = player.scopes.get(scopeRef);
            scope.passed = false;  // Set passed to false to stop propagation
        }
        return 0; // Void
    }

    /**
     * Check if frame(s) are ready (all cast members loaded).
     */
    public static int frameReady(DirPlayer player, List<Integer> args) throws ScriptError {
        // Get start and end frame numbers
        int startFrame, endFrame;
        if (args.isEmpty()) {
            // No arguments - check current frame only
            startFrame = player.movie.currentFrame;
            endFrame = startFrame;
        } else {
            startFrame = player.getDatum(args.get(0)).intValue();
            endFrame = args.size() > 1 ? player.getDatum(args.get(1)).intValue() : startFrame;
        }

        logger.debug("frameReady checking frames {} to {}", startFrame, endFrame);

        // Check if frame range is valid
        if (startFrame < 1 || endFrame < startFrame) {
            return player.allocDatum(Datum.ofInt(0));
        }

        // Collect all unique cast member references used in the frame range
        Set<String> castMembersToCheck = new HashSet<>();

        for (int frameNum = startFrame; frameNum <= endFrame; frameNum++) {
            // Check channel initialization data for this frame
            for (com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry : player.movie.score.channelInitializationData) {
                if (entry.frameIndex + 1 == frameNum) {
                    // Skip empty sprites
                    if (entry.data.castLib > 0 && entry.data.castMember > 0) {
                        castMembersToCheck.add(entry.data.castLib + ":" + entry.data.castMember);
                    }
                }
            }
        }

        logger.debug("Found {} unique cast members to check", castMembersToCheck.size());

        // Check if all cast members are loaded
        for (String key : castMembersToCheck) {
            String[] parts = key.split(":");
            int castLib = Integer.parseInt(parts[0]);
            int castMemberNum = Integer.parseInt(parts[1]);

            CastLib cast = player.movie.castManager.getCast(castLib);
            if (cast != null) {
                CastMember member = cast.members.get(castMemberNum);
                if (member == null) {
                    // Cast member not loaded yet
                    logger.debug("Cast member {}:{} not loaded", castLib, castMemberNum);
                    return player.allocDatum(Datum.ofInt(0));
                }
                // For bitmap members, check if bitmap is loaded
                if (member.memberType == MemberType.Bitmap) {
                    if (member.bitmap == null || !member.bitmap.isLoaded()) {
                        logger.debug("Bitmap not loaded for member {}:{}", castLib, castMemberNum);
                        return player.allocDatum(Datum.ofInt(0));
                    }
                }
            }
        }

        logger.debug("All frames ready!");
        return player.allocDatum(Datum.ofInt(1));
    }

    /**
     * Get marker name at frame or frame number of marker.
     */
    public static int marker(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("marker requires 1 argument");
        }

        Datum arg = player.getDatum(args.get(0));

        if (arg.isInt()) {
            // If argument is an integer, return the marker name at that frame
            int frameNum = arg.intValue();
            for (FrameLabel fl : player.movie.score.frameLabels) {
                if (fl.frameNum == frameNum) {
                    return player.allocDatum(Datum.ofString(fl.label));
                }
            }
            return player.allocDatum(Datum.ofString(""));
        } else if (arg.isString() || arg.isSymbol()) {
            // If argument is a string, return the frame number of that marker
            String markerName = arg.isString() ? arg.stringValue() : arg.symbolValue();
            String markerNameLower = markerName.toLowerCase();

            for (FrameLabel fl : player.movie.score.frameLabels) {
                if (fl.label.toLowerCase().equals(markerNameLower)) {
                    return player.allocDatum(Datum.ofInt(fl.frameNum));
                }
            }
            return player.allocDatum(Datum.ofInt(0));
        } else {
            throw new ScriptError("marker expects string or integer, got " + arg.typeStr());
        }
    }

    // ========================================================================
    // Stub Handlers (to be fully implemented)
    // ========================================================================

    public static int castLib(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("castLib requires at least 1 argument");
        }

        Datum identifier = player.getDatum(args.get(0));

        if (identifier.isInt()) {
            int castLibNum = identifier.intValue();
            return player.allocDatum(Datum.ofCastLibRef(castLibNum));
        } else if (identifier.isString()) {
            String castLibName = identifier.stringValue();
            // Look up cast lib by name
            Integer castLibNum = player.movie.castManager.findCastLibByName(castLibName);
            if (castLibNum != null) {
                return player.allocDatum(Datum.ofCastLibRef(castLibNum));
            } else {
                // Return invalid cast lib reference (0)
                return player.allocDatum(Datum.ofCastLibRef(0));
            }
        } else {
            throw new ScriptError("castLib expects an integer or string, got " + identifier.typeStr());
        }
    }

    public static int preloadNetThing(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("preloadNetThing requires a URL argument");
        }
        String url = player.getDatum(args.get(0)).stringValue();
        int taskId = player.netManager.preloadNetThing(url);
        return player.allocDatum(Datum.ofInt(taskId));
    }

    public static int netDone(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum taskIdDatum = player.getDatum(args.get(0));
            if (!taskIdDatum.isVoid()) {
                taskId = taskIdDatum.intValue();
            }
        }
        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        boolean isDone = taskState != null && taskState.isDone();
        return player.allocDatum(Datum.ofInt(isDone ? 1 : 0));
    }

    public static int getNetText(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getNetText requires a URL argument");
        }
        String originalUrl = player.getDatum(args.get(0)).stringValue();
        // Decode URL-encoded characters
        String url;
        try {
            url = java.net.URLDecoder.decode(originalUrl, "UTF-8");
        } catch (Exception e) {
            throw new ScriptError("Cannot decode URL: " + e.getMessage());
        }
        int taskId = player.netManager.preloadNetThing(url);
        return player.allocDatum(Datum.ofInt(taskId));
    }

    public static int getStreamStatus(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getStreamStatus requires a task ID or URL argument");
        }

        Datum arg = player.getDatum(args.get(0));
        int taskId;

        // Support both task ID (int) and URL (string)
        if (arg.isInt()) {
            taskId = arg.intValue();
        } else if (arg.isString()) {
            String url = arg.stringValue();
            Integer foundTaskId = player.netManager.findTaskByUrl(url);
            if (foundTaskId == null) {
                throw new ScriptError("Network task not found for URL: " + url);
            }
            taskId = foundTaskId;
        } else {
            throw new ScriptError("getStreamStatus requires an integer task ID or URL string");
        }

        NetTask task = player.netManager.getTask(taskId);
        if (task == null) {
            throw new ScriptError("Network task " + taskId + " not found");
        }

        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task state " + taskId + " not found");
        }

        String state;
        String error;
        boolean isOk;

        if (taskState.isDone() && taskState.getResult() != null && taskState.getResult().isOk()) {
            state = "Complete";
            error = "OK";
            isOk = true;
        } else if (taskState.isDone() && taskState.getResult() != null && taskState.getResult().isError()) {
            state = "Complete";
            error = "Task failed";
            isOk = false;
        } else {
            state = "InProgress";
            error = "";
            isOk = false;
        }

        // Build the result property list
        List<Datum.PropListPair> propList = new ArrayList<>();
        propList.add(new Datum.PropListPair(
            player.allocDatum(Datum.ofString("URL")),
            player.allocDatum(Datum.ofString(task.url))));
        propList.add(new Datum.PropListPair(
            player.allocDatum(Datum.ofString("state")),
            player.allocDatum(Datum.ofString(state))));
        propList.add(new Datum.PropListPair(
            player.allocDatum(Datum.ofString("bytesSoFar")),
            player.allocDatum(Datum.ofInt(isOk ? 100 : 0))));
        propList.add(new Datum.PropListPair(
            player.allocDatum(Datum.ofString("bytesTotal")),
            player.allocDatum(Datum.ofInt(100))));
        propList.add(new Datum.PropListPair(
            player.allocDatum(Datum.ofString("error")),
            player.allocDatum(Datum.ofString(error))));

        return player.allocDatum(Datum.ofPropListPairs(propList, false));
    }

    public static int netError(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum datum = player.getDatum(args.get(0));
            if (!datum.isVoid()) {
                taskId = datum.intValue();
            }
        }
        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task not found");
        }
        boolean isOk = taskState.isDone() &&
                       taskState.getResult() != null &&
                       taskState.getResult().isOk();
        Datum error;
        if (isOk) {
            error = Datum.ofString("OK");
        } else if (taskState.getResult() != null && taskState.getResult().isError()) {
            error = Datum.ofInt(taskState.getResult().getErrorCode());
        } else {
            error = Datum.ofInt(0);
        }
        return player.allocDatum(error);
    }

    public static int netTextResult(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum datum = player.getDatum(args.get(0));
            if (!datum.isVoid()) {
                taskId = datum.intValue();
            }
        }
        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task not found");
        }
        boolean isOk = taskState.isDone() &&
                       taskState.getResult() != null &&
                       taskState.getResult().isOk();
        String text;
        if (isOk) {
            text = taskState.getResult().getDataAsString();
        } else {
            text = "";
        }
        return player.allocDatum(Datum.ofString(text));
    }

    public static int postNetText(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("postNetText requires at least 1 argument (url)");
        }
        String url = player.getDatum(args.get(0)).stringValue();

        // Get the post data (can be a property list or string)
        String postData;
        if (args.size() > 1) {
            Datum dataDatum = player.getDatum(args.get(1));
            if (dataDatum.isPropList()) {
                // Convert property list to form data
                List<String> formParts = new ArrayList<>();
                List<PropListPair> propList = dataDatum.toMap();
                for (PropListPair pair : propList) {
                    String key = player.getDatum(pair.key).stringValue();
                    String value = player.getDatum(pair.value).stringValue();
                    try {
                        String encodedKey = java.net.URLEncoder.encode(key, "UTF-8");
                        String encodedValue = java.net.URLEncoder.encode(value, "UTF-8");
                        formParts.add(encodedKey + "=" + encodedValue);
                    } catch (Exception e) {
                        throw new ScriptError("Failed to encode form data: " + e.getMessage());
                    }
                }
                postData = String.join("&", formParts);
            } else if (dataDatum.isString()) {
                postData = dataDatum.stringValue();
            } else {
                throw new ScriptError("postNetText second argument must be a property list or string, got " + dataDatum.typeStr());
            }
        } else {
            postData = "";
        }

        int taskId = player.netManager.postNetText(url, postData);
        return player.allocDatum(Datum.ofInt(taskId));
    }

    public static int puppetTempo(DirPlayer player, List<Integer> args) throws ScriptError {
        if (!args.isEmpty()) {
            int tempo = player.getDatum(args.get(0)).intValue();
            player.currentFrameTempo = tempo;
        }
        return 0; // Void
    }

    public static int script(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("script requires at least 1 argument");
        }
        Datum identifier = player.getDatum(args.get(0));

        com.dirplayer.player.CastMemberRef memberRef = null;

        if (identifier.isString()) {
            String scriptName = identifier.stringValue();
            memberRef = player.movie.castManager.findMemberRefByName(scriptName);
        } else if (identifier.isInt()) {
            int scriptNum = identifier.intValue();
            memberRef = player.movie.castManager.findMemberRefByNumber(scriptNum);
        } else if (identifier.isCastMemberRef()) {
            memberRef = identifier.toCastMemberRef();
        } else {
            throw new ScriptError("Invalid identifier for script: " + player.formatDatum(identifier));
        }

        if (memberRef != null) {
            com.dirplayer.player.script.Script script = player.movie.castManager.getScriptByRef(memberRef);
            if (script != null) {
                return player.allocDatum(Datum.ofScriptRef(memberRef));
            }
        }

        throw new ScriptError("Script not found: " + player.formatDatum(identifier));
    }

    public static int member(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("member requires at least 1 argument");
        }
        if (args.size() > 2) {
            throw new ScriptError("Too many arguments for member");
        }

        Datum memberNameOrNum = player.getDatum(args.get(0));

        // If already a cast member ref, return it
        if (memberNameOrNum.isCastMemberRef()) {
            return args.get(0);
        }

        Datum castNameOrNum = args.size() > 1 ? player.getDatum(args.get(1)) : null;

        com.dirplayer.player.CastMemberRef memberRef = player.movie.castManager.findMemberRefByIdentifiers(memberNameOrNum, castNameOrNum);

        if (memberRef != null) {
            return player.allocDatum(Datum.ofCastMemberRef(memberRef));
        } else {
            // Return invalid member reference
            return player.allocDatum(Datum.ofCastMemberRef(com.dirplayer.player.CastMemberRef.INVALID));
        }
    }

    public static int puppetSprite(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("puppetSprite requires 2 arguments");
        }
        int spriteNumber = player.getDatum(args.get(0)).intValue();
        boolean isPuppet = player.getDatum(args.get(1)).intValue() == 1;
        com.dirplayer.player.Sprite sprite = player.movie.score.getSprite(spriteNumber);
        if (sprite != null) {
            sprite.puppet = isPuppet;
        }
        return 0; // Void
    }

    public static int sprite(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("sprite requires at least 1 argument");
        }
        int spriteNum = player.getDatum(args.get(0)).intValue();
        return player.allocDatum(Datum.ofSpriteRef(spriteNum));
    }

    public static int externalParamCount(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofInt(player.externalParams.size()));
    }

    public static int externalParamName(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofVoid());
        }

        Datum datum = player.getDatum(args.get(0));

        // Case 1: argument is a string (lookup by name, case-insensitive)
        if (datum.isString()) {
            String key = datum.stringValue();
            for (String paramKey : player.externalParams.keySet()) {
                if (paramKey.toLowerCase().equals(key.toLowerCase())) {
                    return player.allocDatum(Datum.ofString(key));
                }
            }
            return player.allocDatum(Datum.ofVoid());
        }

        // Case 2: argument is an integer (index, 1-based)
        if (datum.isInt()) {
            int index = datum.intValue();
            if (index > 0 && index <= player.externalParams.size()) {
                int i = 1;
                for (String key : player.externalParams.keySet()) {
                    if (i == index) {
                        return player.allocDatum(Datum.ofString(key));
                    }
                    i++;
                }
            }
            return player.allocDatum(Datum.ofVoid());
        }

        logger.debug("externalParamName(): invalid argument type, returning Void");
        return player.allocDatum(Datum.ofVoid());
    }

    public static int externalParamValue(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofVoid());
        }

        Datum datum = player.getDatum(args.get(0));

        // Case 1: argument is a string (lookup by name, case-insensitive)
        if (datum.isString()) {
            String key = datum.stringValue();
            for (Map.Entry<String, String> entry : player.externalParams.entrySet()) {
                if (entry.getKey().toLowerCase().equals(key.toLowerCase())) {
                    return player.allocDatum(Datum.ofString(entry.getValue()));
                }
            }
            return player.allocDatum(Datum.ofVoid());
        }

        // Case 2: argument is an integer (index, 1-based)
        if (datum.isInt()) {
            int index = datum.intValue();
            if (index > 0 && index <= player.externalParams.size()) {
                int i = 1;
                for (String value : player.externalParams.values()) {
                    if (i == index) {
                        return player.allocDatum(Datum.ofString(value));
                    }
                    i++;
                }
            }
            return player.allocDatum(Datum.ofVoid());
        }

        logger.debug("externalParamValue(): invalid argument type, returning Void");
        return player.allocDatum(Datum.ofVoid());
    }

    public static int stopEvent(DirPlayer player, List<Integer> args) throws ScriptError {
        int scopeRef = player.currentScopeRef();
        if (scopeRef >= 0 && scopeRef < player.scopes.size()) {
            ScriptScope scope = player.scopes.get(scopeRef);
            scope.passed = false;  // Stop event propagation
        }
        return 0; // Void
    }

    public static int getPref(DirPlayer player, List<Integer> args) throws ScriptError {
        // Return empty string - Lingo code handles the fallback
        return player.allocDatum(Datum.ofString(""));
    }

    public static int setPref(DirPlayer player, List<Integer> args) throws ScriptError {
        // Preferences storage not supported in browser environment
        return 0; // Void
    }

    public static int goToNetPage(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("gotoNetPage requires a URL argument");
        }
        String url = player.getDatum(args.get(0)).stringValue();
        String target = args.size() > 1 ? player.getDatum(args.get(1)).stringValue() : "_self";

        // Log the navigation request - actual navigation handled by JS host
        logger.info("gotoNetPage: {} (target: {})", url, target);

        // Dispatch external event for host to handle
        dispatchExternalEvent("gotoNetPage:" + url);
        return 0; // Void
    }

    public static int pass(DirPlayer player, List<Integer> args) throws ScriptError {
        int scopeRef = player.currentScopeRef();
        if (scopeRef >= 0 && scopeRef < player.scopes.size()) {
            ScriptScope scope = player.scopes.get(scopeRef);
            scope.passed = true;
        }
        return 0; // Void
    }

    public static int rollover(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofInt(player.hoveredSprite));
    }

    public static int puppetSound(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("puppetSound requires at least 1 argument");
        }

        // If only one argument, use channel 1 by default
        int channelNum;
        int memberRef;
        if (args.size() == 1) {
            channelNum = 1;
            memberRef = args.get(0);
        } else {
            channelNum = player.getDatum(args.get(0)).intValue();
            memberRef = args.get(1);
        }

        player.puppetSound(channelNum, memberRef);
        return 0; // Void
    }

    public static int halt(DirPlayer player, List<Integer> args) throws ScriptError {
        player.isPlaying = false;
        return 0; // Void
    }

    public static int cursor(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            // Return current cursor
            return player.allocDatum(Datum.ofInt(player.currentCursor));
        }

        // Set cursor
        Datum cursorDatum = player.getDatum(args.get(0));
        if (cursorDatum.isInt()) {
            player.currentCursor = cursorDatum.intValue();
        } else if (cursorDatum.isList()) {
            // Cursor specified as list [resourceID, maskID] - just set to arrow for now
            player.currentCursor = 0;
        }

        return 0; // Void
    }

    public static int timeout(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("timeout requires at least 1 argument (name)");
        }

        String timeoutName = player.getDatum(args.get(0)).stringValue();

        // Look up existing timeout by name
        com.dirplayer.player.TimeoutManager.TimeoutInstance existingTimeout = player.timeoutManager.getTimeout(timeoutName);
        if (existingTimeout != null) {
            return player.allocDatum(Datum.ofTimeoutInstance(existingTimeout));
        }

        // If not found, create a new timeout (requires #new call in Lingo typically)
        throw new ScriptError("Timeout not found: " + timeoutName);
    }

    public static int image(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("image requires at least 2 arguments (width, height)");
        }

        int width = player.getDatum(args.get(0)).intValue();
        int height = player.getDatum(args.get(1)).intValue();

        // Optional bit depth (default 32)
        int bitDepth = 32;
        if (args.size() >= 3) {
            bitDepth = player.getDatum(args.get(2)).intValue();
        }

        // Create a new bitmap
        com.dirplayer.player.bitmap.Bitmap bitmap = new com.dirplayer.player.bitmap.Bitmap(width, height);
        bitmap.depth = bitDepth;

        // Allocate and return as bitmap ref
        int bitmapId = player.allocBitmap(bitmap);
        return player.allocDatum(Datum.ofBitmapRef(bitmapId));
    }

    public static int xtra(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("xtra requires at least 1 argument (xtra name)");
        }

        String xtraName = player.getDatum(args.get(0)).stringValue();

        // Look up xtra by name
        com.dirplayer.player.xtra.XtraInstance xtraInstance = player.getXtra(xtraName);
        if (xtraInstance != null) {
            return player.allocDatum(Datum.ofXtraInstance(xtraInstance));
        }

        // Create a new xtra instance
        xtraInstance = player.createXtra(xtraName);
        if (xtraInstance != null) {
            return player.allocDatum(Datum.ofXtraInstance(xtraInstance));
        }

        throw new ScriptError("Xtra not found: " + xtraName);
    }

    public static int union(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("union requires 2 arguments");
        }

        int[] rect1 = player.getDatum(args.get(0)).toRect();
        int[] rect2 = player.getDatum(args.get(1)).toRect();

        // Get actual values
        int left1 = player.getDatum(rect1[0]).intValue();
        int top1 = player.getDatum(rect1[1]).intValue();
        int right1 = player.getDatum(rect1[2]).intValue();
        int bottom1 = player.getDatum(rect1[3]).intValue();

        int left2 = player.getDatum(rect2[0]).intValue();
        int top2 = player.getDatum(rect2[1]).intValue();
        int right2 = player.getDatum(rect2[2]).intValue();
        int bottom2 = player.getDatum(rect2[3]).intValue();

        // Calculate union
        int left = Math.min(left1, left2);
        int top = Math.min(top1, top2);
        int right = Math.max(right1, right2);
        int bottom = Math.max(bottom1, bottom2);

        int leftRef = player.allocDatum(Datum.ofInt(left));
        int topRef = player.allocDatum(Datum.ofInt(top));
        int rightRef = player.allocDatum(Datum.ofInt(right));
        int bottomRef = player.allocDatum(Datum.ofInt(bottom));

        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
    }

    public static int add(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("add requires at least 1 argument");
        }
        int listRef = args.get(0);
        List<Integer> addArgs = args.subList(1, args.size());
        return ListHandlers.add(player, listRef, addArgs);
    }

    public static int sendSprite(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("sendSprite requires at least 2 arguments (sprite, message)");
        }

        int spriteNum = player.getDatum(args.get(0)).intValue();
        String message = player.getDatum(args.get(1)).symbolValue();
        List<Integer> remainingArgs = args.size() > 2 ? args.subList(2, args.size()) : new ArrayList<>();

        com.dirplayer.player.Sprite sprite = player.movie.score.getSprite(spriteNum);
        if (sprite == null) {
            throw new ScriptError("sendSprite: sprite " + spriteNum + " not found");
        }

        List<ScriptInstanceRef> receivers = new ArrayList<>(sprite.scriptInstanceList);

        boolean handledBySprite = false;
        for (ScriptInstanceRef receiver : receivers) {
            List<ScriptInstanceRef> singleReceiver = new ArrayList<>();
            singleReceiver.add(receiver);

            try {
                boolean handled = player.eventDispatcher.invokeEventToInstances(message, remainingArgs, singleReceiver);
                if (handled) {
                    handledBySprite = true;
                }
            } catch (ScriptError e) {
                logger.warn("sendSprite continuing after error in handler");
            }
        }

        if (!handledBySprite) {
            player.eventDispatcher.invokeStaticEvent(message, remainingArgs);
        }

        return player.allocDatum(Datum.ofInt(handledBySprite ? 1 : 0));
    }

    public static int sendAllSprites(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("sendAllSprites requires at least 1 argument (message)");
        }

        // Check for re-entrant sendAllSprites call
        if (player.isInSendAllSprites) {
            logger.warn("Blocking re-entrant sendAllSprites call to prevent infinite recursion");
            return 0; // Void
        }
        player.isInSendAllSprites = true;

        try {
            String message = player.getDatum(args.get(0)).symbolValue();
            List<Integer> remainingArgs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<>();

            // Collect receivers from stage score
            List<ScriptInstanceRef> receivers = player.movie.score.getActiveScriptInstanceList();

            boolean handledBySprite = false;
            for (ScriptInstanceRef receiver : receivers) {
                List<ScriptInstanceRef> singleReceiver = new ArrayList<>();
                singleReceiver.add(receiver);

                try {
                    boolean handled = player.eventDispatcher.invokeEventToInstances(message, remainingArgs, singleReceiver);
                    if (handled) {
                        handledBySprite = true;
                    }
                } catch (ScriptError e) {
                    logger.warn("sendAllSprites continuing after error in handler");
                }
            }

            if (!handledBySprite) {
                player.eventDispatcher.invokeStaticEvent(message, remainingArgs);
            }

            return player.allocDatum(Datum.ofInt(handledBySprite ? 1 : 0));
        } finally {
            player.isInSendAllSprites = false;
        }
    }

    public static int updateStage(DirPlayer player, List<Integer> args) throws ScriptError {
        // Trigger synchronous render if we're in a safe state
        if (player.isYieldSafe()) {
            logger.debug("updateStage: performing synchronous render");
            player.renderStage();
        } else {
            logger.debug("updateStage: skipped render, not in yield-safe state");
        }
        return 0; // Void
    }

    public static int go(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("go requires at least 1 argument");
        }

        Datum arg = player.getDatum(args.get(0));

        if (arg.isInt()) {
            player.nextFrame = arg.intValue();
        } else if (arg.isString() || arg.isSymbol()) {
            String labelName = arg.isString() ? arg.stringValue() : arg.symbolValue();
            String labelNameLower = labelName.toLowerCase();

            for (FrameLabel fl : player.movie.score.frameLabels) {
                if (fl.label.toLowerCase().equals(labelNameLower)) {
                    player.nextFrame = fl.frameNum;
                    break;
                }
            }
        }

        player.hasFrameChangedInGo = true;
        return 0; // Void
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Dispatch a debug message (to console and any registered listeners).
     */
    private static void dispatchDebugMessage(String message) {
        // Log to SLF4J
        logger.info(message);
        // Also print to console for immediate visibility
        System.out.println(message);
    }

    /**
     * Dispatch an external event to JavaScript/host environment.
     */
    private static void dispatchExternalEvent(String eventString) {
        // Log the event
        logger.info("External Event: {}", eventString);
        // Emit via JsApi if available (TeaVM environment)
        try {
            com.dirplayer.JsApi.dispatchExternalEvent(eventString);
        } catch (Exception e) {
            // JsApi may not be available in non-browser environments
            logger.debug("JsApi not available for external event dispatch");
        }
    }
}
