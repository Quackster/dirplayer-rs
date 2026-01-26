package com.dirplayer.director;

/**
 * Enumeration of cast member types in Director files.
 * Port of Rust MemberType enum.
 */
public enum MemberType {
    Null(0),
    Bitmap(1),
    FilmLoop(2),
    Text(3),
    Palette(4),
    Picture(5),
    Sound(6),
    Button(7),
    Flash(8),
    Shape(9),
    DigitalVideo(10),
    Script(11),
    RTE(12),
    Transition(13),
    Xtra(14),
    Ole(15),
    Font(16),
    Shockwave3d(17),
    Unknown(255);

    private final int value;

    MemberType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MemberType from(int val) {
        for (MemberType type : values()) {
            if (type.value == val) {
                return type;
            }
        }
        return Unknown;
    }

    public static MemberType fromValue(long val) {
        return from((int) val);
    }

    /**
     * Get the name of this member type as a lowercase string (for symbol representation).
     */
    public String getName() {
        return this.name().toLowerCase();
    }

    // Alias for backward compatibility
    public static final MemberType NULL = Null;
}
