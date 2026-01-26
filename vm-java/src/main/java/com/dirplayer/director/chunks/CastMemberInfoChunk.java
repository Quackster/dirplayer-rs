package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import com.dirplayer.io.ListReaders;
import java.util.List;

/**
 * Cast member info chunk - contains script text and name.
 * Port of Rust CastMemberInfoChunk struct.
 */
public class CastMemberInfoChunk {
    public CastMemberInfoChunkHeader header;
    public String scriptSrcText;
    public String name;

    public CastMemberInfoChunk() {
        this.header = new CastMemberInfoChunkHeader();
        this.scriptSrcText = "";
        this.name = "";
    }

    public static CastMemberInfoChunk read(BinaryReader reader, int dirVersion) {
        CastMemberInfoChunk chunk = new CastMemberInfoChunk();
        chunk.header = CastMemberInfoChunkHeader.read(reader, dirVersion);

        List<Integer> offsetTable = BasicListChunk.readOffsetTable(reader, dirVersion, chunk.header.dataOffset);
        List<byte[]> itemBufs = BasicListChunk.readItems(reader, dirVersion, chunk.header.dataOffset, offsetTable);

        chunk.scriptSrcText = ListReaders.readString(itemBufs, 0);
        chunk.name = ListReaders.readPascalString(itemBufs, 1, reader.getEndian());

        return chunk;
    }

    public static class CastMemberInfoChunkHeader {
        public int dataOffset;
        public int unk1;
        public int unk2;
        public int flags;
        public int scriptId;

        public CastMemberInfoChunkHeader() {
            this.dataOffset = 0;
            this.unk1 = 0;
            this.unk2 = 0;
            this.flags = 0;
            this.scriptId = 0;
        }

        public static CastMemberInfoChunkHeader read(BinaryReader reader, int dirVersion) {
            CastMemberInfoChunkHeader header = new CastMemberInfoChunkHeader();
            header.dataOffset = reader.readU32();
            header.unk1 = reader.readU32();
            header.unk2 = reader.readU32();
            header.flags = reader.readU32();
            header.scriptId = reader.readU32();
            return header;
        }
    }
}
