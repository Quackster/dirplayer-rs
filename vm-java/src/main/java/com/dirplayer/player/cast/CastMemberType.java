package com.dirplayer.player.cast;

/**
 * Cast member type enum - type identifier without data.
 * Port of Rust CastMemberTypeId enum.
 */
public enum CastMemberType {
    Field("field"),
    Text("text"),
    Script("script"),
    Bitmap("bitmap"),
    Palette("palette"),
    Shape("shape"),
    FilmLoop("filmLoop"),
    Sound("sound"),
    Font("font"),
    Flash("flash"),
    DigitalVideo("digitalVideo"),
    Transition("transition"),
    Xtra("xtra"),
    Unknown("unknown");

    private final String symbolString;

    CastMemberType(String symbolString) {
        this.symbolString = symbolString;
    }

    public String getSymbolString() {
        return symbolString;
    }

    public static CastMemberType fromSymbol(String symbol) {
        for (CastMemberType type : values()) {
            if (type.symbolString.equalsIgnoreCase(symbol)) {
                return type;
            }
        }
        return Unknown;
    }
}
