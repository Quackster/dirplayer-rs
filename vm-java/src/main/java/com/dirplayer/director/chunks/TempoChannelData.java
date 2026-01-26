package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;

/**
 * Tempo channel data - frame rate and wait information.
 * Port of Rust TempoChannelData struct.
 */
public class TempoChannelData {
    public int tempo;
    public int flags1;
    public int flags2;
    public int unk3;
    public int unk4;
    public int waitFlags;
    public int channelFlags;
    public int frameData;

    public TempoChannelData() {
    }

    public static TempoChannelData read(BinaryReader reader) {
        TempoChannelData data = new TempoChannelData();

        data.flags1 = reader.readU8();
        data.flags2 = reader.readU8();
        data.unk3 = reader.readU8();
        data.unk4 = reader.readU8();

        // Byte 4 - tempo in FPS
        data.tempo = reader.readU8();

        // Skip bytes 5-7
        reader.readU8();
        reader.readU8();
        reader.readU8();

        // Bytes 8-9 - wait flags
        data.waitFlags = reader.readU16();

        // Bytes 10-11 - channel flags
        data.channelFlags = reader.readU16();

        // Skip bytes 12-17
        for (int i = 0; i < 6; i++) {
            reader.readU8();
        }

        // Bytes 18-19 - frame data
        data.frameData = reader.readU16();

        return data;
    }

    public boolean isDefault() {
        return flags1 == 0xff && flags2 == 0xfe;
    }

    public boolean isEmpty() {
        return flags1 == 0 && flags2 == 0 && tempo == 0;
    }
}
