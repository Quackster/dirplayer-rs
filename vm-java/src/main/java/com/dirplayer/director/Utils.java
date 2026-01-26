package com.dirplayer.director;

/**
 * Utility functions for Director file parsing.
 * Port of Rust director/utils.rs
 */
public class Utils {

    /**
     * Convert a 4-character string to a FOURCC integer.
     */
    public static int FOURCC(String str) {
        if (str.length() != 4) {
            throw new IllegalArgumentException("FOURCC string must be exactly 4 characters");
        }
        byte[] bytes = str.getBytes();
        return ((bytes[0] & 0xFF) << 24)
            | ((bytes[1] & 0xFF) << 16)
            | ((bytes[2] & 0xFF) << 8)
            | (bytes[3] & 0xFF);
    }

    /**
     * Convert a FOURCC integer to a string.
     */
    public static String fourccToString(int fourcc) {
        char[] chars = new char[4];
        chars[0] = (char) ((fourcc >> 24) & 0xFF);
        chars[1] = (char) ((fourcc >> 16) & 0xFF);
        chars[2] = (char) ((fourcc >> 8) & 0xFF);
        chars[3] = (char) (fourcc & 0xFF);
        return new String(chars);
    }

    /**
     * Convert raw Director version to human-readable version number.
     */
    public static int humanVersion(int rawVersion) {
        if (rawVersion >= 0x79F) {
            return 1201;  // Director 12.0.1
        } else if (rawVersion >= 0x783) {
            return 1200;  // Director 12
        } else if (rawVersion >= 0x782) {
            return 1150;  // Director 11.5
        } else if (rawVersion >= 0x781) {
            return 1100;  // Director 11
        } else if (rawVersion >= 0x73B) {
            return 1000;  // Director 10
        } else if (rawVersion >= 0x6A4) {
            return 850;   // Director 8.5
        } else if (rawVersion >= 0x582) {
            return 800;   // Director 8
        } else if (rawVersion >= 0x4C8) {
            return 700;   // Director 7
        } else if (rawVersion >= 0x4C2) {
            return 600;   // Director 6
        } else if (rawVersion >= 0x4B1) {
            return 500;   // Director 5
        } else if (rawVersion >= 0x45B) {
            return 404;   // Director 4.04
        } else if (rawVersion >= 0x45A) {
            return 400;   // Director 4
        } else if (rawVersion >= 0x405) {
            return 310;   // Director 3.1
        } else if (rawVersion >= 0x404) {
            return 300;   // Director 3
        } else {
            return 200;   // Director 2 or earlier
        }
    }

    /**
     * Check if a FOURCC matches a given 4-character string.
     */
    public static boolean fourccEquals(int fourcc, String str) {
        return fourcc == FOURCC(str);
    }

    /**
     * Convert bytes to an integer in big-endian order.
     */
    public static int bytesToIntBE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
            | ((bytes[offset + 1] & 0xFF) << 16)
            | ((bytes[offset + 2] & 0xFF) << 8)
            | (bytes[offset + 3] & 0xFF);
    }

    /**
     * Convert bytes to an integer in little-endian order.
     */
    public static int bytesToIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
            | ((bytes[offset + 1] & 0xFF) << 8)
            | ((bytes[offset + 2] & 0xFF) << 16)
            | ((bytes[offset + 3] & 0xFF) << 24);
    }

    /**
     * Convert bytes to a short in big-endian order.
     */
    public static short bytesToShortBE(byte[] bytes, int offset) {
        return (short) (((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF));
    }

    /**
     * Convert bytes to a short in little-endian order.
     */
    public static short bytesToShortLE(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
    }
}
