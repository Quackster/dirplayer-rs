package com.dirplayer.player.score;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a behavior script attached to a sprite span.
 * Port of Rust ScoreBehaviorReference struct.
 */
public class ScoreBehaviorReference {
    public int castLib;
    public int castMember;
    public List<Object> parameter;

    public ScoreBehaviorReference() {
        this.castLib = 0;
        this.castMember = 0;
        this.parameter = new ArrayList<>();
    }

    public ScoreBehaviorReference(int castLib, int castMember) {
        this.castLib = castLib;
        this.castMember = castMember;
        this.parameter = new ArrayList<>();
    }

    public ScoreBehaviorReference(int castLib, int castMember, List<Object> parameter) {
        this.castLib = castLib;
        this.castMember = castMember;
        this.parameter = parameter != null ? new ArrayList<>(parameter) : new ArrayList<>();
    }

    public ScoreBehaviorReference copy() {
        return new ScoreBehaviorReference(castLib, castMember, parameter);
    }

    @Override
    public String toString() {
        return "ScoreBehaviorReference{" +
            "castLib=" + castLib +
            ", castMember=" + castMember +
            ", paramCount=" + parameter.size() +
            '}';
    }
}
