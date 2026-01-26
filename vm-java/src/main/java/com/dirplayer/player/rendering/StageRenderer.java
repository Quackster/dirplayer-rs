package com.dirplayer.player.rendering;

import com.dirplayer.director.MemberType;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.cast.BitmapMember;
import com.dirplayer.player.cast.FieldMember;
import com.dirplayer.player.cast.FilmLoopMember;
import com.dirplayer.player.cast.ShapeMember;
import com.dirplayer.player.cast.TextMember;
import com.dirplayer.player.score.Score;
import com.dirplayer.player.score.SpriteChannel;
import com.dirplayer.rendering.CopyPixelsParams;
import com.dirplayer.rendering.FilmLoopParentProps;
import com.dirplayer.rendering.IntRect;
import com.dirplayer.rendering.ScoreRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stage rendering logic.
 * Handles rendering the main stage and score sprites.
 * Port of render_stage_to_bitmap and render_score_to_bitmap from Rust.
 */
public class StageRenderer {
    private static final Logger logger = LoggerFactory.getLogger(StageRenderer.class);

    private StageRenderer() {
        // Prevent instantiation
    }

    /**
     * Render the stage to a bitmap.
     *
     * @param player The player
     * @param bitmap The destination bitmap
     * @param debugSpriteNum Optional sprite number to highlight for debugging
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

        RenderingUtils.drawCursor(player, bitmap, palettes);
    }

    /**
     * Render a score to a bitmap.
     *
     * @param player The player
     * @param scoreSource The score source (stage or filmloop)
     * @param bitmap The destination bitmap
     * @param debugSpriteNum Optional sprite number to highlight
     * @param destRect Destination rectangle
     */
    public static void renderScoreToBitmap(
            DirPlayer player,
            ScoreRef scoreSource,
            Bitmap bitmap,
            Integer debugSpriteNum,
            IntRect destRect) {

        renderScoreToBitmapWithOffset(
            player, scoreSource, bitmap, debugSpriteNum, destRect, 0, 0, null);
    }

    /**
     * Render a score to a bitmap with optional coordinate offset.
     * The offset is used for filmloop rendering where sprite coordinates need to be
     * translated relative to the filmloop's initial_rect.
     *
     * @param player The player
     * @param scoreSource The score source
     * @param bitmap The destination bitmap
     * @param debugSpriteNum Optional debug sprite number
     * @param destRect Destination rectangle
     * @param offsetX X offset for filmloop coordinate translation
     * @param offsetY Y offset for filmloop coordinate translation
     * @param parentProps Parent sprite properties for filmloop rendering
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

        // For filmloops, use transparent background and delegate to FilmLoopRenderer
        if (scoreSource.isFilmLoop()) {
            bitmap.clearRectTransparent(destRect.left, destRect.top, destRect.right, destRect.bottom);

            CastMemberRef memberRef = scoreSource.getMemberRef();
            CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
            if (member == null || member.getMemberType() != MemberType.FilmLoop) {
                return;
            }

            // Get initial rect from filmloop member info
            FilmLoopMember filmLoop = (FilmLoopMember) member.specificData;
            if (filmLoop == null) {
                return;
            }

            IntRect initialRect = filmLoop.getInitialRect();
            if (initialRect == null || initialRect.isEmpty()) {
                // Compute initial rect from member data
                initialRect = FilmLoopRenderer.computeFilmLoopInitialRectWithMembers(player, memberRef);
                if (initialRect == null) {
                    initialRect = IntRect.from(0, 0, 1, 1);
                }
            }

            // Get parent props or use defaults
            FilmLoopParentProps props = parentProps;
            if (props == null) {
                props = new FilmLoopParentProps(
                    InkEffect.COPY,
                    ColorRef.paletteIndex(255),
                    ColorRef.paletteIndex(0)
                );
            }

            FilmLoopRenderer.renderFilmLoopFromChannelData(
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
        ColorRef bgColorRef = player.getBgColor();
        int[] bgColor = RenderingUtils.resolveColorRef(
            palettes, bgColorRef,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette()),
            bitmap.getOriginalBitDepth()
        );
        bitmap.clearRect(destRect.left, destRect.top, destRect.right, destRect.bottom,
            bgColor[0], bgColor[1], bgColor[2], palettes);

        // Get sorted channel numbers
        Movie movie = player.getMovie();
        Score score = movie.getScore();
        int frameNum = movie.getCurrentFrame();

        List<SpriteChannel> sortedChannels = score.getSortedChannels(frameNum);
        List<Integer> sortedChannelNumbers = sortedChannels.stream()
            .map(ch -> ch.number)
            .collect(Collectors.toList());

        logger.debug("STAGE RENDER: frame {} channels {}", frameNum, sortedChannelNumbers);

        // Render each sprite
        for (SpriteChannel channel : sortedChannels) {
            Sprite sprite = channel.sprite;
            CastMemberRef memberRef = sprite.getMember();
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }

            CastMember member = movie.getCastManager().findMemberByRef(memberRef);
            if (member == null) {
                logger.debug("  STAGE channel {} SKIPPED: member {}:{} not found",
                    channel.number, memberRef.getCastLib(), memberRef.getCastMember());
                continue;
            }

            logger.debug("  STAGE channel {}: member {}:{} type {}",
                channel.number, memberRef.getCastLib(), memberRef.getCastMember(),
                member.getMemberType());

            renderSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
        }

        // Draw debug rect
        if (debugSpriteNum != null) {
            Sprite sprite = score.getSprite(debugSpriteNum.shortValue());
            if (sprite != null) {
                IntRect spriteRect = RenderingUtils.getConcreteSpriteRect(player, sprite);
                bitmap.strokeRect(spriteRect.left, spriteRect.top,
                    spriteRect.right, spriteRect.bottom, 255, 0, 0, palettes, 1.0f);
                bitmap.setPixel(sprite.getLocH(), sprite.getLocV(), 0, 255, 0, palettes);
            }
        }

        // Draw pick rect if alt+ctrl/cmd is held
        if (player.keyboardManager.isAltDown() &&
            (player.keyboardManager.isControlDown() || player.keyboardManager.isCommandDown())) {
            int hoveredSprite = RenderingUtils.getSpriteAt(player, player.mouseLocX, player.mouseLocY, false);
            if (hoveredSprite > 0) {
                Sprite sprite = score.getSprite((short) hoveredSprite);
                if (sprite != null) {
                    IntRect spriteRect = RenderingUtils.getConcreteSpriteRect(player, sprite);
                    bitmap.strokeRect(spriteRect.left, spriteRect.top,
                        spriteRect.right, spriteRect.bottom, 0, 255, 0, palettes, 1.0f);
                }
            }
        }
    }

    /**
     * Render a single sprite to the bitmap.
     */
    private static void renderSprite(
            DirPlayer player,
            Bitmap bitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes,
            int offsetX,
            int offsetY) {

        MemberType memberType = member.getMemberType();

        switch (memberType) {
            case Bitmap:
                renderBitmapSprite(player, bitmap, sprite, member, palettes);
                break;
            case Shape:
                renderShapeSprite(player, bitmap, sprite, member, palettes, offsetX, offsetY);
                break;
            case Button:
            case RTE:
                renderFieldSprite(player, bitmap, sprite, member, palettes);
                break;
            case Text:
                renderTextSprite(player, bitmap, sprite, member, palettes);
                break;
            case FilmLoop:
                renderFilmLoopSprite(player, bitmap, sprite, member, palettes);
                break;
            default:
                logger.debug("Unsupported member type for rendering: {}", memberType);
                break;
        }
    }

    /**
     * Render a bitmap sprite.
     */
    private static void renderBitmapSprite(
            DirPlayer player,
            Bitmap destBitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes) {

        BitmapMember bitmapMember = (BitmapMember) member.specificData;
        if (bitmapMember == null) {
            return;
        }

        Bitmap srcBitmap = player.getBitmapManager().getBitmap(bitmapMember.getImageRef());
        if (srcBitmap == null) {
            return;
        }

        IntRect spriteRect = RenderingUtils.getConcreteSpriteRect(player, sprite);
        IntRect logicalRect = spriteRect.copy();

        // Determine source rect based on sprite/bitmap size relationship
        IntRect srcRect;
        if (sprite.hasSizeTweened() || sprite.hasSizeChanged()) {
            srcRect = IntRect.from(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());
        } else if (sprite.getWidth() > player.getMovie().getRect().width() &&
                   sprite.getHeight() > player.getMovie().getRect().height()) {
            srcRect = IntRect.from(0, 0, bitmapMember.getInfo().width, bitmapMember.getInfo().height);
        } else if (bitmapMember.getInfo().width == 0 && bitmapMember.getInfo().height == 0) {
            srcRect = IntRect.from(0, 0, sprite.getWidth(), sprite.getHeight());
        } else if ((bitmapMember.getInfo().width < sprite.getWidth() &&
                    bitmapMember.getInfo().height < sprite.getHeight()) ||
                   (bitmapMember.getInfo().width > sprite.getWidth() &&
                    bitmapMember.getInfo().height > sprite.getHeight())) {
            srcRect = IntRect.from(0, 0, bitmapMember.getInfo().width, bitmapMember.getInfo().height);
        } else if (sprite.getWidth() > bitmapMember.getInfo().width ||
                   sprite.getHeight() > bitmapMember.getInfo().height) {
            srcRect = IntRect.from(0, 0, bitmapMember.getInfo().width, bitmapMember.getInfo().height);
        } else {
            srcRect = IntRect.from(0, 0, sprite.getWidth(), sprite.getHeight());
        }

        // Apply flipping
        IntRect dstRect = RenderingUtils.applyFlip(spriteRect, sprite.isFlipH(), sprite.isFlipV());

        // Get matte mask if needed
        BitmapMask mask = null;
        if (RenderingUtils.shouldMatteSprite(sprite.getInk())) {
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
            logicalRect
        );

        logger.debug("DRAW Sprite {} dimensions {}x{} bitmap dimensions {}x{} src_rect: {}",
            sprite.getNumber(), sprite.getWidth(), sprite.getHeight(),
            srcBitmap.getWidth(), srcBitmap.getHeight(), srcRect);

        destBitmap.copyPixelsWithParams(palettes, srcBitmap, dstRect, srcRect, params);
    }

    /**
     * Render a shape sprite.
     */
    private static void renderShapeSprite(
            DirPlayer player,
            Bitmap bitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes,
            int offsetX,
            int offsetY) {

        // Skip tiny shapes (placeholders)
        if (sprite.getWidth() <= 1 || sprite.getHeight() <= 1) {
            return;
        }

        // Skip placeholder 1:1
        CastMemberRef memberRef = sprite.getMember();
        if (memberRef != null && memberRef.getCastLib() == 1 && memberRef.getCastMember() == 1) {
            return;
        }

        logger.debug("  SHAPE RENDER: channel {} member {} size {}x{} color {} bg {} ink {} blend {}",
            sprite.getNumber(), memberRef, sprite.getWidth(), sprite.getHeight(),
            sprite.getColor(), sprite.getBgColor(), sprite.getInk(), sprite.getBlend());

        IntRect rect = RenderingUtils.getConcreteSpriteRect(player, sprite);

        // Apply offset for filmloop coordinate translation
        IntRect spriteRect = IntRect.from(
            rect.left - offsetX,
            rect.top - offsetY,
            rect.right - offsetX,
            rect.bottom - offsetY
        );

        // Get foreground color
        ColorRef color = sprite.getColor();
        int[] rgb = RenderingUtils.resolveColorRef(
            palettes, color,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette()),
            bitmap.getOriginalBitDepth()
        );

        bitmap.fillRect(spriteRect.left, spriteRect.top,
            spriteRect.right, spriteRect.bottom, rgb[0], rgb[1], rgb[2], palettes, 1.0f);
    }

    /**
     * Render a field sprite.
     */
    private static void renderFieldSprite(
            DirPlayer player,
            Bitmap bitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes) {

        FieldMember fieldMember = (FieldMember) member.specificData;
        if (fieldMember == null) {
            return;
        }

        var font = RenderingUtils.getOrLoadFont(
            player.fontManager,
            player.getMovie().getCastManager(),
            fieldMember.getFont(),
            fieldMember.getFontSize(),
            null
        );

        if (font == null) {
            return;
        }

        // Get font bitmap
        Bitmap fontBitmap = player.getBitmapManager().getBitmap(font.bitmapRef);
        if (fontBitmap == null) {
            logger.debug("Font bitmap not found for field rendering");
            return;
        }

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            null,  // mask
            true,  // isTextRendering
            0.0f,
            null,
            null
        );

        bitmap.drawText(
            fieldMember.getText(),
            font,
            fontBitmap,
            sprite.getLocH(),
            sprite.getLocV(),
            params,
            palettes,
            fieldMember.getFixedLineSpace(),
            fieldMember.getTopSpacing()
        );

        // Draw cursor if this field has keyboard focus
        if (player.keyboardFocusSprite == sprite.getNumber()) {
            int cursorX = sprite.getLocH() + (sprite.getWidth() / 2);
            int cursorY = sprite.getLocV();
            int cursorWidth = 1;
            int cursorHeight = font.charHeight;

            bitmap.fillRect(
                cursorX,
                cursorY,
                cursorX + cursorWidth,
                cursorY + cursorHeight,
                0, 0, 0,  // black cursor
                palettes,
                1.0f
            );
        }
    }

    /**
     * Render a text sprite.
     */
    private static void renderTextSprite(
            DirPlayer player,
            Bitmap bitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes) {

        TextMember textMember = (TextMember) member.specificData;
        if (textMember == null) {
            return;
        }

        var font = RenderingUtils.getOrLoadFont(
            player.fontManager,
            player.getMovie().getCastManager(),
            textMember.getFont(),
            textMember.getFontSize(),
            null
        );

        if (font == null) {
            return;
        }

        // Get font bitmap
        Bitmap fontBitmap = player.getBitmapManager().getBitmap(font.bitmapRef);
        if (fontBitmap == null) {
            logger.debug("Font bitmap not found for text rendering");
            return;
        }

        CopyPixelsParams params = new CopyPixelsParams(
            sprite.getBlend(),
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor(),
            null,  // mask
            true,  // isTextRendering
            0.0f,
            null,
            null
        );

        bitmap.drawText(
            textMember.getText(),
            font,
            fontBitmap,
            sprite.getLocH(),
            sprite.getLocV(),
            params,
            palettes,
            textMember.getFixedLineSpace(),
            textMember.getTopSpacing()
        );
    }

    /**
     * Render a filmloop sprite.
     */
    private static void renderFilmLoopSprite(
            DirPlayer player,
            Bitmap bitmap,
            Sprite sprite,
            CastMember member,
            PaletteMap palettes) {

        FilmLoopMember filmLoop = (FilmLoopMember) member.specificData;
        if (filmLoop == null) {
            return;
        }

        CastMemberRef memberRef = sprite.getMember();

        // Get initial rect from filmloop info
        IntRect initialRect = filmLoop.getInitialRect();
        if (initialRect == null || initialRect.isEmpty()) {
            initialRect = FilmLoopRenderer.computeFilmLoopInitialRectWithMembers(player, memberRef);
            if (initialRect == null) {
                initialRect = IntRect.from(0, 0, 1, 1);
            }
        }

        IntRect spriteRect = RenderingUtils.getConcreteSpriteRect(player, sprite);

        // Create filmloop bitmap at natural size
        int width = Math.max(1, initialRect.width());
        int height = Math.max(1, initialRect.height());

        logger.debug("Rendering FilmLoop: channel {} frame {} ink={} blend={} initial_rect {} bitmap size {}x{} sprite_rect {}",
            sprite.getNumber(), filmLoop.getCurrentFrame(), sprite.getInk(), sprite.getBlend(),
            initialRect, width, height, spriteRect);

        Bitmap filmLoopBitmap = new Bitmap(
            width,
            height,
            32,
            32,
            8,  // alpha depth for transparency
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette())
        );
        filmLoopBitmap.setUseAlpha(true);
        filmLoopBitmap.clearData();

        // Render filmloop content
        FilmLoopParentProps parentProps = new FilmLoopParentProps(
            sprite.getInk(),
            sprite.getColor(),
            sprite.getBgColor()
        );

        renderScoreToBitmapWithOffset(
            player,
            ScoreRef.filmLoop(memberRef),
            filmLoopBitmap,
            null,  // no debug sprite
            IntRect.fromSize(0, 0, width, height),
            initialRect.left,
            initialRect.top,
            parentProps
        );

        // Composite filmloop onto stage
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

        // Position at sprite_rect location, keeping natural size
        IntRect dstRect = IntRect.fromSize(spriteRect.left, spriteRect.top, width, height);
        bitmap.copyPixelsWithParams(
            palettes,
            filmLoopBitmap,
            dstRect,
            IntRect.fromSize(0, 0, width, height),
            params
        );
    }
}
