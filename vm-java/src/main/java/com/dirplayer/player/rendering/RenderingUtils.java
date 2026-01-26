package com.dirplayer.player.rendering;

import com.dirplayer.player.CastManager;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.FontManager;
import com.dirplayer.player.FontManager.BitmapFont;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.score.SpriteKeyframe.PathKeyframe;
import com.dirplayer.player.score.SpritePathKeyframes;
import com.dirplayer.rendering.CopyPixelsParams;
import com.dirplayer.rendering.IntRect;
import com.dirplayer.rendering.ScoreRef;
import com.dirplayer.director.MemberType;
import com.dirplayer.SimpleLogger;


import java.util.List;

/**
 * Utility methods for rendering operations.
 * Port of helper functions from Rust rendering.rs.
 */
public final class RenderingUtils {
    private static final SimpleLogger logger = SimpleLogger.getLogger(RenderingUtils.class);

    private RenderingUtils() {
        // Prevent instantiation
    }

    /**
     * Interpolate path position between keyframes for filmloop animation.
     * Returns interpolated (x, y) position for the given frame, or null if no interpolation is needed.
     *
     * @param pathKeyframes The path keyframes
     * @param frame The current frame number
     * @return int array [x, y] or null
     */
    public static int[] interpolatePathPosition(SpritePathKeyframes pathKeyframes, int frame) {
        List<PathKeyframe> keyframes = pathKeyframes.getKeyframes();
        if (keyframes.isEmpty()) {
            return null;
        }

        // Find the keyframe pair surrounding the current frame
        PathKeyframe prevKf = null;
        PathKeyframe nextKf = null;

        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                prevKf = keyframes.get(i);
                break;
            }
        }

        for (PathKeyframe kf : keyframes) {
            if (kf.frame > frame) {
                nextKf = kf;
                break;
            }
        }

        if (prevKf != null && nextKf != null) {
            // Interpolate between prev and next keyframes
            int frameRange = nextKf.frame - prevKf.frame;
            if (frameRange == 0) {
                return new int[] { prevKf.x, prevKf.y };
            }
            float t = (float)(frame - prevKf.frame) / frameRange;
            int x = (int)(prevKf.x + (nextKf.x - prevKf.x) * t);
            int y = (int)(prevKf.y + (nextKf.y - prevKf.y) * t);
            return new int[] { x, y };
        } else if (prevKf != null) {
            // Past the last keyframe - use last keyframe position
            return new int[] { prevKf.x, prevKf.y };
        } else {
            // Before the first keyframe - return null to use channel_initialization_data position
            return null;
        }
    }

    /**
     * Get or load a font for rendering.
     *
     * @param fontManager The font manager
     * @param castManager The cast manager
     * @param fontName The font name
     * @param fontSize Optional font size
     * @param fontStyle Optional font style
     * @return The bitmap font or null
     */
    public static BitmapFont getOrLoadFont(
            FontManager fontManager,
            CastManager castManager,
            String fontName,
            Integer fontSize,
            Integer fontStyle) {

        if (fontName == null || fontName.isEmpty() || "System".equals(fontName)) {
            return fontManager.getSystemFont();
        }

        String cacheKey = String.format("%s_%d_%d",
            fontName,
            fontSize != null ? fontSize : 0,
            fontStyle != null ? fontStyle : 0);

        logger.debug("Looking for font: '{}' (key: '{}')", fontName, cacheKey);

        // Check cache by key
        BitmapFont font = fontManager.getFont(cacheKey);
        if (font != null) {
            logger.debug("Found in cache: '{}'", cacheKey);
            return font;
        }

        // Check cache by name
        font = fontManager.getFont(fontName);
        if (font != null) {
            logger.debug("Found by name: '{}'", fontName);
            return font;
        }

        logger.debug("Font '{}' not in cache, falling back to system font", fontName);
        return fontManager.getSystemFont();
    }

    /**
     * Draw cursor on bitmap.
     *
     * @param player The player
     * @param bitmap The destination bitmap
     * @param palettes The palette map
     */
    public static void drawCursor(DirPlayer player, Bitmap bitmap, PaletteMap palettes) {
        // Get hovered sprite cursor or default cursor
        int hoveredSpriteNum = getSpriteAt(player, player.mouseLocX, player.mouseLocY, false);

        var cursorRef = player.cursor;
        if (hoveredSpriteNum > 0) {
            Sprite hoveredSprite = player.getMovie().getScore().getSprite((short)hoveredSpriteNum);
            if (hoveredSprite != null && hoveredSprite.cursor != null) {
                cursorRef = hoveredSprite.cursor;
            }
        }

        // Check if cursor is a member cursor
        if (cursorRef == null || !cursorRef.isMember()) {
            return;
        }

        List<Integer> cursorList = cursorRef.getMemberList();
        if (cursorList == null || cursorList.isEmpty()) {
            return;
        }

        // Get cursor bitmap member
        int cursorSlot = cursorList.get(0);
        CastMember cursorMember = player.getMovie().getCastManager().findMemberBySlotNumber(cursorSlot);
        if (cursorMember == null || cursorMember.getMemberType() != MemberType.Bitmap) {
            return;
        }

        int cursorImageRef = cursorMember.getImageRef();
        Bitmap cursorBitmap = player.getBitmapManager().getBitmap(cursorImageRef);
        if (cursorBitmap == null) {
            return;
        }

        // Get optional mask bitmap
        BitmapMask mask = null;
        if (cursorList.size() > 1) {
            int maskSlot = cursorList.get(1);
            CastMember maskMember = player.getMovie().getCastManager().findMemberBySlotNumber(maskSlot);
            if (maskMember != null && maskMember.getMemberType() == MemberType.Bitmap) {
                Bitmap maskBitmap = player.getBitmapManager().getBitmap(maskMember.getImageRef());
                if (maskBitmap != null) {
                    mask = maskBitmap.toMask();
                }
            }
        }

        // Draw cursor at mouse position
        int regX = cursorMember.getRegPointX();
        int regY = cursorMember.getRegPointY();

        CopyPixelsParams params = new CopyPixelsParams(
            100,  // blend
            41,   // ink (special cursor ink)
            bitmap.getFgColorRef(),
            bitmap.getBgColorRef(),
            mask,
            false,  // isTextRendering
            0.0f,   // rotation
            null,   // sprite
            null    // originalDstRect
        );

        bitmap.copyPixelsWithParams(
            palettes,
            cursorBitmap,
            IntRect.fromSize(
                player.mouseLocX - regX,
                player.mouseLocY - regY,
                cursorBitmap.getWidth(),
                cursorBitmap.getHeight()
            ),
            IntRect.fromSize(0, 0, cursorBitmap.getWidth(), cursorBitmap.getHeight()),
            params
        );
    }

    /**
     * Get sprite at the specified position.
     *
     * @param player The player
     * @param x X coordinate
     * @param y Y coordinate
     * @param ignoreInvisible Whether to ignore invisible sprites
     * @return The sprite number or 0 if none
     */
    public static int getSpriteAt(DirPlayer player, int x, int y, boolean ignoreInvisible) {
        var score = player.getMovie().getScore();
        var channels = score.getSortedChannels(player.getMovie().getCurrentFrame());

        // Iterate in reverse for z-order (top to bottom)
        for (int i = channels.size() - 1; i >= 0; i--) {
            var channel = channels.get(i);
            Sprite sprite = channel.sprite;

            if (ignoreInvisible && !sprite.visible) {
                continue;
            }

            if (sprite.containsPoint(x, y)) {
                return sprite.number;
            }
        }

        return 0;
    }

    /**
     * Get the concrete sprite rect for rendering.
     *
     * @param player The player
     * @param sprite The sprite
     * @return The sprite's bounding rectangle
     */
    public static IntRect getConcreteSpriteRect(DirPlayer player, Sprite sprite) {
        int locH = sprite.getLocH();
        int locV = sprite.getLocV();
        int width = sprite.getWidth();
        int height = sprite.getHeight();

        // Get registration point from member if available
        CastMemberRef memberRef = sprite.getMember();
        int regX = width / 2;
        int regY = height / 2;

        if (memberRef != null) {
            CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
            if (member != null && member.getMemberType() == MemberType.Bitmap) {
                regX = member.getRegPointX();
                regY = member.getRegPointY();
            }
        }

        return IntRect.from(
            locH - regX,
            locV - regY,
            locH - regX + width,
            locV - regY + height
        );
    }

    /**
     * Check if the sprite ink requires a matte mask.
     *
     * @param ink The ink value
     * @return true if matte should be used
     */
    public static boolean shouldMatteSprite(int ink) {
        return ink == InkEffect.COPY || ink == InkEffect.MATTE;
    }

    /**
     * Resolve a color reference to RGB values.
     *
     * @param palettes The palette map
     * @param colorRef The color reference
     * @param paletteRef The palette reference
     * @param bitDepth The bit depth
     * @return RGB values as int array [r, g, b]
     */
    public static int[] resolveColorRef(
            PaletteMap palettes,
            ColorRef colorRef,
            PaletteRef paletteRef,
            int bitDepth) {

        if (colorRef == null) {
            return new int[] { 0, 0, 0 };
        }

        if (colorRef.isRgb()) {
            return new int[] { colorRef.getR(), colorRef.getG(), colorRef.getB() };
        }

        // Palette index
        int index = colorRef.getPaletteIndex();
        int[][] palette = palettes.getPalette(paletteRef, bitDepth);
        if (palette != null && index >= 0 && index < palette.length) {
            return palette[index];
        }

        return new int[] { 0, 0, 0 };
    }

    /**
     * Get the system default palette.
     *
     * @return The system default palette
     */
    public static BuiltInPalette getSystemDefaultPalette() {
        return BuiltInPalette.SystemWin;
    }

    /**
     * Apply flip transformation to a rectangle.
     *
     * @param rect The original rectangle
     * @param flipH Horizontal flip
     * @param flipV Vertical flip
     * @return The transformed rectangle
     */
    public static IntRect applyFlip(IntRect rect, boolean flipH, boolean flipV) {
        return IntRect.from(
            flipH ? rect.right : rect.left,
            flipV ? rect.bottom : rect.top,
            flipH ? rect.left : rect.right,
            flipV ? rect.top : rect.bottom
        );
    }

    /**
     * Get channel number from channel index (for score data).
     * Channel indices 0-5 are effect channels, 6+ are sprite channels.
     *
     * @param channelIndex The channel index
     * @return The channel number
     */
    public static int getChannelNumberFromIndex(int channelIndex) {
        return channelIndex - 5;
    }

    /**
     * Convert blend value (0-254) to percentage (0-100).
     * Blend 0 means default (100%), 1-254 maps to 0-100%.
     *
     * @param blendValue The blend value from score data
     * @return The blend percentage
     */
    public static int convertBlendToPercentage(int blendValue) {
        if (blendValue == 0) {
            return 100;  // Default is fully opaque
        }
        return blendValue;
    }

    /**
     * Check if a bitmap should use matte for transparency based on ink and bit depth.
     * Port of should_matte_sprite logic from Rust.
     *
     * @param ink The ink value
     * @param originalBitDepth The original bit depth of the bitmap
     * @return true if matte should be used
     */
    public static boolean shouldUseMatte(int ink, int originalBitDepth) {
        boolean isIndexed = originalBitDepth <= 8;
        boolean is16bit = originalBitDepth == 16;

        // Only use matte mask for inks that support it:
        // - Ink 0 (copy): for trimWhiteSpace edge transparency (indexed and 16-bit)
        // - Ink 8 (matte): always uses matte (indexed only)
        // - Ink 7, 36 (color-key): do NOT use matte - they have their own
        //   bgColor-based transparency that conflicts with matte logic
        return (isIndexed && (ink == InkEffect.COPY || ink == InkEffect.MATTE))
            || (is16bit && ink == InkEffect.COPY);
    }

    /**
     * Check if the specified ink requires alpha blending.
     *
     * @param ink The ink value
     * @return true if the ink uses alpha blending
     */
    public static boolean inkRequiresBlending(int ink) {
        return ink >= InkEffect.BLEND;
    }

    /**
     * Check if ink is a "NOT" variant that inverts colors.
     *
     * @param ink The ink value
     * @return true if the ink inverts colors
     */
    public static boolean isNotInk(int ink) {
        return ink == InkEffect.NOT_COPY ||
               ink == InkEffect.NOT_TRANSPARENT ||
               ink == InkEffect.NOT_REVERSE ||
               ink == InkEffect.NOT_GHOST;
    }

    /**
     * Get the score sprite for a given channel number.
     *
     * @param movie The movie
     * @param scoreRef The score reference
     * @param channelNum The channel number
     * @return The sprite or null
     */
    public static Sprite getScoreSprite(
            com.dirplayer.player.Movie movie,
            ScoreRef scoreRef,
            int channelNum) {
        if (scoreRef.isStage()) {
            return movie.getScore().getSprite((short) channelNum);
        } else if (scoreRef.isFilmLoop()) {
            // For film loops, sprites are accessed differently
            // through channel initialization data
            CastMemberRef memberRef = scoreRef.getMemberRef();
            CastMember member = movie.getCastManager().findMemberByRef(memberRef);
            if (member != null && member.getMemberType() == MemberType.FilmLoop) {
                com.dirplayer.player.cast.FilmLoopMember filmLoop =
                    (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                if (filmLoop != null && filmLoop.getScore() != null) {
                    return filmLoop.getScore().getSprite((short) channelNum);
                }
            }
        }
        return null;
    }

    /**
     * Clamp a value between min and max.
     *
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a float value between min and max.
     *
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static float clampf(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Interpolate between two integer values.
     *
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0.0 to 1.0)
     * @return The interpolated value
     */
    public static int lerp(int a, int b, float t) {
        return (int)(a + (b - a) * t);
    }

    /**
     * Calculate the luminance of an RGB color.
     * Used for comparison in lightest/darkest ink effects.
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return The luminance value
     */
    public static int luminance(int r, int g, int b) {
        // Standard luminance calculation
        return (int)(0.299 * r + 0.587 * g + 0.114 * b);
    }

    /**
     * Apply ink effect to a source color given a destination color.
     * This is the core ink blending function.
     *
     * @param ink The ink effect
     * @param srcR Source red
     * @param srcG Source green
     * @param srcB Source blue
     * @param srcA Source alpha
     * @param dstR Destination red
     * @param dstG Destination green
     * @param dstB Destination blue
     * @param blend Blend percentage (0-100)
     * @param fgColor Foreground color [r, g, b]
     * @param bgColor Background color [r, g, b]
     * @return Result color as [r, g, b, a]
     */
    public static int[] applyInkEffect(
            int ink,
            int srcR, int srcG, int srcB, int srcA,
            int dstR, int dstG, int dstB,
            int blend,
            int[] fgColor, int[] bgColor) {

        float alpha = blend / 100.0f;

        switch (ink) {
            case InkEffect.COPY:
                return new int[] { srcR, srcG, srcB, srcA };

            case InkEffect.TRANSPARENT:
                return new int[] { srcR, srcG, srcB, (int)(srcA * alpha) };

            case InkEffect.REVERSE:
                return new int[] { 255 - dstR, 255 - dstG, 255 - dstB, 255 };

            case InkEffect.GHOST:
                return blendColors(srcR, srcG, srcB, dstR, dstG, dstB, 0.5f);

            case InkEffect.NOT_COPY:
                return new int[] { 255 - srcR, 255 - srcG, 255 - srcB, srcA };

            case InkEffect.NOT_TRANSPARENT:
                return new int[] { 255 - srcR, 255 - srcG, 255 - srcB, (int)(srcA * alpha) };

            case InkEffect.NOT_REVERSE:
                return new int[] { dstR, dstG, dstB, 255 };

            case InkEffect.NOT_GHOST:
                return blendColors(255 - srcR, 255 - srcG, 255 - srcB, dstR, dstG, dstB, 0.5f);

            case InkEffect.MATTE:
            case InkEffect.MASK:
                return new int[] { srcR, srcG, srcB, srcA };

            case InkEffect.BLEND:
                return blendColors(srcR, srcG, srcB, dstR, dstG, dstB, alpha);

            case InkEffect.ADD_PIN:
                return new int[] {
                    clamp(dstR + (int)(srcR * alpha), 0, 255),
                    clamp(dstG + (int)(srcG * alpha), 0, 255),
                    clamp(dstB + (int)(srcB * alpha), 0, 255),
                    255
                };

            case InkEffect.ADD:
                return new int[] {
                    (dstR + (int)(srcR * alpha)) & 0xFF,
                    (dstG + (int)(srcG * alpha)) & 0xFF,
                    (dstB + (int)(srcB * alpha)) & 0xFF,
                    255
                };

            case InkEffect.SUBTRACT_PIN:
                return new int[] {
                    clamp(dstR - (int)(srcR * alpha), 0, 255),
                    clamp(dstG - (int)(srcG * alpha), 0, 255),
                    clamp(dstB - (int)(srcB * alpha), 0, 255),
                    255
                };

            case InkEffect.BACKGROUND_TRANSPARENT:
                // If source color matches background, make transparent
                if (bgColor != null &&
                    srcR == bgColor[0] && srcG == bgColor[1] && srcB == bgColor[2]) {
                    return new int[] { srcR, srcG, srcB, 0 };
                }
                return new int[] { srcR, srcG, srcB, srcA };

            case InkEffect.LIGHTEST:
                return new int[] {
                    Math.max(srcR, dstR),
                    Math.max(srcG, dstG),
                    Math.max(srcB, dstB),
                    255
                };

            case InkEffect.SUBTRACT:
                return new int[] {
                    (dstR - (int)(srcR * alpha)) & 0xFF,
                    (dstG - (int)(srcG * alpha)) & 0xFF,
                    (dstB - (int)(srcB * alpha)) & 0xFF,
                    255
                };

            case InkEffect.DARKEST:
                return new int[] {
                    Math.min(srcR, dstR),
                    Math.min(srcG, dstG),
                    Math.min(srcB, dstB),
                    255
                };

            case InkEffect.DARKEN:
                // Similar to darkest but uses luminance comparison
                if (luminance(srcR, srcG, srcB) < luminance(dstR, dstG, dstB)) {
                    return new int[] { srcR, srcG, srcB, 255 };
                }
                return new int[] { dstR, dstG, dstB, 255 };

            case InkEffect.LIGHTEN:
                // Similar to lightest but uses luminance comparison
                if (luminance(srcR, srcG, srcB) > luminance(dstR, dstG, dstB)) {
                    return new int[] { srcR, srcG, srcB, 255 };
                }
                return new int[] { dstR, dstG, dstB, 255 };

            default:
                // Unknown ink, default to copy
                return new int[] { srcR, srcG, srcB, srcA };
        }
    }

    /**
     * Blend two colors together.
     *
     * @param srcR Source red
     * @param srcG Source green
     * @param srcB Source blue
     * @param dstR Destination red
     * @param dstG Destination green
     * @param dstB Destination blue
     * @param alpha Blend factor (0.0 = all dst, 1.0 = all src)
     * @return Blended color as [r, g, b, a]
     */
    private static int[] blendColors(int srcR, int srcG, int srcB,
                                     int dstR, int dstG, int dstB, float alpha) {
        float invAlpha = 1.0f - alpha;
        return new int[] {
            (int)(srcR * alpha + dstR * invAlpha),
            (int)(srcG * alpha + dstG * invAlpha),
            (int)(srcB * alpha + dstB * invAlpha),
            255
        };
    }

    /**
     * Get the rectangle for a sprite, accounting for registration point.
     * This is a simplified version for when we don't need the full member lookup.
     *
     * @param sprite The sprite
     * @return The sprite's bounding rectangle
     */
    public static IntRect getSimpleSpriteRect(Sprite sprite) {
        int locH = sprite.getLocH();
        int locV = sprite.getLocV();
        int width = sprite.getWidth();
        int height = sprite.getHeight();

        // Default registration point is center
        int regX = width / 2;
        int regY = height / 2;

        return IntRect.from(
            locH - regX,
            locV - regY,
            locH - regX + width,
            locV - regY + height
        );
    }

    /**
     * Check if a point is within a sprite's bounds.
     *
     * @param sprite The sprite
     * @param x X coordinate
     * @param y Y coordinate
     * @param rect The sprite's rectangle (or null to compute)
     * @return true if the point is within the sprite
     */
    public static boolean pointInSprite(Sprite sprite, int x, int y, IntRect rect) {
        IntRect r = rect != null ? rect : getSimpleSpriteRect(sprite);
        return x >= r.left && x < r.right && y >= r.top && y < r.bottom;
    }

    /**
     * Get the max keyframe frame number from path keyframes.
     *
     * @param keyframesCache The keyframes cache map
     * @return The maximum frame number
     */
    public static int getMaxKeyframeFrame(
            java.util.Map<Integer, com.dirplayer.player.score.ChannelKeyframes> keyframesCache) {
        if (keyframesCache == null || keyframesCache.isEmpty()) {
            return 1;
        }

        int maxFrame = 1;
        for (com.dirplayer.player.score.ChannelKeyframes channelKf : keyframesCache.values()) {
            if (channelKf.path != null) {
                List<com.dirplayer.player.score.SpriteKeyframe.PathKeyframe> keyframes =
                    channelKf.path.getKeyframes();
                for (com.dirplayer.player.score.SpriteKeyframe.PathKeyframe kf : keyframes) {
                    if (kf.frame > maxFrame) {
                        maxFrame = kf.frame;
                    }
                }
            }
        }
        return maxFrame;
    }
}
