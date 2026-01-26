package com.dirplayer.player.bitmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for bitmap data and palettes.
 * Port of Rust BitmapManager struct.
 */
public class BitmapManager {
    private Map<Integer, BitmapData> bitmaps;
    private Map<Integer, PaletteData> palettes;
    private int nextBitmapId;
    private int currentPaletteId;

    public BitmapManager() {
        this.bitmaps = new HashMap<>();
        this.palettes = new HashMap<>();
        this.nextBitmapId = 1;
        this.currentPaletteId = 0;

        // Initialize default palette
        initDefaultPalette();
    }

    private void initDefaultPalette() {
        // Create a default 256-color palette (grayscale or system)
        PaletteData defaultPalette = new PaletteData(0, 256);
        for (int i = 0; i < 256; i++) {
            defaultPalette.colors[i] = (i << 16) | (i << 8) | i;  // Grayscale
        }
        palettes.put(0, defaultPalette);
    }

    public int createBitmap(int width, int height, int bitDepth) {
        int id = nextBitmapId++;
        BitmapData bitmap = new BitmapData(id, width, height, bitDepth);
        bitmaps.put(id, bitmap);
        return id;
    }

    public int createBitmap(int width, int height, int bitDepth, int[] data) {
        int id = nextBitmapId++;
        BitmapData bitmap = new BitmapData(id, width, height, bitDepth);
        bitmap.data = data;
        bitmaps.put(id, bitmap);
        return id;
    }

    public BitmapData getBitmap(int id) {
        return bitmaps.get(id);
    }

    public void freeBitmap(int id) {
        bitmaps.remove(id);
    }

    public void addPalette(int id, PaletteData palette) {
        palettes.put(id, palette);
    }

    public PaletteData getPalette(int id) {
        return palettes.get(id);
    }

    public PaletteData getCurrentPalette() {
        return palettes.get(currentPaletteId);
    }

    public void setCurrentPalette(int id) {
        this.currentPaletteId = id;
    }

    public int getColorFromPalette(int paletteId, int index) {
        PaletteData palette = palettes.get(paletteId);
        if (palette != null && index >= 0 && index < palette.colors.length) {
            return palette.colors[index];
        }
        return 0;
    }

    /**
     * Bitmap data storage.
     */
    public static class BitmapData {
        public int id;
        public int width;
        public int height;
        public int bitDepth;
        public int[] data;  // ARGB pixel data
        public int paletteId;
        public int regX;
        public int regY;
        public boolean useAlpha;

        public BitmapData(int id, int width, int height, int bitDepth) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.bitDepth = bitDepth;
            this.data = new int[width * height];
            this.paletteId = 0;
            this.regX = 0;
            this.regY = 0;
            this.useAlpha = bitDepth == 32;
        }

        public int getPixel(int x, int y) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                return data[y * width + x];
            }
            return 0;
        }

        public void setPixel(int x, int y, int color) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                data[y * width + x] = color;
            }
        }

        public void fill(int color) {
            for (int i = 0; i < data.length; i++) {
                data[i] = color;
            }
        }

        public BitmapData copy() {
            BitmapData copy = new BitmapData(0, width, height, bitDepth);
            System.arraycopy(data, 0, copy.data, 0, data.length);
            copy.paletteId = paletteId;
            copy.regX = regX;
            copy.regY = regY;
            copy.useAlpha = useAlpha;
            return copy;
        }
    }

    /**
     * Palette data storage.
     */
    public static class PaletteData {
        public int id;
        public int[] colors;  // ARGB colors
        public int colorCount;

        public PaletteData(int id, int colorCount) {
            this.id = id;
            this.colorCount = colorCount;
            this.colors = new int[colorCount];
        }

        public int getColor(int index) {
            if (index >= 0 && index < colorCount) {
                return colors[index];
            }
            return 0;
        }

        public void setColor(int index, int color) {
            if (index >= 0 && index < colorCount) {
                colors[index] = color;
            }
        }

        public PaletteData copy() {
            PaletteData copy = new PaletteData(0, colorCount);
            System.arraycopy(colors, 0, copy.colors, 0, colorCount);
            return copy;
        }
    }
}
