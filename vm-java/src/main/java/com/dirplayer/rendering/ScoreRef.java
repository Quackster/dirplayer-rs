package com.dirplayer.rendering;

import com.dirplayer.player.CastMemberRef;

/**
 * Reference to a score for rendering.
 * Can be either the main stage or a film loop.
 * Port of Rust ScoreRef enum.
 */
public class ScoreRef {
    public enum Type {
        STAGE,
        FILM_LOOP
    }

    private final Type type;
    private final CastMemberRef memberRef;

    private ScoreRef(Type type, CastMemberRef memberRef) {
        this.type = type;
        this.memberRef = memberRef;
    }

    public static ScoreRef stage() {
        return new ScoreRef(Type.STAGE, null);
    }

    public static ScoreRef filmLoop(CastMemberRef memberRef) {
        return new ScoreRef(Type.FILM_LOOP, memberRef);
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
    public String toString() {
        if (type == Type.STAGE) {
            return "ScoreRef.Stage";
        } else {
            return "ScoreRef.FilmLoop(" + memberRef + ")";
        }
    }
}
