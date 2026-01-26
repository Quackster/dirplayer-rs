package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;

/**
 * Information about a film loop cast member.
 * Port of Rust FilmLoopInfo struct.
 */
public class FilmLoopInfo {
    public short regPointX;
    public short regPointY;
    public int width;
    public int height;
    public int center;
    public int crop;
    public int sound;
    public int loops;  // 'loop' is a reserved keyword in Java

    public FilmLoopInfo() {
        this.regPointX = 0;
        this.regPointY = 0;
        this.width = 0;
        this.height = 0;
        this.center = 0;
        this.crop = 0;
        this.sound = 0;
        this.loops = 0;
    }

    public static FilmLoopInfo fromBytes(byte[] bytes) {
        FilmLoopInfo info = new FilmLoopInfo();
        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        int regY = 0;
        int regX = 0;
        int flags = 0;

        if (reader.bytesLeft() >= 2) {
            regY = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            regX = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            info.height = reader.readU16();
        }
        if (reader.bytesLeft() >= 2) {
            info.width = reader.readU16();
        }
        // Skip 3 bytes (read_u24)
        if (reader.bytesLeft() >= 3) {
            reader.skip(3);
        }
        if (reader.bytesLeft() >= 1) {
            flags = reader.readU8();
        }
        // Skip 2 bytes
        if (reader.bytesLeft() >= 2) {
            reader.skip(2);
        }

        info.regPointX = (short) regX;
        info.regPointY = (short) regY;

        info.center = flags & 0b1;
        info.crop = 1 - ((flags & 0b10) >> 1);
        info.sound = (flags & 0b1000) >> 3;
        info.loops = 1 - ((flags & 0b100000) >> 5);

        return info;
    }
}
