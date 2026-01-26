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

    // Getter methods for rendering
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBitDepth() { return bitDepth; }
    public int getOriginalBitDepth() { return originalBitDepth; }
    public byte[] getData() { return data; }
    public BitmapMask getMatte() { return matte; }
    public boolean isUseAlpha() { return useAlpha; }
    public void setUseAlpha(boolean useAlpha) { this.useAlpha = useAlpha; }

    /**
     * Clear bitmap data to zeros (transparent).
     */
    public void clearData() {
        java.util.Arrays.fill(data, (byte) 0);
    }

    /**
     * Set pixel with RGB values.
     */
    public void setPixel(int x, int y, int r, int g, int b, PaletteMap palettes) {
        if (bitDepth == 32) {
            setPixel(x, y, (r << 24) | (g << 16) | (b << 8) | 0xFF);
        } else {
            setPixel(x, y, (r << 16) | (g << 8) | b);
        }
    }

    /**
     * Clear a rectangular region.
     */
    public void clearRect(int left, int top, int right, int bottom,
                         int r, int g, int b, PaletteMap palettes) {
        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                setPixelRGBA(x, y, r, g, b, 255);
            }
        }
    }

    /**
     * Clear a rectangular region to transparent.
     */
    public void clearRectTransparent(int left, int top, int right, int bottom) {
        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                setPixelRGBA(x, y, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Fill a rectangle.
     */
    public void fillRect(int left, int top, int right, int bottom,
                        int r, int g, int b, PaletteMap palettes, float alpha) {
        int a = (int)(alpha * 255);
        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                if (alpha >= 1.0f) {
                    setPixelRGBA(x, y, r, g, b, 255);
                } else {
                    blendPixelRGBA(x, y, r, g, b, a);
                }
            }
        }
    }

    /**
     * Stroke (outline) a rectangle.
     */
    public void strokeRect(int left, int top, int right, int bottom,
                          int r, int g, int b, PaletteMap palettes, float alpha) {
        // Top and bottom edges
        for (int x = left; x < right; x++) {
            setPixelRGBA(x, top, r, g, b, 255);
            setPixelRGBA(x, bottom - 1, r, g, b, 255);
        }
        // Left and right edges
        for (int y = top; y < bottom; y++) {
            setPixelRGBA(left, y, r, g, b, 255);
            setPixelRGBA(right - 1, y, r, g, b, 255);
        }
    }

    /**
     * Set pixel with RGBA values (for 32-bit bitmaps).
     */
    private void setPixelRGBA(int x, int y, int r, int g, int b, int a) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;

        if (bitDepth == 32) {
            int index = (y * width + x) * 4;
            data[index] = (byte) r;
            data[index + 1] = (byte) g;
            data[index + 2] = (byte) b;
            data[index + 3] = (byte) a;
        }
    }

    /**
     * Blend pixel with RGBA values.
     */
    private void blendPixelRGBA(int x, int y, int r, int g, int b, int a) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;

        if (bitDepth == 32) {
            int index = (y * width + x) * 4;
            int srcR = data[index] & 0xFF;
            int srcG = data[index + 1] & 0xFF;
            int srcB = data[index + 2] & 0xFF;

            float alpha = a / 255.0f;
            float invAlpha = 1.0f - alpha;

            data[index] = (byte)(r * alpha + srcR * invAlpha);
            data[index + 1] = (byte)(g * alpha + srcG * invAlpha);
            data[index + 2] = (byte)(b * alpha + srcB * invAlpha);
            data[index + 3] = (byte) 255;
        }
    }

    /**
     * Create matte (transparency mask) from bitmap data.
     */
    public void createMatte(PaletteMap palettes) {
        matte = new BitmapMask(width, height, false);

        if (bitDepth == 32) {
            // Use alpha channel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 4;
                    int alpha = data[index + 3] & 0xFF;
                    matte.setAlpha(x, y, alpha);
                }
            }
        } else if (bitDepth == 8) {
            // Use palette index 0 as transparent (white usually)
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    int paletteIdx = data[index] & 0xFF;
                    // Index 0 is typically transparent/white
                    matte.setAlpha(x, y, paletteIdx == 0 ? 0 : 255);
                }
            }
        }
    }

    /**
     * Get foreground color reference (default black).
     */
    public com.dirplayer.player.ColorRef getFgColorRef() {
        return com.dirplayer.player.ColorRef.paletteIndex(255);
    }

    /**
     * Get background color reference (default white).
     */
    public com.dirplayer.player.ColorRef getBgColorRef() {
        return com.dirplayer.player.ColorRef.paletteIndex(0);
    }

    /**
     * Convert bitmap to a mask.
     */
    public BitmapMask toMask() {
        BitmapMask mask = new BitmapMask(width, height, false);
        if (bitDepth == 32) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 4;
                    int alpha = data[index + 3] & 0xFF;
                    mask.setAlpha(x, y, alpha);
                }
            }
        } else if (bitDepth == 8) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    int paletteIdx = data[index] & 0xFF;
                    // Non-zero index = opaque
                    mask.setAlpha(x, y, paletteIdx == 0 ? 0 : 255);
                }
            }
        }
        return mask;
    }

    /**
     * Copy pixels from source bitmap (simple version without ink effects).
     */
    public void copyPixels(PaletteMap palettes, Bitmap src,
                          com.dirplayer.rendering.IntRect dstRect,
                          com.dirplayer.rendering.IntRect srcRect,
                          java.util.Map<String, Object> options,
                          BitmapMask mask) {
        com.dirplayer.rendering.CopyPixelsParams params = new com.dirplayer.rendering.CopyPixelsParams();
        params.maskImage = mask;
        copyPixelsWithParams(palettes, src, dstRect, srcRect, params);
    }

    /**
     * Copy pixels from source bitmap with parameters.
     */
    public void copyPixelsWithParams(PaletteMap palettes, Bitmap src,
                                     com.dirplayer.rendering.IntRect dstRect,
                                     com.dirplayer.rendering.IntRect srcRect,
                                     com.dirplayer.rendering.CopyPixelsParams params) {
        // Basic implementation - no ink effects yet
        int srcW = srcRect.width();
        int srcH = srcRect.height();
        int dstW = dstRect.width();
        int dstH = dstRect.height();

        float scaleX = (float)srcW / dstW;
        float scaleY = (float)srcH / dstH;

        float blendFactor = params.blend / 100.0f;

        for (int dy = 0; dy < Math.abs(dstH); dy++) {
            for (int dx = 0; dx < Math.abs(dstW); dx++) {
                int destX = dstRect.left + (dstW > 0 ? dx : -dx);
                int destY = dstRect.top + (dstH > 0 ? dy : -dy);

                int srcX = srcRect.left + (int)(dx * scaleX);
                int srcY = srcRect.top + (int)(dy * scaleY);

                // Check mask if present
                if (params.maskImage != null) {
                    if (params.maskImage.isTransparent(srcX - srcRect.left, srcY - srcRect.top)) {
                        continue;
                    }
                }

                // Get source pixel
                int srcColor = src.getPixel(srcX, srcY);

                // Convert to RGBA if needed
                int r, g, b, a;
                if (src.bitDepth == 32) {
                    r = (srcColor >> 24) & 0xFF;
                    g = (srcColor >> 16) & 0xFF;
                    b = (srcColor >> 8) & 0xFF;
                    a = srcColor & 0xFF;
                } else if (src.bitDepth == 8) {
                    // Lookup in palette
                    int[][] palette = palettes.getPalette(src.paletteRef, src.originalBitDepth);
                    int[] rgb = palette != null && srcColor < palette.length ?
                        palette[srcColor] : new int[]{0, 0, 0};
                    r = rgb[0];
                    g = rgb[1];
                    b = rgb[2];
                    a = 255;
                } else {
                    r = g = b = 0;
                    a = 255;
                }

                // Apply blend
                if (blendFactor < 1.0f) {
                    a = (int)(a * blendFactor);
                }

                // Set destination pixel
                if (bitDepth == 32) {
                    blendPixelRGBA(destX, destY, r, g, b, a);
                } else {
                    setPixel(destX, destY, (r << 16) | (g << 8) | b);
                }
            }
        }
    }
}
