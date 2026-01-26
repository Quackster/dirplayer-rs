package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Built-in handler manager - dispatches calls to handler functions.
 * Port of Rust BuiltInHandlerManager struct.
 */
public class BuiltInHandlerManager {

    private static Random random = new Random();

    /**
     * Check if a handler is async (in Java, we treat all as sync but mark which ones might need special handling).
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
     * Call a built-in handler by name.
     */
    public static int callHandler(DirPlayer player, String name, List<Integer> args) throws ScriptError {
        String lowerName = name.toLowerCase();

        switch (lowerName) {
            // Type checks
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

            // Type conversions
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
            case "rect":
                return TypeHandlers.rect(player, args);
            case "rgb":
                return TypeHandlers.rgb(player, args);
            case "paletteindex":
                return TypeHandlers.paletteIndex(player, args);
            case "list":
                return TypeHandlers.list(player, args);
            case "vector":
                return TypeHandlers.vector(player, args);
            case "color":
                return TypeHandlers.color(player, args);

            // Math functions
            case "abs":
                return TypeHandlers.abs(player, args);
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
            case "power":
                return TypeHandlers.power(player, args);
            case "bitxor":
                return TypeHandlers.bitXor(player, args);
            case "min":
                return TypeHandlers.min(player, args);
            case "max":
                return TypeHandlers.max(player, args);
            case "random":
                return random(player, args);
            case "bitand":
                return bitAnd(player, args);
            case "bitor":
                return bitOr(player, args);
            case "bitnot":
                return bitNot(player, args);

            // String functions
            case "space":
                return StringHandlers.space(player, args);
            case "offset":
                return StringHandlers.offset(player, args);
            case "length":
                return StringHandlers.length(player, args);
            case "string":
                return StringHandlers.string(player, args);
            case "chars":
                return StringHandlers.chars(player, args);
            case "chartonum":
                return StringHandlers.charToNum(player, args);
            case "numtochar":
                return StringHandlers.numToChar(player, args);
            case "tab":
                return StringHandlers.tab(player, args);
            case "return":
                return StringHandlers.returnn(player, args);
            case "enter":
                return StringHandlers.enter(player, args);
            case "quote":
                return StringHandlers.quote(player, args);
            case "backslash":
                return StringHandlers.backslash(player, args);
            case "empty":
                return StringHandlers.empty(player, args);

            // Sound functions
            case "sound":
                return TypeHandlers.sound(player, args);
            case "soundbusy":
                return TypeHandlers.soundBusy(player, args);

            // Collection functions
            case "count":
                return count(player, args);
            case "getat":
                return getAt(player, args);
            case "setat":
                return setAt(player, args);
            case "param":
                return param(player, args);
            case "put":
                return put(player, args);
            case "clearglobals":
                return clearGlobals(player, args);

            // Misc
            case "nothing":
                return TypeHandlers.nothing(player, args);

            default:
                throw new ScriptError("No built-in handler: " + name);
        }
    }

    // Local handler implementations

    private static int param(DirPlayer player, List<Integer> args) throws ScriptError {
        int paramNumber = player.getDatum(args.get(0)).intValue();
        int scopeRef = player.currentScopeRef();
        ScriptScope scope = player.scopes.get(scopeRef);
        if (scope.args != null && paramNumber >= 1 && paramNumber <= scope.args.size()) {
            return scope.args.get(paramNumber - 1);
        }
        return 0; // Void
    }

    private static int count(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));

        if (obj.isList()) {
            return player.allocDatum(Datum.ofInt(obj.toList().size()));
        } else if (obj.isPropList()) {
            return player.allocDatum(Datum.ofInt(obj.toPropList().size()));
        } else if (obj.isVoid()) {
            return player.allocDatum(Datum.ofInt(0));
        } else {
            throw new ScriptError("Cannot get count of non-list (type: " + obj.typeStr() + ")");
        }
    }

    private static int getAt(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        int position = player.getDatum(args.get(1)).intValue();
        int index = position - 1;

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
            return list.get(index);
        }

        if (obj.isPropList()) {
            List<int[]> propList = obj.toPropList();
            if (index < 0 || index >= propList.size()) {
                throw new ScriptError("Index " + position + " out of bounds for proplist of length " + propList.size());
            }
            return propList.get(index)[1]; // Return value, not key
        }

        throw new ScriptError("Cannot getAt of non-list (type: " + obj.typeStr() + ")");
    }

    private static int setAt(DirPlayer player, List<Integer> args) throws ScriptError {
        int listRef = args.get(0);
        int position = player.getDatum(args.get(1)).intValue();
        int newValue = args.get(2);
        int index = position - 1;

        Datum listDatum = player.getDatumMut(listRef);

        if (listDatum.isPoint()) {
            if (index < 0 || index >= 2) {
                throw new ScriptError("point index " + position + " out of bounds");
            }
            listDatum.setPointAt(index, newValue);
            return 0;
        }

        if (listDatum.isRect()) {
            if (index < 0 || index >= 4) {
                throw new ScriptError("rect index " + position + " out of bounds");
            }
            listDatum.setRectAt(index, newValue);
            return 0;
        }

        if (listDatum.isList()) {
            List<Integer> list = listDatum.toListMut();
            if (index < 0) {
                throw new ScriptError("Index " + position + " out of bounds");
            }
            if (index < list.size()) {
                list.set(index, newValue);
            } else {
                // Extend list if needed
                while (list.size() <= index) {
                    list.add(0);
                }
                list.set(index, newValue);
            }
            return 0;
        }

        if (listDatum.isPropList()) {
            List<int[]> propList = listDatum.toPropListMut();
            if (index < 0 || index >= propList.size()) {
                throw new ScriptError("Index " + position + " out of bounds");
            }
            propList.get(index)[1] = newValue;
            return 0;
        }

        throw new ScriptError("Cannot setAt of type " + listDatum.typeStr());
    }

    private static int put(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            System.out.println("--");
            return 0;
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) output.append(" ");
            Datum datum = player.getDatum(args.get(i));
            if (datum.isString()) {
                output.append("\"").append(datum.stringValue()).append("\"");
            } else {
                output.append(player.formatDatum(datum));
            }
        }

        System.out.println("-- " + output);
        return 0;
    }

    private static int clearGlobals(DirPlayer player, List<Integer> args) throws ScriptError {
        player.globals.clear();
        player.initializeGlobals();
        return 0;
    }

    private static int random(DirPlayer player, List<Integer> args) throws ScriptError {
        int max = player.getDatum(args.get(0)).intValue();
        if (max <= 0) {
            throw new ScriptError("random: argument must be greater than 0");
        }
        // Director's random(n) returns 1 to n inclusive
        int randomInt = random.nextInt(max) + 1;
        return player.allocDatum(Datum.ofInt(randomInt));
    }

    private static int bitAnd(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        int b = player.getDatum(args.get(1)).intValue();
        return player.allocDatum(Datum.ofInt(a & b));
    }

    private static int bitOr(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        int b = player.getDatum(args.get(1)).intValue();
        return player.allocDatum(Datum.ofInt(a | b));
    }

    private static int bitNot(DirPlayer player, List<Integer> args) throws ScriptError {
        int a = player.getDatum(args.get(0)).intValue();
        return player.allocDatum(Datum.ofInt(~a));
    }
}
