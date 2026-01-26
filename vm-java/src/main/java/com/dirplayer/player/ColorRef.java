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

    public static ColorRef fromPaletteIndex(int index) {
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
}
