package com.dirplayer.director;

/**
 * Enumeration of text box types in Director files.
 * Port of Rust BoxType enum.
 */
public enum BoxType {
    Adjust(0),
    Scroll(1),
    Fixed(2),
    Limit(3);

    private final int value;

    BoxType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BoxType from(int val) {
        for (BoxType type : values()) {
            if (type.value == val) {
                return type;
            }
        }
        return Adjust;
    }
}
