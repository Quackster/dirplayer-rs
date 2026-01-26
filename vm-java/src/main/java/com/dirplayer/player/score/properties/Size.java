package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Combined size property (for size keyframes).
 * Port of Rust Size struct from score_keyframes.rs.
 */
public class Size implements DirectorProperty<int[], Size> {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<int[]> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(new int[] { data.width, data.height });
    }

    @Override
    public Size resolveRaw(int[] raw) {
        return new Size(raw[0], raw[1]);
    }

    @Override
    public Size resolveWithPrev(int[] raw, Size prev) {
        if (prev == null) {
            return new Size(raw[0], raw[1]);
        }
        // If a component is 0, keep the previous value
        return new Size(
            raw[0] == 0 ? prev.width : raw[0],
            raw[1] == 0 ? prev.height : raw[1]
        );
    }

    @Override
    public Size defaultValue() {
        return new Size(0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Size size = (Size) o;
        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Size(" + width + ", " + height + ")";
    }
}
