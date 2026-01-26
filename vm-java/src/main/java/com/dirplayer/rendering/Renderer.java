package com.dirplayer.rendering;

import com.dirplayer.director.MemberType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.CastManager;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.score.Score;
import com.dirplayer.SimpleLogger;


import java.util.List;

/**
 * Core rendering logic for Director movies.
 * Platform-agnostic - outputs to Bitmap objects.
 * Port of Rust rendering.rs
 */
public class Renderer {
    private static final SimpleLogger logger = SimpleLogger.getLogger(Renderer.class);

    /**
     * Render the stage to a bitmap.
     */
    public static void renderStageToBitmap(DirPlayer player, Bitmap bitmap, Integer debugSpriteNum) {
        PaletteMap palettes = player.getMovie().getCastManager().palettes();
        renderScoreToBitmap(
            player,
            ScoreRef.stage(),
            bitmap,
            debugSpriteNum,
            IntRect.fromSize(0, 0,
                player.getMovie().getRect().width(),
                player.getMovie().getRect().height())
        );
        drawCursor(player, bitmap, palettes);
    }

    /**
     * Render a score to a bitmap.
     */
    public static void renderScoreToBitmap(
            DirPlayer player,
            ScoreRef scoreSource,
            Bitmap bitmap,
            Integer debugSpriteNum,
            IntRect destRect) {
        renderScoreToBitmapWithOffset(player, scoreSource, bitmap, debugSpriteNum, destRect, 0, 0, null);
    }

    /**
     * Render a score to a bitmap with optional coordinate offset.
     */
    public static void renderScoreToBitmapWithOffset(
            DirPlayer player,
            ScoreRef scoreSource,
            Bitmap bitmap,
            Integer debugSpriteNum,
            IntRect destRect,
            int offsetX,
            int offsetY,
            FilmLoopParentProps parentProps) {

        PaletteMap palettes = player.getMovie().getCastManager().palettes();

        // For filmloops, use transparent background
        if (scoreSource.isFilmLoop()) {
            bitmap.clearRectTransparent(destRect.left, destRect.top, destRect.right, destRect.bottom);
            // TODO: Implement filmloop rendering
            return;
        }

        // For stage rendering, use the player's background color
        ColorRef bgColorRef = player.getBgColor();
        int[] bgColor = resolveColorRef(palettes, bgColorRef,
            PaletteRef.ofBuiltIn(getSystemDefaultPalette()), bitmap.getOriginalBitDepth());
        bitmap.clearRect(destRect.left, destRect.top, destRect.right, destRect.bottom,
            bgColor[0], bgColor[1], bgColor[2], palettes);

        // Get sorted channel numbers
        Movie movie = player.getMovie();
        Score score = movie.getScore();
        int frameNum = movie.getCurrentFrame();

        List<Integer> sortedChannelNumbers = score.getSortedChannelNumbers(frameNum);

        logger.debug("STAGE RENDER: frame {} channels {}", frameNum, sortedChannelNumbers);

        for (int channelNum : sortedChannelNumbers) {
            Sprite sprite = score.getSprite((short) channelNum);
            if (sprite == null) {
                continue;
            }

            CastMemberRef memberRef = sprite.getMember();
            if (memberRef == null) {
                continue;
            }

            CastMember member = movie.getCastManager().findMemberByRef(memberRef);
            if (member == null) {
                continue;
            }

            logger.debug("  STAGE channel {}: member {}:{} type {}",
                channelNum, memberRef.getCastLib(), memberRef.getCastMember(),
                member.getMemberType());

            renderSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
        }

        // Draw debug rect
        if (debugSpriteNum != null) {
            Sprite sprite = score.getSprite(debugSpriteNum.shortValue());
            if (sprite != null) {
                IntRect spriteRect = getConcreteSpriteRect(player, sprite);
                bitmap.strokeRect(spriteRect.left, spriteRect.top,
                    spriteRect.right, spriteRect.bottom, 255, 0, 0, palettes, 1.0f);
                bitmap.setPixel(sprite.getLocH(), sprite.getLocV(), 0, 255, 0, palettes);
            }
        }
    }

    /**
     * Render a single sprite to the bitmap.
     */
    private static void renderSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                    CastMember member, PaletteMap palettes,
                                    int offsetX, int offsetY) {
        MemberType memberType = member.getMemberType();

        if (memberType == MemberType.Bitmap) {
            renderBitmapSprite(player, bitmap, sprite, member, palettes);
        } else if (memberType == MemberType.Shape) {
            renderShapeSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
        } else if (memberType == MemberType.Button || memberType == MemberType.RTE) {
            // Field/Text rendering - TODO
            logger.debug("Field/Text sprite rendering not yet implemented");
        } else if (memberType == MemberType.Text) {
            // Text rendering - TODO
            logger.debug("Text sprite rendering not yet implemented");
        } else if (memberType == MemberType.FilmLoop) {
            // FilmLoop rendering - TODO
            logger.debug("FilmLoop sprite rendering not yet implemented");
        }
    }

    /**
     * Render a bitmap sprite.
     */
    private static void renderBitmapSprite(DirPlayer player, Bitmap destBitmap, Sprite sprite,
                                          CastMember member, PaletteMap palettes) {
        int imageRef = member.getImageRef();
        Bitmap srcBitmap = player.getBitmapManager().getBitmap(imageRef);
        if (srcBitmap == null) {
            return;
        }

        IntRect spriteRect = getConcreteSpriteRect(player, sprite);
        IntRect srcRect = IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());

        IntRect dstRect = applyFlip(spriteRect, sprite.isFlipH(), sprite.isFlipV());

        BitmapMask mask = null;
        if (shouldMatteSprite(sprite.getInk())) {
            if (srcBitmap.getMatte() == null) {
                srcBitmap.createMatte(palettes);
            }
            mask = srcBitmap.getMatte();
        }

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            mask,
            false,
            sprite.getRotation(),
            sprite,
            spriteRect
        );

        destBitmap.copyPixelsWithParams(palettes, srcBitmap, dstRect, srcRect, params);
    }

    /**
     * Render a shape sprite.
     */
    private static void renderShapeSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                         CastMember member, PaletteMap palettes,
                                         int offsetX, int offsetY) {
        // Skip tiny shapes
        if (sprite.getWidth() <= 1 || sprite.getHeight() <= 1) {
            return;
        }

        // Skip placeholder 1:1
        CastMemberRef memberRef = sprite.getMember();
        if (memberRef != null && memberRef.getCastLib() == 1 && memberRef.getCastMember() == 1) {
            return;
        }

        IntRect rect = getConcreteSpriteRect(player, sprite);
        IntRect spriteRect = IntRect.from(
            rect.left - offsetX,
            rect.top - offsetY,
            rect.right - offsetX,
            rect.bottom - offsetY
        );

        // Get foreground color
        ColorRef color = sprite.getColor();
        int[] rgb = resolveColorRef(palettes, color,
            PaletteRef.ofBuiltIn(getSystemDefaultPalette()), bitmap.getOriginalBitDepth());

        bitmap.fillRect(spriteRect.left, spriteRect.top,
            spriteRect.right, spriteRect.bottom, rgb[0], rgb[1], rgb[2], palettes, 1.0f);
    }

    /**
     * Get the concrete sprite rect for rendering.
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
     * Apply flip to destination rect.
     */
    private static IntRect applyFlip(IntRect rect, boolean flipH, boolean flipV) {
        return IntRect.from(
            flipH ? rect.right : rect.left,
            flipV ? rect.bottom : rect.top,
            flipH ? rect.left : rect.right,
            flipV ? rect.top : rect.bottom
        );
    }

    /**
     * Check if sprite ink requires matte.
     */
    private static boolean shouldMatteSprite(int ink) {
        return ink == 0 || ink == 8;  // Copy or Matte ink
    }

    /**
     * Draw cursor on bitmap.
     */
    private static void drawCursor(DirPlayer player, Bitmap bitmap, PaletteMap palettes) {
        // TODO: Implement cursor drawing
    }

    /**
     * Get system default palette.
     */
    public static BuiltInPalette getSystemDefaultPalette() {
        return BuiltInPalette.SystemWin;
    }

    /**
     * Resolve a color reference to RGB values.
     */
    public static int[] resolveColorRef(PaletteMap palettes, ColorRef colorRef,
                                       PaletteRef paletteRef, int bitDepth) {
        if (colorRef == null) {
            return new int[] {0, 0, 0};
        }

        if (colorRef.isRgb()) {
            return new int[] {colorRef.getR(), colorRef.getG(), colorRef.getB()};
        }

        // Palette index
        int index = colorRef.getPaletteIndex();
        int[][] palette = palettes.getPalette(paletteRef, bitDepth);
        if (palette != null && index >= 0 && index < palette.length) {
            return palette[index];
        }

        return new int[] {0, 0, 0};
    }
}
