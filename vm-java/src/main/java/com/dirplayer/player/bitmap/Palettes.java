package com.dirplayer.player.bitmap;

/**
 * Built-in palette data for Director.
 * Port of Rust palette constants.
 */
public class Palettes {

    // Grayscale 256-color palette
    public static final int[][] GRAYSCALE_PALETTE = generateGrayscale256();

    // Grayscale 16-color palette
    public static final int[][] GRAYSCALE_16_PALETTE = generateGrayscale16();

    // Grayscale 4-color palette (for 2-bit images)
    public static final int[][] GRAYSCALE_4_PALETTE = {
        {255, 255, 255}, {170, 170, 170}, {85, 85, 85}, {0, 0, 0}
    };

    // Web 216 colors
    public static final int[][] WEB_216_PALETTE = generateWeb216();

    // System Windows palette (commonly used)
    public static final int[][] SYSTEM_WIN_PALETTE = generateSystemWinPalette();

    // System Mac palette
    public static final int[][] SYSTEM_MAC_PALETTE = generateSystemMacPalette();

    // 16-color Windows palette
    public static final int[][] WIN_16_PALETTE = {
        {0, 0, 0},       {128, 0, 0},     {0, 128, 0},     {128, 128, 0},
        {0, 0, 128},     {128, 0, 128},   {0, 128, 128},   {192, 192, 192},
        {128, 128, 128}, {255, 0, 0},     {0, 255, 0},     {255, 255, 0},
        {0, 0, 255},     {255, 0, 255},   {0, 255, 255},   {255, 255, 255}
    };

    // 16-color Mac palette
    public static final int[][] MAC_16_PALETTE = {
        {255, 255, 255}, {255, 255, 0},   {255, 102, 0},   {221, 0, 0},
        {255, 0, 153},   {51, 0, 153},    {0, 0, 204},     {0, 153, 255},
        {0, 170, 0},     {0, 102, 0},     {102, 51, 0},    {153, 102, 51},
        {187, 187, 187}, {136, 136, 136}, {68, 68, 68},    {0, 0, 0}
    };

    // Rainbow 16-color palette
    public static final int[][] RAINBOW16_PALETTE = generateRainbow16();

    // Rainbow 256-color palette
    public static final int[][] RAINBOW_PALETTE = generateRainbow256();

    // Pastels 16-color palette
    public static final int[][] PASTELS16_PALETTE = generatePastels16();

    // Pastels 256-color palette
    public static final int[][] PASTELS_PALETTE = generatePastels256();

    // Vivid 16-color palette
    public static final int[][] VIVID16_PALETTE = generateVivid16();

    // Vivid 256-color palette
    public static final int[][] VIVID_PALETTE = generateVivid256();

    // NTSC 16-color palette
    public static final int[][] NTSC16_PALETTE = generateNtsc16();

    // NTSC 256-color palette
    public static final int[][] NTSC_PALETTE = generateNtsc256();

    // Metallic 16-color palette
    public static final int[][] METALLIC16_PALETTE = generateMetallic16();

    // Metallic 256-color palette
    public static final int[][] METALLIC_PALETTE = generateMetallic256();

    private static int[][] generateGrayscale256() {
        int[][] palette = new int[256][3];
        for (int i = 0; i < 256; i++) {
            palette[i] = new int[] {255 - i, 255 - i, 255 - i};
        }
        return palette;
    }

    private static int[][] generateGrayscale16() {
        int[][] palette = new int[16][3];
        for (int i = 0; i < 16; i++) {
            int gray = 255 - (i * 17);
            palette[i] = new int[] {gray, gray, gray};
        }
        return palette;
    }

    private static int[][] generateWeb216() {
        int[][] palette = new int[256][3];
        int[] levels = {0, 51, 102, 153, 204, 255};
        int idx = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    if (idx < 216) {
                        palette[idx++] = new int[] {levels[r], levels[g], levels[b]};
                    }
                }
            }
        }
        // Fill remaining with black
        while (idx < 256) {
            palette[idx++] = new int[] {0, 0, 0};
        }
        return palette;
    }

    private static int[][] generateSystemWinPalette() {
        int[][] palette = new int[256][3];
        // First 10 colors
        palette[0] = new int[] {0, 0, 0};
        palette[1] = new int[] {128, 0, 0};
        palette[2] = new int[] {0, 128, 0};
        palette[3] = new int[] {128, 128, 0};
        palette[4] = new int[] {0, 0, 128};
        palette[5] = new int[] {128, 0, 128};
        palette[6] = new int[] {0, 128, 128};
        palette[7] = new int[] {192, 192, 192};
        palette[8] = new int[] {192, 220, 192};
        palette[9] = new int[] {166, 202, 240};

        // Grayscale in middle
        for (int i = 10; i < 246; i++) {
            int gray = ((i - 10) * 255) / 235;
            palette[i] = new int[] {gray, gray, gray};
        }

        // Last 10 colors
        palette[246] = new int[] {255, 251, 240};
        palette[247] = new int[] {160, 160, 164};
        palette[248] = new int[] {128, 128, 128};
        palette[249] = new int[] {255, 0, 0};
        palette[250] = new int[] {0, 255, 0};
        palette[251] = new int[] {255, 255, 0};
        palette[252] = new int[] {0, 0, 255};
        palette[253] = new int[] {255, 0, 255};
        palette[254] = new int[] {0, 255, 255};
        palette[255] = new int[] {255, 255, 255};

        return palette;
    }

    private static int[][] generateSystemMacPalette() {
        // Generate a 6x6x6 color cube similar to Mac system palette
        int[][] palette = new int[256][3];
        int[] levels = {255, 204, 153, 102, 51, 0};
        int idx = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    if (idx < 216) {
                        palette[idx++] = new int[] {levels[r], levels[g], levels[b]};
                    }
                }
            }
        }
        // Fill remaining with grayscale
        while (idx < 256) {
            int gray = (256 - idx) * 255 / 40;
            palette[idx++] = new int[] {gray, gray, gray};
        }
        return palette;
    }

    private static int[][] generateRainbow16() {
        return new int[][] {
            {255, 0, 0},     {255, 128, 0},   {255, 255, 0},   {128, 255, 0},
            {0, 255, 0},     {0, 255, 128},   {0, 255, 255},   {0, 128, 255},
            {0, 0, 255},     {128, 0, 255},   {255, 0, 255},   {255, 0, 128},
            {255, 255, 255}, {192, 192, 192}, {128, 128, 128}, {0, 0, 0}
        };
    }

    private static int[][] generateRainbow256() {
        int[][] palette = new int[256][3];
        for (int i = 0; i < 256; i++) {
            // Generate rainbow colors using HSV to RGB conversion
            float h = (i / 256.0f) * 360.0f;
            int[] rgb = hsvToRgb(h, 1.0f, 1.0f);
            palette[i] = rgb;
        }
        return palette;
    }

    private static int[][] generatePastels16() {
        return new int[][] {
            {255, 204, 204}, {255, 229, 204}, {255, 255, 204}, {229, 255, 204},
            {204, 255, 204}, {204, 255, 229}, {204, 255, 255}, {204, 229, 255},
            {204, 204, 255}, {229, 204, 255}, {255, 204, 255}, {255, 204, 229},
            {255, 255, 255}, {224, 224, 224}, {192, 192, 192}, {160, 160, 160}
        };
    }

    private static int[][] generatePastels256() {
        int[][] palette = new int[256][3];
        for (int i = 0; i < 256; i++) {
            float h = (i / 256.0f) * 360.0f;
            int[] rgb = hsvToRgb(h, 0.3f, 1.0f);
            palette[i] = rgb;
        }
        return palette;
    }

    private static int[][] generateVivid16() {
        return new int[][] {
            {255, 0, 0},     {255, 128, 0},   {255, 255, 0},   {0, 255, 0},
            {0, 255, 255},   {0, 0, 255},     {255, 0, 255},   {255, 0, 128},
            {128, 0, 0},     {128, 64, 0},    {128, 128, 0},   {0, 128, 0},
            {0, 128, 128},   {0, 0, 128},     {128, 0, 128},   {0, 0, 0}
        };
    }

    private static int[][] generateVivid256() {
        int[][] palette = new int[256][3];
        for (int i = 0; i < 256; i++) {
            float h = (i / 256.0f) * 360.0f;
            int[] rgb = hsvToRgb(h, 1.0f, 1.0f);
            palette[i] = rgb;
        }
        return palette;
    }

    private static int[][] generateNtsc16() {
        return new int[][] {
            {255, 255, 255}, {255, 255, 0},   {0, 255, 255},   {0, 255, 0},
            {255, 0, 255},   {255, 0, 0},     {0, 0, 255},     {0, 0, 0},
            {192, 192, 192}, {192, 192, 0},   {0, 192, 192},   {0, 192, 0},
            {192, 0, 192},   {192, 0, 0},     {0, 0, 192},     {64, 64, 64}
        };
    }

    private static int[][] generateNtsc256() {
        int[][] palette = new int[256][3];
        // Generate NTSC-safe colors
        for (int i = 0; i < 256; i++) {
            int r = Math.min(235, 16 + (i % 16) * 14);
            int g = Math.min(235, 16 + ((i / 16) % 16) * 14);
            int b = Math.min(235, 16 + (i / 256) * 14);
            palette[i] = new int[] {r, g, b};
        }
        return palette;
    }

    private static int[][] generateMetallic16() {
        return new int[][] {
            {255, 255, 255}, {224, 224, 224}, {192, 192, 192}, {160, 160, 160},
            {128, 128, 128}, {96, 96, 96},    {64, 64, 64},    {32, 32, 32},
            {255, 215, 0},   {192, 192, 128}, {128, 128, 96},  {64, 64, 48},
            {192, 128, 64},  {128, 96, 64},   {64, 48, 32},    {0, 0, 0}
        };
    }

    private static int[][] generateMetallic256() {
        int[][] palette = new int[256][3];
        // Generate metallic gradient
        for (int i = 0; i < 256; i++) {
            int base = 255 - i;
            int gold = (i < 128) ? (i * 2) : (255 - (i - 128) * 2);
            palette[i] = new int[] {base, base - gold / 4, base - gold / 2};
        }
        return palette;
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = v - c;

        float r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }

        return new int[] {
            (int) ((r + m) * 255),
            (int) ((g + m) * 255),
            (int) ((b + m) * 255)
        };
    }

    /**
     * Get color from a palette by index.
     */
    public static int[] getColor(int[][] palette, int index) {
        if (index >= 0 && index < palette.length) {
            return palette[index];
        }
        // Default colors for out of bounds
        if (index == 0) {
            return new int[] {255, 255, 255};
        } else if (index == 255) {
            return new int[] {0, 0, 0};
        }
        return new int[] {255, 0, 255}; // Magenta for missing colors
    }

    /**
     * Get palette by built-in type.
     */
    public static int[][] getPalette(BuiltInPalette type, int originalBitDepth) {
        switch (type) {
            case GrayScale:
                if (originalBitDepth == 2) return GRAYSCALE_4_PALETTE;
                if (originalBitDepth == 4) return GRAYSCALE_16_PALETTE;
                return GRAYSCALE_PALETTE;
            case SystemMac:
                if (originalBitDepth == 4) return MAC_16_PALETTE;
                return SYSTEM_MAC_PALETTE;
            case SystemWin:
            case SystemWinDir4:
            case Vga:
                if (originalBitDepth == 4) return WIN_16_PALETTE;
                return SYSTEM_WIN_PALETTE;
            case Rainbow:
                if (originalBitDepth == 4) return RAINBOW16_PALETTE;
                return RAINBOW_PALETTE;
            case Pastels:
                if (originalBitDepth == 4) return PASTELS16_PALETTE;
                return PASTELS_PALETTE;
            case Vivid:
                if (originalBitDepth == 4) return VIVID16_PALETTE;
                return VIVID_PALETTE;
            case Ntsc:
                if (originalBitDepth == 4) return NTSC16_PALETTE;
                return NTSC_PALETTE;
            case Metallic:
                if (originalBitDepth == 4) return METALLIC16_PALETTE;
                return METALLIC_PALETTE;
            case Web216:
                return WEB_216_PALETTE;
            default:
                return SYSTEM_WIN_PALETTE;
        }
    }
}
