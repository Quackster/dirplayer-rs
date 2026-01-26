package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thumbnail chunk (Thum) - contains thumbnail/preview data.
 * Port of Rust ThumChunk struct.
 */
public class ThumChunk {
    private static final Logger logger = LoggerFactory.getLogger(ThumChunk.class);

    public byte[] rawData;

    public ThumChunk() {
        this.rawData = new byte[0];
    }

    public static ThumChunk fromReader(BinaryReader reader) {
        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        ThumChunk chunk = new ThumChunk();
        chunk.rawData = reader.readBytes(reader.getLength() - reader.getPos());

        reader.setEndian(originalEndian);

        if (logger.isDebugEnabled()) {
            StringBuilder hexDump = new StringBuilder();
            for (byte b : chunk.rawData) {
                hexDump.append(String.format("%02X ", b & 0xFF));
            }
            logger.debug("Thum raw_data ({} bytes): {}", chunk.rawData.length, hexDump);
        }

        return chunk;
    }
}
