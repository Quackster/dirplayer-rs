package com.dirplayer.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for sound playback.
 * Port of Rust SoundManager struct.
 */
public class SoundManager {
    public SoundChannel[] channels;
    public int channelCount;
    public Map<Integer, AudioData> loadedSounds;
    public boolean soundEnabled;
    public int masterVolume;

    public SoundManager(int channelCount) {
        this.channelCount = channelCount;
        this.channels = new SoundChannel[channelCount];
        for (int i = 0; i < channelCount; i++) {
            channels[i] = new SoundChannel(i + 1);
        }
        this.loadedSounds = new HashMap<>();
        this.soundEnabled = true;
        this.masterVolume = 255;
    }

    public SoundChannel getChannel(int channelNum) {
        if (channelNum >= 1 && channelNum <= channelCount) {
            return channels[channelNum - 1];
        }
        return null;
    }

    public void loadSound(int memberId, AudioData data) {
        loadedSounds.put(memberId, data);
    }

    public AudioData getSound(int memberId) {
        return loadedSounds.get(memberId);
    }

    public void stopAll() {
        for (SoundChannel channel : channels) {
            channel.stop();
        }
    }

    public static class SoundChannel {
        public int number;
        public int volume;
        public int pan;
        public int loopCount;
        public int startTime;
        public int endTime;
        public int loopStartTime;
        public int loopEndTime;
        public boolean isPlaying;
        public boolean isPaused;
        public int currentMemberId;
        public int currentPosition;

        public SoundChannel(int number) {
            this.number = number;
            this.volume = 255;
            this.pan = 0;
            this.loopCount = 1;
            this.startTime = 0;
            this.endTime = 0;
            this.loopStartTime = 0;
            this.loopEndTime = 0;
            this.isPlaying = false;
            this.isPaused = false;
            this.currentMemberId = 0;
            this.currentPosition = 0;
        }

        public void play(int memberId) {
            this.currentMemberId = memberId;
            this.isPlaying = true;
            this.isPaused = false;
            this.currentPosition = startTime;
        }

        public void stop() {
            this.isPlaying = false;
            this.isPaused = false;
            this.currentPosition = 0;
        }

        public void pause() {
            if (isPlaying) {
                isPaused = true;
            }
        }

        public void resume() {
            if (isPaused) {
                isPaused = false;
            }
        }

        public int getStatus() {
            if (isPlaying && !isPaused) return 1;  // Playing
            if (isPaused) return 2;  // Paused
            return 0;  // Stopped
        }
    }

    public static class AudioData {
        public int memberId;
        public byte[] data;
        public int sampleRate;
        public int sampleSize;
        public int channels;
        public long sampleCount;
        public int duration;  // in milliseconds
        public boolean isCompressed;
        public String compressionType;

        public AudioData() {
            this.memberId = 0;
            this.sampleRate = 44100;
            this.sampleSize = 16;
            this.channels = 2;
            this.sampleCount = 0;
            this.duration = 0;
            this.isCompressed = false;
            this.compressionType = "";
        }
    }
}
