package com.dirplayer.director.chunks;

import com.dirplayer.director.Utils;
import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config chunk - contains movie configuration.
 * Port of Rust ConfigChunk struct.
 */
public class ConfigChunk {
    private static final Logger logger = LoggerFactory.getLogger(ConfigChunk.class);

    public int len;
    public int fileVersion;
    public int movieTop;
    public int movieLeft;
    public int movieBottom;
    public int movieRight;
    public int minMember;
    public int maxMember;
    public int field9;
    public int field10;

    // Director 6 and below
    public int preD77field11;
    // Director 7 and above
    public int d7StageColorG;
    public int d7StageColorB;

    public int commentFont;
    public int commentSize;
    public int commentStyle;

    // Director 6 and below
    public int preD7StageColor;
    // Director 7 and above
    public int d7StageColorIsRgb;
    public int d7StageColorR;

    public int bitDepth;
    public int field17;
    public int field18;
    public int field19;
    public int directorVersion;
    public int field21;
    public int field22;
    public int field23;
    public int field24;
    public int field25;
    public int field26;
    public int frameRate;
    public int platform;
    public int protection;
    public int field29;
    public int checksum;
    public byte[] remnants;

    public ConfigChunk() {
        this.remnants = new byte[0];
    }

    public static ConfigChunk fromReader(BinaryReader reader, int dirVersion, ByteOrder dirEndian) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);
        reader.setPos(36);

        int rawVersion = reader.readU16();
        int humanVer = Utils.humanVersion(rawVersion);

        reader.setPos(0);

        ConfigChunk config = new ConfigChunk();
        config.len = reader.readU16();
        config.fileVersion = reader.readU16();
        config.movieTop = reader.readU16();
        config.movieLeft = reader.readU16();
        config.movieBottom = reader.readU16();
        config.movieRight = reader.readU16();
        config.minMember = reader.readU16();
        config.maxMember = reader.readU16();
        config.field9 = reader.readU8();
        config.field10 = reader.readU8();

        if (humanVer < 700) {
            config.preD77field11 = reader.readU16();
        } else {
            config.d7StageColorG = reader.readU8();
            config.d7StageColorB = reader.readU8();
        }

        config.commentFont = reader.readU16();
        config.commentSize = reader.readU16();
        config.commentStyle = reader.readU16();

        if (humanVer < 700) {
            config.preD7StageColor = reader.readU16();
        } else {
            config.d7StageColorIsRgb = reader.readU8();
            config.d7StageColorR = reader.readU8();
        }

        config.bitDepth = reader.readU16();
        config.field17 = reader.readU8();
        config.field18 = reader.readU8();
        config.field19 = reader.readU32();
        config.directorVersion = rawVersion;
        reader.readU16(); // skip directorVersion read again
        config.field21 = reader.readU16();
        config.field22 = reader.readU32();
        config.field23 = reader.readU32();
        config.field24 = reader.readU32();
        config.field25 = reader.readU8();
        config.field26 = reader.readU8();
        config.frameRate = reader.readU16();
        config.platform = reader.readU16();
        config.protection = reader.readU16();
        config.field29 = reader.readU32();
        config.checksum = reader.readU32();

        int remainingLen = config.len - reader.getPos();
        if (remainingLen > 0) {
            config.remnants = reader.readBytes(remainingLen);
        }

        int computedChecksum = config.computeChecksum(dirEndian);
        if (config.checksum != computedChecksum) {
            logger.debug("Checksums don't match! Stored: {} Computed: {}", config.checksum, computedChecksum);
        }

        return config;
    }

    public int computeChecksum(ByteOrder dirEndian) {
        int ver = Utils.humanVersion(this.directorVersion);

        long check = this.len + 1;
        check = check * (this.fileVersion + 2);
        check = check / (this.movieTop + 3);
        check = check * (this.movieLeft + 4);
        check = check / (this.movieBottom + 5);
        check = check * (this.movieRight + 6);
        check = check - (this.minMember + 7);
        check = check * (this.maxMember + 8);
        check = check - (this.field9 + 9);
        check = check - (this.field10 + 10);

        long operand11;
        if (ver < 700) {
            operand11 = this.preD77field11;
        } else {
            if (dirEndian == ByteOrder.LITTLE_ENDIAN) {
                operand11 = ((this.d7StageColorB << 8) | this.d7StageColorG) & 0xFFFF;
            } else {
                operand11 = ((this.d7StageColorG << 8) | this.d7StageColorB) & 0xFFFF;
            }
        }

        check = check + (operand11 + 11);
        check = check * (this.commentFont + 12);
        check = check + (this.commentSize + 13);

        long operand14;
        if (ver < 800) {
            operand14 = (this.commentSize >> 8) & 0xFF;
        } else {
            operand14 = this.commentStyle;
        }

        check = check * (operand14 + 14);

        long operand15;
        if (ver < 700) {
            operand15 = this.preD7StageColor;
        } else {
            operand15 = this.d7StageColorR;
        }

        check = check + (operand15 + 15);
        check = check + (this.bitDepth + 16);
        check = check + (this.field17 + 17);
        check = check * (this.field18 + 18);
        check = check + (this.field19 + 19);
        check = check * (this.directorVersion + 20);
        check = check + (this.field21 + 21);
        check = check + (this.field22 + 22);
        check = check + (this.field23 + 23);
        check = check + (this.field24 + 24);
        check = check * (this.field25 + 25);
        check = check + (this.frameRate + 26);
        check = check * (this.platform + 27);
        check = check * (this.protection * 0xE06);
        check = check + 0xFF450000L;
        check ^= Utils.FOURCC("ralf");

        return (int) (check & 0xFFFFFFFFL);
    }
}
