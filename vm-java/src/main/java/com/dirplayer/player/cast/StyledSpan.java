package com.dirplayer.player.cast;

import java.util.ArrayList;
import java.util.List;

/**
 * Styled text span for rich text formatting.
 * Port of Rust StyledSpan struct.
 */
public class StyledSpan {
    public int startIndex;
    public int endIndex;
    public String fontName;
    public int fontSize;
    public List<String> fontStyles;
    public int colorR;
    public int colorG;
    public int colorB;
    public boolean bold;
    public boolean italic;
    public boolean underline;
    public String hyperlink;

    public StyledSpan() {
        this.startIndex = 0;
        this.endIndex = 0;
        this.fontName = "Arial";
        this.fontSize = 12;
        this.fontStyles = new ArrayList<>();
        this.colorR = 0;
        this.colorG = 0;
        this.colorB = 0;
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.hyperlink = null;
    }

    public StyledSpan(int start, int end, String font, int size) {
        this();
        this.startIndex = start;
        this.endIndex = end;
        this.fontName = font;
        this.fontSize = size;
    }

    public void setColor(int r, int g, int b) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
    }

    public int[] getColor() {
        return new int[]{colorR, colorG, colorB};
    }

    public boolean hasStyle(String style) {
        return fontStyles.contains(style.toLowerCase());
    }

    public void addStyle(String style) {
        String lower = style.toLowerCase();
        if (!fontStyles.contains(lower)) {
            fontStyles.add(lower);
        }
        // Update convenience flags
        if ("bold".equals(lower)) bold = true;
        if ("italic".equals(lower)) italic = true;
        if ("underline".equals(lower)) underline = true;
    }

    public StyledSpan copy() {
        StyledSpan copy = new StyledSpan();
        copy.startIndex = startIndex;
        copy.endIndex = endIndex;
        copy.fontName = fontName;
        copy.fontSize = fontSize;
        copy.fontStyles = new ArrayList<>(fontStyles);
        copy.colorR = colorR;
        copy.colorG = colorG;
        copy.colorB = colorB;
        copy.bold = bold;
        copy.italic = italic;
        copy.underline = underline;
        copy.hyperlink = hyperlink;
        return copy;
    }
}
