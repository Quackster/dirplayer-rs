package com.dirplayer.player.cast;

import com.dirplayer.director.SoundInfo;
import com.dirplayer.director.chunks.SoundChunk;

/**
 * Sound cast member data.
 * Port of Rust SoundMember struct.
 */
public class SoundMember {
    public SoundInfo info;
    public SoundChunk sound;

    public SoundMember() {
        this.info = new SoundInfo();
        this.sound = null;
    }

    public SoundInfo getInfo() {
        return info;
    }

    public SoundChunk getSound() {
        return sound;
    }

    public long getSampleRate() {
        return info != null ? info.sampleRate : 0;
    }

    public int getSampleSize() {
        return info != null ? info.sampleSize : 0;
    }

    public int getChannelCount() {
        return info != null ? info.channels : 0;
    }

    public long getSampleCount() {
        return info != null ? info.sampleCount : 0;
    }

    /**
     * Get duration in milliseconds.
     */
    public long getDurationMs() {
        if (info == null || info.sampleRate == 0) return 0;
        return (info.sampleCount * 1000L) / info.sampleRate;
    }

    public SoundMember copy() {
        SoundMember copy = new SoundMember();
        copy.info = info;
        copy.sound = sound;
        return copy;
    }
}
