package com.dirplayer.player.score;

import com.dirplayer.player.CastMemberRef;

import java.util.Objects;

/**
 * Reference to a score - either the main stage or a film loop's score.
 * Used to determine which score context to operate on.
 * Port of Rust ScoreRef enum.
 */
public class ScoreRef {

    /**
     * The type of score reference.
     */
    public enum Type {
        /** The main stage score */
        STAGE,
        /** A film loop's embedded score */
        FILM_LOOP
    }

    private final Type type;
    private final CastMemberRef memberRef;

    private ScoreRef(Type type, CastMemberRef memberRef) {
        this.type = type;
        this.memberRef = memberRef;
    }

    /**
     * Create a reference to the main stage score.
     */
    public static ScoreRef stage() {
        return new ScoreRef(Type.STAGE, null);
    }

    /**
     * Create a reference to a film loop's score.
     */
    public static ScoreRef filmLoop(CastMemberRef memberRef) {
        return new ScoreRef(Type.FILM_LOOP, memberRef);
    }

    /**
     * Create a reference to a film loop's score.
     */
    public static ScoreRef filmLoop(int castLib, int castMember) {
        return new ScoreRef(Type.FILM_LOOP, new CastMemberRef(castLib, castMember));
    }

    public Type getType() {
        return type;
    }

    public boolean isStage() {
        return type == Type.STAGE;
    }

    public boolean isFilmLoop() {
        return type == Type.FILM_LOOP;
    }

    public CastMemberRef getMemberRef() {
        return memberRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreRef scoreRef = (ScoreRef) o;
        if (type != scoreRef.type) return false;
        return Objects.equals(memberRef, scoreRef.memberRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, memberRef);
    }

    @Override
    public String toString() {
        if (type == Type.STAGE) {
            return "ScoreRef{STAGE}";
        } else {
            return "ScoreRef{FILM_LOOP " + memberRef + "}";
        }
    }
}
