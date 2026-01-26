package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;

/**
 * Information about a field cast member.
 * Port of Rust FieldInfo struct.
 */
public class FieldInfo {
    public int border;              // Byte 0: 0-5
    public int margin;              // Byte 1: 0-5
    public int boxDropShadow;       // Byte 2: 0-5
    public int boxType;             // Byte 3: 0=adjust, 1=scroll, 2=fixed, 3=limit

    public int alignmentHigh;       // Byte 4
    public int alignmentLow;        // Byte 5

    public int bgColorR;            // Byte 6
    public int bgColorG;            // Byte 7
    public int bgColorRDup;         // Byte 8
    public int bgColorGDup;         // Byte 9
    public int fgColorR;            // Byte 10
    public int fgColorG;            // Byte 11

    public int reserved12;          // Byte 12
    public int scrollTop;           // Byte 13

    public byte[] reserved14_18 = new byte[5];  // Bytes 14-18

    public int reserved19;          // Byte 19
    public int reserved20;          // Byte 20
    public int width;               // Byte 21
    public int reserved22;          // Byte 22

    public int height;              // Byte 23
    public int fontType;            // Byte 24

    public int dropShadow;          // Byte 25: 0-5
    public int flags;               // Byte 26: editable|autoTab|wordwrap bits

    public byte[] reserved27_28 = new byte[2];  // Bytes 27-28

    public FieldInfo() {
        // Default initialization
    }

    public static FieldInfo fromBytes(byte[] bytes) {
        FieldInfo info = new FieldInfo();
        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        if (reader.bytesLeft() >= 1) info.border = reader.readU8();
        if (reader.bytesLeft() >= 1) info.margin = reader.readU8();
        if (reader.bytesLeft() >= 1) info.boxDropShadow = reader.readU8();
        if (reader.bytesLeft() >= 1) info.boxType = reader.readU8();

        if (reader.bytesLeft() >= 1) info.alignmentHigh = reader.readU8();
        if (reader.bytesLeft() >= 1) info.alignmentLow = reader.readU8();

        if (reader.bytesLeft() >= 1) info.bgColorR = reader.readU8();
        if (reader.bytesLeft() >= 1) info.bgColorG = reader.readU8();
        if (reader.bytesLeft() >= 1) info.bgColorRDup = reader.readU8();
        if (reader.bytesLeft() >= 1) info.bgColorGDup = reader.readU8();
        if (reader.bytesLeft() >= 1) info.fgColorR = reader.readU8();
        if (reader.bytesLeft() >= 1) info.fgColorG = reader.readU8();

        if (reader.bytesLeft() >= 1) info.reserved12 = reader.readU8();
        if (reader.bytesLeft() >= 1) info.scrollTop = reader.readU8();

        for (int i = 0; i < 5 && reader.bytesLeft() >= 1; i++) {
            info.reserved14_18[i] = (byte) reader.readU8();
        }

        if (reader.bytesLeft() >= 1) info.reserved19 = reader.readU8();
        if (reader.bytesLeft() >= 1) info.reserved20 = reader.readU8();
        if (reader.bytesLeft() >= 1) info.width = reader.readU8();
        if (reader.bytesLeft() >= 1) info.reserved22 = reader.readU8();

        if (reader.bytesLeft() >= 1) info.height = reader.readU8();
        if (reader.bytesLeft() >= 1) info.fontType = reader.readU8();

        if (reader.bytesLeft() >= 1) info.dropShadow = reader.readU8();
        if (reader.bytesLeft() >= 1) info.flags = reader.readU8();

        for (int i = 0; i < 2 && reader.bytesLeft() >= 1; i++) {
            info.reserved27_28[i] = (byte) reader.readU8();
        }

        return info;
    }

    public boolean isEditable() {
        return (flags & 0x01) != 0;
    }

    public boolean isAutoTab() {
        return (flags & 0x02) != 0;
    }

    public boolean isWordwrap() {
        return (flags & 0x04) == 0;  // Inverted: 0=true, 1=false
    }

    public String getAlignmentStr() {
        if (alignmentHigh == 0x00 && alignmentLow == 0x00) {
            return "left";
        } else if (alignmentHigh == 0x00 && alignmentLow == 0x01) {
            return "center";
        } else if (alignmentHigh == 0xFF && alignmentLow == 0xFF) {
            return "right";
        }
        return "left";
    }

    public String getBoxTypeStr() {
        switch (boxType) {
            case 0: return "adjust";
            case 1: return "scroll";
            case 2: return "fixed";
            case 3: return "limit";
            default: return "adjust";
        }
    }

    public String getFontName() {
        switch (fontType) {
            case 0x10: return "Arial";
            case 0x0E: return "Courier";
            case 0x11: return "Times New Roman";
            case 0x0F: return "Calibri";
            default: return "Arial";
        }
    }

    public int getBgColor() {
        return (bgColorR << 8) | bgColorG;
    }

    public int getFgColor() {
        return (fgColorR << 8) | fgColorG;
    }
}
