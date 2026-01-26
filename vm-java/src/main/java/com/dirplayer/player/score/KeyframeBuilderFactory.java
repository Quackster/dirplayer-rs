package com.dirplayer.player.score;

import com.dirplayer.director.chunks.ScoreChunk.FrameIntervalPrimary;
import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.score.SpriteKeyframe.*;
import com.dirplayer.player.score.properties.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory for building keyframe tracks using the generic property system.
 * Port of Rust keyframe building functions from score_keyframes.rs.
 *
 * This factory provides methods for building all types of keyframe tracks:
 * - Path (position) keyframes
 * - Size keyframes
 * - Rotation keyframes
 * - Skew keyframes
 * - Blend keyframes
 * - ForeColor keyframes
 * - BackColor keyframes
 */
public class KeyframeBuilderFactory {
    private static final Logger logger = LoggerFactory.getLogger(KeyframeBuilderFactory.class);

    /**
     * Build all keyframes cache from frame channel data and intervals.
     * Port of Rust build_all_keyframes_cache function.
     *
     * @param frameChannelData List of frame channel entries
     * @param frameIntervals Map of channel number to list of intervals
     * @return Map of channel number to ChannelKeyframes
     */
    public static Map<Integer, ChannelKeyframes> buildAllKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        logger.debug("Building keyframes cache from {} frame entries and {} interval channels",
            frameChannelData.size(), frameIntervals.size());

        Map<Integer, ChannelKeyframes> combinedCache = new HashMap<>();

        // Get unique channel indices
        Set<Integer> channelIndices = frameChannelData.stream()
            .map(e -> e.channelIndex)
            .collect(Collectors.toSet());

        // Build individual keyframe type caches
        Map<Integer, SpriteBlendKeyframes> blendCache = buildBlendKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpriteRotationKeyframes> rotationCache = buildRotationKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpriteSkewKeyframes> skewCache = buildSkewKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpritePathKeyframes> pathCache = buildPathKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpriteSizeKeyframes> sizeCache = buildSizeKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpriteForeColorKeyframes> foreColorCache = buildForeColorKeyframesCache(frameChannelData, frameIntervals);
        Map<Integer, SpriteBackColorKeyframes> backColorCache = buildBackColorKeyframesCache(frameChannelData, frameIntervals);

        // Combine into ChannelKeyframes
        Set<Integer> allChannels = new HashSet<>();
        allChannels.addAll(blendCache.keySet());
        allChannels.addAll(rotationCache.keySet());
        allChannels.addAll(skewCache.keySet());
        allChannels.addAll(pathCache.keySet());
        allChannels.addAll(sizeCache.keySet());
        allChannels.addAll(foreColorCache.keySet());
        allChannels.addAll(backColorCache.keySet());

        for (int channel : allChannels) {
            ChannelKeyframes keyframes = new ChannelKeyframes(channel);
            keyframes.blend = blendCache.get(channel);
            keyframes.rotation = rotationCache.get(channel);
            keyframes.skew = skewCache.get(channel);
            keyframes.path = pathCache.get(channel);
            keyframes.size = sizeCache.get(channel);
            keyframes.foreColor = foreColorCache.get(channel);
            keyframes.backColor = backColorCache.get(channel);
            combinedCache.put(channel, keyframes);
        }

        logger.debug("Built keyframes cache for {} channels", combinedCache.size());
        return combinedCache;
    }

    /**
     * Build blend keyframes cache.
     */
    public static Map<Integer, SpriteBlendKeyframes> buildBlendKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteBlendKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteBlendKeyframes keyframes = buildBlendKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("blend", cache.size());
        return cache;
    }

    /**
     * Build rotation keyframes cache.
     */
    public static Map<Integer, SpriteRotationKeyframes> buildRotationKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteRotationKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteRotationKeyframes keyframes = buildRotationKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("rotation", cache.size());
        return cache;
    }

    /**
     * Build skew keyframes cache.
     */
    public static Map<Integer, SpriteSkewKeyframes> buildSkewKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteSkewKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteSkewKeyframes keyframes = buildSkewKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("skew", cache.size());
        return cache;
    }

    /**
     * Build path keyframes cache.
     */
    public static Map<Integer, SpritePathKeyframes> buildPathKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpritePathKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpritePathKeyframes keyframes = buildPathKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("path", cache.size());
        return cache;
    }

    /**
     * Build size keyframes cache.
     */
    public static Map<Integer, SpriteSizeKeyframes> buildSizeKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteSizeKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteSizeKeyframes keyframes = buildSizeKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("size", cache.size());
        return cache;
    }

    /**
     * Build fore color keyframes cache.
     */
    public static Map<Integer, SpriteForeColorKeyframes> buildForeColorKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteForeColorKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteForeColorKeyframes keyframes = buildForeColorKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("fore color", cache.size());
        return cache;
    }

    /**
     * Build back color keyframes cache.
     */
    public static Map<Integer, SpriteBackColorKeyframes> buildBackColorKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            Map<Integer, List<FrameIntervalPrimary>> frameIntervals) {

        Map<Integer, SpriteBackColorKeyframes> cache = new HashMap<>();
        Set<Integer> channelIndices = getUniqueChannelIndices(frameChannelData);

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = frameIntervals.get(channelNum);

            if (intervals == null || intervals.isEmpty()) continue;

            SpriteBackColorKeyframes keyframes = buildBackColorKeyframes(channelIndex, frameChannelData, intervals);
            if (!keyframes.keyframes.isEmpty()) {
                cache.put(keyframes.channel, keyframes);
            }
        }

        logCacheResult("back color", cache.size());
        return cache;
    }

    // ========== Individual keyframe builders ==========

    private static SpriteBlendKeyframes buildBlendKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteBlendKeyframes keyframes = new SpriteBlendKeyframes(channelIndex, tweenInfo);

        // Store interval ranges
        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        // Process each interval
        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isBlendTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            // Check for real animation using generic collector
            PropertyKeyframeCollector<Integer, Blend> collector =
                new PropertyKeyframeCollector<>(() -> new Blend(100));

            if (!collector.hasRealAnimation(frames)) continue;

            // Collect keyframes
            List<PropertyKeyframeCollector.KeyframeEntry<Blend>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<Blend> entry : propFrames) {
                int blendPercent = KeyframeUtils.convertBlendToPercentage(entry.value.value);
                keyframes.keyframes.add(new BlendKeyframe(entry.frame, blendPercent));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("blend", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpriteRotationKeyframes buildRotationKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteRotationKeyframes keyframes = new SpriteRotationKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isRotationTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<Double, Rotation> collector =
                new PropertyKeyframeCollector<>(() -> new Rotation(0.0));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<Rotation>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<Rotation> entry : propFrames) {
                keyframes.keyframes.add(new RotationKeyframe(entry.frame, entry.value.value));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("rotation", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpriteSkewKeyframes buildSkewKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteSkewKeyframes keyframes = new SpriteSkewKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isSkewTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<Double, Skew> collector =
                new PropertyKeyframeCollector<>(() -> new Skew(0.0));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<Skew>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<Skew> entry : propFrames) {
                keyframes.keyframes.add(new SkewKeyframe(entry.frame, entry.value.value));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("skew", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpritePathKeyframes buildPathKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpritePathKeyframes keyframes = new SpritePathKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isPathTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<short[], Position> collector =
                new PropertyKeyframeCollector<>(() -> new Position((short) 0, (short) 0));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<Position>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<Position> entry : propFrames) {
                // Convert 0-based frame_idx to 1-based Director frame
                int directorFrame = entry.frame + 1;
                keyframes.keyframes.add(new PathKeyframe(directorFrame, entry.value.x, entry.value.y));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("path", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpriteSizeKeyframes buildSizeKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteSizeKeyframes keyframes = new SpriteSizeKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isSizeTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<int[], Size> collector =
                new PropertyKeyframeCollector<>(() -> new Size(0, 0));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<Size>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<Size> entry : propFrames) {
                keyframes.keyframes.add(new SizeKeyframe(entry.frame, entry.value.width, entry.value.height));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("size", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpriteForeColorKeyframes buildForeColorKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteForeColorKeyframes keyframes = new SpriteForeColorKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isForecolorTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<ColorRef, ForeColor> collector =
                new PropertyKeyframeCollector<>(() -> new ForeColor(ColorRef.paletteIndex(255)));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<ForeColor>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<ForeColor> entry : propFrames) {
                keyframes.keyframes.add(new ColorKeyframe(entry.frame, entry.value.color));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("fore color", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    private static SpriteBackColorKeyframes buildBackColorKeyframes(
            int channelIndex,
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPrimary> intervals) {

        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteBackColorKeyframes keyframes = new SpriteBackColorKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isBackcolorTweened()) continue;

            List<PropertyKeyframeCollector.FrameEntry> frames = collectFramesForInterval(
                frameChannelData, channelIndex, interval);

            if (frames.isEmpty()) continue;

            PropertyKeyframeCollector<ColorRef, BackColor> collector =
                new PropertyKeyframeCollector<>(() -> new BackColor(ColorRef.paletteIndex(0)));

            if (!collector.hasRealAnimation(frames)) continue;

            List<PropertyKeyframeCollector.KeyframeEntry<BackColor>> propFrames =
                collector.collectPropertyKeyframes(frames);

            for (PropertyKeyframeCollector.KeyframeEntry<BackColor> entry : propFrames) {
                keyframes.keyframes.add(new ColorKeyframe(entry.frame, entry.value.color));
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        logKeyframesBuilt("back color", keyframes.channel, keyframes.keyframes.size(), intervals.size());
        return keyframes;
    }

    // ========== Helper methods ==========

    private static Set<Integer> getUniqueChannelIndices(List<FrameChannelEntry> frameChannelData) {
        return frameChannelData.stream()
            .map(e -> e.channelIndex)
            .collect(Collectors.toSet());
    }

    private static List<PropertyKeyframeCollector.FrameEntry> collectFramesForInterval(
            List<FrameChannelEntry> frameChannelData,
            int channelIndex,
            FrameIntervalPrimary interval) {

        // Collect frames in Director frame range AND matching this channel
        List<PropertyKeyframeCollector.FrameEntry> frames = new ArrayList<>();
        Map<Integer, FrameChannelEntry> dedup = new HashMap<>();

        for (FrameChannelEntry entry : frameChannelData) {
            int directorFrame = entry.frameIndex + 1;
            boolean inRange = directorFrame >= interval.startFrame && directorFrame <= interval.endFrame;
            boolean matchesChannel = entry.channelIndex == channelIndex;

            if (inRange && matchesChannel) {
                // Deduplicate by frame number
                dedup.put(entry.frameIndex, entry);
            }
        }

        // Sort by frame number and convert to FrameEntry
        List<FrameChannelEntry> sorted = new ArrayList<>(dedup.values());
        sorted.sort(Comparator.comparingInt(e -> e.frameIndex));

        for (FrameChannelEntry entry : sorted) {
            frames.add(new PropertyKeyframeCollector.FrameEntry(
                entry.frameIndex, entry.channelIndex, entry.data));
        }

        return frames;
    }

    private static void logCacheResult(String type, int count) {
        if (count == 0) {
            logger.debug("No {} keyframes found", type);
        } else {
            logger.debug("Found {} keyframes in {} channels", type, count);
        }
    }

    private static void logKeyframesBuilt(String type, int channel, int count, int intervalCount) {
        if (count > 0) {
            logger.debug("Channel {} has {} {} keyframes across {} intervals",
                channel, count, type, intervalCount);
        }
    }
}
