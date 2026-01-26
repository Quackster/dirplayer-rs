package com.dirplayer.player;

import com.dirplayer.player.sound.SoundChannel;

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

    public SoundChannel getChannel(int channelIdx) {
        if (channelIdx >= 0 && channelIdx < channelCount) {
            return channels[channelIdx];
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

    public int getNumChannels() {
        return channelCount;
    }

    public boolean isChannelBusy(int channelNum) {
        if (channelNum > 0 && channelNum <= channelCount) {
            return channels[channelNum - 1].isBusy();
        }
        return false;
    }

    /**
     * Play a sound on a channel.
     * @param channelNum The channel number (1-based)
     * @param memberRef The sound member reference
     */
    public void playSound(int channelNum, CastMemberRef memberRef) throws ScriptError {
        if (channelNum <= 0 || channelNum > channelCount) {
            throw new ScriptError("Invalid sound channel: " + channelNum);
        }
        SoundChannel channel = channels[channelNum - 1];
        // TODO: Load sound data from cast member and play
        channel.play(memberRef);
    }

    /**
     * Stop a sound channel.
     * @param channelNum The channel number (1-based)
     */
    public void stopSound(int channelNum) throws ScriptError {
        if (channelNum <= 0 || channelNum > channelCount) {
            throw new ScriptError("Invalid sound channel: " + channelNum);
        }
        channels[channelNum - 1].stop();
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
