package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;

/**
 * Bitmap chunk - stores raw bitmap data.
 * Port of Rust BitmapChunk struct.
 */
public class BitmapChunk {
    public byte[] data;
    public int version;

    public BitmapChunk() {
        this.data = new byte[0];
        this.version = 0;
    }

    public static BitmapChunk read(BinaryReader reader, int dirVersion) {
        BitmapChunk chunk = new BitmapChunk();
        chunk.data = reader.getData().clone();
        chunk.version = dirVersion;
        return chunk;
    }
}
