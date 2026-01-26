package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.score.SpriteKeyframe.SkewKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks skew keyframes for a single sprite channel.
 * Port of Rust SpriteSkewKeyframes struct.
 */
public class SpriteSkewKeyframes implements KeyframeTrack<SkewKeyframe> {
    public int channel;
    public List<SkewKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpriteSkewKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<SkewKeyframe> getKeyframes() {
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
     * Get skew angle at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public Double getSkewAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        SkewKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return currentKf.skew;
        }
        return null;
    }

    /**
     * Get skew delta at a specific frame relative to base skew.
     */
    public Double getDeltaAtFrame(int frame, double baseSkew) {
        Double s = getSkewAtFrame(frame);
        if (s == null) {
            return null;
        }
        return s - baseSkew;
    }

    /**
     * Check if this track has valid tween info for skew tweening.
     */
    public boolean isSkewTweened() {
        return tweenInfo != null && tweenInfo.isSkewTweened();
    }
}
