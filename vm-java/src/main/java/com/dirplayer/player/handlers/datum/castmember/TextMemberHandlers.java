package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.TextMember;
import com.dirplayer.player.handlers.datum.StringChunkHandlers;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for text cast member datum operations.
 * Port of Rust TextMemberHandlers.
 */
public class TextMemberHandlers {

    /**
     * Call a handler on a text cast member.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        TextMember textData = getTextMember(player, memberRef);
        if (textData == null) {
            throw new ScriptError("Invalid text member");
        }

        switch (handlerName.toLowerCase()) {
            case "count": {
                if (args.size() != 1) {
                    throw new ScriptError("count requires 1 argument");
                }
                String countOf = player.getDatum(args.get(0)).stringValue();
                char delimiter = player.getItemDelimiter();
                StringChunkType chunkType = StringChunkType.fromString(countOf);
                int count = StringChunkHandlers.resolveChunkCount(textData.text, chunkType, delimiter);
                return player.allocDatum(Datum.ofInt(count));
            }

            case "getpropref": {
                if (args.size() < 2) {
                    throw new ScriptError("getPropRef requires at least 2 arguments");
                }
                String propName = player.getDatum(args.get(0)).stringValue();
                int start = player.getDatum(args.get(1)).intValue();
                int end = args.size() > 2 ? player.getDatum(args.get(2)).intValue() : start;

                StringChunkType chunkType = StringChunkType.fromString(propName);
                StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, start, end, player.getItemDelimiter());
                String resolvedStr = StringChunkHandlers.resolveChunkExprString(textData.text, chunkExpr);

                return player.allocDatum(Datum.ofStringChunk(
                    player.allocDatum(Datum.ofCastMember(memberRef)),
                    chunkExpr,
                    resolvedStr
                ));
            }

            case "loctocharpos": {
                if (args.isEmpty()) {
                    throw new ScriptError("locToCharPos requires 1 argument");
                }
                int[] pointRefs = player.getDatum(args.get(0)).toPoint();
                int x = player.getDatum(pointRefs[0]).intValue();
                int y = player.getDatum(pointRefs[1]).intValue();

                // Simplified calculation
                int fontSize = textData.fontSize > 0 ? textData.fontSize : 12;
                int charWidth = fontSize / 2;
                int lineHeight = textData.fixedLineSpace > 0 ? textData.fixedLineSpace : fontSize + 4;

                int charPos = 1;
                if (charWidth > 0 && lineHeight > 0 && textData.text != null) {
                    int line = y / lineHeight;
                    int col = x / charWidth;
                    String[] lines = textData.text.split("\n");
                    for (int i = 0; i < Math.min(line, lines.length); i++) {
                        charPos += lines[i].length() + 1;
                    }
                    if (line < lines.length) {
                        charPos += Math.min(col, lines[line].length());
                    }
                }

                return player.allocDatum(Datum.ofInt(charPos));
            }

            default:
                throw new ScriptError("No handler " + handlerName + " for text member type");
        }
    }

    /**
     * Get a property from a text cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        TextMember textData = getTextMember(player, memberRef);
        if (textData == null) {
            throw new ScriptError("Invalid text member");
        }

        switch (prop.toLowerCase()) {
            case "text":
                return player.allocDatum(Datum.ofString(textData.text != null ? textData.text : ""));

            case "alignment":
                return player.allocDatum(Datum.ofString(textData.alignment != null ? textData.alignment : "left"));

            case "wordwrap":
                return player.allocDatum(Datum.ofBool(textData.wordWrap));

            case "width":
                return player.allocDatum(Datum.ofInt(textData.width));

            case "font":
                return player.allocDatum(Datum.ofString(textData.font != null ? textData.font : ""));

            case "fontsize":
                return player.allocDatum(Datum.ofInt(textData.fontSize));

            case "fontstyle": {
                List<Integer> styleRefs = new ArrayList<>();
                if (textData.fontStyle != null) {
                    for (String style : textData.fontStyle) {
                        styleRefs.add(player.allocDatum(Datum.ofSymbol(style)));
                    }
                }
                return player.allocDatum(Datum.ofList(DatumType.List, styleRefs, false));
            }

            case "fixedlinespace":
                return player.allocDatum(Datum.ofInt(textData.fixedLineSpace));

            case "topspacing":
                return player.allocDatum(Datum.ofInt(textData.topSpacing));

            case "boxtype":
                return player.allocDatum(Datum.ofSymbol(textData.boxType != null ? textData.boxType : "adjust"));

            case "antialias":
                return player.allocDatum(Datum.ofBool(textData.antiAlias));

            case "rect": {
                int width = textData.width > 0 ? textData.width : 100;
                int height = calculateTextHeight(textData);
                int leftRef = player.allocDatum(Datum.ofInt(0));
                int topRef = player.allocDatum(Datum.ofInt(0));
                int rightRef = player.allocDatum(Datum.ofInt(width));
                int bottomRef = player.allocDatum(Datum.ofInt(height));
                return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
            }

            case "height":
                return player.allocDatum(Datum.ofInt(calculateTextHeight(textData)));

            case "image":
                // TODO: Generate bitmap from text
                return 0; // Void

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for text");
        }
    }

    /**
     * Set a property on a text cast member.
     */
    public static void setProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);
        TextMember textData = getTextMemberMut(player, memberRef);
        if (textData == null) {
            throw new ScriptError("Invalid text member");
        }

        switch (prop.toLowerCase()) {
            case "text":
                textData.text = value.stringValue();
                break;

            case "alignment":
                textData.alignment = value.stringValue();
                break;

            case "wordwrap":
                textData.wordWrap = value.boolValue();
                break;

            case "width":
                textData.width = value.intValue();
                break;

            case "font":
                textData.font = value.stringValue();
                break;

            case "fontsize":
                textData.fontSize = value.intValue();
                break;

            case "fontstyle": {
                List<Integer> styleList = value.toList();
                List<String> styles = new ArrayList<>();
                for (int ref : styleList) {
                    styles.add(player.getDatum(ref).stringValue());
                }
                textData.fontStyle = styles;
                break;
            }

            case "fixedlinespace":
                textData.fixedLineSpace = value.intValue();
                break;

            case "topspacing":
                textData.topSpacing = value.intValue();
                break;

            case "boxtype":
                textData.boxType = value.stringValue();
                break;

            case "antialias":
                textData.antiAlias = value.boolValue();
                break;

            case "rect": {
                if (!value.isRect()) {
                    throw new ScriptError("rect must be a Rect");
                }
                int[] rectRefs = value.toRect();
                int x2 = player.getDatum(rectRefs[2]).intValue();
                textData.width = x2;
                break;
            }

            default:
                throw new ScriptError("Cannot set castMember prop " + prop + " for text");
        }
    }

    // Helper methods

    private static TextMember getTextMember(DirPlayer player, CastMemberRef memberRef) {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.specificData == null) {
            return null;
        }
        if (member.specificData instanceof TextMember) {
            return (TextMember) member.specificData;
        }
        return null;
    }

    private static TextMember getTextMemberMut(DirPlayer player, CastMemberRef memberRef) {
        return getTextMember(player, memberRef);
    }

    private static int calculateTextHeight(TextMember textData) {
        if (textData.text == null || textData.text.isEmpty()) {
            return textData.fontSize > 0 ? textData.fontSize : 12;
        }
        int lineCount = 1;
        for (char c : textData.text.toCharArray()) {
            if (c == '\n') {
                lineCount++;
            }
        }
        int lineHeight = textData.fixedLineSpace > 0 ? textData.fixedLineSpace
            : (textData.fontSize > 0 ? textData.fontSize + 4 : 16);
        return lineCount * lineHeight + textData.topSpacing;
    }
}
