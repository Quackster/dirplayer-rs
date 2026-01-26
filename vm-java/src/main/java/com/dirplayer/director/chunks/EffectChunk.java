package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Effect chunk (FXmp) - contains effect/transition data.
 * Port of Rust EffectChunk struct.
 */
public class EffectChunk {
    private static final Logger logger = LoggerFactory.getLogger(EffectChunk.class);

    public byte[] rawData;

    public EffectChunk() {
        this.rawData = new byte[0];
    }

    public static EffectChunk fromReader(BinaryReader reader) {
        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        EffectChunk chunk = new EffectChunk();
        chunk.rawData = reader.readBytes(reader.getLength() - reader.getPos());

        reader.setEndian(originalEndian);

        if (logger.isDebugEnabled()) {
            StringBuilder hexDump = new StringBuilder();
            for (byte b : chunk.rawData) {
                hexDump.append(String.format("%02X ", b & 0xFF));
            }
            logger.debug("FXmp raw_data ({} bytes): {}", chunk.rawData.length, hexDump);
        }

        return chunk;
    }
}
