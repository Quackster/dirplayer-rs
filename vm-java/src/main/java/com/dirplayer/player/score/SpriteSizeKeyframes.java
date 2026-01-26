package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.player.score.SpriteKeyframe.SizeKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks size keyframes for a single sprite channel.
 * Port of Rust SpriteSizeKeyframes struct.
 */
public class SpriteSizeKeyframes implements KeyframeTrack<SizeKeyframe> {
    public int channel;
    public List<SizeKeyframe> keyframes;
    public TweenInfo tweenInfo;
    public List<int[]> intervals; // Each int[] is {startFrame, endFrame}

    public SpriteSizeKeyframes(int channelIndex, TweenInfo tweenInfo) {
        this.channel = KeyframeUtils.indexToChannelNumber(channelIndex);
        this.keyframes = new ArrayList<>();
        this.tweenInfo = tweenInfo;
        this.intervals = new ArrayList<>();
    }

    @Override
    public List<SizeKeyframe> getKeyframes() {
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
     * Get size (width, height) at a specific frame.
     * Returns null if the frame is outside the active range.
     */
    public int[] getSizeAtFrame(int frame) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (!isActiveAtFrame(frame)) {
            return null;
        }

        // Find the keyframe at or before the current frame
        SizeKeyframe currentKf = null;
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).frame <= frame) {
                currentKf = keyframes.get(i);
                break;
            }
        }

        if (currentKf != null) {
            return new int[] { currentKf.width, currentKf.height };
        }
        return null;
    }

    /**
     * Get size delta at a specific frame relative to base size.
     */
    public int[] getDeltaAtFrame(int frame, int baseWidth, int baseHeight) {
        int[] size = getSizeAtFrame(frame);
        if (size == null) {
            return null;
        }
        return new int[] {
            size[0] - baseWidth,
            size[1] - baseHeight
        };
    }

    /**
     * Check if this track has valid tween info for size tweening.
     */
    public boolean isSizeTweened() {
        return tweenInfo != null && tweenInfo.isSizeTweened();
    }
}
