package com.dirplayer.player.score;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sprite's span in the score timeline.
 * A span defines when a sprite is active (from start_frame to end_frame)
 * and what behaviors/scripts are attached to it.
 * Port of Rust ScoreSpriteSpan struct.
 */
public class ScoreSpriteSpan {
    public int channelNumber;
    public int startFrame;
    public int endFrame;
    public List<ScoreBehaviorReference> scripts;

    public ScoreSpriteSpan() {
        this.channelNumber = 0;
        this.startFrame = 0;
        this.endFrame = 0;
        this.scripts = new ArrayList<>();
    }

    public ScoreSpriteSpan(int channelNumber, int startFrame, int endFrame) {
        this.channelNumber = channelNumber;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.scripts = new ArrayList<>();
    }

    public ScoreSpriteSpan(int channelNumber, int startFrame, int endFrame, List<ScoreBehaviorReference> scripts) {
        this.channelNumber = channelNumber;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.scripts = scripts != null ? new ArrayList<>(scripts) : new ArrayList<>();
    }

    /**
     * Check if this span is active at the given frame.
     */
    public boolean isActiveAtFrame(int frame) {
        return frame >= startFrame && frame <= endFrame;
    }

    /**
     * Check if the given frame number falls within this span.
     */
    public static boolean isSpanInFrame(ScoreSpriteSpan span, int frameNum) {
        return span.startFrame <= frameNum && span.endFrame >= frameNum;
    }

    public ScoreSpriteSpan copy() {
        ScoreSpriteSpan copy = new ScoreSpriteSpan(channelNumber, startFrame, endFrame);
        for (ScoreBehaviorReference ref : scripts) {
            copy.scripts.add(ref.copy());
        }
        return copy;
    }

    @Override
    public String toString() {
        return "ScoreSpriteSpan{" +
            "channel=" + channelNumber +
            ", frames=" + startFrame + "-" + endFrame +
            ", scripts=" + scripts.size() +
            '}';
    }
}
