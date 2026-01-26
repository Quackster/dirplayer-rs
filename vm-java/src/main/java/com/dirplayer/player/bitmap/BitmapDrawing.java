package com.dirplayer.player.bitmap;

import com.dirplayer.player.ColorRef;
import com.dirplayer.player.Sprite;
import com.dirplayer.rendering.CopyPixelsParams;
import com.dirplayer.rendering.IntRect;

import java.util.*;

/**
 * Bitmap drawing operations including copyPixels, fill, stroke, and line drawing.
 * Port of Rust vm-rust/src/player/bitmap/drawing.rs
 */
public class BitmapDrawing {

    // ========================================================================
    // Ink type constants
    // ========================================================================
    public static final int INK_COPY = 0;
    public static final int INK_NOT_GHOST = 7;
    public static final int INK_MATTE = 8;
    public static final int INK_MASK = 9;
    public static final int INK_ADD_PIN = 33;
    public static final int INK_BACKGROUND_TRANSPARENT = 36;
    public static final int INK_LIGHTEN = 40;
    public static final int INK_DARKEN = 41;

    // ========================================================================
    // Static utility methods
    // ========================================================================

    /**
     * Blend a single channel with alpha.
     */
    private static int blendAlpha(int dst, int src, float alpha) {
        return (int) (src * alpha + dst * (1.0f - alpha));
    }

    /**
     * Blend two colors with alpha.
     */
    public static int[] blendColorAlpha(int[] dst, int[] src, float alpha) {
        if (alpha == 0.0f) {
            return dst;
        } else if (alpha == 1.0f) {
            return src;
        }
        int r = blendAlpha(dst[0], src[0], alpha);
        int g = blendAlpha(dst[1], src[1], alpha);
        int b = blendAlpha(dst[2], src[2], alpha);
        return new int[]{r, g, b};
    }

    /**
     * Check if ink mode requires matte sprite processing.
     */
    public static boolean shouldMatteSprite(int ink) {
        return ink == 36 || ink == 33 || ink == 41 || ink == 8 || ink == 7;
    }

    /**
     * Director blend ink 0 implementation.
     */
    private static int[] directorBlendInk0(int[] dst, int[] src, float srcAlpha, float blend) {
        // Premultiply source by its own alpha
        float sr = src[0] * srcAlpha;
        float sg = src[1] * srcAlpha;
        float sb = src[2] * srcAlpha;

        float dr = dst[0];
        float dg = dst[1];
        float db = dst[2];

        float inv = 1.0f - blend;

        return new int[]{
            (int) Math.min(255, Math.max(0, Math.round(dr * inv + sr * blend))),
            (int) Math.min(255, Math.max(0, Math.round(dg * inv + sg * blend))),
            (int) Math.min(255, Math.max(0, Math.round(db * inv + sb * blend)))
        };
    }

    /**
     * Blend a pixel based on ink type.
     */
    public static int[] blendPixel(int[] dst, int[] src, int ink, int[] bgColor,
                                   float blendAlpha, float srcAlpha) {
        // Calculate the effective alpha: combination of native source alpha and blend parameter
        float effectiveAlpha = srcAlpha * blendAlpha;

        switch (ink) {
            // 0 = Copy (Director semantics: copy source over destination)
            case INK_COPY:
                if (blendAlpha >= 0.999f) {
                    // Normal copy, still respecting source alpha
                    if (srcAlpha >= 0.999f) {
                        return src;
                    } else {
                        return blendColorAlpha(dst, src, srcAlpha);
                    }
                } else {
                    return directorBlendInk0(dst, src, srcAlpha, blendAlpha);
                }

            // 7 = Not Ghost
            case INK_NOT_GHOST:
                return blendColorAlpha(dst, src, effectiveAlpha);

            // 8 = Matte
            case INK_MATTE:
                if (srcAlpha <= 0.001f) {
                    return dst;
                } else if (effectiveAlpha >= 0.999f) {
                    return src;
                } else {
                    return blendColorAlpha(dst, src, effectiveAlpha);
                }

            // 9 = Mask
            case INK_MASK:
                if (srcAlpha <= 0.001f) {
                    return dst;
                } else {
                    if (effectiveAlpha >= 0.999f) {
                        return src;
                    } else {
                        return blendColorAlpha(dst, src, effectiveAlpha);
                    }
                }

            // 33 = Add Pin (Director-style additive, pinned to 255)
            case INK_ADD_PIN:
                if (colorEquals(src, bgColor)) {
                    return dst;
                } else {
                    // Standard additive: add source to destination
                    int r = Math.min(255, dst[0] + src[0]);
                    int g = Math.min(255, dst[1] + src[1]);
                    int b = Math.min(255, dst[2] + src[2]);

                    // Apply blend factor
                    if (blendAlpha >= 0.999f) {
                        return new int[]{r, g, b};
                    } else {
                        return blendColorAlpha(dst, new int[]{r, g, b}, blendAlpha);
                    }
                }

            // 36 = Background Transparent
            case INK_BACKGROUND_TRANSPARENT:
                return blendColorAlpha(dst, src, effectiveAlpha);

            // 40 = Lighten
            case INK_LIGHTEN:
                if (colorEquals(src, bgColor)) {
                    return dst;
                } else {
                    return blendColorAlpha(dst, src, effectiveAlpha);
                }

            // 41 = Darken
            case INK_DARKEN:
                float rr = (src[0] / 255.0f) * (bgColor[0] / 255.0f) * 255.0f;
                float gg = (src[1] / 255.0f) * (bgColor[1] / 255.0f) * 255.0f;
                float bb = (src[2] / 255.0f) * (bgColor[2] / 255.0f) * 255.0f;
                int[] color = new int[]{(int) rr, (int) gg, (int) bb};
                return blendColorAlpha(dst, color, effectiveAlpha);

            default:
                return blendColorAlpha(dst, src, effectiveAlpha);
        }
    }

    /**
     * Compare two RGB colors for equality.
     */
    public static boolean colorEquals(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    // ========================================================================
    // Bitmap instance methods - pixel operations
    // ========================================================================

    /**
     * Get pixel color with alpha from a bitmap.
     */
    public static int[] getPixelColorWithAlpha(Bitmap bitmap, PaletteMap palettes, int x, int y) {
        ColorRef colorRef = getPixelColorRef(bitmap, x, y);
        int[] rgb = resolveColorRef(palettes, colorRef, bitmap.paletteRef, bitmap.originalBitDepth);

        if (bitmap.bitDepth == 32) {
            if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                int index = (y * bitmap.width + x) * 4;
                // The alpha component is the 4th byte for 32-bit data (R, G, B, A)
                int a = bitmap.data[index + 3] & 0xFF;
                return new int[]{rgb[0], rgb[1], rgb[2], a};
            }
        }
        // Default to fully opaque
        return new int[]{rgb[0], rgb[1], rgb[2], 0xFF};
    }

    /**
     * Set pixel color on a bitmap.
     */
    public static void setPixelColor(Bitmap bitmap, int x, int y, int[] color, PaletteMap palettes) {
        if (x < 0 || y < 0 || x >= bitmap.width || y >= bitmap.height) {
            return;
        }
        // Invalidate matte when pixels change - the matte mask needs to be regenerated
        bitmap.matte = null;
        int r = color[0];
        int g = color[1];
        int b = color[2];

        int bytesPerPixel = bitmap.bitDepth / 8;
        int index = (y * bitmap.width + x) * bytesPerPixel;

        switch (bitmap.bitDepth) {
            case 1: {
                int bitIndex = y * bitmap.width + x;
                int byteIndex = bitIndex / 8;
                int bitOffset = bitIndex % 8;
                byte value = bitmap.data[byteIndex];
                int mask = 1 << (7 - bitOffset);
                if (r > 127 || g > 127 || b > 127) {
                    value = (byte) (value | mask);
                } else {
                    value = (byte) (value & ~mask);
                }
                bitmap.data[byteIndex] = value;
                break;
            }
            case 4: {
                int resultIndex = 0;
                int resultDistance = Integer.MAX_VALUE;

                for (int paletteIdx = 0; paletteIdx < 16; paletteIdx++) {
                    int[] paletteColor = resolveColorRef(palettes,
                        ColorRef.paletteIndex(paletteIdx),
                        bitmap.paletteRef,
                        bitmap.originalBitDepth);
                    int distance = Math.abs(r - paletteColor[0])
                        + Math.abs(g - paletteColor[1])
                        + Math.abs(b - paletteColor[2]);
                    if (distance < resultDistance) {
                        resultIndex = paletteIdx;
                        resultDistance = distance;
                    }
                }
                bitmap.data[index] = (byte) resultIndex;
                break;
            }
            case 8: {
                int resultIndex = 0;
                int resultDistance = Integer.MAX_VALUE;

                for (int idx = 0; idx <= 255; idx++) {
                    int[] paletteColor = resolveColorRef(palettes,
                        ColorRef.paletteIndex(idx),
                        bitmap.paletteRef,
                        bitmap.originalBitDepth);
                    int distance = Math.abs(r - paletteColor[0])
                        + Math.abs(g - paletteColor[1])
                        + Math.abs(b - paletteColor[2]);
                    if (distance < resultDistance) {
                        resultIndex = idx;
                        resultDistance = distance;
                    }
                }
                bitmap.data[index] = (byte) resultIndex;
                break;
            }
            case 16: {
                float rf = r * 31.0f / 255.0f;
                float gf = g * 63.0f / 255.0f;
                float bf = b * 31.0f / 255.0f;
                int packed = packRgb565((int) rf, (int) gf, (int) bf);
                bitmap.data[index] = (byte) (packed & 0xFF);
                bitmap.data[index + 1] = (byte) ((packed >> 8) & 0xFF);
                break;
            }
            case 32: {
                bitmap.data[index] = (byte) r;
                bitmap.data[index + 1] = (byte) g;
                bitmap.data[index + 2] = (byte) b;
                bitmap.data[index + 3] = (byte) 0xFF;
                break;
            }
            default:
                System.out.println("Unsupported bit depth for setPixel: " + bitmap.bitDepth);
        }
    }

    /**
     * Get pixel color reference from a bitmap.
     */
    public static ColorRef getPixelColorRef(Bitmap bitmap, int x, int y) {
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
            return getBgColorRef(bitmap);
        }

        switch (bitmap.bitDepth) {
            case 4: {
                int bitIndex = (y * bitmap.width + x) * 4;
                int byteIndex = bitIndex / 8;
                byte value = bitmap.data[byteIndex];

                int nibble;
                if (x % 2 == 0) {
                    nibble = (value >> 4) & 0x0F; // High nibble (0-15)
                } else {
                    nibble = value & 0x0F; // Low nibble (0-15)
                }

                return ColorRef.paletteIndex(nibble);
            }
            case 8: {
                int index = y * bitmap.width + x;
                return ColorRef.paletteIndex(bitmap.data[index] & 0xFF);
            }
            case 16: {
                int index = (y * bitmap.width + x) * 2;
                int value = (bitmap.data[index] & 0xFF) | ((bitmap.data[index + 1] & 0xFF) << 8);
                int[] rgb = unpackRgb565(value);
                int red = (int) (rgb[0] / 31.0f * 255.0f);
                int green = (int) (rgb[1] / 63.0f * 255.0f);
                int blue = (int) (rgb[2] / 31.0f * 255.0f);
                return ColorRef.rgb(red, green, blue);
            }
            case 32: {
                int index = (y * bitmap.width + x) * 4;
                return ColorRef.rgb(
                    bitmap.data[index] & 0xFF,
                    bitmap.data[index + 1] & 0xFF,
                    bitmap.data[index + 2] & 0xFF
                );
            }
            default:
                return getBgColorRef(bitmap);
        }
    }

    /**
     * Get pixel color (resolved RGB) from a bitmap.
     */
    public static int[] getPixelColor(Bitmap bitmap, PaletteMap palettes, int x, int y) {
        ColorRef colorRef = getPixelColorRef(bitmap, x, y);
        return resolveColorRef(palettes, colorRef, bitmap.paletteRef, bitmap.originalBitDepth);
    }

    /**
     * Check if bitmap uses a palette.
     */
    public static boolean hasPalette(Bitmap bitmap) {
        return bitmap.bitDepth != 16 && bitmap.bitDepth != 32;
    }

    /**
     * Get background color reference for a bitmap.
     */
    public static ColorRef getBgColorRef(Bitmap bitmap) {
        if (hasPalette(bitmap)) {
            return ColorRef.paletteIndex(0);
        } else {
            return ColorRef.rgb(255, 255, 255);
        }
    }

    /**
     * Get foreground color reference for a bitmap.
     */
    public static ColorRef getFgColorRef(Bitmap bitmap) {
        if (hasPalette(bitmap)) {
            return ColorRef.paletteIndex(255);
        } else {
            return ColorRef.rgb(0, 0, 0);
        }
    }

    // ========================================================================
    // Flip operations
    // ========================================================================

    /**
     * Create a horizontally and vertically flipped copy of the bitmap.
     */
    public static Bitmap flippedHV(Bitmap bitmap, PaletteMap palettes) {
        Bitmap flipped = bitmap.copy();
        for (int y = 0; y < bitmap.height; y++) {
            for (int x = 0; x < bitmap.width; x++) {
                int dstX = bitmap.width - x - 1;
                int dstY = bitmap.height - y - 1;
                int[] srcColor = getPixelColor(bitmap, palettes, x, y);
                setPixelColor(flipped, dstX, dstY, srcColor, palettes);
            }
        }
        return flipped;
    }

    /**
     * Create a horizontally flipped copy of the bitmap.
     */
    public static Bitmap flippedH(Bitmap bitmap, PaletteMap palettes) {
        Bitmap flipped = bitmap.copy();
        for (int y = 0; y < bitmap.height; y++) {
            for (int x = 0; x < bitmap.width; x++) {
                int dstX = bitmap.width - x - 1;
                int[] srcColor = getPixelColor(bitmap, palettes, x, y);
                setPixelColor(flipped, dstX, y, srcColor, palettes);
            }
        }
        return flipped;
    }

    /**
     * Create a vertically flipped copy of the bitmap.
     */
    public static Bitmap flippedV(Bitmap bitmap, PaletteMap palettes) {
        Bitmap flipped = bitmap.copy();
        for (int y = 0; y < bitmap.height; y++) {
            for (int x = 0; x < bitmap.width; x++) {
                int dstY = bitmap.height - y - 1;
                int[] srcColor = getPixelColor(bitmap, palettes, x, y);
                setPixelColor(flipped, x, dstY, srcColor, palettes);
            }
        }
        return flipped;
    }

    // ========================================================================
    // Rectangle operations
    // ========================================================================

    /**
     * Stroke a sized rectangle.
     */
    public static void strokeSizedRect(Bitmap bitmap, int left, int top, int width, int height,
                                       int[] color, PaletteMap palettes, float alpha) {
        left = Math.max(0, left);
        top = Math.max(0, top);
        int right = left + width;
        int bottom = top + height;
        strokeRect(bitmap, left, top, right, bottom, color, palettes, alpha);
    }

    /**
     * Stroke (outline) a rectangle.
     */
    public static void strokeRect(Bitmap bitmap, int x1, int y1, int x2, int y2,
                                  int[] color, PaletteMap palettes, float alpha) {
        int left = x1;
        int top = y1;
        int right = x2 - 1;
        int bottom = y2 - 1;

        for (int x = x1; x < x2; x++) {
            int[] topColor = getPixelColor(bitmap, palettes, x, top);
            int[] bottomColor = getPixelColor(bitmap, palettes, x, bottom);
            int[] blendedTop = blendColorAlpha(topColor, color, alpha);
            int[] blendedBottom = blendColorAlpha(bottomColor, color, alpha);
            setPixelColor(bitmap, x, top, blendedTop, palettes);
            setPixelColor(bitmap, x, bottom, blendedBottom, palettes);
        }
        for (int y = y1; y < y2; y++) {
            int[] leftColor = getPixelColor(bitmap, palettes, left, y);
            int[] rightColor = getPixelColor(bitmap, palettes, right, y);
            int[] blendedLeft = blendColorAlpha(leftColor, color, alpha);
            int[] blendedRight = blendColorAlpha(rightColor, color, alpha);
            setPixelColor(bitmap, left, y, blendedLeft, palettes);
            setPixelColor(bitmap, right, y, blendedRight, palettes);
        }
    }

    /**
     * Clear a rectangle to a solid color.
     */
    public static void clearRect(Bitmap bitmap, int x1, int y1, int x2, int y2,
                                 int[] color, PaletteMap palettes) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                setPixelColor(bitmap, x, y, color, palettes);
            }
        }
    }

    /**
     * Clear a rectangular region with fully transparent pixels (alpha = 0).
     * Used for filmloop rendering where we need transparency instead of a solid background.
     */
    public static void clearRectTransparent(Bitmap bitmap, int x1, int y1, int x2, int y2) {
        // Only works for 32-bit bitmaps
        if (bitmap.bitDepth != 32) {
            return;
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (x < 0 || y < 0 || x >= bitmap.width || y >= bitmap.height) {
                    continue;
                }
                int index = (y * bitmap.width + x) * 4;
                // Set RGBA to (0, 0, 0, 0) - fully transparent black
                bitmap.data[index] = 0;
                bitmap.data[index + 1] = 0;
                bitmap.data[index + 2] = 0;
                bitmap.data[index + 3] = 0; // Alpha = 0 (transparent)
            }
        }
    }

    /**
     * Fill a relative rectangle.
     */
    public static void fillRelativeRect(Bitmap bitmap, int left, int top, int right, int bottom,
                                        int[] color, PaletteMap palettes, float alpha) {
        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(bitmap.width - 1, right);
        bottom = Math.min(bitmap.height - 1, bottom);

        int x1 = left;
        int y1 = top;
        int x2 = bitmap.width - right;
        int y2 = bitmap.height - bottom;

        fillRect(bitmap, x1, y1, x2, y2, color, palettes, alpha);
    }

    /**
     * Fill a rectangle with color and alpha blending.
     */
    public static void fillRect(Bitmap bitmap, int x1, int y1, int x2, int y2,
                                int[] color, PaletteMap palettes, float alpha) {
        if (alpha == 0.0f) {
            return;
        }
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int[] blendedColor;
                if (alpha == 1.0f) {
                    blendedColor = color;
                } else {
                    int[] dstColor = getPixelColor(bitmap, palettes, x, y);
                    blendedColor = blendColorAlpha(dstColor, color, alpha);
                }
                setPixelColor(bitmap, x, y, blendedColor, palettes);
            }
        }
    }

    // ========================================================================
    // CopyPixels operations
    // ========================================================================

    /**
     * Copy pixels with a map of parameters (Lingo-style).
     */
    public static void copyPixels(Bitmap dst, PaletteMap palettes, Bitmap src,
                                  IntRect dstRect, IntRect srcRect,
                                  Map<String, Object> paramList) {
        int blend = 100;
        if (paramList.containsKey("blend")) {
            Object blendObj = paramList.get("blend");
            if (blendObj instanceof Number) {
                blend = ((Number) blendObj).intValue();
            }
        }

        int ink = 0;
        if (paramList.containsKey("ink")) {
            Object inkObj = paramList.get("ink");
            if (inkObj instanceof Number) {
                ink = ((Number) inkObj).intValue();
            }
        }

        ColorRef bgColor = ColorRef.paletteIndex(0);
        if (paramList.containsKey("bgColor")) {
            Object bgObj = paramList.get("bgColor");
            if (bgObj instanceof ColorRef) {
                bgColor = (ColorRef) bgObj;
            }
        }

        ColorRef color = ColorRef.paletteIndex(255);
        if (paramList.containsKey("color")) {
            Object colorObj = paramList.get("color");
            if (colorObj instanceof ColorRef) {
                color = (ColorRef) colorObj;
            }
        }

        BitmapMask maskImage = null;
        if (paramList.containsKey("maskImage")) {
            Object maskObj = paramList.get("maskImage");
            if (maskObj instanceof BitmapMask) {
                maskImage = (BitmapMask) maskObj;
            }
        }

        float rotation = 0.0f;
        if (paramList.containsKey("rotation")) {
            Object rotObj = paramList.get("rotation");
            if (rotObj instanceof Number) {
                rotation = ((Number) rotObj).floatValue();
            }
        }

        boolean isTextRendering = false;
        if (paramList.containsKey("is_text_rendering")) {
            Object textObj = paramList.get("is_text_rendering");
            if (textObj instanceof Boolean) {
                isTextRendering = (Boolean) textObj;
            }
        }

        Sprite sprite = null;
        if (paramList.containsKey("sprite")) {
            Object spriteObj = paramList.get("sprite");
            if (spriteObj instanceof Sprite) {
                sprite = (Sprite) spriteObj;
            }
        }

        IntRect originalDstRect = null;
        if (paramList.containsKey("original_dst_rect")) {
            Object rectObj = paramList.get("original_dst_rect");
            if (rectObj instanceof IntRect) {
                originalDstRect = (IntRect) rectObj;
            }
        }

        // Text glyphs ALWAYS use Copy ink
        if (isTextRendering) {
            ink = 0;
        }

        CopyPixelsParams params = new CopyPixelsParams(
            blend, ink, color, bgColor, maskImage,
            isTextRendering, rotation, sprite, originalDstRect
        );
        copyPixelsWithParams(dst, palettes, src, dstRect, srcRect, params);
    }

    /**
     * Calculate bounding box for a rotated rectangle.
     */
    private static IntRect calculateRotatedBoundingBox(IntRect rect, double rotationDegrees,
                                                       int pivotX, int pivotY) {
        double theta = rotationDegrees * Math.PI / 180.0;
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double px = pivotX;
        double py = pivotY;

        // Define the 4 corners of the original rectangle
        double[][] corners = {
            {rect.left, rect.top},
            {rect.right, rect.top},
            {rect.right, rect.bottom},
            {rect.left, rect.bottom}
        };

        // Rotate each corner around the pivot point
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (double[] corner : corners) {
            double dx = corner[0] - px;
            double dy = corner[1] - py;

            double rotatedX = px + (dx * cosTheta - dy * sinTheta);
            double rotatedY = py + (dx * sinTheta + dy * cosTheta);

            minX = Math.min(minX, rotatedX);
            maxX = Math.max(maxX, rotatedX);
            minY = Math.min(minY, rotatedY);
            maxY = Math.max(maxY, rotatedY);
        }

        return IntRect.from((int) minX, (int) minY, (int) maxX, (int) maxY);
    }

    /**
     * Apply forecolor tint to a color.
     */
    private static int[] applyForecolorTint(int[] src, int[] fore) {
        return new int[]{
            (src[0] * fore[0]) / 255,
            (src[1] * fore[1]) / 255,
            (src[2] * fore[2]) / 255
        };
    }

    /**
     * Check if colorization is allowed for given depth and ink.
     */
    private static boolean allowsColorize(int depth, int ink, boolean isText) {
        if (isText) {
            return true; // text has its own rules
        }

        if (depth == 32 && ink == 0) return true;           // grayscale remap
        if (depth == 32 && (ink == 8 || ink == 9)) return true; // foreColor only
        if (depth <= 8 && ink == 0) return true;
        if (depth <= 8 && (ink == 8 || ink == 9)) return true;
        return false;
    }

    /**
     * Check if back color is used for given depth and ink.
     */
    private static boolean usesBackColor(int depth, int ink) {
        return ink == 0 && (depth == 32 || depth <= 8);
    }

    /**
     * Copy pixels from src to dst, respecting scaling, flipping, masks, blending, and rotation.
     */
    public static void copyPixelsWithParams(Bitmap dst, PaletteMap palettes, Bitmap src,
                                            IntRect dstRect, IntRect srcRect,
                                            CopyPixelsParams params) {
        int ink = params.ink;
        float alpha = params.blend / 100.0f;
        BitmapMask maskImage = params.maskImage;

        // Resolve background color
        int[] bgColorResolved;
        if (src.originalBitDepth == 32 && !src.useAlpha && ink != 0) {
            if (params.bgColor.isRgb()) {
                bgColorResolved = new int[]{params.bgColor.getR(), params.bgColor.getG(), params.bgColor.getB()};
            } else {
                // Director behavior: palette indices are ignored for 32-bit bgColor
                bgColorResolved = new int[]{255, 255, 255};
            }
        } else {
            bgColorResolved = resolveColorRef(palettes, params.bgColor, src.paletteRef, src.originalBitDepth);
        }

        int[] fgColorResolved = resolveColorRef(palettes, params.color, src.paletteRef, src.originalBitDepth);

        boolean isIndexed = src.originalBitDepth <= 8;

        int bgIndex = params.bgColor.isPaletteIndex() ? params.bgColor.getPaletteIndex() : 0;

        boolean isMatteBitmap = src.trimWhiteSpace || params.isTextRendering;

        boolean useGrayscaleAsAlpha = false;
        if (src.paletteRef != null && src.paletteRef.isBuiltIn()) {
            BuiltInPalette palette = src.paletteRef.getBuiltIn();
            if (palette != null && palette.toSymbol().equalsIgnoreCase("grayscale")) {
                useGrayscaleAsAlpha = true;
            }
        }

        // Setup destination bounds and flip flags
        int minDstX = Math.min(dstRect.left, dstRect.right);
        int maxDstX = Math.max(dstRect.left, dstRect.right);
        int minDstY = Math.min(dstRect.top, dstRect.bottom);
        int maxDstY = Math.max(dstRect.top, dstRect.bottom);
        boolean flipX = dstRect.right < dstRect.left;
        boolean flipY = dstRect.bottom < dstRect.top;

        // Scaling factors
        double dstW = maxDstX - minDstX;
        double dstH = maxDstY - minDstY;
        double srcW = srcRect.width();
        double srcH = srcRect.height();

        // For rotation: use original rect dimensions for scaling
        double scaleW, scaleH;
        if (params.originalDstRect != null) {
            scaleW = params.originalDstRect.right - params.originalDstRect.left;
            scaleH = params.originalDstRect.bottom - params.originalDstRect.top;
        } else {
            scaleW = dstW;
            scaleH = dstH;
        }

        double scaleX = srcW / scaleW;
        double scaleY = srcH / scaleH;

        double minDstXF = minDstX;
        double minDstYF = minDstY;
        double srcLeftF = srcRect.left;
        double srcTopF = srcRect.top;

        // Calculate sprite rotation pivot
        double centerX, centerY;
        if (params.sprite != null) {
            centerX = params.sprite.getLocH();
            centerY = params.sprite.getLocV();
        } else {
            centerX = (dstRect.left + dstRect.right) / 2.0;
            centerY = (dstRect.top + dstRect.bottom) / 2.0;
        }

        // Precompute rotation values
        boolean hasSpriteRotation = Math.abs(params.rotation) > 0.1;
        double cosTheta = 1.0;
        double sinTheta = 0.0;
        if (hasSpriteRotation) {
            double theta = -params.rotation * Math.PI / 180.0;
            cosTheta = Math.cos(theta);
            sinTheta = Math.sin(theta);
        }

        // Draw bounds (allow rotated overflow)
        int drawMinX, drawMaxX, drawMinY, drawMaxY;
        if (hasSpriteRotation && params.originalDstRect != null && params.sprite != null) {
            IntRect expanded = calculateRotatedBoundingBox(
                params.originalDstRect,
                params.rotation,
                params.sprite.getLocH(),
                params.sprite.getLocV()
            );
            drawMinX = Math.min(expanded.left, expanded.right);
            drawMaxX = Math.max(expanded.left, expanded.right);
            drawMinY = Math.min(expanded.top, expanded.bottom);
            drawMaxY = Math.max(expanded.top, expanded.bottom);
        } else {
            drawMinX = minDstX;
            drawMaxX = maxDstX;
            drawMinY = minDstY;
            drawMaxY = maxDstY;
        }

        boolean needsMatteMask =
            !params.isTextRendering
            && (ink == 8 || ink == 0)
            && isMatteBitmap
            && (src.originalBitDepth <= 8 || src.originalBitDepth == 32);

        boolean[][] matteMask = null;

        // 32-bit matte key: use edge color, NOT backColor
        int[] edgeMatteColor = null;
        if (src.originalBitDepth == 32 && !src.useAlpha) {
            int[] edgeRgba = getPixelColorWithAlpha(src, palettes, 0, 0);
            edgeMatteColor = new int[]{edgeRgba[0], edgeRgba[1], edgeRgba[2]};
        }

        if (needsMatteMask) {
            int width = src.width;
            int height = src.height;

            matteMask = new boolean[height][width];
            Deque<int[]> stack = new ArrayDeque<>();

            // Seed flood fill from edges
            for (int x = 0; x < width; x++) {
                int[] rgba1 = getPixelColorWithAlpha(src, palettes, x, 0);
                int[] rgb1 = new int[]{rgba1[0], rgba1[1], rgba1[2]};
                boolean isBg1 = edgeMatteColor != null ? colorEquals(rgb1, edgeMatteColor) : colorEquals(rgb1, bgColorResolved);
                if (isBg1) {
                    stack.push(new int[]{x, 0});
                }

                int[] rgba2 = getPixelColorWithAlpha(src, palettes, x, height - 1);
                int[] rgb2 = new int[]{rgba2[0], rgba2[1], rgba2[2]};
                boolean isBg2 = edgeMatteColor != null ? colorEquals(rgb2, edgeMatteColor) : colorEquals(rgb2, bgColorResolved);
                if (isBg2) {
                    stack.push(new int[]{x, height - 1});
                }
            }

            for (int y = 0; y < height; y++) {
                int[] rgba1 = getPixelColorWithAlpha(src, palettes, 0, y);
                int[] rgb1 = new int[]{rgba1[0], rgba1[1], rgba1[2]};
                boolean isBg1 = edgeMatteColor != null ? colorEquals(rgb1, edgeMatteColor) : colorEquals(rgb1, bgColorResolved);
                if (isBg1) {
                    stack.push(new int[]{0, y});
                }

                int[] rgba2 = getPixelColorWithAlpha(src, palettes, width - 1, y);
                int[] rgb2 = new int[]{rgba2[0], rgba2[1], rgba2[2]};
                boolean isBg2 = edgeMatteColor != null ? colorEquals(rgb2, edgeMatteColor) : colorEquals(rgb2, bgColorResolved);
                if (isBg2) {
                    stack.push(new int[]{width - 1, y});
                }
            }

            // Flood fill
            while (!stack.isEmpty()) {
                int[] pos = stack.pop();
                int x = pos[0];
                int y = pos[1];

                if (x < 0 || x >= width || y < 0 || y >= height) continue;
                if (matteMask[y][x]) continue;

                int[] rgba = getPixelColorWithAlpha(src, palettes, x, y);
                int[] rgb = new int[]{rgba[0], rgba[1], rgba[2]};
                boolean isBg = edgeMatteColor != null ? colorEquals(rgb, edgeMatteColor) : colorEquals(rgb, bgColorResolved);

                if (!isBg) continue;

                matteMask[y][x] = true;

                if (x > 0) stack.push(new int[]{x - 1, y});
                if (x + 1 < width) stack.push(new int[]{x + 1, y});
                if (y > 0) stack.push(new int[]{x, y - 1});
                if (y + 1 < height) stack.push(new int[]{x, y + 1});
            }
        }

        // Pixel loop
        for (int dstY = drawMinY; dstY < drawMaxY; dstY++) {
            for (int dstX = drawMinX; dstX < drawMaxX; dstX++) {
                if (dstX < 0 || dstY < 0 || dstX >= dst.width || dstY >= dst.height) {
                    continue;
                }

                // Apply rotation to destination coordinates
                double rotatedX, rotatedY;
                if (hasSpriteRotation) {
                    double dx = dstX - centerX;
                    double dy = dstY - centerY;
                    double rx = dx * cosTheta - dy * sinTheta;
                    double ry = dx * sinTheta + dy * cosTheta;
                    rotatedX = rx + centerX;
                    rotatedY = ry + centerY;
                } else {
                    rotatedX = dstX;
                    rotatedY = dstY;
                }

                // Calculate indices relative to destination rect
                double dstXIdx = rotatedX - minDstXF;
                double dstYIdx = rotatedY - minDstYF;

                // Check if rotated pixel is within destination bounds
                if (dstXIdx < 0 || dstXIdx >= dstW || dstYIdx < 0 || dstYIdx >= dstH) {
                    continue;
                }

                // Map destination pixel to source coordinate with scaling
                double srcFX = srcLeftF + (dstXIdx + 0.5) * scaleX;
                double srcFY = srcTopF + (dstYIdx + 0.5) * scaleY;

                // Handle horizontal flip
                double srcMappedX;
                if (flipX) {
                    double rel = srcFX - srcLeftF;
                    srcMappedX = srcLeftF + srcW - rel;
                } else {
                    srcMappedX = srcFX;
                }

                // Handle vertical flip
                double srcMappedY;
                if (flipY) {
                    double rel = srcFY - srcTopF;
                    srcMappedY = srcTopF + srcH - rel;
                } else {
                    srcMappedY = srcFY;
                }

                // Convert to integer sample coordinates
                int sx = (int) Math.floor(srcMappedX);
                int sy = (int) Math.floor(srcMappedY);

                int srcMaxX = srcRect.right - 1;
                int srcMaxY = srcRect.bottom - 1;

                if (srcRect.left > srcMaxX || srcRect.top > srcMaxY) {
                    continue;
                }

                sx = Math.max(srcRect.left, Math.min(sx, srcMaxX));
                sy = Math.max(srcRect.top, Math.min(sy, srcMaxY));

                if (sx >= src.width || sy >= src.height) {
                    continue;
                }

                // Indexed bitmap (1-8 bit) ink 0
                if (ink == 0 && isIndexed) {
                    // Check mask for trimWhiteSpace transparency
                    if (maskImage != null) {
                        if (maskImage.isTransparent(sx, sy)) {
                            continue;
                        }
                    }

                    ColorRef colorRef = getPixelColorRef(src, sx, sy);
                    int[] srcColor = resolveColorRef(palettes, colorRef, src.paletteRef, src.originalBitDepth);
                    setPixelColor(dst, dstX, dstY, srcColor, palettes);
                    continue;
                }

                // Indexed bitmap (1-8 bit) ink 36 color-key transparency
                if (ink == 36 && isIndexed) {
                    ColorRef colorRef = getPixelColorRef(src, sx, sy);
                    if (!colorRef.isPaletteIndex()) {
                        continue;
                    }
                    int i = colorRef.getPaletteIndex();

                    // For 1-bit bitmaps: use strict index-based transparency only
                    if (src.originalBitDepth == 1) {
                        if (i == 0) {
                            continue; // Background bit -> transparent
                        }
                        // Foreground bit -> render with foreColor
                        int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                        int[] blended;
                        if (alpha >= 0.999f) {
                            blended = fgColorResolved;
                        } else {
                            blended = blendColorAlpha(dstColor, fgColorResolved, alpha);
                        }
                        setPixelColor(dst, dstX, dstY, blended, palettes);
                        continue;
                    }

                    int transparentIndex = src.originalBitDepth <= 4 ? 0 : bgIndex;

                    // Fast path: check index match first
                    if (i == transparentIndex) {
                        int[] rgb = resolveColorRef(palettes, ColorRef.paletteIndex(i), src.paletteRef, src.originalBitDepth);
                        if (colorEquals(rgb, bgColorResolved)) {
                            continue;
                        }
                    }

                    int[] rgb = resolveColorRef(palettes, ColorRef.paletteIndex(i), src.paletteRef, src.originalBitDepth);
                    if (colorEquals(rgb, bgColorResolved)) {
                        continue;
                    }

                    float srcAlpha = 1.0f;

                    // For monochrome-style bitmaps with ink 36
                    int[] srcColor;
                    if (i == 255 || colorEquals(rgb, new int[]{0, 0, 0})) {
                        srcColor = fgColorResolved;
                    } else {
                        srcColor = rgb;
                    }

                    int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                    int[] blended;
                    if (srcAlpha >= 0.999f && alpha >= 0.999f) {
                        blended = srcColor;
                    } else {
                        blended = blendColorAlpha(dstColor, srcColor, srcAlpha * alpha);
                    }
                    setPixelColor(dst, dstX, dstY, blended, palettes);
                    continue;
                }

                // 16-bit bitmap ink 36 color-key transparency
                if (ink == 36 && src.originalBitDepth == 16) {
                    int[] rgba = getPixelColorWithAlpha(src, palettes, sx, sy);
                    int[] rgb = new int[]{rgba[0], rgba[1], rgba[2]};

                    if (colorEquals(rgb, bgColorResolved)) {
                        continue;
                    }

                    int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                    int[] blended;
                    if (alpha >= 0.999f) {
                        blended = rgb;
                    } else {
                        blended = blendColorAlpha(dstColor, rgb, alpha);
                    }
                    setPixelColor(dst, dstX, dstY, blended, palettes);
                    continue;
                }

                // Skip mask check for ink 33
                if (ink != 33) {
                    if (maskImage != null) {
                        if (maskImage.isTransparent(sx, sy)) {
                            continue;
                        }
                    }
                }

                // 16-bit bitmap ink 0
                if (ink == 0 && src.originalBitDepth == 16) {
                    int[] rgba = getPixelColorWithAlpha(src, palettes, sx, sy);
                    int[] srcColor = new int[]{rgba[0], rgba[1], rgba[2]};
                    int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);

                    int[] blended;
                    if (alpha >= 0.999f) {
                        blended = srcColor;
                    } else {
                        blended = blendColorAlpha(dstColor, srcColor, alpha);
                    }
                    setPixelColor(dst, dstX, dstY, blended, palettes);
                    continue;
                }

                // Indexed bitmap (1-8 bit) ink 8
                if (ink == 8 && isIndexed) {
                    ColorRef colorRef = getPixelColorRef(src, sx, sy);
                    int[] srcRgb = resolveColorRef(palettes, colorRef, src.paletteRef, src.originalBitDepth);

                    // Check matte mask
                    if (matteMask != null) {
                        if (matteMask[sy][sx]) {
                            continue;
                        }
                    }

                    float srcAlpha = 1.0f;
                    int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);

                    int[] blended;
                    if (srcAlpha >= 0.999f && alpha >= 0.999f) {
                        blended = srcRgb;
                    } else {
                        blended = blendColorAlpha(dstColor, srcRgb, srcAlpha * alpha);
                    }
                    setPixelColor(dst, dstX, dstY, blended, palettes);
                    continue;
                }

                // Sample source pixel
                int[] srcRgba = getPixelColorWithAlpha(src, palettes, sx, sy);
                int sr = srcRgba[0];
                int sg = srcRgba[1];
                int sb = srcRgba[2];
                int sa = srcRgba[3];

                // Skip fully transparent pixels from RGBA bitmaps
                if (src.originalBitDepth == 32 && src.useAlpha && sa == 0) {
                    continue;
                }

                int[] srcColor = new int[]{sr, sg, sb};

                // Director colorize (foreColor / backColor tweening)
                if (params.sprite != null) {
                    if (allowsColorize(src.originalBitDepth, ink, params.isTextRendering)) {
                        boolean hasFg = params.sprite.hasForeColor;
                        boolean hasBg = params.sprite.hasBackColor;

                        if (hasFg || hasBg) {
                            if (src.originalBitDepth == 32) {
                                // Treat source as grayscale intensity
                                int gray = (sr + sg + sb) / 3;

                                if (hasFg && hasBg && usesBackColor(32, ink)) {
                                    float t = gray / 255.0f;
                                    srcColor = new int[]{
                                        (int) ((1.0f - t) * fgColorResolved[0] + t * bgColorResolved[0]),
                                        (int) ((1.0f - t) * fgColorResolved[1] + t * bgColorResolved[1]),
                                        (int) ((1.0f - t) * fgColorResolved[2] + t * bgColorResolved[2])
                                    };
                                } else if (hasFg && gray <= 1) {
                                    srcColor = fgColorResolved;
                                }
                            } else {
                                // Indexed (<=8-bit)
                                ColorRef colorRef = getPixelColorRef(src, sx, sy);
                                if (colorRef.isPaletteIndex()) {
                                    int i = colorRef.getPaletteIndex();
                                    int max = (1 << src.originalBitDepth) - 1;
                                    float t = (float) i / max;

                                    if (hasFg && hasBg && usesBackColor(src.originalBitDepth, ink)) {
                                        srcColor = new int[]{
                                            (int) ((1.0f - t) * fgColorResolved[0] + t * bgColorResolved[0]),
                                            (int) ((1.0f - t) * fgColorResolved[1] + t * bgColorResolved[1]),
                                            (int) ((1.0f - t) * fgColorResolved[2] + t * bgColorResolved[2])
                                        };
                                    } else if (hasFg && i == 0) {
                                        srcColor = fgColorResolved;
                                    }
                                }
                            }
                        }
                    }
                }

                if (src.originalBitDepth == 32 && ink == 0 && !params.isTextRendering) {
                    if (!src.useAlpha) {
                        sa = 255;
                    } else if (sa == 0) {
                        continue;
                    }

                    if (src.trimWhiteSpace && colorEquals(srcColor, new int[]{255, 255, 255})) {
                        if (matteMask != null) {
                            if (matteMask[sy][sx]) {
                                continue;
                            }
                        }
                    }
                }

                if (src.originalBitDepth == 32 && ink == 8 && !params.isTextRendering) {
                    if (!src.useAlpha) {
                        if (matteMask != null) {
                            if (matteMask[sy][sx]) {
                                continue;
                            }
                        }
                        sa = 255;
                    }

                    float srcAlpha = src.useAlpha ? sa / 255.0f : 1.0f;
                    int[] finalSrcColor = srcColor.clone();

                    if (params.sprite != null && params.sprite.hasForeColor &&
                        !colorEquals(fgColorResolved, new int[]{0, 0, 0})) {
                        finalSrcColor = applyForecolorTint(finalSrcColor, fgColorResolved);
                    }

                    int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                    int[] blended;
                    if (srcAlpha >= 0.999f && alpha >= 0.999f) {
                        blended = finalSrcColor;
                    } else {
                        blended = blendColorAlpha(dstColor, finalSrcColor, srcAlpha * alpha);
                    }
                    setPixelColor(dst, dstX, dstY, blended, palettes);
                    continue;
                }

                // Director ink 36 alpha semantics
                if (ink == 36 && sa == 0 && src.originalBitDepth == 32) {
                    if (colorEquals(srcColor, bgColorResolved)) {
                        continue;
                    }
                    sa = 255;
                }

                // Skip background transparent ink
                if (!params.isTextRendering && sa == 255 && ink == 36 && colorEquals(srcColor, bgColorResolved)) {
                    continue;
                }

                // Matte/Mask grayscale white = transparent
                if (!params.isTextRendering && (ink == 8 || ink == 9) && useGrayscaleAsAlpha
                    && src.originalBitDepth <= 8 && colorEquals(srcColor, new int[]{255, 255, 255})) {
                    continue;
                }

                // Text rendering mode
                if (params.isTextRendering) {
                    // Black pixel -> foreground color
                    if (colorEquals(srcColor, new int[]{0, 0, 0})) {
                        int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                        int[] blended = blendPixel(dstColor, fgColorResolved, ink, bgColorResolved,
                            alpha, sa / 255.0f);
                        setPixelColor(dst, dstX, dstY, blended, palettes);
                    }
                    // White pixel -> fully transparent -> skip
                    continue;
                }

                // Non-text normal rendering
                float srcAlpha = sa / 255.0f;
                int[] dstColor = getPixelColor(dst, palettes, dstX, dstY);
                int[] blended = blendPixel(dstColor, srcColor, ink, bgColorResolved, alpha, srcAlpha);
                setPixelColor(dst, dstX, dstY, blended, palettes);
            }
        }
    }

    /**
     * Draw a bitmap at a location.
     */
    public static void drawBitmap(Bitmap dst, PaletteMap palettes, Bitmap src,
                                  int locH, int locV, int width, int height,
                                  int ink, int[] bgColor, float alpha) {
        Map<String, Object> params = new HashMap<>();
        params.put("blend", (int) (alpha * 100));
        params.put("ink", ink);
        params.put("bgColor", ColorRef.rgb(bgColor[0], bgColor[1], bgColor[2]));

        IntRect srcRect = IntRect.from(0, 0, src.width, src.height);
        IntRect dstRect = IntRect.from(locH, locV, locH + width, locV + height);
        copyPixels(dst, palettes, src, dstRect, srcRect, params);
    }

    // ========================================================================
    // Whitespace trimming
    // ========================================================================

    /**
     * Trim whitespace from a bitmap.
     */
    public static void trimWhitespace(Bitmap bitmap, PaletteMap palettes) {
        int left = 0;
        int top = 0;
        int right = bitmap.width;
        int bottom = bitmap.height;
        ColorRef bgColor = getBgColorRef(bitmap);

        // Find left edge
        for (int x = 0; x < bitmap.width; x++) {
            boolean isEmpty = true;
            for (int y = 0; y < bitmap.height; y++) {
                ColorRef color = getPixelColorRef(bitmap, x, y);
                if (!color.equals(bgColor)) {
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                left = x;
                break;
            }
        }

        // Find right edge
        for (int x = bitmap.width - 1; x >= 0; x--) {
            boolean isEmpty = true;
            for (int y = 0; y < bitmap.height; y++) {
                ColorRef color = getPixelColorRef(bitmap, x, y);
                if (!color.equals(bgColor)) {
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                right = x + 1;
                break;
            }
        }

        // Find top edge
        for (int y = 0; y < bitmap.height; y++) {
            boolean isEmpty = true;
            for (int x = 0; x < bitmap.width; x++) {
                ColorRef color = getPixelColorRef(bitmap, x, y);
                if (!color.equals(bgColor)) {
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                top = y;
                break;
            }
        }

        // Find bottom edge
        for (int y = bitmap.height - 1; y >= 0; y--) {
            boolean isEmpty = true;
            for (int x = 0; x < bitmap.width; x++) {
                ColorRef color = getPixelColorRef(bitmap, x, y);
                if (!color.equals(bgColor)) {
                    isEmpty = false;
                    break;
                }
            }
            if (!isEmpty) {
                bottom = y + 1;
                break;
            }
        }

        int newWidth = right - left;
        int newHeight = bottom - top;

        Bitmap trimmed = new Bitmap(newWidth, newHeight, bitmap.bitDepth,
            bitmap.originalBitDepth, 0, bitmap.paletteRef);

        CopyPixelsParams params = new CopyPixelsParams();
        params.color = getFgColorRef(bitmap);
        params.bgColor = getBgColorRef(bitmap);

        copyPixelsWithParams(trimmed, palettes, bitmap,
            IntRect.from(0, 0, newWidth, newHeight),
            IntRect.from(left, top, right, bottom),
            params);

        bitmap.width = newWidth;
        bitmap.height = newHeight;
        bitmap.data = trimmed.data;
    }

    /**
     * Convert a bitmap to a mask.
     */
    public static BitmapMask toMask(Bitmap bitmap) {
        BitmapMask mask = new BitmapMask(bitmap.width, bitmap.height, false);
        ColorRef bgColor = getBgColorRef(bitmap);

        for (int y = 0; y < bitmap.height; y++) {
            for (int x = 0; x < bitmap.width; x++) {
                ColorRef pixel = getPixelColorRef(bitmap, x, y);
                if (!pixel.equals(bgColor)) {
                    mask.setTransparent(x, y, false); // Not transparent = opaque
                }
            }
        }
        return mask;
    }

    // ========================================================================
    // Flood fill
    // ========================================================================

    /**
     * Flood fills starting from a point, replacing the original color with the target color.
     * Emulates Director's image.floodFill(point, color) behavior.
     */
    public static void floodFill(Bitmap bitmap, int startX, int startY,
                                 int[] targetColor, PaletteMap palettes) {
        // Bounds check
        if (startX < 0 || startY < 0 || startX >= bitmap.width || startY >= bitmap.height) {
            return;
        }

        // Capture the original color at the starting pixel
        int[] originalColor = getPixelColor(bitmap, palettes, startX, startY);

        // If the starting color is already the target color, nothing to fill
        if (colorEquals(originalColor, targetColor)) {
            return;
        }

        Deque<int[]> stack = new ArrayDeque<>(256);
        Set<Long> visited = new HashSet<>(256);

        stack.push(new int[]{startX, startY});
        visited.add(packCoord(startX, startY));

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int x = pos[0];
            int y = pos[1];

            // Bounds check
            if (x < 0 || y < 0 || x >= bitmap.width || y >= bitmap.height) {
                continue;
            }

            // Check current pixel color
            int[] currentColor = getPixelColor(bitmap, palettes, x, y);

            // Only fill if the color matches the original color
            if (!colorEquals(currentColor, originalColor)) {
                continue;
            }

            // Set pixel to target color
            setPixelColor(bitmap, x, y, targetColor, palettes);

            // Push 4-connected neighbors
            int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
            for (int[] neighbor : neighbors) {
                int nx = neighbor[0];
                int ny = neighbor[1];
                if (nx >= 0 && ny >= 0 && nx < bitmap.width && ny < bitmap.height) {
                    long key = packCoord(nx, ny);
                    if (!visited.contains(key)) {
                        visited.add(key);
                        stack.push(new int[]{nx, ny});
                    }
                }
            }
        }
    }

    private static long packCoord(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    // ========================================================================
    // Shape fill with sprite
    // ========================================================================

    /**
     * Fill a shape rectangle using sprite properties.
     */
    public static void fillShapeRectWithSprite(Bitmap bitmap, Sprite sprite,
                                               IntRect dstRect, PaletteMap palettes) {
        // Create a temporary 1x1 bitmap representing the foreground color
        Bitmap temp = new Bitmap(1, 1, bitmap.bitDepth, bitmap.originalBitDepth, 0, bitmap.paletteRef);

        // Resolve sprite.color (foreground)
        int[] fgRgb = resolveColorRef(palettes, sprite.getColor(),
            PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin), bitmap.originalBitDepth);
        setPixelColor(temp, 0, 0, fgRgb, palettes);

        // Build Director-style copy_pixels parameters
        Map<String, Object> params = new HashMap<>();
        params.put("blend", sprite.getBlend());
        params.put("ink", sprite.getInk());
        params.put("color", sprite.getColor());
        params.put("bgColor", sprite.getBgColor());

        // Copy the 1x1 bitmap over the rectangle
        copyPixels(bitmap, palettes, temp, dstRect, IntRect.from(0, 0, 1, 1), params);
    }

    // ========================================================================
    // Color resolution utilities
    // ========================================================================

    /**
     * Resolve a ColorRef to RGB values using the palette map.
     */
    public static int[] resolveColorRef(PaletteMap palettes, ColorRef colorRef,
                                        PaletteRef paletteRef, int originalBitDepth) {
        if (colorRef == null) {
            return new int[]{255, 255, 255};
        }

        if (colorRef.isRgb()) {
            return new int[]{colorRef.getR(), colorRef.getG(), colorRef.getB()};
        }

        // Palette index
        int index = colorRef.getPaletteIndex();
        if (palettes != null) {
            int[] color = palettes.getColor(paletteRef, index, originalBitDepth);
            if (color != null) {
                return color;
            }
        }

        // Fallback: return the index as grayscale
        return new int[]{index, index, index};
    }

    // ========================================================================
    // RGB565 utilities
    // ========================================================================

    /**
     * Pack RGB values into RGB565 format.
     */
    private static int packRgb565(int r5, int g6, int b5) {
        return ((r5 & 0x1F) << 11) | ((g6 & 0x3F) << 5) | (b5 & 0x1F);
    }

    /**
     * Unpack RGB565 format to RGB components.
     */
    private static int[] unpackRgb565(int rgb565) {
        int r = (rgb565 >> 11) & 0x1F;
        int g = (rgb565 >> 5) & 0x3F;
        int b = rgb565 & 0x1F;
        return new int[]{r, g, b};
    }

    // ========================================================================
    // Default CopyPixelsParams factory
    // ========================================================================

    /**
     * Create default CopyPixelsParams for a bitmap.
     */
    public static CopyPixelsParams defaultParams(Bitmap bitmap) {
        CopyPixelsParams params = new CopyPixelsParams();
        params.blend = 100;
        params.ink = 0;
        params.color = getFgColorRef(bitmap);
        params.bgColor = getBgColorRef(bitmap);
        params.maskImage = null;
        params.isTextRendering = false;
        params.rotation = 0.0f;
        params.sprite = null;
        params.originalDstRect = null;
        return params;
    }
}
