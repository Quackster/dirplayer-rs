package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Frame labels chunk - maps frame numbers to label names.
 * Port of Rust FrameLabelsChunk struct.
 */
public class FrameLabelsChunk {
    public List<FrameLabel> labels;

    public FrameLabelsChunk() {
        this.labels = new ArrayList<>();
    }

    public static FrameLabelsChunk fromReader(BinaryReader reader, int dirVersion) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        FrameLabelsChunk chunk = new FrameLabelsChunk();

        int labelsCount = reader.readU16();

        // Read frame numbers and offsets
        List<int[]> labelFrames = new ArrayList<>();
        for (int i = 0; i < labelsCount; i++) {
            int frameNum = reader.readU16();
            int labelOffset = reader.readU16();
            labelFrames.add(new int[] { labelOffset, frameNum });
        }

        int labelsSize = (int) reader.readU32();

        // Read label strings
        for (int i = 0; i < labelsCount; i++) {
            int labelOffset = labelFrames.get(i)[0];
            int frameNum = labelFrames.get(i)[1];

            int labelLen;
            if (i < labelsCount - 1) {
                labelLen = labelFrames.get(i + 1)[0] - labelOffset;
            } else {
                labelLen = labelsSize - labelOffset;
            }

            String labelStr = reader.readString(labelLen);

            FrameLabel label = new FrameLabel();
            label.frameNum = frameNum;
            label.label = labelStr;
            chunk.labels.add(label);
        }

        return chunk;
    }

    public static class FrameLabel {
        public int frameNum;
        public String label;
    }
}
