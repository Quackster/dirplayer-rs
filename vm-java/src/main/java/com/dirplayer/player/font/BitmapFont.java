package com.dirplayer.player.font;

import com.dirplayer.player.bitmap.BitmapRef;

/**
 * Bitmap font for text rendering.
 * Port of Rust BitmapFont struct.
 */
public class BitmapFont {
    public int bitmapRef;
    public int charWidth;
    public int charHeight;
    public int gridColumns;
    public int gridRows;
    public int gridCellWidth;
    public int gridCellHeight;
    public int charOffsetX;
    public int charOffsetY;
    public int firstCharNum;
    public String fontName;
    public int fontSize;
    public int fontStyle;

    public BitmapFont() {
        this.bitmapRef = 0;
        this.charWidth = 8;
        this.charHeight = 12;
        this.gridColumns = 16;
        this.gridRows = 8;
        this.gridCellWidth = 8;
        this.gridCellHeight = 12;
        this.charOffsetX = 0;
        this.charOffsetY = 0;
        this.firstCharNum = 32;
        this.fontName = "";
        this.fontSize = 12;
        this.fontStyle = 0;
    }

    /**
     * Get the source rectangle for a character.
     */
    public int[] getCharRect(char c) {
        int charNum = c - firstCharNum;
        if (charNum < 0) {
            charNum = 0;
        }

        int col = charNum % gridColumns;
        int row = charNum / gridColumns;

        int x = col * gridCellWidth + charOffsetX;
        int y = row * gridCellHeight + charOffsetY;

        return new int[] { x, y, x + charWidth, y + charHeight };
    }

    /**
     * Calculate the width of text.
     */
    public int measureText(String text) {
        return text.length() * charWidth;
    }

    /**
     * Create font with basic parameters.
     */
    public static BitmapFont create(int bitmapRef, int charWidth, int charHeight,
                                     int gridColumns, int gridRows, String fontName) {
        BitmapFont font = new BitmapFont();
        font.bitmapRef = bitmapRef;
        font.charWidth = charWidth;
        font.charHeight = charHeight;
        font.gridColumns = gridColumns;
        font.gridRows = gridRows;
        font.gridCellWidth = charWidth;
        font.gridCellHeight = charHeight;
        font.fontName = fontName;
        return font;
    }
}
