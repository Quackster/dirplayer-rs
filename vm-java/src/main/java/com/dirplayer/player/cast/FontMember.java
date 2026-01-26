package com.dirplayer.player.cast;

import com.dirplayer.director.FontInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Font cast member data.
 * Port of Rust FontMember struct.
 */
public class FontMember {
    public FontInfo fontInfo;
    public String previewText;
    public String previewFontName;
    public List<StyledSpan> previewHtmlSpans;
    public int fixedLineSpace;
    public int topSpacing;
    public Integer bitmapRef;  // For bitmap fonts
    public Integer charWidth;
    public Integer charHeight;
    public Integer gridColumns;
    public Integer gridRows;
    public TextAlignment alignment;

    public FontMember() {
        this.fontInfo = new FontInfo();
        this.previewText = "";
        this.previewFontName = null;
        this.previewHtmlSpans = new ArrayList<>();
        this.fixedLineSpace = 0;
        this.topSpacing = 0;
        this.bitmapRef = null;
        this.charWidth = null;
        this.charHeight = null;
        this.gridColumns = null;
        this.gridRows = null;
        this.alignment = TextAlignment.LEFT;
    }

    public FontInfo getInfo() {
        return fontInfo;
    }

    public boolean isBitmapFont() {
        return bitmapRef != null;
    }

    public String getFontName() {
        return fontInfo != null ? fontInfo.name : "Arial";
    }

    public FontMember copy() {
        FontMember copy = new FontMember();
        copy.fontInfo = fontInfo;
        copy.previewText = previewText;
        copy.previewFontName = previewFontName;
        copy.previewHtmlSpans = new ArrayList<>(previewHtmlSpans);
        copy.fixedLineSpace = fixedLineSpace;
        copy.topSpacing = topSpacing;
        copy.bitmapRef = bitmapRef;
        copy.charWidth = charWidth;
        copy.charHeight = charHeight;
        copy.gridColumns = gridColumns;
        copy.gridRows = gridRows;
        copy.alignment = alignment;
        return copy;
    }

    /**
     * Text alignment enum.
     */
    public enum TextAlignment {
        LEFT("left"),
        CENTER("center"),
        RIGHT("right"),
        FULL("full");

        private final String value;

        TextAlignment(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static TextAlignment fromString(String s) {
            if (s == null) return LEFT;
            switch (s.toLowerCase()) {
                case "center": return CENTER;
                case "right": return RIGHT;
                case "full": return FULL;
                default: return LEFT;
            }
        }
    }
}
