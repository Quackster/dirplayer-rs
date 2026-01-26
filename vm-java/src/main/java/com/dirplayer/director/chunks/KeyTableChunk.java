package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Key table chunk - maps section IDs to cast IDs.
 * Port of Rust KeyTableChunk struct.
 */
public class KeyTableChunk {
    public int entrySize;
    public int entrySize2;
    public int entryCount;
    public int usedCount;
    public List<KeyTableEntry> entries;

    public KeyTableChunk() {
        this.entries = new ArrayList<>();
    }

    public static KeyTableChunk fromReader(BinaryReader reader, int dirVersion) {
        KeyTableChunk chunk = new KeyTableChunk();
        chunk.entrySize = reader.readU16();
        chunk.entrySize2 = reader.readU16();
        chunk.entryCount = reader.readU32();
        chunk.usedCount = reader.readU32();

        for (int i = 0; i < chunk.entryCount; i++) {
            chunk.entries.add(KeyTableEntry.fromReader(reader, dirVersion));
        }

        return chunk;
    }

    public static class KeyTableEntry {
        public int sectionId;
        public int castId;
        public int fourcc;

        public static KeyTableEntry fromReader(BinaryReader reader, int dirVersion) {
            KeyTableEntry entry = new KeyTableEntry();
            entry.sectionId = reader.readU32();
            entry.castId = reader.readU32();
            entry.fourcc = reader.readU32();
            return entry;
        }
    }
}
