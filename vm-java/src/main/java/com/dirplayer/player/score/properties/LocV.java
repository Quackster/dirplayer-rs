package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Vertical location property.
 * Port of Rust LocV struct from score_keyframes.rs.
 */
public class LocV implements DirectorProperty<Short, LocV> {
    public final short value;

    public LocV(short value) {
        this.value = value;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<Short> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(data.posY);
    }

    @Override
    public LocV resolveRaw(Short raw) {
        return new LocV(raw);
    }

    @Override
    public LocV defaultValue() {
        return new LocV((short) 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocV locV = (LocV) o;
        return value == locV.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "LocV(" + value + ")";
    }
}
