package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Score frame data - contains per-frame sprite channel data.
 * Port of Rust ScoreFrameData struct.
 */
public class ScoreFrameData {
    private static final Logger logger = LoggerFactory.getLogger(ScoreFrameData.class);

    public ScoreFrameDataHeader header;
    public byte[] decompressedData;
    public List<FrameChannelEntry> frameChannelData;
    public List<SoundChannelEntry> soundChannelData;
    public List<TempoChannelEntry> tempoChannelData;

    public ScoreFrameData() {
        this.header = new ScoreFrameDataHeader();
        this.decompressedData = new byte[0];
        this.frameChannelData = new ArrayList<>();
        this.soundChannelData = new ArrayList<>();
        this.tempoChannelData = new ArrayList<>();
    }

    public static ScoreFrameData read(BinaryReader reader) {
        ScoreFrameData data = new ScoreFrameData();
        data.header = ScoreFrameDataHeader.read(reader);

        logger.debug("ScoreFrameData {} {} {}",
            data.header.frameCount, data.header.numChannels, data.header.spriteRecordSize);

        int channelDataSize = data.header.frameCount * data.header.numChannels * data.header.spriteRecordSize;
        byte[] channelData = new byte[channelDataSize];

        int frameIndex = 0;
        while (!reader.eof()) {
            int length = reader.readU16();
            if (length == 0) break;

            int frameLength = length - 2;
            if (frameLength > 0) {
                byte[] chunkData = reader.readBytes(frameLength);
                BinaryReader frameChunkReader = new BinaryReader(chunkData);
                frameChunkReader.setEndian(ByteOrder.BIG_ENDIAN);

                Set<Integer> channelsWithDeltas = new HashSet<>();

                while (!frameChunkReader.eof()) {
                    int channelSize = frameChunkReader.readU16();
                    int channelOffset = frameChunkReader.readU16();
                    byte[] channelDelta = frameChunkReader.readBytes(channelSize);

                    int frameOffset = frameIndex * data.header.numChannels * data.header.spriteRecordSize;
                    int endOffset = frameOffset + channelOffset + channelSize;

                    if (endOffset <= channelData.length) {
                        System.arraycopy(channelDelta, 0, channelData, frameOffset + channelOffset, channelSize);
                    }

                    // Mark affected channels
                    int firstChannel = channelOffset / data.header.spriteRecordSize;
                    int lastByte = channelOffset + channelSize - 1;
                    int lastChannel = lastByte / data.header.spriteRecordSize;
                    for (int ch = firstChannel; ch <= lastChannel; ch++) {
                        channelsWithDeltas.add(ch);
                    }
                }

                // Carry forward unchanged channels from previous frame
                if (frameIndex > 0) {
                    int prevFrameOffset = (frameIndex - 1) * data.header.numChannels * data.header.spriteRecordSize;
                    int currFrameOffset = frameIndex * data.header.numChannels * data.header.spriteRecordSize;

                    for (int ch = 0; ch < data.header.numChannels; ch++) {
                        if (!channelsWithDeltas.contains(ch)) {
                            int chOffset = ch * data.header.spriteRecordSize;
                            System.arraycopy(channelData, prevFrameOffset + chOffset,
                                channelData, currFrameOffset + chOffset, data.header.spriteRecordSize);
                        }
                    }
                }
            }
            frameIndex++;
        }

        data.decompressedData = channelData;

        // Parse channel data
        BinaryReader channelReader = new BinaryReader(channelData);
        channelReader.setEndian(ByteOrder.BIG_ENDIAN);

        for (int fi = 0; fi < data.header.frameCount; fi++) {
            for (int ci = 0; ci < data.header.numChannels; ci++) {
                int pos = channelReader.getPos();

                if (ci == 3 || ci == 4) {
                    // Sound channel
                    SoundChannelData snd = SoundChannelData.read(channelReader);
                    if (snd.castMember != 0) {
                        SoundChannelEntry entry = new SoundChannelEntry();
                        entry.frameIndex = fi;
                        entry.channelIndex = ci;
                        entry.data = snd;
                        data.soundChannelData.add(entry);
                    }
                } else if (ci == 5) {
                    // Tempo channel
                    TempoChannelData tempo = TempoChannelData.read(channelReader);
                    if (!tempo.isDefault() && !tempo.isEmpty()) {
                        TempoChannelEntry entry = new TempoChannelEntry();
                        entry.frameIndex = fi;
                        entry.data = tempo;
                        data.tempoChannelData.add(entry);
                    }
                } else {
                    // Sprite channel
                    ScoreFrameChannelData spriteData = ScoreFrameChannelData.read(channelReader);
                    if (spriteData.hasSpriteData()) {
                        FrameChannelEntry entry = new FrameChannelEntry();
                        entry.frameIndex = fi;
                        entry.channelIndex = ci;
                        entry.data = spriteData;
                        data.frameChannelData.add(entry);
                    }
                }

                channelReader.setPos(pos + data.header.spriteRecordSize);
            }
        }

        logger.debug("Finished processing {} frames. Sprites: {}, Sounds: {}, Tempo changes: {}",
            data.header.frameCount, data.frameChannelData.size(),
            data.soundChannelData.size(), data.tempoChannelData.size());

        return data;
    }

    // Inner classes

    public static class ScoreFrameDataHeader {
        public int frameCount;
        public int spriteRecordSize;
        public int numChannels;

        public static ScoreFrameDataHeader read(BinaryReader reader) {
            ScoreFrameDataHeader header = new ScoreFrameDataHeader();
            int actualLength = reader.readU32();
            int unk1 = reader.readU32();
            header.frameCount = reader.readU32();
            int framesVersion = reader.readU16();
            header.spriteRecordSize = reader.readU16();
            header.numChannels = reader.readU16();

            if (framesVersion > 13) {
                reader.readU16(); // numChannelsDisplayed
            } else {
                reader.readU16(); // skip
            }

            return header;
        }
    }

    public static class FrameChannelEntry {
        public int frameIndex;
        public int channelIndex;
        public ScoreFrameChannelData data;
    }

    public static class SoundChannelEntry {
        public int frameIndex;
        public int channelIndex;
        public SoundChannelData data;
    }

    public static class TempoChannelEntry {
        public int frameIndex;
        public TempoChannelData data;
    }
}
