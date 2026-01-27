package com.dirplayer.player.bitmap;

/**
 * Reference to a bitmap in the bitmap manager.
 * Port of Rust BitmapRef struct.
 */
public class BitmapRef {
    public int bitmapId;
    public int width;
    public int height;
    public int bitDepth;
    public int paletteRef;

    public BitmapRef() {
        this.bitmapId = 0;
        this.width = 0;
        this.height = 0;
        this.bitDepth = 8;
        this.paletteRef = -1;
    }

    public BitmapRef(int bitmapId) {
        this.bitmapId = bitmapId;
        this.width = 0;
        this.height = 0;
        this.bitDepth = 8;
        this.paletteRef = -1;
    }

    public BitmapRef(int bitmapId, int width, int height, int bitDepth) {
        this.bitmapId = bitmapId;
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.paletteRef = -1;
    }

    public BitmapRef copy() {
        BitmapRef ref = new BitmapRef(bitmapId, width, height, bitDepth);
        ref.paletteRef = paletteRef;
        return ref;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Check if bitmap is loaded (has valid dimensions).
     */
    public boolean isLoaded() {
        return width > 0 && height > 0;
    }

    @Override
    public String toString() {
        return "bitmap(" + bitmapId + ", " + width + "x" + height + ", " + bitDepth + "bpp)";
    }
}
