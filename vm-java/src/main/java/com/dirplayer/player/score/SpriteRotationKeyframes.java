package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.score.SpriteKeyframe.RotationKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks rotation keyframes for a single sprite channel.
 * Port of Rust SpriteRotationKeyframes struct.
 */
public class SpriteRotationKeyframes implements KeyframeTrack<RotationKeyframe> {
    public int channel;
    public List<RotationKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpriteRotationKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<RotationKeyframe> getKeyframes() {
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
     * Get rotation angle at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public Double getRotationAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        RotationKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return currentKf.rotation;
        }
        return null;
    }

    /**
     * Get rotation delta at a specific frame relative to base rotation.
     */
    public Double getDeltaAtFrame(int frame, double baseRotation) {
        Double r = getRotationAtFrame(frame);
        if (r == null) {
            return null;
        }
        return r - baseRotation;
    }

    /**
     * Check if this track has valid tween info for rotation tweening.
     */
    public boolean isRotationTweened() {
        return tweenInfo != null && tweenInfo.isRotationTweened();
    }
}
