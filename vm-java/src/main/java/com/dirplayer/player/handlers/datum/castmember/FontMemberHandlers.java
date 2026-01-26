package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.FontMember;
import com.dirplayer.player.cast.StyledSpan;
import com.dirplayer.player.cast.TextMember;
import com.dirplayer.player.handlers.datum.StringChunkHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handlers for font cast member datum operations.
 * Also handles Text member type properties that overlap with Font.
 * Port of Rust FontMemberHandlers.
 */
public class FontMemberHandlers {

    /**
     * Call a handler on a font/text cast member.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        // Get text content based on member type
        String text = getTextContent(member);
        if (text == null) {
            throw new ScriptError("Member type does not support text operations");
        }

        switch (handlerName.toLowerCase()) {
            case "count": {
                if (args.size() != 1) {
                    throw new ScriptError("count requires 1 argument");
                }
                String countOf = player.getDatum(args.get(0)).stringValue();
                char delimiter = player.getItemDelimiter();
                StringChunkType chunkType = StringChunkType.fromString(countOf);
                int count = StringChunkHandlers.resolveChunkCount(text, chunkType, delimiter);
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
                String resolvedStr = StringChunkHandlers.resolveChunkExprString(text, chunkExpr);

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

                // Simplified calculation - would need proper font metrics
                int fontSize = getFontSize(member);
                int charWidth = fontSize / 2; // Approximate
                int lineHeight = fontSize + 4;

                int charPos = 1;
                if (charWidth > 0 && lineHeight > 0) {
                    int line = y / lineHeight;
                    int col = x / charWidth;
                    // Find position in text
                    String[] lines = text.split("\n");
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
     * Get a property from a font cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        // Check if it's a TextMember
        if (member.specificData instanceof TextMember) {
            return getTextMemberProp(player, (TextMember) member.specificData, prop);
        }

        // Check if it's a FontMember
        if (member.specificData instanceof FontMember) {
            return getFontMemberProp(player, (FontMember) member.specificData, prop);
        }

        throw new ScriptError("Cannot get castMember property " + prop + " for this member type");
    }

    /**
     * Set a property on a font cast member.
     */
    public static void setProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        Datum value = player.getDatum(valueRef);

        // Check if it's a FontMember
        if (member.specificData instanceof FontMember) {
            setFontMemberProp(player, (FontMember) member.specificData, prop, value);
            return;
        }

        throw new ScriptError("Cannot set castMember prop '" + prop + "' for non-Font member");
    }

    // Text member property getters

    private static int getTextMemberProp(DirPlayer player, TextMember textData, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "text":
                return player.allocDatum(Datum.ofString(textData.text));

            case "alignment":
                return player.allocDatum(Datum.ofString(textData.alignment));

            case "wordwrap":
                return player.allocDatum(Datum.ofBool(textData.wordWrap));

            case "width":
                return player.allocDatum(Datum.ofInt(textData.width));

            case "font":
                return player.allocDatum(Datum.ofString(textData.font));

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
                return player.allocDatum(Datum.ofSymbol(textData.boxType));

            case "antialias":
                return player.allocDatum(Datum.ofBool(textData.antiAlias));

            case "rect":
            case "height":
            case "image": {
                // Calculate dimensions
                int width = textData.width > 0 ? textData.width : 100;
                int height = calculateTextHeight(textData);

                switch (prop.toLowerCase()) {
                    case "rect": {
                        int leftRef = player.allocDatum(Datum.ofInt(0));
                        int topRef = player.allocDatum(Datum.ofInt(0));
                        int rightRef = player.allocDatum(Datum.ofInt(width));
                        int bottomRef = player.allocDatum(Datum.ofInt(height));
                        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
                    }
                    case "height":
                        return player.allocDatum(Datum.ofInt(height));
                    case "image":
                        // TODO: Generate bitmap from text
                        return 0; // Void
                    default:
                        throw new ScriptError("Unexpected property: " + prop);
                }
            }

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for Text member");
        }
    }

    // Font member property getters

    private static int getFontMemberProp(DirPlayer player, FontMember fontData, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "text":
                return player.allocDatum(Datum.ofString(fontData.previewText));

            case "previewtext":
                return player.allocDatum(Datum.ofString(fontData.previewText));

            case "previewhtml": {
                StringBuilder htmlString = new StringBuilder();
                if (fontData.previewHtmlSpans != null) {
                    for (StyledSpan span : fontData.previewHtmlSpans) {
                        // Convert span back to text (simplified)
                        htmlString.append(span.fontName != null ? span.fontName : "");
                    }
                }
                return player.allocDatum(Datum.ofString(htmlString.toString()));
            }

            case "fontstyle":
                return player.allocDatum(Datum.ofList(DatumType.List, new ArrayList<>(), false));

            case "name":
                return player.allocDatum(Datum.ofString(
                    fontData.fontInfo != null ? fontData.fontInfo.name : ""));

            case "size":
                return player.allocDatum(Datum.ofInt(
                    fontData.fontInfo != null ? fontData.fontInfo.size : 12));

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for Font member");
        }
    }

    // Font member property setters

    private static void setFontMemberProp(DirPlayer player, FontMember fontMember, String prop, Datum value) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "text":
                fontMember.previewText = value.stringValue();
                break;

            case "html": {
                String htmlString = value.stringValue();
                List<StyledSpan> spans = parseHtml(htmlString);
                StringBuilder plainText = new StringBuilder();
                for (StyledSpan span : spans) {
                    // Extract plain text from spans
                    if (span.fontName != null) {
                        plainText.append(span.fontName);
                    }
                }
                fontMember.previewText = plainText.toString();
                fontMember.previewHtmlSpans = spans;
                break;
            }

            case "fixedlinespace":
                fontMember.fixedLineSpace = value.intValue();
                break;

            case "alignment":
                fontMember.alignment = FontMember.TextAlignment.fromString(value.stringValue());
                break;

            default:
                throw new ScriptError("Cannot set castMember prop '" + prop + "' for Font member");
        }
    }

    // Helper methods

    private static String getTextContent(com.dirplayer.player.CastMember member) {
        if (member.specificData instanceof TextMember) {
            return ((TextMember) member.specificData).text;
        }
        if (member.specificData instanceof FontMember) {
            return ((FontMember) member.specificData).previewText;
        }
        return null;
    }

    private static int getFontSize(com.dirplayer.player.CastMember member) {
        if (member.specificData instanceof TextMember) {
            return ((TextMember) member.specificData).fontSize;
        }
        if (member.specificData instanceof FontMember) {
            FontMember font = (FontMember) member.specificData;
            return font.fontInfo != null ? font.fontInfo.size : 12;
        }
        return 12;
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

    /**
     * Parse HTML into styled spans (simplified parser).
     */
    public static List<StyledSpan> parseHtml(String html) {
        List<StyledSpan> spans = new ArrayList<>();
        HtmlStyle defaultStyle = new HtmlStyle();

        // Extract body style if present
        extractBodyStyle(html, defaultStyle);

        // Parse HTML recursively
        parseHtmlRecursive(html, spans, defaultStyle);

        return spans;
    }

    private static void extractBodyStyle(String html, HtmlStyle style) {
        String lower = html.toLowerCase();

        // Extract text color from body tag
        String textAttr = extractAttr(lower, "body", "text");
        if (textAttr != null) {
            Integer color = parseColor(textAttr);
            if (color != null) {
                style.color = color;
            }
        }

        // Extract background color
        String bgAttr = extractAttr(lower, "body", "bgcolor");
        if (bgAttr == null) {
            bgAttr = extractAttr(lower, "body", "bg");
        }
        if (bgAttr != null) {
            Integer color = parseColor(bgAttr);
            if (color != null) {
                style.bgColor = color;
            }
        }
    }

    private static String extractAttr(String html, String tag, String attr) {
        String tagStart = "<" + tag;
        int startIdx = html.indexOf(tagStart);
        if (startIdx >= 0) {
            int endIdx = html.indexOf('>', startIdx);
            if (endIdx > startIdx) {
                String tagContent = html.substring(startIdx, endIdx);
                String attrPattern = attr + "=";
                int attrIdx = tagContent.indexOf(attrPattern);
                if (attrIdx >= 0) {
                    String afterEq = tagContent.substring(attrIdx + attrPattern.length());
                    char quoteChar = afterEq.charAt(0);
                    if (quoteChar == '"' || quoteChar == '\'') {
                        int end = afterEq.indexOf(quoteChar, 1);
                        if (end > 0) {
                            return afterEq.substring(1, end);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void parseHtmlRecursive(String html, List<StyledSpan> spans, HtmlStyle currentStyle) {
        int pos = 0;
        List<HtmlStyle> styleStack = new ArrayList<>();
        styleStack.add(currentStyle);

        while (pos < html.length()) {
            if (html.charAt(pos) == '<') {
                int end = html.indexOf('>', pos);
                if (end > pos) {
                    String tag = html.substring(pos + 1, end);
                    String tagLower = tag.toLowerCase();

                    // Handle closing tags
                    if (tag.startsWith("/")) {
                        if (styleStack.size() > 1) {
                            styleStack.remove(styleStack.size() - 1);
                        }
                    } else {
                        // Handle opening tags
                        HtmlStyle newStyle = styleStack.get(styleStack.size() - 1).copy();
                        String tagName = tagLower.split("\\s+")[0];

                        switch (tagName) {
                            case "font":
                                String face = extractTagAttr(tag, "face");
                                if (face != null) newStyle.fontFace = face;
                                String size = extractTagAttr(tag, "size");
                                if (size != null) {
                                    try {
                                        newStyle.fontSize = Integer.parseInt(size);
                                    } catch (NumberFormatException ignored) {}
                                }
                                String color = extractTagAttr(tag, "color");
                                if (color != null) {
                                    Integer c = parseColor(color);
                                    if (c != null) newStyle.color = c;
                                }
                                break;
                            case "b":
                            case "strong":
                                newStyle.bold = true;
                                break;
                            case "i":
                            case "em":
                                newStyle.italic = true;
                                break;
                            case "u":
                                newStyle.underline = true;
                                break;
                            case "br":
                                StyledSpan brSpan = new StyledSpan();
                                brSpan.fontName = "\n";
                                spans.add(brSpan);
                                break;
                        }

                        if (!tag.endsWith("/") && !tagName.equals("br")) {
                            styleStack.add(newStyle);
                        }
                    }
                    pos = end + 1;
                    continue;
                }
            }

            // Collect text content
            StringBuilder text = new StringBuilder();
            while (pos < html.length() && html.charAt(pos) != '<') {
                text.append(html.charAt(pos));
                pos++;
            }

            String textContent = text.toString().trim();
            if (!textContent.isEmpty()) {
                StyledSpan span = new StyledSpan();
                span.fontName = textContent;
                HtmlStyle style = styleStack.get(styleStack.size() - 1);
                if (style.color != null) {
                    span.colorR = (style.color >> 16) & 0xFF;
                    span.colorG = (style.color >> 8) & 0xFF;
                    span.colorB = style.color & 0xFF;
                }
                span.bold = style.bold;
                span.italic = style.italic;
                span.underline = style.underline;
                spans.add(span);
            }
        }
    }

    private static String extractTagAttr(String tag, String attr) {
        String lower = tag.toLowerCase();
        String attrPattern = attr.toLowerCase() + "=";
        int idx = lower.indexOf(attrPattern);
        if (idx >= 0) {
            String afterEq = tag.substring(idx + attrPattern.length());
            char quoteChar = afterEq.charAt(0);
            if (quoteChar == '"' || quoteChar == '\'') {
                int end = afterEq.indexOf(quoteChar, 1);
                if (end > 0) {
                    return afterEq.substring(1, end);
                }
            } else {
                // Unquoted
                int end = afterEq.indexOf(' ');
                if (end < 0) end = afterEq.length();
                return afterEq.substring(0, end);
            }
        }
        return null;
    }

    /**
     * Parse a color string to RGB integer.
     */
    public static Integer parseColor(String colorStr) {
        if (colorStr == null) return null;
        String color = colorStr.trim().toLowerCase();

        if (color.startsWith("#")) {
            String hex = color.substring(1);
            if (hex.length() == 6) {
                try {
                    return Integer.parseInt(hex, 16);
                } catch (NumberFormatException ignored) {}
            }
        } else if (color.startsWith("0x")) {
            String hex = color.substring(2);
            if (hex.length() == 6) {
                try {
                    return Integer.parseInt(hex, 16);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Named colors
        Map<String, Integer> namedColors = new HashMap<>();
        namedColors.put("black", 0x000000);
        namedColors.put("white", 0xFFFFFF);
        namedColors.put("red", 0xFF0000);
        namedColors.put("green", 0x00FF00);
        namedColors.put("blue", 0x0000FF);
        namedColors.put("yellow", 0xFFFF00);
        namedColors.put("cyan", 0x00FFFF);
        namedColors.put("magenta", 0xFF00FF);
        namedColors.put("gray", 0x808080);
        namedColors.put("grey", 0x808080);
        namedColors.put("silver", 0xC0C0C0);
        namedColors.put("maroon", 0x800000);
        namedColors.put("olive", 0x808000);
        namedColors.put("lime", 0x00FF00);
        namedColors.put("aqua", 0x00FFFF);
        namedColors.put("teal", 0x008080);
        namedColors.put("navy", 0x000080);
        namedColors.put("purple", 0x800080);

        return namedColors.get(color);
    }

    /**
     * HTML style state for parsing.
     */
    public static class HtmlStyle {
        public String fontFace;
        public Integer fontSize;
        public Integer color;
        public Integer bgColor;
        public boolean bold;
        public boolean italic;
        public boolean underline;

        public HtmlStyle() {
            this.fontFace = null;
            this.fontSize = null;
            this.color = null;
            this.bgColor = null;
            this.bold = false;
            this.italic = false;
            this.underline = false;
        }

        public HtmlStyle copy() {
            HtmlStyle copy = new HtmlStyle();
            copy.fontFace = this.fontFace;
            copy.fontSize = this.fontSize;
            copy.color = this.color;
            copy.bgColor = this.bgColor;
            copy.bold = this.bold;
            copy.italic = this.italic;
            copy.underline = this.underline;
            return copy;
        }
    }

    /**
     * Text alignment enum.
     */
    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT;

        public static TextAlignment fromString(String s) {
            if (s == null) return LEFT;
            switch (s.toLowerCase()) {
                case "center": return CENTER;
                case "right": return RIGHT;
                default: return LEFT;
            }
        }
    }
}
