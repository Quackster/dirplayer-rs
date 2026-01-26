package com.dirplayer.director.chunks;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.io.BinaryReader;

/**
 * Literal store for reading script literals.
 * Port of Rust LiteralStore struct.
 */
public class LiteralStore {

    public static class LiteralStoreRecord {
        public LiteralType literalType;
        public int offset;

        public LiteralStoreRecord(LiteralType literalType, int offset) {
            this.literalType = literalType;
            this.offset = offset;
        }
    }

    public LiteralStoreRecord record;
    public Datum data;

    public LiteralStore(LiteralStoreRecord record, Datum data) {
        this.record = record;
        this.data = data;
    }

    public static LiteralStoreRecord readRecord(BinaryReader reader, int dirVersion) {
        int literalTypeId;
        if (dirVersion >= 500) {
            literalTypeId = (int) reader.readU32();
        } else {
            literalTypeId = reader.readU16();
        }

        LiteralType literalType = LiteralType.from(literalTypeId);
        int offset = (int) reader.readU32();

        return new LiteralStoreRecord(literalType, offset);
    }

    public static Datum readData(BinaryReader reader, LiteralStoreRecord record, int startOffset) {
        switch (record.literalType) {
            case Int:
                return Datum.ofInt(record.offset);

            case String:
                reader.setPos(startOffset + record.offset);
                int length = (int) reader.readU32();
                String str = reader.readString(length - 1);
                return Datum.ofString(str);

            case Float:
                reader.setPos(startOffset + record.offset);
                int floatLength = (int) reader.readU32();
                double floatVal;

                if (floatLength == 8) {
                    // Length 8 means f64 (double precision)
                    floatVal = reader.readF64();
                } else if (floatLength == 10) {
                    // Apple 80-bit extended precision
                    floatVal = reader.readAppleFloat80();
                } else {
                    floatVal = 0.0;
                }
                return Datum.ofFloat(floatVal);

            default:
                return Datum.VOID;
        }
    }

    public static LiteralStore fromReader(BinaryReader reader, int dirVersion, int startOffset) {
        LiteralStoreRecord record = readRecord(reader, dirVersion);
        Datum data = readData(reader, record, startOffset);
        return new LiteralStore(record, data);
    }
}
