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
        chunk.offset = reader.readU32();

        if (chunk.offset != 12) {
            throw new RuntimeException("Stxt init: unhandled offset");
        }

        chunk.textLength = reader.readU32();
        chunk.dataLength = reader.readU32();
        chunk.text = reader.readString(chunk.textLength);
        chunk.data = reader.readBytes(chunk.dataLength);

        return chunk;
    }
}
