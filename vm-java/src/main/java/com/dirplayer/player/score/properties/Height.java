package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Height property.
 * Port of Rust Height struct from score_keyframes.rs.
 */
public class Height implements DirectorProperty<Integer, Height> {
    public final int value;

    public Height(int value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Integer> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.height);
    }

    @Override
    public Height resolveRaw(Integer raw) {
        return new Height(raw);
    }

    @Override
    public Height defaultValue() {
        return new Height(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Height height = (Height) o;
        return value == height.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Height(" + value + ")";
    }
}
