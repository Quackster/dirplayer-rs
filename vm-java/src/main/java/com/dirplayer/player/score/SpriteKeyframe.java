package com.dirplayer.player.score;

import com.dirplayer.player.ColorRef;

/**
 * Base class for keyframe data in sprite animations.
 * Port of Rust keyframe structs from score_keyframes.rs.
 */
public abstract class SpriteKeyframe {
    public int frame;

    public SpriteKeyframe(int frame) {
        this.frame = frame;
    }

    public int getFrame() {
        return frame;
    }

    /**
     * Blend keyframe - stores blend percentage at a frame.
     */
    public static class BlendKeyframe extends SpriteKeyframe {
        public int blendPercent;

        public BlendKeyframe(int frame, int blendPercent) {
            super(frame);
            this.blendPercent = blendPercent;
        }
    }

    /**
     * Rotation keyframe - stores rotation angle at a frame.
     */
    public static class RotationKeyframe extends SpriteKeyframe {
        public double rotation;

        public RotationKeyframe(int frame, double rotation) {
            super(frame);
            this.rotation = rotation;
        }
    }

    /**
     * Skew keyframe - stores skew angle at a frame.
     */
    public static class SkewKeyframe extends SpriteKeyframe {
        public double skew;

        public SkewKeyframe(int frame, double skew) {
            super(frame);
            this.skew = skew;
        }
    }

    /**
     * Path keyframe - stores position (x, y) at a frame.
     */
    public static class PathKeyframe extends SpriteKeyframe {
        public short x;
        public short y;

        public PathKeyframe(int frame, short x, short y) {
            super(frame);
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Size keyframe - stores width and height at a frame.
     */
    public static class SizeKeyframe extends SpriteKeyframe {
        public int width;
        public int height;

        public SizeKeyframe(int frame, int width, int height) {
            super(frame);
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Color keyframe - stores color reference at a frame.
     */
    public static class ColorKeyframe extends SpriteKeyframe {
        public ColorRef color;

        public ColorKeyframe(int frame, ColorRef color) {
            super(frame);
            this.color = color;
        }
    }
}
