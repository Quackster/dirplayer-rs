package com.dirplayer.player.rendering;

/**
 * Ink effects for sprite rendering in Director.
 * Inks control how sprite pixels are composited onto the stage.
 * Port of Rust ink constants.
 */
public final class InkEffect {

    private InkEffect() {
        // Prevent instantiation
    }

    /** Copy ink - direct pixel copy, white pixels become background */
    public static final int COPY = 0;

    /** Transparent ink - same as copy but with built-in transparency */
    public static final int TRANSPARENT = 1;

    /** Reverse ink - inverts background pixels under the sprite */
    public static final int REVERSE = 2;

    /** Ghost ink - sprite appears semi-transparent */
    public static final int GHOST = 3;

    /** Not Copy - logical NOT of copy ink */
    public static final int NOT_COPY = 4;

    /** Not Transparent - logical NOT of transparent */
    public static final int NOT_TRANSPARENT = 5;

    /** Not Reverse - logical NOT of reverse */
    public static final int NOT_REVERSE = 6;

    /** Not Ghost - logical NOT of ghost */
    public static final int NOT_GHOST = 7;

    /** Matte ink - uses sprite's matte (alpha) channel */
    public static final int MATTE = 8;

    /** Mask ink - sprite acts as a mask */
    public static final int MASK = 9;

    /** Blend ink - alpha blending using sprite's blend property */
    public static final int BLEND = 32;

    /** Add Pin ink - additive blending, clamped */
    public static final int ADD_PIN = 33;

    /** Add ink - additive blending, may overflow */
    public static final int ADD = 34;

    /** Subtract Pin ink - subtractive blending, clamped */
    public static final int SUBTRACT_PIN = 35;

    /** Background Transparent ink - background color becomes transparent */
    public static final int BACKGROUND_TRANSPARENT = 36;

    /** Lightest ink - keeps the lighter pixel */
    public static final int LIGHTEST = 37;

    /** Subtract ink - subtractive blending */
    public static final int SUBTRACT = 38;

    /** Darkest ink - keeps the darker pixel */
    public static final int DARKEST = 39;

    /** Darken ink - similar to darkest but different algorithm */
    public static final int DARKEN = 40;

    /** Lighten ink - similar to lightest but different algorithm */
    public static final int LIGHTEN = 41;

    /**
     * Check if the ink requires a matte/mask for proper rendering.
     *
     * @param ink The ink value
     * @return true if the ink should use matte
     */
    public static boolean shouldUseMatte(int ink) {
        return ink == COPY || ink == MATTE;
    }

    /**
     * Check if the ink requires alpha blending.
     *
     * @param ink The ink value
     * @return true if the ink uses alpha blending
     */
    public static boolean requiresBlending(int ink) {
        return ink >= BLEND;
    }

    /**
     * Check if the ink is a color-key based transparency.
     *
     * @param ink The ink value
     * @return true if the ink uses color-key transparency
     */
    public static boolean isColorKeyInk(int ink) {
        return ink == BACKGROUND_TRANSPARENT;
    }

    /**
     * Get a human-readable name for an ink value.
     *
     * @param ink The ink value
     * @return The ink name
     */
    public static String getName(int ink) {
        switch (ink) {
            case COPY: return "Copy";
            case TRANSPARENT: return "Transparent";
            case REVERSE: return "Reverse";
            case GHOST: return "Ghost";
            case NOT_COPY: return "Not Copy";
            case NOT_TRANSPARENT: return "Not Transparent";
            case NOT_REVERSE: return "Not Reverse";
            case NOT_GHOST: return "Not Ghost";
            case MATTE: return "Matte";
            case MASK: return "Mask";
            case BLEND: return "Blend";
            case ADD_PIN: return "Add Pin";
            case ADD: return "Add";
            case SUBTRACT_PIN: return "Subtract Pin";
            case BACKGROUND_TRANSPARENT: return "Background Transparent";
            case LIGHTEST: return "Lightest";
            case SUBTRACT: return "Subtract";
            case DARKEST: return "Darkest";
            case DARKEN: return "Darken";
            case LIGHTEN: return "Lighten";
            default: return "Unknown (" + ink + ")";
        }
    }
}
