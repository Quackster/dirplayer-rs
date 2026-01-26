package com.dirplayer.player.events;

import com.dirplayer.player.CastMemberRef;

import java.util.Objects;

/**
 * Reference to a score context - either the main stage or a filmloop.
 * Port of Rust ScoreRef enum.
 */
public abstract class ScoreRef {

    /**
     * Reference to the main stage score.
     */
    public static class Stage extends ScoreRef {
        public static final Stage INSTANCE = new Stage();

        private Stage() {
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Stage;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "ScoreRef.Stage";
        }

        @Override
        public ScoreRef copy() {
            return INSTANCE;
        }
    }

    /**
     * Reference to a filmloop's score.
     */
    public static class FilmLoop extends ScoreRef {
        public final CastMemberRef memberRef;

        public FilmLoop(CastMemberRef memberRef) {
            this.memberRef = memberRef;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FilmLoop filmLoop = (FilmLoop) o;
            return Objects.equals(memberRef, filmLoop.memberRef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberRef);
        }

        @Override
        public String toString() {
            return "ScoreRef.FilmLoop(" + memberRef + ")";
        }

        @Override
        public ScoreRef copy() {
            return new FilmLoop(memberRef.copy());
        }
    }

    /**
     * Create a stage score reference.
     */
    public static ScoreRef stage() {
        return Stage.INSTANCE;
    }

    /**
     * Create a filmloop score reference.
     */
    public static ScoreRef filmLoop(CastMemberRef memberRef) {
        return new FilmLoop(memberRef);
    }

    /**
     * Check if this is the main stage.
     */
    public boolean isStage() {
        return this instanceof Stage;
    }

    /**
     * Check if this is a filmloop.
     */
    public boolean isFilmLoop() {
        return this instanceof FilmLoop;
    }

    /**
     * Create a copy of this score reference.
     */
    public abstract ScoreRef copy();
}
