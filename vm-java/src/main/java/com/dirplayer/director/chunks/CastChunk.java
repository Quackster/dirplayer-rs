package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Cast chunk - contains list of member IDs.
 * Port of Rust CastChunk struct.
 */
public class CastChunk {
    public List<Integer> memberIds;

    public CastChunk() {
        this.memberIds = new ArrayList<>();
    }

    public static CastChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        CastChunk chunk = new CastChunk();
        while (!reader.eof()) {
            chunk.memberIds.add((int) reader.readU32());
        }
        return chunk;
    }
}
