package com.dirplayer.player.score;

/**
 * Utility functions for keyframe processing.
 * Port of Rust score_keyframes.rs utility functions.
 */
public class KeyframeUtils {

    /**
     * Convert channel index (stored in score) to channel number (displayed in Director).
     */
    public static int indexToChannelNumber(int index) {
        if (index <= 5) {
            return index;
        } else {
            return index - 5;
        }
    }

    /**
     * Convert channel number to channel index.
     */
    public static int getChannelNumberFromIndex(int index) {
        if (index == 0) {
            return 0;
        }
        return index - 5;
    }

    /**
     * Convert raw blend value to percentage.
     */
    public static int convertBlendToPercentage(int rawBlend) {
        if (rawBlend == 0) {
            return 100;
        } else if (rawBlend == 255) {
            return 0;
        } else {
            return (int) ((255.0 - rawBlend) * 100.0 / 255.0);
        }
    }

    /**
     * Apply easing to interpolation parameter t.
     */
    public static double applyEasing(double t, int easeIn, int easeOut, boolean smoothSpeed) {
        if (!smoothSpeed) {
            return t;
        }

        double easeInPct = easeIn / 100.0;
        double easeOutPct = easeOut / 100.0;

        double totalEase = easeInPct + easeOutPct;
        double easeInNorm, easeOutNorm;
        if (totalEase > 1.0) {
            easeInNorm = easeInPct / totalEase;
            easeOutNorm = easeOutPct / totalEase;
        } else {
            easeInNorm = easeInPct;
            easeOutNorm = easeOutPct;
        }

        if (t < easeInNorm) {
            double localT = t / easeInNorm;
            return easeInNorm * localT * localT;
        } else if (t > (1.0 - easeOutNorm)) {
            double localT = (t - (1.0 - easeOutNorm)) / easeOutNorm;
            return 1.0 - easeOutNorm + easeOutNorm * (1.0 - (1.0 - localT) * (1.0 - localT));
        } else {
            return t;
        }
    }

    /**
     * Curvature types for path interpolation.
     */
    public enum CurvatureType {
        LINEAR(0),    // Straight line
        NORMAL(1),    // Curved path inside keyframes
        EXTREME(2);   // Curved path outside keyframes

        private final int value;

        CurvatureType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static CurvatureType fromValue(int val) {
            // Curvature comes as 65536 (0x10000) for value 1.0 in fixed-point
            // Convert from fixed-point to integer: divide by 65536
            int normalized = Math.min(val / 65536, 2);
            switch (normalized) {
                case 0: return LINEAR;
                case 1: return NORMAL;
                case 2: return EXTREME;
                default: return NORMAL;
            }
        }
    }

    /**
     * Apply curvature to interpolation.
     */
    public static double applyCurvature(double t, CurvatureType curvature) {
        switch (curvature) {
            case LINEAR:
                return t;
            case NORMAL:
                // Smooth S-curve using cubic ease-in-out
                if (t < 0.5) {
                    return 4.0 * t * t * t;
                } else {
                    return 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;
                }
            case EXTREME:
                // More extreme S-curve
                double t2 = t * t;
                double t3 = t2 * t;
                return 3.0 * t2 - 2.0 * t3;
            default:
                return t;
        }
    }
}
