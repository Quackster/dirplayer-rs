package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Width property.
 * Port of Rust Width struct from score_keyframes.rs.
 */
public class Width implements DirectorProperty<Integer, Width> {
    public final int value;

    public Width(int value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Integer> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.width);
    }

    @Override
    public Width resolveRaw(Integer raw) {
        return new Width(raw);
    }

    @Override
    public Width defaultValue() {
        return new Width(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Width width = (Width) o;
        return value == width.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Width(" + value + ")";
    }
}
