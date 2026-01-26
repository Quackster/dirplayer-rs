package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Foreground color property.
 * Port of Rust ForeColor struct from score_keyframes.rs.
 */
public class ForeColor implements DirectorProperty<ColorRef, ForeColor> {
    public final ColorRef color;

    public ForeColor(ColorRef color) {
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
            case 2:
                // fore is palette index
                colorRef = ColorRef.paletteIndex(data.foreColor);
                break;
            case 1:
            case 3:
                // fore is RGB
                colorRef = ColorRef.rgb(data.foreColor, data.foreColorG, data.foreColorB);
                break;
            default:
                colorRef = ColorRef.paletteIndex(data.foreColor);
                break;
        }
        return Optional.of(colorRef);
    }

    @Override
    public ForeColor resolveRaw(ColorRef raw) {
        return new ForeColor(raw);
    }

    @Override
    public ForeColor resolveWithPrev(ColorRef raw, ForeColor prev) {
        // If color is PaletteIndex(0), keep the previous value
        if (raw.isPaletteIndex() && raw.getPaletteIndex() == 0 && prev != null) {
            return prev;
        }
        return new ForeColor(raw);
    }

    @Override
    public ForeColor defaultValue() {
        return new ForeColor(ColorRef.paletteIndex(255));
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
        ForeColor foreColor = (ForeColor) o;
        return Objects.equals(color, foreColor.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }

    @Override
    public String toString() {
        return "ForeColor(" + color + ")";
    }
}
