package com.dirplayer.director.chunks;

/**
 * Types of literals in Lingo scripts.
 * Port of Rust LiteralType enum.
 */
public enum LiteralType {
    Invalid(0),
    String(1),
    Int(4),
    Float(9),
    Unknown1(11);

    private final int value;

    LiteralType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LiteralType from(int value) {
        for (LiteralType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return Invalid;
    }
}
