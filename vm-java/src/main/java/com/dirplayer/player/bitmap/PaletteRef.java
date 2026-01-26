package com.dirplayer.player.bitmap;

import com.dirplayer.player.CastMemberRef;

/**
 * Reference to a palette - either built-in or a cast member.
 * Port of Rust PaletteRef enum.
 */
public class PaletteRef {
    private BuiltInPalette builtIn;
    private CastMemberRef memberRef;
    private boolean isBuiltIn;

    private PaletteRef() {}

    public static PaletteRef ofBuiltIn(BuiltInPalette palette) {
        PaletteRef ref = new PaletteRef();
        ref.builtIn = palette;
        ref.isBuiltIn = true;
        return ref;
    }

    public static PaletteRef ofMember(CastMemberRef memberRef) {
        PaletteRef ref = new PaletteRef();
        ref.memberRef = memberRef;
        ref.isBuiltIn = false;
        return ref;
    }

    public static PaletteRef from(short paletteId, int castLib) {
        if (paletteId < 0) {
            BuiltInPalette palette = BuiltInPalette.fromValue(paletteId);
            if (palette != null) {
                return ofBuiltIn(palette);
            }
            // Unknown built-in palette, default to SystemWin
            return ofBuiltIn(BuiltInPalette.SystemWin);
        } else {
            return ofMember(new CastMemberRef(castLib, paletteId + 1));
        }
    }

    public boolean isBuiltIn() {
        return isBuiltIn;
    }

    public BuiltInPalette getBuiltIn() {
        return builtIn;
    }

    public CastMemberRef getMemberRef() {
        return memberRef;
    }

    @Override
    public String toString() {
        if (isBuiltIn) {
            return "PaletteRef(BuiltIn: " + builtIn + ")";
        } else {
            return "PaletteRef(Member: " + memberRef + ")";
        }
    }
}
