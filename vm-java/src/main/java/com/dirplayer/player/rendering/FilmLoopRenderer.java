package com.dirplayer.player.rendering;

import com.dirplayer.director.MemberType;
import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.cast.BitmapMember;
import com.dirplayer.player.cast.FilmLoopMember;
import com.dirplayer.player.cast.ShapeMember;
import com.dirplayer.player.score.ChannelKeyframes;
import com.dirplayer.player.score.Score;
import com.dirplayer.player.score.SpritePathKeyframes;
import com.dirplayer.rendering.CopyPixelsParams;
import com.dirplayer.rendering.IntRect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Film loop rendering logic.
 * Handles rendering film loop cast members from their channel data.
 * Port of render_filmloop_from_channel_data from Rust.
 */
public class FilmLoopRenderer {
    private static final Logger logger = LoggerFactory.getLogger(FilmLoopRenderer.class);

    private FilmLoopRenderer() {
        // Prevent instantiation
    }

    /**
     * Compute the initial_rect for a filmloop using actual bitmap dimensions.
     * This is more accurate than the precomputed initial_rect because it uses
     * the real cast member dimensions instead of the channel_data dimensions.
     *
     * @param player The player
     * @param memberRef The filmloop member reference
     * @return The computed initial rect, or null if unable to compute
     */
    public static IntRect computeFilmLoopInitialRectWithMembers(
            DirPlayer player,
            CastMemberRef memberRef) {

        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null || member.getMemberType() != MemberType.FilmLoop) {
            return null;
        }

        FilmLoopMember filmLoop = (FilmLoopMember) member.specificData;
        if (filmLoop == null) {
            return null;
        }

        int filmLoopCastLib = memberRef.getCastLib();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean foundAny = false;

        Score score = filmLoop.getScore();
        if (score == null) {
            return null;
        }

        for (FrameChannelEntry entry : score.channelInitializationData) {
            // Skip effect channels (channels 0-5 in raw data)
            if (entry.channelIndex < 6) {
                continue;
            }
            // Skip empty sprites
            if (entry.data.castMember == 0 || entry.data.castLib == 0) {
                continue;
            }

            // Resolve the cast member reference
            int spriteCastLib = entry.data.castLib == 65535 ? filmLoopCastLib : entry.data.castLib;
            CastMemberRef spriteMemberRef = new CastMemberRef(spriteCastLib, entry.data.castMember);

            // Get actual bitmap dimensions and registration point from the cast member
            int actualWidth, actualHeight, regX, regY;
            CastMember spriteMember = player.getMovie().getCastManager().findMemberByRef(spriteMemberRef);

            if (spriteMember != null && spriteMember.getMemberType() == MemberType.Bitmap) {
                BitmapMember bm = (BitmapMember) spriteMember.specificData;
                if (bm != null) {
                    // Try to get actual bitmap dimensions from BitmapManager
                    Bitmap bitmap = player.getBitmapManager().getBitmap(bm.getImageRef());
                    if (bitmap != null) {
                        actualWidth = bitmap.getWidth();
                        actualHeight = bitmap.getHeight();
                    } else {
                        actualWidth = bm.getInfo().width;
                        actualHeight = bm.getInfo().height;
                    }
                    regX = bm.getRegPointX();
                    regY = bm.getRegPointY();
                } else {
                    actualWidth = entry.data.width;
                    actualHeight = entry.data.height;
                    regX = entry.data.width / 2;
                    regY = entry.data.height / 2;
                }
            } else {
                actualWidth = entry.data.width;
                actualHeight = entry.data.height;
                regX = entry.data.width / 2;
                regY = entry.data.height / 2;
            }

            if (actualWidth == 0 && actualHeight == 0) {
                continue;
            }

            // pos_x/pos_y is the loc (registration point position)
            // The sprite's top-left corner is: pos - reg_point
            int spriteLeft = entry.data.posX - regX;
            int spriteTop = entry.data.posY - regY;
            int spriteRight = spriteLeft + actualWidth;
            int spriteBottom = spriteTop + actualHeight;

            logger.debug("  compute_initial_rect: ch {} m {}:{} pos ({}, {}) size {}x{} reg ({}, {}) -> bounds ({}, {}, {}, {})",
                entry.channelIndex, spriteMemberRef.getCastLib(), spriteMemberRef.getCastMember(),
                entry.data.posX, entry.data.posY, actualWidth, actualHeight, regX, regY,
                spriteLeft, spriteTop, spriteRight, spriteBottom);

            minX = Math.min(minX, spriteLeft);
            minY = Math.min(minY, spriteTop);
            maxX = Math.max(maxX, spriteRight);
            maxY = Math.max(maxY, spriteBottom);
            foundAny = true;
        }

        if (!foundAny) {
            return null;
        }

        IntRect result = IntRect.from(minX, minY, maxX, maxY);
        logger.debug("compute_filmloop_initial_rect_with_members: FINAL rect ({}, {}, {}, {}) size {}x{}",
            result.left, result.top, result.right, result.bottom,
            result.width(), result.height());

        return result;
    }

    /**
     * Render a filmloop directly from its channel_initialization_data.
     * This is needed because filmloop Score.channels are not populated with sprite data
     * like the main stage score. Instead, we read sprite info directly from channel_initialization_data.
     *
     * Director behavior: Film loop frames use the PARENT sprite's ink semantics,
     * not their own stored ink values.
     *
     * @param player The player
     * @param memberRef The filmloop member reference
     * @param bitmap The destination bitmap
     * @param destRect Destination rectangle
     * @param initialRect The filmloop's initial rectangle (coordinate space)
     * @param parentInk The parent sprite's ink value
     * @param parentColor The parent sprite's foreground color
     * @param parentBgColor The parent sprite's background color
     */
    public static void renderFilmLoopFromChannelData(
            DirPlayer player,
            CastMemberRef memberRef,
            Bitmap bitmap,
            IntRect destRect,
            IntRect initialRect,
            int parentInk,
            ColorRef parentColor,
            ColorRef parentBgColor) {

        PaletteMap palettes = player.getMovie().getCastManager().palettes();
        int filmLoopCastLib = memberRef.getCastLib();

        // Get filmloop data
        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null || member.getMemberType() != MemberType.FilmLoop) {
            return;
        }

        FilmLoopMember filmLoop = (FilmLoopMember) member.specificData;
        if (filmLoop == null) {
            return;
        }

        Score filmLoopScore = filmLoop.getScore();
        if (filmLoopScore == null) {
            return;
        }

        int currentFrame = filmLoop.getCurrentFrame();
        int frameIdxTarget = currentFrame - 1;  // Convert 1-based to 0-based

        // Group data by channel, keeping only valid sprite channels
        // Only consider frames <= current frame (keyframe interpolation)
        Map<Integer, ChannelData> channelMap = new HashMap<>();

        for (FrameChannelEntry entry : filmLoopScore.channelInitializationData) {
            // Skip effect channels (0-5)
            if (entry.channelIndex < 6) {
                continue;
            }
            // Skip empty sprites
            if (entry.data.castMember == 0) {
                continue;
            }
            // Only consider frames <= current frame
            if (entry.frameIndex > frameIdxTarget) {
                continue;
            }

            // Keep the most recent (highest frame_idx) data for each channel
            ChannelData existing = channelMap.get(entry.channelIndex);
            if (existing == null || entry.frameIndex > existing.frameIndex) {
                channelMap.put(entry.channelIndex, new ChannelData(entry.frameIndex, entry.data));
            }
        }

        // Calculate total frames from multiple sources
        int initDataMax = filmLoopScore.channelInitializationData.stream()
            .mapToInt(e -> e.frameIndex + 1)
            .max()
            .orElse(1);

        int spanMax = filmLoopScore.spriteSpans.stream()
            .mapToInt(span -> span.endFrame)
            .max()
            .orElse(1);

        int totalFrames = Math.max(initDataMax, spanMax);

        // Get keyframes cache for path interpolation
        Map<Integer, ChannelKeyframes> keyframesCache = filmLoopScore.keyframesCache;

        float scaleX = (float) destRect.width() / Math.max(1, initialRect.width());
        float scaleY = (float) destRect.height() / Math.max(1, initialRect.height());

        logger.debug("render_filmloop_from_channel_data: frame {}/{}, {} sprites, initial_rect ({}, {}, {}, {}), scale ({:.2f}, {:.2f})",
            currentFrame, totalFrames, channelMap.size(),
            initialRect.left, initialRect.top, initialRect.right, initialRect.bottom,
            scaleX, scaleY);

        // Sort by channel number for consistent z-ordering
        List<Map.Entry<Integer, ChannelData>> sortedEntries = new ArrayList<>(channelMap.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(a.getKey(), b.getKey()));

        for (Map.Entry<Integer, ChannelData> entry : sortedEntries) {
            int channelIndex = entry.getKey();
            ChannelData channelData = entry.getValue();
            ScoreFrameChannelData data = channelData.data;

            int channelNum = RenderingUtils.getChannelNumberFromIndex(channelIndex);

            // Build member ref from channel data
            // cast_lib 65535 means "use the filmloop's cast library"
            CastMemberRef spriteMemberRef = new CastMemberRef(
                data.castLib == 65535 ? filmLoopCastLib : data.castLib,
                data.castMember
            );

            CastMember spriteMember = player.getMovie().getCastManager().findMemberByRef(spriteMemberRef);
            if (spriteMember == null) {
                logger.debug("  channel {}: member {}:{} not found",
                    channelNum, spriteMemberRef.getCastLib(), spriteMemberRef.getCastMember());
                continue;
            }

            // Get position, potentially interpolated from path keyframes
            int posX = data.posX;
            int posY = data.posY;

            ChannelKeyframes channelKeyframes = keyframesCache.get(channelNum);
            if (channelKeyframes != null) {
                SpritePathKeyframes pathKeyframes = channelKeyframes.path;
                if (pathKeyframes != null) {
                    int[] interpolated = RenderingUtils.interpolatePathPosition(pathKeyframes, currentFrame);
                    if (interpolated != null) {
                        posX = interpolated[0];
                        posY = interpolated[1];
                    }
                }
            }

            // Get member dimensions and registration point
            int memberWidth, memberHeight, regX, regY;
            if (spriteMember.getMemberType() == MemberType.Bitmap) {
                BitmapMember bm = (BitmapMember) spriteMember.specificData;
                if (bm != null) {
                    memberWidth = bm.getInfo().width;
                    memberHeight = bm.getInfo().height;
                    regX = bm.getRegPointX();
                    regY = bm.getRegPointY();
                } else {
                    memberWidth = data.width;
                    memberHeight = data.height;
                    regX = data.width / 2;
                    regY = data.height / 2;
                }
            } else {
                memberWidth = data.width;
                memberHeight = data.height;
                regX = data.width / 2;
                regY = data.height / 2;
            }

            // Use channel data dimensions if valid
            int useWidth = (data.width > 0 && data.height > 0) ? data.width : memberWidth;
            int useHeight = (data.width > 0 && data.height > 0) ? data.height : memberHeight;

            // Coordinate transformation
            int spriteLeft = posX - regX;
            int spriteTop = posY - regY;
            int relX = spriteLeft - initialRect.left;
            int relY = spriteTop - initialRect.top;

            IntRect spriteRect = IntRect.from(
                relX,
                relY,
                relX + useWidth,
                relY + useHeight
            );

            logger.debug("  channel {}: member {}:{} type {} orig ({}, {}) interp ({}, {}) -> rect ({}, {}, {}, {})",
                channelNum, spriteMemberRef.getCastLib(), spriteMemberRef.getCastMember(),
                spriteMember.getMemberType(),
                data.posX, data.posY, posX, posY,
                spriteRect.left, spriteRect.top, spriteRect.right, spriteRect.bottom);

            // Render based on member type
            if (spriteMember.getMemberType() == MemberType.Bitmap) {
                renderFilmLoopBitmap(player, bitmap, spriteMember, spriteRect, data,
                    palettes, parentInk, parentColor, parentBgColor);
            } else if (spriteMember.getMemberType() == MemberType.Shape) {
                renderFilmLoopShape(bitmap, spriteRect, data, palettes);
            } else {
                logger.debug("  channel {}: unsupported member type {}",
                    channelNum, spriteMember.getMemberType());
            }
        }
    }

    /**
     * Render a bitmap member in a filmloop.
     */
    private static void renderFilmLoopBitmap(
            DirPlayer player,
            Bitmap destBitmap,
            CastMember member,
            IntRect spriteRect,
            ScoreFrameChannelData data,
            PaletteMap palettes,
            int parentInk,
            ColorRef parentColor,
            ColorRef parentBgColor) {

        BitmapMember bitmapMember = (BitmapMember) member.specificData;
        if (bitmapMember == null) {
            return;
        }

        Bitmap srcBitmap = player.getBitmapManager().getBitmap(bitmapMember.getImageRef());
        if (srcBitmap == null) {
            logger.debug("    Bitmap image_ref {} not found in bitmap_manager", bitmapMember.getImageRef());
            return;
        }

        IntRect srcRect = IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        IntRect dstRect = spriteRect;

        // Director behavior: Film loop internal sprites use the PARENT sprite's
        // ink, color, and bgColor
        ColorRef spriteColor = parentColor;
        ColorRef spriteBgColor = parentBgColor;
        int ink = parentInk;

        // In Director, blend=0 means "default" which is fully opaque (100)
        int blend = data.blend == 0 ? 100 : data.blend;

        // Determine if matte should be used
        boolean isIndexed = srcBitmap.getOriginalBitDepth() <= 8;
        boolean is16bit = srcBitmap.getOriginalBitDepth() == 16;
        boolean shouldUseMatte = (isIndexed && (ink == InkEffect.COPY || ink == InkEffect.MATTE))
            || (is16bit && ink == InkEffect.COPY);

        BitmapMask mask = null;
        if (shouldUseMatte) {
            if (srcBitmap.getMatte() == null) {
                srcBitmap.createMatte(palettes);
            }
            mask = srcBitmap.getMatte();
        }

        logger.debug("    Bitmap found: {}x{} bit_depth={} image_ref={} ink={} blend={} has_matte={}",
            srcBitmap.getWidth(), srcBitmap.getHeight(), srcBitmap.getBitDepth(),
            bitmapMember.getImageRef(), ink, blend, mask != null);

        CopyPixelsParams params = new CopyPixelsParams(
            blend,
            ink,
            spriteColor,
            spriteBgColor,
            mask,
            false,
            0.0f,
            null,
            dstRect
        );

        destBitmap.copyPixelsWithParams(palettes, srcBitmap, dstRect, srcRect, params);
    }

    /**
     * Render a shape member in a filmloop.
     */
    private static void renderFilmLoopShape(
            Bitmap bitmap,
            IntRect spriteRect,
            ScoreFrameChannelData data,
            PaletteMap palettes) {

        // Skip tiny shapes
        if (data.width <= 1 || data.height <= 1) {
            return;
        }

        // Get sprite foreground color from channel data
        boolean foreIsRgb = (data.colorFlag & 0x1) != 0
            || data.foreColorG != 0
            || data.foreColorB != 0;

        ColorRef spriteColor;
        if (foreIsRgb) {
            spriteColor = ColorRef.rgb(data.foreColor, data.foreColorG, data.foreColorB);
        } else {
            spriteColor = ColorRef.paletteIndex(data.foreColor);
        }

        int[] color = RenderingUtils.resolveColorRef(
            palettes,
            spriteColor,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette()),
            bitmap.getOriginalBitDepth()
        );

        logger.debug("    Shape color: fore_is_rgb={} fore_color={} -> resolved ({}, {}, {})",
            foreIsRgb, data.foreColor, color[0], color[1], color[2]);

        bitmap.fillRect(
            spriteRect.left,
            spriteRect.top,
            spriteRect.right,
            spriteRect.bottom,
            color[0], color[1], color[2],
            palettes,
            1.0f
        );
    }

    /**
     * Helper class to hold channel data with frame index.
     */
    private static class ChannelData {
        final int frameIndex;
        final ScoreFrameChannelData data;

        ChannelData(int frameIndex, ScoreFrameChannelData data) {
            this.frameIndex = frameIndex;
            this.data = data;
        }
    }
}
