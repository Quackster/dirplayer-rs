package com.dirplayer.director.lingo;

/**
 * Types of string chunks in Lingo.
 * Port of Rust StringChunkType enum.
 */
public enum StringChunkType {
    ITEM(0x01, "item"),
    WORD(0x02, "word"),
    CHAR(0x03, "char"),
    LINE(0x04, "line");

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

    public static StringChunkType fromId(int id) {
        // Map property IDs to chunk types
        switch (id) {
            case 0x01:
                return CHAR;
            case 0x02:
                return WORD;
            case 0x03:
                return ITEM;
            case 0x04:
                return LINE;
            default:
                return fromValue(id);
        }
    }

    public static StringChunkType fromName(String name) {
        for (StringChunkType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid string chunk type: " + name);
    }

    /**
     * Parse chunk type from string (case-insensitive, handles plurals).
     */
    public static StringChunkType fromString(String str) {
        String lower = str.toLowerCase();
        switch (lower) {
            case "char":
            case "chars":
                return CHAR;
            case "word":
            case "words":
                return WORD;
            case "item":
            case "items":
                return ITEM;
            case "line":
            case "lines":
                return LINE;
            default:
                throw new IllegalArgumentException("Invalid string chunk type: " + str);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
