package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreFrameChannelData;

import java.util.Optional;

/**
 * Interface for Director property extraction and resolution.
 * Port of Rust DirectorProperty trait from score_keyframes.rs.
 *
 * This trait enables generic keyframe collection by defining how to:
 * - Extract raw values from frame data
 * - Resolve raw values into property values
 * - Handle delta-aware resolution (for properties that accumulate)
 * - Provide default values
 *
 * @param <R> The raw type extracted from ScoreFrameChannelData
 * @param <T> The resolved property type (typically the implementing class itself)
 */
public interface DirectorProperty<R, T> {

    /**
     * Whether to use baseline skip logic (used for color properties).
     * When true, the first frame's value is used as a baseline, and
     * subsequent frames with the baseline value are skipped.
     */
    default boolean useBaselineSkip() {
        return false;
    }

    /**
     * Extract raw value (if present) from frame data.
     * @param data The score frame channel data
     * @return Optional containing the raw value, or empty if not extractable
     */
    Optional<R> extractRaw(ScoreFrameChannelData data);

    /**
     * Convert raw value into a resolved property value.
     * @param raw The raw value extracted from frame data
     * @return The resolved property value
     */
    T resolveRaw(R raw);

    /**
     * Resolve with previous value (delta-aware).
     * Default implementation just calls resolveRaw.
     * Override for properties that need to consider previous values
     * (e.g., Position where 0 means "keep previous").
     *
     * @param raw The raw value
     * @param prev The previous resolved value, if any
     * @return The resolved property value
     */
    default T resolveWithPrev(R raw, T prev) {
        return resolveRaw(raw);
    }

    /**
     * Get the default value for this property.
     * Used when the property never appears in frame data.
     * @return The default property value
     */
    T defaultValue();

    /**
     * Check if this value is a standard default.
     * Used to filter out false positive "animations" between default values.
     * For example, palette index 0 and 255 are standard defaults for colors.
     * @return true if this is a standard default value
     */
    default boolean isStandardDefault() {
        return false;
    }
}
