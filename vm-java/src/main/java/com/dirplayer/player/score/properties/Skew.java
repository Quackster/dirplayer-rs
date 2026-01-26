package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Skew property (in degrees).
 * Port of Rust Skew struct from score_keyframes.rs.
 */
public class Skew implements DirectorProperty<Double, Skew> {
    public final double value;

    public Skew(double value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Double> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.skew);
    }

    @Override
    public Skew resolveRaw(Double raw) {
        return new Skew(raw);
    }

    @Override
    public Skew defaultValue() {
        return new Skew(0.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skew skew = (Skew) o;
        return Double.compare(skew.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Skew(" + value + ")";
    }
}
