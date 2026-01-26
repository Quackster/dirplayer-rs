package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import com.dirplayer.SimpleLogger;


/**
 * Media chunk - contains audio media data with header info.
 * Port of Rust MediaChunk struct.
 */
public class MediaChunk {
    private static final SimpleLogger logger = SimpleLogger.getLogger(MediaChunk.class);

    public int sampleRate;
    public int dataSizeField;
    public byte[] guid;
    public byte[] audioData;
    public boolean isCompressed;

    public MediaChunk() {
        this.sampleRate = 0;
        this.dataSizeField = 0;
        this.guid = null;
        this.audioData = new byte[0];
        this.isCompressed = false;
    }

    public static MediaChunk fromReader(BinaryReader reader) {
        // Read all data for debug
        int rBegin = reader.getPos();
        byte[] dataTest = reader.readBytes(reader.getLength() - reader.getPos());
        reader.setPos(rBegin);

        if (logger.isDebugEnabled()) {
            StringBuilder hexDump = new StringBuilder();
            for (byte b : dataTest) {
                hexDump.append(String.format("%02X ", b & 0xFF));
            }
            logger.debug("WAV Hex Dump (Full File, {} bytes):\n{}", dataTest.length, hexDump);
        }

        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        MediaChunk chunk = new MediaChunk();

        int headerSize = (int) reader.readU32();
        int unknown1 = (int) reader.readU32();
        chunk.sampleRate = (int) reader.readU32();
        int sampleRate2 = (int) reader.readU32();
        int unknown2 = (int) reader.readU32();
        chunk.dataSizeField = (int) reader.readU32();

        int bytesRead = 24;
        int skipBytes = headerSize - bytesRead;

        // Read GUID if present
        if (skipBytes >= 16) {
            chunk.guid = reader.readBytes(16);
            skipBytes -= 16;
        }

        // Skip remaining header padding
        if (skipBytes > 0) {
            reader.readBytes(skipBytes);
        }

        // Read all remaining data as audio data
        chunk.audioData = reader.readBytes(reader.getLength() - reader.getPos());

        // Detect compression type
        boolean isMp3 = chunk.audioData.length >= 2
            && (chunk.audioData[0] & 0xFF) == 0xFF
            && ((chunk.audioData[1] & 0xE0) == 0xE0);

        float compressionRatio = chunk.audioData.length > 0
            ? (float) chunk.dataSizeField / chunk.audioData.length
            : 1.0f;

        boolean isImaAdpcm = compressionRatio > 2.0f && !isMp3;
        chunk.isCompressed = isMp3 || isImaAdpcm;

        logger.info("MediaChunk: {} bytes (expected {}), ratio={:.2}, mp3={}, ima_adpcm={}, rate={}",
            chunk.audioData.length, chunk.dataSizeField, compressionRatio, isMp3, isImaAdpcm, chunk.sampleRate);

        reader.setEndian(originalEndian);

        // Store the full data for later use
        chunk.audioData = dataTest;

        return chunk;
    }

    public String getCodecName() {
        // Check GUID for IMA ADPCM
        if (guid != null && guid.length >= 8) {
            if (guid[0] == 0x5A && guid[1] == 0x08 && guid[2] == (byte) 0xCD && guid[3] == 0x40
                && guid[4] == 0x53 && guid[5] == 0x5B && guid[6] == 0x11 && guid[7] == (byte) 0xD0) {
                return "ima_adpcm";
            }
        }

        // Check for MP3
        if (audioData.length >= 2
            && (audioData[0] & 0xFF) == 0xFF
            && ((audioData[1] & 0xE0) == 0xE0)) {
            return "mp3";
        }

        // Check for IMA ADPCM by compression ratio
        float compressionRatio = audioData.length > 0
            ? (float) dataSizeField / audioData.length
            : 1.0f;

        if (compressionRatio > 2.0f) {
            return "ima_adpcm";
        }

        return "raw_pcm";
    }

    public boolean isSound() {
        return isCompressed || audioData.length > 0;
    }
}
