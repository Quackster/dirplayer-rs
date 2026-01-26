package com.dirplayer.director.lingo;

/**
 * Types of string chunks in Lingo.
 * Port of Rust StringChunkType enum.
 */
public enum StringChunkType {
    Item(0x01, "item"),
    Word(0x02, "word"),
    Char(0x03, "char"),
    Line(0x04, "line");

    private final int value;
    private final String name;

    StringChunkType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static StringChunkType fromValue(int value) {
        for (StringChunkType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid string chunk type: " + value);
    }

    public static StringChunkType fromName(String name) {
        for (StringChunkType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid string chunk type: " + name);
    }

    @Override
    public String toString() {
        return name;
    }
}
