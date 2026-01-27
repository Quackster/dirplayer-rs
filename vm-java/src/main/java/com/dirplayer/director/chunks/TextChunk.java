package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;

/**
 * Text chunk (Stxt) - contains styled text data.
 * Port of Rust TextChunk struct.
 */
public class TextChunk {
    public int offset;
    public int textLength;
    public int dataLength;
    public String text;
    public byte[] data;

    public TextChunk() {
        this.offset = 0;
        this.textLength = 0;
        this.dataLength = 0;
        this.text = "";
        this.data = new byte[0];
    }

    public static TextChunk read(BinaryReader reader) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        TextChunk chunk = new TextChunk();
        chunk.offset = (int) reader.readU32();

        // Standard offset should be 12 (size of header: offset + textLength + dataLength)
        // If offset is different, the chunk format might be unsupported
        if (chunk.offset != 12) {
            // Return empty chunk for unsupported formats
            return chunk;
        }

        chunk.textLength = (int) reader.readU32();
        chunk.dataLength = (int) reader.readU32();

        // Sanity check: lengths shouldn't be excessively large
        if (chunk.textLength < 0 || chunk.textLength > 10_000_000 ||
            chunk.dataLength < 0 || chunk.dataLength > 10_000_000) {
            return chunk;
        }

        try {
            chunk.text = reader.readString(chunk.textLength);
            chunk.data = reader.readBytes(chunk.dataLength);
        } catch (Exception e) {
            // Handle read errors gracefully
            chunk.text = "";
            chunk.data = new byte[0];
        }

        return chunk;
    }
}
