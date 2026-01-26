package com.dirplayer.player.score.properties;

import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.player.score.DirectorProperty;

import java.util.Objects;
import java.util.Optional;

/**
 * Combined position property (for path keyframes).
 * Port of Rust Position struct from score_keyframes.rs.
 */
public class Position implements DirectorProperty<short[], Position> {
    public final short x;
    public final short y;

    public Position(short x, short y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean useBaselineSkip() {
        return false;
    }

    @Override
    public Optional<short[]> extractRaw(ScoreFrameChannelData data) {
        return Optional.of(new short[] { data.posX, data.posY });
    }

    @Override
    public Position resolveRaw(short[] raw) {
        return new Position(raw[0], raw[1]);
    }

    @Override
    public Position resolveWithPrev(short[] raw, Position prev) {
        if (prev == null) {
            return new Position(raw[0], raw[1]);
        }
        // If a component is 0, keep the previous value
        return new Position(
            raw[0] == 0 ? prev.x : raw[0],
            raw[1] == 0 ? prev.y : raw[1]
        );
    }

    @Override
    public Position defaultValue() {
        return new Position((short) 0, (short) 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Position(" + x + ", " + y + ")";
    }
}
