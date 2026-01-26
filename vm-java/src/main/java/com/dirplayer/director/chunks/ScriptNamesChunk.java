package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Script names chunk (Lnam) - contains script/handler names.
 * Port of Rust ScriptNamesChunk struct.
 */
public class ScriptNamesChunk {
    public List<String> names;

    public ScriptNamesChunk() {
        this.names = new ArrayList<>();
    }

    public static ScriptNamesChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        ScriptNamesChunk chunk = new ScriptNamesChunk();

        int unknown0 = reader.readU32();
        int unknown1 = reader.readU32();
        int len1 = reader.readU32();
        int len2 = reader.readU32();
        int namesOffset = reader.readU16();
        int namesCount = reader.readU16();

        reader.setPos(namesOffset);

        for (int i = 0; i < namesCount; i++) {
            chunk.names.add(reader.readPascalString());
        }

        return chunk;
    }
}
