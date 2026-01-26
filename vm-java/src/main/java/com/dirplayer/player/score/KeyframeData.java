package com.dirplayer.player.score;

/**
 * Interface for keyframe data.
 * Port of Rust KeyframeData trait from score_keyframes.rs.
 *
 * All keyframe types (BlendKeyframe, RotationKeyframe, etc.) implement
 * this interface to provide uniform access to the frame number.
 */
public interface KeyframeData {
    /**
     * Get the frame number for this keyframe.
     * @return The frame number (1-indexed Director frame)
     */
    int getFrame();
}
