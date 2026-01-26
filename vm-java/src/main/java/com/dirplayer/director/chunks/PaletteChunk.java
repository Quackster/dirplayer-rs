package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Palette chunk - contains color palette data.
 * Port of Rust PaletteChunk struct.
 */
public class PaletteChunk {
    public List<int[]> colors;

    public PaletteChunk() {
        this.colors = new ArrayList<>();
    }

    public static PaletteChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        PaletteChunk chunk = new PaletteChunk();

        for (int i = 0; i < 256; i++) {
            int r = reader.eof() ? 255 : reader.readU8();
            if (!reader.eof()) reader.readU8(); // skip
            int g = reader.eof() ? 0 : reader.readU8();
            if (!reader.eof()) reader.readU8(); // skip
            int b = reader.eof() ? 255 : reader.readU8();
            if (!reader.eof()) reader.readU8(); // skip

            chunk.colors.add(new int[] { r, g, b });
        }

        return chunk;
    }

    public int getColor(int index) {
        if (index >= 0 && index < colors.size()) {
            int[] rgb = colors.get(index);
            return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
        }
        return 0;
    }
}
