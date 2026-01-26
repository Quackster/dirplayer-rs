package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for string chunk datum operations.
 * Port of Rust StringChunkHandlers and StringChunkUtils.
 */
public class StringChunkHandlers {

    /**
     * Count chunks in string chunk.
     */
    public static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String value = player.getDatum(datumRef).stringValue();
        String operand = player.getDatum(args.get(0)).stringValue();
        char delimiter = player.getItemDelimiter();

        StringChunkType chunkType = StringChunkType.fromString(operand);
        int count = resolveChunkCount(value, chunkType, delimiter);
        return player.allocDatum(Datum.ofInt(count));
    }

    /**
     * Get chunk property.
     */
    public static int getProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        String strValue = player.getDatum(datumRef).stringValue();
        String propName = player.getDatum(args.get(0)).stringValue();
        int start = player.getDatum(args.get(1)).intValue();
        int end = args.size() > 2 ? player.getDatum(args.get(2)).intValue() : start;

        StringChunkType chunkType = StringChunkType.fromString(propName);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, start, end, player.getItemDelimiter());

        String resolvedStr = resolveChunkExprString(strValue, chunkExpr);
        return player.allocDatum(Datum.ofString(resolvedStr));
    }

    /**
     * Set chunk property (font, fontStyle, color).
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "font":
            case "fontstyle":
            case "color":
                // TODO: Implement text styling
                break;
            default:
                throw new ScriptError("Cannot set property " + prop + " for string chunk datum");
        }
    }

    /**
     * Delete chunk from source string.
     */
    public static int delete(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (!datum.isStringChunk()) {
            throw new ScriptError("Expected string chunk datum");
        }

        StringChunkExpr chunkExpr = datum.getStringChunkExpr();
        int sourceRef = datum.getStringChunkSourceRef();
        String originalStr = player.getDatum(sourceRef).stringValue();

        String newString = stringByDeletingChunk(originalStr, chunkExpr);
        player.getDatum(sourceRef).setStringValue(newString);

        return 0; // Void
    }

    /**
     * Set contents of chunk.
     */
    public static int setContents(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (!datum.isStringChunk()) {
            throw new ScriptError("Expected string chunk datum");
        }

        StringChunkExpr chunkExpr = datum.getStringChunkExpr();
        int sourceRef = datum.getStringChunkSourceRef();
        String originalStr = player.getDatum(sourceRef).stringValue();
        String newStr = player.getDatum(args.get(0)).stringValue();

        String result = stringBySettingChunk(originalStr, chunkExpr, newStr);
        player.getDatum(sourceRef).setStringValue(result);

        return 0; // Void
    }

    /**
     * Call handler on string chunk datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "count":
                return count(player, datumRef, args);
            case "getprop":
                return getProp(player, datumRef, args);
            case "delete":
                return delete(player, datumRef, args);
            case "setcontents":
                return setContents(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for string chunk datum");
        }
    }

    // String chunk utility methods

    /**
     * Resolve chunk expression to string.
     */
    public static String resolveChunkExprString(String str, StringChunkExpr chunkExpr) throws ScriptError {
        if (str.isEmpty()) {
            return "";
        }

        switch (chunkExpr.getChunkType()) {
            case Item: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return "";
                }
                return joinChunks(chunkList.subList(range[0], range[1]), String.valueOf(chunkExpr.getItemDelimiter()));
            }
            case Word: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return "";
                }
                return joinChunks(chunkList.subList(range[0], range[1]), " ");
            }
            case Char: {
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
                return str.substring(range[0], range[1]);
            }
            case Line: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return "";
                }
                return joinChunks(chunkList.subList(range[0], range[1]), "\r\n");
            }
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Resolve chunk list from string.
     */
    public static List<String> resolveChunkList(String str, StringChunkType chunkType, char itemDelimiter) throws ScriptError {
        switch (chunkType) {
            case Item:
                return StringDatumHandlers.getItems(str, itemDelimiter);
            case Word:
                return StringDatumHandlers.getWords(str);
            case Char: {
                List<String> chars = new ArrayList<>();
                for (char c : str.toCharArray()) {
                    chars.add(String.valueOf(c));
                }
                return chars;
            }
            case Line:
                return StringDatumHandlers.getLines(str);
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Resolve last chunk of type.
     */
    public static String resolveLastChunk(String str, StringChunkType chunkType, char itemDelimiter) throws ScriptError {
        List<String> chunks = resolveChunkList(str, chunkType, itemDelimiter);
        if (chunks.isEmpty()) {
            return "";
        }
        return chunks.get(chunks.size() - 1);
    }

    /**
     * Count chunks of type in string.
     */
    public static int resolveChunkCount(String str, StringChunkType chunkType, char itemDelimiter) throws ScriptError {
        switch (chunkType) {
            case Item: {
                int count = 0;
                for (char c : str.toCharArray()) {
                    if (c == itemDelimiter) {
                        count++;
                    }
                }
                return count + 1;
            }
            case Word:
                return str.trim().isEmpty() ? 0 : str.trim().split("\\s+").length;
            case Char:
                return str.length();
            case Line:
                return StringDatumHandlers.getLines(str).size();
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Delete chunk from string.
     */
    public static String stringByDeletingChunk(String str, StringChunkExpr chunkExpr) throws ScriptError {
        switch (chunkExpr.getChunkType()) {
            case Char: {
                StringBuilder sb = new StringBuilder(str);
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
                sb.delete(range[0], range[1]);
                return sb.toString();
            }
            case Item:
            case Word:
            case Line: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return str;
                }

                List<String> newChunks = new ArrayList<>();
                for (int i = 0; i < chunkList.size(); i++) {
                    if (i < range[0] || i >= range[1]) {
                        newChunks.add(chunkList.get(i));
                    }
                }

                String delimiter = getDelimiterForChunkType(chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                return joinChunks(newChunks, delimiter);
            }
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Set chunk value in string.
     */
    public static String stringBySettingChunk(String str, StringChunkExpr chunkExpr, String replaceWith) throws ScriptError {
        if (chunkExpr.getChunkType() == StringChunkType.Char) {
            StringBuilder sb = new StringBuilder(str);
            int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
            sb.replace(range[0], range[1], replaceWith);
            return sb.toString();
        } else {
            throw new ScriptError("Only char chunk type is supported for string by setting chunk");
        }
    }

    /**
     * Put value into chunk (replace).
     */
    public static String stringByPuttingIntoChunk(String str, StringChunkExpr chunkExpr, String replaceWith) throws ScriptError {
        switch (chunkExpr.getChunkType()) {
            case Char: {
                StringBuilder sb = new StringBuilder(str);
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
                sb.replace(range[0], range[1], replaceWith);
                return sb.toString();
            }
            case Item:
            case Word:
            case Line: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return str;
                }

                List<String> newChunks = new ArrayList<>();
                for (int i = 0; i < range[0]; i++) {
                    newChunks.add(chunkList.get(i));
                }
                newChunks.add(replaceWith);
                for (int i = range[1]; i < chunkList.size(); i++) {
                    newChunks.add(chunkList.get(i));
                }

                String delimiter = getDelimiterForChunkType(chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                return joinChunks(newChunks, delimiter);
            }
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Put value before chunk.
     */
    public static String stringByPuttingBeforeChunk(String str, StringChunkExpr chunkExpr, String insertValue) throws ScriptError {
        switch (chunkExpr.getChunkType()) {
            case Char: {
                StringBuilder sb = new StringBuilder(str);
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
                sb.insert(range[0], insertValue);
                return sb.toString();
            }
            case Item:
            case Word:
            case Line: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return insertValue;
                }

                List<String> newChunks = new ArrayList<>(chunkList);
                if (range[0] < newChunks.size()) {
                    newChunks.set(range[0], insertValue + newChunks.get(range[0]));
                } else if (!newChunks.isEmpty()) {
                    int lastIdx = newChunks.size() - 1;
                    newChunks.set(lastIdx, newChunks.get(lastIdx) + insertValue);
                }

                String delimiter = getDelimiterForChunkType(chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                return joinChunks(newChunks, delimiter);
            }
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    /**
     * Put value after chunk.
     */
    public static String stringByPuttingAfterChunk(String str, StringChunkExpr chunkExpr, String insertValue) throws ScriptError {
        switch (chunkExpr.getChunkType()) {
            case Char: {
                StringBuilder sb = new StringBuilder(str);
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), str.length());
                sb.insert(range[1], insertValue);
                return sb.toString();
            }
            case Item:
            case Word:
            case Line: {
                List<String> chunkList = resolveChunkList(str, chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                int[] range = vmRangeToHost(chunkExpr.getStart(), chunkExpr.getEnd(), chunkList.size());
                if (chunkList.isEmpty()) {
                    return insertValue;
                }

                List<String> newChunks = new ArrayList<>(chunkList);
                if (range[1] > 0 && range[1] <= newChunks.size()) {
                    int idx = range[1] - 1;
                    newChunks.set(idx, newChunks.get(idx) + insertValue);
                } else if (!newChunks.isEmpty()) {
                    int lastIdx = newChunks.size() - 1;
                    newChunks.set(lastIdx, newChunks.get(lastIdx) + insertValue);
                }

                String delimiter = getDelimiterForChunkType(chunkExpr.getChunkType(), chunkExpr.getItemDelimiter());
                return joinChunks(newChunks, delimiter);
            }
            default:
                throw new ScriptError("Unknown chunk type");
        }
    }

    // Helper methods

    /**
     * Convert VM range (1-based) to host range (0-based, exclusive end).
     */
    private static int[] vmRangeToHost(int start, int end, int maxLength) {
        int startIndex = Math.max(0, start - 1);
        int endIndex;
        if (end == 0) {
            endIndex = startIndex + 1;
        } else if (end == -1 || end > maxLength) {
            endIndex = maxLength;
        } else {
            endIndex = end;
        }
        startIndex = Math.min(Math.max(startIndex, 0), maxLength);
        endIndex = Math.max(startIndex, Math.min(endIndex, maxLength));
        return new int[] { startIndex, endIndex };
    }

    private static String getDelimiterForChunkType(StringChunkType chunkType, char itemDelimiter) {
        switch (chunkType) {
            case Item:
                return String.valueOf(itemDelimiter);
            case Word:
                return " ";
            case Line:
                return "\r\n";
            default:
                return "";
        }
    }

    private static String joinChunks(List<String> chunks, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(chunks.get(i));
        }
        return sb.toString();
    }
}
