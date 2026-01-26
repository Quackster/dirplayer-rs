package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.score.SpriteKeyframe.BlendKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks blend keyframes for a single sprite channel.
 * Port of Rust SpriteBlendKeyframes struct.
 */
public class SpriteBlendKeyframes implements KeyframeTrack<BlendKeyframe> {
    public int channel;
    public List<BlendKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpriteBlendKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<BlendKeyframe> getKeyframes() {
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
     * Get blend percentage at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public Integer getBlendAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        BlendKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return currentKf.blendPercent;
        }
        return null;
    }

    /**
     * Check if this track has valid tween info for blend tweening.
     */
    public boolean isBlendTweened() {
        return tweenInfo != null && tweenInfo.isBlendTweened();
    }
}
