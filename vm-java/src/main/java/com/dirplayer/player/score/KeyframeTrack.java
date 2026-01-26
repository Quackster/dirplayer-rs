package com.dirplayer.player.score;

import java.util.List;

/**
 * Interface for keyframe animation tracks.
 * Port of Rust KeyframeTrack trait.
 *
 * @param <T> The keyframe type (must extend SpriteKeyframe)
 */
public interface KeyframeTrack<T extends SpriteKeyframe> {

    /**
     * Get all keyframes in this track.
     */
    List<T> getKeyframes();

    /**
     * Get the intervals for this track.
     * Each interval is an int[] with {startFrame, endFrame}.
     */
    List<int[]> getIntervals();

    /**
     * Get the frame range of this track.
     * Returns int[] with {firstFrame, lastFrame} or null if empty.
     */
    int[] getFrameRange();

    /**
     * Check if this track is active at the given frame.
     * Default implementation checks if the frame falls within any interval
     * that contains keyframes both before and after the current frame.
     */
    default boolean isActiveAtFrame(int frame) {
        List<T> keyframes = getKeyframes();
        List<int[]> intervals = getIntervals();

        if (keyframes.isEmpty()) {
            return false;
        }

        // Case 1: Frame is exactly on a keyframe - always active
        for (T kf : keyframes) {
            if (kf.getFrame() == frame) {
                return true;
            }
        }

        // Case 2: Frame is between two keyframes - check if they're in the same interval
        T prevKf = null;
        T nextKf = null;

        // Find previous keyframe
        for (int i = keyframes.size() - 1; i >= 0; i--) {
            if (keyframes.get(i).getFrame() <= frame) {
                prevKf = keyframes.get(i);
                break;
            }
        }

        // Find next keyframe
        for (T kf : keyframes) {
            if (kf.getFrame() > frame) {
                nextKf = kf;
                break;
            }
        }

        if (prevKf != null && nextKf != null) {
            int prevFrame = prevKf.getFrame();
            int nextFrame = nextKf.getFrame();

            // Check if both keyframes and current frame are in the same interval
            for (int[] interval : intervals) {
                int start = interval[0];
                int end = interval[1];

                if (prevFrame >= start && prevFrame <= end &&
                    nextFrame >= start && nextFrame <= end &&
                    frame >= start && frame <= end) {
                    return true;
                }
            }
        }

        return false;
    }
}
