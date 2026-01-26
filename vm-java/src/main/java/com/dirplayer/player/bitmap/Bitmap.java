package com.dirplayer.player.bitmap;

/**
 * Bitmap data storage with palette support.
 * Port of Rust Bitmap struct.
 */
public class Bitmap {
    public int width;
    public int height;
    public int bitDepth;           // Current storage format (8, 16, or 32)
    public int originalBitDepth;   // Original format (1, 2, 4, 8, 16, 32)
    public byte[] data;            // Pixel data (format depends on bitDepth)
    public PaletteRef paletteRef;
    public BitmapMask matte;
    public boolean useAlpha;
    public boolean trimWhiteSpace;
    public boolean wasTrimmed;

    public Bitmap() {
        this.width = 0;
        this.height = 0;
        this.bitDepth = 8;
        this.originalBitDepth = 8;
        this.data = new byte[0];
        this.paletteRef = PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin);
        this.matte = null;
        this.useAlpha = false;
        this.trimWhiteSpace = false;
        this.wasTrimmed = false;
    }

    public Bitmap(int width, int height, int bitDepth, int originalBitDepth, int alphaDepth, PaletteRef paletteRef) {
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.originalBitDepth = originalBitDepth;
        this.paletteRef = paletteRef;

        int bytesPerPixel = bitDepth / 8;
        int initialColor = (bitDepth == 16 || bitDepth == 32) ? 255 : 0;
        this.data = new byte[width * height * bytesPerPixel];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) initialColor;
        }

        // Create matte for 32-bit images or if alpha depth > 0
        if (alphaDepth > 0 || bitDepth == 32) {
            this.matte = new BitmapMask(width, height, true);
        }

        this.useAlpha = false;
        this.trimWhiteSpace = false;
        this.wasTrimmed = false;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 0;
        }

        int bytesPerPixel = bitDepth / 8;
        int index = (y * width + x) * bytesPerPixel;

        if (bitDepth == 8) {
            return data[index] & 0xFF;
        } else if (bitDepth == 16) {
            return ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
        } else if (bitDepth == 32) {
            return ((data[index] & 0xFF) << 24) |
                   ((data[index + 1] & 0xFF) << 16) |
                   ((data[index + 2] & 0xFF) << 8) |
                   (data[index + 3] & 0xFF);
        }
        return 0;
    }

    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        int bytesPerPixel = bitDepth / 8;
        int index = (y * width + x) * bytesPerPixel;

        if (bitDepth == 8) {
            data[index] = (byte) color;
        } else if (bitDepth == 16) {
            data[index] = (byte) ((color >> 8) & 0xFF);
            data[index + 1] = (byte) (color & 0xFF);
        } else if (bitDepth == 32) {
            data[index] = (byte) ((color >> 24) & 0xFF);
            data[index + 1] = (byte) ((color >> 16) & 0xFF);
            data[index + 2] = (byte) ((color >> 8) & 0xFF);
            data[index + 3] = (byte) (color & 0xFF);
        }
    }

    public Bitmap copy() {
        Bitmap copy = new Bitmap();
        copy.width = width;
        copy.height = height;
        copy.bitDepth = bitDepth;
        copy.originalBitDepth = originalBitDepth;
        copy.data = data.clone();
        copy.paletteRef = paletteRef;
        copy.matte = matte != null ? matte.copy() : null;
        copy.useAlpha = useAlpha;
        copy.trimWhiteSpace = trimWhiteSpace;
        copy.wasTrimmed = wasTrimmed;
        return copy;
    }
}
