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
     * This is based on Lingo's `the fileVersion` with a correction to the
     * version number for Director 12.
     */
    public static int humanVersion(int rawVersion) {
        if (rawVersion >= 1951) {
            return 1200;  // Director 12
        } else if (rawVersion >= 1922) {
            return 1150;  // Director 11.5
        } else if (rawVersion >= 1921) {
            return 1100;  // Director 11
        } else if (rawVersion >= 1851) {
            return 1000;  // Director 10
        } else if (rawVersion >= 1700) {
            return 850;   // Director 8.5
        } else if (rawVersion >= 1410) {
            return 800;   // Director 8
        } else if (rawVersion >= 1224) {
            return 700;   // Director 7
        } else if (rawVersion >= 1218) {
            return 600;   // Director 6
        } else if (rawVersion >= 1201) {
            return 500;   // Director 5
        } else if (rawVersion >= 1117) {
            return 404;   // Director 4.04
        } else if (rawVersion >= 1115) {
            return 400;   // Director 4
        } else if (rawVersion >= 1029) {
            return 310;   // Director 3.1
        } else if (rawVersion >= 1028) {
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
