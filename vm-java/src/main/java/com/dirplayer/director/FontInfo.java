package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;

/**
 * Information about a font cast member.
 * Port of Rust FontInfo struct.
 */
public class FontInfo {
    public int fontId;     // Internal font resource ID
    public String name;    // Font name (if stored or resolved)
    public int size;       // point size
    public int style;      // style flags (bold/italic/etc)

    public FontInfo() {
        this.fontId = 0;
        this.name = "";
        this.size = 0;
        this.style = 0;
    }

    public static FontInfo fromBytes(byte[] bytes) {
        FontInfo info = new FontInfo();
        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        if (reader.bytesLeft() >= 2) {
            info.fontId = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            info.size = reader.readU16();
        }
        if (reader.bytesLeft() >= 1) {
            info.style = reader.readU8();
        }

        return info;
    }

    /**
     * Parse FontInfo from raw bytes with FourCC prefix.
     */
    public static FontInfo fromRawWithFourCC(byte[] bytes) {
        if (bytes.length < 8) {
            return null;
        }

        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        // Skip length field
        reader.readI32();

        // Read FourCC
        String fourcc = reader.readFourCC();
        if (!"font".equals(fourcc)) {
            return null;
        }

        // Read data length
        reader.readI32();

        // Parse font info fields
        FontInfo info = new FontInfo();
        if (reader.bytesLeft() >= 2) {
            info.fontId = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            info.size = reader.readU16();
        }
        if (reader.bytesLeft() >= 1) {
            info.style = reader.readU8();
        }

        return info;
    }

    /**
     * Check if raw bytes look like valid font data.
     */
    public static boolean looksLikeRealFontData(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }

        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        // Skip the first 4 bytes (length field)
        reader.readI32();

        // Read the FourCC type identifier
        String fourcc = reader.readFourCC();
        return "font".equals(fourcc);
    }

    /**
     * Check if raw bytes indicate text data (not font).
     */
    public static boolean looksLikeTextData(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }

        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        // Skip the first 4 bytes
        reader.readI32();

        // Read the FourCC type identifier
        String fourcc = reader.readFourCC();
        return "text".equals(fourcc);
    }

    public static FontInfo minimal(String name) {
        FontInfo info = new FontInfo();
        info.fontId = 0;
        info.size = 12;
        info.style = 0;
        info.name = name;
        return info;
    }

    public FontInfo withDefaultName(String defaultName) {
        if (this.name == null || this.name.isEmpty()) {
            this.name = defaultName;
        }
        return this;
    }
}
