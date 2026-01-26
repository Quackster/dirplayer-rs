package com.dirplayer.player.rendering;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.rendering.IntRect;
import com.dirplayer.rendering.ScoreRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main renderer for the Director player.
 * Handles canvas/context management and frame rendering.
 * Port of Rust PlayerCanvasRenderer struct.
 *
 * This class is designed to work with TeaVM for browser rendering.
 * Platform-specific rendering is abstracted through the CanvasContext interface.
 */
public class PlayerCanvasRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PlayerCanvasRenderer.class);

    /** Canvas context abstraction for platform-specific rendering */
    private CanvasContext canvasContext;

    /** Preview canvas context for member preview */
    private CanvasContext previewContext;

    /** Main stage size */
    private int width;
    private int height;

    /** Preview size */
    private int previewWidth;
    private int previewHeight;

    /** Member being previewed */
    private CastMemberRef previewMemberRef;

    /** Debug selected channel number */
    private Integer debugSelectedChannelNum;

    /** Reusable bitmap for stage rendering */
    private Bitmap stageBitmap;

    public PlayerCanvasRenderer() {
        this.width = 1;
        this.height = 1;
        this.previewWidth = 1;
        this.previewHeight = 1;
        this.previewMemberRef = null;
        this.debugSelectedChannelNum = null;
        this.stageBitmap = new Bitmap(1, 1, 32, 32, 0,
            PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin));
    }

    /**
     * Create renderer with a canvas context.
     */
    public PlayerCanvasRenderer(CanvasContext canvasContext) {
        this();
        this.canvasContext = canvasContext;
    }

    /**
     * Set the main canvas size.
     */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        if (canvasContext != null) {
            canvasContext.setSize(width, height);
        }
    }

    /**
     * Set the preview canvas size.
     */
    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
        if (previewContext != null) {
            previewContext.setSize(width, height);
        }
    }

    /**
     * Get the main canvas size.
     */
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Set the canvas context.
     */
    public void setCanvasContext(CanvasContext context) {
        this.canvasContext = context;
    }

    /**
     * Set the preview canvas context.
     */
    public void setPreviewContext(CanvasContext context) {
        this.previewContext = context;
    }

    /**
     * Set the preview member reference.
     */
    public void setPreviewMemberRef(CastMemberRef memberRef) {
        this.previewMemberRef = memberRef;
    }

    /**
     * Get the preview member reference.
     */
    public CastMemberRef getPreviewMemberRef() {
        return previewMemberRef;
    }

    /**
     * Set the debug selected channel number.
     */
    public void setDebugSelectedChannelNum(Integer channelNum) {
        this.debugSelectedChannelNum = channelNum;
    }

    /**
     * Get the debug selected channel number.
     */
    public Integer getDebugSelectedChannelNum() {
        return debugSelectedChannelNum;
    }

    /**
     * Draw a frame to the main canvas.
     */
    public void drawFrame(DirPlayer player) {
        int movieWidth = player.getMovie().getRect().width();
        int movieHeight = player.getMovie().getRect().height();

        // Resize bitmap if needed
        if (stageBitmap.getWidth() != movieWidth || stageBitmap.getHeight() != movieHeight) {
            stageBitmap = new Bitmap(
                movieWidth,
                movieHeight,
                32,
                32,
                0,
                PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin)
            );
        }

        // Render stage to bitmap
        StageRenderer.renderStageToBitmap(player, stageBitmap, debugSelectedChannelNum);

        // Output to canvas context
        if (canvasContext != null) {
            canvasContext.putImageData(stageBitmap.getData(), movieWidth, movieHeight);
        }
    }

    /**
     * Draw the preview frame.
     */
    public void drawPreviewFrame(DirPlayer player) {
        if (previewMemberRef == null || previewContext == null) {
            return;
        }

        // Get member and render preview based on type
        var member = player.getMovie().getCastManager().findMemberByRef(previewMemberRef);
        if (member == null) {
            return;
        }

        // Create preview bitmap based on member type
        Bitmap previewBitmap = PreviewRenderer.renderMemberPreview(
            player, previewMemberRef, previewWidth, previewHeight);

        if (previewBitmap != null) {
            // Update preview size if needed
            if (previewWidth != previewBitmap.getWidth() ||
                previewHeight != previewBitmap.getHeight()) {
                setPreviewSize(previewBitmap.getWidth(), previewBitmap.getHeight());
            }
            previewContext.putImageData(
                previewBitmap.getData(),
                previewBitmap.getWidth(),
                previewBitmap.getHeight()
            );
        }
    }

    /**
     * Get the stage bitmap for direct access.
     */
    public Bitmap getStageBitmap() {
        return stageBitmap;
    }
}
