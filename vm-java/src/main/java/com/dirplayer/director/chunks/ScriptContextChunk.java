package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Script context chunk (Lctx) - maps scripts to sections.
 * Port of Rust ScriptContextChunk struct.
 */
public class ScriptContextChunk {
    public int entryCount;
    public int entryCount2;
    public int entriesOffset;
    public int lnamSectionId;
    public int validCount;
    public int flags;
    public int freePointer;
    public List<ScriptContextMapEntry> sectionMap;

    public ScriptContextChunk() {
        this.sectionMap = new ArrayList<>();
    }

    public static ScriptContextChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        ScriptContextChunk chunk = new ScriptContextChunk();

        int unknown0 = (int) reader.readU32();
        int unknown1 = (int) reader.readU32();
        chunk.entryCount = (int) reader.readU32();
        chunk.entryCount2 = (int) reader.readU32();
        chunk.entriesOffset = reader.readU16();
        int unknown2 = reader.readU16();
        int unknown3 = (int) reader.readU32();
        int unknown4 = (int) reader.readU32();
        int unknown5 = (int) reader.readU32();
        chunk.lnamSectionId = (int) reader.readU32();
        chunk.validCount = reader.readU16();
        chunk.flags = reader.readU16();
        chunk.freePointer = reader.readU16();

        reader.setPos(chunk.entriesOffset);
        for (int i = 0; i < chunk.entryCount; i++) {
            chunk.sectionMap.add(ScriptContextMapEntry.fromReader(reader, dirVersion));
        }

        return chunk;
    }

    public static class ScriptContextMapEntry {
        public int unknown0;
        public int sectionId;
        public int unknown1;
        public int unknown2;

        public static ScriptContextMapEntry fromReader(BinaryReader reader, int dirVersion) {
            ScriptContextMapEntry entry = new ScriptContextMapEntry();
            entry.unknown0 = (int) reader.readU32();
            entry.sectionId = reader.readI32();
            entry.unknown1 = reader.readU16();
            entry.unknown2 = reader.readU16();
            return entry;
        }
    }
}
