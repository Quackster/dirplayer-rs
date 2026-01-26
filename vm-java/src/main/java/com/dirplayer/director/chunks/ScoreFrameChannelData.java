package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;

/**
 * Score frame channel data - per-channel sprite data.
 * Port of Rust ScoreFrameChannelData struct.
 */
public class ScoreFrameChannelData {
    public int spriteType;
    public int ink;
    public int foreColor;
    public int backColor;
    public int castLib;
    public int castMember;
    public int spriteListIdxHi;
    public int spriteListIdxLo;
    public short posY;
    public short posX;
    public int height;
    public int width;
    public int colorFlag;
    public int foreColorG;
    public int backColorG;
    public int foreColorB;
    public int backColorB;
    public int blend;
    public double rotation;
    public double skew;

    public ScoreFrameChannelData() {
    }

    public int getSpriteListIdx() {
        return (spriteListIdxHi << 16) | spriteListIdxLo;
    }

    public boolean hasSpriteData() {
        return castMember != 0
            || rotation != 0.0
            || skew != 0.0
            || blend != 0
            || width != 0
            || height != 0
            || posX != 0
            || posY != 0
            || ink != 0
            || spriteType != 0
            || colorFlag != 0
            || foreColor != 0
            || foreColorG != 0
            || foreColorB != 0
            || backColor != 0
            || backColorG != 0
            || backColorB != 0;
    }

    public static ScoreFrameChannelData read(BinaryReader reader) {
        ScoreFrameChannelData data = new ScoreFrameChannelData();

        data.spriteType = reader.readU8();
        data.ink = reader.readU8();
        data.foreColor = reader.readU8();
        data.backColor = reader.readU8();
        data.castLib = reader.readU16();
        data.castMember = reader.readU16();
        data.spriteListIdxHi = reader.readU16();
        data.spriteListIdxLo = reader.readU16();
        data.posY = (short) reader.readU16();
        data.posX = (short) reader.readU16();
        data.height = reader.readU16();
        data.width = reader.readU16();

        int unk3 = reader.readU8();
        data.colorFlag = (unk3 & 0xF0) >> 4;

        data.blend = reader.readU8();

        int unk5 = reader.readU8();
        int unk6 = reader.readU8();
        data.foreColorG = reader.readU8();
        data.backColorG = reader.readU8();
        data.foreColorB = reader.readU8();
        data.backColorB = reader.readU8();
        int unk7 = reader.readU16();

        // Rotation (fixed-point * 100)
        short rotationRaw = (short) reader.readU16();
        if (rotationRaw != 0) {
            data.rotation = rotationRaw / 100.0;
        }

        int unk8 = reader.readU16();

        // Skew angle (fixed-point * 100)
        short skewRaw = (short) reader.readU16();
        if (skewRaw != 0) {
            data.skew = skewRaw / 100.0;
        }

        return data;
    }
}
