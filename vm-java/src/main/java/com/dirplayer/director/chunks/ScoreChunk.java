package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.dirplayer.SimpleLogger;


/**
 * Score chunk - contains timeline/score data with sprite positions and behaviors.
 * Port of Rust ScoreChunk struct.
 */
public class ScoreChunk {
    private static final SimpleLogger logger = SimpleLogger.getLogger(ScoreChunk.class);

    public ScoreChunkHeader header;
    public List<byte[]> entries;
    public List<FrameIntervalPair> frameIntervals;
    public ScoreFrameData frameData;
    public Map<Integer, SpriteDetailInfo> spriteDetails;

    public ScoreChunk() {
        this.header = new ScoreChunkHeader();
        this.entries = new ArrayList<>();
        this.frameIntervals = new ArrayList<>();
        this.frameData = new ScoreFrameData();
        this.spriteDetails = new HashMap<>();
    }

    public static ScoreChunk read(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        ScoreChunk chunk = new ScoreChunk();
        chunk.header = ScoreChunkHeader.read(reader);

        // Read offsets table
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i <= chunk.header.entryCount; i++) {
            offsets.add((int) reader.readU32());
        }

        // Validate offsets
        if (offsets.size() != chunk.header.entryCount + 1) {
            logger.error("Offsets count mismatch! Expected {}, Got {}",
                chunk.header.entryCount + 1, offsets.size());
            return chunk;
        }

        // Read all entries
        for (int index = 0; index < chunk.header.entryCount; index++) {
            int currentOffset = offsets.get(index);
            int nextOffset = offsets.get(index + 1);
            int length = nextOffset - currentOffset;

            if (length > 0) {
                chunk.entries.add(reader.readBytes(length));
            } else {
                chunk.entries.add(new byte[0]);
            }
        }

        // Process frame data from first entry
        if (!chunk.entries.isEmpty() && chunk.entries.get(0).length > 0) {
            BinaryReader deltaReader = new BinaryReader(chunk.entries.get(0));
            deltaReader.setEndian(ByteOrder.BIG_ENDIAN);
            chunk.frameData = ScoreFrameData.read(deltaReader);
        }

        // Analyze behavior attachment entries
        chunk.frameIntervals = analyzeFrameIntervals(chunk.entries);

        // Parse sprite details
        chunk.spriteDetails = parseSpriteDetails(chunk.entries);

        return chunk;
    }

    private static List<FrameIntervalPair> analyzeFrameIntervals(List<byte[]> entries) {
        List<FrameIntervalPair> results = new ArrayList<>();
        int i = 2; // Start at 2, skip entries 0 and 1

        while (i < entries.size()) {
            byte[] entryBytes = entries.get(i);

            if (entryBytes.length == 0) {
                i++;
                continue;
            }

            if (entryBytes.length == 44 || entryBytes.length == 48) {
                BinaryReader reader = new BinaryReader(entryBytes);
                reader.setEndian(ByteOrder.BIG_ENDIAN);

                FrameIntervalPrimary primary = FrameIntervalPrimary.read(reader);
                if (primary != null) {
                    logger.debug("Found primary at entry {}: channel={}, frames={}-{}",
                        i, primary.channelIndex, primary.startFrame, primary.endFrame);

                    // Collect secondary entries (behaviors)
                    List<FrameIntervalSecondary> secondaries = new ArrayList<>();
                    int j = i + 1;

                    while (j < entries.size()) {
                        int nextSize = entries.get(j).length;

                        if (nextSize >= 8 && nextSize % 8 == 0) {
                            int behaviorCount = nextSize / 8;
                            BinaryReader secReader = new BinaryReader(entries.get(j));
                            secReader.setEndian(ByteOrder.BIG_ENDIAN);

                            boolean foundValidBehavior = false;
                            for (int k = 0; k < behaviorCount; k++) {
                                int castLib = secReader.readU16();
                                int castMember = secReader.readU16();
                                int unk0 = (int) secReader.readU32();

                                if (castLib > 0 && castMember > 0) {
                                    FrameIntervalSecondary secondary = new FrameIntervalSecondary();
                                    secondary.castLib = castLib;
                                    secondary.castMember = castMember;
                                    secondary.unk0 = unk0;
                                    secondaries.add(secondary);
                                    foundValidBehavior = true;
                                }
                            }

                            if (foundValidBehavior) {
                                j++;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    // Create result entries
                    if (secondaries.isEmpty()) {
                        results.add(new FrameIntervalPair(primary, null));
                    } else {
                        for (FrameIntervalSecondary secondary : secondaries) {
                            results.add(new FrameIntervalPair(primary.clone(), secondary));
                        }
                    }

                    i = j;
                    continue;
                }
            }

            i++;
        }

        return results;
    }

    private static Map<Integer, SpriteDetailInfo> parseSpriteDetails(List<byte[]> entries) {
        Map<Integer, SpriteDetailInfo> details = new HashMap<>();

        if (entries.isEmpty() || entries.get(0).length < 12) {
            return details;
        }

        byte[] entry0 = entries.get(0);
        BinaryReader reader = new BinaryReader(entry0);
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        int framesStreamSize = (int) reader.readU32();
        int version = (int) reader.readU32();
        int listStart = (int) reader.readU32();

        if (listStart == 0 || listStart >= entry0.length || listStart + 12 > entry0.length) {
            return details;
        }

        reader.setPos(listStart);

        int numEntries = (int) reader.readU32();
        int listSize = (int) reader.readU32();
        int maxDataLen = (int) reader.readU32();

        if (numEntries == 0 || numEntries > 100000) {
            return details;
        }

        int indexStart = listStart + 12;
        int frameDataOffset = indexStart + listSize * 4;

        // Read all absolute offsets
        List<Integer> absoluteOffsets = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            int relativeOff = (int) reader.readU32();
            absoluteOffsets.add(frameDataOffset + relativeOff);
        }

        // Parse behaviors
        for (int spriteListIdx = 0; spriteListIdx < numEntries - 1; spriteListIdx++) {
            int behaviorStreamIdx = spriteListIdx + 1;
            if (behaviorStreamIdx >= absoluteOffsets.size()) {
                continue;
            }

            int behaviorStart = absoluteOffsets.get(behaviorStreamIdx);
            int behaviorEnd = (behaviorStreamIdx + 1 < absoluteOffsets.size())
                ? absoluteOffsets.get(behaviorStreamIdx + 1)
                : entry0.length;

            if (behaviorStart >= entry0.length || behaviorStart >= behaviorEnd) {
                continue;
            }

            int behaviorSize = behaviorEnd - behaviorStart;
            if (behaviorSize < 8) {
                continue;
            }

            byte[] behaviorData = new byte[behaviorSize];
            System.arraycopy(entry0, behaviorStart, behaviorData, 0, behaviorSize);
            BinaryReader behaviorReader = new BinaryReader(behaviorData);
            behaviorReader.setEndian(ByteOrder.BIG_ENDIAN);

            SpriteDetailInfo info = new SpriteDetailInfo();

            while (behaviorReader.getPos() + 8 <= behaviorSize) {
                int castLib = behaviorReader.readU16();
                int castMember = behaviorReader.readU16();
                int initializerIdx = (int) behaviorReader.readU32();

                if (castLib == 0 && castMember == 0) {
                    break;
                }

                if (castMember > 0 && castMember < 10000) {
                    SpriteBehavior behavior = new SpriteBehavior();
                    behavior.castLib = castLib;
                    behavior.castMember = castMember;
                    info.behaviors.add(behavior);
                }
            }

            if (!info.behaviors.isEmpty()) {
                details.put(spriteListIdx, info);
            }
        }

        return details;
    }

    // Inner classes

    public static class ScoreChunkHeader {
        public int totalLength;
        public int unk1;
        public int unk2;
        public int entryCount;
        public int unk3;
        public int entrySizeSum;

        public static ScoreChunkHeader read(BinaryReader reader) {
            ScoreChunkHeader header = new ScoreChunkHeader();
            header.totalLength = (int) reader.readU32();
            header.unk1 = (int) reader.readU32();
            header.unk2 = (int) reader.readU32();
            header.entryCount = (int) reader.readU32();
            header.unk3 = (int) reader.readU32();
            header.entrySizeSum = (int) reader.readU32();
            return header;
        }
    }

    public static class FrameIntervalPair {
        public FrameIntervalPrimary primary;
        public FrameIntervalSecondary secondary;

        public FrameIntervalPair(FrameIntervalPrimary primary, FrameIntervalSecondary secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    public static class FrameIntervalPrimary implements Cloneable {
        public int startFrame;
        public int endFrame;
        public int xtraInfo;
        public int spriteFlags;
        public int channelIndex;
        public TweenInfo tweenInfo;

        public static FrameIntervalPrimary read(BinaryReader reader) {
            FrameIntervalPrimary primary = new FrameIntervalPrimary();
            primary.startFrame = (int) reader.readU32();
            primary.endFrame = (int) reader.readU32();
            primary.xtraInfo = (int) reader.readU32();
            primary.spriteFlags = (int) reader.readU32();
            primary.channelIndex = (int) reader.readU32();
            primary.tweenInfo = TweenInfo.read(reader);
            return primary;
        }

        public FrameIntervalPrimary clone() {
            FrameIntervalPrimary copy = new FrameIntervalPrimary();
            copy.startFrame = this.startFrame;
            copy.endFrame = this.endFrame;
            copy.xtraInfo = this.xtraInfo;
            copy.spriteFlags = this.spriteFlags;
            copy.channelIndex = this.channelIndex;
            copy.tweenInfo = this.tweenInfo;
            return copy;
        }
    }

    public static class FrameIntervalSecondary {
        public int castLib;
        public int castMember;
        public int unk0;
        public List<Object> parameter = new ArrayList<>();
    }

    public static class TweenInfo {
        public int curvature;
        public int flags;
        public int easeIn;
        public int easeOut;
        public int padding;

        public static TweenInfo read(BinaryReader reader) {
            TweenInfo info = new TweenInfo();
            info.curvature = (int) reader.readU32();
            info.flags = (int) reader.readU32();
            info.easeIn = (int) reader.readU32();
            info.easeOut = (int) reader.readU32();
            info.padding = (int) reader.readU32();
            return info;
        }

        public boolean isPathTweened() { return (flags & 0x00000004) != 0; }
        public boolean isSizeTweened() { return (flags & 0x00000008) != 0; }
        public boolean isForecolorTweened() { return (flags & 0x00000010) != 0; }
        public boolean isBackcolorTweened() { return (flags & 0x00000020) != 0; }
        public boolean isBlendTweened() { return (flags & 0x00000040) != 0; }
        public boolean isRotationTweened() { return (flags & 0x00000080) != 0; }
        public boolean isSkewTweened() { return (flags & 0x00000100) != 0; }
        public boolean isContinuous() { return (flags & 0x00000002) != 0; }
        public boolean isSmoothSpeed() { return (flags & 0x00000400) != 0; }
    }

    public static class SpriteBehavior {
        public int castLib;
        public int castMember;
    }

    public static class SpriteDetailInfo {
        public List<SpriteBehavior> behaviors = new ArrayList<>();
    }
}
