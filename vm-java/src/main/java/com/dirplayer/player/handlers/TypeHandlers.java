package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CursorRef;
import com.dirplayer.player.DateObject;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.MathObject;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.XmlDocument;
import com.dirplayer.rendering.IntRect;
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

    /**
     * value(expr) - Evaluates a string as a Lingo expression.
     * For non-string inputs, returns the input unchanged.
     * Note: In Java version, we don't have full eval capability, so this is simplified.
     */
    public static int value(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));
        if (datum.isString()) {
            String s = datum.stringValue();
            // Try to parse as integer
            try {
                int intVal = Integer.parseInt(s.trim());
                return player.allocDatum(Datum.ofInt(intVal));
            } catch (NumberFormatException e) {
                // Try to parse as float
                try {
                    double floatVal = Double.parseDouble(s.trim());
                    return player.allocDatum(Datum.ofFloat(floatVal));
                } catch (NumberFormatException e2) {
                    // Return void for unparseable expressions
                    return 0; // Void
                }
            }
        }
        return args.get(0);
    }

    /**
     * cursor(cursorId) or cursor([member1, member2]) - Sets the cursor.
     */
    public static int cursor(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() == 1) {
            Datum arg = player.getDatum(args.get(0));
            if (arg.isInt()) {
                player.cursor = CursorRef.system(arg.intValue());
                return 0; // Void
            } else if (arg.isList()) {
                List<Integer> list = arg.toList();
                List<Integer> members = new ArrayList<>();
                for (int ref : list) {
                    members.add(player.getDatum(ref).intValue());
                }
                player.cursor = CursorRef.memberList(members);
                return 0; // Void
            } else {
                throw new ScriptError("Invalid argument for cursor");
            }
        } else if (args.size() == 2) {
            throw new ScriptError("Cursor call with 2 arguments not implemented");
        } else {
            throw new ScriptError("Invalid number of arguments for cursor");
        }
    }

    /**
     * timeout() or timeout(name) - Returns timeout factory or reference.
     */
    public static int timeout(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            // Called without arguments: return the timeout factory
            return player.allocDatum(Datum.ofTimeoutFactory());
        } else {
            // Called with a name argument: return a timeout reference
            String name = player.getDatum(args.get(0)).stringValue();
            return player.allocDatum(Datum.ofTimeoutRef(name));
        }
    }

    /**
     * image(width, height, bitDepth, [alphaDepth/palette]) - Creates a new bitmap.
     */
    public static int image(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 3) {
            throw new ScriptError("image() expects at least 3 arguments: width, height, bitDepth, optional alphaDepth, got " + args.size());
        }

        Datum widthDatum = player.getDatum(args.get(0));
        Datum heightDatum = player.getDatum(args.get(1));

        int width;
        if (widthDatum.isInt()) {
            width = widthDatum.intValue();
        } else if (widthDatum.isFloat()) {
            width = (int) Math.round(widthDatum.floatValue());
        } else {
            width = widthDatum.intValue();
        }

        int height;
        if (heightDatum.isInt()) {
            height = heightDatum.intValue();
        } else if (heightDatum.isFloat()) {
            height = (int) Math.round(heightDatum.floatValue());
        } else {
            height = heightDatum.intValue();
        }

        int bitDepth = player.getDatum(args.get(2)).intValue();

        // Note: alphaDepth and palette handling from args[3] not yet implemented

        // Create bitmap using bitmap manager
        int bitmapId = player.bitmapManager.createBitmap(width, height, bitDepth);
        return player.allocDatum(Datum.ofBitmapRef(bitmapId));
    }

    /**
     * xtra(name) - Gets an Xtra by name.
     */
    public static int xtra(DirPlayer player, List<Integer> args) throws ScriptError {
        String xtraName = player.getDatum(args.get(0)).stringValue();
        // In Java version, we check if xtra is registered (simplified)
        // For now, just return an Xtra datum
        return player.allocDatum(Datum.ofXtra(xtraName));
    }

    /**
     * union(rect1, rect2) - Returns the union of two rectangles.
     */
    public static int union(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("Union requires 2 arguments");
        }

        int[] leftRefs = player.getDatum(args.get(0)).toRect();
        int[] rightRefs = player.getDatum(args.get(1)).toRect();

        int l1 = player.getDatum(leftRefs[0]).intValue();
        int t1 = player.getDatum(leftRefs[1]).intValue();
        int r1 = player.getDatum(leftRefs[2]).intValue();
        int b1 = player.getDatum(leftRefs[3]).intValue();

        int l2 = player.getDatum(rightRefs[0]).intValue();
        int t2 = player.getDatum(rightRefs[1]).intValue();
        int r2 = player.getDatum(rightRefs[2]).intValue();
        int b2 = player.getDatum(rightRefs[3]).intValue();

        IntRect rect1 = new IntRect(l1, t1, r1, b1);
        IntRect rect2 = new IntRect(l2, t2, r2, b2);
        IntRect result = rect1.union(rect2);

        int left = player.allocDatum(Datum.ofInt(result.left));
        int top = player.allocDatum(Datum.ofInt(result.top));
        int right = player.allocDatum(Datum.ofInt(result.right));
        int bottom = player.allocDatum(Datum.ofInt(result.bottom));

        return player.allocDatum(Datum.ofRect(left, top, right, bottom));
    }

    /**
     * intersect(rect1, rect2) - Returns the intersection of two rectangles.
     */
    public static int intersect(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("Intersect requires 2 arguments");
        }

        int[] leftRefs = player.getDatum(args.get(0)).toRect();
        int[] rightRefs = player.getDatum(args.get(1)).toRect();

        int l1 = player.getDatum(leftRefs[0]).intValue();
        int t1 = player.getDatum(leftRefs[1]).intValue();
        int r1 = player.getDatum(leftRefs[2]).intValue();
        int b1 = player.getDatum(leftRefs[3]).intValue();

        int l2 = player.getDatum(rightRefs[0]).intValue();
        int t2 = player.getDatum(rightRefs[1]).intValue();
        int r2 = player.getDatum(rightRefs[2]).intValue();
        int b2 = player.getDatum(rightRefs[3]).intValue();

        IntRect rect1 = new IntRect(l1, t1, r1, b1);
        IntRect rect2 = new IntRect(l2, t2, r2, b2);
        IntRect result = rect1.intersect(rect2);

        int left = player.allocDatum(Datum.ofInt(result.left));
        int top = player.allocDatum(Datum.ofInt(result.top));
        int right = player.allocDatum(Datum.ofInt(result.right));
        int bottom = player.allocDatum(Datum.ofInt(result.bottom));

        return player.allocDatum(Datum.ofRect(left, top, right, bottom));
    }

    /**
     * add(list, item) - Adds an item to a list.
     */
    public static int add(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("Add requires 2 arguments");
        }

        Datum left = player.getDatum(args.get(0));
        if (left.isVoid()) {
            return 0; // Void
        }

        if (left.isList()) {
            List<Integer> list = left.toListMut();
            list.add(args.get(1));
            return 0; // Void - add modifies in place
        }

        throw new ScriptError("Add not supported for " + left.typeStr());
    }

    /**
     * getAProp(obj, propName) - Gets a property from a proplist or script instance.
     */
    public static int getAProp(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));
        Datum propKey = player.getDatum(args.get(1));

        if (obj.isPropList()) {
            List<int[]> propList = obj.toPropList();
            // Search by key
            if (propKey.isSymbol() || propKey.isString()) {
                String keyStr = propKey.isSymbol() ? propKey.symbolValue() : propKey.stringValue();
                for (int[] pair : propList) {
                    Datum key = player.getDatum(pair[0]);
                    String keyName = key.isSymbol() ? key.symbolValue() : key.stringValue();
                    if (keyName.equalsIgnoreCase(keyStr)) {
                        return pair[1];
                    }
                }
            } else if (propKey.isInt()) {
                // Numeric index (1-based)
                int index = propKey.intValue() - 1;
                if (index >= 0 && index < propList.size()) {
                    return propList.get(index)[1];
                }
            }
            return 0; // Void - not found
        }

        if (obj.isScriptInstanceRef()) {
            int instanceId = obj.getScriptInstanceRef();
            var instance = player.allocator.getScriptInstance(instanceId);
            if (instance != null) {
                String propName = propKey.isSymbol() ? propKey.symbolValue() : propKey.stringValue();
                Integer propRef = instance.getProperty(propName);
                return propRef != null ? propRef : 0;
            }
            return 0; // Void
        }

        throw new ScriptError("Cannot getaProp of type: " + obj.typeStr());
    }

    /**
     * setAProp(obj, propName, value) - Sets a property on a proplist or script instance.
     */
    public static int setAProp(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("setAProp requires 3 arguments");
        }

        Datum obj = player.getDatum(args.get(0));
        Datum propKey = player.getDatum(args.get(1));
        int valueRef = args.get(2);

        if (obj.isPropList()) {
            List<int[]> propList = obj.toPropListMut();
            String keyStr = propKey.isSymbol() ? propKey.symbolValue() : propKey.stringValue();

            // Search for existing key
            for (int[] pair : propList) {
                Datum key = player.getDatum(pair[0]);
                String keyName = key.isSymbol() ? key.symbolValue() : key.stringValue();
                if (keyName.equalsIgnoreCase(keyStr)) {
                    pair[1] = valueRef;
                    return 0; // Void
                }
            }
            // Key not found - add new entry
            int keyRef = player.allocDatum(propKey.isSymbol() ? Datum.ofSymbol(keyStr) : Datum.ofString(keyStr));
            propList.add(new int[] { keyRef, valueRef });
            return 0; // Void
        }

        if (obj.isScriptInstanceRef()) {
            int instanceId = obj.getScriptInstanceRef();
            var instance = player.allocator.getScriptInstance(instanceId);
            if (instance != null) {
                String propName = propKey.isSymbol() ? propKey.symbolValue() : propKey.stringValue();
                instance.setProperty(propName, valueRef);
            }
            return 0; // Void
        }

        throw new ScriptError("Cannot setaProp on type: " + obj.typeStr());
    }

    /**
     * getPropAt(propList, position) - Gets the key at a specific position in a proplist.
     */
    public static int getPropAt(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum propListDatum = player.getDatum(args.get(0));
        int position = player.getDatum(args.get(1)).intValue();
        int index = position - 1;

        if (!propListDatum.isPropList()) {
            throw new ScriptError("getPropAt requires a property list");
        }

        List<int[]> propList = propListDatum.toPropList();
        if (index < 0 || index >= propList.size()) {
            throw new ScriptError("Index " + position + " out of bounds for proplist of length " + propList.size());
        }

        // Return the KEY at this position, not the value
        return propList.get(index)[0];
    }

    /**
     * sort(list) - Sorts a list or proplist in place.
     */
    public static int sort(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));

        if (datum.isList()) {
            List<Integer> list = datum.toListMut();
            // Simple sort by comparing datum values
            list.sort((a, b) -> {
                try {
                    Datum da = player.getDatum(a);
                    Datum db = player.getDatum(b);
                    return compareDatums(da, db);
                } catch (ScriptError e) {
                    return 0;
                }
            });
            datum.setSorted(true);
            return 0; // Void
        }

        if (datum.isPropList()) {
            List<int[]> propList = datum.toPropListMut();
            // Sort by keys
            propList.sort((a, b) -> {
                try {
                    Datum ka = player.getDatum(a[0]);
                    Datum kb = player.getDatum(b[0]);
                    return compareDatums(ka, kb);
                } catch (ScriptError e) {
                    return 0;
                }
            });
            datum.setSorted(true);
            return 0; // Void
        }

        throw new ScriptError("Cannot sort type: " + datum.typeStr());
    }

    /**
     * Helper method to compare two datums for sorting.
     */
    private static int compareDatums(Datum a, Datum b) throws ScriptError {
        // Numbers come before strings, strings before symbols
        if (a.isNumber() && b.isNumber()) {
            return Double.compare(a.toFloat(), b.toFloat());
        }
        if (a.isString() && b.isString()) {
            return a.stringValue().compareToIgnoreCase(b.stringValue());
        }
        if (a.isSymbol() && b.isSymbol()) {
            return a.symbolValue().compareToIgnoreCase(b.symbolValue());
        }
        // Different types - use type order
        return Integer.compare(getTypeOrder(a), getTypeOrder(b));
    }

    /**
     * Helper method to get type order for sorting.
     */
    private static int getTypeOrder(Datum d) {
        if (d.isInt()) return 0;
        if (d.isFloat()) return 1;
        if (d.isString()) return 2;
        if (d.isSymbol()) return 3;
        return 4;
    }

    /**
     * newObject(type, [args...]) - Creates a new object of the specified type.
     */
    public static int newObject(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("newObject requires at least one argument");
        }

        String objectType = player.getDatum(args.get(0)).stringValue();

        switch (objectType.toLowerCase()) {
            case "xml":
                int xmlId = player.nextXmlId++;
                XmlDocument xmlDoc = new XmlDocument(xmlId);
                player.xmlDocuments.put(xmlId, xmlDoc);
                return player.allocDatum(Datum.ofXmlRef(xmlId));

            case "date":
                int dateId = player.nextXmlId++;  // Use same ID counter
                DateObject dateObj = new DateObject(dateId);
                player.dateObjects.put(dateId, dateObj);
                return player.allocDatum(Datum.ofDateRef(dateId));

            case "math":
                int mathId = player.nextXmlId++;  // Use same ID counter
                MathObject mathObj = new MathObject(mathId);
                player.mathObjects.put(mathId, mathObj);
                return player.allocDatum(Datum.ofMathRef(mathId));

            case "object":
                // Allocate an empty prop list, unsorted
                return player.allocDatum(Datum.ofPropList(new ArrayList<>(), false));

            case "string":
                String value = args.size() > 1 ? player.getDatum(args.get(1)).stringValue() : "";
                return player.allocDatum(Datum.ofString(value));

            default:
                throw new ScriptError("newObject: Unsupported object type '" + objectType + "'");
        }
    }

    /**
     * getSubProp(datum, propKey) - Gets a sub-property from various datum types.
     * Used for bracket notation access like obj[key].
     */
    public static int getSubProp(DirPlayer player, int datumRef, int propKeyRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        Datum propKey = player.getDatum(propKeyRef);

        if (datum.isPropList()) {
            return getAProp(player, List.of(datumRef, propKeyRef));
        }

        if (datum.isRect()) {
            int[] arr = datum.toRect();
            int index = propKey.intValue(); // 1-4
            int idx = index - 1;
            if (idx < 0 || idx >= 4) {
                throw new ScriptError("Rect index " + index + " out of bounds (must be 1-4)");
            }
            Datum val = player.getDatum(arr[idx]);
            double fval = val.toFloat();
            if (fval == Math.floor(fval)) {
                return player.allocDatum(Datum.ofInt((int) fval));
            } else {
                return player.allocDatum(Datum.ofFloat(fval));
            }
        }

        if (datum.isList()) {
            List<Integer> list = datum.toList();
            int position = propKey.intValue();
            int index = position - 1;
            if (index < 0 || index >= list.size()) {
                throw new ScriptError("Index out of bounds: " + index);
            }
            return list.get(index);
        }

        if (datum.isPoint()) {
            int[] arr = datum.toPoint();
            int index = propKey.intValue();
            if (index < 1 || index > 2) {
                throw new ScriptError("Invalid sub-prop position for point: " + index);
            }
            Datum val = player.getDatum(arr[index - 1]);
            double fval = val.toFloat();
            if (fval == Math.floor(fval)) {
                return player.allocDatum(Datum.ofInt((int) fval));
            } else {
                return player.allocDatum(Datum.ofFloat(fval));
            }
        }

        if (datum.isScriptInstanceRef()) {
            // Numeric index
            if (propKey.isInt()) {
                int index = propKey.intValue();
                int instanceId = datum.getScriptInstanceRef();
                var instance = player.allocator.getScriptInstance(instanceId);
                if (instance != null) {
                    List<String> propertyNames = new ArrayList<>(instance.properties.keySet());
                    propertyNames.sort(String::compareToIgnoreCase);
                    int zeroBasedIndex = index - 1;
                    if (zeroBasedIndex >= 0 && zeroBasedIndex < propertyNames.size()) {
                        String propName = propertyNames.get(zeroBasedIndex);
                        Integer propRef = instance.getProperty(propName);
                        return propRef != null ? propRef : 0;
                    }
                }
                return 0; // Void
            }

            // String/symbol key
            String propName = propKey.isSymbol() ? propKey.symbolValue() : propKey.stringValue();
            int instanceId = datum.getScriptInstanceRef();
            var instance = player.allocator.getScriptInstance(instanceId);
            if (instance != null) {
                Integer propRef = instance.getProperty(propName);
                return propRef != null ? propRef : 0;
            }
            return 0; // Void
        }

        if (datum.isInt()) {
            String propName = propKey.stringValue();
            int i = datum.intValue();
            switch (propName.toLowerCase()) {
                case "abs":
                    return player.allocDatum(Datum.ofInt(Math.abs(i)));
                case "integer":
                    return datumRef;
                case "float":
                    return player.allocDatum(Datum.ofFloat(i));
                case "char":
                    if (i >= 0 && i <= 255) {
                        return player.allocDatum(Datum.ofString(String.valueOf((char) i)));
                    } else {
                        throw new ScriptError("Integer " + i + " out of range for char");
                    }
                case "string":
                    return player.allocDatum(Datum.ofString(String.valueOf(i)));
                default:
                    throw new ScriptError("Unknown property '" + propName + "' for integer");
            }
        }

        if (datum.isFloat()) {
            String propName = propKey.stringValue();
            double f = datum.floatValue();
            switch (propName.toLowerCase()) {
                case "abs":
                    return player.allocDatum(Datum.ofFloat(Math.abs(f)));
                case "integer":
                    return player.allocDatum(Datum.ofInt((int) Math.round(f)));
                case "float":
                    return datumRef;
                case "string":
                    return player.allocDatum(Datum.ofString(String.valueOf(f)));
                default:
                    throw new ScriptError("Unknown property '" + propName + "' for float");
            }
        }

        throw new ScriptError("Cannot get sub-prop from type " + datum.typeStr());
    }

    /**
     * setSubProp(datum, propKey, value) - Sets a sub-property on various datum types.
     * Used for bracket notation assignment like obj[key] = value.
     */
    public static void setSubProp(DirPlayer player, int datumRef, int propKeyRef, int valueRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        Datum propKey = player.getDatum(propKeyRef);

        if (datum.isPropList()) {
            setAProp(player, List.of(datumRef, propKeyRef, valueRef));
            return;
        }

        if (datum.isList()) {
            int position = propKey.intValue();
            int index = position - 1;
            List<Integer> list = datum.toListMut();
            if (index < 0) {
                throw new ScriptError("Index out of bounds: " + index);
            } else if (index < list.size()) {
                list.set(index, valueRef);
            } else {
                // Extend list with zeros
                while (list.size() <= index) {
                    list.add(0);
                }
                list.set(index, valueRef);
            }
            return;
        }

        throw new ScriptError("Cannot set sub-prop on type " + datum.typeStr());
    }

    /**
     * tan(angle) - Returns the tangent of an angle (in radians).
     */
    public static int tan(DirPlayer player, List<Integer> args) throws ScriptError {
        double value = player.getDatum(args.get(0)).toFloat();
        return player.allocDatum(Datum.ofFloat(Math.tan(value)));
    }

    /**
     * exp(value) - Returns e raised to the power of value.
     */
    public static int exp(DirPlayer player, List<Integer> args) throws ScriptError {
        double value = player.getDatum(args.get(0)).toFloat();
        return player.allocDatum(Datum.ofFloat(Math.exp(value)));
    }

    /**
     * log(value) - Returns the natural logarithm of value.
     */
    public static int log(DirPlayer player, List<Integer> args) throws ScriptError {
        double value = player.getDatum(args.get(0)).toFloat();
        if (value <= 0) {
            throw new ScriptError("log of non-positive number");
        }
        return player.allocDatum(Datum.ofFloat(Math.log(value)));
    }

    /**
     * inside(point, rect) - Returns true if the point is inside the rectangle.
     */
    public static int inside(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("inside requires 2 arguments");
        }

        Datum pointDatum = player.getDatum(args.get(0));
        Datum rectDatum = player.getDatum(args.get(1));

        if (!pointDatum.isPoint()) {
            throw new ScriptError("First argument to inside must be a point");
        }
        if (!rectDatum.isRect()) {
            throw new ScriptError("Second argument to inside must be a rect");
        }

        int[] pointRefs = pointDatum.toPoint();
        int[] rectRefs = rectDatum.toRect();

        int px = player.getDatum(pointRefs[0]).intValue();
        int py = player.getDatum(pointRefs[1]).intValue();

        int left = player.getDatum(rectRefs[0]).intValue();
        int top = player.getDatum(rectRefs[1]).intValue();
        int right = player.getDatum(rectRefs[2]).intValue();
        int bottom = player.getDatum(rectRefs[3]).intValue();

        boolean isInside = px >= left && px < right && py >= top && py < bottom;
        return player.allocDatum(Datum.ofInt(isInside ? 1 : 0));
    }

    /**
     * map(value, srcRect, destRect) - Maps a point/rect from source to destination coordinates.
     */
    public static int map(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("map requires 3 arguments");
        }

        Datum valueDatum = player.getDatum(args.get(0));
        int[] srcRectRefs = player.getDatum(args.get(1)).toRect();
        int[] destRectRefs = player.getDatum(args.get(2)).toRect();

        double srcLeft = player.getDatum(srcRectRefs[0]).toFloat();
        double srcTop = player.getDatum(srcRectRefs[1]).toFloat();
        double srcRight = player.getDatum(srcRectRefs[2]).toFloat();
        double srcBottom = player.getDatum(srcRectRefs[3]).toFloat();

        double destLeft = player.getDatum(destRectRefs[0]).toFloat();
        double destTop = player.getDatum(destRectRefs[1]).toFloat();
        double destRight = player.getDatum(destRectRefs[2]).toFloat();
        double destBottom = player.getDatum(destRectRefs[3]).toFloat();

        double srcWidth = srcRight - srcLeft;
        double srcHeight = srcBottom - srcTop;
        double destWidth = destRight - destLeft;
        double destHeight = destBottom - destTop;

        if (valueDatum.isPoint()) {
            int[] pointRefs = valueDatum.toPoint();
            double px = player.getDatum(pointRefs[0]).toFloat();
            double py = player.getDatum(pointRefs[1]).toFloat();

            double newX = destLeft + (px - srcLeft) * destWidth / srcWidth;
            double newY = destTop + (py - srcTop) * destHeight / srcHeight;

            int xRef = player.allocDatum(Datum.ofInt((int) Math.round(newX)));
            int yRef = player.allocDatum(Datum.ofInt((int) Math.round(newY)));
            return player.allocDatum(Datum.ofPoint(xRef, yRef));
        }

        if (valueDatum.isRect()) {
            int[] rectRefs = valueDatum.toRect();
            double l = player.getDatum(rectRefs[0]).toFloat();
            double t = player.getDatum(rectRefs[1]).toFloat();
            double r = player.getDatum(rectRefs[2]).toFloat();
            double b = player.getDatum(rectRefs[3]).toFloat();

            double newL = destLeft + (l - srcLeft) * destWidth / srcWidth;
            double newT = destTop + (t - srcTop) * destHeight / srcHeight;
            double newR = destLeft + (r - srcLeft) * destWidth / srcWidth;
            double newB = destTop + (b - srcTop) * destHeight / srcHeight;

            int leftRef = player.allocDatum(Datum.ofInt((int) Math.round(newL)));
            int topRef = player.allocDatum(Datum.ofInt((int) Math.round(newT)));
            int rightRef = player.allocDatum(Datum.ofInt((int) Math.round(newR)));
            int bottomRef = player.allocDatum(Datum.ofInt((int) Math.round(newB)));
            return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
        }

        throw new ScriptError("map requires a point or rect as first argument");
    }

    /**
     * offset(rect/point, dx, dy) - Offsets a rect or point by dx, dy.
     * Note: This is different from string offset() which is in StringHandlers.
     */
    public static int offsetGeo(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("offset requires 3 arguments");
        }

        Datum valueDatum = player.getDatum(args.get(0));
        int dx = player.getDatum(args.get(1)).intValue();
        int dy = player.getDatum(args.get(2)).intValue();

        if (valueDatum.isPoint()) {
            int[] pointRefs = valueDatum.toPoint();
            int px = player.getDatum(pointRefs[0]).intValue();
            int py = player.getDatum(pointRefs[1]).intValue();

            int xRef = player.allocDatum(Datum.ofInt(px + dx));
            int yRef = player.allocDatum(Datum.ofInt(py + dy));
            return player.allocDatum(Datum.ofPoint(xRef, yRef));
        }

        if (valueDatum.isRect()) {
            int[] rectRefs = valueDatum.toRect();
            int l = player.getDatum(rectRefs[0]).intValue() + dx;
            int t = player.getDatum(rectRefs[1]).intValue() + dy;
            int r = player.getDatum(rectRefs[2]).intValue() + dx;
            int b = player.getDatum(rectRefs[3]).intValue() + dy;

            int leftRef = player.allocDatum(Datum.ofInt(l));
            int topRef = player.allocDatum(Datum.ofInt(t));
            int rightRef = player.allocDatum(Datum.ofInt(r));
            int bottomRef = player.allocDatum(Datum.ofInt(b));
            return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
        }

        throw new ScriptError("offset requires a point or rect as first argument");
    }

    /**
     * inflate(rect, dx, dy) - Inflates a rect by dx horizontally and dy vertically.
     */
    public static int inflate(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("inflate requires 3 arguments");
        }

        int[] rectRefs = player.getDatum(args.get(0)).toRect();
        int dx = player.getDatum(args.get(1)).intValue();
        int dy = player.getDatum(args.get(2)).intValue();

        int l = player.getDatum(rectRefs[0]).intValue() - dx;
        int t = player.getDatum(rectRefs[1]).intValue() - dy;
        int r = player.getDatum(rectRefs[2]).intValue() + dx;
        int b = player.getDatum(rectRefs[3]).intValue() + dy;

        int leftRef = player.allocDatum(Datum.ofInt(l));
        int topRef = player.allocDatum(Datum.ofInt(t));
        int rightRef = player.allocDatum(Datum.ofInt(r));
        int bottomRef = player.allocDatum(Datum.ofInt(b));
        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
    }

    /**
     * duplicate(list) - Creates a shallow copy of a list or proplist.
     */
    public static int duplicate(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));

        if (datum.isList()) {
            List<Integer> original = datum.toList();
            List<Integer> copy = new ArrayList<>(original);
            return player.allocDatum(Datum.ofList(copy, datum.isSorted()));
        }

        if (datum.isPropList()) {
            List<int[]> original = datum.toPropList();
            List<Datum.PropListPair> copy = new ArrayList<>();
            for (int[] pair : original) {
                copy.add(new Datum.PropListPair(pair[0], pair[1]));
            }
            return player.allocDatum(Datum.ofPropList(copy, datum.isSorted()));
        }

        throw new ScriptError("Cannot duplicate type: " + datum.typeStr());
    }

    /**
     * deleteAt(list, position) - Deletes an item at a position from a list.
     */
    public static int deleteAt(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("deleteAt requires 2 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        int position = player.getDatum(args.get(1)).intValue();
        int index = position - 1;

        if (datum.isList()) {
            List<Integer> list = datum.toListMut();
            if (index >= 0 && index < list.size()) {
                list.remove(index);
            }
            return 0; // Void
        }

        if (datum.isPropList()) {
            List<int[]> propList = datum.toPropListMut();
            if (index >= 0 && index < propList.size()) {
                propList.remove(index);
            }
            return 0; // Void
        }

        throw new ScriptError("Cannot deleteAt on type: " + datum.typeStr());
    }

    /**
     * addAt(list, position, value) - Adds an item at a position in a list.
     */
    public static int addAt(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("addAt requires 3 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        int position = player.getDatum(args.get(1)).intValue();
        int index = position - 1;
        int valueRef = args.get(2);

        if (datum.isList()) {
            List<Integer> list = datum.toListMut();
            if (index < 0) index = 0;
            if (index > list.size()) index = list.size();
            list.add(index, valueRef);
            return 0; // Void
        }

        throw new ScriptError("Cannot addAt on type: " + datum.typeStr());
    }

    /**
     * addProp(propList, key, value) - Adds a property to a proplist.
     */
    public static int addProp(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 3) {
            throw new ScriptError("addProp requires 3 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        int keyRef = args.get(1);
        int valueRef = args.get(2);

        if (datum.isPropList()) {
            List<int[]> propList = datum.toPropListMut();
            propList.add(new int[] { keyRef, valueRef });
            return 0; // Void
        }

        throw new ScriptError("Cannot addProp on type: " + datum.typeStr());
    }

    /**
     * deleteProp(propList, key) - Deletes a property from a proplist.
     */
    public static int deleteProp(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("deleteProp requires 2 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        Datum key = player.getDatum(args.get(1));

        if (datum.isPropList()) {
            List<int[]> propList = datum.toPropListMut();
            String keyStr = key.isSymbol() ? key.symbolValue() : key.stringValue();

            for (int i = 0; i < propList.size(); i++) {
                Datum k = player.getDatum(propList.get(i)[0]);
                String kStr = k.isSymbol() ? k.symbolValue() : k.stringValue();
                if (kStr.equalsIgnoreCase(keyStr)) {
                    propList.remove(i);
                    break;
                }
            }
            return 0; // Void
        }

        throw new ScriptError("Cannot deleteProp on type: " + datum.typeStr());
    }

    /**
     * deleteOne(list, value) - Deletes the first occurrence of a value from a list.
     */
    public static int deleteOne(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("deleteOne requires 2 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        int valueRef = args.get(1);

        if (datum.isList()) {
            List<Integer> list = datum.toListMut();
            // Find and remove first occurrence
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(valueRef)) {
                    list.remove(i);
                    break;
                }
            }
            return 0; // Void
        }

        throw new ScriptError("Cannot deleteOne on type: " + datum.typeStr());
    }

    /**
     * findPos(list, value) - Finds the position of a value in a list.
     * Returns 0 if not found.
     */
    public static int findPos(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("findPos requires 2 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        int valueRef = args.get(1);
        Datum searchValue = player.getDatum(valueRef);

        if (datum.isList()) {
            List<Integer> list = datum.toList();
            for (int i = 0; i < list.size(); i++) {
                Datum item = player.getDatum(list.get(i));
                if (datumsEqual(item, searchValue, player)) {
                    return player.allocDatum(Datum.ofInt(i + 1)); // 1-based
                }
            }
            return player.allocDatum(Datum.ofInt(0)); // Not found
        }

        throw new ScriptError("Cannot findPos on type: " + datum.typeStr());
    }

    /**
     * findPosNear(list, value) - Finds the position where a value would be inserted in a sorted list.
     */
    public static int findPosNear(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("findPosNear requires 2 arguments");
        }

        Datum datum = player.getDatum(args.get(0));
        Datum searchValue = player.getDatum(args.get(1));

        if (datum.isList()) {
            List<Integer> list = datum.toList();
            for (int i = 0; i < list.size(); i++) {
                Datum item = player.getDatum(list.get(i));
                if (compareDatums(searchValue, item) <= 0) {
                    return player.allocDatum(Datum.ofInt(i + 1)); // 1-based
                }
            }
            return player.allocDatum(Datum.ofInt(list.size() + 1));
        }

        throw new ScriptError("Cannot findPosNear on type: " + datum.typeStr());
    }

    /**
     * getOne(list, value) - Returns the position of a value in a list (1-based), or 0 if not found.
     */
    public static int getOne(DirPlayer player, List<Integer> args) throws ScriptError {
        return findPos(player, args);
    }

    /**
     * getLast(list) - Returns the last item in a list.
     */
    public static int getLast(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));

        if (datum.isList()) {
            List<Integer> list = datum.toList();
            if (list.isEmpty()) {
                return 0; // Void
            }
            return list.get(list.size() - 1);
        }

        throw new ScriptError("Cannot getLast on type: " + datum.typeStr());
    }

    /**
     * Helper method to check if two datums are equal.
     */
    private static boolean datumsEqual(Datum a, Datum b, DirPlayer player) throws ScriptError {
        if (a.getType() != b.getType()) {
            return false;
        }
        if (a.isInt()) return a.intValue() == b.intValue();
        if (a.isFloat()) return a.floatValue() == b.floatValue();
        if (a.isString()) return a.stringValue().equals(b.stringValue());
        if (a.isSymbol()) return a.symbolValue().equalsIgnoreCase(b.symbolValue());
        return false;
    }
}
