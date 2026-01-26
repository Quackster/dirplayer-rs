package com.dirplayer.rendering;

import com.dirplayer.player.ColorRef;
import com.dirplayer.player.bitmap.BitmapMask;
import com.dirplayer.player.Sprite;

/**
 * Parameters for copy_pixels operations.
 * Port of Rust CopyPixelsParams struct.
 */
public class CopyPixelsParams {
    public int blend;
    public int ink;
    public ColorRef color;
    public ColorRef bgColor;
    public BitmapMask maskImage;
    public boolean isTextRendering;
    public float rotation;
    public Sprite sprite;
    public IntRect originalDstRect;

    public CopyPixelsParams() {
        this.blend = 100;
        this.ink = 0;
        this.color = ColorRef.paletteIndex(255);  // Black
        this.bgColor = ColorRef.paletteIndex(0);  // White
        this.maskImage = null;
        this.isTextRendering = false;
        this.rotation = 0.0f;
        this.sprite = null;
        this.originalDstRect = null;
    }

    public CopyPixelsParams(int blend, int ink, ColorRef color, ColorRef bgColor,
                           BitmapMask maskImage, boolean isTextRendering,
                           float rotation, Sprite sprite, IntRect originalDstRect) {
        this.blend = blend;
        this.ink = ink;
        this.color = color;
        this.bgColor = bgColor;
        this.maskImage = maskImage;
        this.isTextRendering = isTextRendering;
        this.rotation = rotation;
        this.sprite = sprite;
        this.originalDstRect = originalDstRect;
    }

    public CopyPixelsParams copy() {
        return new CopyPixelsParams(
            blend, ink, color, bgColor,
            maskImage != null ? maskImage.copy() : null,
            isTextRendering, rotation,
            sprite, // Note: sprite is not deep copied
            originalDstRect != null ? originalDstRect.copy() : null
        );
    }
}
