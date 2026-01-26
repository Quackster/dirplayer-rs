package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;

/**
 * Information about a bitmap cast member.
 * Port of Rust BitmapInfo struct.
 */
public class BitmapInfo {
    public int width;
    public int height;
    public short regX;
    public short regY;
    public int bitDepth;
    public short paletteId;
    public boolean useAlpha;
    public boolean trimWhiteSpace;
    public boolean centerRegPoint;

    public BitmapInfo() {
        this.width = 0;
        this.height = 0;
        this.regX = 0;
        this.regY = 0;
        this.bitDepth = 1;
        this.paletteId = 0;
        this.useAlpha = false;
        this.trimWhiteSpace = false;
        this.centerRegPoint = false;
    }

    public static BitmapInfo fromBytes(byte[] bytes) {
        BitmapInfo info = new BitmapInfo();
        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        reader.readU8(); // Skip first byte
        reader.readU8(); // Logo -> 16
        reader.readI32(); // Skip u32

        if (reader.bytesLeft() >= 2) {
            info.height = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            info.width = reader.readU16();
        }

        reader.skip(8); // Skip 4 u16 values

        if (reader.bytesLeft() >= 2) {
            info.regY = reader.readI16();
        }
        if (reader.bytesLeft() >= 2) {
            info.regX = reader.readI16();
        }

        // Read flags byte
        if (reader.bytesLeft() >= 1) {
            int flags = reader.readU8();
            info.centerRegPoint = (flags & 0x20) != 0;  // Bit 5
            info.useAlpha = (flags & 0x10) != 0;        // Bit 4
            info.trimWhiteSpace = (flags & 0x80) == 0;  // Bit 7 (inverted)
        }

        if (!reader.eof()) {
            if (reader.bytesLeft() >= 1) {
                info.bitDepth = reader.readU8();
            }
            reader.skip(2); // palette?
            if (reader.bytesLeft() >= 2) {
                info.paletteId = (short)(reader.readI16() - 1);
            }
        }

        // If centerRegPoint is enabled, calculate the centered registration point
        if (info.centerRegPoint && info.width > 0 && info.height > 0) {
            info.regX = (short)(info.width / 2);
            info.regY = (short)(info.height / 2);
        }

        return info;
    }
}
