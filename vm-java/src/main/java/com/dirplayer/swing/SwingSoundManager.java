package com.dirplayer.swing;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.SoundManager;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Sound manager for Swing player using Java Sound API.
 * Provides audio playback functionality matching the Rust player.
 */
public class SwingSoundManager {
    private final DirPlayer player;

    // Sound channels (Director supports up to 8 sound channels)
    private static final int MAX_CHANNELS = 8;
    private final SoundChannel[] channels = new SoundChannel[MAX_CHANNELS];

    // Audio format for playback
    private static final float SAMPLE_RATE = 22050.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS_MONO = 1;
    private static final int CHANNELS_STEREO = 2;

    public SwingSoundManager(DirPlayer player) {
        this.player = player;

        // Initialize sound channels
        for (int i = 0; i < MAX_CHANNELS; i++) {
            channels[i] = new SoundChannel(i + 1);
        }
    }

    /**
     * Update sound playback state.
     * Called each tick to check for new sounds to play.
     */
    public void update() {
        // Check player's sound manager for queued sounds
        SoundManager sm = player.soundManager;
        if (sm == null) return;

        for (int i = 0; i < MAX_CHANNELS; i++) {
            // Check if channel needs to play a sound
            SoundManager.SoundChannelState state = sm.getChannelState(i + 1);
            if (state != null && state.shouldPlay) {
                playSound(i + 1, state.soundData, state.loop);
                state.shouldPlay = false;
            }

            // Update channel state
            if (channels[i].isPlaying()) {
                sm.setChannelBusy(i + 1, true);
            } else {
                sm.setChannelBusy(i + 1, false);
            }
        }
    }

    /**
     * Play a sound on the specified channel.
     */
    public void playSound(int channelNum, byte[] audioData, boolean loop) {
        if (channelNum < 1 || channelNum > MAX_CHANNELS) return;

        SoundChannel channel = channels[channelNum - 1];
        channel.stop();

        if (audioData != null && audioData.length > 0) {
            channel.play(audioData, loop);
        }
    }

    /**
     * Stop sound on the specified channel.
     */
    public void stopChannel(int channelNum) {
        if (channelNum < 1 || channelNum > MAX_CHANNELS) return;
        channels[channelNum - 1].stop();
    }

    /**
     * Stop all sounds.
     */
    public void stopAll() {
        for (SoundChannel channel : channels) {
            channel.stop();
        }
    }

    /**
     * Set volume for a channel (0.0 to 1.0).
     */
    public void setVolume(int channelNum, float volume) {
        if (channelNum < 1 || channelNum > MAX_CHANNELS) return;
        channels[channelNum - 1].setVolume(volume);
    }

    /**
     * Check if a channel is currently playing.
     */
    public boolean isChannelBusy(int channelNum) {
        if (channelNum < 1 || channelNum > MAX_CHANNELS) return false;
        return channels[channelNum - 1].isPlaying();
    }

    /**
     * Internal class representing a single sound channel.
     */
    private static class SoundChannel {
        private final int channelNum;
        private Clip clip;
        private float volume = 1.0f;
        private boolean looping = false;

        public SoundChannel(int channelNum) {
            this.channelNum = channelNum;
        }

        public void play(byte[] audioData, boolean loop) {
            stop();

            try {
                // Try to detect audio format from data
                AudioFormat format = detectAudioFormat(audioData);
                if (format == null) {
                    // Default format for Director sounds
                    format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        SAMPLE_RATE,
                        SAMPLE_SIZE_BITS,
                        CHANNELS_MONO,
                        2, // frame size
                        SAMPLE_RATE,
                        false // little endian
                    );
                }

                // Create audio input stream
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream ais;

                try {
                    // Try to get audio system to decode
                    ais = AudioSystem.getAudioInputStream(bais);
                } catch (Exception e) {
                    // Use raw PCM format
                    ais = new AudioInputStream(bais, format,
                        audioData.length / format.getFrameSize());
                }

                // Get a clip and open with the stream
                clip = AudioSystem.getClip();
                clip.open(ais);

                // Set volume
                applyVolume();

                // Set looping
                this.looping = loop;
                if (loop) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    clip.start();
                }

            } catch (Exception e) {
                System.err.println("Failed to play sound on channel " + channelNum + ": " + e.getMessage());
            }
        }

        public void stop() {
            if (clip != null) {
                clip.stop();
                clip.close();
                clip = null;
            }
            looping = false;
        }

        public boolean isPlaying() {
            return clip != null && clip.isRunning();
        }

        public void setVolume(float vol) {
            this.volume = Math.max(0.0f, Math.min(1.0f, vol));
            applyVolume();
        }

        private void applyVolume() {
            if (clip != null) {
                try {
                    FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    // Convert linear volume to dB
                    float dB = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                    dB = Math.max(control.getMinimum(), Math.min(control.getMaximum(), dB));
                    control.setValue(dB);
                } catch (Exception e) {
                    // Volume control not available
                }
            }
        }

        /**
         * Try to detect audio format from data.
         */
        private AudioFormat detectAudioFormat(byte[] data) {
            if (data == null || data.length < 44) return null;

            // Check for WAV header
            if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
                    return ais.getFormat();
                } catch (Exception e) {
                    // Not a valid WAV
                }
            }

            // Check for AIFF header
            if (data[0] == 'F' && data[1] == 'O' && data[2] == 'R' && data[3] == 'M') {
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
                    return ais.getFormat();
                } catch (Exception e) {
                    // Not a valid AIFF
                }
            }

            return null;
        }
    }
}
