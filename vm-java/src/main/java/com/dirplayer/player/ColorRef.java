package com.dirplayer.player;

import java.util.Objects;

/**
 * Reference to a color value.
 * Port of Rust ColorRef struct.
 */
public class ColorRef {
    public int red;
    public int green;
    public int blue;
    public int paletteIndex;
    public boolean isPaletteColor;

    public ColorRef() {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        this.paletteIndex = -1;
        this.isPaletteColor = false;
    }

    public ColorRef(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.paletteIndex = -1;
        this.isPaletteColor = false;
    }

    public static ColorRef fromRgb(int red, int green, int blue) {
        return new ColorRef(red, green, blue);
    }

    public static ColorRef ofRgb(int red, int green, int blue) {
        return new ColorRef(red, green, blue);
    }

    public static ColorRef fromPaletteIndex(int index) {
        ColorRef c = new ColorRef();
        c.paletteIndex = index;
        c.isPaletteColor = true;
        return c;
    }

    public static ColorRef ofPaletteIndex(int index) {
        ColorRef c = new ColorRef();
        c.paletteIndex = index;
        c.isPaletteColor = true;
        return c;
    }

    public static ColorRef fromPackedRgb(int packed) {
        return new ColorRef(
            (packed >> 16) & 0xFF,
            (packed >> 8) & 0xFF,
            packed & 0xFF
        );
    }

    public int toPackedRgb() {
        return (red << 16) | (green << 8) | blue;
    }

    public ColorRef copy() {
        ColorRef c = new ColorRef(red, green, blue);
        c.paletteIndex = paletteIndex;
        c.isPaletteColor = isPaletteColor;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColorRef colorRef = (ColorRef) o;
        if (isPaletteColor && colorRef.isPaletteColor) {
            return paletteIndex == colorRef.paletteIndex;
        }
        return red == colorRef.red && green == colorRef.green && blue == colorRef.blue;
    }

    @Override
    public int hashCode() {
        if (isPaletteColor) {
            return Objects.hash(paletteIndex, true);
        }
        return Objects.hash(red, green, blue);
    }

    @Override
    public String toString() {
        if (isPaletteColor) {
            return "paletteIndex(" + paletteIndex + ")";
        }
        return "color(" + red + ", " + green + ", " + blue + ")";
    }

    // Alias methods for rendering compatibility
    public static ColorRef paletteIndex(int index) {
        return fromPaletteIndex(index);
    }

    public static ColorRef rgb(int r, int g, int b) {
        return fromRgb(r, g, b);
    }

    public boolean isRgb() {
        return !isPaletteColor;
    }

    public boolean isPaletteIndex() {
        return isPaletteColor;
    }

    public int getR() {
        return red;
    }

    public int getG() {
        return green;
    }

    public int getB() {
        return blue;
    }

    public int getPaletteIndex() {
        return paletteIndex;
    }

    /**
     * Create a ColorRef from a hex string like "#RRGGBB".
     * Port of Rust ColorRef::from_hex.
     */
    public static ColorRef fromHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return fromRgb(r, g, b);
    }

    /**
     * Convert this ColorRef to a palette index using the given palette.
     * If this is already a palette index, returns that index.
     * Otherwise, finds the closest matching color in the palette.
     * Port of Rust ColorRef::to_index.
     *
     * @param palette Array of RGB triplets [r, g, b]
     * @return The best matching palette index
     */
    public int toIndex(int[][] palette) {
        if (isPaletteColor) {
            return paletteIndex;
        }

        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < palette.length; i++) {
            int pr = palette[i][0];
            int pg = palette[i][1];
            int pb = palette[i][2];

            int dr = red - pr;
            int dg = green - pg;
            int db = blue - pb;
            int distance = dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }
}
