package com.dirplayer.director;

/**
 * Information about a sound cast member.
 * Port of Rust SoundInfo struct.
 */
public class SoundInfo {
    public long sampleRate;
    public int sampleSize;
    public int channels;
    public long sampleCount;
    public long duration;
    public boolean loopEnabled;

    public SoundInfo() {
        this.sampleRate = 0;
        this.sampleSize = 0;
        this.channels = 0;
        this.sampleCount = 0;
        this.duration = 0;
        this.loopEnabled = false;
    }
}
