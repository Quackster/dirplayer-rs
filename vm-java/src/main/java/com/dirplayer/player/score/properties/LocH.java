package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Horizontal location property.
 * Port of Rust LocH struct from score_keyframes.rs.
 */
public class LocH implements DirectorProperty<Short, LocH> {
    public final short value;

    public LocH(short value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Short> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.posX);
    }

    @Override
    public LocH resolveRaw(Short raw) {
        return new LocH(raw);
    }

    @Override
    public LocH defaultValue() {
        return new LocH((short) 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocH locH = (LocH) o;
        return value == locH.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "LocH(" + value + ")";
    }
}
