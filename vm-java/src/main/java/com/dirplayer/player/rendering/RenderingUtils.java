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
import com.dirplayer.director.MemberType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility methods for rendering operations.
 * Port of helper functions from Rust rendering.rs.
 */
public final class RenderingUtils {
    private static final Logger logger = LoggerFactory.getLogger(RenderingUtils.class);

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
}
