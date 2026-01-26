package com.dirplayer.player.score;

import com.dirplayer.player.Sprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Score (timeline) for a Director movie.
 * Port of Rust Score struct.
 */
public class Score {
    public List<Channel> channels;
    public List<Frame> frames;
    public Map<String, Integer> frameLabels;
    public int totalFrames;

    public Score() {
        this.channels = new ArrayList<>();
        this.frames = new ArrayList<>();
        this.frameLabels = new HashMap<>();
        this.totalFrames = 0;
    }

    public static Score empty() {
        return new Score();
    }

    public Channel getChannel(int channelNum) {
        if (channelNum >= 0 && channelNum < channels.size()) {
            return channels.get(channelNum);
        }
        return null;
    }

    public Frame getFrame(int frameNum) {
        int index = frameNum - 1;
        if (index >= 0 && index < frames.size()) {
            return frames.get(index);
        }
        return null;
    }

    public Integer getFrameByLabel(String label) {
        return frameLabels.get(label);
    }

    public void setChannelCount(int count) {
        while (channels.size() < count) {
            channels.add(new Channel(channels.size()));
        }
    }

    public void setFrameCount(int count) {
        while (frames.size() < count) {
            frames.add(new Frame(frames.size() + 1));
        }
        totalFrames = count;
    }

    /**
     * Get sprite from a channel.
     */
    public Sprite getSprite(int channelNum) {
        Channel channel = getChannel(channelNum);
        return channel != null ? channel.sprite : null;
    }

    /**
     * Get sorted channel numbers for a frame (in z-order).
     */
    public List<Integer> getSortedChannelNumbers(int frameNum) {
        List<Integer> result = new ArrayList<>();
        Frame frame = getFrame(frameNum);
        if (frame != null) {
            for (FrameSprite fs : frame.sprites) {
                result.add(fs.channelNum);
            }
        } else {
            // Default to all visible channels
            for (int i = 0; i < channels.size(); i++) {
                Channel ch = channels.get(i);
                if (ch.sprite != null && ch.sprite.visible) {
                    result.add(i);
                }
            }
        }
        return result;
    }

    /**
     * Get sorted channels for a frame.
     */
    public List<Channel> getSortedChannels(int frameNum) {
        List<Channel> result = new ArrayList<>();
        for (int num : getSortedChannelNumbers(frameNum)) {
            Channel ch = getChannel(num);
            if (ch != null) {
                result.add(ch);
            }
        }
        return result;
    }

    public static class Channel {
        public int number;
        public String name;
        public Sprite sprite;
        public boolean isPuppet;

        public Channel(int number) {
            this.number = number;
            this.name = "";
            this.sprite = new Sprite(number);
            this.isPuppet = false;
        }
    }

    public static class Frame {
        public int frameNum;
        public List<FrameSprite> sprites;
        public int tempo;
        public int transitionId;
        public int soundChannel1;
        public int soundChannel2;
        public int scriptId;
        public int palette;

        public Frame(int frameNum) {
            this.frameNum = frameNum;
            this.sprites = new ArrayList<>();
            this.tempo = 30;
            this.transitionId = 0;
            this.soundChannel1 = 0;
            this.soundChannel2 = 0;
            this.scriptId = 0;
            this.palette = 0;
        }
    }

    public static class FrameSprite {
        public int channelNum;
        public int memberRef;
        public int locH;
        public int locV;
        public int width;
        public int height;
        public int ink;
        public int blend;
        public boolean visible;
        public int foreColor;
        public int backColor;

        public FrameSprite() {
            this.channelNum = 0;
            this.memberRef = 0;
            this.locH = 0;
            this.locV = 0;
            this.width = 0;
            this.height = 0;
            this.ink = 0;
            this.blend = 100;
            this.visible = true;
            this.foreColor = 255;
            this.backColor = 0;
        }
    }
}
