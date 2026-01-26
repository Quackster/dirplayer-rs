package com.dirplayer.player.bytecode;

/**
 * Put operation type for string operations.
 * Port of Rust PutType enum.
 */
public enum PutType {
    INTO,
    AFTER,
    BEFORE;

    public static PutType fromValue(int val) {
        switch (val) {
            case 0x01:
                return INTO;
            case 0x02:
                return AFTER;
            case 0x03:
                return BEFORE;
            default:
                throw new IllegalArgumentException("Invalid put type: " + val);
        }
    }
}
