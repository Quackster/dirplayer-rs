package com.dirplayer.rendering;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.Score;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.director.cast.CastMember;
import com.dirplayer.director.cast.CastMemberType;
import com.dirplayer.director.cast.BitmapMember;
import com.dirplayer.director.cast.ShapeMember;
import com.dirplayer.director.cast.FieldMember;
import com.dirplayer.director.cast.TextMember;
import com.dirplayer.director.cast.FilmLoopMember;
import com.dirplayer.director.chunks.score.ScoreFrameChannelData;
import com.dirplayer.player.score.SpritePathKeyframes;
import com.dirplayer.player.score.SpriteKeyframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core rendering logic for Director movies.
 * Platform-agnostic - outputs to Bitmap objects.
 * Port of Rust rendering.rs
 */
public class Renderer {
    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

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
     * The offset is used for filmloop rendering where sprite coordinates need to be
     * translated relative to the filmloop's initial_rect.
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

            CastMemberRef memberRef = scoreSource.getMemberRef();
            IntRect initialRect = getFilmLoopInitialRect(player, memberRef);

            FilmLoopParentProps props = parentProps != null ? parentProps : new FilmLoopParentProps();

            renderFilmloopFromChannelData(
                player,
                memberRef,
                bitmap,
                destRect,
                initialRect,
                props.ink,
                props.color,
                props.bgColor
            );
            return;
        }

        // For stage rendering, use the player's background color
        int[] bgColor = resolveColorRef(palettes, player.getBgColor(),
            PaletteRef.ofBuiltIn(getSystemDefaultPalette()), bitmap.originalBitDepth);
        bitmap.clearRect(destRect.left, destRect.top, destRect.right, destRect.bottom,
            bgColor[0], bgColor[1], bgColor[2], palettes);

        // Get sorted channel numbers
        Movie movie = player.getMovie();
        Score score = movie.getScore();
        int frameNum = movie.getCurrentFrame();

        List<Integer> sortedChannelNumbers = score.getSortedChannelNumbers(frameNum);

        logger.debug("STAGE RENDER: frame {} channels {}", frameNum, sortedChannelNumbers);

        for (int channelNum : sortedChannelNumbers) {
            Sprite sprite = score.getSprite(channelNum);
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
            Sprite sprite = score.getSprite(debugSpriteNum);
            if (sprite != null) {
                IntRect spriteRect = getConcreteSpri teRect(player, sprite);
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
        CastMemberType memberType = member.getMemberType();

        if (memberType == CastMemberType.BITMAP) {
            renderBitmapSprite(player, bitmap, sprite, member.getBitmapMember(), palettes);
        } else if (memberType == CastMemberType.SHAPE) {
            renderShapeSprite(player, bitmap, sprite, member.getShapeMember(), palettes, offsetX, offsetY);
        } else if (memberType == CastMemberType.FIELD) {
            renderFieldSprite(player, bitmap, sprite, member.getFieldMember(), palettes);
        } else if (memberType == CastMemberType.TEXT) {
            renderTextSprite(player, bitmap, sprite, member.getTextMember(), palettes);
        } else if (memberType == CastMemberType.FILM_LOOP) {
            renderFilmLoopSprite(player, bitmap, sprite, member.getFilmLoopMember(),
                sprite.getMember(), palettes);
        }
    }

    /**
     * Render a bitmap sprite.
     */
    private static void renderBitmapSprite(DirPlayer player, Bitmap destBitmap, Sprite sprite,
                                          BitmapMember bitmapMember, PaletteMap palettes) {
        Bitmap srcBitmap = player.getBitmapManager().getBitmap(bitmapMember.getImageRef());
        if (srcBitmap == null) {
            return;
        }

        IntRect spriteRect = getConcreteSpri teRect(player, sprite);
        IntRect srcRect = calculateSrcRect(sprite, bitmapMember, srcBitmap, player.getMovie());

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
                                         ShapeMember shapeMember, PaletteMap palettes,
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

        IntRect rect = getConcreteSpri teRect(player, sprite);
        IntRect spriteRect = IntRect.from(
            rect.left - offsetX,
            rect.top - offsetY,
            rect.right - offsetX,
            rect.bottom - offsetY
        );

        bitmap.fillShapeRectWithSprite(sprite, spriteRect, palettes);
    }

    /**
     * Render a field sprite.
     */
    private static void renderFieldSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                         FieldMember fieldMember, PaletteMap palettes) {
        // TODO: Implement text rendering
        logger.debug("Field sprite rendering not yet implemented");
    }

    /**
     * Render a text sprite.
     */
    private static void renderTextSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                        TextMember textMember, PaletteMap palettes) {
        // TODO: Implement text rendering
        logger.debug("Text sprite rendering not yet implemented");
    }

    /**
     * Render a film loop sprite.
     */
    private static void renderFilmLoopSprite(DirPlayer player, Bitmap destBitmap, Sprite sprite,
                                            FilmLoopMember filmLoop, CastMemberRef memberRef,
                                            PaletteMap palettes) {
        IntRect initialRect = IntRect.from(
            filmLoop.getInfo().getRegPoint().x,
            filmLoop.getInfo().getRegPoint().y,
            filmLoop.getInfo().getWidth(),
            filmLoop.getInfo().getHeight()
        );

        IntRect spriteRect = getConcreteSpri teRect(player, sprite);

        int width = Math.max(1, initialRect.width());
        int height = Math.max(1, initialRect.height());

        logger.debug("Rendering FilmLoop: channel {} frame {} ink={} blend={} initial_rect {} bitmap size {}x{}",
            sprite.getNumber(), filmLoop.getCurrentFrame(), sprite.getInk(), sprite.getBlend(),
            initialRect, width, height);

        Bitmap filmloopBitmap = new Bitmap(width, height, 32, 32, 8,
            PaletteRef.ofBuiltIn(getSystemDefaultPalette()));
        filmloopBitmap.setUseAlpha(true);
        filmloopBitmap.clearData();

        renderScoreToBitmapWithOffset(
            player,
            ScoreRef.filmLoop(memberRef),
            filmloopBitmap,
            null,
            IntRect.fromSize(0, 0, width, height),
            initialRect.left,
            initialRect.top,
            new FilmLoopParentProps(sprite.getInk(), sprite.getColor(), sprite.getBgColor())
        );

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            null,
            false,
            sprite.getRotation(),
            null,
            spriteRect
        );

        IntRect dstRect = IntRect.fromSize(spriteRect.left, spriteRect.top, width, height);
        destBitmap.copyPixelsWithParams(palettes, filmloopBitmap, dstRect,
            IntRect.fromSize(0, 0, width, height), params);
    }

    /**
     * Render a filmloop directly from its channel_initialization_data.
     */
    private static void renderFilmloopFromChannelData(
            DirPlayer player,
            CastMemberRef memberRef,
            Bitmap bitmap,
            IntRect destRect,
            IntRect initialRect,
            int parentInk,
            ColorRef parentColor,
            ColorRef parentBgColor) {

        PaletteMap palettes = player.getMovie().getCastManager().palettes();
        int filmloopCastLib = memberRef.getCastLib();

        // Get filmloop data
        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null || member.getMemberType() != CastMemberType.FILM_LOOP) {
            return;
        }

        FilmLoopMember filmLoop = member.getFilmLoopMember();
        int currentFrame = filmLoop.getCurrentFrame();

        // Group channel data by channel, keeping most recent frame <= current
        Map<Integer, ChannelFrameData> channelMap = new HashMap<>();

        for (FilmLoopMember.InitData initData : filmLoop.getScore().getChannelInitializationData()) {
            int frameIdx = initData.frameIdx;
            int channelIdx = initData.channelIdx;
            ScoreFrameChannelData data = initData.data;

            // Skip effect channels (0-5)
            if (channelIdx < 6) continue;
            // Skip empty sprites
            if (data.castMember == 0) continue;
            // Only consider frames <= current
            if (frameIdx > currentFrame - 1) continue;

            ChannelFrameData existing = channelMap.get(channelIdx);
            if (existing == null || frameIdx > existing.frameIdx) {
                channelMap.put(channelIdx, new ChannelFrameData(frameIdx, data));
            }
        }

        logger.debug("render_filmloop_from_channel_data: frame {}, {} sprites",
            currentFrame, channelMap.size());

        // Sort by channel number for z-ordering
        List<Integer> sortedChannels = new ArrayList<>(channelMap.keySet());
        sortedChannels.sort(Integer::compareTo);

        for (int channelIdx : sortedChannels) {
            ChannelFrameData cfd = channelMap.get(channelIdx);
            ScoreFrameChannelData data = cfd.data;

            // Build member ref
            int spriteCastLib = data.castLib == 65535 ? filmloopCastLib : data.castLib;
            CastMemberRef spriteMemberRef = new CastMemberRef(spriteCastLib, data.castMember);

            CastMember spriteMember = player.getMovie().getCastManager().findMemberByRef(spriteMemberRef);
            if (spriteMember == null) {
                continue;
            }

            // Get position (may be interpolated from keyframes)
            int posX = data.posX;
            int posY = data.posY;

            // Get dimensions and registration point
            int memberWidth, memberHeight, regX, regY;
            if (spriteMember.getMemberType() == CastMemberType.BITMAP) {
                BitmapMember bm = spriteMember.getBitmapMember();
                memberWidth = bm.getInfo().getWidth();
                memberHeight = bm.getInfo().getHeight();
                regX = bm.getRegPoint().x;
                regY = bm.getRegPoint().y;
            } else {
                memberWidth = data.width;
                memberHeight = data.height;
                regX = data.width / 2;
                regY = data.height / 2;
            }

            int useWidth = data.width > 0 ? data.width : memberWidth;
            int useHeight = data.height > 0 ? data.height : memberHeight;

            // Calculate sprite rect relative to initial_rect
            int spriteLeft = posX - regX;
            int spriteTop = posY - regY;
            int relX = spriteLeft - initialRect.left;
            int relY = spriteTop - initialRect.top;

            IntRect spriteRect = IntRect.from(relX, relY, relX + useWidth, relY + useHeight);

            // Render based on member type
            if (spriteMember.getMemberType() == CastMemberType.BITMAP) {
                BitmapMember bitmapMember = spriteMember.getBitmapMember();
                Bitmap srcBitmap = player.getBitmapManager().getBitmap(bitmapMember.getImageRef());
                if (srcBitmap == null) continue;

                IntRect srcRect = IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());

                // Use parent ink semantics
                int ink = parentInk;
                int blend = data.blend == 0 ? 100 : data.blend;

                // Determine if should use matte
                boolean isIndexed = srcBitmap.getOriginalBitDepth() <= 8;
                boolean is16bit = srcBitmap.getOriginalBitDepth() == 16;
                boolean shouldUseMatte = (isIndexed && (ink == 0 || ink == 8)) || (is16bit && ink == 0);

                BitmapMask mask = null;
                if (shouldUseMatte) {
                    if (srcBitmap.getMatte() == null) {
                        srcBitmap.createMatte(palettes);
                    }
                    mask = srcBitmap.getMatte();
                }

                CopyPixelsParams params = new CopyPixelsParams(
                    blend, ink, parentColor, parentBgColor,
                    mask, false, 0.0f, null, spriteRect
                );

                bitmap.copyPixelsWithParams(palettes, srcBitmap, spriteRect, srcRect, params);
            } else if (spriteMember.getMemberType() == CastMemberType.SHAPE) {
                // Skip tiny shapes
                if (data.width <= 1 || data.height <= 1) continue;

                // Resolve color
                boolean foreIsRgb = (data.colorFlag & 0x1) != 0 ||
                    data.foreColorG != 0 || data.foreColorB != 0;

                int r, g, b;
                if (foreIsRgb) {
                    r = data.foreColor;
                    g = data.foreColorG;
                    b = data.foreColorB;
                } else {
                    int[] resolved = resolveColorRef(palettes,
                        ColorRef.paletteIndex(data.foreColor),
                        PaletteRef.ofBuiltIn(getSystemDefaultPalette()),
                        bitmap.getOriginalBitDepth());
                    r = resolved[0];
                    g = resolved[1];
                    b = resolved[2];
                }

                bitmap.fillRect(spriteRect.left, spriteRect.top,
                    spriteRect.right, spriteRect.bottom, r, g, b, palettes, 1.0f);
            }
        }
    }

    /**
     * Get the concrete sprite rect for rendering.
     */
    public static IntRect getConcreteSpri teRect(DirPlayer player, Sprite sprite) {
        // Basic implementation - may need refinement
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
            if (member != null && member.getMemberType() == CastMemberType.BITMAP) {
                BitmapMember bm = member.getBitmapMember();
                regX = bm.getRegPoint().x;
                regY = bm.getRegPoint().y;
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
     * Calculate source rect for bitmap copy.
     */
    private static IntRect calculateSrcRect(Sprite sprite, BitmapMember bitmapMember,
                                           Bitmap srcBitmap, Movie movie) {
        if (sprite.hasSizeTweened() || sprite.hasSizeChanged()) {
            return IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        }

        int infoWidth = bitmapMember.getInfo().getWidth();
        int infoHeight = bitmapMember.getInfo().getHeight();

        if (sprite.getWidth() > movie.getRect().width() && sprite.getHeight() > movie.getRect().height()) {
            return IntRect.from(0, 0, infoWidth, infoHeight);
        }

        if (infoWidth == 0 && infoHeight == 0) {
            return IntRect.from(0, 0, sprite.getWidth(), sprite.getHeight());
        }

        if ((infoWidth < sprite.getWidth() && infoHeight < sprite.getHeight()) ||
            (infoWidth > sprite.getWidth() && infoHeight > sprite.getHeight())) {
            return IntRect.from(0, 0, infoWidth, infoHeight);
        }

        if (sprite.getWidth() > infoWidth || sprite.getHeight() > infoHeight) {
            return IntRect.from(0, 0, infoWidth, infoHeight);
        }

        return IntRect.from(0, 0, sprite.getWidth(), sprite.getHeight());
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

    /**
     * Get filmloop initial rect.
     */
    private static IntRect getFilmLoopInitialRect(DirPlayer player, CastMemberRef memberRef) {
        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member != null && member.getMemberType() == CastMemberType.FILM_LOOP) {
            FilmLoopMember fl = member.getFilmLoopMember();
            return IntRect.from(
                fl.getInfo().getRegPoint().x,
                fl.getInfo().getRegPoint().y,
                fl.getInfo().getWidth(),
                fl.getInfo().getHeight()
            );
        }
        return IntRect.from(0, 0, 1, 1);
    }

    /**
     * Interpolate path position between keyframes for filmloop animation.
     */
    private static int[] interpolatePathPosition(SpritePathKeyframes pathKeyframes, int frame) {
        List<SpriteKeyframe> keyframes = pathKeyframes.getKeyframes();
        if (keyframes.isEmpty()) {
            return null;
        }

        // Find keyframe pair surrounding current frame
        SpriteKeyframe prev = null;
        SpriteKeyframe next = null;

        for (SpriteKeyframe kf : keyframes) {
            if (kf.getFrame() <= frame) {
                prev = kf;
            }
            if (kf.getFrame() > frame && next == null) {
                next = kf;
            }
        }

        if (prev != null && next != null) {
            int frameRange = next.getFrame() - prev.getFrame();
            if (frameRange == 0) {
                return new int[] {prev.getX(), prev.getY()};
            }
            float t = (float)(frame - prev.getFrame()) / frameRange;
            int x = (int)(prev.getX() + (next.getX() - prev.getX()) * t);
            int y = (int)(prev.getY() + (next.getY() - prev.getY()) * t);
            return new int[] {x, y};
        } else if (prev != null) {
            return new int[] {prev.getX(), prev.getY()};
        }

        return null;
    }

    /**
     * Helper class for channel frame data during filmloop rendering.
     */
    private static class ChannelFrameData {
        final int frameIdx;
        final ScoreFrameChannelData data;

        ChannelFrameData(int frameIdx, ScoreFrameChannelData data) {
            this.frameIdx = frameIdx;
            this.data = data;
        }
    }
}
