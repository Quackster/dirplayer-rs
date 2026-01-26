package com.dirplayer.player.bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of palettes available in a movie.
 * Port of Rust PaletteMap struct.
 */
public class PaletteMap {
    private final Map<Integer, int[][]> castPalettes;

    public PaletteMap() {
        this.castPalettes = new HashMap<>();
    }

    /**
     * Add a cast member palette.
     */
    public void addCastPalette(int castMemberNum, int[][] palette) {
        castPalettes.put(castMemberNum, palette);
    }

    /**
     * Get a palette by reference.
     */
    public int[][] getPalette(PaletteRef ref, int bitDepth) {
        if (ref == null) {
            return Palettes.getPalette(BuiltInPalette.SystemWin, bitDepth);
        }

        if (ref.isBuiltIn()) {
            return Palettes.getPalette(ref.getBuiltInType(), bitDepth);
        }

        // Cast member palette
        int[][] castPalette = castPalettes.get(ref.getCastMemberNum());
        if (castPalette != null) {
            return castPalette;
        }

        // Fallback to system palette
        return Palettes.getPalette(BuiltInPalette.SystemWin, bitDepth);
    }

    /**
     * Get palette for original bit depth.
     */
    public int[][] getPaletteForBitDepth(PaletteRef ref, int originalBitDepth) {
        return getPalette(ref, originalBitDepth);
    }

    /**
     * Get color from palette.
     */
    public int[] getColor(PaletteRef ref, int index, int bitDepth) {
        int[][] palette = getPalette(ref, bitDepth);
        return Palettes.getColor(palette, index);
    }
}
