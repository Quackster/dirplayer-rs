package com.dirplayer.director;

/**
 * GUID constants for compression types.
 * Port of Rust director/guid.rs
 */
public class GuidConstants {
    // Zlib compression GUIDs
    public static final byte[] ZLIB_COMPRESSION_GUID = new byte[] {
        (byte) 0x5A, (byte) 0x52, (byte) 0x4C, (byte) 0x5A,  // ZRLZ
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    public static final byte[] ZLIB_COMPRESSION_GUID2 = new byte[] {
        (byte) 0x4A, (byte) 0x97, (byte) 0x4A, (byte) 0xF3,
        (byte) 0xBA, (byte) 0x68, (byte) 0x11, (byte) 0xD0,
        (byte) 0x8B, (byte) 0x22, (byte) 0x00, (byte) 0xA0,
        (byte) 0xC9, (byte) 0x08, (byte) 0x31, (byte) 0x05
    };

    // Sound compression GUID
    public static final byte[] SND_COMPRESSION_GUID = new byte[] {
        (byte) 0x5A, (byte) 0x08, (byte) 0xCD, (byte) 0x40,
        (byte) 0x53, (byte) 0x5B, (byte) 0x11, (byte) 0xD0,
        (byte) 0xA8, (byte) 0xBB, (byte) 0x00, (byte) 0xA0,
        (byte) 0xC9, (byte) 0x00, (byte) 0x8A, (byte) 0x48
    };

    // Font map compression GUID
    public static final byte[] FONTMAP_COMPRESSION_GUID = new byte[] {
        (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x6D,  // defm
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    // Null compression GUID (no compression)
    public static final byte[] NULL_COMPRESSION_GUID = new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    public static boolean isZlibCompression(byte[] guid) {
        return java.util.Arrays.equals(guid, ZLIB_COMPRESSION_GUID)
            || java.util.Arrays.equals(guid, ZLIB_COMPRESSION_GUID2);
    }

    public static boolean isSndCompression(byte[] guid) {
        return java.util.Arrays.equals(guid, SND_COMPRESSION_GUID);
    }

    public static boolean isFontmapCompression(byte[] guid) {
        return java.util.Arrays.equals(guid, FONTMAP_COMPRESSION_GUID);
    }

    public static boolean isNullCompression(byte[] guid) {
        return java.util.Arrays.equals(guid, NULL_COMPRESSION_GUID);
    }

    public static boolean isCompressionImplemented(byte[] guid) {
        return isZlibCompression(guid) || isSndCompression(guid);
    }
}
