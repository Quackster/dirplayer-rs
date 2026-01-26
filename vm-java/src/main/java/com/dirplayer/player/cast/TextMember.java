package com.dirplayer.player.cast;

import java.util.ArrayList;
import java.util.List;

/**
 * Text cast member data.
 * Port of Rust TextMember struct.
 */
public class TextMember {
    public String text;
    public String alignment;
    public String boxType;
    public boolean wordWrap;
    public boolean antiAlias;
    public String font;
    public List<String> fontStyle;
    public int fontSize;
    public int fixedLineSpace;
    public int topSpacing;
    public int width;
    public List<StyledSpan> htmlStyledSpans;

    public TextMember() {
        this.text = "";
        this.alignment = "left";
        this.boxType = "adjust";
        this.wordWrap = true;
        this.antiAlias = false;
        this.font = "Arial";
        this.fontStyle = new ArrayList<>();
        this.fontStyle.add("plain");
        this.fontSize = 12;
        this.fixedLineSpace = 0;
        this.topSpacing = 0;
        this.width = 100;
        this.htmlStyledSpans = new ArrayList<>();
    }

    public boolean hasHtmlStyling() {
        return !htmlStyledSpans.isEmpty();
    }

    public String getTextContent() {
        return text;
    }

    public TextMember copy() {
        TextMember copy = new TextMember();
        copy.text = text;
        copy.alignment = alignment;
        copy.boxType = boxType;
        copy.wordWrap = wordWrap;
        copy.antiAlias = antiAlias;
        copy.font = font;
        copy.fontStyle = new ArrayList<>(fontStyle);
        copy.fontSize = fontSize;
        copy.fixedLineSpace = fixedLineSpace;
        copy.topSpacing = topSpacing;
        copy.width = width;
        copy.htmlStyledSpans = new ArrayList<>(htmlStyledSpans);
        return copy;
    }

    // Getter methods for rendering
    public String getFont() { return font; }
    public Integer getFontSize() { return fontSize; }
    public String getText() { return text; }
    public int getFixedLineSpace() { return fixedLineSpace; }
    public int getTopSpacing() { return topSpacing; }

    /**
     * Styled text span for HTML-style text formatting.
     */
    public static class StyledSpan {
        public int start;
        public int end;
        public String fontName;
        public int fontSize;
        public List<String> fontStyles;
        public int[] color;  // RGB

        public StyledSpan() {
            this.start = 0;
            this.end = 0;
            this.fontName = "Arial";
            this.fontSize = 12;
            this.fontStyles = new ArrayList<>();
            this.color = new int[]{0, 0, 0};
        }

        public StyledSpan copy() {
            StyledSpan copy = new StyledSpan();
            copy.start = start;
            copy.end = end;
            copy.fontName = fontName;
            copy.fontSize = fontSize;
            copy.fontStyles = new ArrayList<>(fontStyles);
            copy.color = color.clone();
            return copy;
        }
    }
}
