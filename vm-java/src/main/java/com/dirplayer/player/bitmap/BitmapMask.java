package com.dirplayer.player.bitmap;

/**
 * Bitmap mask (matte) for transparency.
 * Port of Rust BitmapMask struct.
 */
public class BitmapMask {
    public int width;
    public int height;
    public byte[] data;  // 1 byte per pixel, 0 = transparent, 255 = opaque

    public BitmapMask(int width, int height, boolean fill) {
        this.width = width;
        this.height = height;
        this.data = new byte[width * height];
        if (fill) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) 255;
            }
        }
    }

    public boolean isTransparent(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return true;
        }
        return data[y * width + x] == 0;
    }

    public void setTransparent(int x, int y, boolean transparent) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y * width + x] = transparent ? (byte) 0 : (byte) 255;
        }
    }

    public int getAlpha(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }
        return data[y * width + x] & 0xFF;
    }

    public void setAlpha(int x, int y, int alpha) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y * width + x] = (byte) (alpha & 0xFF);
        }
    }

    public BitmapMask copy() {
        BitmapMask copy = new BitmapMask(width, height, false);
        System.arraycopy(data, 0, copy.data, 0, data.length);
        return copy;
    }
}
