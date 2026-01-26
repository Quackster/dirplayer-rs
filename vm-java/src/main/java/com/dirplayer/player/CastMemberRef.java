package com.dirplayer.player;

import java.util.Objects;

/**
 * Reference to a cast member in a cast library.
 * Port of Rust CastMemberRef struct.
 */
public class CastMemberRef {
    public int castLib;
    public int castMember;

    public CastMemberRef() {
        this.castLib = 0;
        this.castMember = 0;
    }

    public CastMemberRef(int castLib, int castMember) {
        this.castLib = castLib;
        this.castMember = castMember;
    }

    public static CastMemberRef of(int castLib, int castMember) {
        return new CastMemberRef(castLib, castMember);
    }

    public CastMemberRef copy() {
        return new CastMemberRef(castLib, castMember);
    }

    public boolean isValid() {
        return castMember > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastMemberRef that = (CastMemberRef) o;
        return castLib == that.castLib && castMember == that.castMember;
    }

    @Override
    public int hashCode() {
        return Objects.hash(castLib, castMember);
    }

    @Override
    public String toString() {
        return "member(" + castMember + ", " + castLib + ")";
    }
}
