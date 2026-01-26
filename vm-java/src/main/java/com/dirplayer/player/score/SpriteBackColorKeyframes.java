package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.score.SpriteKeyframe.ColorKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks background color keyframes for a single sprite channel.
 * Port of Rust SpriteBackColorKeyframes struct.
 */
public class SpriteBackColorKeyframes implements KeyframeTrack<ColorKeyframe> {
    public int channel;
    public List<ColorKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpriteBackColorKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<ColorKeyframe> getKeyframes() {
        return keyframes;
    }

    @Override
    public List<int[]> getIntervals() {
        return intervals;
    }

    @Override
    public int[] getFrameRange() {
        if (keyframes.isEmpty()) {
            return null;
        }
        int first = keyframes.get(0).frame;
        int last = keyframes.get(keyframes.size() - 1).frame;
        return new int[] { first, last };
    }

    /**
     * Get color at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public ColorRef getColorAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        ColorKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return currentKf.color;
        }
        return null;
    }

    /**
     * Check if this track has valid tween info for backcolor tweening.
     */
    public boolean isBackcolorTweened() {
        return tweenInfo != null && tweenInfo.isBackcolorTweened();
    }
}
