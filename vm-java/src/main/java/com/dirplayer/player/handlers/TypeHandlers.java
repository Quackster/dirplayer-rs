package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-related handler functions.
 * Port of Rust TypeHandlers struct.
 */
public class TypeHandlers {

    /**
     * Get the ilk (type) names of a datum.
     */
    public static List<String> getDatumIlks(Datum datum) throws ScriptError {
        List<String> ilks = new ArrayList<>();
        DatumType type = datum.getType();

        switch (type) {
            case List:
            case ArgList:
            case ArgListNoRet:
                ilks.add("list");
                ilks.add("linearlist");
                break;
            case Int:
                ilks.add("integer");
                break;
            case Float:
                ilks.add("float");
                break;
            case String:
                ilks.add("string");
                break;
            case Symbol:
                ilks.add("symbol");
                break;
            case Void:
                ilks.add("void");
                break;
            case PropList:
                ilks.add("proplist");
                ilks.add("list");
                break;
            case ScriptInstanceRef:
                ilks.add("instance");
                break;
            case ScriptRef:
                ilks.add("script");
                break;
            case CastMemberRef:
                ilks.add(datum.isCastMemberValid() ? "member" : "void");
                break;
            case ColorRef:
                ilks.add("color");
                break;
            case TimeoutRef:
            case TimeoutFactory:
            case TimeoutInstance:
                ilks.add("timeout");
                break;
            case BitmapRef:
            case Matte:
                ilks.add("image");
                break;
            case Rect:
                ilks.add("rect");
                break;
            case Point:
                ilks.add("point");
                break;
            case SpriteRef:
                ilks.add("sprite");
                break;
            case PaletteRef:
                ilks.add("palette");
                break;
            case Vector:
                ilks.add("vector");
                break;
            case StringChunk:
                ilks.add("string");
                break;
            case CastLibRef:
                ilks.add("castlib");
                break;
            case StageRef:
                ilks.add("stage");
                break;
            case SoundChannel:
            case SoundRef:
                ilks.add("sound");
                break;
            case CursorRef:
                ilks.add("cursor");
                break;
            case Xtra:
                ilks.add("xtra");
                break;
            case XtraInstance:
                ilks.add("instance");
                break;
            case PlayerRef:
                ilks.add("player");
                break;
            case MovieRef:
                ilks.add("movie");
                break;
            case XmlRef:
                ilks.add("xml");
                break;
            case DateRef:
                ilks.add("date");
                break;
            case MathRef:
                ilks.add("math");
                break;
            default:
                throw new ScriptError("Getting ilk for unknown type: " + datum.typeStr());
        }
        return ilks;
    }

    public static String getDatumIlk(Datum datum) throws ScriptError {
        List<String> ilks = getDatumIlks(datum);
        return ilks.isEmpty() ? "void" : ilks.get(0);
    }

    public static boolean isDatumIlk(Datum datum, String ilk) throws ScriptError {
        List<String> ilks = getDatumIlks(datum);
        for (String i : ilks) {
            if (i.equalsIgnoreCase(ilk)) {
                return true;
            }
        }
        return false;
    }

    // Built-in functions

    public static int objectp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        boolean isObject = !obj.isVoid() && !obj.isFloat() && !obj.isInt() && !obj.isSymbol() && !obj.isString();
        return player.allocDatum(Datum.ofInt(isObject ? 1 : 0));
    }

    public static int voidp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        return player.allocDatum(Datum.ofInt(obj.isVoid() ? 1 : 0));
    }

    public static int listp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        boolean isList = obj.isList() || obj.isPropList();
        return player.allocDatum(Datum.ofInt(isList ? 1 : 0));
    }

    public static int symbolp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        return player.allocDatum(Datum.ofInt(obj.isSymbol() ? 1 : 0));
    }

    public static int stringp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        boolean isString = obj.isString() || obj.isStringChunk();
        return player.allocDatum(Datum.ofInt(isString ? 1 : 0));
    }

    public static int integerp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        return player.allocDatum(Datum.ofInt(obj.isInt() ? 1 : 0));
    }

    public static int floatp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        return player.allocDatum(Datum.ofInt(obj.isFloat() ? 1 : 0));
    }

    public static int voidFunc(DirPlayer player, List<Integer> args) throws ScriptError {
        return 0; // Void
    }

    public static int ilk(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));

        if (args.size() > 1) {
            Datum queryDatum = player.getDatum(args.get(1));
            String query = queryDatum.stringValue();
            boolean result = isDatumIlk(obj, query);
            return player.allocDatum(Datum.ofInt(result ? 1 : 0));
        } else {
            String ilkStr = getDatumIlk(obj);
            return player.allocDatum(Datum.ofSymbol(ilkStr));
        }
    }

    public static int integer(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum value = player.getDatum(args.get(0));

        Datum result;
        if (value.isInt()) {
            result = Datum.ofInt(value.intValue());
        } else if (value.isFloat()) {
            result = Datum.ofInt((int) Math.round(value.floatValue()));
        } else if (value.isSpriteRef()) {
            result = Datum.ofInt(value.toSpriteRef());
        } else if (value.isString()) {
            String s = value.stringValue();
            Integer parsed = parseInteger(s);
            if (parsed != null) {
                result = Datum.ofInt(parsed);
            } else {
                return 0; // Void
            }
        } else if (value.isVoid()) {
            return 0; // Void
        } else {
            throw new ScriptError("Cannot convert datum of type " + value.typeStr() + " to integer");
        }
        return player.allocDatum(result);
    }

    private static Integer parseInteger(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }

        if (trimmed.equals("-")) {
            return 0;
        }

        StringBuilder result = new StringBuilder();
        boolean foundValidDigit = false;

        for (char c : trimmed.toCharArray()) {
            if (c >= '0' && c <= '9') {
                result.append(c);
                foundValidDigit = true;
            } else if (c == '.') {
                return null;
            } else if (c == '-') {
                if (result.length() == 0) {
                    result.append(c);
                } else {
                    return null;
                }
            } else {
                if (!foundValidDigit) {
                    return null;
                }
            }
        }

        if (!foundValidDigit) {
            return null;
        }

        try {
            return Integer.parseInt(result.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int floatFunc(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum value = player.getDatum(args.get(0));

        Datum result;
        if (value.isFloat()) {
            result = Datum.ofFloat(value.floatValue());
        } else if (value.isInt()) {
            result = Datum.ofFloat(value.intValue());
        } else if (value.isSpriteRef()) {
            result = Datum.ofFloat(value.toSpriteRef());
        } else if (value.isString()) {
            try {
                double floatValue = Double.parseDouble(value.stringValue());
                result = Datum.ofFloat(floatValue);
            } catch (NumberFormatException e) {
                result = value.clone();
            }
        } else if (value.isVoid()) {
            return 0; // Void
        } else {
            throw new ScriptError("Cannot convert datum of type " + value.typeStr() + " to float");
        }
        return player.allocDatum(result);
    }

    public static int symbol(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum symbolName = player.getDatum(args.get(0));

        Datum result;
        if (symbolName.isSymbol()) {
            result = symbolName.clone();
        } else if (symbolName.isString()) {
            String strValue = symbolName.stringValue();
            if (strValue.isEmpty()) {
                result = Datum.ofSymbol("");
            } else if (strValue.startsWith("#")) {
                result = Datum.ofSymbol("#");
            } else {
                result = Datum.ofSymbol(strValue);
            }
        } else {
            throw new ScriptError("Cannot convert datum of type " + symbolName.typeStr() + " to symbol");
        }
        return player.allocDatum(result);
    }

    public static int point(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("point() requires exactly 2 arguments");
        }

        Datum x = player.getDatum(args.get(0));
        Datum y = player.getDatum(args.get(1));

        int xRef = allocNumeric(player, x);
        int yRef = allocNumeric(player, y);

        return player.allocDatum(Datum.ofPoint(xRef, yRef));
    }

    public static int rect(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2 && args.size() != 4) {
            throw new ScriptError("rect() requires 2 or 4 arguments");
        }

        if (args.size() == 4 && player.getDatum(args.get(0)).isNumber()) {
            // rect(left, top, right, bottom)
            int left = allocNumeric(player, player.getDatum(args.get(0)));
            int top = allocNumeric(player, player.getDatum(args.get(1)));
            int right = allocNumeric(player, player.getDatum(args.get(2)));
            int bottom = allocNumeric(player, player.getDatum(args.get(3)));
            return player.allocDatum(Datum.ofRect(left, top, right, bottom));
        }

        if (args.size() == 2) {
            // rect(Point, Point)
            int[] p1 = player.getDatum(args.get(0)).toPoint();
            int[] p2 = player.getDatum(args.get(1)).toPoint();

            int left = allocNumeric(player, player.getDatum(p1[0]));
            int top = allocNumeric(player, player.getDatum(p1[1]));
            int right = allocNumeric(player, player.getDatum(p2[0]));
            int bottom = allocNumeric(player, player.getDatum(p2[1]));
            return player.allocDatum(Datum.ofRect(left, top, right, bottom));
        }

        throw new ScriptError("Invalid rect() arguments");
    }

    private static int allocNumeric(DirPlayer player, Datum d) throws ScriptError {
        if (d.isInt()) {
            return player.allocDatum(Datum.ofInt(d.intValue()));
        } else if (d.isFloat()) {
            return player.allocDatum(Datum.ofFloat(d.floatValue()));
        } else {
            throw new ScriptError("Rect/Point component must be numeric, got " + d.typeStr());
        }
    }

    public static int rgb(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() == 3) {
            int r = player.getDatum(args.get(0)).intValue() & 0xFF;
            int g = player.getDatum(args.get(1)).intValue() & 0xFF;
            int b = player.getDatum(args.get(2)).intValue() & 0xFF;
            return player.allocDatum(Datum.ofColorRef(r, g, b));
        } else if (args.size() == 1) {
            Datum firstArg = player.getDatum(args.get(0));
            if (firstArg.isString()) {
                String hexStr = firstArg.stringValue().replace("#", "");
                if (hexStr.length() != 6) {
                    return player.allocDatum(Datum.ofColorRef(0, 0, 0));
                }
                int r = Integer.parseInt(hexStr.substring(0, 2), 16);
                int g = Integer.parseInt(hexStr.substring(2, 4), 16);
                int b = Integer.parseInt(hexStr.substring(4, 6), 16);
                return player.allocDatum(Datum.ofColorRef(r, g, b));
            }
        }
        throw new ScriptError("Invalid number of arguments for rgb");
    }

    public static int paletteIndex(DirPlayer player, List<Integer> args) throws ScriptError {
        int color = player.getDatum(args.get(0)).intValue();
        return player.allocDatum(Datum.ofPaletteIndexColor(color & 0xFF));
    }

    public static int list(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofList(DatumType.List, args, false));
    }

    public static int abs(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum value = player.getDatum(args.get(0));
        Datum result;
        if (value.isInt()) {
            result = Datum.ofInt(Math.abs(value.intValue()));
        } else if (value.isFloat()) {
            result = Datum.ofFloat(Math.abs(value.floatValue()));
        } else {
            throw new ScriptError("Cannot get abs of type: " + value.typeStr());
        }
        return player.allocDatum(result);
    }

    public static int nothing(DirPlayer player, List<Integer> args) throws ScriptError {
        return 0; // Void
    }

    public static int pi(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofFloat(Math.PI));
    }

    public static int sin(DirPlayer player, List<Integer> args) throws ScriptError {
        double value = player.getDatum(args.get(0)).toFloat();
        return player.allocDatum(Datum.ofFloat(Math.sin(value)));
    }

    public static int cos(DirPlayer player, List<Integer> args) throws ScriptError {
        double value = player.getDatum(args.get(0)).toFloat();
        return player.allocDatum(Datum.ofFloat(Math.cos(value)));
    }

    public static int sqrt(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum value = player.getDatum(args.get(0));
        double num;
        if (value.isFloat()) {
            num = value.floatValue();
        } else if (value.isInt()) {
            num = value.intValue();
        } else {
            throw new ScriptError("sqrt requires a number");
        }
        if (num < 0) {
            throw new ScriptError("sqrt of negative number");
        }
        return player.allocDatum(Datum.ofFloat(Math.sqrt(num)));
    }

    public static int atan(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum value = player.getDatum(args.get(0));
        double num;
        if (value.isFloat()) {
            num = value.floatValue();
        } else if (value.isInt()) {
            num = value.intValue();
        } else {
            throw new ScriptError("atan requires a number");
        }
        return player.allocDatum(Datum.ofFloat(Math.atan(num)));
    }

    public static int power(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("Power requires 2 arguments");
        }
        Datum base = player.getDatum(args.get(0));
        Datum exponent = player.getDatum(args.get(1));

        if (base.isInt() && exponent.isInt()) {
            return player.allocDatum(Datum.ofInt((int) Math.pow(base.intValue(), exponent.intValue())));
        } else if (base.isNumber() && exponent.isNumber()) {
            return player.allocDatum(Datum.ofFloat(Math.pow(base.toFloat(), exponent.toFloat())));
        } else {
            throw new ScriptError("Power requires two numbers");
        }
    }

    public static int bitXor(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("Bitwise XOR requires 2 arguments");
        }
        int left = player.getDatum(args.get(0)).intValue();
        int right = player.getDatum(args.get(1)).intValue();
        return player.allocDatum(Datum.ofInt(left ^ right));
    }

    public static int vector(DirPlayer player, List<Integer> args) throws ScriptError {
        double x, y, z;
        if (args.size() == 0) {
            x = y = z = 0.0;
        } else if (args.size() == 3) {
            x = player.getDatum(args.get(0)).toFloat();
            y = player.getDatum(args.get(1)).toFloat();
            z = player.getDatum(args.get(2)).toFloat();
        } else {
            throw new ScriptError("vector() expects 0 or 3 arguments");
        }
        return player.allocDatum(Datum.ofVector(x, y, z));
    }

    public static int color(DirPlayer player, List<Integer> args) throws ScriptError {
        switch (args.size()) {
            case 1: {
                // color(paletteIndex)
                int index = player.getDatum(args.get(0)).intValue();
                return player.allocDatum(Datum.ofPaletteIndexColor(index & 0xFF));
            }
            case 3: {
                // color(r, g, b)
                int r = player.getDatum(args.get(0)).intValue() & 0xFF;
                int g = player.getDatum(args.get(1)).intValue() & 0xFF;
                int b = player.getDatum(args.get(2)).intValue() & 0xFF;
                return player.allocDatum(Datum.ofColorRef(r, g, b));
            }
            case 4: {
                // color(#rgb, r, g, b)
                int r = player.getDatum(args.get(1)).intValue() & 0xFF;
                int g = player.getDatum(args.get(2)).intValue() & 0xFF;
                int b = player.getDatum(args.get(3)).intValue() & 0xFF;
                return player.allocDatum(Datum.ofColorRef(r, g, b));
            }
            default:
                throw new ScriptError("color() expects 1, 3, or 4 arguments");
        }
    }

    public static int min(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        List<Integer> argList = args;
        if (player.getDatum(args.get(0)).isList()) {
            argList = player.getDatum(args.get(0)).toList();
        }

        if (argList.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        // Find minimum
        int minRef = argList.get(0);
        double minVal = player.getDatum(minRef).toFloat();
        for (int i = 1; i < argList.size(); i++) {
            double val = player.getDatum(argList.get(i)).toFloat();
            if (val < minVal) {
                minVal = val;
                minRef = argList.get(i);
            }
        }
        return minRef;
    }

    public static int max(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        List<Integer> argList = args;
        if (player.getDatum(args.get(0)).isList()) {
            argList = player.getDatum(args.get(0)).toList();
        }

        if (argList.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        // Find maximum
        int maxRef = argList.get(0);
        double maxVal = player.getDatum(maxRef).toFloat();
        for (int i = 1; i < argList.size(); i++) {
            double val = player.getDatum(argList.get(i)).toFloat();
            if (val > maxVal) {
                maxVal = val;
                maxRef = argList.get(i);
            }
        }
        return maxRef;
    }

    public static int sound(DirPlayer player, List<Integer> args) throws ScriptError {
        int channelNum = player.getDatum(args.get(0)).intValue();
        if (channelNum == 0 || channelNum > player.soundManager.getNumChannels()) {
            throw new ScriptError("Invalid sound channel: " + channelNum);
        }
        return player.allocDatum(Datum.ofSoundChannel(channelNum));
    }

    public static int soundBusy(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("soundBusy requires a channel number");
        }
        int channelNum = player.getDatum(args.get(0)).intValue();
        boolean isBusy = player.soundManager.isChannelBusy(channelNum);
        return player.allocDatum(Datum.ofInt(isBusy ? 1 : 0));
    }
}
