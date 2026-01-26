package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Background color property.
 * Port of Rust BackColor struct from score_keyframes.rs.
 */
public class BackColor implements DirectorProperty<ColorRef, BackColor> {
    public final ColorRef color;

    public BackColor(ColorRef color) {
        this.color = color;
    }

    @Override
    public boolean useBaselineSkip() {
        return true;
    }

    @Override
    public Optional<ColorRef> extractRaw(ScoreFrameChannelData data) {
        ColorRef colorRef;
        switch (data.colorFlag) {
            case 0:
            case 1:
                // back is palette index
                colorRef = ColorRef.paletteIndex(data.backColor);
                break;
            case 2:
            case 3:
                // back is RGB
                colorRef = ColorRef.rgb(data.backColor, data.backColorG, data.backColorB);
                break;
            default:
                colorRef = ColorRef.paletteIndex(data.backColor);
                break;
        }
        return Optional.of(colorRef);
    }

    @Override
    public BackColor resolveRaw(ColorRef raw) {
        return new BackColor(raw);
    }

    @Override
    public BackColor resolveWithPrev(ColorRef raw, BackColor prev) {
        // If color is PaletteIndex(0), keep the previous value
        if (raw.isPaletteIndex() && raw.getPaletteIndex() == 0 && prev != null) {
            return prev;
        }
        return new BackColor(raw);
    }

    @Override
    public BackColor defaultValue() {
        return new BackColor(ColorRef.paletteIndex(0));
    }

    @Override
    public boolean isStandardDefault() {
        if (color.isPaletteIndex()) {
            int idx = color.getPaletteIndex();
            return idx == 0 || idx == 255;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackColor backColor = (BackColor) o;
        return Objects.equals(color, backColor.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }

    @Override
    public String toString() {
        return "BackColor(" + color + ")";
    }
}
