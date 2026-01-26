package com.dirplayer.player.sound;

/**
 * Sound channel for audio playback.
 */
public class SoundChannel {
    public int channelNum;
    public SoundStatus status;
    public int volume;
    public int pan;
    public int currentTime;
    public int duration;

    public SoundChannel(int channelNum) {
        this.channelNum = channelNum;
        this.status = SoundStatus.Stopped;
        this.volume = 255;
        this.pan = 0;
        this.currentTime = 0;
        this.duration = 0;
    }

    public boolean isBusy() {
        return status == SoundStatus.Playing;
    }

    public void play() {
        status = SoundStatus.Playing;
    }

    public void stop() {
        status = SoundStatus.Stopped;
        currentTime = 0;
    }

    public void pause() {
        status = SoundStatus.Paused;
    }
}
