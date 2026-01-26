package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cast info chunk - contains raw cast information data.
 * Port of Rust CastInfoChunk struct.
 */
public class CastInfoChunk {
    private static final Logger logger = LoggerFactory.getLogger(CastInfoChunk.class);

    public byte[] rawData;

    public CastInfoChunk() {
        this.rawData = new byte[0];
    }

    public static CastInfoChunk fromReader(BinaryReader reader) {
        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        CastInfoChunk chunk = new CastInfoChunk();

        // Read all remaining bytes
        int remaining = reader.getLength() - reader.getPos();
        if (remaining > 0) {
            chunk.rawData = reader.readBytes(remaining);
        }

        reader.setEndian(originalEndian);

        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (byte b : chunk.rawData) {
                sb.append(String.format("%02X ", b & 0xFF));
            }
            logger.debug("Cinf raw_data ({} bytes): {}", chunk.rawData.length, sb.toString());
        }

        return chunk;
    }
}
