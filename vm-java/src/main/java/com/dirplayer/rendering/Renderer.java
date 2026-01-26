package com.dirplayer.rendering;

import com.dirplayer.director.MemberType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.CastManager;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.FontManager;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.score.Score;
import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // For filmloops, render filmloop content
        if (scoreSource.isFilmLoop()) {
            bitmap.clearRectTransparent(destRect.left, destRect.top, destRect.right, destRect.bottom);
            renderFilmLoop(player, scoreSource, bitmap, destRect, palettes, parentProps);
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
            renderTextSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
        } else if (memberType == MemberType.Text) {
            renderTextSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
        } else if (memberType == MemberType.FilmLoop) {
            renderFilmLoopSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
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
     * Render a text/field sprite.
     */
    private static void renderTextSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                         CastMember member, PaletteMap palettes,
                                         int offsetX, int offsetY) {
        String text = member.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        // Get font
        String fontName = member.font != null ? member.font : "System";
        int fontSize = member.fontSize > 0 ? member.fontSize : 12;

        FontManager.BitmapFont font = player.fontManager.getFont(fontName);
        if (font == null) {
            font = player.fontManager.getSystemFont();
        }
        if (font == null) {
            logger.debug("No font available for text rendering");
            return;
        }

        // Get font bitmap
        Bitmap fontBitmap = null;
        if (font.bitmapRef >= 0) {
            fontBitmap = player.getBitmapManager().getBitmap(font.bitmapRef);
        }
        if (fontBitmap == null) {
            logger.debug("No font bitmap available");
            return;
        }

        IntRect spriteRect = getConcreteSpriteRect(player, sprite);

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            null,
            true,   // isTextRendering
            0.0f,
            sprite,
            spriteRect
        );

        // Draw text
        int textX = spriteRect.left - offsetX;
        int textY = spriteRect.top - offsetY;

        // Handle line spacing based on member settings
        int fixedLineSpace = 0;  // Use font default
        int topSpacing = 0;

        if (member.specificData instanceof com.dirplayer.player.cast.TextMember) {
            com.dirplayer.player.cast.TextMember textMember =
                (com.dirplayer.player.cast.TextMember) member.specificData;
            fixedLineSpace = textMember.fixedLineSpace;
            topSpacing = textMember.topSpacing;
        }

        bitmap.drawText(text, font, fontBitmap, textX, textY, params, palettes, fixedLineSpace, topSpacing);
    }

    /**
     * Render a filmloop sprite on the stage.
     */
    private static void renderFilmLoopSprite(DirPlayer player, Bitmap bitmap, Sprite sprite,
                                             CastMember member, PaletteMap palettes,
                                             int offsetX, int offsetY) {
        if (!(member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember)) {
            return;
        }

        com.dirplayer.player.cast.FilmLoopMember filmLoop =
            (com.dirplayer.player.cast.FilmLoopMember) member.specificData;

        // Get sprite rect
        IntRect spriteRect = getConcreteSpriteRect(player, sprite);

        // Get initial rect for filmloop content
        IntRect initialRect = filmLoop.getInitialRect();
        if (initialRect == null || initialRect.width() <= 0 || initialRect.height() <= 0) {
            initialRect = computeFilmloopInitialRect(player, sprite.getMember());
            if (initialRect == null) {
                return;
            }
        }

        // Create filmloop bitmap at natural size (initial_rect dimensions)
        int width = Math.max(1, initialRect.width());
        int height = Math.max(1, initialRect.height());

        Bitmap filmloopBitmap = new Bitmap(width, height);
        filmloopBitmap.depth = 32;
        filmloopBitmap.useAlpha = true;
        // Clear to fully transparent
        java.util.Arrays.fill(filmloopBitmap.data, (byte)0);

        // Create parent props from sprite properties
        FilmLoopParentProps parentProps = new FilmLoopParentProps(
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor()
        );

        // Render filmloop content to temporary bitmap
        IntRect destRect = IntRect.from(0, 0, width, height);
        renderFilmLoop(player, ScoreRef.filmLoop(sprite.getMember()),
            filmloopBitmap, destRect, palettes, parentProps);

        // Composite filmloop bitmap onto stage at sprite location
        IntRect srcRect = IntRect.from(0, 0, width, height);
        IntRect dstRect = IntRect.from(
            spriteRect.left - offsetX,
            spriteRect.top - offsetY,
            spriteRect.left - offsetX + width,
            spriteRect.top - offsetY + height
        );

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            null,
            false,
            sprite.getRotation(),
            sprite,
            spriteRect
        );

        bitmap.copyPixelsWithParams(palettes, filmloopBitmap, dstRect, srcRect, params);
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
        int cursorType = player.currentCursor;
        int mouseX = player.mouseLocX;
        int mouseY = player.mouseLocY;

        // Only draw custom cursor if not using system cursor
        if (cursorType == -1) {
            // System cursor - don't draw
            return;
        }

        // Simple arrow cursor (4x4 pixels)
        int cursorSize = 8;
        int[] cursorColor = {255, 255, 255};

        // Draw simple cursor outline
        for (int i = 0; i < cursorSize; i++) {
            // Top line of arrow
            if (mouseX + i < bitmap.width && mouseY + i < bitmap.height && mouseY + i >= 0) {
                bitmap.setPixel(mouseX, mouseY + i, cursorColor[0], cursorColor[1], cursorColor[2], palettes);
                bitmap.setPixel(mouseX + i, mouseY, cursorColor[0], cursorColor[1], cursorColor[2], palettes);
            }
        }
    }

    /**
     * Render a filmloop to bitmap using channel_initialization_data.
     * This is needed because filmloop Score.channels are not populated like the main stage score.
     * Instead, we read sprite info directly from channel_initialization_data.
     *
     * Director behavior: Film loop frames use the PARENT sprite's ink semantics,
     * not their own stored ink values. The parent_ink, parent_color, and parent_bg_color
     * are the properties of the sprite displaying the film loop on the stage.
     */
    private static void renderFilmLoop(DirPlayer player, ScoreRef scoreSource, Bitmap bitmap,
                                       IntRect destRect, PaletteMap palettes,
                                       FilmLoopParentProps parentProps) {
        CastMemberRef memberRef = scoreSource.getMemberRef();
        if (memberRef == null) {
            return;
        }

        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null || member.memberType != MemberType.FilmLoop) {
            return;
        }

        if (!(member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember)) {
            return;
        }

        com.dirplayer.player.cast.FilmLoopMember filmLoop =
            (com.dirplayer.player.cast.FilmLoopMember) member.specificData;

        Score filmLoopScore = filmLoop.getScore();
        int currentFrame = filmLoop.getCurrentFrame();
        int frameIdxTarget = Math.max(0, currentFrame - 1); // Convert 1-based to 0-based index

        // The filmloop's own cast_lib is used as the default when channel data has cast_lib=65535
        int filmloopCastLib = memberRef.getCastLib();

        // Get initial rect for coordinate transformation
        IntRect initialRect = filmLoop.getInitialRect();
        if (initialRect.width() <= 0 || initialRect.height() <= 0) {
            // Compute initial rect if not set
            initialRect = computeFilmloopInitialRect(player, memberRef);
            if (initialRect == null) {
                return;
            }
        }

        // Collect channel data for the current frame, using keyframe interpolation
        // Group data by channel, keeping only valid sprite channels
        Map<Integer, com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry> channelMap = new HashMap<>();

        for (com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry : filmLoopScore.channelInitializationData) {
            int frameIdx = entry.frameIndex;
            int channelIdx = entry.channelIndex;
            com.dirplayer.director.chunks.ScoreFrameChannelData data = entry.data;

            // Skip effect channels (0-5)
            if (channelIdx < 6) {
                continue;
            }
            // Skip empty sprites (cast_member 0 means no sprite)
            if (data.castMember == 0) {
                continue;
            }
            // Only consider frames <= current frame (keyframe interpolation)
            if (frameIdx > frameIdxTarget) {
                continue;
            }
            // Keep the most recent (highest frame_idx) data for each channel
            com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry existing = channelMap.get(channelIdx);
            if (existing == null || frameIdx > existing.frameIndex) {
                channelMap.put(channelIdx, entry);
            }
        }

        // Sort by channel number for consistent z-ordering
        List<Map.Entry<Integer, com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry>> sortedData =
            new ArrayList<>(channelMap.entrySet());
        sortedData.sort(Comparator.comparingInt(Map.Entry::getKey));

        // Get parent props (use defaults if null)
        int parentInk = parentProps != null ? parentProps.ink : 0;
        ColorRef parentColor = parentProps != null ? parentProps.color : ColorRef.paletteIndex(255);
        ColorRef parentBgColor = parentProps != null ? parentProps.bgColor : ColorRef.paletteIndex(0);

        for (Map.Entry<Integer, com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry> e : sortedData) {
            int channelIdx = e.getKey();
            com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry = e.getValue();
            com.dirplayer.director.chunks.ScoreFrameChannelData data = entry.data;

            // Build member ref from channel data
            // cast_lib 65535 means "use the filmloop's cast library"
            int spriteCastLib = (data.castLib == 65535) ? filmloopCastLib : data.castLib;
            CastMemberRef spriteMemberRef = new CastMemberRef(spriteCastLib, data.castMember);

            CastMember spriteMember = player.getMovie().getCastManager().findMemberByRef(spriteMemberRef);
            if (spriteMember == null) {
                logger.debug("  channel {}: member {}:{} not found", channelIdx, spriteCastLib, data.castMember);
                continue;
            }

            // Get interpolated position from path keyframes (or use data position)
            int posX = data.posX;
            int posY = data.posY;

            // Check for path keyframes and interpolate
            com.dirplayer.player.score.ChannelKeyframes channelKeyframes =
                filmLoopScore.keyframesCache.get(channelIdx);
            if (channelKeyframes != null && channelKeyframes.path != null) {
                int[] interpolated = interpolatePathPosition(channelKeyframes.path, currentFrame);
                if (interpolated != null) {
                    posX = (short) interpolated[0];
                    posY = (short) interpolated[1];
                }
            }

            // Get member dimensions and registration point
            int memberWidth = data.width;
            int memberHeight = data.height;
            int regX = memberWidth / 2;
            int regY = memberHeight / 2;

            if (spriteMember.memberType == MemberType.Bitmap) {
                memberWidth = spriteMember.bitmapWidth > 0 ? spriteMember.bitmapWidth : memberWidth;
                memberHeight = spriteMember.bitmapHeight > 0 ? spriteMember.bitmapHeight : memberHeight;
                regX = spriteMember.regPointX;
                regY = spriteMember.regPointY;
            }

            // Use channel data dimensions if valid, otherwise fall back to member dimensions
            int useWidth = (data.width > 0 && data.height > 0) ? data.width : memberWidth;
            int useHeight = (data.width > 0 && data.height > 0) ? data.height : memberHeight;

            // Coordinate transformation: translate sprite position relative to initial_rect origin.
            // pos_x/pos_y are the sprite's loc (registration point position).
            // We need to subtract the registration point (the bitmap's anchor)
            // to get the sprite's top-left corner, then translate relative to initial_rect.
            int spriteLeft = posX - regX;
            int spriteTop = posY - regY;
            int relX = spriteLeft - initialRect.left;
            int relY = spriteTop - initialRect.top;

            IntRect spriteRect = IntRect.from(relX, relY, relX + useWidth, relY + useHeight);

            logger.debug("  channel {}: member {}:{} type {} pos ({}, {}) size {}x{} reg ({}, {}) -> rect ({}, {}, {}, {})",
                channelIdx, spriteCastLib, data.castMember, spriteMember.memberType,
                posX, posY, useWidth, useHeight, regX, regY,
                spriteRect.left, spriteRect.top, spriteRect.right, spriteRect.bottom);

            // Render based on member type
            if (spriteMember.memberType == MemberType.Bitmap) {
                renderFilmLoopBitmap(player, bitmap, spriteMember, spriteRect, palettes,
                    parentInk, parentColor, parentBgColor, data.blend);
            } else if (spriteMember.memberType == MemberType.Shape) {
                // Skip tiny shapes
                if (data.width <= 1 || data.height <= 1) {
                    continue;
                }
                renderFilmLoopShape(player, bitmap, data, spriteRect, palettes);
            }
        }
    }

    /**
     * Interpolate path position between keyframes for filmloop animation.
     * Returns interpolated [x, y] position for the given frame, or null if no interpolation is needed.
     */
    private static int[] interpolatePathPosition(com.dirplayer.player.score.SpritePathKeyframes pathKeyframes,
                                                  int frame) {
        List<com.dirplayer.player.score.SpriteKeyframe.PathKeyframe> keyframes = pathKeyframes.keyframes;
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }

        // Find the keyframe pair surrounding the current frame
        com.dirplayer.player.score.SpriteKeyframe.PathKeyframe prevKf = null;
        com.dirplayer.player.score.SpriteKeyframe.PathKeyframe nextKf = null;

        for (com.dirplayer.player.score.SpriteKeyframe.PathKeyframe kf : keyframes) {
            if (kf.frame <= frame) {
                prevKf = kf;
            }
            if (kf.frame > frame && nextKf == null) {
                nextKf = kf;
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
        }

        return null;
    }

    /**
     * Render a bitmap sprite within a filmloop.
     */
    private static void renderFilmLoopBitmap(DirPlayer player, Bitmap destBitmap, CastMember member,
                                             IntRect spriteRect, PaletteMap palettes,
                                             int parentInk, ColorRef parentColor, ColorRef parentBgColor,
                                             int blend) {
        int imageRef = member.getImageRef();
        Bitmap srcBitmap = player.getBitmapManager().getBitmap(imageRef);
        if (srcBitmap == null) {
            return;
        }

        IntRect srcRect = IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());

        // Director behavior: Film loop internal sprites use the PARENT sprite's
        // ink, color, and bgColor - not their own stored values.
        int ink = parentInk;
        ColorRef spriteColor = parentColor;
        ColorRef spriteBgColor = parentBgColor;

        // In Director, blend=0 means "default" which is fully opaque (100)
        // Only values 1-99 represent partial transparency
        int useBlend = (blend == 0) ? 100 : blend;

        // Determine if we should use matte mask
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
            useBlend,
            ink,
            spriteColor,
            spriteBgColor,
            mask,
            false,  // isTextRendering
            0.0f,   // rotation
            null,   // sprite
            spriteRect
        );

        destBitmap.copyPixelsWithParams(palettes, srcBitmap, spriteRect, srcRect, params);
    }

    /**
     * Render a shape sprite within a filmloop.
     */
    private static void renderFilmLoopShape(DirPlayer player, Bitmap bitmap,
                                            com.dirplayer.director.chunks.ScoreFrameChannelData data,
                                            IntRect spriteRect, PaletteMap palettes) {
        // Get sprite foreground color from channel data
        // Detect RGB mode by checking color_flag OR non-zero G/B components
        boolean foreIsRgb = (data.colorFlag & 0x1) != 0
            || data.foreColorG != 0
            || data.foreColorB != 0;

        ColorRef spriteColor;
        if (foreIsRgb) {
            spriteColor = ColorRef.rgb(data.foreColor & 0xFF, data.foreColorG & 0xFF, data.foreColorB & 0xFF);
        } else {
            spriteColor = ColorRef.paletteIndex(data.foreColor & 0xFF);
        }

        int[] color = resolveColorRef(palettes, spriteColor,
            PaletteRef.ofBuiltIn(getSystemDefaultPalette()), bitmap.getOriginalBitDepth());

        bitmap.fillRect(spriteRect.left, spriteRect.top,
            spriteRect.right, spriteRect.bottom, color[0], color[1], color[2], palettes, 1.0f);
    }

    /**
     * Compute the initial_rect for a filmloop using actual bitmap dimensions.
     * This is more accurate than precomputed values because it uses real cast member dimensions.
     */
    private static IntRect computeFilmloopInitialRect(DirPlayer player, CastMemberRef memberRef) {
        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null) {
            return null;
        }
        if (!(member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember)) {
            return null;
        }

        com.dirplayer.player.cast.FilmLoopMember filmLoop =
            (com.dirplayer.player.cast.FilmLoopMember) member.specificData;

        int filmloopCastLib = memberRef.getCastLib();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean foundAny = false;

        for (com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry : filmLoop.getScore().channelInitializationData) {
            int channelIdx = entry.channelIndex;
            com.dirplayer.director.chunks.ScoreFrameChannelData data = entry.data;

            // Skip effect channels (0-5)
            if (channelIdx < 6) {
                continue;
            }
            // Skip empty sprites
            if (data.castMember == 0 || data.castLib == 0) {
                continue;
            }

            // Resolve the cast member reference
            int spriteCastLib = (data.castLib == 65535) ? filmloopCastLib : data.castLib;
            CastMemberRef spriteMemberRef = new CastMemberRef(spriteCastLib, data.castMember);

            // Get actual bitmap dimensions and registration point from the cast member
            int actualWidth = data.width;
            int actualHeight = data.height;
            int regX = data.width / 2;
            int regY = data.height / 2;

            CastMember spriteMember = player.getMovie().getCastManager().findMemberByRef(spriteMemberRef);
            if (spriteMember != null && spriteMember.memberType == MemberType.Bitmap) {
                // Try to get actual bitmap dimensions
                Bitmap spriteBitmap = player.getBitmapManager().getBitmap(spriteMember.getImageRef());
                if (spriteBitmap != null) {
                    actualWidth = spriteBitmap.getWidth();
                    actualHeight = spriteBitmap.getHeight();
                } else if (spriteMember.bitmapWidth > 0) {
                    actualWidth = spriteMember.bitmapWidth;
                    actualHeight = spriteMember.bitmapHeight;
                }
                regX = spriteMember.regPointX;
                regY = spriteMember.regPointY;
            }

            if (actualWidth == 0 && actualHeight == 0) {
                continue;
            }

            // pos_x/pos_y is the loc (registration point position).
            // The sprite's top-left corner is: pos - reg_point
            int spriteLeft = data.posX - regX;
            int spriteTop = data.posY - regY;
            int spriteRight = spriteLeft + actualWidth;
            int spriteBottom = spriteTop + actualHeight;

            if (spriteLeft < minX) minX = spriteLeft;
            if (spriteTop < minY) minY = spriteTop;
            if (spriteRight > maxX) maxX = spriteRight;
            if (spriteBottom > maxY) maxY = spriteBottom;
            foundAny = true;
        }

        if (!foundAny) {
            return null;
        }

        return IntRect.from(minX, minY, maxX, maxY);
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
