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
        public int charHeight;
        public int[] bitmapData;
        public int bitmapWidth;
        public int bitmapHeight;
        public int charsPerRow;

        public BitmapFont(String name, int size) {
            this.name = name;
            this.size = size;
            this.charWidths = new int[256];
            this.charHeight = size;
        }

        public int getCharWidth(char c) {
            int index = c & 0xFF;
            return charWidths[index];
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
