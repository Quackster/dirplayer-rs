package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for string datum operations.
 * Port of Rust StringDatumHandlers and StringDatumUtils.
 */
public class StringDatumHandlers {

    /**
     * Get a built-in property from a string.
     */
    public static Datum getBuiltInProp(String value, String propName) throws ScriptError {
        switch (propName.toLowerCase()) {
            case "length":
                return Datum.ofInt(value.length());
            case "ilk":
                return Datum.ofSymbol("string");
            case "string":
                return Datum.ofString(value);
            default:
                throw new ScriptError("Invalid string built-in property " + propName);
        }
    }

    /**
     * Get a property from a string datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String propName) throws ScriptError {
        String value = player.getDatum(datumRef).stringValue();
        Datum result = getBuiltInProp(value, propName);
        return player.allocDatum(result);
    }

    /**
     * Count chunks in string.
     */
    public static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String value = player.getDatum(datumRef).stringValue();
        String operand = player.getDatum(args.get(0)).stringValue();
        char delimiter = player.getItemDelimiter();
        int count = stringGetCount(value, operand, delimiter);
        return player.allocDatum(Datum.ofInt(count));
    }

    /**
     * Get chunk property reference.
     */
    public static int getChunkPropRef(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String propName = player.getDatum(args.get(0)).stringValue();
        int start = player.getDatum(args.get(1)).intValue();
        int end = args.size() > 2 ? player.getDatum(args.get(2)).intValue() : start;

        StringChunkType chunkType = StringChunkType.fromString(propName);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, start, end, player.getItemDelimiter());

        String strValue = player.getDatum(datumRef).stringValue();
        String resolvedStr = StringChunkHandlers.resolveChunkExprString(strValue, chunkExpr);

        return player.allocDatum(Datum.ofStringChunk(datumRef, chunkExpr, resolvedStr));
    }

    /**
     * Get chunk property value.
     */
    public static int getChunkProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String propName = player.getDatum(args.get(0)).stringValue();
        int start = player.getDatum(args.get(1)).intValue();
        int end = args.size() > 2 ? player.getDatum(args.get(2)).intValue() : start;

        StringChunkType chunkType = StringChunkType.fromString(propName);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, start, end, player.getItemDelimiter());

        String strValue = player.getDatum(datumRef).stringValue();
        String resolvedStr = StringChunkHandlers.resolveChunkExprString(strValue, chunkExpr);

        return player.allocDatum(Datum.ofString(resolvedStr));
    }

    /**
     * Split string by delimiter.
     */
    public static int split(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String value = player.getDatum(datumRef).stringValue();

        String delimiter = "&";
        if (!args.isEmpty()) {
            delimiter = player.getDatum(args.get(0)).stringValue();
        }

        String[] parts = value.split(java.util.regex.Pattern.quote(delimiter), -1);
        List<Integer> partRefs = new ArrayList<>();
        for (String part : parts) {
            partRefs.add(player.allocDatum(Datum.ofString(part)));
        }

        return player.allocDatum(Datum.ofList(DatumType.String, partRefs, false));
    }

    /**
     * Call handler on string datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "count":
                return count(player, datumRef, args);
            case "getpropref":
                return getChunkPropRef(player, datumRef, args);
            case "getprop":
                return getChunkProp(player, datumRef, args);
            case "split":
                return split(player, datumRef, args);
            case "setat":
                // setAt on strings is not supported - throw error
                // This prevents infinite loops when scripts incorrectly try to use strings as lists
                throw new ScriptError("Cannot setAt on string (expected list, proplist, point, or rect)");
            default:
                throw new ScriptError("No handler " + handlerName + " for string datum");
        }
    }

    // Helper methods for string chunk operations

    /**
     * Count chunks of a specific type.
     */
    public static int stringGetCount(String value, String operand, char delimiter) throws ScriptError {
        switch (operand.toLowerCase()) {
            case "char":
            case "chars":
                return value.length();
            case "item":
            case "items":
                return getItems(value, delimiter).size();
            case "word":
            case "words":
                if (value.isEmpty()) {
                    return 0;
                }
                return value.trim().split("\\s+").length;
            case "line":
            case "lines":
                return getLines(value).size();
            default:
                throw new ScriptError("Invalid operand " + operand + " for string_get_count");
        }
    }

    /**
     * Split string into items by delimiter.
     */
    public static List<String> getItems(String value, char delimiter) {
        List<String> items = new ArrayList<>();
        if (delimiter == '\r' || delimiter == '\n') {
            return getLines(value);
        }

        String delimStr = String.valueOf(delimiter);
        String[] parts = value.split(java.util.regex.Pattern.quote(delimStr), -1);
        for (String part : parts) {
            items.add(part);
        }
        return items;
    }

    /**
     * Split string into words.
     */
    public static List<String> getWords(String value) {
        List<String> words = new ArrayList<>();
        String[] parts = value.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part);
            }
        }
        return words;
    }

    /**
     * Split string into lines.
     */
    public static List<String> getLines(String value) {
        List<String> lines = new ArrayList<>();
        String lineBreak;
        if (value.contains("\r\n")) {
            lineBreak = "\r\n";
        } else if (value.contains("\n")) {
            lineBreak = "\n";
        } else {
            lineBreak = "\r";
        }

        String[] parts = value.split(java.util.regex.Pattern.quote(lineBreak), -1);
        for (String part : parts) {
            lines.add(part);
        }
        return lines;
    }
}
