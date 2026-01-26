package com.dirplayer.player.rendering;

import com.dirplayer.director.MemberType;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.cast.BitmapMember;
import com.dirplayer.player.cast.FilmLoopMember;
import com.dirplayer.rendering.IntRect;
import com.dirplayer.rendering.ScoreRef;
import com.dirplayer.SimpleLogger;


import java.util.HashMap;

/**
 * Preview rendering for cast members.
 * Renders preview images of cast members for the debugger/editor UI.
 * Port of draw_preview_frame from Rust.
 */
public class PreviewRenderer {
    private static final SimpleLogger logger = SimpleLogger.getLogger(PreviewRenderer.class);

    private PreviewRenderer() {
        // Prevent instantiation
    }

    /**
     * Render a preview of a cast member.
     *
     * @param player The player
     * @param memberRef The cast member reference to preview
     * @param maxWidth Maximum preview width (0 for no limit)
     * @param maxHeight Maximum preview height (0 for no limit)
     * @return The rendered preview bitmap, or null if unable to render
     */
    public static Bitmap renderMemberPreview(
            DirPlayer player,
            CastMemberRef memberRef,
            int maxWidth,
            int maxHeight) {

        CastMember member = player.getMovie().getCastManager().findMemberByRef(memberRef);
        if (member == null) {
            return null;
        }

        MemberType memberType = member.getMemberType();

        switch (memberType) {
            case Bitmap:
                return renderBitmapPreview(player, member);
            case FilmLoop:
                return renderFilmLoopPreview(player, member, memberRef);
            default:
                logger.debug("Preview not implemented for member type: {}", memberType);
                return null;
        }
    }

    /**
     * Render a preview of a bitmap cast member.
     */
    private static Bitmap renderBitmapPreview(DirPlayer player, CastMember member) {
        BitmapMember bitmapMember = (BitmapMember) member.specificData;
        if (bitmapMember == null) {
            return null;
        }

        Bitmap srcBitmap = player.getBitmapManager().getBitmap(bitmapMember.getImageRef());
        if (srcBitmap == null) {
            return null;
        }

        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();

        // Create preview bitmap
        Bitmap previewBitmap = new Bitmap(
            width,
            height,
            32,
            32,
            0,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette())
        );

        PaletteMap palettes = player.getMovie().getCastManager().palettes();

        // Fill with background color
        ColorRef bgColorRef = player.getBgColor();
        int[] bgColor = RenderingUtils.resolveColorRef(
            palettes, bgColorRef,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette()),
            srcBitmap.getOriginalBitDepth()
        );
        previewBitmap.fillRect(0, 0, width, height, bgColor[0], bgColor[1], bgColor[2], palettes, 1.0f);

        // Copy bitmap pixels
        previewBitmap.copyPixels(
            palettes,
            srcBitmap,
            IntRect.from(0, 0, width, height),
            IntRect.from(0, 0, width, height),
            new HashMap<>(),
            null
        );

        // Draw registration point marker (magenta pixel)
        int regX = bitmapMember.getRegPointX();
        int regY = bitmapMember.getRegPointY();
        previewBitmap.setPixel(regX, regY, 255, 0, 255, palettes);

        return previewBitmap;
    }

    /**
     * Render a preview of a filmloop cast member.
     */
    private static Bitmap renderFilmLoopPreview(
            DirPlayer player,
            CastMember member,
            CastMemberRef memberRef) {

        FilmLoopMember filmLoop = (FilmLoopMember) member.specificData;
        if (filmLoop == null) {
            return null;
        }

        // Get sprite rect for the preview
        // Use filmloop info dimensions
        int width = filmLoop.getInfo().width;
        int height = filmLoop.getInfo().height;

        if (width <= 0 || height <= 0) {
            // Try to compute from initial rect
            IntRect initialRect = FilmLoopRenderer.computeFilmLoopInitialRectWithMembers(player, memberRef);
            if (initialRect != null && !initialRect.isEmpty()) {
                width = initialRect.width();
                height = initialRect.height();
            } else {
                width = 100;
                height = 100;
            }
        }

        // Create preview bitmap
        Bitmap previewBitmap = new Bitmap(
            width,
            height,
            32,
            32,
            0,
            PaletteRef.ofBuiltIn(RenderingUtils.getSystemDefaultPalette())
        );

        // Render the filmloop score
        StageRenderer.renderScoreToBitmap(
            player,
            ScoreRef.filmLoop(memberRef),
            previewBitmap,
            null,
            IntRect.fromSize(0, 0, width, height)
        );

        return previewBitmap;
    }
}
