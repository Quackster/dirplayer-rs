package com.dirplayer.player.bitmap;

/**
 * Built-in palette types in Director.
 * Port of Rust BuiltInPalette enum.
 */
public enum BuiltInPalette {
    GrayScale(-3),
    Pastels(-4),
    Vivid(-5),
    Ntsc(-6),
    Metallic(-7),
    Web216(-8),
    Vga(-9),
    SystemWinDir4(-101),
    SystemWin(-102),
    SystemMac(-1),
    Rainbow(-2);

    private final int value;

    BuiltInPalette(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BuiltInPalette fromValue(int val) {
        for (BuiltInPalette p : values()) {
            if (p.value == val) {
                return p;
            }
        }
        return null;
    }

    public static BuiltInPalette fromSymbol(String symbol) {
        switch (symbol.toLowerCase()) {
            case "grayscale": return GrayScale;
            case "pastels": return Pastels;
            case "vivid": return Vivid;
            case "ntsc": return Ntsc;
            case "metallic": return Metallic;
            case "web216": return Web216;
            case "vga": return Vga;
            case "systemwindir4": return SystemWinDir4;
            case "systemwin": return SystemWin;
            case "systemmac": return SystemMac;
            case "rainbow": return Rainbow;
            default: return null;
        }
    }

    public String toSymbol() {
        switch (this) {
            case GrayScale: return "grayscale";
            case Pastels: return "pastels";
            case Vivid: return "vivid";
            case Ntsc: return "ntsc";
            case Metallic: return "metallic";
            case Web216: return "web216";
            case Vga: return "vga";
            case SystemWinDir4: return "systemWinDir4";
            case SystemWin: return "systemWin";
            case SystemMac: return "systemMac";
            case Rainbow: return "rainbow";
            default: return "unknown";
        }
    }
}
