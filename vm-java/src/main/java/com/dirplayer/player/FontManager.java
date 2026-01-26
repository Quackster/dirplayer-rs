package com.dirplayer.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for fonts used in Director movies.
 * Port of Rust FontManager struct.
 */
public class FontManager {
    public Map<String, BitmapFont> fonts;
    public BitmapFont systemFont;

    public FontManager() {
        this.fonts = new HashMap<>();
        this.systemFont = null;
    }

    public void addFont(String name, BitmapFont font) {
        fonts.put(name.toLowerCase(), font);
    }

    public BitmapFont getFont(String name) {
        return fonts.get(name.toLowerCase());
    }

    public BitmapFont getSystemFont() {
        return systemFont;
    }

    public void setSystemFont(BitmapFont font) {
        this.systemFont = font;
    }

    public static class BitmapFont {
        public String name;
        public int size;
        public int[] charWidths;
        public int charWidth;   // Default char width (for fixed-width fonts)
        public int charHeight;
        public int[] bitmapData;
        public int bitmapWidth;
        public int bitmapHeight;
        public int charsPerRow;
        public int bitmapRef;   // Reference to the font bitmap in BitmapManager
        public int firstChar;   // First character code in the font
        public int lastChar;    // Last character code in the font

        public BitmapFont(String name, int size) {
            this.name = name;
            this.size = size;
            this.charWidths = new int[256];
            this.charWidth = size;  // Default to size for fixed-width
            this.charHeight = size;
            this.firstChar = 32;    // Space
            this.lastChar = 127;    // DEL (end of ASCII printable range)
            this.bitmapRef = -1;
        }

        public int getCharWidth(char c) {
            int index = c & 0xFF;
            if (charWidths[index] > 0) {
                return charWidths[index];
            }
            return charWidth;  // Fall back to default width
        }

        public int measureString(String text) {
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                width += getCharWidth(text.charAt(i));
            }
            return width;
        }
    }
}
