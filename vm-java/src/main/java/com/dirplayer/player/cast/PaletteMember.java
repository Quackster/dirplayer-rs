package com.dirplayer.player.cast;

/**
 * Palette cast member data.
 * Port of Rust PaletteMember struct.
 */
public class PaletteMember {
    public int[][] colors;  // RGB values for each color

    public PaletteMember() {
        this.colors = new int[256][3];
        // Initialize to black
        for (int i = 0; i < 256; i++) {
            colors[i] = new int[]{0, 0, 0};
        }
    }

    public PaletteMember(int colorCount) {
        this.colors = new int[colorCount][3];
        for (int i = 0; i < colorCount; i++) {
            colors[i] = new int[]{0, 0, 0};
        }
    }

    public int[] getColor(int index) {
        if (index >= 0 && index < colors.length) {
            return colors[index];
        }
        return new int[]{0, 0, 0};
    }

    public void setColor(int index, int r, int g, int b) {
        if (index >= 0 && index < colors.length) {
            colors[index][0] = r;
            colors[index][1] = g;
            colors[index][2] = b;
        }
    }

    public int getColorCount() {
        return colors.length;
    }

    public PaletteMember copy() {
        PaletteMember copy = new PaletteMember(colors.length);
        for (int i = 0; i < colors.length; i++) {
            copy.colors[i] = colors[i].clone();
        }
        return copy;
    }
}
