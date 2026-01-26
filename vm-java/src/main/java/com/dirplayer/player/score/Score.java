package com.dirplayer.player.score;

import com.dirplayer.director.chunks.FrameLabelsChunk.FrameLabel;
import com.dirplayer.director.chunks.ScoreChunk;
import com.dirplayer.director.chunks.ScoreChunk.FrameIntervalPair;
import com.dirplayer.director.chunks.ScoreChunk.FrameIntervalPrimary;
import com.dirplayer.director.chunks.ScoreChunk.FrameIntervalSecondary;
import com.dirplayer.director.chunks.ScoreChunk.SpriteDetailInfo;
import com.dirplayer.director.chunks.ScoreChunk.SpriteBehavior;
import com.dirplayer.director.chunks.ScoreChunk.TweenInfo;
import com.dirplayer.director.chunks.ScoreFrameChannelData;
import com.dirplayer.director.chunks.ScoreFrameData;
import com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry;
import com.dirplayer.director.chunks.ScoreFrameData.SoundChannelEntry;
import com.dirplayer.director.chunks.ScoreFrameData.TempoChannelEntry;
import com.dirplayer.director.chunks.SoundChannelData;
import com.dirplayer.director.chunks.TempoChannelData;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.score.SpriteKeyframe.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Score (timeline) for a Director movie.
 * Manages sprite channels, frame data, and animation tweening.
 * Port of Rust Score struct from score.rs.
 */
public class Score {
    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Sprite channels in the score */
    public List<SpriteChannel> channels;

    /** Sprite spans defining when sprites are active */
    public List<ScoreSpriteSpan> spriteSpans;

    /** Per-frame sprite initialization data (frameIndex, channelIndex, data) */
    public List<FrameChannelEntry> channelInitializationData;

    /** Sound channel data (frameIndex, channelIndex, data) */
    public List<SoundChannelEntry> soundChannelData;

    /** Tempo channel data (frameIndex, data) */
    public List<TempoChannelEntry> tempoChannelData;

    /** Frame labels for navigation */
    public List<FrameLabel> frameLabels;

    /** Track which sounds have been triggered on which frames */
    public Map<Integer, Integer> soundChannelTriggered;

    /** Keyframes cache for tween animations */
    public Map<Integer, ChannelKeyframes> keyframesCache;

    /** Sprite detail behaviors indexed by spriteListIdx (D6+) */
    public Map<Integer, SpriteDetailInfo> spriteDetails;

    /** Track the last frame where we cleared sound triggers */
    public Integer lastSoundClearFrame;

    public Score() {
        this.channels = new ArrayList<>();
        this.spriteSpans = new ArrayList<>();
        this.channelInitializationData = new ArrayList<>();
        this.soundChannelData = new ArrayList<>();
        this.tempoChannelData = new ArrayList<>();
        this.frameLabels = new ArrayList<>();
        this.soundChannelTriggered = new HashMap<>();
        this.keyframesCache = new HashMap<>();
        this.spriteDetails = new HashMap<>();
        this.lastSoundClearFrame = null;
    }

    /**
     * Create an empty score.
     */
    public static Score empty() {
        return new Score();
    }

    /**
     * Get the script behavior reference for a frame (if any).
     */
    public ScoreBehaviorReference getScriptInFrame(int frame) {
        for (ScoreSpriteSpan span : spriteSpans) {
            if (span.channelNumber == 0 && frame >= span.startFrame && frame <= span.endFrame) {
                if (!span.scripts.isEmpty()) {
                    return span.scripts.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Check if a span is active at a given frame.
     */
    public static boolean isSpanInFrame(ScoreSpriteSpan span, int frameNum) {
        return span.startFrame <= frameNum && span.endFrame >= frameNum;
    }

    /**
     * Begin sprites for a frame - initialize sprites entering the frame.
     * This handles sprite initialization, behavior attachment, and sound triggering.
     */
    public void beginSprites(ScoreRef scoreRef, int frameNum) {
        // Clean up sound channel triggers - but only once per frame
        boolean alreadyProcessed = lastSoundClearFrame != null && lastSoundClearFrame == frameNum;

        if (!alreadyProcessed) {
            lastSoundClearFrame = frameNum;

            // For film loops, clear all triggers when starting fresh or looping
            boolean shouldClearAll = frameNum == 1 ||
                soundChannelTriggered.values().stream().anyMatch(triggeredFrame -> triggeredFrame > frameNum);

            if (shouldClearAll) {
                soundChannelTriggered.clear();
            } else {
                // Normal progression - only clear triggers for sounds not on current frame
                Set<Integer> soundsOnCurrentFrame = new HashSet<>();
                for (SoundChannelEntry entry : soundChannelData) {
                    if (entry.frameIndex + 1 == frameNum) {
                        soundsOnCurrentFrame.add(entry.channelIndex);
                    }
                }

                soundChannelTriggered.entrySet().removeIf(
                    entry -> !soundsOnCurrentFrame.contains(entry.getKey())
                );
            }
        }

        // Find spans that should be entered
        List<ScoreSpriteSpan> spansToEnter = spriteSpans.stream()
            .filter(span -> isSpanInFrame(span, frameNum))
            .filter(span -> {
                Sprite sprite = getSprite((short) span.channelNumber);
                return sprite == null || (!sprite.entered && !sprite.exited);
            })
            .collect(Collectors.toList());

        // Get initialization data for sprites
        List<SpanInitData> spanInitData = new ArrayList<>();
        for (ScoreSpriteSpan span : spansToEnter) {
            for (FrameChannelEntry entry : channelInitializationData) {
                int channelNumber = KeyframeUtils.getChannelNumberFromIndex(entry.channelIndex);
                if (channelNumber == span.channelNumber && entry.frameIndex + 1 == span.startFrame) {
                    spanInitData.add(new SpanInitData(span, entry.channelIndex, entry.data));
                    break;
                }
            }
        }

        // Initialize sprite properties (member, position, etc.)
        for (SpanInitData initData : spanInitData) {
            ScoreSpriteSpan span = initData.span;
            int channelIndex = initData.channelIndex;
            ScoreFrameChannelData data = initData.data;

            short spriteNum = (short) span.channelNumber;
            Sprite sprite = getSpriteMut(spriteNum);
            if (sprite == null) continue;

            sprite.entered = true;
            boolean isSprite = span.channelNumber > 0;

            if (isSprite) {
                // Resolve cast_lib 65535 to cast 1 for main stage sprites
                int resolvedCastLib = data.castLib;
                if (data.castLib == 65535 && scoreRef.isStage()) {
                    resolvedCastLib = 1;
                }

                CastMemberRef member = new CastMemberRef(resolvedCastLib, data.castMember);

                // Set sprite properties
                sprite.setMember(member);
                sprite.locH = data.posX;
                sprite.locV = data.posY;
                sprite.width = data.width;
                sprite.height = data.height;
                sprite.skew = (int) data.skew;
                sprite.rotation = (int) data.rotation;

                // Handle ink and blend based on member type
                // Note: In full implementation, we'd check if member is a shape
                sprite.ink = data.ink;
                sprite.blend = data.blend == 0 ? 100 : data.blend;

                // Handle color flags
                switch (data.colorFlag) {
                    case 0:
                        // fore + back are palette indexes
                        sprite.foreColor = data.foreColor;
                        sprite.setColor(ColorRef.paletteIndex(data.foreColor));
                        sprite.backColor = data.backColor;
                        sprite.setBgColor(ColorRef.paletteIndex(data.backColor));
                        break;
                    case 1:
                        // foreColor is RGB, backColor is palette index
                        sprite.setColor(ColorRef.rgb(data.foreColor, data.foreColorG, data.foreColorB));
                        sprite.backColor = data.backColor;
                        sprite.setBgColor(ColorRef.paletteIndex(data.backColor));
                        break;
                    case 2:
                        // foreColor is palette index, backColor is RGB
                        sprite.foreColor = data.foreColor;
                        sprite.setColor(ColorRef.paletteIndex(data.foreColor));
                        sprite.setBgColor(ColorRef.rgb(data.backColor, data.backColorG, data.backColorB));
                        break;
                    case 3:
                        // both fore + back are RGB
                        sprite.setColor(ColorRef.rgb(data.foreColor, data.foreColorG, data.foreColorB));
                        sprite.setBgColor(ColorRef.rgb(data.backColor, data.backColorG, data.backColorB));
                        break;
                    default:
                        logger.warn("Unexpected color flag: {}", data.colorFlag);
                }

                // Store base values for tweening
                sprite.baseLocH = sprite.locH;
                sprite.baseLocV = sprite.locV;
                sprite.baseWidth = sprite.width;
                sprite.baseHeight = sprite.height;
                sprite.baseRotation = sprite.rotation;
                sprite.baseBlend = sprite.blend;
                sprite.baseSkew = sprite.skew;
                sprite.baseColor = sprite.getColor() != null ? sprite.getColor().copy() : null;
                sprite.baseBgColor = sprite.getBgColor() != null ? sprite.getBgColor().copy() : null;

                // Reset size flags
                sprite.setHasSizeTweened(false);
                sprite.setHasSizeChanged(false);
            }
        }

        // Note: Sound handling and behavior attachment would require
        // additional infrastructure (sound manager, script system)
        // that is beyond the scope of this port

        logger.debug("beginSprites: frame {} - {} spans entered", frameNum, spansToEnter.size());
    }

    /**
     * Apply tween modifiers to sprites for the given frame.
     */
    public void applyTweenModifiers(int frame) {
        // Build set of active channels for this frame
        Set<Integer> activeChannels = spriteSpans.stream()
            .filter(span -> isSpanInFrame(span, frame))
            .map(span -> span.channelNumber)
            .collect(Collectors.toSet());

        for (SpriteChannel channel : channels) {
            Sprite sprite = channel.sprite;
            int spriteNum = sprite.number;

            // Skip if sprite isn't in an active span
            if (!activeChannels.contains(spriteNum)) {
                if (!sprite.puppet || !sprite.visible) {
                    continue;
                }
            }

            ChannelKeyframes keyframes = keyframesCache.get(spriteNum);
            if (keyframes == null) {
                continue;
            }

            // Position tween (additive)
            if (keyframes.path != null &&
                keyframes.path.tweenInfo != null &&
                keyframes.path.tweenInfo.isPathTweened() &&
                keyframes.path.isActiveAtFrame(frame)) {

                int[] delta = keyframes.path.getDeltaAtFrame(frame, sprite.baseLocH, sprite.baseLocV);
                if (delta != null) {
                    logger.debug("PATH TWEEN: sprite {} frame {} - base: ({},{}), delta: ({},{})",
                        spriteNum, frame, sprite.baseLocH, sprite.baseLocV, delta[0], delta[1]);

                    sprite.locH = sprite.baseLocH + delta[0];
                    sprite.locV = sprite.baseLocV + delta[1];
                }
            }

            // Size tween (additive)
            if (keyframes.size != null &&
                keyframes.size.tweenInfo != null &&
                keyframes.size.tweenInfo.isSizeTweened() &&
                keyframes.size.isActiveAtFrame(frame)) {

                int[] delta = keyframes.size.getDeltaAtFrame(frame, sprite.baseWidth, sprite.baseHeight);
                if (delta != null) {
                    logger.debug("SIZE TWEEN: sprite {} frame {} - base: {}x{}, delta: ({},{})",
                        spriteNum, frame, sprite.baseWidth, sprite.baseHeight, delta[0], delta[1]);

                    sprite.width = sprite.baseWidth + delta[0];
                    sprite.height = sprite.baseHeight + delta[1];
                    sprite.setHasSizeChanged(true);

                    if (!sprite.hasSizeTweened()) {
                        sprite.setHasSizeTweened(true);
                    }
                }
            } else if (sprite.hasSizeTweened()) {
                sprite.setHasSizeTweened(false);
            }

            // Rotation tween (additive)
            if (keyframes.rotation != null &&
                keyframes.rotation.tweenInfo != null &&
                keyframes.rotation.tweenInfo.isRotationTweened() &&
                keyframes.rotation.isActiveAtFrame(frame)) {

                Double delta = keyframes.rotation.getDeltaAtFrame(frame, sprite.baseRotation);
                if (delta != null) {
                    logger.debug("ROTATION TWEEN: sprite {} frame {} - base: {}, delta: {}",
                        spriteNum, frame, sprite.baseRotation, delta);

                    sprite.setRotation((float) (sprite.baseRotation + delta));
                }
            }

            // Blend tween (absolute)
            if (keyframes.blend != null &&
                keyframes.blend.tweenInfo != null &&
                keyframes.blend.tweenInfo.isBlendTweened() &&
                keyframes.blend.isActiveAtFrame(frame)) {

                Integer value = keyframes.blend.getBlendAtFrame(frame);
                if (value != null) {
                    logger.debug("BLEND TWEEN: sprite {} frame {} - old: {}, new: {}",
                        spriteNum, frame, sprite.blend, value);

                    sprite.blend = value;
                }
            }

            // Skew tween (additive)
            if (keyframes.skew != null &&
                keyframes.skew.tweenInfo != null &&
                keyframes.skew.tweenInfo.isSkewTweened() &&
                keyframes.skew.isActiveAtFrame(frame)) {

                Double delta = keyframes.skew.getDeltaAtFrame(frame, sprite.baseSkew);
                if (delta != null) {
                    logger.debug("SKEW TWEEN: sprite {} frame {} - base: {}, delta: {}",
                        spriteNum, frame, sprite.baseSkew, delta);

                    sprite.skew = (int) (sprite.baseSkew + delta);
                }
            }

            // Foreground color tween (absolute)
            if (keyframes.foreColor != null &&
                keyframes.foreColor.tweenInfo != null &&
                keyframes.foreColor.tweenInfo.isForecolorTweened() &&
                keyframes.foreColor.isActiveAtFrame(frame)) {

                ColorRef color = keyframes.foreColor.getColorAtFrame(frame);
                if (color != null) {
                    logger.debug("FORECOLOR TWEEN: sprite {} frame {} - color: {}",
                        spriteNum, frame, color);

                    sprite.setColor(color);
                    sprite.hasForeColor = true;
                }
            }

            // Background color tween (absolute)
            if (keyframes.backColor != null &&
                keyframes.backColor.tweenInfo != null &&
                keyframes.backColor.tweenInfo.isBackcolorTweened() &&
                keyframes.backColor.isActiveAtFrame(frame)) {

                ColorRef color = keyframes.backColor.getColorAtFrame(frame);
                if (color != null) {
                    logger.debug("BACKCOLOR TWEEN: sprite {} frame {} - color: {}",
                        spriteNum, frame, color);

                    sprite.setBgColor(color);
                    sprite.hasBackColor = true;
                }
            }
        }
    }

    /**
     * End sprites for a frame - mark sprites that are exiting.
     * Returns the list of channel numbers that ended.
     */
    public List<Integer> endSprites(ScoreRef scoreRef, int prevFrame, int nextFrame) {
        List<Integer> channelsToEnd = spriteSpans.stream()
            .filter(span -> isSpanInFrame(span, prevFrame) && !isSpanInFrame(span, nextFrame))
            .map(span -> span.channelNumber)
            .collect(Collectors.toList());

        // Mark sprites as exited
        for (int channelNum : channelsToEnd) {
            Sprite sprite = getSprite((short) channelNum);
            if (sprite != null) {
                sprite.exited = true;
            }
        }

        return channelsToEnd;
    }

    /**
     * Get the channel count (excluding channel 0).
     */
    public int getChannelCount() {
        return Math.max(0, channels.size() - 1);
    }

    /**
     * Set the channel count.
     */
    public void setChannelCount(int newCount) {
        if (newCount > channels.size()) {
            int baseNumber = channels.size();
            int addCount = newCount - channels.size();
            for (int i = 0; i < addCount; i++) {
                channels.add(new SpriteChannel(baseNumber + i));
            }
        } else if (newCount < channels.size()) {
            while (channels.size() > newCount) {
                channels.remove(channels.size() - 1);
            }
        }
    }

    /**
     * Get a sprite by channel number (read-only).
     */
    public Sprite getSprite(short number) {
        if (number < 0 || number >= channels.size()) {
            return null;
        }
        return channels.get(number).sprite;
    }

    /**
     * Get a sprite by channel number (mutable).
     */
    public Sprite getSpriteMut(short number) {
        if (number < 0 || number >= channels.size()) {
            return null;
        }
        return channels.get(number).sprite;
    }

    /**
     * Get a channel by number.
     */
    public SpriteChannel getChannel(short number) {
        if (number < 0 || number >= channels.size()) {
            return null;
        }
        return channels.get(number);
    }

    /**
     * Get sorted channels for rendering (by z-order).
     */
    public List<SpriteChannel> getSortedChannels(int frameNum) {
        // Build set of active channel numbers for the frame
        Set<Integer> activeChannels = spriteSpans.stream()
            .filter(span -> isSpanInFrame(span, frameNum))
            .map(span -> span.channelNumber)
            .collect(Collectors.toSet());

        return channels.stream()
            .filter(ch -> {
                // Skip channel 0 (frame behaviors)
                if (ch.number == 0) {
                    return false;
                }
                // Render if in active span or is puppeted
                boolean isActive = activeChannels.contains(ch.number) || ch.sprite.puppet;
                if (!isActive) {
                    return false;
                }
                // Must have a valid member and be visible
                return ch.sprite.memberRef != null &&
                       ch.sprite.memberRef.isValid() &&
                       ch.sprite.visible;
            })
            .sorted((a, b) -> {
                int zCompare = Integer.compare(a.sprite.locZ, b.sprite.locZ);
                if (zCompare != 0) {
                    return zCompare;
                }
                return Integer.compare(a.number, b.number);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get all active script instance refs from sprites.
     */
    public List<Integer> getActiveScriptInstanceList() {
        List<Integer> instanceList = new ArrayList<>();
        for (SpriteChannel channel : channels) {
            instanceList.addAll(channel.sprite.scriptInstanceList);
        }
        return instanceList;
    }

    /**
     * Get the tempo for a frame.
     */
    public Integer getFrameTempo(int frame) {
        // Search backwards for most recent tempo change at or before the frame
        TempoChannelEntry found = null;
        for (int i = tempoChannelData.size() - 1; i >= 0; i--) {
            TempoChannelEntry entry = tempoChannelData.get(i);
            if (entry.frameIndex <= frame) {
                found = entry;
                break;
            }
        }
        if (found != null) {
            return found.data.tempo;
        }
        return null;
    }

    /**
     * Load score data from a ScoreChunk.
     */
    public void loadFromScoreChunk(ScoreChunk scoreChunk) {
        setChannelCount(scoreChunk.frameData.header.numChannels);

        // Copy initialization data
        channelInitializationData = new ArrayList<>(scoreChunk.frameData.frameChannelData);
        soundChannelData = new ArrayList<>(scoreChunk.frameData.soundChannelData);
        tempoChannelData = new ArrayList<>(scoreChunk.frameData.tempoChannelData);

        // Build keyframes cache
        keyframesCache = buildAllKeyframesCache(
            scoreChunk.frameData.frameChannelData,
            scoreChunk.frameIntervals
        );

        // Process frame intervals to create sprite spans
        for (FrameIntervalPair pair : scoreChunk.frameIntervals) {
            FrameIntervalPrimary primary = pair.primary;
            FrameIntervalSecondary secondary = pair.secondary;

            boolean isFrameScriptOrSpriteScript = primary.channelIndex == 0 || primary.channelIndex > 5;
            if (isFrameScriptOrSpriteScript) {
                ScoreSpriteSpan span = new ScoreSpriteSpan();
                span.channelNumber = KeyframeUtils.getChannelNumberFromIndex(primary.channelIndex);
                span.startFrame = primary.startFrame;
                span.endFrame = primary.endFrame;

                if (secondary != null) {
                    ScoreBehaviorReference ref = new ScoreBehaviorReference();
                    ref.castLib = secondary.castLib;
                    ref.castMember = secondary.castMember;
                    ref.parameter = secondary.parameter;
                    span.scripts.add(ref);
                }

                spriteSpans.add(span);
            }
        }

        // Generate sprite spans from channel data if intervals are empty (filmloops)
        if (spriteSpans.isEmpty() && !channelInitializationData.isEmpty()) {
            generateSpriteSpansFromChannelData();
        }

        // Copy sprite details
        spriteDetails = new HashMap<>(scoreChunk.spriteDetails);

        logger.debug("Loaded score: {} channels, {} spans, {} keyframe channels",
            channels.size(), spriteSpans.size(), keyframesCache.size());
    }

    /**
     * Generate sprite spans from channel initialization data.
     * Used for filmloops which don't have frame intervals.
     */
    private void generateSpriteSpansFromChannelData() {
        // Group by channel: find min/max frame for each channel
        Map<Integer, int[]> channelFrames = new HashMap<>();

        for (FrameChannelEntry entry : channelInitializationData) {
            // Skip effect channels (0-5)
            if (entry.channelIndex < 6) {
                continue;
            }
            // Skip empty sprites
            if (entry.data.castMember == 0) {
                continue;
            }

            int channelNumber = KeyframeUtils.getChannelNumberFromIndex(entry.channelIndex);
            int frameNum = entry.frameIndex + 1;

            int[] minMax = channelFrames.computeIfAbsent(channelNumber, k -> new int[] { frameNum, frameNum });
            if (frameNum < minMax[0]) minMax[0] = frameNum;
            if (frameNum > minMax[1]) minMax[1] = frameNum;
        }

        // Create sprite spans
        for (Map.Entry<Integer, int[]> entry : channelFrames.entrySet()) {
            ScoreSpriteSpan span = new ScoreSpriteSpan();
            span.channelNumber = entry.getKey();
            span.startFrame = entry.getValue()[0];
            span.endFrame = entry.getValue()[1];
            spriteSpans.add(span);
        }

        logger.debug("Generated {} sprite spans from channel data", spriteSpans.size());
    }

    /**
     * Reset the score (clear puppeted sprites, etc.).
     */
    public void reset() {
        for (SpriteChannel channel : channels) {
            // Clear script instances for all sprites
            channel.sprite.scriptInstanceList.clear();

            if (channel.sprite.puppet) {
                resetSprite(channel.sprite);
            }
        }
    }

    /**
     * Reset a sprite to default state.
     */
    private void resetSprite(Sprite sprite) {
        sprite.puppet = false;
        sprite.visible = true;
        sprite.entered = false;
        sprite.exited = false;
        sprite.memberRef = new CastMemberRef();
        sprite.locH = 0;
        sprite.locV = 0;
        sprite.width = 0;
        sprite.height = 0;
        sprite.setHasSizeChanged(false);
        sprite.setHasSizeTweened(false);
    }

    /**
     * Build combined keyframes cache for all channels.
     */
    private Map<Integer, ChannelKeyframes> buildAllKeyframesCache(
            List<FrameChannelEntry> frameChannelData,
            List<FrameIntervalPair> frameIntervals) {

        logger.debug("Building keyframes cache from {} frame entries and {} intervals",
            frameChannelData.size(), frameIntervals.size());

        // Build map of channel -> list of intervals
        Map<Integer, List<FrameIntervalPrimary>> intervalsByChannel = new HashMap<>();
        for (FrameIntervalPair pair : frameIntervals) {
            int channelNum = KeyframeUtils.indexToChannelNumber(pair.primary.channelIndex);
            intervalsByChannel.computeIfAbsent(channelNum, k -> new ArrayList<>()).add(pair.primary);
        }

        // Get unique channel indices
        Set<Integer> channelIndices = frameChannelData.stream()
            .map(e -> e.channelIndex)
            .collect(Collectors.toSet());

        Map<Integer, ChannelKeyframes> combinedCache = new HashMap<>();

        for (int channelIndex : channelIndices) {
            int channelNum = KeyframeUtils.indexToChannelNumber(channelIndex);
            List<FrameIntervalPrimary> intervals = intervalsByChannel.get(channelNum);

            if (intervals == null || intervals.isEmpty()) {
                continue;
            }

            // Get tween info from first interval
            TweenInfo tweenInfo = intervals.get(0).tweenInfo;
            if (tweenInfo == null) {
                continue;
            }

            // Filter frame data for this channel
            List<FrameChannelEntry> channelData = frameChannelData.stream()
                .filter(e -> e.channelIndex == channelIndex)
                .collect(Collectors.toList());

            if (channelData.isEmpty()) {
                continue;
            }

            ChannelKeyframes keyframes = new ChannelKeyframes(channelNum);

            // Build path keyframes
            if (tweenInfo.isPathTweened()) {
                SpritePathKeyframes pathKf = buildPathKeyframes(channelIndex, channelData, intervals);
                if (!pathKf.keyframes.isEmpty()) {
                    keyframes.path = pathKf;
                }
            }

            // Build size keyframes
            if (tweenInfo.isSizeTweened()) {
                SpriteSizeKeyframes sizeKf = buildSizeKeyframes(channelIndex, channelData, intervals);
                if (!sizeKf.keyframes.isEmpty()) {
                    keyframes.size = sizeKf;
                }
            }

            // Build rotation keyframes
            if (tweenInfo.isRotationTweened()) {
                SpriteRotationKeyframes rotKf = buildRotationKeyframes(channelIndex, channelData, intervals);
                if (!rotKf.keyframes.isEmpty()) {
                    keyframes.rotation = rotKf;
                }
            }

            // Build blend keyframes
            if (tweenInfo.isBlendTweened()) {
                SpriteBlendKeyframes blendKf = buildBlendKeyframes(channelIndex, channelData, intervals);
                if (!blendKf.keyframes.isEmpty()) {
                    keyframes.blend = blendKf;
                }
            }

            // Build skew keyframes
            if (tweenInfo.isSkewTweened()) {
                SpriteSkewKeyframes skewKf = buildSkewKeyframes(channelIndex, channelData, intervals);
                if (!skewKf.keyframes.isEmpty()) {
                    keyframes.skew = skewKf;
                }
            }

            // Build forecolor keyframes
            if (tweenInfo.isForecolorTweened()) {
                SpriteForeColorKeyframes foreKf = buildForeColorKeyframes(channelIndex, channelData, intervals);
                if (!foreKf.keyframes.isEmpty()) {
                    keyframes.foreColor = foreKf;
                }
            }

            // Build backcolor keyframes
            if (tweenInfo.isBackcolorTweened()) {
                SpriteBackColorKeyframes backKf = buildBackColorKeyframes(channelIndex, channelData, intervals);
                if (!backKf.keyframes.isEmpty()) {
                    keyframes.backColor = backKf;
                }
            }

            if (keyframes.hasKeyframes()) {
                combinedCache.put(channelNum, keyframes);
            }
        }

        logger.debug("Built keyframes cache for {} channels", combinedCache.size());
        return combinedCache;
    }

    private SpritePathKeyframes buildPathKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpritePathKeyframes keyframes = new SpritePathKeyframes(channelIndex, tweenInfo);

        // Store intervals
        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        // Collect keyframes from all intervals
        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isPathTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    keyframes.keyframes.add(new PathKeyframe(
                        directorFrame,
                        entry.data.posX,
                        entry.data.posY
                    ));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteSizeKeyframes buildSizeKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteSizeKeyframes keyframes = new SpriteSizeKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isSizeTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    keyframes.keyframes.add(new SizeKeyframe(
                        directorFrame,
                        entry.data.width,
                        entry.data.height
                    ));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteRotationKeyframes buildRotationKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteRotationKeyframes keyframes = new SpriteRotationKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isRotationTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    keyframes.keyframes.add(new RotationKeyframe(
                        directorFrame,
                        entry.data.rotation
                    ));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteBlendKeyframes buildBlendKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteBlendKeyframes keyframes = new SpriteBlendKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isBlendTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    int blendPercent = KeyframeUtils.convertBlendToPercentage(entry.data.blend);
                    keyframes.keyframes.add(new BlendKeyframe(directorFrame, blendPercent));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteSkewKeyframes buildSkewKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteSkewKeyframes keyframes = new SpriteSkewKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isSkewTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    keyframes.keyframes.add(new SkewKeyframe(directorFrame, entry.data.skew));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteForeColorKeyframes buildForeColorKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteForeColorKeyframes keyframes = new SpriteForeColorKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isForecolorTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    ColorRef color;
                    if (entry.data.colorFlag == 1 || entry.data.colorFlag == 3) {
                        color = ColorRef.rgb(entry.data.foreColor, entry.data.foreColorG, entry.data.foreColorB);
                    } else {
                        color = ColorRef.paletteIndex(entry.data.foreColor);
                    }
                    keyframes.keyframes.add(new ColorKeyframe(directorFrame, color));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    private SpriteBackColorKeyframes buildBackColorKeyframes(int channelIndex,
            List<FrameChannelEntry> channelData, List<FrameIntervalPrimary> intervals) {
        TweenInfo tweenInfo = intervals.get(0).tweenInfo;
        SpriteBackColorKeyframes keyframes = new SpriteBackColorKeyframes(channelIndex, tweenInfo);

        for (FrameIntervalPrimary interval : intervals) {
            keyframes.intervals.add(new int[] { interval.startFrame, interval.endFrame });
        }

        for (FrameIntervalPrimary interval : intervals) {
            if (interval.startFrame == interval.endFrame) continue;
            if (!interval.tweenInfo.isBackcolorTweened()) continue;

            for (FrameChannelEntry entry : channelData) {
                int directorFrame = entry.frameIndex + 1;
                if (directorFrame >= interval.startFrame && directorFrame <= interval.endFrame) {
                    ColorRef color;
                    if (entry.data.colorFlag == 2 || entry.data.colorFlag == 3) {
                        color = ColorRef.rgb(entry.data.backColor, entry.data.backColorG, entry.data.backColorB);
                    } else {
                        color = ColorRef.paletteIndex(entry.data.backColor);
                    }
                    keyframes.keyframes.add(new ColorKeyframe(directorFrame, color));
                }
            }
        }

        keyframes.keyframes.sort(Comparator.comparingInt(k -> k.frame));
        return keyframes;
    }

    /**
     * Helper class to hold span initialization data.
     */
    private static class SpanInitData {
        final ScoreSpriteSpan span;
        final int channelIndex;
        final ScoreFrameChannelData data;

        SpanInitData(ScoreSpriteSpan span, int channelIndex, ScoreFrameChannelData data) {
            this.span = span;
            this.channelIndex = channelIndex;
            this.data = data;
        }
    }

    // ========== Legacy compatibility methods ==========
    // These maintain backwards compatibility with the original simple API

    /**
     * Get frame by number (1-indexed).
     * @deprecated Use getSprite() and channel data instead
     */
    @Deprecated
    public Frame getFrame(int frameNum) {
        // Create a synthetic frame from current state
        Frame frame = new Frame(frameNum);
        // Legacy API - frames are not stored separately anymore
        return frame;
    }

    /**
     * Get frame label by name.
     */
    public Integer getFrameByLabel(String label) {
        for (FrameLabel fl : frameLabels) {
            if (fl.label.equals(label)) {
                return fl.frameNum;
            }
        }
        return null;
    }

    /**
     * Legacy Frame class for backwards compatibility.
     * @deprecated Use Score directly for frame data
     */
    @Deprecated
    public static class Frame {
        public int frameNum;
        public List<FrameSprite> sprites;
        public int tempo;
        public int transitionId;
        public int soundChannel1;
        public int soundChannel2;
        public int scriptId;
        public int palette;

        public Frame(int frameNum) {
            this.frameNum = frameNum;
            this.sprites = new ArrayList<>();
            this.tempo = 30;
            this.transitionId = 0;
            this.soundChannel1 = 0;
            this.soundChannel2 = 0;
            this.scriptId = 0;
            this.palette = 0;
        }
    }

    /**
     * Legacy FrameSprite class for backwards compatibility.
     * @deprecated Use Sprite class directly
     */
    @Deprecated
    public static class FrameSprite {
        public int channelNum;
        public int memberRef;
        public int locH;
        public int locV;
        public int width;
        public int height;
        public int ink;
        public int blend;
        public boolean visible;
        public int foreColor;
        public int backColor;

        public FrameSprite() {
            this.channelNum = 0;
            this.memberRef = 0;
            this.locH = 0;
            this.locV = 0;
            this.width = 0;
            this.height = 0;
            this.ink = 0;
            this.blend = 100;
            this.visible = true;
            this.foreColor = 255;
            this.backColor = 0;
        }
    }

    /**
     * Legacy Channel class - use SpriteChannel instead.
     * @deprecated Use SpriteChannel class
     */
    @Deprecated
    public static class Channel {
        public int number;
        public String name;
        public Sprite sprite;
        public boolean isPuppet;

        public Channel(int number) {
            this.number = number;
            this.name = "";
            this.sprite = new Sprite(number);
            this.isPuppet = false;
        }
    }
}
