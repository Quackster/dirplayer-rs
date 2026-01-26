package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreFrameChannelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generic keyframe collection utilities for Director properties.
 * Port of Rust collect_property_keyframes and has_real_animation functions.
 *
 * @param <R> The raw type extracted from ScoreFrameChannelData
 * @param <T> The resolved property type
 */
public class PropertyKeyframeCollector<R, T extends DirectorProperty<R, T>> {

    private final Supplier<T> propertyFactory;

    /**
     * Create a collector with a factory for creating property instances.
     * The factory should return an instance that can be used for extractRaw/resolveRaw calls.
     *
     * @param propertyFactory Factory function that creates a property instance
     */
    public PropertyKeyframeCollector(Supplier<T> propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

    /**
     * Frame data entry containing frame number, channel index, and channel data.
     */
    public static class FrameEntry {
        public final int frameNum;
        public final int channelIndex;
        public final ScoreFrameChannelData data;

        public FrameEntry(int frameNum, int channelIndex, ScoreFrameChannelData data) {
            this.frameNum = frameNum;
            this.channelIndex = channelIndex;
            this.data = data;
        }
    }

    /**
     * Keyframe entry containing frame number and property value.
     */
    public static class KeyframeEntry<T> {
        public final int frame;
        public final T value;

        public KeyframeEntry(int frame, T value) {
            this.frame = frame;
            this.value = value;
        }
    }

    /**
     * Resolves effective values per frame and returns only real changes.
     * Filters out frames with default values.
     * Port of Rust collect_property_keyframes function.
     *
     * @param frames List of frame entries to process
     * @return List of keyframe entries with (frame, property) pairs
     */
    public List<KeyframeEntry<T>> collectPropertyKeyframes(List<FrameEntry> frames) {
        List<KeyframeEntry<T>> keyframes = new ArrayList<>();
        T prop = propertyFactory.get();
        T defaultVal = prop.defaultValue();
        T current = prop.defaultValue();
        boolean initialized = false;

        // For colors: get the sprite's baseline color from first frame
        T baseline = null;
        if (prop.useBaselineSkip() && !frames.isEmpty()) {
            Optional<R> rawOpt = prop.extractRaw(frames.get(0).data);
            if (rawOpt.isPresent()) {
                baseline = prop.resolveRaw(rawOpt.get());
            }
        }

        // For colors: check if there's any value different from baseline
        // If so, we need to include the baseline as the first keyframe
        boolean hasAnimation = false;
        if (prop.useBaselineSkip() && baseline != null) {
            final T baselineVal = baseline;
            hasAnimation = frames.stream().anyMatch(entry -> {
                Optional<R> rawOpt = prop.extractRaw(entry.data);
                if (rawOpt.isPresent()) {
                    T resolved = prop.resolveRaw(rawOpt.get());
                    return !resolved.equals(baselineVal);
                }
                return false;
            });
        }

        T lastValue = null;

        for (FrameEntry entry : frames) {
            Optional<R> rawOpt = prop.extractRaw(entry.data);
            if (rawOpt.isPresent()) {
                R raw = rawOpt.get();
                T resolved = prop.resolveWithPrev(raw, lastValue);

                // Update lastValue BEFORE any continue statements
                // so resolveWithPrev has access to previous value on next iteration
                lastValue = resolved;

                // Skip if this is a default value
                if (prop.useBaselineSkip()) {
                    // For colors: if there's animation, include baseline as first keyframe
                    // then skip subsequent baseline values but include changed values
                    if (baseline != null) {
                        if (resolved.equals(baseline)) {
                            // Only include baseline if it's the first frame AND there's animation
                            if (hasAnimation && !initialized) {
                                keyframes.add(new KeyframeEntry<>(entry.frameNum, resolved));
                                current = resolved;
                                initialized = true;
                            }
                            continue;
                        }
                    }
                } else {
                    // Normal logic
                    if (resolved.equals(defaultVal)) {
                        continue;
                    }
                }

                if (!initialized || !resolved.equals(current)) {
                    keyframes.add(new KeyframeEntry<>(entry.frameNum, resolved));
                    current = resolved;
                    initialized = true;
                }
            }
        }

        return keyframes;
    }

    /**
     * Checks if property actually animates across the interval.
     * Only considers non-default values.
     * Port of Rust has_real_animation function.
     *
     * @param frames List of frame entries to check
     * @return true if the property has real animation
     */
    public boolean hasRealAnimation(List<FrameEntry> frames) {
        T prop = propertyFactory.get();
        T defaultVal = prop.defaultValue();
        List<T> values = new ArrayList<>();

        if (prop.useBaselineSkip()) {
            // For colors: animation exists if ANY frame has a non-standard-default color
            // Standard defaults are PaletteIndex(0) and PaletteIndex(255)
            // If all frames only have 0 or 255, it's just sprite initialization, not animation
            // But if any frame has a different color (like 14), that's real animation
            for (FrameEntry entry : frames) {
                Optional<R> rawOpt = prop.extractRaw(entry.data);
                if (rawOpt.isPresent()) {
                    T current = prop.resolveRaw(rawOpt.get());
                    if (!current.isStandardDefault()) {
                        // Found a non-standard-default color - that's real animation!
                        return true;
                    }
                }
            }
            // All frames only have standard defaults (0 or 255) - no animation
            return false;
        }

        // Non-color properties: original logic
        for (FrameEntry entry : frames) {
            Optional<R> rawOpt = prop.extractRaw(entry.data);
            if (rawOpt.isPresent()) {
                T current = prop.resolveRaw(rawOpt.get());
                // Only consider non-default values
                if (!current.equals(defaultVal)) {
                    values.add(current);
                }
            }
        }

        // Need at least 2 different non-default values to be real animation
        if (values.size() < 2) {
            return false;
        }

        T first = values.get(0);
        return values.stream().anyMatch(v -> !v.equals(first));
    }
}
