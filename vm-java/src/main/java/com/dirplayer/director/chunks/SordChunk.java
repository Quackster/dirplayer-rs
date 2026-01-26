package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import com.dirplayer.SimpleLogger;


/**
 * Score order chunk (Sord) - contains channel ordering information.
 * Port of Rust SordChunk struct.
 */
public class SordChunk {
    private static final SimpleLogger logger = SimpleLogger.getLogger(SordChunk.class);

    public byte[] rawData;

    public SordChunk() {
        this.rawData = new byte[0];
    }

    public static SordChunk fromReader(BinaryReader reader) {
        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        SordChunk chunk = new SordChunk();
        chunk.rawData = reader.readBytes(reader.getLength() - reader.getPos());

        reader.setEndian(originalEndian);

        logger.debug("Read {} bytes for Sord chunk", chunk.rawData.length);

        if (chunk.rawData.length < 20) {
            logger.warn("Sord chunk too small to contain header");
            return chunk;
        }

        // Parse header for debugging
        byte[] header = chunk.rawData;
        int channels = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
        int bitsPerSample = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
        int sampleRate = ((header[4] & 0xFF) << 24) | ((header[5] & 0xFF) << 16)
            | ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
        int dataOffset = ((header[8] & 0xFF) << 24) | ((header[9] & 0xFF) << 16)
            | ((header[10] & 0xFF) << 8) | (header[11] & 0xFF);
        int dataLength = ((header[12] & 0xFF) << 24) | ((header[13] & 0xFF) << 16)
            | ((header[14] & 0xFF) << 8) | (header[15] & 0xFF);
        int codec = ((header[16] & 0xFF) << 8) | (header[17] & 0xFF);
        int flags = ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);

        logger.debug("Parsed Sord header: channels={} bits={} sample_rate={} offset={} length={} codec={} flags={}",
            channels, bitsPerSample, sampleRate, dataOffset, dataLength, codec, flags);

        return chunk;
    }
}
