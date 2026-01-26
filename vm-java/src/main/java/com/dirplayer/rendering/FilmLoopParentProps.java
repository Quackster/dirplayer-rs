package com.dirplayer.rendering;

import com.dirplayer.player.ColorRef;

/**
 * Parent sprite properties for film loop rendering.
 * In Director, film loop internal sprites use the parent sprite's ink semantics.
 * Port of Rust FilmLoopParentProps struct.
 */
public class FilmLoopParentProps {
    public int ink;
    public ColorRef color;
    public ColorRef bgColor;

    public FilmLoopParentProps() {
        this.ink = 0;  // Default to copy ink
        this.color = ColorRef.paletteIndex(255);  // Default foreground (black)
        this.bgColor = ColorRef.paletteIndex(0);  // Default background (white)
    }

    public FilmLoopParentProps(int ink, ColorRef color, ColorRef bgColor) {
        this.ink = ink;
        this.color = color;
        this.bgColor = bgColor;
    }
}
