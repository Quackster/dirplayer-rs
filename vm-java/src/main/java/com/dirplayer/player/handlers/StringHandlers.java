package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import java.util.List;

/**
 * String-related handler functions.
 * Port of Rust StringHandlers struct.
 */
public class StringHandlers {

    public static int space(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString(" "));
    }

    public static int offset(DirPlayer player, List<Integer> args) throws ScriptError {
        String strToFind = player.getDatum(args.get(0)).stringValue();
        String findIn = player.getDatum(args.get(1)).stringValue();

        // Lingo edge cases
        if (strToFind.isEmpty()) {
            return player.allocDatum(Datum.ofInt(1));
        }

        if (findIn.isEmpty()) {
            return player.allocDatum(Datum.ofInt(0));
        }

        // Case-insensitive search (like Mac Lingo)
        String findInLower = findIn.toLowerCase();
        String strToFindLower = strToFind.toLowerCase();

        int byteIndex = findInLower.indexOf(strToFindLower);
        int result;
        if (byteIndex >= 0) {
            // Count characters up to the found byte index
            int charIndex = findIn.substring(0, byteIndex).length();
            result = charIndex + 1; // 1-based indexing
        } else {
            result = 0; // Not found
        }

        return player.allocDatum(Datum.ofInt(result));
    }

    public static int length(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));

        if (obj.isString()) {
            return player.allocDatum(Datum.ofInt(obj.stringValue().length()));
        } else if (obj.isStringChunk()) {
            String s = obj.stringValue();
            return player.allocDatum(Datum.ofInt(s.length()));
        } else {
            throw new ScriptError("Cannot get length of non-string");
        }
    }

    public static int string(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum obj = player.getDatum(args.get(0));

        String result;
        if (obj.isString()) {
            result = obj.stringValue();
        } else if (obj.isVoid()) {
            result = "";
        } else {
            result = player.formatDatum(obj);
        }

        return player.allocDatum(Datum.ofString(result));
    }

    public static int chars(DirPlayer player, List<Integer> args) throws ScriptError {
        String string = player.getDatum(args.get(0)).stringValue();
        int start = Math.max(0, player.getDatum(args.get(1)).intValue() - 1);
        int end = player.getDatum(args.get(2)).intValue();

        int len = string.length();
        end = Math.min(end, len);

        if (start >= len || end < start + 1) {
            return player.allocDatum(Datum.ofString(""));
        }

        String substr = string.substring(start, end);
        return player.allocDatum(Datum.ofString(substr));
    }

    public static int charToNum(DirPlayer player, List<Integer> args) throws ScriptError {
        String strValue = player.getDatum(args.get(0)).stringValue();

        int byteVal = 0;
        if (!strValue.isEmpty()) {
            byteVal = strValue.charAt(0) & 0xFF;
        }

        return player.allocDatum(Datum.ofInt(byteVal));
    }

    public static int numToChar(DirPlayer player, List<Integer> args) throws ScriptError {
        int num = player.getDatum(args.get(0)).intValue();
        int byteVal = num & 0xFF;

        // Build a single character string
        String resultString = String.valueOf((char) byteVal);

        return player.allocDatum(Datum.ofString(resultString));
    }

    public static int tab(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString("\t"));
    }

    public static int returnn(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString("\r"));
    }

    public static int enter(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString("\n"));
    }

    public static int quote(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString("\""));
    }

    public static int backslash(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString("\\"));
    }

    public static int empty(DirPlayer player, List<Integer> args) throws ScriptError {
        return player.allocDatum(Datum.ofString(""));
    }
}
