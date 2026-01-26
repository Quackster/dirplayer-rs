package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;

/**
 * Sound channel data - sound playback information.
 * Port of Rust SoundChannelData struct.
 */
public class SoundChannelData {
    public int castMember;

    public SoundChannelData() {
    }

    public static SoundChannelData read(BinaryReader reader) {
        SoundChannelData data = new SoundChannelData();

        int unk0 = reader.readU8();
        int unk1 = reader.readU8();
        int unk2 = reader.readU8();
        data.castMember = reader.readU8();

        return data;
    }
}
