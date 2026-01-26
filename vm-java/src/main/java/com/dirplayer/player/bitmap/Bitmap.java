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
     * Fill a rectangle relative to sprite position.
     * Used for shape rendering where coordinates are relative to width/height.
     */
    public void fillRelativeRect(int relLeft, int relTop, int relRight, int relBottom,
                                  int r, int g, int b, PaletteMap palettes, float alpha) {
        int left = relLeft;
        int top = relTop;
        int right = relRight != 0 ? relRight : width;
        int bottom = relBottom != 0 ? relBottom : height;

        fillRect(left, top, right, bottom, r, g, b, palettes, alpha);
    }

    /**
     * Fill a shape rectangle using sprite properties (ink, color, bgColor).
     * Port of fill_shape_rect_with_sprite from Rust.
     *
     * @param sprite The sprite with rendering properties
     * @param rect The rectangle to fill
     * @param palettes The palette map for color resolution
     */
    public void fillShapeRectWithSprite(com.dirplayer.player.Sprite sprite,
                                         com.dirplayer.rendering.IntRect rect,
                                         PaletteMap palettes) {
        // Get foreground and background colors from sprite
        com.dirplayer.player.ColorRef fgColor = sprite.getColor();
        com.dirplayer.player.ColorRef bgColor = sprite.getBgColor();
        int ink = sprite.getInk();
        int blend = sprite.getBlend();

        // Resolve foreground color
        int[] fg = resolveColorRefInternal(palettes, fgColor, paletteRef, originalBitDepth);
        // Resolve background color
        int[] bg = resolveColorRefInternal(palettes, bgColor, paletteRef, originalBitDepth);

        // Calculate alpha from blend (blend 0 = 100%, blend 100 = fully transparent for some inks)
        float alpha = blend == 0 ? 1.0f : blend / 100.0f;

        // Apply ink effect
        switch (ink) {
            case 0: // Copy
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;

            case 1: // Transparent
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;

            case 2: // Reverse - invert colors in the destination
                fillRectReverse(rect.left, rect.top, rect.right, rect.bottom, palettes);
                break;

            case 3: // Ghost
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, 0.5f);
                break;

            case 4: // Not Copy
                fillRect(rect.left, rect.top, rect.right, rect.bottom,
                    255 - fg[0], 255 - fg[1], 255 - fg[2], palettes, alpha);
                break;

            case 5: // Not Transparent
                fillRect(rect.left, rect.top, rect.right, rect.bottom,
                    255 - fg[0], 255 - fg[1], 255 - fg[2], palettes, alpha);
                break;

            case 6: // Not Reverse
                fillRectReverse(rect.left, rect.top, rect.right, rect.bottom, palettes);
                break;

            case 7: // Not Ghost
                fillRect(rect.left, rect.top, rect.right, rect.bottom,
                    255 - fg[0], 255 - fg[1], 255 - fg[2], palettes, 0.5f);
                break;

            case 8: // Matte
            case 9: // Mask
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;

            case 32: // Blend
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;

            case 33: // Add Pin
                fillRectAdditive(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], alpha, true);
                break;

            case 34: // Add
                fillRectAdditive(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], alpha, false);
                break;

            case 35: // Subtract Pin
                fillRectSubtractive(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], alpha, true);
                break;

            case 36: // Background Transparent
                // Fill with foreground but background color is transparent
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;

            case 37: // Lightest
                fillRectLightest(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2]);
                break;

            case 38: // Subtract
                fillRectSubtractive(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], alpha, false);
                break;

            case 39: // Darkest
                fillRectDarkest(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2]);
                break;

            default:
                // Default to copy behavior
                fillRect(rect.left, rect.top, rect.right, rect.bottom, fg[0], fg[1], fg[2], palettes, alpha);
                break;
        }
    }

    /**
     * Helper to resolve color reference.
     */
    private int[] resolveColorRefInternal(PaletteMap palettes, com.dirplayer.player.ColorRef colorRef,
                                          PaletteRef paletteRef, int bitDepth) {
        if (colorRef == null) {
            return new int[] { 0, 0, 0 };
        }

        if (colorRef.isRgb()) {
            return new int[] { colorRef.getR(), colorRef.getG(), colorRef.getB() };
        }

        int index = colorRef.getPaletteIndex();
        int[][] palette = palettes.getPalette(paletteRef, bitDepth);
        if (palette != null && index >= 0 && index < palette.length) {
            return palette[index];
        }

        return new int[] { 0, 0, 0 };
    }

    /**
     * Fill rectangle with reverse ink (invert colors).
     */
    private void fillRectReverse(int left, int top, int right, int bottom, PaletteMap palettes) {
        if (bitDepth != 32) return;

        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                int index = (y * this.width + x) * 4;
                data[index] = (byte)(255 - (data[index] & 0xFF));
                data[index + 1] = (byte)(255 - (data[index + 1] & 0xFF));
                data[index + 2] = (byte)(255 - (data[index + 2] & 0xFF));
            }
        }
    }

    /**
     * Fill rectangle with additive blending.
     */
    private void fillRectAdditive(int left, int top, int right, int bottom,
                                   int r, int g, int b, float alpha, boolean clamp) {
        if (bitDepth != 32) return;

        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                int index = (y * this.width + x) * 4;
                int srcR = data[index] & 0xFF;
                int srcG = data[index + 1] & 0xFF;
                int srcB = data[index + 2] & 0xFF;

                int newR = srcR + (int)(r * alpha);
                int newG = srcG + (int)(g * alpha);
                int newB = srcB + (int)(b * alpha);

                if (clamp) {
                    newR = Math.min(255, newR);
                    newG = Math.min(255, newG);
                    newB = Math.min(255, newB);
                } else {
                    newR = newR & 0xFF;
                    newG = newG & 0xFF;
                    newB = newB & 0xFF;
                }

                data[index] = (byte) newR;
                data[index + 1] = (byte) newG;
                data[index + 2] = (byte) newB;
            }
        }
    }

    /**
     * Fill rectangle with subtractive blending.
     */
    private void fillRectSubtractive(int left, int top, int right, int bottom,
                                      int r, int g, int b, float alpha, boolean clamp) {
        if (bitDepth != 32) return;

        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                int index = (y * this.width + x) * 4;
                int srcR = data[index] & 0xFF;
                int srcG = data[index + 1] & 0xFF;
                int srcB = data[index + 2] & 0xFF;

                int newR = srcR - (int)(r * alpha);
                int newG = srcG - (int)(g * alpha);
                int newB = srcB - (int)(b * alpha);

                if (clamp) {
                    newR = Math.max(0, newR);
                    newG = Math.max(0, newG);
                    newB = Math.max(0, newB);
                } else {
                    newR = newR & 0xFF;
                    newG = newG & 0xFF;
                    newB = newB & 0xFF;
                }

                data[index] = (byte) newR;
                data[index + 1] = (byte) newG;
                data[index + 2] = (byte) newB;
            }
        }
    }

    /**
     * Fill rectangle keeping the lightest pixel.
     */
    private void fillRectLightest(int left, int top, int right, int bottom, int r, int g, int b) {
        if (bitDepth != 32) return;

        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                int index = (y * this.width + x) * 4;
                int srcR = data[index] & 0xFF;
                int srcG = data[index + 1] & 0xFF;
                int srcB = data[index + 2] & 0xFF;

                data[index] = (byte) Math.max(srcR, r);
                data[index + 1] = (byte) Math.max(srcG, g);
                data[index + 2] = (byte) Math.max(srcB, b);
            }
        }
    }

    /**
     * Fill rectangle keeping the darkest pixel.
     */
    private void fillRectDarkest(int left, int top, int right, int bottom, int r, int g, int b) {
        if (bitDepth != 32) return;

        for (int y = Math.max(0, top); y < Math.min(height, bottom); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, right); x++) {
                int index = (y * this.width + x) * 4;
                int srcR = data[index] & 0xFF;
                int srcG = data[index + 1] & 0xFF;
                int srcB = data[index + 2] & 0xFF;

                data[index] = (byte) Math.min(srcR, r);
                data[index + 1] = (byte) Math.min(srcG, g);
                data[index + 2] = (byte) Math.min(srcB, b);
            }
        }
    }

    /**
     * Create matte for text rendering (uses white as transparent).
     * Port of create_matte_text from Rust.
     */
    public void createMatteText(PaletteMap palettes) {
        matte = new BitmapMask(width, height, false);

        if (bitDepth == 32) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 4;
                    int r = data[index] & 0xFF;
                    int g = data[index + 1] & 0xFF;
                    int b = data[index + 2] & 0xFF;
                    // White (255,255,255) is transparent in text
                    boolean isWhite = r >= 250 && g >= 250 && b >= 250;
                    matte.setAlpha(x, y, isWhite ? 0 : 255);
                }
            }
        } else if (bitDepth == 8) {
            // Index 0 (typically white) is transparent
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    int paletteIdx = data[index] & 0xFF;
                    matte.setAlpha(x, y, paletteIdx == 0 ? 0 : 255);
                }
            }
        }
    }

    /**
     * Draw text to the bitmap using a bitmap font.
     * Port of draw_text from Rust.
     *
     * @param text The text to render
     * @param font The bitmap font
     * @param fontBitmap The font's bitmap data
     * @param x Starting X position
     * @param y Starting Y position
     * @param params Copy pixels parameters (ink, color, etc.)
     * @param palettes Palette map for color resolution
     * @param fixedLineSpace Fixed line spacing (0 for auto)
     * @param topSpacing Top spacing offset
     */
    public void drawText(String text, com.dirplayer.player.FontManager.BitmapFont font,
                         Bitmap fontBitmap, int x, int y,
                         com.dirplayer.rendering.CopyPixelsParams params,
                         PaletteMap palettes, int fixedLineSpace, int topSpacing) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }

        int lineHeight = fixedLineSpace > 0 ? fixedLineSpace : font.charHeight;
        int cursorX = x;
        int cursorY = y + topSpacing;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle newlines
            if (c == '\n') {
                cursorX = x;
                cursorY += lineHeight;
                continue;
            }

            // Handle carriage return
            if (c == '\r') {
                continue;
            }

            // Get character glyph info
            int charCode = c;
            if (charCode < font.firstChar || charCode > font.lastChar) {
                // Character not in font, use space width
                cursorX += font.charWidth;
                continue;
            }

            int charIndex = charCode - font.firstChar;
            int charWidth = font.charWidth;
            int charHeight = font.charHeight;

            // Calculate source position in font bitmap
            // Fonts are typically arranged in a grid
            int charsPerRow = fontBitmap.getWidth() / charWidth;
            if (charsPerRow <= 0) charsPerRow = 1;

            int srcRow = charIndex / charsPerRow;
            int srcCol = charIndex % charsPerRow;
            int srcX = srcCol * charWidth;
            int srcY = srcRow * charHeight;

            // Copy the character glyph
            com.dirplayer.rendering.IntRect srcRect = com.dirplayer.rendering.IntRect.from(
                srcX, srcY, srcX + charWidth, srcY + charHeight);
            com.dirplayer.rendering.IntRect dstRect = com.dirplayer.rendering.IntRect.from(
                cursorX, cursorY, cursorX + charWidth, cursorY + charHeight);

            // Use copy pixels with the text rendering params
            copyPixelsWithParams(palettes, fontBitmap, dstRect, srcRect, params);

            cursorX += charWidth;
        }
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
     * Supports ink effects, masks, and color transformations.
     */
    public void copyPixelsWithParams(PaletteMap palettes, Bitmap src,
                                     com.dirplayer.rendering.IntRect dstRect,
                                     com.dirplayer.rendering.IntRect srcRect,
                                     com.dirplayer.rendering.CopyPixelsParams params) {
        int srcW = srcRect.width();
        int srcH = srcRect.height();
        int dstW = dstRect.width();
        int dstH = dstRect.height();

        if (srcW == 0 || srcH == 0 || dstW == 0 || dstH == 0) {
            return;
        }

        float scaleX = (float)Math.abs(srcW) / Math.abs(dstW);
        float scaleY = (float)Math.abs(srcH) / Math.abs(dstH);

        int blend = params.blend;
        int ink = params.ink;

        // Resolve foreground and background colors for ink effects
        int[] fgColor = resolveColorRefInternal(palettes, params.color, src.paletteRef, src.originalBitDepth);
        int[] bgColor = resolveColorRefInternal(palettes, params.bgColor, src.paletteRef, src.originalBitDepth);

        // Handle flipping via negative dimensions
        boolean flipH = dstW < 0;
        boolean flipV = dstH < 0;
        int absDstW = Math.abs(dstW);
        int absDstH = Math.abs(dstH);

        for (int dy = 0; dy < absDstH; dy++) {
            for (int dx = 0; dx < absDstW; dx++) {
                // Calculate destination position
                int destX = flipH ? dstRect.left - dx - 1 : dstRect.left + dx;
                int destY = flipV ? dstRect.top - dy - 1 : dstRect.top + dy;

                // Bounds check on destination
                if (destX < 0 || destX >= width || destY < 0 || destY >= height) {
                    continue;
                }

                // Calculate source position
                int srcX = srcRect.left + (int)(dx * scaleX);
                int srcY = srcRect.top + (int)(dy * scaleY);

                // Bounds check on source
                if (srcX < 0 || srcX >= src.width || srcY < 0 || srcY >= src.height) {
                    continue;
                }

                // Check mask if present
                if (params.maskImage != null) {
                    int maskX = (int)(dx * scaleX);
                    int maskY = (int)(dy * scaleY);
                    if (params.maskImage.isTransparent(maskX, maskY)) {
                        continue;
                    }
                }

                // Get source pixel and convert to RGBA
                int srcColor = src.getPixel(srcX, srcY);
                int srcR, srcG, srcB, srcA;

                if (src.bitDepth == 32) {
                    srcR = (srcColor >> 24) & 0xFF;
                    srcG = (srcColor >> 16) & 0xFF;
                    srcB = (srcColor >> 8) & 0xFF;
                    srcA = srcColor & 0xFF;
                } else if (src.bitDepth == 8) {
                    int[][] palette = palettes.getPalette(src.paletteRef, src.originalBitDepth);
                    int[] rgb = palette != null && srcColor >= 0 && srcColor < palette.length ?
                        palette[srcColor] : new int[]{0, 0, 0};
                    srcR = rgb[0];
                    srcG = rgb[1];
                    srcB = rgb[2];
                    srcA = 255;
                } else if (src.bitDepth == 16) {
                    // RGB555 format
                    srcR = ((srcColor >> 10) & 0x1F) * 255 / 31;
                    srcG = ((srcColor >> 5) & 0x1F) * 255 / 31;
                    srcB = (srcColor & 0x1F) * 255 / 31;
                    srcA = 255;
                } else {
                    srcR = srcG = srcB = 0;
                    srcA = 255;
                }

                // Handle alpha from source bitmap (if 32-bit with alpha)
                if (src.useAlpha && src.bitDepth == 32 && srcA == 0) {
                    continue;  // Skip fully transparent pixels
                }

                // Get destination pixel for ink effects that need it
                int dstR = 0, dstG = 0, dstB = 0;
                if (bitDepth == 32) {
                    int dstIndex = (destY * width + destX) * 4;
                    if (dstIndex >= 0 && dstIndex + 2 < data.length) {
                        dstR = data[dstIndex] & 0xFF;
                        dstG = data[dstIndex + 1] & 0xFF;
                        dstB = data[dstIndex + 2] & 0xFF;
                    }
                }

                // Apply ink effect
                int[] result = applyInkEffect(ink, srcR, srcG, srcB, srcA,
                    dstR, dstG, dstB, blend, fgColor, bgColor);

                int finalR = result[0];
                int finalG = result[1];
                int finalB = result[2];
                int finalA = result[3];

                // Skip if fully transparent
                if (finalA == 0) {
                    continue;
                }

                // Write to destination
                if (bitDepth == 32) {
                    if (finalA >= 255) {
                        setPixelRGBA(destX, destY, finalR, finalG, finalB, 255);
                    } else {
                        blendPixelRGBA(destX, destY, finalR, finalG, finalB, finalA);
                    }
                } else {
                    setPixel(destX, destY, (finalR << 16) | (finalG << 8) | finalB);
                }
            }
        }
    }

    /**
     * Apply ink effect to colors.
     * Port of ink effect logic from Rust rendering.
     */
    private int[] applyInkEffect(int ink, int srcR, int srcG, int srcB, int srcA,
                                  int dstR, int dstG, int dstB, int blend,
                                  int[] fgColor, int[] bgColor) {
        float alpha = blend / 100.0f;

        switch (ink) {
            case 0: // Copy
                return new int[] { srcR, srcG, srcB, (int)(srcA * alpha) };

            case 1: // Transparent
                return new int[] { srcR, srcG, srcB, (int)(srcA * alpha) };

            case 2: // Reverse
                return new int[] { 255 - dstR, 255 - dstG, 255 - dstB, 255 };

            case 3: // Ghost
                return blendColorsArr(srcR, srcG, srcB, dstR, dstG, dstB, 0.5f);

            case 4: // Not Copy
                return new int[] { 255 - srcR, 255 - srcG, 255 - srcB, (int)(srcA * alpha) };

            case 5: // Not Transparent
                return new int[] { 255 - srcR, 255 - srcG, 255 - srcB, (int)(srcA * alpha) };

            case 6: // Not Reverse
                return new int[] { dstR, dstG, dstB, 255 };

            case 7: // Not Ghost
                return blendColorsArr(255 - srcR, 255 - srcG, 255 - srcB, dstR, dstG, dstB, 0.5f);

            case 8: // Matte
            case 9: // Mask
                return new int[] { srcR, srcG, srcB, srcA };

            case 32: // Blend
                return blendColorsArr(srcR, srcG, srcB, dstR, dstG, dstB, alpha);

            case 33: // Add Pin
                return new int[] {
                    Math.min(255, dstR + (int)(srcR * alpha)),
                    Math.min(255, dstG + (int)(srcG * alpha)),
                    Math.min(255, dstB + (int)(srcB * alpha)),
                    255
                };

            case 34: // Add
                return new int[] {
                    (dstR + (int)(srcR * alpha)) & 0xFF,
                    (dstG + (int)(srcG * alpha)) & 0xFF,
                    (dstB + (int)(srcB * alpha)) & 0xFF,
                    255
                };

            case 35: // Subtract Pin
                return new int[] {
                    Math.max(0, dstR - (int)(srcR * alpha)),
                    Math.max(0, dstG - (int)(srcG * alpha)),
                    Math.max(0, dstB - (int)(srcB * alpha)),
                    255
                };

            case 36: // Background Transparent
                // If source matches background color, make transparent
                if (bgColor != null &&
                    Math.abs(srcR - bgColor[0]) < 8 &&
                    Math.abs(srcG - bgColor[1]) < 8 &&
                    Math.abs(srcB - bgColor[2]) < 8) {
                    return new int[] { srcR, srcG, srcB, 0 };
                }
                return new int[] { srcR, srcG, srcB, (int)(srcA * alpha) };

            case 37: // Lightest
                return new int[] {
                    Math.max(srcR, dstR),
                    Math.max(srcG, dstG),
                    Math.max(srcB, dstB),
                    255
                };

            case 38: // Subtract
                return new int[] {
                    (dstR - (int)(srcR * alpha)) & 0xFF,
                    (dstG - (int)(srcG * alpha)) & 0xFF,
                    (dstB - (int)(srcB * alpha)) & 0xFF,
                    255
                };

            case 39: // Darkest
                return new int[] {
                    Math.min(srcR, dstR),
                    Math.min(srcG, dstG),
                    Math.min(srcB, dstB),
                    255
                };

            case 40: // Darken
                // Use luminance comparison
                int srcLum = (int)(0.299 * srcR + 0.587 * srcG + 0.114 * srcB);
                int dstLum = (int)(0.299 * dstR + 0.587 * dstG + 0.114 * dstB);
                if (srcLum < dstLum) {
                    return new int[] { srcR, srcG, srcB, 255 };
                }
                return new int[] { dstR, dstG, dstB, 255 };

            case 41: // Lighten
                // Use luminance comparison
                int srcLum2 = (int)(0.299 * srcR + 0.587 * srcG + 0.114 * srcB);
                int dstLum2 = (int)(0.299 * dstR + 0.587 * dstG + 0.114 * dstB);
                if (srcLum2 > dstLum2) {
                    return new int[] { srcR, srcG, srcB, 255 };
                }
                return new int[] { dstR, dstG, dstB, 255 };

            default:
                // Unknown ink - default to copy
                return new int[] { srcR, srcG, srcB, (int)(srcA * alpha) };
        }
    }

    /**
     * Blend two colors and return as array.
     */
    private int[] blendColorsArr(int srcR, int srcG, int srcB,
                                  int dstR, int dstG, int dstB, float alpha) {
        float invAlpha = 1.0f - alpha;
        return new int[] {
            (int)(srcR * alpha + dstR * invAlpha),
            (int)(srcG * alpha + dstG * invAlpha),
            (int)(srcB * alpha + dstB * invAlpha),
            255
        };
    }
}
