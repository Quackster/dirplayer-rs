package com.dirplayer.director;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about a shape cast member.
 * Port of Rust ShapeInfo struct.
 */
public class ShapeInfo {
    private static final Logger logger = LoggerFactory.getLogger(ShapeInfo.class);

    public ShapeType shapeType;
    public short regPointX;
    public short regPointY;
    public int width;
    public int height;
    public int color;

    public ShapeInfo() {
        this.shapeType = ShapeType.Unknown;
        this.regPointX = 0;
        this.regPointY = 0;
        this.width = 0;
        this.height = 0;
        this.color = 0;
    }

    public static ShapeInfo from(byte[] bytes) {
        return fromBytes(bytes);
    }

    public static ShapeInfo fromBytes(byte[] bytes) {
        ShapeInfo info = new ShapeInfo();
        BinaryReader reader = new BinaryReader(bytes);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        int shapeTypeRaw = 0;
        int regY = 0;
        int regX = 0;

        if (reader.bytesLeft() >= 2) {
            shapeTypeRaw = reader.readU16();
        }
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
        reader.skip(2);
        if (reader.bytesLeft() >= 1) {
            info.color = reader.readU8();
        }
        reader.skip(4);

        info.regPointX = (short) regX;
        info.regPointY = (short) regY;

        switch (shapeTypeRaw) {
            case 0x0001:
                info.shapeType = ShapeType.Rect;
                break;
            case 0x0002:
                info.shapeType = ShapeType.OvalRect;
                break;
            case 0x0003:
                info.shapeType = ShapeType.Oval;
                break;
            case 0x0008:
                info.shapeType = ShapeType.Line;
                break;
            default:
                logger.warn("Unknown shape type: 0x{}", Integer.toHexString(shapeTypeRaw));
                info.shapeType = ShapeType.Unknown;
                break;
        }

        return info;
    }
}
