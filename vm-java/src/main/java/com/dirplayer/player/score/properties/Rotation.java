package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Rotation property (in degrees).
 * Port of Rust Rotation struct from score_keyframes.rs.
 */
public class Rotation implements DirectorProperty<Double, Rotation> {
    public final double value;

    public Rotation(double value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Double> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.rotation);
    }

    @Override
    public Rotation resolveRaw(Double raw) {
        return new Rotation(raw);
    }

    @Override
    public Rotation defaultValue() {
        return new Rotation(0.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rotation rotation = (Rotation) o;
        return Double.compare(rotation.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Rotation(" + value + ")";
    }
}
