package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.score.SpriteKeyframe.PathKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks path (position) keyframes for a single sprite channel.
 * Port of Rust SpritePathKeyframes struct.
 */
public class SpritePathKeyframes implements KeyframeTrack<PathKeyframe> {
    public int channel;
    public List<PathKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpritePathKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<PathKeyframe> getKeyframes() {
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
     * Get interpolated position at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public short[] getPositionAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        PathKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return new short[] { currentKf.x, currentKf.y };
        }
        return null;
    }

    /**
     * Get position delta at a specific frame relative to base position.
     */
    public int[] getDeltaAtFrame(int frame, int baseX, int baseY) {
        short[] pos = getPositionAtFrame(frame);
        if (pos == null) {
            return null;
        }
        return new int[] {
            pos[0] - baseX,
            pos[1] - baseY
        };
    }

    /**
     * Check if this track has valid tween info for path tweening.
     */
    public boolean isPathTweened() {
        return tweenInfo != null && tweenInfo.isPathTweened();
    }
}
