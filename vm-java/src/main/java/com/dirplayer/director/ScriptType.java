package com.dirplayer.director;

/**
 * Enumeration of script types in Director files.
 * Port of Rust ScriptType enum.
 */
public enum ScriptType {
    Invalid(0),
    Score(1),
    Member(2),
    Movie(3),
    Parent(7),
    Unknown(255);

    private final int value;

    ScriptType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ScriptType from(int val) {
        for (ScriptType type : values()) {
            if (type.value == val) {
                return type;
            }
        }
        return Unknown;
    }

    public static ScriptType fromValue(int val) {
        return from(val);
    }
}
