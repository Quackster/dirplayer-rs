package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import com.dirplayer.io.ListReaders;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Cast list chunk - contains list of cast entries.
 * Port of Rust CastListChunk struct.
 */
public class CastListChunk {
    public List<CastListEntry> entries;

    public CastListChunk() {
        this.entries = new ArrayList<>();
    }

    public static CastListChunk fromReader(BinaryReader reader, int dirVersion, ByteOrder itemEndian) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        CastListChunk chunk = new CastListChunk();
        CastListChunkHeader header = CastListChunkHeader.read(reader, dirVersion);
        List<Integer> offsetTable = BasicListChunk.readOffsetTable(reader, dirVersion, header.dataOffset);
        List<byte[]> itemBufs = BasicListChunk.readItems(reader, dirVersion, header.dataOffset, offsetTable);

        for (int i = 0; i < header.castCount; i++) {
            CastListEntry entry = new CastListEntry();

            if (header.itemsPerCast >= 1) {
                entry.name = ListReaders.readPascalString(itemBufs, i * header.itemsPerCast + 1, itemEndian);
            }
            if (header.itemsPerCast >= 2) {
                entry.filePath = ListReaders.readPascalString(itemBufs, i * header.itemsPerCast + 2, itemEndian);
            }
            if (header.itemsPerCast >= 3) {
                entry.preloadSettings = ListReaders.readU16(itemBufs, i * header.itemsPerCast + 3, ByteOrder.BIG_ENDIAN);
            }
            if (header.itemsPerCast >= 4) {
                int idx = i * header.itemsPerCast + 4;
                if (idx < itemBufs.size() && itemBufs.get(idx).length >= 8) {
                    BinaryReader itemReader = new BinaryReader(itemBufs.get(idx));
                    itemReader.setEndian(reader.getEndian());
                    entry.minMember = itemReader.readU16();
                    entry.maxMember = itemReader.readU16();
                    entry.id = itemReader.readU32();
                }
            }

            chunk.entries.add(entry);
        }

        return chunk;
    }

    private static class CastListChunkHeader {
        int dataOffset;
        int unk0;
        int castCount;
        int itemsPerCast;
        int unk1;

        static CastListChunkHeader read(BinaryReader reader, int dirVersion) {
            CastListChunkHeader header = new CastListChunkHeader();
            header.dataOffset = reader.readU32();
            header.unk0 = reader.readU16();
            header.castCount = reader.readU16();
            header.itemsPerCast = reader.readU16();
            header.unk1 = reader.readU16();
            return header;
        }
    }

    public static class CastListEntry {
        public String name = "";
        public String filePath = "";
        public int preloadSettings = 0;
        public int minMember = 0;
        public int maxMember = 0;
        public int id = 0;
    }
}
