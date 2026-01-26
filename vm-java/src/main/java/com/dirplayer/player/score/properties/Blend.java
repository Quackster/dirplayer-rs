package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Blend property (0-100 percent).
 * Port of Rust Blend struct from score_keyframes.rs.
 */
public class Blend implements DirectorProperty<Integer, Blend> {
    public final int value;

    public Blend(int value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Integer> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.blend);
    }

    @Override
    public Blend resolveRaw(Integer raw) {
        return new Blend(raw);
    }

    @Override
    public Blend defaultValue() {
        return new Blend(100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blend blend = (Blend) o;
        return value == blend.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Blend(" + value + ")";
    }
}
